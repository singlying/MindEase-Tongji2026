package com.mindease.service.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 定时提醒任务实体
 */
@Data
public class ReminderTask {
    private Long id;
    private Long userId;
    private String title;              // 任务标题
    private String taskType;           // GOAL_CHECKIN/APPOINTMENT/CUSTOM/DAILY_SUMMARY/AI_CARE
    private Long targetId;             // 关联的目标/预约等ID
    private LocalTime remindTime;      // 提醒时间
    private String frequency;          // once/daily/weekly/monthly
    private String message;            // 提醒内容模板
    private String status;             // ACTIVE/PAUSED/CANCELLED/COMPLETED
    private LocalDateTime nextExecuteAt;  // 下次执行时间
    private LocalDateTime lastExecutedAt; // 上次实际执行时间
    private Integer executeCount;       // 累计执行次数

    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
}
