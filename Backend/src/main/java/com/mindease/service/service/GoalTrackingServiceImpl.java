package com.mindease.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.exception.BaseException;
import com.mindease.mapper.GoalMapper;
import com.mindease.mapper.GoalCheckInMapper;
import com.mindease.pojo.dto.GoalCreateDTO;
import com.mindease.pojo.dto.GoalUpdateDTO;
import com.mindease.pojo.entity.Goal;
import com.mindease.pojo.entity.GoalCheckIn;
import com.mindease.pojo.vo.*;
import com.mindease.service.GoalTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 心理健康目标追踪服务实现类
 * 支持目标设定、每日打卡、进度统计、日历展示等功能
 *
 * @version 2.0 重构版本 - 枚举化、职责分离、缓存优化
 */
@Service
@Slf4j
public class GoalTrackingServiceImpl implements GoalTrackingService {

    // ============ 常量定义 ============
    private static final String REDIS_CHECKIN_PREFIX = "mindease:goal:checkin:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // 业务规则阈值
    private static final int PAUSE_RESET_STREAK_DAYS = 7;
    private static final double PROGRESS_RECALC_THRESHOLD = 5.0;
    private static final int DEFAULT_PRIORITY = 2;
    private static final String DEFAULT_DIFFICULTY = "medium";
    private static final String DEFAULT_FREQUENCY = "daily";
    private static final int MAX_RECENT_CHECKIN_DAYS = 14;

    // 目标模板库
    private static final Map<GoalCategory, List<String>> GOAL_TEMPLATES = new EnumMap<>(GoalCategory.class);

    static {
        GOAL_TEMPLATES.put(GoalCategory.SLEEP, Arrays.asList(
                "每晚23:00前入睡", "睡前30分钟不看手机", "每天睡眠不少于7小时"
        ));
        GOAL_TEMPLATES.put(GoalCategory.EXERCISE, Arrays.asList(
                "每天步行30分钟", "每周运动3次，每次30分钟以上", "每天做10分钟拉伸"
        ));
        GOAL_TEMPLATES.put(GoalCategory.MEDITATION, Arrays.asList(
                "每天冥想10分钟", "每周完成3次正念练习", "每天进行5分钟深呼吸训练"
        ));
        GOAL_TEMPLATES.put(GoalCategory.EMOTION, Arrays.asList(
                "每天记录一条情绪日记", "每周进行一次自我反思", "学会识别并命名自己的情绪"
        ));
        GOAL_TEMPLATES.put(GoalCategory.SOCIAL, Arrays.asList(
                "每周与朋友联系至少3次", "每月参加1次社交活动", "每天对一个人表达感谢"
        ));
    }

    // ============ 依赖注入 ============
    @Autowired
    private GoalMapper goalMapper;

    @Autowired
    private GoalCheckInMapper checkInMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============ 公开接口实现 ============

    @Override
    public GoalDetailVO createGoal(GoalCreateDTO goalDTO, Long userId) {
        log.info("用户 {} 创建新目标: {}", userId, goalDTO.getTitle());

        // 参数校验
        validateGoalCreate(goalDTO);

        // 使用建造者模式构建 Goal 对象
        Goal goal = GoalBuilder.fromCreateDTO(goalDTO, userId)
                .withStatus(GoalStatus.ACTIVE)
                .withDefaultStats()
                .withDefaultPriorityAndDifficulty()
                .withFrequencyOrDefault(DEFAULT_FREQUENCY)
                .withAISuggestion(generateGoalSuggestion(goalDTO.getCategory()))
                .build();

        goalMapper.insert(goal);
        log.info("目标创建成功, ID: {}", goal.getId());

        return convertToDetailVO(goal, Collections.emptyList());
    }

    @Override
    public GoalListVO getGoalList(Long userId, String status) {
        log.info("获取目标列表, userId={}, status={}", userId, status);
        List<Goal> goals = goalMapper.selectByUserIdAndStatus(userId, status);
        List<GoalSummaryVO> items = goals.stream()
                .map(this::convertToSummaryVO)
                .collect(Collectors.toList());
        return GoalListVO.builder().total(items.size()).items(items).build();
    }

