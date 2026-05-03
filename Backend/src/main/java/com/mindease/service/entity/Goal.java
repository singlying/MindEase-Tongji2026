package com.mindease.pojo.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 心理健康目标实体
 */
@Data
public class Goal {
    private Long id;
    private Long userId;
    private String title;             // 目标标题，如"每天冥想10分钟"
    private String description;       // 详细描述
    private String category;          // sleep/exercise/meditation/emotion/social/custom
    private String frequency;         // daily/weekly/monthly/custom
    private LocalDate targetDate;     // 目标截止日期
    private String status;            // ACTIVE/COMPLETED/PAUSED/CANCELLED/DELETED
    private Double progress;          // 进度百分比 0.0-100.0
    private Integer currentStreak;    // 当前连续天数
    private Integer longestStreak;    // 历史最长连续天数
    private Integer totalCheckIns;    // 累计打卡次数
    private Integer priority;         // 优先级 1-高 2-中 3-低
    private String difficulty;        // 难度等级: easy/medium/hard
    private String aiSuggestion;      // AI生成的建议（首次创建时生成）
    private LocalTime reminderTime;   // 每日提醒时间

    // 状态变更记录
    private String pauseReason;       // 暂停原因
    private LocalDateTime pauseTime;  // 暂停时间
    private LocalDateTime completeTime;  // 完成时间
    private String completionSummary;  // 完成总结

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
