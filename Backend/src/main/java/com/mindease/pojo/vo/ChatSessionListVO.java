package com.mindease.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatSessionListVO {
    private Integer total;
    private List<ChatSessionVO> sessions;
}