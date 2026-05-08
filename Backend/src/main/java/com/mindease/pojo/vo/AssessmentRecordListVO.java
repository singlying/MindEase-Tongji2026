package com.mindease.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 测评记录列表VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRecordListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录列表
     */
    private List<RecordItem> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordItem implements Serializable {
        /**
         * 记录ID
         */
        private Long id;

        /**
         * 量表Key
         */
        private String scaleKey;

        /**
         * 量表标题
         */
        private String scaleTitle;

        /**
         * 总分
         */
        private Integer totalScore;

        /**
         * 结果等级
         */
        private String resultLevel;

        /**
         * 创建时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
    }
}

