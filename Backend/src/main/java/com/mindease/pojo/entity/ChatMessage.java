package com.mindease.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private String sessionId;
    private Long userId;
    private String messageRole;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}