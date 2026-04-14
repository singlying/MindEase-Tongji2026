package com.mindease.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String type;

    private String title;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;
}

