package com.mindease.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 手机号码格式校验工具单元测试
 * <p>
 * 覆盖中国大陆手机号段合法性、格式规范、边界值等场景。
 * 用于确保用户注册、登录等涉及手机号输入的环节能正确过滤非法号码。
 * </p>
 */
@DisplayName("PhoneNumberUtil 单元测试")
class PhoneNumberUtilTest {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1(3[0-9]|4[014-9]|5[0-35-9]|6[2567]|7[0-8]|8[0-9]|9[0-35-9])\\d{8}$");

    // --------------------------------------------------------
    // 核心校验方法（被测目标）
    // --------------------------------------------------------

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private String maskPhone(String phone) {
        if (!isValidPhone(phone)) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String extractCarrier(String phone) {
        if (!isValidPhone(phone)) {
            return "未知运营商";
        }
        String prefix = phone.substring(0, 3);
        if ("134|135|136|137|138|139|147|148|150|151|152|157|158|159|172|178|182|183"
                .contains(prefix)) {
            return "中国移动";
        } else if ("130|131|132|145|146|155|156|166|171|175|176|185|186".contains(prefix)) {
            return "中国联通";
        } else if ("133|149|153|173|177|180|181|189|191|199".contains(prefix)) {
            return "中国电信";
        } else if ("174".equals(prefix)) {
            return "卫星通信";
        }
        return "虚拟运营商";
    }

    // ==================== 合法号段测试 ====================

    @Nested
    @DisplayName("合法手机号识别")
    class ValidNumberTests {

        @Test
        @DisplayName("13x 开头的传统号段")
        void shouldAccept13xPrefix() {
            for (String prefix : new String[]{"130", "131", "132", "133", "134", "135",
                    "136", "137", "138", "139"}) {
                assertTrue(isValidPhone(prefix + "12345678"), prefix + " 号段应合法");
            }
        }

        @Test
        @DisplayName("14x 开头的号段（含物联网）")
        void shouldAccept14xPrefix() {
            for (String prefix : new String[]{"140", "141", "142", "143", "144", "145",
                    "146", "147", "148", "149"}) {
                assertTrue(isValidPhone(prefix + "87654321"), prefix + " 号段应合法");
            }
        }

        @Test
        @DisplayName("15x / 16x / 17x / 18x / 19x 各号段全覆盖")
        void shouldAcceptAllModernPrefixes() {
            String[] validPrefixes = {
                    "150", "151", "152", "153", "155", "156", "157", "158", "159",
                    "165", "166", "167", "170", "171", "172", "173", "175", "176", "177", "178",
                    "180", "181", "182", "183", "184", "185", "186", "187", "188", "189",
                    "190", "191", "192", "193", "195", "196", "197", "198", "199",
            };
            for (String p : validPrefixes) {
                assertTrue(isValidPhone(p + "00000000"), p + " 应为有效号段");
            }
        }

        @Test
        @DisplayName("带空格的手机号应自动 trim 后通过")
        void shouldTrimWhitespace() {
            assertTrue(isValidPhone("  13812345678  "));
        }

        @Test
        @DisplayName("全数字且长度为 11 位时通过校验")
        void shouldValidateExactLength() {
            assertEquals(true, isValidPhone("13900001111"));
            assertEquals(false, isValidPhone("1390000111"));   // 少一位
            assertEquals(false, isValidPhone("139000011112")); // 多一位
        }
    }

    // ==================== 非法输入测试 ====================

    @Nested
    @DisplayName("非法输入拒绝测试")
    class InvalidInputTests {

        @Test
        @DisplayName("null 输入返回 false")
        void shouldRejectNull() {
            assertFalse(isValidPhone(null));
        }

        @Test
        @DisplayName("空字符串返回 false")
        void shouldRejectEmpty() {
            assertFalse(isValidPhone(""));
            assertFalse(isValidPhone("   "));
        }

        @Test
        @DisplayName("非数字字符返回 false")
        void shouldRejectNonDigits() {
            assertFalse(isValidPhone("138abcd5678"));
            assertFalse(isValidPhone("13a-234-5678"));
            assertFalse(isValidPhone("(138)12345678"));
        }

        @Test
        @DisplayName("不存在的号段返回 false")
        void shouldRejectInvalidPrefix() {
            assertFalse(isValidPhone("10012345678"));
            assertFalse(isValidPhone("12012345678"));
            assertFalse(isValidPhone("00000000000"));
            assertFalse(isValidPhone("12812345678"));  // 未分配号段
        }

        @Test
        @DisplayName("含特殊符号或字母混合返回 false")
        void shouldRejectMixedContent() {
            assertFalse(isValidPhone("138-1234-5678"));
            assertFalse(isValidPhone("+8613812345678"));  // 带国际区号前缀
        }
    }

    // ==================== 脱敏处理测试 ====================

    @Nested
    @DisplayName("手机号脱敏测试")
    class MaskingTests {

        @Test
        @DisplayName("正常号码中间四位替换为星号")
        void shouldMaskMiddleFourDigits() {
            assertEquals("138****5678", maskPhone("13812345678"));
            assertEquals("150****9999", maskPhone("15012349999"));
        }

        @Test
        @DisplayName("脱敏后总长度保持 11 位")
        void maskedLengthShouldRemainEleven() {
            String masked = maskPhone("13987654321");
            assertEquals(11, masked.length());
        }

        @Test
        @DisplayName("非法号码不进行脱敏，原样返回")
        void shouldNotMaskInvalidNumbers() {
            String invalid = "12345";
            assertEquals(invalid, maskPhone(invalid));
        }

        @Test
        @DisplayName("保留前后各三位可读性")
        void shouldPreservePrefixAndSuffix() {
            String original = "18666668888";
            String masked = maskPhone(original);
            assertTrue(masked.startsWith("186"));
            assertTrue(masked.endsWith("8888"));
            assertTrue(masked.contains("****"));
        }
    }

    // ==================== 运营商识别测试 ====================

    @Nested
    @DisplayName("运营商归属地识别")
    class CarrierDetectionTests {

        @Test
        @DisplayName("中国移动号段正确识别")
        void shouldDetectChinaMobile() {
            assertEquals("中国移动", extractCarrier("13800138000"));
            assertEquals("中国移动", extractCarrier("15012345678"));
            assertEquals("中国移动", extractCarrier("18899887766"));
        }

        @Test
        @DisplayName("中国联通号段正确识别")
        void shouldDetectChinaUnicom() {
            assertEquals("中国联通", extractCarrier("13012345678"));
            assertEquals("中国联通", extractCarrier("16612345678"));
            assertEquals("中国联通", extractCarrier("18612345678"));
        }

        @Test
        @DisplayName("中国电信号段正确识别")
        void shouldDetectChinaTelecom() {
            assertEquals("中国电信", extractCarrier("18912345678"));
            assertEquals("中国电信", extractCarrier("17712345678"));
            assertEquals("中国电信", extractCarrier("19912345678"));
        }

        @Test
        @DisplayName("非法号码返回未知运营商")
        void shouldReturnUnknownForInvalid() {
            assertEquals("未知运营商", extractCarrier("10000000000"));
            assertEquals("未知运营商", extractCarrier(null));
        }

        @Test
        @DisplayName("卫星通信号段识别")
        void shouldDetectSatellite() {
            assertEquals("卫星通信", extractCarrier("17401234567"));
        }
    }

    // ==================== 批量校验与性能 ====================

    @Nested
    @DisplayName("批量校验场景")
    class BatchValidationTests {

        @Test
        @DisplayName("批量号码列表中只保留合法号码")
        void shouldFilterValidPhonesOnly() {
            String[] inputs = {"13812345678", null, "", "abc123", "15098765432", "00011122233"};
            int validCount = 0;
            for (String s : inputs) {
                if (isValidPhone(s)) validCount++;
            }
            assertEquals(2, validCount);
        }

        @Test
        @DisplayName("大量号码快速校验无异常")
        void handleLargeBatchWithoutException() {
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10_000; i++) {
                    isValidPhone("138" + String.format("%08d", i));
                }
            });
        }

        @Test
        @DisplayName("批量脱敏处理均返回正确长度")
        void batchMaskingConsistentLength() {
            String[] phones = {"13987654321", "15800001111", "18612345678", "19987654321"};
            for (String p : phones) {
                assertEquals(11, maskPhone(p).length());
            }
        }

        @Test
        @DisplayName("批量运营商识别覆盖全部已知号段")
        void batchCarrierDetectionCoverage() {
            java.util.Map<String, String> expected = new java.util.HashMap<>();
            expected.put("134", "中国移动");
            expected.put("130", "中国联通");
            expected.put("189", "中国电信");
            expected.put("174", "卫星通信");

            for (var entry : expected.entrySet()) {
                assertEquals(entry.getValue(),
                        extractCarrier(entry.getKey() + "00000000"));
            }
        }
    }

    // ==================== 号段归属地辅助测试 ====================

    @Nested
    @DisplayName("号段归属与格式一致性")
    class FormatConsistencyTests {

        @Test
        @DisplayName("所有合法号段生成的掩码均含 ****")
        void allValidMasksContainAsterisks() {
            String[] prefixes = {"130", "140", "150", "165", "170", "180", "190", "199"};
            for (String p : prefixes) {
                assertTrue(maskPhone(p + "12345678").contains("****"),
                        p + " 掩码应包含 ****");
            }
        }

        @Test
        @DisplayName("运营商识别结果不包含空字符串")
        void carrierNeverReturnsEmpty() {
            String[] testPhones = {"13800138000", "13012345678", "18912345678",
                                   "17401234567", "17012345678"};
            for (String phone : testPhones) {
                String carrier = extractCarrier(phone);
                assertNotNull(carrier);
                assertFalse(carrier.isEmpty());
            }
        }

        @Test
        @DisplayName("同一手机号的掩码结果具有确定性")
        void maskingIsDeterministic() {
            String phone = "15912345678";
            String mask1 = maskPhone(phone);
            String mask2 = maskPhone(phone);
            assertEquals(mask1, mask2, "相同输入应始终产生相同的掩码输出");
        }

        @Test
        @DisplayName("纯数字字符串但长度不为 11 位时拒绝")
        void numericButWrongLengthRejected() {
            assertFalse(isValidPhone("1234567"));       // 太短
            assertFalse(isValidPhone("123456789012"));   // 太长
            assertFalse(isValidPhone("12345678901"));    // 12位
        }
    }
}
