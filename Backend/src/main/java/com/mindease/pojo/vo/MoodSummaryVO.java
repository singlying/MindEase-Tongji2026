package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class MoodSummaryVO {

    private BigDecimal avgScore;

    private Integer continuousDays;
}

