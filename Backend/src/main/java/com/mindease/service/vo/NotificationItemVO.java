package com.mindease.pojo.vo;

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
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createTime;
}
