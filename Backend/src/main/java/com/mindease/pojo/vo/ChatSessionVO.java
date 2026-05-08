package com.mindease.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {
    private String sessionId;
    private String sessionTitle;
    private LocalDateTime createTime;
}