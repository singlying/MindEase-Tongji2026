package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 提醒任务VO
 */
@Data
@Builder
public class ReminderTaskVO {
    private Long taskId;
    private String title;
    private String taskType;       // GOAL_CHECKIN/APPOINTMENT/CUSTOM/DAILY_SUMMARY/AI_CARE
    private Long targetId;         // 关联的目标/预约等ID
    private LocalTime remindTime;
    private String frequency;      // once/daily/weekly/monthly
    private String status;         // ACTIVE/PAUSED/CANCELLED/COMPLETED
    private LocalDateTime nextExecuteAt;
    private String message;
    private LocalDateTime createTime;
}
