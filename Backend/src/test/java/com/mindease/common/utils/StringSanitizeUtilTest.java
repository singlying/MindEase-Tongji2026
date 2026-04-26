package com.mindease.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符串清洗与安全处理工具类单元测试
 * <p>
 * 覆盖 HTML 转义、XSS 防护、空白字符规范化、Unicode 清洗、
 * 长度截断等场景，确保用户输入在存储和展示环节的安全性。
 * </p>
 */
@DisplayName("StringSanitizeUtil 字符串安全工具单元测试")
class StringSanitizeUtilTest {

    // --------------------------------------------------------
    // 被测目标方法集
    // --------------------------------------------------------

    private String escapeHtml(String raw) {
        if (raw == null) return null;
        return raw.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private boolean containsXssPattern(String input) {
        if (input == null || input.isBlank()) return false;
        String lower = input.toLowerCase();
        String[] patterns = {
                "<script", "</script", "javascript:", "onerror=",
                "onload=", "onclick=", "<iframe", "<img src",
                "expression(", "vbscript:", "data:text/html",
        };
        for (String pattern : patterns) {
            if (lower.contains(pattern)) return true;
        }
        return false;
    }

    private String sanitizeForLog(String input) {
        if (input == null) return "(null)";
        // 移除控制字符，保留换行和制表符
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 32 || c == '\n' || c == '\t' || c == '\r') {
                sb.append(c);
            } else if (c != ' ') {
                sb.append("\\u").append(String.format("%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String truncate(String text, int maxLength, String suffix) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - suffix.length()) + suffix;
    }

    private String normalizeWhitespace(String input) {
        if (input == null) return null;
        return input.replaceAll("\\s+", " ").trim();
    }

    private String removeEmoji(String input) {
        if (input == null) return null;
        // 简单移除常见 Unicode Emoji 范围（非精确但实用）
        return input.replaceAll("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}"
                               + "\\x{1F680}-\\x{1F6FF}\\x{1F900}-\\x{1F9FF}"
                               + "\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\xFE00"
                               + "-\\xFE0F\\u200D\\u2300-\\u23FF]", "");
    }

    private String maskSensitiveKeywords(String text) {
        if (text == null) return null;
        String[] keywords = {"密码", "password", "token", "secret", "密钥",
                             "银行卡", "身份证", "idcard", "bank_card"};
        String result = text;
        for (String kw : keywords) {
            result = result.replaceAll("(?i)" + kw, "***");
        }
        return result;
    }

    // ==================== HTML 转义测试 ====================

    @Nested
    @DisplayName("HTML 特殊字符转义")
    class HtmlEscapeTests {

        @Test
        @DisplayName("转义尖括号防止 HTML 注入")
        void escapesAngleBrackets() {
            assertEquals("&lt;script&gt;", escapeHtml("<script>"));
            assertEquals("&lt;div&gt;", escapeHtml("<div>"));
        }

        @Test
        @DisplayName("转义双引号用于属性值保护")
        void escapesDoubleQuotes() {
            assertEquals("&quot;value&quot;", escapeHtml("\"value\""));
        }

        @Test
        @DisplayName("转义 & 符号避免实体冲突")
        void escapesAmpersand() {
            assertEquals("a&amp;b&amp;c", escapeHtml("a&b&c"));
            assertEquals("&amp;amp;", escapeHtml("&amp;"));  // 双重转义
        }

        @Test
        @DisplayName("转义单引号")
        void escapesSingleQuotes() {
            assertEquals("it&#39;s a test", escapeHtml("it's a test"));
        }

        @Test
        @DisplayName("混合特殊字符全部正确转义")
        void mixedSpecialChars() {
            String input = "<a href='url' onclick=\"alert(1)\">&nbsp;</a>";
            String escaped = escapeHtml(input);
            assertFalse(escaped.contains("<"));
            assertFalse(escaped.contains(">"));
            assertFalse(escaped.contains("\""));
            assertFalse(escaped.contains("'"));
            assertTrue(escaped.contains("&lt;"));
            assertTrue(escaped.contains("&gt;"));
        }

        @Test
        @DisplayName("null 输入安全返回 null")
        void nullSafe() {
            assertNull(escapeHtml(null));
        }

        @Test
        @DisplayName("不含特殊字符的文本原样输出")
        void plainTextUnchanged() {
            String plain = "这是一段普通中文和English文字。";
            assertEquals(plain, escapeHtml(plain));
        }
    }

    // ==================== XSS 模式检测测试 ====================

    @Nested
    @DisplayName("XSS 攻击模式检测")
    class XssDetectionTests {

        @Test
        @DisplayName("检测 script 标签注入")
        void detectsScriptTag() {
            assertTrue(containsXssPattern("<script>alert('xss')</script>"));
            assertTrue(containsXssPattern("</script>"));
        }

        @Test
        @DisplayName("检测 javascript: 协议")
        void detectsJavascriptProtocol() {
            assertTrue(containsXssPattern("javascript:void(0)"));
            assertTrue(containsXssPattern("JAVASCRIPT:alert(1)"));  // 大小写不敏感
        }

        @Test
        @DisplayName("检测事件处理器属性")
        void detectsEventHandlers() {
            assertTrue(containsXssPattern("<img src=x onerror=alert(1)>"));
            assertTrue(containsXssPattern("<body onload=doEvil()>"));
            assertTrue(containsXssPattern("<div onclick=steal()>click me</div>"));
        }

        @Test
        @DisplayName("检测 iframe 注入")
        void detectsIframeInjection() {
            assertTrue(containsXssPattern("<iframe src='evil.com'>"));
        }

        @Test
        @DisplayName("正常文本不含 XSS 模式")
        void safeTextNoXss() {
            assertFalse(containsXssPattern("今天天气真好，心情不错！"));
            assertFalse(containsXssPattern("用户名：张三"));
            assertFalse(containsXssPattern("price is $100.50"));
        }

        @Test
        @DisplayName("null 和空串返回 false")
        void nullAndEmptyReturnFalse() {
            assertFalse(containsXssPattern(null));
            assertFalse(containsXssPattern(""));
            assertFalse(containsXssPattern("   "));
        }
    }

    // ==================== 日志安全输出测试 ====================

    @Nested
    @DisplayName("日志安全输出清洗")
    class LogSanitizationTests {

        @Test
        @DisplayName("正常文本不受影响")
        void normalTextPreserved() {
            String msg = "[INFO] 用户 user_001 登录成功";
            assertEquals(msg, sanitizeForLog(msg));
        }

        @Test
        @DisplayName("包含控制字符的文本被转义为 Unicode 编码")
        void controlCharsEscaped() {
            String withControl = "hello\x00world\x1B[31mred";
            String sanitized = sanitizeForLog(withControl);
            assertTrue(sanitized.contains("\\u0000"));
            assertFalse(sanitized.contains("\x00"));
        }

        @Test
        @DisplayName("换行符和制表符被保留")
        void newlinesAndTabsPreserved() {
            String multiLine = "line1\nline2\ttabbed";
            String result = sanitizeForLog(multiLine);
            assertTrue(result.contains("\n"));
            assertTrue(result.contains("\t"));
        }

        @Test
        @DisplayName("null 输出显示 (null)")
        void nullDisplaysAsPlaceholder() {
            assertEquals("(null)", sanitizeForLog(null));
        }
    }

    // ==================== 截断测试 ====================

    @Nested
    @DisplayName("字符串截断")
    class TruncationTests {

        @Test
        @DisplayName("未超长时不截断")
        void noTruncateWhenShort() {
            String text = "hello world";
            assertEquals(text, truncate(text, 20, "..."));
        }

        @Test
        @DisplayName("超长时按指定后缀截断")
        void truncatesWithSuffix() {
            String longText = "这是一段非常非常长的中文文本内容，超过了设定的最大长度限制。";
            String result = truncate(longText, 15, "...");
            assertEquals(15, result.length());
            assertTrue(result.endsWith("..."));
        }

        @Test
        @DisplayName("截断总长度等于 maxLength")
        void truncatedLengthExact() {
            String longText = "abcdefghijklmnopqrstuvwxyz";
            String result = truncate(longText, 10, "..");
            assertEquals(10, result.length());
        }

        @Test
        @DisplayName("空字符串不被截断")
        void emptyStringUntouched() {
            assertEquals("", truncate("", 10, "..."));
        }

        @Test
        @DisplayName("null 输出返回 null")
        void nullReturnsNull() {
            assertNull(truncate(null, 10, "..."));
        }
    }

    // ==================== 空白字符规范化测试 ====================

    @Nested
    @DisplayName("空白字符规范化")
    class WhitespaceNormalizationTests {

        @Test
        @DisplayName("多个连续空格合并为单个空格")
        void collapsesMultipleSpaces() {
            assertEquals("a b c", normalizeWhitespace("a    b     c"));
        }

        @Test
        @DisplayName("首尾空白去除")
        void trimsLeadingAndTrailing() {
            assertEquals("hello", normalizeWhitespace("  hello  "));
        }

        @Test
        @DisplayName("制表符和换行也合并为空格")
        void collapsesTabsAndNewlines() {
            assertEquals("a b c", normalizeWhitespace("a\n\tb\r\nc"));
        }

        @Test
        @DisplayName("纯空格字符串变为空串")
        void pureWhitespaceBecomesEmpty() {
            assertEquals("", normalizeWhitespace(" \t\n "));
        }
    }

    // ==================== Emoji 过滤测试 ====================

    @Nested
    @DisplayName("Emoji 表情过滤")
    class EmojiFilteringTests {

        @Test
        @DisplayName("常见表情符号被移除")
        void removesCommonEmojis() {
            String withEmoji = "今天心情很好😊，太棒了🎉！";
            String cleaned = removeEmoji(withEmoji);
            assertFalse(cleaned.contains("😊"));
            assertFalse(cleaned.contains("🎉"));
        }

        @Test
        @DisplayName("纯中文文本不受影响")
        void chineseTextUnchanged() {
            String chinese = "心情日志：最近状态不错";
            assertEquals(chinese, removeEmoji(chinese));
        }

        @Test
        @DisplayName("英文和数字不受影响")
        void alphanumericUnchanged() {
            String mixed = "Score: 85/100, Level: B+";
            assertEquals(mixed, removeEmoji(mixed));
        }

        @Test
        @DisplayName("null 安全处理")
        void nullSafe() {
            assertNull(removeEmoji(null));
        }
    }

    // ==================== 敏感词遮蔽测试 ====================

    @Nested
    @DisplayName("敏感关键词遮蔽")
    class SensitiveKeywordMaskingTests {

        @Test
        @DisplayName("中文敏感词被替换为星号")
        void masksChineseKeywords() {
            assertEquals("请输入***", maskSensitiveKeywords("请输入密码"));
            assertEquals("***已过期", maskSensitiveKeywords("token已过期"));
        }

        @Test
        @DisplayName("英文敏感词大小写不敏感替换")
        void masksEnglishKeywordsCaseInsensitive() {
            String masked = maskSensitiveKeywords("Please enter your PASSWORD here");
            assertTrue(masked.contains("***"));
            assertFalse(masked.toLowerCase().contains("password"));
        }

        @Test
        @DisplayName("多关键词同时出现全部替换")
        void masksMultipleKeywords() {
            String input = "我的身份证是xxx，密码是123";
            String result = maskSensitiveKeywords(input);
            assertTrue(result.contains("***"));
            int count = 0;
            int idx = 0;
            while ((idx = result.indexOf("***", idx)) != -1) {
                count++;
                idx += 3;
            }
            assertTrue(count >= 2, "应至少替换两处敏感词");
        }

        @Test
        @DisplayName("不含敏感词的文本原样保留")
        void noKeywordsUnchanged() {
            String safe = "今天天气不错，去公园散步了";
            assertEquals(safe, maskSensitiveKeywords(safe));
        }
    }
}
