package com.mindease.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SensitiveWordFilter 单元测试")
class SensitiveWordFilterTest {

    private final SensitiveWordFilter filter = new SensitiveWordFilter();

    // ==================== containsSensitiveWord 测试 ====================

    @Nested
    @DisplayName("containsSensitiveWord 敏感词检测测试")
    class ContainsSensitiveWordTests {

        @Test
        @DisplayName("文本包含敏感词 '自杀' 返回 true")
        void shouldDetectSingleSensitiveWord() {
            assertTrue(filter.containsSensitiveWord("我有自杀倾向"));
        }

        @Test
        @DisplayName("文本包含敏感词 '想死' 返回 true")
        void shouldDetectAnotherSensitiveWord() {
            assertTrue(filter.containsSensitiveWord("最近状态很差，感觉想死了"));
        }

        @Test
        @DisplayName("文本包含多个敏感词时返回 true")
        void shouldDetectMultipleSensitiveWords() {
            assertTrue(filter.containsSensitiveWord("我想自杀或者跳楼"));
        }

        @Test
        @DisplayName("不包含任何敏感词的文本返回 false")
        void shouldReturnFalseForSafeText() {
            assertFalse(filter.containsSensitiveWord("今天天气很好，心情不错"));
        }

        @Test
        @DisplayName("正常心理咨询对话返回 false")
        void shouldReturnFalseForNormalCounselingText() {
            assertFalse(filter.containsSensitiveWord("我最近有些焦虑，工作压力比较大"));
            assertFalse(filter.containsSensitiveWord("和朋友聊天后感觉好多了"));
            assertFalse(filter.containsSensitiveWord("我喜欢听音乐放松自己"));
        }

        @Test
        @DisplayName("null 文本返回 false")
        void shouldReturnFalseForNull() {
            assertFalse(filter.containsSensitiveWord(null));
        }

        @Test
        @DisplayName("空字符串返回 false")
        void shouldReturnFalseForEmptyString() {
            assertFalse(filter.containsSensitiveWord(""));
        }

        @Test
        @DisplayName("全空格字符串返回 false")
        void shouldReturnFalseForWhitespaceOnly() {
            assertFalse(filter.containsSensitiveWord("     "));
        }

        @Test
        @DisplayName("敏感词作为文本子串出现时应检测到")
        void shouldDetectSensitiveWordAsSubstring() {
            // "自杀" 是文本的子串
            assertTrue(filter.containsSensitiveWord("预防自杀热线电话"));
            assertTrue(filter.containsSensitiveWord("关于自杀干预的研究"));
        }
    }

    // ==================== getFirstSensitiveWord 测试 ====================

    @Nested
    @DisplayName("getFirstSensitiveWord 获取首个敏感词测试")
    class GetFirstSensitiveWordTests {

        @Test
        @DisplayName("文本包含单个敏感词时返回该词")
        void shouldReturnSingleSensitiveWord() {
            String result = filter.getFirstSensitiveWord("最近有自杀的念头");
            assertEquals("自杀", result);
        }

        @Test
        @DisplayName("文本包含单个敏感词 '跳楼' 时返回该词")
        void shouldReturnSpecificSensitiveWord() {
            String result = filter.getFirstSensitiveWord("站在高处想跳楼");
            assertEquals("跳楼", result);
        }

        @Test
        @DisplayName("文本包含多个敏感词时返回其中一个")
        void shouldReturnOneOfMultipleSensitiveWords() {
            String result = filter.getFirstSensitiveWord("我想自杀或者上吊");
            assertNotNull(result);
            // HashSet 迭代顺序不确定，检查是否为其中某个敏感词
            assertTrue("自杀".equals(result) || "上吊".equals(result),
                    "返回的敏感词应为 '自杀' 或 '上吊'，实际: " + result);
        }

        @Test
        @DisplayName("不包含敏感词返回 null")
        void shouldReturnNullForSafeText() {
            assertNull(filter.getFirstSensitiveWord("今天心情还不错"));
        }

