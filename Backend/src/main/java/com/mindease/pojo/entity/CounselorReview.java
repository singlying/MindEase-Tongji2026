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
public class CounselorReview implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long appointmentId;

    private Long counselorId;

    private Long userId;

    private Integer rating;

    private String content;

    private LocalDateTime createTime;
}

