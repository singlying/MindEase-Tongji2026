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
 * 测评记录详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentRecordDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 结果描述
     */
    private String resultDesc;

    /**
     * 答案详情
     */
    private List<AnswerDetail> answersDetail;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail implements Serializable {
        /**
         * 题目ID
         */
        private Long questionId;

        /**
         * 题目内容
         */
        private String questionText;

        /**
         * 得分
         */
        private Integer score;

        /**
         * 答案文本
         */
        private String answerText;
    }
}

