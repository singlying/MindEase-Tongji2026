package com.mindease.controller.report;

import com.mindease.pojo.vo.EmotionReportVO;
import com.mindease.service.ReportService;
import com.mindease.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;

/**
 * 报告控制器
 */
@RestController
@RequestMapping("/report")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 生成情绪报告
     *
     * @param startDate 开始日期 (格式: yyyy-MM-dd)
     * @param endDate 结束日期 (格式: yyyy-MM-dd)
     * @param request HTTP请求
     * @return 情绪报告
     */
    @GetMapping("/emotion")
    public Result<EmotionReportVO> generateEmotionReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletRequest request) {

        log.info("生成情绪报告，开始日期: {}, 结束日期: {}", startDate, endDate);

        // 获取当前用户ID
        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);

        // 参数验证
        if (startDate.isAfter(endDate)) {
            return Result.error("开始日期不能晚于结束日期");
        }

        if (startDate.isAfter(LocalDate.now())) {
            return Result.error("开始日期不能晚于当前日期");
        }

        // 生成情绪报告
        EmotionReportVO report = reportService.generateEmotionReport(userId, startDate, endDate);

        return Result.success(report);
    }
}