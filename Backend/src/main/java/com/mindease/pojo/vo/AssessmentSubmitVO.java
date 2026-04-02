package com.mindease.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测评提交结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSubmitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 总分
     */
    private Integer totalScore;

    /**
     * 结果等级
     */
    private String resultLevel;

    /**
     * 结果描述
     */
    private String resultDesc;
}

