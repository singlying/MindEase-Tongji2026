package com.mindease.common.utils;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SensitiveWordFilter {

    // 敏感词库，可以从数据库或配置文件加载
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            "自杀", "自残", "自尽", "轻生", "寻死", "了结生命", "结束生命",
            "割腕", "上吊", "跳楼", "投河", "服毒", "割脉", "开煤气","想死",
            "自戕", "自绝", "自裁", "轻生念头", "不想活了", "活着没意义",
            "求死", "赴死", "自我了结", "结束自己", "放弃生命", "解脱",
            "划手", "戳伤自己", "烟头烫", "撞墙", "击打", "刀片割", "剪刀划",
            "跳桥", "跳崖", "跳江", "跳海", "烧炭", "吸煤气", "吞药", "喝农药", "刎颈",
            "买安眠药", "找绳子", "准备刀片", "攒毒药", "找悬崖",
            "想消失", "离开这个世界", "不再醒来", "睡过去", "告别世界",
            "活着太累了", "撑不下去了", "没有活下去的勇气", "了结一切", "一了百了", "摆脱困境"
    ));

    /**
     * 检测文本中是否包含敏感词
     * @param text 待检测的文本
     * @return 如果包含敏感词返回true，否则返回false
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 转换为小写进行检测，提高匹配率
        String lowerText = text.toLowerCase();

        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取检测到的第一个敏感词
     * @param text 待检测的文本
     * @return 返回检测到的第一个敏感词，如果没有则返回null
     */
    public String getFirstSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase();

        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                return word;
            }
        }

        return null;
    }
    
    /**
     * 获取文本中所有的敏感词
     * @param text 待检测的文本
     * @return 返回检测到的所有敏感词列表，如果没有则返回空列表
     */
    public List<String> getAllSensitiveWords(String text) {
        List<String> foundWords = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return foundWords;
        }

        String lowerText = text.toLowerCase();

        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                foundWords.add(word);
            }
        }

        return foundWords;
    }
}