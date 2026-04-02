package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class ReviewListVO {

    private Integer total;

    private BigDecimal avgRating;

    private List<CounselorReviewVO> reviews;
}

