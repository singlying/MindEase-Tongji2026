package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class RecommendResultVO {

    private RecommendContextVO recommendContext;

    private List<CounselorRecommendVO> counselors;
}

