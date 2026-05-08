package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RecommendStatusVO {

    private Boolean hasAssessment;

    private Boolean hasMoodLog;

    private String lastAssessmentLevel;

    private Boolean recommendationReady;
}

