package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目管理DTO（管理员）
 */
@Data
public class QuestionManageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 量表Key
     */
    private String scaleKey;

    /**
     * 题目列表
     */
    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem implements Serializable {
        /**
         * 题目ID（更新或删除时传入）
         */
        private Long id;

        /**
         * 题目内容
         */
        private String questionText;

        /**
         * 排序
         */
        private Integer sortOrder;

        /**
         * 选项列表
         */
        private List<QuestionOption> options;

        /**
         * 是否删除（true-删除该题目，false或null-正常处理）
         */
        private Boolean deleted;
    }

    @Data
    public static class QuestionOption implements Serializable {
        /**
         * 选项文本
         */
        private String label;

        /**
         * 选项分数
         */
        private Integer score;
    }
}

