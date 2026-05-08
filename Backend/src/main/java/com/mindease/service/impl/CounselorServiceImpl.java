package com.mindease.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.exception.BaseException;
import com.mindease.mapper.*;
import org.springframework.transaction.annotation.Transactional;
import com.mindease.pojo.dto.ReviewSubmitDTO;
import com.mindease.pojo.entity.*;
import com.mindease.pojo.vo.*;
import com.mindease.service.CounselorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CounselorServiceImpl implements CounselorService {

    @Autowired
    private CounselorProfileMapper counselorProfileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MoodLogMapper moodLogMapper;

    @Autowired
    private AssessmentRecordMapper assessmentRecordMapper;

    @Autowired
    private CounselorReviewMapper counselorReviewMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private com.mindease.service.AppointmentService appointmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 测评量表到关键词的映射
    private static final Map<String, List<String>> ASSESSMENT_KEYWORD_MAP = new HashMap<String, List<String>>() {{
        put("焦虑", Arrays.asList("焦虑", "紧张", "担忧", "恐慌"));
        put("抑郁", Arrays.asList("抑郁", "情绪低落", "悲伤", "失落"));
        put("失眠", Arrays.asList("失眠", "睡眠", "入睡困难", "睡眠障碍"));
        put("压力", Arrays.asList("压力", "疲惫", "倦怠", "应激"));
        put("强迫", Arrays.asList("强迫", "反复", "重复行为"));
        put("恐惧", Arrays.asList("恐惧", "害怕", "回避"));
    }};

    // 情绪类型到关键词映射（基于实际数据库 mood_type 字段值）
    private static final Map<String, List<String>> MOOD_TYPE_KEYWORD_MAP = new HashMap<String, List<String>>() {{
        // Anxious - 焦虑：需要专业咨询
        put("Anxious", Arrays.asList("焦虑", "紧张", "担忧", "恐慌"));
        
        // Sad - 悲伤：可能涉及抑郁情绪
        put("Sad", Arrays.asList("抑郁", "情绪低落", "悲伤", "失落"));
        
        // Angry - 愤怒：情绪管理需求
        put("Angry", Arrays.asList("愤怒", "情绪管理", "冲动控制"));
        
        // Tired - 疲惫：压力与倦怠
        put("Tired", Arrays.asList("压力", "疲惫", "倦怠", "失眠"));
        
        // Happy - 开心：积极心理维护（低优先级）
        put("Happy", Arrays.asList("积极心理", "心理健康"));
        
        // Calm - 平静：可能需要压力管理指导（低优先级）
        put("Calm", Arrays.asList("压力管理", "放松技巧"));
        
        // Excited - 兴奋：情绪波动管理（低优先级）
        put("Excited", Arrays.asList("情绪管理", "情绪波动"));
    }};

    // 负面情绪类型（需要重点关注的情绪）
    private static final Set<String> NEGATIVE_MOOD_TYPES = new HashSet<>(Arrays.asList(
        "Anxious", "Sad", "Angry", "Tired"
    ));

    // 中文地名列表
    private static final Set<String> CHINESE_CITIES = new HashSet<>(Arrays.asList(
        "北京", "上海", "广州", "深圳", "天津", "重庆", "成都", "杭州", "武汉", "西安",
        "南京", "郑州", "长沙", "沈阳", "青岛", "大连", "宁波", "厦门", "济南", "哈尔滨",
        "苏州", "无锡", "福州", "石家庄", "昆明", "兰州", "太原", "合肥", "南昌", "贵阳",
        "南宁", "海口", "银川", "西宁", "呼和浩特", "乌鲁木齐", "拉萨", "线上", "在线"
    ));

    /**
     * 智能推荐咨询师（增强版）
     */
    @Override
    public RecommendResultVO recommendCounselors(Long userId, String keyword, String sort) {
        log.info("智能推荐咨询师，用户ID:{}，关键词:{}，排序:{}", userId, keyword, sort);

        // 1. 获取用户画像数据
        List<String> keywords = new ArrayList<>();
        String strategy = "hot_list";
        String basedOn = "热门咨询师列表";
        List<String> userTags = new ArrayList<>();

        // 1.1 查询用户最近7天的情绪日志
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<MoodLog> recentMoodLogs = moodLogMapper.getRecentMoodLogs(userId, sevenDaysAgo);

        boolean isUrgent = false;
        
        if (recentMoodLogs != null && !recentMoodLogs.isEmpty()) {
            log.info("用户{}最近7天有{}条情绪日志", userId, recentMoodLogs.size());
            
            double avgScore = recentMoodLogs.stream()
                    .mapToInt(MoodLog::getMoodScore)
                    .average()
                    .orElse(10.0);

            log.info("用户{}情绪平均分: {}", userId, avgScore);

            if (avgScore < 4) {
                isUrgent = true;
                userTags.add("紧急");
            }
            
            Map<String, Long> moodTypeCounts = recentMoodLogs.stream()
                    .collect(Collectors.groupingBy(MoodLog::getMoodType, Collectors.counting()));
            
            log.info("用户{}情绪类型分布: {}", userId, moodTypeCounts);
            
            // 优先提取负面情绪关键词
            List<String> negativeMoodTypes = new ArrayList<>();
            List<String> positiveMoodTypes = new ArrayList<>();
            
            for (Map.Entry<String, Long> entry : moodTypeCounts.entrySet()) {
                if (entry.getValue() >= 1) {
                    if (NEGATIVE_MOOD_TYPES.contains(entry.getKey())) {
                        negativeMoodTypes.add(entry.getKey());
                    } else {
                        positiveMoodTypes.add(entry.getKey());
                    }
                }
            }
            
            // 先处理负面情绪
            for (String moodType : negativeMoodTypes) {
                List<String> moodKeywords = MOOD_TYPE_KEYWORD_MAP.get(moodType);
                if (moodKeywords != null) {
                    keywords.addAll(moodKeywords);
                    userTags.add(moodType); // 添加情绪类型标签
                    log.info("从负面情绪类型 {} 提取关键词: {}", moodType, moodKeywords);
                }
            }
            
            // 如果没有负面情绪，再考虑正面情绪（权重较低）
            if (keywords.isEmpty() && !positiveMoodTypes.isEmpty()) {
                for (String moodType : positiveMoodTypes) {
                    List<String> moodKeywords = MOOD_TYPE_KEYWORD_MAP.get(moodType);
                    if (moodKeywords != null) {
                        keywords.addAll(moodKeywords);
                        log.info("从正面情绪类型 {} 提取关键词: {}", moodType, moodKeywords);
                    }
                }
            }
            
            if (!keywords.isEmpty()) {
                strategy = "mood_based";
                if (!negativeMoodTypes.isEmpty()) {
                    basedOn = "近期情绪状态分析（" + String.join("、", negativeMoodTypes) + "）";
                } else {
                    basedOn = "近期情绪状态分析";
                }
                log.info("基于情绪分析提取关键词: {}", keywords);
            } else {
                // 即使没有提取到关键词，也标记为情绪驱动
                strategy = "mood_based";
                basedOn = "近期情绪状态";
                log.info("有情绪数据但未提取到关键词，使用热门推荐");
            }
        } else {
            log.info("用户{}最近7天无情绪日志", userId);
        }

        // 1.2 查询用户最近的测评记录
        AssessmentRecord latestAssessment = assessmentRecordMapper.getLatestByUserId(userId);
        if (latestAssessment != null) {
            strategy = "assessment_based";
            basedOn = latestAssessment.getScaleKey() + " " + latestAssessment.getResultLevel();

            String resultLevel = latestAssessment.getResultLevel();
            if (resultLevel != null) {
                List<String> extractedKeywords = extractKeywordsFromText(resultLevel);
                keywords.addAll(extractedKeywords);
                userTags.add(resultLevel);
                log.info("从测评结果提取关键词: {}", extractedKeywords);
            }
        }

        // 1.3 手动关键词优先级最高
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            keywords.clear();
            keywords.add(trimmedKeyword);
            strategy = "keyword_search";
            basedOn = "搜索关键词：" + trimmedKeyword;
        }

        // 1.4 生成关键词变体，提升模糊匹配（问题7：优化避免过度裁剪）
        keywords = expandKeywordVariantsOptimized(keywords);
        log.info("扩展后关键词: {}", keywords);

        // 【改进3】协同过滤：查询用户历史预约的咨询师
        List<Long> historyCounselorIds = new ArrayList<>();
        int completedCount = appointmentMapper.countCompletedByUserId(userId);
        if (completedCount > 0) {
            historyCounselorIds = appointmentMapper.getTopCounselorIdsByUser(userId, 3);
            log.info("用户历史预约咨询师: {}, 完成预约数: {}", historyCounselorIds, completedCount);
        }

        // 个性化权重：根据用户预约历史判断消费偏好
        UserPreference userPreference = analyzeUserPreference(userId, completedCount);
        log.info("用户偏好分析: {}", userPreference);

        // 2. 查询咨询师列表
        List<CounselorProfile> profiles;
        boolean hasKeyword = !keywords.isEmpty();
        if (!hasKeyword) {
            profiles = counselorProfileMapper.getAllActiveCounselors();
        } else {
            profiles = counselorProfileMapper.recommendCounselors(
                    keywords,
                    sort != null ? sort : "smart"
            );
        }

        // 【改进3】协同过滤增强：加入相似咨询师
        if (!historyCounselorIds.isEmpty() && profiles.size() < 10) {
            List<CounselorProfile> similarProfiles = counselorProfileMapper.getByCounselorIds(historyCounselorIds);
            // 去重后添加
            for (CounselorProfile sp : similarProfiles) {
                if (profiles.stream().noneMatch(p -> p.getUserId().equals(sp.getUserId()))) {
                    profiles.add(sp);
                    if (profiles.size() >= 10) break;
                }
            }
            log.info("协同过滤增强后咨询师数量: {}", profiles.size());
        }

        // 【改进9】根据用户偏好调整排序权重
        if (userPreference != null && "smart".equals(sort)) {
            profiles = applyPersonalizedWeight(profiles, userPreference);
        }

        // 【改进6】多样性控制：前5个按匹配度，后5个按多样性
        profiles = applyDiversityControl(profiles);

        // 3. 构建推荐列表
        final List<String> finalKeywords = keywords;
        final boolean finalIsUrgent = isUrgent;
        final List<Long> finalHistoryIds = historyCounselorIds;
        
        List<CounselorRecommendVO> counselors = profiles.stream().map(profile -> {
            User user = userMapper.getById(profile.getUserId());
            List<String> specialtyList = parseJsonArray(profile.getSpecialty());

            String matchReason = generateMatchReason(profile, finalKeywords, finalIsUrgent, finalHistoryIds);

            // 【改进5】真实检查"今日可约"标签
            List<String> tags = generateTagsWithRealAvailability(profile, finalIsUrgent);

            // 计算最近可用时间
            String nextAvailableTime = calculateNextAvailableTime(profile.getUserId());

            return CounselorRecommendVO.builder()
                    .id(profile.getUserId())
                    .realName(profile.getRealName())
                    .avatar(user != null ? user.getAvatar() : null)
                    .title(profile.getTitle())
                    .experienceYears(profile.getExperienceYears())
                    .specialty(specialtyList)
                    .rating(profile.getRating())
                    .pricePerHour(profile.getPricePerHour())
                    .location(profile.getLocation())
                    .nextAvailableTime(nextAvailableTime)
                    .matchReason(matchReason)
                    .tags(tags)
                    .build();
        }).collect(Collectors.toList());

        // 4. 构建推荐上下文
        RecommendContextVO context = RecommendContextVO.builder()
                .strategy(strategy)
                .basedOn(basedOn)
                .userTags(userTags)
                .build();

        return RecommendResultVO.builder()
                .recommendContext(context)
                .counselors(counselors)
                .build();
    }

    /**
     * 【改进1】从文本中提取关键词（基于规则+NLP思想）
     */
    private List<String> extractKeywordsFromText(String text) {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return keywords;
        }

        // 遍历映射表，查找匹配的关键词
        for (Map.Entry<String, List<String>> entry : ASSESSMENT_KEYWORD_MAP.entrySet()) {
            if (text.contains(entry.getKey())) {
                keywords.addAll(entry.getValue());
            }
        }

        // 使用正则提取中文关键词（2-4个字）
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String word = matcher.group();
            // 过滤掉常见的无意义词
            if (!isStopWord(word)) {
                addIfAbsent(keywords, word);
            }
        }

        return keywords.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 判断是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "的", "是", "在", "有", "和", "了", "不", "与", "中", "为", "对", "及", 
            "个", "等", "但", "或", "从", "到", "而", "由", "也", "很", "就", "可能",
            "轻度", "中度", "重度", "严重", "明显", "症状", "状态", "情况", "程度"
        ));
        return stopWords.contains(word);
    }

    /**
     * 【改进7】优化的关键词扩展：避免过度裁剪地名等
     */
    private List<String> expandKeywordVariantsOptimized(List<String> source) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> suffixes = Arrays.asList("症", "障碍", "问题", "情况", "状态", "情绪", "病", "感");
        List<String> result = new ArrayList<>();
        
        for (String kw : source) {
            if (kw == null) continue;
            String base = kw.trim();
            if (base.isEmpty()) continue;
            
            // 总是保留原词
            addIfAbsent(result, base);
            
            // 去后缀变体
            for (String suffix : suffixes) {
                if (base.endsWith(suffix) && base.length() > suffix.length()) {
                    String stripped = base.substring(0, base.length() - suffix.length());
                    if (stripped.length() >= 2) {
                        addIfAbsent(result, stripped);
                    }
                }
            }
            
            // 【改进7】只对非地名且长度>3的词进行末尾裁剪
            if (base.length() > 3 && !isLocationName(base)) {
                String shorter = base.substring(0, base.length() - 1);
                addIfAbsent(result, shorter);
            }
        }
        return result;
    }

    /**
     * 判断是否为地名
     */
    private boolean isLocationName(String word) {
        return CHINESE_CITIES.contains(word) || CHINESE_CITIES.stream().anyMatch(word::contains);
    }

    private void addIfAbsent(List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    /**
     * 【改进9】分析用户偏好
     */
    private UserPreference analyzeUserPreference(Long userId, int completedCount) {
        if (completedCount == 0) {
            return new UserPreference("balanced", 0);
        }

        // 查询用户历史预约的咨询师，分析价格偏好
        List<Long> topCounselorIds = appointmentMapper.getTopCounselorIdsByUser(userId, 5);
        if (topCounselorIds.isEmpty()) {
            return new UserPreference("balanced", completedCount);
        }

        List<CounselorProfile> historyCounselors = counselorProfileMapper.getByCounselorIds(topCounselorIds);
        if (historyCounselors.isEmpty()) {
            return new UserPreference("balanced", completedCount);
        }

        // 计算平均价格和评分偏好
        double avgPrice = historyCounselors.stream()
                .filter(c -> c.getPricePerHour() != null)
                .mapToDouble(c -> c.getPricePerHour().doubleValue())
                .average()
                .orElse(300.0);

        double avgRating = historyCounselors.stream()
                .filter(c -> c.getRating() != null)
                .mapToDouble(c -> c.getRating().doubleValue())
                .average()
                .orElse(4.5);

        String preferenceType;
        if (avgPrice < 250) {
            preferenceType = "price_sensitive";
        } else if (avgRating >= 4.8) {
            preferenceType = "quality_first";
        } else {
            preferenceType = "balanced";
        }

        return new UserPreference(preferenceType, completedCount);
    }

    /**
     * 用户偏好内部类
     */
    private static class UserPreference {
        String type; // price_sensitive, quality_first, balanced
        int experienceLevel; // 预约次数

        UserPreference(String type, int experienceLevel) {
            this.type = type;
            this.experienceLevel = experienceLevel;
        }

        @Override
        public String toString() {
            return "UserPreference{type='" + type + "', experienceLevel=" + experienceLevel + "}";
        }
    }

    /**
     * 【改进9】应用个性化权重
     */
    private List<CounselorProfile> applyPersonalizedWeight(List<CounselorProfile> profiles, UserPreference preference) {
        if (preference == null || "balanced".equals(preference.type)) {
            return profiles;
        }

        return profiles.stream()
                .sorted((p1, p2) -> {
                    double score1 = calculatePersonalizedScore(p1, preference);
                    double score2 = calculatePersonalizedScore(p2, preference);
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算个性化得分
     */
    private double calculatePersonalizedScore(CounselorProfile profile, UserPreference preference) {
        double score = 0;

        // 基础评分权重
        if (profile.getRating() != null) {
            score += profile.getRating().doubleValue() * 20;
        }

        // 根据用户偏好调整
        if ("price_sensitive".equals(preference.type)) {
            // 价格敏感：价格越低分越高
            if (profile.getPricePerHour() != null) {
                score += Math.max(0, (500 - profile.getPricePerHour().doubleValue()) / 10);
            }
        } else if ("quality_first".equals(preference.type)) {
            // 质量优先：评分和评价数越高分越高
            if (profile.getReviewCount() != null) {
                score += Math.min(50, profile.getReviewCount() / 2.0);
            }
        }

        return score;
    }

    /**
     * 【改进6】多样性控制（MMR算法简化版）
     */
    private List<CounselorProfile> applyDiversityControl(List<CounselorProfile> profiles) {
        if (profiles.size() <= 5) {
            return profiles;
        }

        List<CounselorProfile> result = new ArrayList<>();
        
        // 前5个保持原顺序（高匹配度）
        result.addAll(profiles.subList(0, Math.min(5, profiles.size())));

        // 后续从剩余中选择多样性高的
        List<CounselorProfile> remaining = new ArrayList<>(profiles.subList(5, profiles.size()));
        
        while (result.size() < 10 && !remaining.isEmpty()) {
            CounselorProfile mostDiverse = findMostDiverse(result, remaining);
            if (mostDiverse != null) {
                result.add(mostDiverse);
                remaining.remove(mostDiverse);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * 找到最多样化的咨询师
     */
    private CounselorProfile findMostDiverse(List<CounselorProfile> selected, List<CounselorProfile> candidates) {
        CounselorProfile mostDiverse = null;
        double maxDiversity = -1;

        for (CounselorProfile candidate : candidates) {
            double diversity = calculateDiversity(candidate, selected);
            if (diversity > maxDiversity) {
                maxDiversity = diversity;
                mostDiverse = candidate;
            }
        }

        return mostDiverse;
    }

    /**
     * 计算多样性得分
     */
    private double calculateDiversity(CounselorProfile candidate, List<CounselorProfile> selected) {
        double diversityScore = 0;

        List<String> candidateSpecialty = parseJsonArray(candidate.getSpecialty());
        
        for (CounselorProfile s : selected) {
            List<String> selectedSpecialty = parseJsonArray(s.getSpecialty());
            
            // 专长重叠度
            long overlap = candidateSpecialty.stream()
                    .filter(selectedSpecialty::contains)
                    .count();
            
            // 重叠越少，多样性越高
            diversityScore += (candidateSpecialty.size() - overlap);
            
            // 价格差异
            if (candidate.getPricePerHour() != null && s.getPricePerHour() != null) {
                double priceDiff = Math.abs(candidate.getPricePerHour().doubleValue() - 
                                           s.getPricePerHour().doubleValue());
                diversityScore += priceDiff / 100;
            }
            
            // 地区差异
            if (candidate.getLocation() != null && s.getLocation() != null && 
                !candidate.getLocation().equals(s.getLocation())) {
                diversityScore += 10;
            }
        }

        return diversityScore;
    }

    /**
     * 【改进5】生成带真实可用性检查的标签
     */
    private List<String> generateTagsWithRealAvailability(CounselorProfile profile, boolean isUrgent) {
        List<String> tags = new ArrayList<>();

        if (profile.getPricePerHour() != null && profile.getPricePerHour().compareTo(BigDecimal.valueOf(300)) < 0) {
            tags.add("价格亲民");
        }

        if (profile.getRating() != null && profile.getRating().compareTo(BigDecimal.valueOf(4.8)) >= 0) {
            tags.add("高评分");
        }

        if (profile.getReviewCount() != null && profile.getReviewCount() > 50) {
            tags.add("经验丰富");
        }

        // 【改进5】真实检查今日是否可约
        if (isUrgent) {
            try {
                LocalDate today = LocalDate.now();
                AvailableSlotsVO slots = appointmentService.getAvailableSlots(
                    profile.getUserId(), 
                    today.toString()
                );
                if (slots != null && slots.getSlots() != null && !slots.getSlots().isEmpty()) {
                    tags.add("今日可约");
                }
            } catch (Exception e) {
                log.warn("检查咨询师{}今日可约状态失败: {}", profile.getUserId(), e.getMessage());
            }
        }

        return tags;
    }

    /**
     * 检查推荐前置状态
     */
    @Override
    public RecommendStatusVO getRecommendStatus(Long userId) {
        boolean hasAssessment = assessmentRecordMapper.countByUserId(userId) > 0;
        boolean hasMoodLog = moodLogMapper.countByUserId(userId) > 0;

        String lastAssessmentLevel = null;
        if (hasAssessment) {
            AssessmentRecord latestRecord = assessmentRecordMapper.getLatestByUserId(userId);
            lastAssessmentLevel = latestRecord != null ? latestRecord.getResultLevel() : null;
        }

        boolean recommendationReady = hasAssessment || hasMoodLog;

        return RecommendStatusVO.builder()
                .hasAssessment(hasAssessment)
                .hasMoodLog(hasMoodLog)
                .lastAssessmentLevel(lastAssessmentLevel)
                .recommendationReady(recommendationReady)
                .build();
    }

    /**
     * 获取咨询师详情
     */
    @Override
    public CounselorDetailVO getCounselorDetail(Long counselorId) {
        CounselorProfile profile = counselorProfileMapper.getByUserId(counselorId);
        if (profile == null) {
            throw new BaseException("咨询师不存在");
        }

        User user = userMapper.getById(counselorId);
        List<String> specialtyList = parseJsonArray(profile.getSpecialty());

        List<String> tags = extractTagsFromReviews(counselorId);

        return CounselorDetailVO.builder()
                .id(counselorId)
                .realName(profile.getRealName())
                .avatar(user != null ? user.getAvatar() : null)
                .title(profile.getTitle())
                .experienceYears(profile.getExperienceYears())
                .specialty(specialtyList)
                .bio(profile.getBio())
                .qualificationUrl(profile.getQualificationUrl())
                .rating(profile.getRating())
                .reviewCount(profile.getReviewCount())
                .pricePerHour(profile.getPricePerHour())
                .location(profile.getLocation())
                .isOnline(profile.getLocation() != null && profile.getLocation().contains("线上"))
                .tags(tags)
                .build();
    }

    /**
     * 获取咨询师评价列表
     */
    @Override
    public ReviewListVO getCounselorReviews(Long counselorId, Integer limit, Integer offset) {
        List<CounselorReview> reviews = counselorReviewMapper.getByCounselorId(counselorId, limit, offset);
        int total = counselorReviewMapper.countByCounselorId(counselorId);
        Double avgRating = counselorReviewMapper.getAvgRatingByCounselorId(counselorId);

        List<CounselorReviewVO> reviewVOList = reviews.stream().map(review -> {
            User user = userMapper.getById(review.getUserId());
            return CounselorReviewVO.builder()
                    .id(review.getId())
                    .userId(review.getUserId())
                    .nickname(user != null ? user.getNickname() : "匿名用户")
                    .avatar(user != null ? user.getAvatar() : null)
                    .rating(review.getRating())
                    .content(review.getContent())
                    .createTime(review.getCreateTime())
                    .build();
        }).collect(Collectors.toList());

        return ReviewListVO.builder()
                .total(total)
                .avgRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .reviews(reviewVOList)
                .build();
    }

    /**
     * 提交评价
     */
    @Override
    @Transactional
    public Long submitReview(Long userId, ReviewSubmitDTO reviewSubmitDTO) {
        appointmentService.autoCompleteExpiredAppointments(userId);

        Appointment appointment = appointmentMapper.getById(reviewSubmitDTO.getAppointmentId());
        if (appointment == null) {
            throw new BaseException("预约订单不存在");
        }
        
        if (!appointment.getUserId().equals(userId)) {
            throw new BaseException("无权评价该预约");
        }
        
        if (!"COMPLETED".equals(appointment.getStatus())) {
            throw new BaseException("只能评价已完成的预约");
        }

        int existingReviewCount = counselorReviewMapper.countByAppointmentId(reviewSubmitDTO.getAppointmentId());
        if (existingReviewCount > 0) {
            throw new BaseException("该预约已经评价过了");
        }

        Long counselorId = appointment.getCounselorId();

        CounselorReview review = CounselorReview.builder()
                .appointmentId(reviewSubmitDTO.getAppointmentId())
                .counselorId(counselorId)
                .userId(userId)
                .rating(reviewSubmitDTO.getRating())
                .content(reviewSubmitDTO.getContent())
                .createTime(LocalDateTime.now())
                .build();

        counselorReviewMapper.insert(review);

        updateCounselorRating(counselorId);

        return review.getId();
    }

    /**
     * 更新咨询师评分统计
     */
    private void updateCounselorRating(Long counselorId) {
        List<CounselorReview> reviews = counselorReviewMapper.listByCounselorId(counselorId);
        
        if (reviews.isEmpty()) {
            return;
        }

        double avgRating = reviews.stream()
                .mapToInt(CounselorReview::getRating)
                .average()
                .orElse(5.0);

        BigDecimal rating = BigDecimal.valueOf(avgRating)
                .setScale(1, RoundingMode.HALF_UP);

        CounselorProfile profile = counselorProfileMapper.getByUserId(counselorId);
        if (profile != null) {
            profile.setRating(rating);
            profile.setReviewCount(reviews.size());
            counselorProfileMapper.update(profile);
        }
    }

    /**
     * 从评价中提取标签
     */
    private List<String> extractTagsFromReviews(Long counselorId) {
        List<CounselorReview> reviews = counselorReviewMapper.getByCounselorId(counselorId, 20, 0);
        
        if (reviews.isEmpty()) {
            return Arrays.asList("暂无评价");
        }

        List<String> predefinedTags = Arrays.asList(
            "专业", "耐心", "温和", "负责", "细心", "热情", "友善", 
            "经验丰富", "善于倾听", "有帮助", "靠谱", "值得信赖"
        );
        
        List<String> matchedTags = new ArrayList<>();
        String allContent = reviews.stream()
                .map(CounselorReview::getContent)
                .filter(content -> content != null && !content.isEmpty())
                .collect(Collectors.joining(" "));

        for (String tag : predefinedTags) {
            if (allContent.contains(tag)) {
                matchedTags.add(tag);
                if (matchedTags.size() >= 3) {
                    break;
                }
            }
        }

        if (matchedTags.isEmpty()) {
            matchedTags.add("专业咨询师");
        }

        return matchedTags;
    }

    /**
     * 解析 JSON 数组
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("解析 JSON 失败: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * 计算咨询师最近可用时间
     * 检查今天、明天以及未来7天内的第一个可用时间段
     * 
     * @param counselorId 咨询师ID
     * @return 最近可用时间（格式：yyyy-MM-dd HH:mm），如果没有可用时间则返回null
     */
    private String calculateNextAvailableTime(Long counselorId) {
        try {
            LocalDate today = LocalDate.now();
            
            // 检查未来7天内的排班
            for (int i = 0; i < 7; i++) {
                LocalDate checkDate = today.plusDays(i);
                String dateStr = checkDate.toString(); // yyyy-MM-dd 格式
                
                // 获取该日期的可用时间段
                AvailableSlotsVO slotsVO = appointmentService.getAvailableSlots(counselorId, dateStr);
                
                if (slotsVO != null && slotsVO.getSlots() != null && !slotsVO.getSlots().isEmpty()) {
                    // 找到第一个 available = true 的时间段
                    for (TimeSlotVO slot : slotsVO.getSlots()) {
                        if (Boolean.TRUE.equals(slot.getAvailable())) {
                            // 返回该时间段的开始时间
                            return dateStr + " " + slot.getStartTime();
                        }
                    }
                }
            }
            
            // 未来7天内没有可用时间
            return null;
        } catch (Exception e) {
            log.error("计算咨询师 {} 的最近可用时间失败", counselorId, e);
            return null;
        }
    }

    /**
     * 生成匹配理由（增强版，包含协同过滤提示）
     */
    private String generateMatchReason(CounselorProfile profile, List<String> keywords, 
                                      boolean isUrgent, List<Long> historyIds) {
        // 【改进3】优先提示历史预约关系
        if (historyIds.contains(profile.getUserId())) {
            return "您曾预约过该咨询师，口碑良好。";
        }

        if (keywords.isEmpty()) {
            return "经验丰富，评价良好。";
        }

        List<String> specialtyList = parseJsonArray(profile.getSpecialty());
        long matchCount = keywords.stream()
                .filter(specialtyList::contains)
                .count();

        if (matchCount > 0) {
            String keywordsStr = keywords.stream()
                    .filter(specialtyList::contains)
                    .limit(2)
                    .collect(Collectors.joining("、"));
            return String.format("擅长处理%s问题，有%d年经验。", 
                keywordsStr, 
                profile.getExperienceYears() != null ? profile.getExperienceYears() : 0);
        }

        return "综合评分高，服务专业。";
    }
}
