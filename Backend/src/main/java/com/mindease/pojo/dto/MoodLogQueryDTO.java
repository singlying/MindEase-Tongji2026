package com.mindease.pojo.dto;

import lombok.Data;

@Data
public class MoodLogQueryDTO {
    private Integer limit = 10;
    private Integer offset = 0;
}