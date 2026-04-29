package com.mindease.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselorProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;  // 主键，关联 sys_user.id

    private String realName;

    private String title;

    private Integer experienceYears;

    private String specialty;  // JSON 字符串

    private String bio;

    private String qualificationUrl;

    private String location;

    private String workSchedule;  // 排班配置 JSON 字符串

    private BigDecimal pricePerHour;

    private BigDecimal rating;

    private Integer reviewCount;
}

