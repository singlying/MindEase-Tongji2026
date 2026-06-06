package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/**
 * 打卡结果VO
 */
@Data
@Builder
public class GoalCheckInVO {
    private Boolean success;
    private LocalDate checkDate;
    private Integer currentStreak;
    private Integer totalCheckIns;
    private String message;
}
