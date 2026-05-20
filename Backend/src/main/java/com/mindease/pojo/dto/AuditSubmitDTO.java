package com.mindease.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AuditSubmitDTO {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotBlank(message = "资质证书URL不能为空")
    private String qualificationUrl;

    private String idCardUrl;

    // 以下信息将用于 counselor_profile 表
    private String title;
    private Integer experienceYears;
    private List<String> specialty;      // JSON 数组
    private String bio;
    private String location;
    private BigDecimal pricePerHour;
}

