package com.mindease.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditProcessDTO {

    @NotNull(message = "审核ID不能为空")
    private Long auditId;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private String remark;
}

