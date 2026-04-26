package com.mindease.pojo.vo;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class MoodLogItemVO {
    private Long id;
    private LocalDateTime logDate;
    private String moodType;
    private Integer moodScore;
    private String content;
    private String emoji;
}