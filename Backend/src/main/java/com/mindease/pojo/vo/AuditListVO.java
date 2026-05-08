package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AuditListVO {

    private Integer total;

    private List<AuditListItemVO> list;
}