    @Override
    public GoalDetailVO getGoalDetail(Long goalId, Long userId) {
        log.info("获取目标详情, goalId={}, userId={}", goalId, userId);
        Goal goal = verifyOwnership(goalId, userId);

        // 获取最近打卡记录
        LocalDate since = LocalDate.now().minusDays(MAX_RECENT_CHECKIN_DAYS - 1);
        List<GoalCheckIn> recentCheckIns = checkInMapper.selectByGoalIdSince(goalId, since);

        boolean todayChecked = isTodayChecked(goalId, userId);
        return convertToDetailVO(goal, recentCheckIns, todayChecked);
    }

    @Override
    public GoalDetailVO updateGoal(Long goalId, GoalUpdateDTO updateDTO, Long userId) {
        log.info("更新目标, goalId={}", goalId);
        Goal goal = verifyOwnership(goalId, userId);

        // 应用更新
        applyUpdate(goal, updateDTO);

        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);
        log.info("目标更新成功, goalId={}", goalId);

        return getGoalDetail(goalId, userId);
    }

    @Override
    public GoalCheckInVO checkIn(Long goalId, Long userId, String note) {
        log.info("目标打卡, goalId={}, userId={}", goalId, userId);
        Goal goal = verifyOwnership(goalId, userId);

        // 检查状态是否允许打卡
        if (!GoalStatus.ACTIVE.equals(GoalStatus.fromCode(goal.getStatus()))) {
            throw new BaseException("当前目标状态不支持打卡");
        }

        LocalDate today = LocalDate.now();

        // 今日是否已打卡
        if (isTodayChecked(goalId, userId)) {
            throw new BaseException("今日已完成打卡，无需重复操作");
        }

        // 创建打卡记录
        GoalCheckIn checkIn = new GoalCheckIn();
        checkIn.setGoalId(goalId);
        checkIn.setUserId(userId);
        checkIn.setCheckDate(today);
        checkIn.setNote(Optional.ofNullable(note).orElse("").trim());
        checkIn.setCreateTime(LocalDateTime.now());
        checkInMapper.insert(checkIn);

        // 更新目标统计（内部会更新 Redis 缓存）
        updateGoalStatsAfterCheckIn(goal, today);

        log.info("打卡成功, goalId={}, date={}", goalId, today);

        return GoalCheckInVO.builder()
                .success(true)
                .checkDate(today)
                .currentStreak(goal.getCurrentStreak())
                .totalCheckIns(goal.getTotalCheckIns())
                .message("打卡成功！坚持就是胜利 💪")
                .build();
    }

    @Override
    public Boolean pauseGoal(Long goalId, String reason, Long userId) {
        log.info("暂停目标, goalId={}", goalId);
        Goal goal = verifyOwnership(goalId, userId);

        if (!GoalStatus.ACTIVE.equals(GoalStatus.fromCode(goal.getStatus()))) {
            throw new BaseException("只有进行中的目标才能暂停");
        }

        goal.setStatus(GoalStatus.PAUSED.getCode());
        goal.setPauseReason(reason);
        goal.setPauseTime(LocalDateTime.now());
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标已暂停, goalId={}", goalId);
        return true;
    }

    @Override
    public Boolean resumeGoal(Long goalId, Long userId) {
        log.info("恢复目标, goalId={}", goalId);
        Goal goal = verifyOwnership(goalId, userId);

        if (!GoalStatus.PAUSED.equals(GoalStatus.fromCode(goal.getStatus()))) {
            throw new BaseException("只有已暂停的目标才能恢复");
        }

        // 计算暂停天数
        long pausedDays = java.time.temporal.ChronoUnit.DAYS.between(
                goal.getPauseTime().toLocalDate(), LocalDate.now()
        );

        goal.setStatus(GoalStatus.ACTIVE.getCode());
        goal.setPauseReason(null);
        goal.setPauseTime(null);

        // 如果暂停超过阈值，重置连续打卡天数
        if (pausedDays > PAUSE_RESET_STREAK_DAYS) {
            goal.setCurrentStreak(0);
            log.info("目标暂停超{}天，连续天数已重置, goalId={}", PAUSE_RESET_STREAK_DAYS, goalId);
        }

        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标已恢复, goalId={}", goalId);
        return true;
    }

    @Override
    public Boolean completeGoal(Long goalId, String summary, Long userId) {
        log.info("完成目标, goalId={}", goalId);
        Goal goal = verifyOwnership(goalId, userId);

        goal.setStatus(GoalStatus.COMPLETED.getCode());
        goal.setCompletionSummary(summary);
        goal.setCompleteTime(LocalDateTime.now());
        goal.setProgress(100.0);
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        // 记录完成成就
        logAchievement(userId, goal);

        log.info("恭喜! 目标已完成, goalId={}, 总打卡{}次", goalId, goal.getTotalCheckIns());
        return true;
    }

    @Override
    public Boolean deleteGoal(Long goalId, Long userId) {
        log.info("删除目标, goalId={}", goalId);
        Goal goal = verifyOwnership(goalId, userId);

        goal.setStatus(GoalStatus.DELETED.getCode());
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标已删除, goalId={}", goalId);
        return true;
    }

    @Override
    public Boolean cancelGoal(Long goalId, String reason, Long userId) {
        log.info("取消目标, goalId={}, reason={}", goalId, reason);
        Goal goal = verifyOwnership(goalId, userId);

        GoalStatus currentStatus = GoalStatus.fromCode(goal.getStatus());
        if (currentStatus == GoalStatus.COMPLETED || currentStatus == GoalStatus.DELETED) {
            throw new BaseException("已完成或已删除的目标不能取消");
        }

        goal.setStatus(GoalStatus.CANCELLED.getCode());
        goal.setPauseReason(reason);  // 复用字段
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标已取消, goalId={}", goalId);
        return true;
    }

    @Override
    public GoalStatisticsVO getGoalStatistics(Long userId) {
        log.info("获取目标统计数据, userId={}", userId);
        GoalStatisticsBuilder statsBuilder = new GoalStatisticsBuilder(userId);

        // 各状态数量
        for (GoalStatus status : GoalStatus.values()) {
            if (status == GoalStatus.DELETED) continue; // 已删除不统计
            int count = goalMapper.countByUserIdAndStatus(userId, status.getCode());
            statsBuilder.withStatusCount(status, count);
        }

        // 本周及本月打卡数
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        statsBuilder.withWeeklyCheckIns(checkInMapper.countByUserIdSince(userId, weekStart))
                .withMonthlyCheckIns(checkInMapper.countByUserIdSince(userId, monthStart));

        // 最长连续记录
        Integer longest = goalMapper.getMaxLongestStreak(userId);
        statsBuilder.withLongestStreak(Optional.ofNullable(longest).orElse(0));

        // 今日打卡数
        statsBuilder.withTodayCheckIns(countTodayCheckIns(userId));

        // 分类和难度分布
        statsBuilder.withCategoryDistribution(goalMapper.getCountByCategory(userId))
                .withDifficultyDistribution(goalMapper.getCountByDifficulty(userId));

        // 平均连续天数
        Double avg = goalMapper.getAvgStreak(userId);
        statsBuilder.withAverageStreak(Optional.ofNullable(avg).orElse(0.0));

        // 完成率
        int totalCreated = statsBuilder.getActiveCount() + statsBuilder.getCompletedCount() + statsBuilder.getPausedCount();
        double completionRate = totalCreated > 0 ?
                Math.round((double) statsBuilder.getCompletedCount() / totalCreated * 10000.0) / 100.0 : 0.0;
        statsBuilder.withCompletionRate(completionRate);

        return statsBuilder.build();
    }

    @Override
    public GoalCalendarVO getCheckInCalendar(Long goalId, String yearMonth, Long userId) {
        log.info("获取打卡日历, goalId={}, yearMonth={}", goalId, yearMonth);
        verifyOwnership(goalId, userId);

        LocalDate targetMonth = parseYearMonth(yearMonth);
        LocalDate monthStart = targetMonth.withDayOfMonth(1);
        LocalDate monthEnd = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());

        // 批量查询该月打卡记录
        List<GoalCheckIn> monthlyRecords = checkInMapper.selectByGoalIdBetweenDates(
                goalId, monthStart, monthEnd
        );

        Set<LocalDate> checkedDates = monthlyRecords.stream()
                .map(GoalCheckIn::getCheckDate)
                .collect(Collectors.toSet());

        // 构建日历数据
        List<Map<String, Object>> calendarDays = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= targetMonth.lengthOfMonth(); day++) {
            LocalDate date = targetMonth.withDayOfMonth(day);
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DATE_FORMATTER));
            dayData.put("dayOfMonth", day);
            dayData.put("checked", checkedDates.contains(date));
            dayData.put("isFuture", date.isAfter(today));
            dayData.put("isToday", date.isEqual(today));
            calendarDays.add(dayData);
        }

        return GoalCalendarVO.builder()
                .yearMonth(yearMonth)
                .monthName(targetMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")))
                .checkedDays(checkedDates.stream().map(d -> d.format(DATE_FORMATTER)).collect(Collectors.toList()))
                .totalCheckedDays(checkedDates.size())
                .totalDays(targetMonth.lengthOfMonth())
                .calendar(calendarDays)
                .build();
    }

    // ============ 新增或扩展接口 ============

    @Override
    public List<GoalSummaryVO> getRecentCompletedGoals(Long userId, int limit) {
        log.info("获取用户最近完成目标列表, userId={}, limit={}", userId, limit);
        List<Goal> completedGoals = goalMapper.selectRecentCompleted(userId, Math.max(limit, 1));
        return completedGoals.stream().map(this::convertToSummaryVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getGoalTemplatesByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return GOAL_TEMPLATES.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        try {
            GoalCategory cat = GoalCategory.valueOf(category.toUpperCase());
            return new ArrayList<>(GOAL_TEMPLATES.getOrDefault(cat, Collections.emptyList()));
        } catch (IllegalArgumentException e) {
            log.warn("未知的目标分类: {}", category);
            return Collections.emptyList();
        }
    }

    @Override
    public int recalculateAllGoalProgress(Long userId) {
        log.info("重新计算用户 {} 的所有目标进度数据", userId);
        List<Goal> allGoals = goalMapper.selectByUserIdAndStatus(userId, null);
        int fixedCount = 0;

        for (Goal goal : allGoals) {
            GoalStatus status = GoalStatus.fromCode(goal.getStatus());
            double expectedProgress = calculateExpectedProgress(goal, status);
            if (expectedProgress < 0) continue; // 不修正的状态

            double currentProgress = Optional.ofNullable(goal.getProgress()).orElse(0.0);
            if (Math.abs(expectedProgress - currentProgress) > PROGRESS_RECALC_THRESHOLD) {
                goal.setProgress(Math.round(expectedProgress * 100.0) / 100.0);
                goal.setUpdateTime(LocalDateTime.now());
                goalMapper.updateById(goal);
                fixedCount++;
            }
        }

        log.info("进度重算完成, 用户 {}, 共修正{}个目标", userId, fixedCount);
        return fixedCount;
    }

    // ============ 私有辅助方法 ============

    /** 校验创建参数 */
    private void validateGoalCreate(GoalCreateDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BaseException("目标标题不能为空");
        }
        if (dto.getTitle().length() > 100) {
            throw new BaseException("目标标题不能超过100字");
        }
        if (dto.getTargetDate() != null && dto.getTargetDate().isBefore(LocalDate.now())) {
            throw new BaseException("目标截止日期不能早于今天");
        }
        if (dto.getDescription() != null && dto.getDescription().length() > 2000) {
            throw new BaseException("目标描述不能超过2000字");
        }
    }

    /** 校验目标归属权和存在性，返回 Goal 对象 */
    private Goal verifyOwnership(Long goalId, Long userId) {
        Goal goal = goalMapper.selectById(goalId);
        if (goal == null || GoalStatus.DELETED.getCode().equals(goal.getStatus())) {
            throw new BaseException("目标不存在或已被删除");
        }
        if (!goal.getUserId().equals(userId)) {
            throw new BaseException("无权操作此目标");
        }
        return goal;
    }

    /** 检查今日是否已打卡（Redis + DB） */
    private boolean isTodayChecked(Long goalId, Long userId) {
        String redisKey = buildCheckinRedisKey(goalId, userId, LocalDate.now());
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            return true;
        }
        // 未命中则查 DB
        int count = checkInMapper.existsByGoalAndDate(goalId, userId, LocalDate.now());
        if (count > 0) {
            // 写入缓存，有效期 24 小时
            stringRedisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
            return true;
        }
        return false;
    }

    /** 统计用户今日总打卡数 */
    private int countTodayCheckIns(Long userId) {
        return checkInMapper.countByUserOnDate(userId, LocalDate.now());
    }

    /** 打卡后更新目标统计（包含缓存更新） */
    private void updateGoalStatsAfterCheckIn(Goal goal, LocalDate checkDate) {
        // 1. 增加总打卡次数
        goal.setTotalCheckIns(goal.getTotalCheckIns() + 1);

        // 2. 计算连续天数
        LocalDate yesterday = checkDate.minusDays(1);
        boolean wasYesterdayChecked = checkInMapper.existsByGoalAndDate(
                goal.getId(), goal.getUserId(), yesterday
        ) > 0;

        if (wasYesterdayChecked) {
            goal.setCurrentStreak(goal.getCurrentStreak() + 1);
        } else {
            goal.setCurrentStreak(1);
        }

        if (goal.getCurrentStreak() > goal.getLongestStreak()) {
            goal.setLongestStreak(goal.getCurrentStreak());
        }

        // 3. 计算进度
        goal.setProgress(calculateProgress(goal));

        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        // 4. 缓存今日打卡标记
        String redisKey = buildCheckinRedisKey(goal.getId(), goal.getUserId(), checkDate);
        stringRedisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
    }

    /** 计算目标进度百分比 */
    private double calculateProgress(Goal goal) {
        if (GoalStatus.COMPLETED.getCode().equals(goal.getStatus())) {
            return 100.0;
        }
        if (goal.getTargetDate() != null) {
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                    goal.getCreateTime().toLocalDate(), goal.getTargetDate());
            if (totalDays > 0) {
                return Math.min(100.0, (double) goal.getTotalCheckIns() / totalDays * 100.0);
            }
        }
        // 无截止日期：每2次打卡增加1%
        return Math.min(100.0, (double) goal.getTotalCheckIns() * 2.0);
    }

    /** 计算预期进度（用于重算） */
    private double calculateExpectedProgress(Goal goal, GoalStatus status) {
        if (status == GoalStatus.COMPLETED) {
            return 100.0;
        }
        if (status == GoalStatus.ACTIVE) {
            return calculateProgress(goal);
        }
        // PAUSED, CANCELLED 等保持原样，返回 -1 表示不修正
        return -1.0;
    }

    /** 生成 AI 建议 */
    private String generateGoalSuggestion(String categoryCode) {
        GoalCategory category = GoalCategory.fromCode(categoryCode);
        List<String> suggestions = GOAL_TEMPLATES.getOrDefault(category, Collections.emptyList());

        StringBuilder sb = new StringBuilder();
        sb.append("基于当前目标，建议你：\n\n");
        if (!suggestions.isEmpty()) {
            sb.append("**推荐做法**：\n");
            for (int i = 0; i < suggestions.size(); i++) {
                sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("**小贴士**：\n");
        sb.append("- 从小目标开始，逐步建立习惯\n");
        sb.append("- 设定固定的时间和地点来执行目标\n");
        sb.append("- 找一个伙伴互相监督，效果更好哦\n");
        return sb.toString();
    }

    /** 记录成就（预留扩展） */
    private void logAchievement(Long userId, Goal goal) {
        log.info("🎉 用户 {} 完成目标: [{}] 连续{}天, 共打卡{}次",
                userId, goal.getTitle(), goal.getLongestStreak(), goal.getTotalCheckIns());
        // 可扩展：写入成就表、触发通知、发放积分等
    }

    /** 应用更新 DTO 到实体 */
    private void applyUpdate(Goal goal, GoalUpdateDTO dto) {
        if (dto.getTitle() != null) {
            goal.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            goal.setDescription(dto.getDescription());
        }
        if (dto.getTargetDate() != null) {
            if (dto.getTargetDate().isBefore(LocalDate.now())) {
                throw new BaseException("截止日期不能早于今天");
            }
            goal.setTargetDate(dto.getTargetDate());
        }
        if (dto.getFrequency() != null) {
            goal.setFrequency(dto.getFrequency());
        }
        if (dto.getReminderTime() != null) {
            goal.setReminderTime(dto.getReminderTime());
        }
    }

    /** 解析年月字符串 */
    private LocalDate parseYearMonth(String yearMonth) {
        try {
            return LocalDate.parse(yearMonth + "-01", DATE_FORMATTER);
        } catch (Exception e) {
            throw new BaseException("日期格式错误，请使用 yyyy-MM 格式");
        }
    }

    /** 构建 Redis 打卡键 */
    private String buildCheckinRedisKey(Long goalId, Long userId, LocalDate date) {
        return REDIS_CHECKIN_PREFIX + goalId + ":" + userId + ":" + date.format(DATE_FORMATTER);
    }

    // ============ VO 转换方法 ============

    private GoalDetailVO convertToDetailVO(Goal goal, List<GoalCheckIn> recentCheckIns) {
        return convertToDetailVO(goal, recentCheckIns, false);
    }

    private GoalDetailVO convertToDetailVO(Goal goal, List<GoalCheckIn> recentCheckIns, boolean todayChecked) {
        List<GoalRecentCheckInVO> checkInVOs = recentCheckIns.stream()
                .map(ci -> GoalRecentCheckInVO.builder()
                        .date(ci.getCheckDate().format(DATE_FORMATTER))
                        .note(ci.getNote())
                        .build())
                .collect(Collectors.toList());

        return GoalDetailVO.builder()
                .goalId(goal.getId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .category(goal.getCategory())
                .frequency(goal.getFrequency())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .progress(goal.getProgress())
                .priority(goal.getPriority())
                .difficulty(goal.getDifficulty())
                .currentStreak(goal.getCurrentStreak())
                .longestStreak(goal.getLongestStreak())
                .totalCheckIns(goal.getTotalCheckIns())
                .aiSuggestion(goal.getAiSuggestion())
                .todayChecked(todayChecked)
                .recentCheckIns(checkInVOs)
                .createTime(goal.getCreateTime())
                .updateTime(goal.getUpdateTime())
                .build();
    }

    private GoalSummaryVO convertToSummaryVO(Goal goal) {
        return GoalSummaryVO.builder()
                .goalId(goal.getId())
                .title(goal.getTitle())
                .category(goal.getCategory())
                .status(goal.getStatus())
                .progress(goal.getProgress())
                .currentStreak(goal.getCurrentStreak())
                .totalCheckIns(goal.getTotalCheckIns())
                .targetDate(goal.getTargetDate())
                .build();
    }

    // ============ 内部枚举定义 ============

    /** 目标状态枚举 */
    public enum GoalStatus {
        ACTIVE("ACTIVE"),
        PAUSED("PAUSED"),
        COMPLETED("COMPLETED"),
        CANCELLED("CANCELLED"),
        DELETED("DELETED");

        private final String code;

        GoalStatus(String code) { this.code = code; }

        public String getCode() { return code; }

        public static GoalStatus fromCode(String code) {
            for (GoalStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new BaseException("未知的目标状态: " + code);
        }
    }

    /** 目标分类枚举 */
    public enum GoalCategory {
        SLEEP("sleep"),
        EXERCISE("exercise"),
        MEDITATION("meditation"),
        EMOTION("emotion"),
        SOCIAL("social");

        private final String code;

        GoalCategory(String code) { this.code = code; }

        public String getCode() { return code; }

        public static GoalCategory fromCode(String code) {
            for (GoalCategory cat : values()) {
                if (cat.code.equalsIgnoreCase(code)) {
                    return cat;
                }
            }
            // 默认返回 SLEEP 或抛出异常，这里简单返回 null 并让调用方处理
            return null;
        }
    }

    // ============ 内部建造者类 ============

    /** Goal 实体建造者 */
    private static class GoalBuilder {
        private final Goal goal = new Goal();

        public static GoalBuilder fromCreateDTO(GoalCreateDTO dto, Long userId) {
            GoalBuilder builder = new GoalBuilder();
            BeanUtils.copyProperties(dto, builder.goal);
            builder.goal.setUserId(userId);
            return builder;
        }

        public GoalBuilder withStatus(GoalStatus status) {
            goal.setStatus(status.getCode());
            return this;
        }

        public GoalBuilder withDefaultStats() {
            goal.setCurrentStreak(0);
            goal.setLongestStreak(0);
            goal.setTotalCheckIns(0);
            goal.setProgress(0.0);
            goal.setCreateTime(LocalDateTime.now());
            goal.setUpdateTime(LocalDateTime.now());
            return this;
        }

        public GoalBuilder withDefaultPriorityAndDifficulty() {
            if (goal.getPriority() == null) {
                goal.setPriority(DEFAULT_PRIORITY);
            }
            if (goal.getDifficulty() == null) {
                goal.setDifficulty(DEFAULT_DIFFICULTY);
            }
            return this;
        }

        public GoalBuilder withFrequencyOrDefault(String defaultFreq) {
            if (goal.getFrequency() == null) {
                goal.setFrequency(defaultFreq);
            }
            return this;
        }

        public GoalBuilder withAISuggestion(String suggestion) {
            goal.setAiSuggestion(suggestion);
            return this;
        }

        public Goal build() {
            return goal;
        }
    }

    // ============ 内部统计构建器 ============

    private static class GoalStatisticsBuilder {
        private int activeCount = 0;
        private int completedCount = 0;
        private int pausedCount = 0;
        private int cancelledCount = 0;
        private int weeklyCheckIns = 0;
        private int monthlyCheckIns = 0;
        private int todayCheckIns = 0;
        private int longestStreak = 0;
        private double averageStreak = 0.0;
        private Map<String, Integer> categoryDistribution = new HashMap<>();
        private Map<String, Integer> difficultyDistribution = new HashMap<>();
        private double completionRate = 0.0;

        public GoalStatisticsBuilder(Long userId) { /* 可初始化 */ }

        public GoalStatisticsBuilder withStatusCount(GoalStatus status, int count) {
            switch (status) {
                case ACTIVE: activeCount = count; break;
                case COMPLETED: completedCount = count; break;
                case PAUSED: pausedCount = count; break;
                case CANCELLED: cancelledCount = count; break;
                default: break;
            }
            return this;
        }

        public GoalStatisticsBuilder withWeeklyCheckIns(int count) { this.weeklyCheckIns = count; return this; }
        public GoalStatisticsBuilder withMonthlyCheckIns(int count) { this.monthlyCheckIns = count; return this; }
        public GoalStatisticsBuilder withTodayCheckIns(int count) { this.todayCheckIns = count; return this; }
        public GoalStatisticsBuilder withLongestStreak(int streak) { this.longestStreak = streak; return this; }
        public GoalStatisticsBuilder withAverageStreak(double avg) { this.averageStreak = avg; return this; }
        public GoalStatisticsBuilder withCategoryDistribution(Map<String, Integer> map) { this.categoryDistribution = map; return this; }
        public GoalStatisticsBuilder withDifficultyDistribution(Map<String, Integer> map) { this.difficultyDistribution = map; return this; }
        public GoalStatisticsBuilder withCompletionRate(double rate) { this.completionRate = rate; return this; }

        public int getActiveCount() { return activeCount; }
        public int getCompletedCount() { return completedCount; }
        public int getPausedCount() { return pausedCount; }

        public GoalStatisticsVO build() {
            return GoalStatisticsVO.builder()
                    .activeGoals(activeCount)
                    .completedGoals(completedCount)
                    .pausedGoals(pausedCount)
                    .cancelledGoals(cancelledCount)
                    .weeklyCheckIns(weeklyCheckIns)
                    .monthlyCheckIns(monthlyCheckIns)
                    .todayCheckIns(todayCheckIns)
                    .longestStreak(longestStreak)
                    .averageStreak(averageStreak)
                    .categoryDistribution(categoryDistribution)
                    .difficultyDistribution(difficultyDistribution)
                    .completionRate(completionRate)
                    .build();
        }
    }
}