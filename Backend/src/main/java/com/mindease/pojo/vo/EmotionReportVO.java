package com.mindease.pojo.vo;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class EmotionReportVO {

    private String period;

    private Double avgScore;

    private Double positiveRate;

    private Integer continuousDays;

    private TrendData trendData;

    private Map<String, String> distribution;

    private List<RecentLog> recentLogs;

    private List<String> aiSuggestions;

    /**
     * 趋势数据内部类
     */
    @Data
    @Builder
    public static class TrendData {
        private List<String> dates;
        private List<Integer> scores;
    }
    
    /**
     * 最近日志内部类
     */
    @Data
    @Builder
    public static class RecentLog {
        private String date;
        private String moodType;
        private Integer score;
        private String content;
    }
}