package com.mindease.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 心理量表定义实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentScale implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 量表唯一标识(如gad-7)
     */
    private String scaleKey;

    /**
     * 量表标题
     */
    private String title;

    /**
     * 量表封面图片URL
     */
    private String coverUrl;

    /**
     * 量表描述/引导语
     */
    private String description;

    /**
     * 评分规则JSON数组: [{"min":0,"max":5,"level":"正常","desc":"建议..."}]
     */
    private String scoringRules;

    /**
     * 状态: active上架, inactive下架
     */
    private String status;
}

