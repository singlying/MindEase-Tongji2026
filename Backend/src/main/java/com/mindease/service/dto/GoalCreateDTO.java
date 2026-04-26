package com.mindease.pojo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

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
}