        @Test
        @DisplayName("null 文本返回 null")
        void shouldReturnNullForNull() {
            assertNull(filter.getFirstSensitiveWord(null));
        }

        @Test
        @DisplayName("空字符串返回 null")
        void shouldReturnNullForEmptyString() {
            assertNull(filter.getFirstSensitiveWord(""));
        }
    }

    // ==================== getAllSensitiveWords 测试 ====================

    @Nested
    @DisplayName("getAllSensitiveWords 获取所有敏感词测试")
    class GetAllSensitiveWordsTests {

        @Test
        @DisplayName("文本包含单个敏感词时返回包含该词的列表")
        void shouldReturnListWithSingleWord() {
            List<String> result = filter.getAllSensitiveWords("我有自残行为");
            assertEquals(1, result.size());
            assertEquals("自残", result.get(0));
        }

        @Test
        @DisplayName("文本包含多个敏感词时返回全部匹配词")
        void shouldReturnAllMatchedWords() {
            List<String> result = filter.getAllSensitiveWords("我想自杀、自残、跳楼，不想活了");

            assertTrue(result.size() >= 3, "至少应检测到 3 个敏感词，实际: " + result.size());
            assertTrue(result.contains("自杀"));
            assertTrue(result.contains("自残"));
            assertTrue(result.contains("跳楼"));
            assertTrue(result.contains("不想活了"));
        }

        @Test
        @DisplayName("包含所有类型敏感词的文本应全部检测到")
        void shouldDetectAllTypesOfSensitiveWords() {
            String text = "绝望到想自杀，准备割腕、上吊或者跳楼，只想自我了结，活着太累了，撑不下去了";
            List<String> result = filter.getAllSensitiveWords(text);

            assertTrue(result.contains("自杀"));
            assertTrue(result.contains("割腕"));
            assertTrue(result.contains("上吊"));
            assertTrue(result.contains("跳楼"));
            assertTrue(result.contains("自我了结"));
            assertTrue(result.contains("活着太累了"));
            assertTrue(result.contains("撑不下去了"));
        }

