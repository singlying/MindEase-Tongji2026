package com.mindease.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounselorAuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String realName;

    private String qualificationUrl;

    private String idCardUrl;

    private String status;

    private Long auditorId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime createTime;
}

