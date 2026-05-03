package com.mindease.pojo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知模板DTO
 */
@Data
public class NotificationTemplateDTO {
    private String type;           // SYSTEM/APPOINTMENT/GOAL/COMMUNITY/AI_CARE
    private String title;
    private String content;
    private Map<String, Object> extraData;  // 额外数据（如预约ID、目标ID等）
    private String actionUrl;      // 点击跳转地址
    private LocalDateTime expireAt; // 过期时间，过期后不再展示
    private Integer priority;       // 推送优先级: 0-普通 1-高优 2-紧急
    private String targetRole;      // 目标角色过滤: ALL/USER/COUNSELOR/ADMIN
}
