package com.mindease.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatHistoryVO {
    private String sessionId;
    private List<ChatMessageVO> messages;
}