package com.mindease.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class AuditListItemVO {

    private Long auditId;

    private Long userId;

    private String username;

    private String realName;

    private String qualificationUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;
    
    private String title;                    // 职称
    
    private Integer experienceYears;          // 从业年限
    
    private String bio;                       // 个人简介
    
    private String location;                  // 所在地区
    
    private BigDecimal pricePerHour;         // 期望价格/小时
    
    private List<String> specialty;           // 专长领域
}

