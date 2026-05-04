package com.mindease.pojo.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 心理评估报告 VO 单元测试
 * <p>
 * 验证评估报告视图对象的字段映射、分值计算、等级判定、
 * 维度分析、风险标签生成等核心逻辑的正确性。
 * </p>
 */
@DisplayName("AssessmentReportVO 心理评估报告单元测试")
class AssessmentReportVOTest {

    // --------------------------------------------------------
    // 被测目标：轻量级 AssessmentReportVO 实现
    // --------------------------------------------------------

    static class AssessmentReportVO {
        private Long userId;
        private Long assessmentId;
        private String assessmentName;
        private LocalDateTime completedAt;
        private BigDecimal totalScore;
        private BigDecimal maxScore;
        private Integer level;           // 1=健康 2=轻度 3=中度 4=重度
        private List<DimensionScore> dimensions;
        private Set<String> riskTags;
        private Map<String, String> suggestions;
        private Boolean needsFollowUp;
        private String counselorComment;

        // --- 分数计算工具方法 ---

        public double getPercentage() {
            if (totalScore == null || maxScore == null || maxScore.doubleValue() == 0) return 0;
            return totalScore.divide(maxScore, 4, java.math.RoundingMode.HALF_UP)
                              .doubleValue() * 100;
        }

        public String getGradeLabel() {
            double pct = getPercentage();
            if (pct < 30) return "低风险";
            if (pct < 50) return "轻度关注";
            if (pct < 70) return "中度关注";
            return "高度关注";
        }

        public String getStatusDescription() {
            switch (level) {
                case 1: return "心理状况良好，继续保持";
                case 2: return "存在轻度症状，建议自我调节";
                case 3: return "存在中度症状，建议寻求专业帮助";
                case 4: return "存在较严重症状，强烈建议咨询专业心理咨询师";
                default: return "暂无评估结论";
            }
        }

        public boolean hasRiskTag(String tag) {
            return riskTags != null && riskTags.contains(tag);
        }

        public DimensionScore getHighestDimension() {
            if (dimensions == null || dimensions.isEmpty()) return null;
            return Collections.max(dimensions, Comparator.comparingDouble(DimensionScore::getScore));
        }

        public DimensionScore getLowestDimension() {
            if (dimensions == null || dimensions.isEmpty()) return null;
            return Collections.min(dimensions, Comparator.comparingDouble(DimensionScore::getScore));
        }

        // --- getter/setter ---
        public Long getUserId() { return userId; }
        public void setUserId(Long v) { this.userId = v; }
        public Long getAssessmentId() { return assessmentId; }
        public void setAssessmentId(Long v) { this.assessmentId = v; }
        public String getAssessmentName() { return assessmentName; }
        public void setAssessmentName(String v) { this.assessmentName = v; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime v) { this.completedAt = v; }
        public BigDecimal getTotalScore() { return totalScore; }
        public void setTotalScore(BigDecimal v) { this.totalScore = v; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal v) { this.maxScore = v; }
        public Integer getLevel() { return level; }
        public void setLevel(Integer v) { this.level = v; }
        public List<DimensionScore> getDimensions() { return dimensions; }
        public void setDimensions(List<DimensionScore> v) { this.dimensions = v; }
        public Set<String> getRiskTags() { return riskTags; }
        public void setRiskTags(Set<String> v) { this.riskTags = v; }
        public Map<String, String> getSuggestions() { return suggestions; }
        public void setSuggestions(Map<String, String> v) { this.suggestions = v; }
        public Boolean getNeedsFollowUp() { return needsFollowUp; }
        public void setNeedsFollowUp(Boolean v) { this.needsFollowUp = v; }
        public String getCounselorComment() { return counselorComment; }
        public void setCounselorComment(String v) { this.counselorComment = v; }
    }

    static class DimensionScore {
        private String dimensionName;
        private double score;
        private double maxValue;
        private String interpretation;

        DimensionScore(String name, double score, double max, String interp) {
            this.dimensionName = name;
            this.score = score;
            this.maxValue = max;
            this.interpretation = interp;
        }

        public double getScore() { return score; }
        public double getPercentage() { return maxValue > 0 ? score / maxValue * 100 : 0; }
        public String getDimensionName() { return dimensionName; }
        public String getInterpretation() { return interpretation; }
    }

    // ==================== 分数百分比计算 ====================

    @Nested
    @DisplayName("分数百分比计算")
    class PercentageCalculationTests {

