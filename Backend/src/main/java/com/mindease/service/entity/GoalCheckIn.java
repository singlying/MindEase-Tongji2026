package com.mindease.service.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 目标打卡记录实体
 */
@Data
public class GoalCheckIn {
    private Long id;
    private Long goalId;
    private Long userId;
    private LocalDate checkDate;     // 打卡日期
    private String note;             // 打卡备注/感受
    private LocalDateTime createTime;
}
