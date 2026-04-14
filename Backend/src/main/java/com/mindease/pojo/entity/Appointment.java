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
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private Long counselorId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private String userNote;

    private String cancelReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

