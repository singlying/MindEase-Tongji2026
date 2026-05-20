package com.mindease.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MoodLogDTO {
    
    private String moodType;
    
    private Integer moodScore;
    
    private String content;
    
    private List<String> tags;

    private LocalDateTime logDate;
}