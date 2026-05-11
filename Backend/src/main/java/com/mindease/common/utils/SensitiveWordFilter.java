package com.mindease.common.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class SensitiveWordFilter {

    private static final Set<String> SENSITIVE_WORDS = new LinkedHashSet<>(Arrays.asList(
            "自杀", "自残", "自尽", "轻生", "寻死", "想死", "求死", "赴死",
            "结束生命", "结束自己", "放弃生命", "不想活了", "活着没意义",
            "离开这个世界", "不再醒来", "睡过去", "告别世界",
            "活着太累了", "撑不下去了", "没有活下去的勇气", "一了百了",
            "解脱", "摆脱困境", "伤害自己", "弄伤自己",
            "割腕", "划手", "上吊", "跳楼", "跳桥", "跳崖", "跳河", "跳海",
            "投河", "服毒", "吞药", "喝农药", "开煤气", "烧炭",
            "买安眠药", "找绳子", "准备刀片", "攒毒药", "找悬崖"
    ));

    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public String getFirstSensitiveWord(String text) {
        if (text == null || text.isBlank()) {
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

    public List<String> getAllSensitiveWords(String text) {
        List<String> foundWords = new ArrayList<>();
        if (text == null || text.isBlank()) {
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
