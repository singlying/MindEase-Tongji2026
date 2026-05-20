package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知VO
 */
@Data
@Builder
public class NotificationVO {
    private Long notificationId;
    private String type;           // SYSTEM/APPOINTMENT/GOAL/COMMUNITY/AI_CARE
    private String title;
    private String content;
    private String actionUrl;      // 点击跳转地址
    private Map<String, Object> extraData;
    private Boolean isRead;
    private LocalDateTime createTime;
}
