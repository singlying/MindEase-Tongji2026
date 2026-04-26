package com.mindease.pojo.vo;

import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class MoodTrendVO {
    private List<String> dates;
    private List<Integer> scores;
    private Double avgScore;
    private Double positiveRate;
    private Integer continuousDays;
}