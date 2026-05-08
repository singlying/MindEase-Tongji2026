package com.mindease.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 量表题目实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 所属量表Key
     */
    private String scaleKey;

    /**
     * 题目内容
     */
    private String questionText;

    /**
     * 选项配置JSON
     */
    private String options;

    /**
     * 排序
     */
    private Integer sortOrder;
}

