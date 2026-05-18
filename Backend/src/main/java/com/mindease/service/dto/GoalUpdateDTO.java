package com.mindease.pojo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 目标更新DTO
 */
@Data
public class GoalUpdateDTO {
    private String title;
    private String description;
    private LocalDate targetDate;
    private String frequency;
    private LocalTime reminderTime;
}
