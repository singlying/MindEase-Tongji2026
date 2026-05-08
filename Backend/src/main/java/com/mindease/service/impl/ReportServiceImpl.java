package com.mindease.service.impl;

import com.mindease.mapper.MoodLogMapper;
import com.mindease.pojo.entity.MoodLog;
import com.mindease.pojo.vo.EmotionReportVO;
import com.mindease.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private MoodLogMapper moodLogMapper;

    @Override
    public EmotionReportVO generateEmotionReport(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("生成情绪报告，用户ID: {}, 开始日期: {}, 结束日期: {}", userId, startDate, endDate);

        // 查询指定时间范围内的情绪日志
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<MoodLog> moodLogs = moodLogMapper.getByUserIdAndDateRange(userId, startDateTime, endDateTime);
        
        // 构建报告
        return EmotionReportVO.builder()
                .period(generatePeriodString(startDate, endDate))
                .avgScore(calculateAverageScore(moodLogs))
                .positiveRate(calculatePositiveRate(moodLogs))
                .continuousDays(calculateContinuousDays(moodLogs))
                .trendData(buildTrendData(moodLogs, startDate, endDate))
                .distribution(calculateDistribution(moodLogs))
                .recentLogs(buildRecentLogs(moodLogs))
                .aiSuggestions(generateAiSuggestions(moodLogs))
                .build();
    }

    @Override
    public EmotionReportVO generateOverallReport(Long userId) {
        log.info("生成用户整体情绪报告，用户ID: {}", userId);

        // 查询用户所有情绪日志
        List<MoodLog> allMoodLogs = getAllUserMoodLogs(userId);
        
        if (allMoodLogs.isEmpty()) {
            log.info("用户 {} 暂无情绪记录", userId);
            return buildEmptyReport(userId);
        }

        // 获取最早和最晚的记录日期
        LocalDate startDate = getEarliestLogDate(allMoodLogs);
        LocalDate endDate = getLatestLogDate(allMoodLogs);
        
        // 构建整体报告
        return EmotionReportVO.builder()
                .period(generateOverallPeriodString(startDate, endDate))
                .avgScore(calculateAverageScore(allMoodLogs))
                .positiveRate(calculatePositiveRate(allMoodLogs))
                .continuousDays(calculateContinuousDays(allMoodLogs))
                .trendData(buildOverallTrendData(allMoodLogs))
                .distribution(calculateDistribution(allMoodLogs))
                .recentLogs(buildRecentLogs(allMoodLogs))
                .aiSuggestions(generateOverallAiSuggestions(allMoodLogs))
                .build();
    }

    /**
     * 生成报告周期字符串
     */
    private String generatePeriodString(LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

        // 同一天
        if (startDate.equals(endDate)) {
            return startDate.format(monthFormatter);
        }

        // 同一个月
        if (startDate.getYear() == endDate.getYear() &&
                startDate.getMonth() == endDate.getMonth()) {
            return startDate.format(monthFormatter);
        }

        // 同一年不同月
        if (startDate.getYear() == endDate.getYear()) {
            return startDate.format(yearFormatter) + "年" +
                    startDate.getMonthValue() + "月-" +
                    endDate.getMonthValue() + "月";
        }

        // 跨年情况
        return startDate.format(yearFormatter) + "年" +
                startDate.getMonthValue() + "月-" +
                endDate.format(yearFormatter) + "年" +
                endDate.getMonthValue() + "月";
    }

    /**
     * 计算平均情绪分数
     */
    private Double calculateAverageScore(List<MoodLog> moodLogs) {
        if (moodLogs.isEmpty()) {
            return 0.0;
        }
        double avgScore = moodLogs.stream()
                .mapToInt(MoodLog::getMoodScore)
                .average()
                .orElse(0.0);
        return Math.round(avgScore * 10.0) / 10.0;
    }

    /**
     * 计算积极情绪比例（评分>=6为积极）
     */
    private Double calculatePositiveRate(List<MoodLog> moodLogs) {
        if (moodLogs.isEmpty()) {
            return 0.0;
        }
        long positiveCount = moodLogs.stream()
                .filter(log -> log.getMoodScore() >= 6)
                .count();
        double positiveRate = (double) positiveCount / moodLogs.size();
        return Math.round(positiveRate * 100.0) / 100.0;
    }

    /**
     * 计算连续记录天数
     */
    private Integer calculateContinuousDays(List<MoodLog> moodLogs) {
        if (moodLogs.isEmpty()) {
            return 0;
        }
        
        // 获取所有有记录的日期并排序
        List<LocalDate> dates = moodLogs.stream()
                .map(log -> log.getLogDate().toLocalDate())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        int maxContinuous = 0;
        int currentContinuous = 1;
        
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).minusDays(1).equals(dates.get(i - 1))) {
                currentContinuous++;
                maxContinuous = Math.max(maxContinuous, currentContinuous);
            } else {
                currentContinuous = 1;
            }
        }
        
        return Math.max(maxContinuous, currentContinuous);
    }

    /**
     * 构建趋势数据
     */
    private EmotionReportVO.TrendData buildTrendData(List<MoodLog> moodLogs, LocalDate startDate, LocalDate endDate) {
        // 按日期分组
        Map<LocalDate, List<MoodLog>> logsByDate = moodLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getLogDate().toLocalDate()));
        
        List<String> dates = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        // 遍历日期范围内的每一天
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate.format(formatter));
            
            List<MoodLog> dailyLogs = logsByDate.get(currentDate);
            if (dailyLogs != null && !dailyLogs.isEmpty()) {
                int avgScore = (int) Math.round(dailyLogs.stream()
                        .mapToInt(MoodLog::getMoodScore)
                        .average()
                        .orElse(0.0));
                scores.add(avgScore);
            } else {
                scores.add(0);
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return EmotionReportVO.TrendData.builder()
                .dates(dates)
                .scores(scores)
                .build();
    }

    /**
     * 计算情绪类型分布
     */
    private Map<String, String> calculateDistribution(List<MoodLog> moodLogs) {
        if (moodLogs.isEmpty()) {
            return Map.of("happy", "0%", "neutral", "0%", "sad", "0%");
        }
        
        // 统计每种情绪类型的数量
        Map<String, Long> typeCounts = moodLogs.stream()
                .collect(Collectors.groupingBy(
                        MoodLog::getMoodType,
                        Collectors.counting()
                ));
        
        // 转换为百分比
        Map<String, String> distribution = new HashMap<>();
        int total = moodLogs.size();
        
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            double percentage = (entry.getValue().doubleValue() / total) * 100;
            distribution.put(entry.getKey().toLowerCase(), String.format("%.0f%%", percentage));
        }
        
        return distribution;
    }

    /**
     * 构建最近的情绪日志
     */
    private List<EmotionReportVO.RecentLog> buildRecentLogs(List<MoodLog> moodLogs) {
        // 获取最近5条日志
        List<MoodLog> recentMoodLogs = moodLogs.stream()
                .sorted((a, b) -> b.getLogDate().compareTo(a.getLogDate()))
                .limit(5)
                .collect(Collectors.toList());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        return recentMoodLogs.stream()
                .map(log -> EmotionReportVO.RecentLog.builder()
                        .date(log.getLogDate().format(formatter))
                        .moodType(log.getMoodType())
                        .score(log.getMoodScore())
                        .content(log.getContent())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 生成AI建议
     */
    private List<String> generateAiSuggestions(List<MoodLog> moodLogs) {
        List<String> suggestions = new ArrayList<>();
        
        if (moodLogs.isEmpty()) {
            suggestions.add("建议开始记录情绪，了解自己的情绪变化规律");
            suggestions.add("每天花几分钟记录自己的感受，有助于情绪管理");
            return suggestions;
        }
        
        // 分析情绪数据，生成个性化建议
        double avgScore = calculateAverageScore(moodLogs);
        double positiveRate = calculatePositiveRate(moodLogs);
        
        if (avgScore < 5) {
            suggestions.add("近期情绪偏低，建议尝试运动、冥想等方式提升情绪");
            suggestions.add("可以尝试与朋友交流，分享自己的感受");
        } else if (avgScore >= 7) {
            suggestions.add("情绪状态良好，继续保持积极的生活态度");
            suggestions.add("可以尝试记录更多积极体验，增强积极情绪");
        }
        
        if (positiveRate < 0.5) {
            suggestions.add("积极情绪比例较低，建议关注生活中的积极事件");
            suggestions.add("每天记录三件感恩的事情，有助于提升幸福感");
        }
        
        // 添加一些通用建议
        suggestions.add("建议保持规律作息，充足的睡眠对情绪很重要");
        suggestions.add("适当运动可以帮助释放压力，改善情绪");
        
        // 确保至少有2条建议
        if (suggestions.size() < 2) {
            suggestions.add("建议保持运动");
            suggestions.add("睡前冥想");
        }
        
        return suggestions.stream().limit(3).collect(Collectors.toList());
    }

    /**
     * 获取用户所有情绪日志
     */
    private List<MoodLog> getAllUserMoodLogs(Long userId) {
        // 查询用户所有情绪日志，按时间倒序排列
        List<MoodLog> allLogs = moodLogMapper.getByUserIdWithPagination(userId, 1000, 0); // 假设最多1000条记录
        
        // 如果需要获取所有记录，可能需要分页查询
        Long totalCount = moodLogMapper.countByUserIdWithTotal(userId);
        if (totalCount > 1000) {
            log.warn("用户 {} 情绪记录超过1000条，仅分析最近1000条记录", userId);
        }
        
        return allLogs;
    }

    /**
     * 构建空报告
     */
    private EmotionReportVO buildEmptyReport(Long userId) {
        return EmotionReportVO.builder()
                .period("整体记录")
                .avgScore(0.0)
                .positiveRate(0.0)
                .continuousDays(0)
                .trendData(EmotionReportVO.TrendData.builder()
                        .dates(new ArrayList<>())
                        .scores(new ArrayList<>())
                        .build())
                .distribution(Map.of("暂无记录", "100%"))
                .recentLogs(new ArrayList<>())
                .aiSuggestions(Arrays.asList("建议开始记录情绪，了解自己的情绪变化规律", 
                        "每天花几分钟记录自己的感受，有助于情绪管理"))
                .build();
    }

    /**
     * 获取最早的记录日期
     */
    private LocalDate getEarliestLogDate(List<MoodLog> moodLogs) {
        return moodLogs.stream()
                .map(log -> log.getLogDate().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    /**
     * 获取最晚的记录日期
     */
    private LocalDate getLatestLogDate(List<MoodLog> moodLogs) {
        return moodLogs.stream()
                .map(log -> log.getLogDate().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    /**
     * 生成整体报告周期字符串
     */
    private String generateOverallPeriodString(LocalDate startDate, LocalDate endDate) {
        if (startDate.equals(endDate)) {
            return startDate.format(DateTimeFormatter.ofPattern("yyyy年MM月"));
        }
        
        if (startDate.getYear() == endDate.getYear()) {
            return startDate.getYear() + "年" + startDate.getMonthValue() + "月-" + endDate.getMonthValue() + "月";
        }
        
        return startDate.getYear() + "年" + startDate.getMonthValue() + "月-" + 
               endDate.getYear() + "年" + endDate.getMonthValue() + "月";
    }

    /**
     * 构建整体趋势数据（按月统计）
     */
    private EmotionReportVO.TrendData buildOverallTrendData(List<MoodLog> moodLogs) {
        // 按月份分组
        Map<String, List<MoodLog>> logsByMonth = moodLogs.stream()
                .collect(Collectors.groupingBy(log -> 
                        log.getLogDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        
        List<String> months = new ArrayList<>(logsByMonth.keySet());
        Collections.sort(months);
        
        List<Integer> monthlyAvgScores = new ArrayList<>();
        
        for (String month : months) {
            List<MoodLog> monthlyLogs = logsByMonth.get(month);
            int avgScore = (int) Math.round(monthlyLogs.stream()
                    .mapToInt(MoodLog::getMoodScore)
                    .average()
                    .orElse(0.0));
            monthlyAvgScores.add(avgScore);
        }
        
        return EmotionReportVO.TrendData.builder()
                .dates(months)
                .scores(monthlyAvgScores)
                .build();
    }

    /**
     * 生成整体AI建议
     */
    private List<String> generateOverallAiSuggestions(List<MoodLog> moodLogs) {
        List<String> suggestions = new ArrayList<>();
        
        // 基于整体数据分析
        double avgScore = calculateAverageScore(moodLogs);
        double positiveRate = calculatePositiveRate(moodLogs);
        int continuousDays = calculateContinuousDays(moodLogs);
        
        // 根据整体数据给出长期建议
        if (avgScore < 5) {
            suggestions.add("长期情绪状态偏低，建议寻求专业心理咨询帮助");
            suggestions.add("可以尝试建立规律的运动习惯，有助于改善情绪状态");
        } else if (avgScore >= 7) {
            suggestions.add("长期情绪状态良好，继续保持积极的生活方式");
            suggestions.add("可以尝试帮助他人，分享积极情绪体验");
        }
        
        if (positiveRate < 0.4) {
            suggestions.add("积极情绪比例较低，建议培养感恩心态");
            suggestions.add("每天记录生活中的小确幸，提升幸福感");
        }
        
        if (continuousDays < 7) {
            suggestions.add("情绪记录不够连续，建议养成每天记录的习惯");
        } else {
            suggestions.add("情绪记录习惯良好，继续保持");
        }
        
        // 添加通用长期建议
        suggestions.add("建议定期进行情绪回顾，了解自己的情绪变化规律");
        suggestions.add("保持社交活动，与朋友家人保持联系对情绪健康很重要");
        
        return suggestions.stream().limit(4).collect(Collectors.toList());
    }
}