package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
public class CounselorDetailVO {

    private Long id;

    private String realName;

    private String avatar;

    private String title;

    private Integer experienceYears;

    private List<String> specialty;

    private String bio;

    private String qualificationUrl;

    private BigDecimal rating;

    private Integer reviewCount;

    private BigDecimal pricePerHour;

    private String location;

    private Boolean isOnline;

    private List<String> tags;
}

