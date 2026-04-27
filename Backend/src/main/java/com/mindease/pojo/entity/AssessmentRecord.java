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
public class AssessmentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String scaleKey;

    private Integer totalScore;

    private String resultLevel;

    private String resultDesc;

    private LocalDateTime createTime;
}

