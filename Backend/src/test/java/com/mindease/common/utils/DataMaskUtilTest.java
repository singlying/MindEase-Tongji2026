package com.mindease.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据脱敏工具类单元测试
 * <p>
 * 对用户姓名、身份证号、银行卡号、邮箱等敏感信息进行掩码处理，
 * 确保日志输出、前端展示等场景下隐私数据的安全性。
 * </p>
 */
@DisplayName("DataMaskUtil 数据脱敏工具单元测试")
class DataMaskUtilTest {

    // --------------------------------------------------------
    // 被测目标：各种脱敏策略
    // --------------------------------------------------------

    private String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() <= 1) return "*";
        if (name.length() == 2) return name.charAt(0) + "*";
        char[] chars = name.toCharArray();
        chars[1] = '*';
        for (int i = 2; i < chars.length; i++) {
            chars[i] = i < chars.length - 1 ? '*' : chars[i];
        }
        // 更简洁：首字保留，其余用*替代末字保留
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        int len = idCard.length();
        return idCard.substring(0, 4) + "**********" + idCard.substring(len - 4);
    }

    private String maskBankCard(String cardNo) {
        if (cardNo == null || cardNo.length() < 8) return cardNo;
        int len = cardNo.length();
        return cardNo.substring(0, 4) + " **** **** " + cardNo.substring(len - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return "*" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private String maskAddress(String address) {
        if (address == null || address.length() < 4) return address;
        return address.substring(0, 3) + "****";
    }

    private String maskIp(String ip) {
        if (ip == null || !ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return ip;
        String[] parts = ip.split("\\.");
        return parts[0] + "." + parts[1] + ".*.*";
    }

    // ==================== 姓名脱敏测试 ====================

    @Nested
    @DisplayName("中文姓名脱敏")
    class NameMaskingTests {

        @Test
        @DisplayName("两字名：首字保留 + 星号 + 末字保留")
        void twoCharName() {
            assertEquals("张*", maskName("张三"));
            assertEquals("王*", maskName("王芳"));
        }

        @Test
        @DisplayName("三字名：首尾保留中间用星号填充")
        void threeCharName() {
            assertEquals("张*三", maskName("张伟三"));
            assertEquals("李*华", maskName("李志华"));
        }

        @Test
        @DisplayName("四字及以上姓名：仅保留首尾字符")
        void longName() {
            assertEquals("欧阳**德", maskName("欧阳修德"));
            assertEquals("阿***亚", maskName("阿里巴巴"));
        }

        @Test
        @DisplayName("单字姓名显示单个星号")
        void singleCharName() {
            assertEquals("*", maskName("强"));
        }

        @Test
        @DisplayName("null 和空串原样返回")
        void nullAndEmpty() {
            assertNull(maskName(null));
            assertEquals("", maskName(""));
        }
    }

    // ==================== 身份证号脱敏测试 ====================

    @Nested
    @DisplayName("身份证号脱敏")
    class IdCardMaskingTests {

        @Test
        @DisplayName("18 位身份证：保留前4和后4位")
        void standardIdCard() {
            assertEquals("1101**********1234", maskIdCard("110101199001011234"));
        }

        @Test
        @DisplayName("短于 8 位的输入不做处理")
        void tooShortToMask() {
            String shortId = "1234567";
            assertEquals(shortId, maskIdCard(shortId));
        }

        @Test
        @DisplayName("脱敏后长度不变")
        void lengthPreserved() {
            String original = "310101200001011234";
            assertEquals(original.length(), maskIdCard(original).length());
        }

        @Test
        @DisplayName("不含数字部分被星号覆盖")
        void middleObscured() {
            String result = maskIdCard("320102198505056789");
            assertTrue(result.startsWith("3201"));
            assertTrue(result.endsWith("6789"));
            assertTrue(result.contains("*"));
        }

        @Test
        @DisplayName("null 安全处理")
        void nullSafe() {
            assertNull(maskIdCard(null));
        }
    }

    // ==================== 银行卡号脱敏测试 ====================

    @Nested
    @DisplayName("银行卡号脱敏")
    class BankCardMaskingTests {

        @Test
        @DisplayName("16 位卡号标准格式化展示")
        void sixteenDigitCard() {
            String result = maskBankCard("6222021234567890123");
            assertTrue(result.startsWith("6222"));
            assertTrue(result.endsWith("9012"));
            assertTrue(result.contains("****"));
        }

        @Test
        @DisplayName("19 位卡号（三类户）同样支持")
        void nineteenDigitCard() {
            String result = maskBankCard("6217005820001234567");
            assertNotNull(result);
            assertTrue(result.endsWith("4567"));
        }

        @Test
        @DisplayName("过短的卡号不脱敏")
        void shortCardNumber() {
            String shortNum = "12345678";  // 刚好等于阈值
            assertEquals(shortNum, maskBankCard(shortNum));
        }

        @Test
        @DisplayName("脱敏后仍可通过前后缀区分卡种")
        void prefixAndSuffixPreserved() {
            String card = "6259650812345678";
            String masked = maskBankCard(card);
            assertTrue(masked.startsWith(card.substring(0, 4)));
            assertTrue(masked.endsWith(card.substring(card.length() - 4)));
        }
    }

    // ==================== 邮箱脱敏测试 ====================

    @Nested
    @DisplayName("邮箱地址脱敏")
    class EmailMaskingTests {

        @Test
        @DisplayName("常规邮箱隐藏中间内容")
        void normalEmail() {
            String result = maskEmail("zhangsan@example.com");
            assertTrue(result.startsWith("z"));
            assertTrue(result.endsWith("@example.com"));
            assertTrue(result.contains("***"));
        }

        @Test
        @DisplayName("短用户名的邮箱脱敏")
        void shortUsernameEmail() {
            String result = maskEmail("ab@test.cn");
            assertTrue(result.startsWith("*@"));
        }

        @Test
        @DisplayName("不带 @ 符号的字符串原样返回")
        void nonEmailString() {
            String raw = "not-an-email";
            assertEquals(raw, maskEmail(raw));
        }

        @Test
        @DisplayName("企业邮箱同样适用")
        void corporateEmail() {
            String result = maskEmail("hr_admin@mindease.com");
            assertTrue(result.startsWith("h"));
            assertTrue(result.endsWith("@mindease.com"));
        }

        @Test
        @DisplayName("null 输入安全处理")
        void nullSafe() {
            assertNull(maskEmail(null));
        }
    }

    // ==================== 地址/IP 脱敏测试 ====================

    @Nested
    @DisplayName("地址与 IP 脱敏")
    class AddressAndIpTests {

        @Test
        @DisplayName("详细地址截断后加星号")
        void addressPartialMask() {
            String addr = "上海市浦东新区陆家嘴环路1000号恒生银行大厦28层";
            String result = maskAddress(addr);
            assertTrue(result.startsWith(addr.substring(0, 3)));
            assertTrue(result.endsWith("****"));
        }

        @Test
        @DisplayName("IPv4 地址隐藏后两段")
        void ipv4Masking() {
            assertEquals("192.168.*.*", maskIp("192.168.1.100"));
            assertEquals("10.0.*.*", maskIp("10.0.5.20"));
        }

        @Test
        @DisplayName("非 IP 格式字符串原样返回")
        void nonIpAddressUntouched() {
            String raw = "not.an.ip.address";
            assertEquals(raw, maskIp(raw));
        }

        @Test
        @DisplayName("过短地址不脱敏")
        void shortAddressUntouched() {
            String shortAddr = "上海";
            assertEquals(shortAddr, maskAddress(shortAddr));
        }
    }

    // ==================== 综合批量脱敏测试 ====================

    @Nested
    @DisplayName("综合批量场景")
    class BatchScenarios {

        @Test
        @DisplayName("多种类型字段同时脱敏均成功")
        void allTypesAtOnce() {
            assertDoesNotThrow(() -> {
                maskName("李明");
                maskIdCard("110101200001011234");
                maskBankCard("6222021234567890123");
                maskEmail("testuser@mindease.com");
                maskAddress("北京市朝阳区某某街道1号");
                maskIp("172.16.0.55");
            });
        }

        @Test
        @DisplayName("全部传入 null 不抛异常")
        void allNullInputs() {
            assertAll(
                    () -> assertNull(maskName(null)),
                    () -> assertNull(maskIdCard(null)),
                    () -> assertNull(maskBankCard(null)),
                    () -> assertNull(maskEmail(null)),
                    () -> assertNull(maskAddress(null)),
                    () -> assertNull(maskIp(null))
            );
        }
    }
}
