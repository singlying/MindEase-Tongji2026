package com.mindease.service;

import com.mindease.pojo.vo.EmotionReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 生成情绪报告
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 情绪报告VO
     */
    EmotionReportVO generateEmotionReport(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 生成用户整体情绪报告
     * @param userId 用户ID
     * @return 情绪报告VO
     */
    EmotionReportVO generateOverallReport(Long userId);
}