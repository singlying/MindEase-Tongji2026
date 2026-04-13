package com.mindease.pojo.dto;

import lombok.Data;

@Data
public class ChatMessageSendDTO {
    private String sessionId;
    private String content;
}