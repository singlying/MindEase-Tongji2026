package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 测评提交DTO
 */
@Data
public class AssessmentSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 量表Key
     */
    private String scaleKey;

    /**
     * 答案列表
     */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem implements Serializable {
        /**
         * 题目ID
         */
        private Long questionId;

        /**
         * 得分
         */
        private Integer score;
    }
}

