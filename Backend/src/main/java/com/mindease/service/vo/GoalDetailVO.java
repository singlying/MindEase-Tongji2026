package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 目标详情VO
 */
@Data
@Builder
public class GoalDetailVO {
    private Long goalId;
    private String title;
    private String description;
    private String category;       // sleep/exercise/meditation/emotion/social
    private String frequency;      // daily/weekly/custom
    private LocalDate targetDate;
    private String status;         // ACTIVE/COMPLETED/PAUSED/CANCELLED
    private Double progress;       // 0.0 - 100.0
    private Integer priority;      // 目标优先级 1-高 2-中 3-低
    private String difficulty;     // 难度: easy/medium/hard
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalCheckIns;
    private String aiSuggestion;
    private Boolean todayChecked;
    private List<GoalRecentCheckInVO> recentCheckIns;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
