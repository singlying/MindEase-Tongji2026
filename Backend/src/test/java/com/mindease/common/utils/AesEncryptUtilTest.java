package com.mindease.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AES-256-GCM 加解密工具类 {@link AesEncryptUtil} 的单元测试。
 *
 * <p>测试环境说明：不启动 Spring 容器时，{@code actualSecretKey} 不会被 {@code @PostConstruct} 初始化，
 * 工具类会回退到默认密钥 {@code DEFAULT_SECRET_KEY}（"MindEase2024SecretKey32Bytes!!"，32 字节，满足 AES-256 要求）。
 */
@DisplayName("AesEncryptUtil 单元测试 (AES-256-GCM)")
class AesEncryptUtilTest {

    // ==================== 加解密往返测试 ====================

    @Nested
    @DisplayName("加解密往返测试 (Round-Trip)")
    class RoundTripTests {

        @Test
        @DisplayName("普通英文字符串加解密后还原")
        void shouldRoundTripPlainEnglishText() {
            String original = "Hello, MindEase!";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertNotNull(encrypted);
            assertNotEquals(original, encrypted, "密文不应与明文相同");
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("中文字符串加解密后还原")
        void shouldRoundTripChineseText() {
            String original = "你好，这是一段测试文本";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("特殊字符加解密后还原")
        void shouldRoundTripSpecialCharacters() {
            String original = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~ \t\n";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Emoji 和 Unicode 字符加解密后还原")
        void shouldRoundTripEmojiAndUnicode() {
            String original = "😊🎉✅ 微笑 😀 Heart: ♥ 数学: ∑ ∫ ∞";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("长文本加解密后还原")
        void shouldRoundTripLongText() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("长文本测试数据第").append(i).append("条。");
            }
            String original = sb.toString();

            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("数字字符串加解密后还原")
        void shouldRoundTripNumericString() {
            String original = "1234567890";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("单字符加解密后还原")
        void shouldRoundTripSingleCharacter() {
            String original = "A";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }
    }

    // ==================== 加密特性测试 ====================

    @Nested
    @DisplayName("加密特性测试")
    class EncryptionCharacteristicsTests {

        @Test
        @DisplayName("每次加密相同明文产生不同密文（随机 IV）")
        void shouldProduceDifferentCiphertextsForSamePlaintext() {
            String plaintext = "SamePlaintext";

            String cipher1 = AesEncryptUtil.encrypt(plaintext);
            String cipher2 = AesEncryptUtil.encrypt(plaintext);
            String cipher3 = AesEncryptUtil.encrypt(plaintext);

            assertNotEquals(cipher1, cipher2, "第1次和第2次加密结果应不同");
            assertNotEquals(cipher2, cipher3, "第2次和第3次加密结果应不同");
            assertNotEquals(cipher1, cipher3, "第1次和第3次加密结果应不同");
        }

        @Test
        @DisplayName("不同明文产生不同密文")
        void shouldProduceDifferentCiphertextsForDifferentPlaintexts() {
            String cipher1 = AesEncryptUtil.encrypt("Plaintext A");
            String cipher2 = AesEncryptUtil.encrypt("Plaintext B");

            assertNotEquals(cipher1, cipher2);
        }

        @Test
        @DisplayName("加密结果应为合法 Base64 字符串")
        void shouldProduceValidBase64() {
            String cipher = AesEncryptUtil.encrypt("test");
            assertDoesNotThrow(() -> Base64.getDecoder().decode(cipher),
                    "加密结果应是有效的 Base64 编码");
        }

        @Test
        @DisplayName("密文长度大于明文长度（含 IV + 认证标签）")
        void shouldHaveCiphertextLongerThanPlaintext() {
            String plaintext = "Hello";
            String cipher = AesEncryptUtil.encrypt(plaintext);

            // Base64 编码后的密文远大于原文（12字节IV + 密文 + 16字节GCM标签）
            assertTrue(cipher.length() > plaintext.length(),
                    "密文长度应大于明文长度");
        }
    }

    // ==================== null / 空字符串边界测试 ====================

    @Nested
    @DisplayName("null / 空字符串边界测试")
    class NullAndEmptyTests {

        @Test
        @DisplayName("加密 null 返回 null")
        void shouldEncryptNullReturnNull() {
            assertNull(AesEncryptUtil.encrypt(null));
        }

        @Test
        @DisplayName("加密空字符串返回空字符串")
        void shouldEncryptEmptyReturnEmpty() {
            assertEquals("", AesEncryptUtil.encrypt(""));
        }

        @Test
        @DisplayName("解密 null 返回 null")
        void shouldDecryptNullReturnNull() {
            assertNull(AesEncryptUtil.decrypt(null));
        }

        @Test
        @DisplayName("解密空字符串返回空字符串")
        void shouldDecryptEmptyReturnEmpty() {
            assertEquals("", AesEncryptUtil.decrypt(""));
        }
    }

    // ==================== 非法密文测试 ====================

    @Nested
    @DisplayName("非法密文 / 篡改测试")
    class InvalidCiphertextTests {

        @Test
        @DisplayName("解密非 Base64 字符串返回 [解密失败]")
        void shouldReturnDecryptFailureForNonBase64() {
            String result = AesEncryptUtil.decrypt("!!!非法密文!!!");
            assertEquals("[解密失败]", result);
        }

        @Test
        @DisplayName("解密被篡改的密文返回 [解密失败]")
        void shouldReturnDecryptFailureForTamperedCiphertext() {
            String original = AesEncryptUtil.encrypt("SecretMessage");
            // 篡改 Base64 字符串中的一个字符
            String tampered = original.substring(0, original.length() - 2) + "==";

            String result = AesEncryptUtil.decrypt(tampered);
            assertEquals("[解密失败]", result);
        }

        @Test
        @DisplayName("解密随机乱码字符串返回 [解密失败]")
        void shouldReturnDecryptFailureForRandomGarbage() {
            String result = AesEncryptUtil.decrypt("abcdefghijklmnopqrstuvwxyz");
            assertEquals("[解密失败]", result);
        }

        @Test
        @DisplayName("解密长度过短的字符串返回 [解密失败]")
        void shouldReturnDecryptFailureForTooShortInput() {
            // 合法密文至少包含 12 字节 IV + 密文，太短的字符串无法解析
            String result = AesEncryptUtil.decrypt("YQ=="); // Base64 for "a"
            assertEquals("[解密失败]", result);
        }

        @Test
        @DisplayName("使用其他密钥加密的密文无法被本工具解密")
        void shouldFailToDecryptCiphertextFromDifferentKey() {
            // 模拟用不同密钥加密的结果（实际依赖默认密钥，这里用篡改密文模拟）
            String cipher = AesEncryptUtil.encrypt("test");

            // 修改密文中间的一个字符（模拟内容损坏）
            char[] chars = cipher.toCharArray();
            int mid = chars.length / 2;
            chars[mid] = chars[mid] == 'A' ? 'B' : 'A';

            String result = AesEncryptUtil.decrypt(new String(chars));
            // GCM 认证标签校验会失败
            assertEquals("[解密失败]", result,
                    "GCM 认证应检测到密文被篡改");
        }
    }

    // ==================== 解密一致性测试 ====================

    @Nested
    @DisplayName("解密一致性测试")
    class DecryptionConsistencyTests {

        @Test
        @DisplayName("加密后再解密多次结果一致")
        void shouldConsistentlyDecryptSameCiphertext() {
            String cipher = AesEncryptUtil.encrypt("ConsistentPlaintext");

            for (int i = 0; i < 10; i++) {
                assertEquals("ConsistentPlaintext", AesEncryptUtil.decrypt(cipher),
                        "第 " + (i + 1) + " 次解密结果应一致");
            }
        }

        @Test
        @DisplayName("含数字和符号的混合文本加解密正确")
        void shouldRoundTripMixedContent() {
            String original = "用户ID: 10086, 手机号: 138****1234, 金额: ¥99.80";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("JSON 格式文本加解密正确")
        void shouldRoundTripJsonText() {
            String original = "{\"name\":\"张三\",\"age\":25,\"email\":\"zhangsan@example.com\"}";
            String encrypted = AesEncryptUtil.encrypt(original);
            String decrypted = AesEncryptUtil.decrypt(encrypted);

            assertEquals(original, decrypted);
        }
    }
}