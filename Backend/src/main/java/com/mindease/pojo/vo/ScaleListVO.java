package com.mindease.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 量表列表VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 量表列表
     */
    private List<ScaleItem> scales;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaleItem implements Serializable {
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
    }
}

