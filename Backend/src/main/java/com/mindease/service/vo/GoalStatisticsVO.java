package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * 目标统计VO
 */
@Data
@Builder
public class GoalStatisticsVO {
    private Integer activeGoals;
    private Integer completedGoals;
    private Integer pausedGoals;
    private Integer cancelledGoals;           // 已取消目标数
    private Integer weeklyCheckIns;
    private Integer monthlyCheckIns;
    private Integer todayCheckIns;
    private Integer longestStreak;
    private Double averageStreak;             // 平均连续打卡天数
    private Map<String, Integer> categoryDistribution;  // 分类 -> 目标数
    private Map<String, Integer> difficultyDistribution; // 难度 -> 目标数
    private Double completionRate;            // 完成率百分比
}
