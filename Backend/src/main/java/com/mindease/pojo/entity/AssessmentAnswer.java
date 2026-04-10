package com.mindease.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测评答案详情实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAnswer implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联记录ID
     */
    private Long recordId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 选项快照
     */
    private String answerText;
}

