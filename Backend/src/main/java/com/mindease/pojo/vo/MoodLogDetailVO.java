package com.mindease.pojo.vo;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MoodLogDetailVO {
    private Long id;
    private Long userId;
    private String moodType;
    private Integer moodScore;
    private String content;
    private List<String> tags;
    private String aiAnalysis;
    private LocalDateTime logDate;
    private LocalDateTime createTime;
}