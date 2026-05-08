package com.mindease.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 量表详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 量表ID
     */
    private Long id;

    /**
     * 量表Key
     */
    private String scaleKey;

    /**
     * 量表标题
     */
    private String title;

    /**
     * 量表描述
     */
    private String description;

    /**
     * 题目列表
     */
    private List<QuestionItem> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem implements Serializable {
        /**
         * 题目ID
         */
        private Long id;

        /**
         * 题目内容
         */
        private String text;

        /**
         * 选项列表
         */
        private List<OptionItem> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem implements Serializable {
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

