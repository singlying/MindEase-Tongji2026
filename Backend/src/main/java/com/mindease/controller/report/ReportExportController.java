package com.mindease.controller.report;

import com.itextpdf.text.DocumentException;
import com.mindease.pojo.vo.EmotionReportVO;
import com.mindease.service.ReportService;
import com.mindease.common.utils.PdfExportUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 报告导出控制器
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Slf4j
public class ReportExportController {

    private final ReportService reportService;
    private final PdfExportUtil pdfExportUtil;

    /**
     * 导出报告PDF
     */
    @GetMapping("/export")
    @Operation(summary = "导出报告PDF", description = "导出用户整体情绪报告的PDF文件")
    public ResponseEntity<byte[]> exportReport(
            @Parameter(description = "导出格式，当前仅支持pdf", required = true, example = "pdf")
            @RequestParam String format,
            
            HttpServletRequest request) {

        try {
            // 验证格式参数
            if (!"pdf".equalsIgnoreCase(format)) {
                return ResponseEntity.badRequest()
                        .body("不支持的导出格式，当前仅支持pdf".getBytes());
            }

            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("用户未登录或token无效".getBytes());
            }

            // 生成用户整体情绪报告
            EmotionReportVO report = reportService.generateOverallReport(userId);
            
            // 生成PDF
            byte[] pdfBytes = pdfExportUtil.generateEmotionReportPdf(report);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // 使用URL编码处理中文文件名
            String fileName = generateFileName(report.getPeriod()) + ".pdf";
            try {
                String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
                headers.setContentDispositionFormData("attachment", encodedFileName);
            } catch (Exception e) {
                // 如果编码失败，使用默认文件名
                headers.setContentDispositionFormData("attachment", "emotion_report.pdf");
            }
            headers.setContentLength(pdfBytes.length);

            log.info("用户 {} 成功导出整体情绪报告PDF", userId);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (DocumentException | IOException e) {
            log.error("生成PDF报告失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("生成PDF报告失败".getBytes());
        } catch (Exception e) {
            log.error("导出报告失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("导出报告失败".getBytes());
        }
    }

    /**
     * 咨询师查看用户的情绪档案
     */
    @GetMapping("/export/{userId}")
    public ResponseEntity<byte[]> exportUserReport(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            
            @Parameter(description = "导出格式，当前仅支持pdf", required = true, example = "pdf")
            @RequestParam String format,
            
            HttpServletRequest request) {

        try {
            // 验证格式参数
            if (!"pdf".equalsIgnoreCase(format)) {
                return ResponseEntity.badRequest()
                        .body("不支持的导出格式，当前仅支持pdf".getBytes());
            }

            // 获取咨询师ID
            Long counselorId = (Long) request.getAttribute("userId");
            if (counselorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("咨询师未登录或token无效".getBytes());
            }

            // 生成用户整体情绪报告
            EmotionReportVO report = reportService.generateOverallReport(userId);
            
            // 生成PDF
            byte[] pdfBytes = pdfExportUtil.generateEmotionReportPdf(report);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // 使用URL编码处理中文文件名
            String fileName = "用户情绪档案_" + userId + "_" + generateFileName(report.getPeriod()) + ".pdf";
            try {
                String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
                headers.setContentDispositionFormData("attachment", encodedFileName);
            } catch (Exception e) {
                // 如果编码失败，使用默认文件名
                headers.setContentDispositionFormData("attachment", "user_emotion_report.pdf");
            }
            headers.setContentLength(pdfBytes.length);

            log.info("咨询师 {} 成功查看用户 {} 的情绪档案PDF", counselorId, userId);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (DocumentException | IOException e) {
            log.error("生成PDF报告失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("生成PDF报告失败".getBytes());
        } catch (Exception e) {
            log.error("导出报告失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("导出报告失败".getBytes());
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String period) {
        // 将周期字符串转换为文件名友好的格式
        String fileName = period.replace("-", "至")
                               .replace("年", "")
                               .replace("月", "");
        return "情绪报告_" + fileName;
    }
}