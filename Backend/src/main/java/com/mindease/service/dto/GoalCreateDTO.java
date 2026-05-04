package com.mindease.service.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 目标创建DTO
 */
@Data
public class GoalCreateDTO {
    private String title;
    private String description;
    private String category;       // sleep/exercise/meditation/emotion/social/custom
    private LocalDate targetDate;
    private String frequency;      // daily/weekly/monthly/custom
    private LocalTime reminderTime;
    private Integer priority;      // 优先级 1-高 2-中 3-低
    private String difficulty;     // 难度: easy/medium/hard
    private List<String> tags;     // 自定义标签
}
