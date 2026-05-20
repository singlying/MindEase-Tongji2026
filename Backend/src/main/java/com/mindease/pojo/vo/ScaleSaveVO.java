package com.mindease.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 量表保存结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 量表ID
     */
    private Long scaleId;

    /**
     * 是否为更新操作（true-更新，false-创建）
     */
    private Boolean isUpdate;
}

