package com.mindease.pojo.vo;

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
    private Integer weeklyCheckIns;
    private Integer monthlyCheckIns;
    private Integer todayCheckIns;
    private Integer longestStreak;
    private Map<String, Integer> categoryDistribution;  // 分类 -> 目标数
    private Double completionRate;  // 完成率百分比
}
