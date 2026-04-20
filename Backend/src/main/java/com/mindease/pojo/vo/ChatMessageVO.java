package com.mindease.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private String sender;
    private String content;
    private LocalDateTime createTime;
}