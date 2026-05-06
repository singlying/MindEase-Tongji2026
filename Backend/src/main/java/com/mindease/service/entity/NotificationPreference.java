package com.mindease.service.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 通知偏好设置实体
 */
@Data
public class NotificationPreference {
    private Long id;
    private Long userId;

    // 分类开关
    private Boolean enableSystemNotify;       // 系统通知
    private Boolean enableAppointmentNotify;  // 预约通知
    private Boolean enableGoalRemind;         // 目标提醒
    private Boolean enableAiCare;             // AI关怀推送
    private Boolean enableCommunityNotify;    // 社区互动通知

    // 时间控制
    private LocalTime quietStartTime;          // 免打扰开始
    private LocalTime quietEndTime;            // 免打扰结束
    private LocalDateTime mutedUntil;          // 全局静音截止
    private LocalTime dailySummaryTime;        // 每日总结推送时间
    private Integer maxDailyPushCount;         // 每日最大推送数量限制，0表示不限制

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
