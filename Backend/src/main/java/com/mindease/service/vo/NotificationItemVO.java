package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知列表项VO
 */
@Data
@Builder
public class NotificationItemVO {
    private Long notificationId;
    private String type;
    private String iconType;          // 图标类型标识: bell/star/calendar/etc.
    private String title;
    private String summary;           // 内容摘要（截断版）
    private String content;
    private String actionUrl;         // 点击跳转地址
    private Boolean isRead;
    private LocalDateTime expireAt;   // 过期时间
    private LocalDateTime createTime;
}
