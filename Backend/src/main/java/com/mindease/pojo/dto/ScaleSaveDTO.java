package com.mindease.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 量表保存DTO（管理员）
 */
@Data
public class ScaleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID（更新时传入）
     */
    private Long id;

    /**
     * 量表唯一标识
     */
    private String scaleKey;

    /**
     * 量表标题
     */
    private String title;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 量表描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 评分规则
     */
    private List<ScoringRule> scoringRules;

    @Data
    public static class ScoringRule implements Serializable {
        /**
         * 最小分
         */
        private Integer min;

        /**
         * 最大分
         */
        private Integer max;

        /**
         * 等级
         */
        private String level;

        /**
         * 描述
         */
        private String desc;
    }
}

