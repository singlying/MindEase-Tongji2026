package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class CounselorRecommendVO {

    private Long id;

    private String realName;

    private String avatar;

    private String title;

    private Integer experienceYears;

    private List<String> specialty;

    private BigDecimal rating;

    private BigDecimal pricePerHour;

    private String location;

    private String nextAvailableTime;

    private String matchReason;

    private List<String> tags;
}