        @Test
        @DisplayName("满分时百分比为 100%")
        void fullScoreHundredPercent() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setTotalScore(BigDecimal.valueOf(100));
            vo.setMaxScore(BigDecimal.valueOf(100));
            assertEquals(100.0, vo.getPercentage(), 0.001);
        }

        @Test
        @DisplayName("半分时百分比约 50%")
        void halfScoreFiftyPercent() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setTotalScore(BigDecimal.valueOf(42));
            vo.setMaxScore(BigDecimal.valueOf(84));
            assertEquals(50.0, vo.getPercentage(), 0.01);
        }

        @Test
        @DisplayName("零分时百分比约 0%")
        void zeroScoreZeroPercent() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setTotalScore(BigDecimal.ZERO);
            vo.setMaxScore(BigDecimal.valueOf(100));
            assertEquals(0.0, vo.getPercentage(), 0.001);
        }

        @Test
        @DisplayName("总分或最大分为 null 时安全返回 0")
        void nullScoresReturnZero() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setTotalScore(null);
            vo.setMaxScore(BigDecimal.valueOf(100));
            assertEquals(0.0, vo.getPercentage());

            vo.setTotalScore(BigDecimal.valueOf(50));
            vo.setMaxScore(null);
            assertEquals(0.0, vo.getPercentage());

            vo.setTotalScore(null);
            vo.setMaxScore(null);
            assertEquals(0.0, vo.getPercentage());
        }

        @Test
        @DisplayName("小数精度四舍五入正确")
        void decimalPrecisionRounded() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setTotalScore(new BigDecimal("33.3333"));
            vo.setMaxScore(new BigDecimal("100"));
            double pct = vo.getPercentage();
            assertTrue(pct > 33.33 && pct < 33.34);
        }
    }

    // ==================== 等级标签判定 ====================

    @Nested
    @DisplayName("等级标签自动判定")
    class GradeLabelTests {

        private AssessmentReportVO makeVo(double total, double max) {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setTotalScore(BigDecimal.valueOf(total));
            vo.setMaxScore(BigDecimal.valueOf(max));
            return vo;
        }

        @Test
        @DisplayName("低于 30% → 低风险")
        void lowRiskLabel() {
            assertEquals("低风险", makeVo(20, 100).getGradeLabel());
            assertEquals("低风险", makeVo(29.99, 100).getGradeLabel());
        }

        @Test
        @DisplayName("30%~50% → 轻度关注")
        void mildConcernLabel() {
            assertEquals("轻度关注", makeVo(30, 100).getGradeLabel());
            assertEquals("轻度关注", makeVo(49.99, 100).getGradeLabel());
        }

        @Test
        @DisplayName("50%~70% → 中度关注")
        void moderateConcernLabel() {
            assertEquals("中度关注", makeVo(50, 100).getGradeLabel());
            assertEquals("中度关注", makeVo(69.99, 100).getGradeLabel());
        }

        @Test
        @DisplayName("70% 以上 → 高度关注")
        void highConcernLabel() {
            assertEquals("高度关注", makeVo(70, 100).getGradeLabel());
            assertEquals("高度关注", makeVo(95, 100).getGradeLabel());
            assertEquals("高度关注", makeVo(100, 100).getGradeLabel());
        }
    }

    // ==================== 状态描述文案 ====================

    @Nested
    @DisplayName("评估状态描述文案")
    class StatusDescriptionTests {

        @Test
        @DisplayName("level=1 返回良好描述")
        void levelOneDescription() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setLevel(1);
            assertTrue(vo.getStatusDescription().contains("良好"));
        }

        @Test
        @DisplayName("level=2 返回轻度症状描述")
        void levelTwoDescription() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setLevel(2);
            assertTrue(vo.getStatusDescription().contains("轻度"));
        }

        @Test
        @DisplayName("level=3 返回中度症状描述")
        void levelThreeDescription() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setLevel(3);
            assertTrue(vo.getStatusDescription().contains("中度"));
        }

        @Test
        @DisplayName("level=4 返回重度症状+建议咨询描述")
        void levelFourDescription() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setLevel(4);
            assertTrue(vo.getStatusDescription().contains("严重"));
            assertTrue(vo.getStatusDescription().contains("咨询"));
        }

        @Test
        @DisplayName("未知 level 返回默认描述")
        void unknownLevelDefault() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setLevel(99);
            assertEquals("暂无评估结论", vo.getStatusDescription());
        }
    }

    // ==================== 维度分数排序 ====================

    @Nested
    @DisplayName("维度分数排序与分析")
    class DimensionAnalysisTests {

        private AssessmentReportVO createWithDimensions() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setDimensions(Arrays.asList(
                    new DimensionScore("焦虑", 18.0, 25.0, "轻度偏高"),
                    new DimensionScore("抑郁", 12.0, 25.0, "正常范围"),
                    new DimensionScore("压力", 22.0, 25.0, "明显偏高"),
                    new DimensionScore("睡眠", 8.0, 15.0, "轻度问题")
            ));
            return vo;
        }

        @Test
        @DisplayName("最高分维度为压力")
        void highestDimensionIsStress() {
            AssessmentReportVO vo = createWithDimensions();
            DimensionScore highest = vo.getHighestDimension();
            assertEquals("压力", highest.getDimensionName());
        }

        @Test
        @DisplayName("最低分维度为睡眠")
        void lowestDimensionIsSleep() {
            AssessmentReportVO vo = createWithDimensions();
            DimensionScore lowest = vo.getLowestDimension();
            assertEquals("睡眠", lowest.getDimensionName());
        }

        @Test
        @DisplayName("维度自身百分比计算正确")
        void dimensionPercentageAccurate() {
            DimensionScore ds = new DimensionScore("焦虑", 20.0, 25.0, "");
            assertEquals(80.0, ds.getPercentage(), 0.001);
        }

        @Test
        @DisplayName("无维度数据时返回 null")
        void nullWhenNoDimensions() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setDimensions(Collections.emptyList());
            assertNull(vo.getHighestDimension());
            assertNull(vo.getLowestDimension());
        }
    }

    // ==================== 风险标签检测 ====================

    @Nested
    @DisplayName("风险标签集合操作")
    class RiskTagTests {

        @Test
        @DisplayName("已有标签返回 true")
        void existingTagDetected() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setRiskTags(Set.of("自伤倾向", "社交回避", "失眠"));
            assertTrue(vo.hasRiskTag("自伤倾向"));
            assertTrue(vo.hasRiskTag("社交回避"));
            assertTrue(vo.hasRiskTag("失眠"));
        }

        @Test
        @DisplayName("不存在标签返回 false")
        void missingTagNotDetected() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setRiskTags(Set.of("焦虑"));
            assertFalse(vo.hasRiskTag("抑郁"));
            assertFalse(vo.hasRiskTag(""));
        }

        @Test
        @DisplayName("空标签集任何查询返回 false")
        void emptyTagsAllFalse() {
            AssessmentReportVO vo = new AssessmentReportVO();
            vo.setRiskTags(Set.of());
            assertFalse(vo.hasRiskTag("anything"));
        }
    }

    // ==================== 综合报告组装 ====================

    @Nested
    @DisplayName("完整报告对象组装")
    class FullReportAssemblyTests {

        @Test
        @DisplayName("完整报告所有字段可正确赋值和读取")
        void fullReportRoundTrip() {
            AssessmentReportVO report = new AssessmentReportVO();
            report.setUserId(10086L);
            report.setAssessmentId(5001L);
            report.setAssessmentName("PHQ-9 抑郁筛查量表");
            report.setCompletedAt(LocalDateTime.of(2026, 6, 29, 14, 30, 0));
            report.setTotalScore(new BigDecimal("14.00"));
            report.setMaxScore(new BigDecimal("27.00"));
            report.setLevel(2);
            report.setNeedsFollowUp(true);
            report.setCounselorComment("建议进行后续随访观察");

            assertEquals(10086L, report.getUserId());
            assertEquals("PHQ-9 抑郁筛查量表", report.getAssessmentName());
            assertEquals(2, report.getLevel());
            assertTrue(report.getNeedsFollowUp());
            assertNotNull(report.getCounselorComment());

            // 验证衍生字段
            double pct = report.getPercentage();
            assertTrue(pct > 51 && pct < 52);  // 14/27 ≈ 51.85%
            assertEquals("中度关注", report.getGradeLabel());
            assertEquals("存在轻度症状，建议自我调节", report.getStatusDescription());
        }

        @Test
        @DisplayName("带建议字典的报告")
        void reportWithSuggestionsMap() {
            AssessmentReportVO report = new AssessmentReportVO();
            Map<String, String> suggestions = new LinkedHashMap<>();
            suggestions.put("运动建议", "每周至少 3 次，每次 30 分钟有氧运动");
            suggestions.put("作息调整", "保持规律作息，睡前避免使用电子设备");
            suggestions.put("社交互动", "尝试每周参加一次线下社交活动");
            report.setSuggestions(suggestions);

            assertEquals(3, report.getSuggestions().size());
            assertTrue(report.getSuggestions().containsKey("运动建议"));
            assertTrue(report.getSuggestions().containsValue("每周至少 3 次"));
        }
    }
}
