package com.mindease.common.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.mindease.pojo.vo.EmotionReportVO;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PdfExportUtil {

    // 加载中文字体
    private static com.itextpdf.text.Font getChineseFont(int size, int style) {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new com.itextpdf.text.Font(baseFont, size, style);
        } catch (DocumentException | IOException e) {
            log.error("加载中文字体失败", e);
            return new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, size, style);
        }
    }

    private static final com.itextpdf.text.Font TITLE_FONT = getChineseFont(18, com.itextpdf.text.Font.BOLD);
    private static final com.itextpdf.text.Font SUBTITLE_FONT = getChineseFont(14, com.itextpdf.text.Font.BOLD);
    private static final com.itextpdf.text.Font NORMAL_FONT = getChineseFont(12, com.itextpdf.text.Font.NORMAL);
    private static final com.itextpdf.text.Font SMALL_FONT = getChineseFont(10, com.itextpdf.text.Font.NORMAL);

    /**
     * 生成情绪报告PDF
     */
    public byte[] generateEmotionReportPdf(EmotionReportVO report) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        document.open();

        // 添加标题
        addTitle(document, "情绪分析报告");
        
        // 添加报告基本信息
        addReportInfo(document, report);
        
        // 添加统计数据
        addStatistics(document, report);
        
        // 添加趋势分析
        addTrendAnalysis(document, report);
        
        // 添加情绪分布
        addDistribution(document, report);
        
        // 添加最近记录
        addRecentLogs(document, report);
        
        // 添加AI建议
        addAiSuggestions(document, report);
        
        document.close();
        
        return outputStream.toByteArray();
    }

    /**
     * 添加标题
     */
    private void addTitle(Document document, String title) throws DocumentException {
        Paragraph titleParagraph = new Paragraph(title, TITLE_FONT);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);
    }

    /**
     * 添加报告基本信息
     */
    private void addReportInfo(Document document, EmotionReportVO report) throws DocumentException {
        Paragraph infoParagraph = new Paragraph();
        infoParagraph.add(new Chunk("报告周期: ", SUBTITLE_FONT));
        infoParagraph.add(new Chunk(report.getPeriod(), NORMAL_FONT));
        infoParagraph.add(Chunk.NEWLINE);
        infoParagraph.add(new Chunk("生成时间: ", SUBTITLE_FONT));
        infoParagraph.add(new Chunk(java.time.LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), NORMAL_FONT));
        infoParagraph.setSpacingAfter(15);
        document.add(infoParagraph);
    }

    /**
     * 添加统计数据
     */
    private void addStatistics(Document document, EmotionReportVO report) throws DocumentException {
        Paragraph statsParagraph = new Paragraph("情绪统计", SUBTITLE_FONT);
        statsParagraph.setSpacingAfter(10);
        document.add(statsParagraph);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);

        // 表头
        table.addCell(createCell("平均情绪分数", true));
        table.addCell(createCell("积极情绪比例", true));
        table.addCell(createCell("连续记录天数", true));

        // 数据
        table.addCell(createCell(String.format("%.1f", report.getAvgScore()), false));
        table.addCell(createCell(String.format("%.0f%%", report.getPositiveRate() * 100), false));
        table.addCell(createCell(report.getContinuousDays().toString(), false));

        document.add(table);
    }

    /**
     * 添加趋势分析
     */
    private void addTrendAnalysis(Document document, EmotionReportVO report) throws DocumentException, IOException {
        Paragraph trendParagraph = new Paragraph("情绪趋势", SUBTITLE_FONT);
        trendParagraph.setSpacingAfter(10);
        document.add(trendParagraph);

        // 生成情绪趋势图
        if (report.getTrendData() != null) {
            byte[] chartBytes = generateTrendChart(report.getTrendData());
            if (chartBytes != null) {
                com.itextpdf.text.Image chartImage = com.itextpdf.text.Image.getInstance(chartBytes);
                chartImage.setAlignment(Element.ALIGN_CENTER);
                chartImage.scaleToFit(500, 300);
                document.add(chartImage);
            }
        }

        Paragraph description = new Paragraph(
            "在报告周期内，您的情绪整体保持稳定。平均分数为 " + 
            String.format("%.1f", report.getAvgScore()) + " 分，属于正常范围。", 
            NORMAL_FONT
        );
        description.setSpacingAfter(15);
        document.add(description);
    }

    /**
     * 添加情绪分布
     */
    private void addDistribution(Document document, EmotionReportVO report) throws DocumentException, IOException {
        Paragraph distributionParagraph = new Paragraph("情绪类型分布", SUBTITLE_FONT);
        distributionParagraph.setSpacingAfter(10);
        document.add(distributionParagraph);

        // 添加详细数据表格
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);

        // 表头
        table.addCell(createCell("情绪类型", true));
        table.addCell(createCell("占比", true));

        // 数据
        if (report.getDistribution() != null) {
            report.getDistribution().forEach((type, percentage) -> {
                table.addCell(createCell(type, false));
                table.addCell(createCell(percentage, false));
            });
        }

        document.add(table);

        // 生成情绪分布饼图
        if (report.getDistribution() != null && !report.getDistribution().isEmpty()) {
            byte[] chartBytes = generatePieChart(report.getDistribution());
            if (chartBytes != null) {
                com.itextpdf.text.Image chartImage = com.itextpdf.text.Image.getInstance(chartBytes);
                chartImage.setAlignment(Element.ALIGN_CENTER);
                chartImage.scaleToFit(400, 300);
                document.add(chartImage);
            }
        }
    }

    /**
     * 生成情绪趋势图
     */
    private byte[] generateTrendChart(EmotionReportVO.TrendData trendData) throws IOException {
        if (trendData == null || trendData.getDates() == null || trendData.getScores() == null || trendData.getDates().size() < 2) {
            return null;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<String> dates = trendData.getDates();
        List<Integer> scores = trendData.getScores();

        for (int i = 0; i < dates.size() && i < scores.size(); i++) {
            dataset.addValue(scores.get(i), "月平均分数", dates.get(i));
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "情绪趋势",
                "日期",
                "月平均分数",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        // 设置图表样式
        CategoryPlot plot = chart.getCategoryPlot();
        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        // 显示连线
        renderer.setSeriesLinesVisible(0, true);
        // 同时显示数据点
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0));
        renderer.setSeriesOutlinePaint(0, Color.BLUE);
        renderer.setSeriesFillPaint(0, Color.WHITE);

        // 设置图表标题字体
        chart.getTitle().setFont(new java.awt.Font("SimHei", java.awt.Font.BOLD, 14));
        
        // 设置坐标轴
        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setLabelFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 12));
        xAxis.setTickLabelFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 10));

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setLabelFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 12));
        yAxis.setRange(0, 10);
        yAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(1));

        // 设置图例字体
        chart.getLegend().setItemFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 12));
        
        // 保存图表到字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 600, 400);
        return baos.toByteArray();
    }

    /**
     * 生成情绪分布饼图
     */
    private byte[] generatePieChart(Map<String, String> distribution) throws IOException {
        DefaultPieDataset dataset = new DefaultPieDataset();

        for (Map.Entry<String, String> entry : distribution.entrySet()) {
            String type = entry.getKey();
            String percentageStr = entry.getValue().replace("%", "");
            try {
                double percentage = Double.parseDouble(percentageStr);
                dataset.setValue(type, percentage);
            } catch (NumberFormatException e) {
                log.warn("无法解析百分比: {}", percentageStr);
            }
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "情绪类型分布",
                dataset,
                true,
                true,
                false
        );

        // 设置图表样式
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 12));
        plot.setLabelGenerator(new org.jfree.chart.labels.StandardPieSectionLabelGenerator("{0}: {1}%"));
        // 设置图例字体
        chart.getLegend().setItemFont(new java.awt.Font("SimHei", java.awt.Font.PLAIN, 12));
        // 设置标题字体
        chart.getTitle().setFont(new java.awt.Font("SimHei", java.awt.Font.BOLD, 14));

        // 保存图表到字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 500, 350);
        return baos.toByteArray();
    }

    /**
     * 添加最近记录
     */
    private void addRecentLogs(Document document, EmotionReportVO report) throws DocumentException {
        Paragraph logsParagraph = new Paragraph("最近情绪记录", SUBTITLE_FONT);
        logsParagraph.setSpacingAfter(10);
        document.add(logsParagraph);

        if (report.getRecentLogs() != null && !report.getRecentLogs().isEmpty()) {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);

            // 表头
            table.addCell(createCell("日期", true));
            table.addCell(createCell("情绪类型", true));
            table.addCell(createCell("分数", true));
            table.addCell(createCell("内容", true));

            // 数据
            report.getRecentLogs().forEach(log -> {
                table.addCell(createCell(log.getDate(), false));
                table.addCell(createCell(log.getMoodType(), false));
                table.addCell(createCell(log.getScore().toString(), false));
                table.addCell(createCell(log.getContent() != null ? log.getContent() : "", false));
            });

            document.add(table);
        } else {
            Paragraph noData = new Paragraph("暂无记录", NORMAL_FONT);
            noData.setSpacingAfter(15);
            document.add(noData);
        }
    }

    /**
     * 添加AI建议
     */
    private void addAiSuggestions(Document document, EmotionReportVO report) throws DocumentException {
        Paragraph suggestionsParagraph = new Paragraph("AI建议", SUBTITLE_FONT);
        suggestionsParagraph.setSpacingAfter(10);
        document.add(suggestionsParagraph);

        if (report.getAiSuggestions() != null && !report.getAiSuggestions().isEmpty()) {
            com.itextpdf.text.List list = new com.itextpdf.text.List(com.itextpdf.text.List.ORDERED);
            list.setIndentationLeft(20);
            
            report.getAiSuggestions().forEach(suggestion -> {
                list.add(new ListItem(suggestion, NORMAL_FONT));
            });
            
            document.add(list);
        } else {
            Paragraph noSuggestions = new Paragraph("暂无建议", NORMAL_FONT);
            document.add(noSuggestions);
        }
    }

    /**
     * 创建表格单元格
     */
    private PdfPCell createCell(String content, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(content, isHeader ? SUBTITLE_FONT : NORMAL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        
        if (isHeader) {
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
        }
        
        return cell;
    }
}