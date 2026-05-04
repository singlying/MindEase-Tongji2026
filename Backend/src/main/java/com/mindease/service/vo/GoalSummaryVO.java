package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/**
 * 目标摘要VO（列表项）
 */
@Data
@Builder
public class GoalSummaryVO {
    private Long goalId;
    private String title;
    private String category;
    private String status;
    private Double progress;
    private Integer currentStreak;
    private Integer totalCheckIns;
    private LocalDate targetDate;
}