        @Test
        @DisplayName("不包含敏感词返回空列表")
        void shouldReturnEmptyListForSafeText() {
            List<String> result = filter.getAllSensitiveWords("今天阳光明媚，心情很好");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null 文本返回空列表")
        void shouldReturnEmptyListForNull() {
            List<String> result = filter.getAllSensitiveWords(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("空字符串返回空列表")
        void shouldReturnEmptyListForEmptyString() {
            List<String> result = filter.getAllSensitiveWords("");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 大小写不敏感测试 ====================

    @Nested
    @DisplayName("大小写不敏感检测测试")
    class CaseInsensitiveTests {

        @Test
        @DisplayName("敏感词 '自杀' 匹配 '自杀' （大小写不变）")
        void shouldMatchExactCaseChineseChars() {
            assertTrue(filter.containsSensitiveWord("自杀"));
        }

        @Test
        @DisplayName("不包含敏感词的英文文本也应正常处理")
        void shouldHandleEnglishTextWithoutSensitiveWords() {
            assertFalse(filter.containsSensitiveWord("I feel a bit sad today"));
        }

        @Test
        @DisplayName("中英文混合安全文本不被误报")
        void shouldNotFlagMixedContentWithoutSensitiveWords() {
            assertFalse(filter.containsSensitiveWord("I feel happy today 今天很开心"));
        }

        @Test
        @DisplayName("中英文混合含敏感词文本应被检测到")
        void shouldDetectSensitiveWordInMixedContent() {
            assertTrue(filter.containsSensitiveWord("I feel terrible, 我想自杀"));
            assertTrue(filter.containsSensitiveWord("绝望了 I want to die 跳楼"));
        }
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况测试")
    class BoundaryTests {

        @Test
        @DisplayName("仅有敏感词本身的文本返回 true")
        void shouldDetectExactWordOnly() {
            assertTrue(filter.containsSensitiveWord("自杀"));
            assertTrue(filter.containsSensitiveWord("跳楼"));
            assertTrue(filter.containsSensitiveWord("割腕"));
        }

        @Test
        @DisplayName("敏感词紧贴标点符号也能检测到")
        void shouldDetectWithAdjacentPunctuation() {
            assertTrue(filter.containsSensitiveWord("「自杀」"));
            assertTrue(filter.containsSensitiveWord("他说：\"想死\""));
            assertTrue(filter.containsSensitiveWord("(跳楼)"));
        }

        @Test
        @DisplayName("非常长的安全文本返回 false")
        void shouldHandleVeryLongSafeText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("今天天气真好，适合出去走走。");
            }
            assertFalse(filter.containsSensitiveWord(sb.toString()));
        }

        @Test
        @DisplayName("getAllSensitiveWords 返回的列表不包含重复项")
        void shouldNotContainDuplicatesInResult() {
            List<String> result = filter.getAllSensitiveWords("自杀自杀自杀");
            // 因为 HashSet 迭代，每个敏感词只检测一次，所以结果不重复
            long suicideCount = result.stream().filter("自杀"::equals).count();
            assertTrue(suicideCount <= 1, "敏感词列表中不应有重复项");
        }
    }

    // ==================== 词库覆盖验证测试 ====================

    @Nested
    @DisplayName("词库覆盖验证测试")
    class DictionaryCoverageTests {

        @Test
        @DisplayName("验证关键类别敏感词均可被检测")
        void shouldCoverKeyCategories() {
            // 自杀相关
            assertTrue(filter.containsSensitiveWord("想自杀"));
            assertTrue(filter.containsSensitiveWord("想自尽"));
            assertTrue(filter.containsSensitiveWord("轻生念头"));
            assertTrue(filter.containsSensitiveWord("不想活了"));
            assertTrue(filter.containsSensitiveWord("活着没意义"));

            // 自伤方式
            assertTrue(filter.containsSensitiveWord("割腕"));
            assertTrue(filter.containsSensitiveWord("上吊"));
            assertTrue(filter.containsSensitiveWord("跳楼"));
            assertTrue(filter.containsSensitiveWord("投河"));
            assertTrue(filter.containsSensitiveWord("服毒"));
            assertTrue(filter.containsSensitiveWord("烧炭"));

            // 准备行为
            assertTrue(filter.containsSensitiveWord("买安眠药"));
            assertTrue(filter.containsSensitiveWord("找绳子"));

            // 绝望表达
            assertTrue(filter.containsSensitiveWord("活着太累了"));
            assertTrue(filter.containsSensitiveWord("撑不下去了"));
            assertTrue(filter.containsSensitiveWord("一了百了"));
        }

        @Test
        @DisplayName("词库中不存在但形式相近的词汇不应误报")
        void shouldNotFlagSimilarButSafeWords() {
            // 这些词不在敏感词库中，不应被误报
            assertFalse(filter.containsSensitiveWord("杀死"));
            assertFalse(filter.containsSensitiveWord("死心"));
            assertFalse(filter.containsSensitiveWord("楼房"));
            assertFalse(filter.containsSensitiveWord("火炭"));
            assertFalse(filter.containsSensitiveWord("绳子"));
        }

        @Test
        @DisplayName("getAllSensitiveWords 在复杂文本中返回完整的词列表")
        void shouldReturnCompleteWordListForComplexText() {
            String text = "我活着太累了，撑不下去了，想自杀，已经准备好刀片割腕了，真的一了百了";
            List<String> result = filter.getAllSensitiveWords(text);

            assertTrue(result.contains("活着太累了"));
            assertTrue(result.contains("撑不下去了"));
            assertTrue(result.contains("自杀"));
            assertTrue(result.contains("刀片割"));
            assertTrue(result.contains("割腕"));
            assertTrue(result.contains("一了百了"));
        }
    }
}
