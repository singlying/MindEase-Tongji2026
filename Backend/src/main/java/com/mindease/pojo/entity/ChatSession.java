package com.mindease.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSession {
    private Long id;
    private String sessionId;
    private Long userId;
    private String sessionTitle;
    private LocalDateTime createTime;
}