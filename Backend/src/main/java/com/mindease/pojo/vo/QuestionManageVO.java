package com.mindease.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 题目管理结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionManageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 处理的题目数量
     */
    private Integer count;
}

