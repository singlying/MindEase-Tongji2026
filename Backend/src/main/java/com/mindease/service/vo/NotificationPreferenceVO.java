package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 通知偏好设置VO
 */
@Data
@Builder
public class NotificationPreferenceVO {
    private Boolean enableSystemNotify;      // 系统通知开关
    private Boolean enableAppointmentNotify; // 预约相关通知
    private Boolean enableGoalRemind;        // 目标打卡提醒
    private Boolean enableAiCare;            // AI关怀推送
    private LocalTime quietStartTime;        // 免打扰开始时间
    private LocalTime quietEndTime;          // 免打扰结束时间
    private LocalDateTime mutedUntil;        // 全局静音截止时间（null表示不静音）
    private LocalTime dailySummaryTime;      // 每日总结推送时间
}
