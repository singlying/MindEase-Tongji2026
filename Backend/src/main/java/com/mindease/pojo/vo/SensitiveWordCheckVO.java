package com.mindease.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class SensitiveWordCheckVO {
    /**
     * 是否包含敏感词
     */
    private Boolean containsSensitiveWord;
    
    /**
     * 检测到的敏感词列表
     */
    private List<String> sensitiveWords;
    
    /**
     * 原始文本
     */
    private String originalText;

}