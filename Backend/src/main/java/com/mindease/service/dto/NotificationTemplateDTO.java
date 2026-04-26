package com.mindease.pojo.dto;

import lombok.Data;
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
}
