package com.mindease.pojo.vo;

import lombok.Data;
import lombok.Builder;

import java.util.Map;

@Data
@Builder
public class MoodStatisticsVO {
    private Map<String, String> distribution;
    private Integer totalLogs;
    private Double avgScore;
}