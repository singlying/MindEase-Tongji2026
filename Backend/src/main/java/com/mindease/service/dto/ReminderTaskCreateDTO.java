package com.mindease.pojo.dto;

import lombok.Data;
import java.time.LocalTime;

/**
 * 提醒任务创建DTO
 */
@Data
public class ReminderTaskCreateDTO {
    private String title;           // 任务标题
    private String taskType;        // GOAL_CHECKIN/APPOINTMENT/CUSTOM/DAILY_SUMMARY/AI_CARE
    private Long targetId;          // 关联的目标/预约等ID（可选）
    private LocalTime remindTime;   // 提醒时间
    private String message;         // 提醒内容
    private String frequency;       // once/daily/weekly/monthly
}
