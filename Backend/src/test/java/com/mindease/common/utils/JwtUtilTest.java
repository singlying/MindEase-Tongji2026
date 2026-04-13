package com.mindease.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试 (HS256)")
class JwtUtilTest {

    /**
     * HMAC-SHA256 要求密钥至少 256 位（32 字节）。
     * 以下测试密钥长度为 35 字节，满足 jjwt 0.12.x 的强密钥要求。
     */
    private static final String SECRET_KEY = "TestSecretKeyForJWT2024!!Min32Bytes";
    private static final long DEFAULT_TTL = 60_000L; // 60 秒

    @BeforeAll
    @DisplayName("验证密钥长度满足 HS256 最低要求")
    static void verifyKeyLength() {
        assertTrue(SECRET_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32,
                "测试密钥长度必须 >= 32 字节");
    }

    // ==================== Token 生成测试 ====================

    @Nested
    @DisplayName("Token 生成测试")
    class TokenCreationTests {

        @Test
        @DisplayName("生成 Token 不为空")
        void shouldCreateNonEmptyToken() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("生成的 Token 由 3 部分组成（Header.Payload.Signature）")
        void shouldHaveThreePartsSeparatedByDots() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);

            String[] parts = token.split("\\.");
            assertEquals(3, parts.length,
                    "JWT 应由 Header、Payload、Signature 三部分组成");
            assertFalse(parts[0].isEmpty());
            assertFalse(parts[1].isEmpty());
            assertFalse(parts[2].isEmpty());
        }

        @Test
        @DisplayName("相同 Claims 每次生成的 Token 应不同（iat/exp 不同导致）")
        void shouldProduceDifferentTokensForSameClaims() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token1 = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            String token2 = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);

            assertNotEquals(token1, token2,
                    "每次生成的 Token 应不同（时间戳不同）");
        }
    }

    // ==================== 解析与验证测试 ====================

    @Nested
    @DisplayName("解析与验证测试")
    class ParseAndValidationTests {

        @Test
        @DisplayName("生成后解析，Claims 数据完整性验证")
        void shouldParseAndVerifyClaimsIntegrity() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", 100L);
            claims.put("username", "testUser");
            claims.put("role", "admin");

            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            Claims parsedClaims = JwtUtil.parseJWT(SECRET_KEY, token);

            assertEquals(100L, parsedClaims.get("userId", Long.class));
            assertEquals("testUser", parsedClaims.get("username", String.class));
            assertEquals("admin", parsedClaims.get("role", String.class));
        }

        @Test
        @DisplayName("解析后的 expiration 应在合理的时间范围内")
        void shouldHaveValidExpirationTime() {
            Map<String, Object> claims = Map.of("userId", 1L);
            long beforeCreate = System.currentTimeMillis();
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            long afterCreate = System.currentTimeMillis();

            Claims parsedClaims = JwtUtil.parseJWT(SECRET_KEY, token);
            Date expiration = parsedClaims.getExpiration();

            assertNotNull(expiration);
            // expiration ≈ 创建时间 + TTL
            long expectedMin = beforeCreate + DEFAULT_TTL;
            long expectedMax = afterCreate + DEFAULT_TTL;
            assertTrue(expiration.getTime() >= expectedMin - 1000,
                    "过期时间不应早于创建时间 + TTL（允许1s误差）");
            assertTrue(expiration.getTime() <= expectedMax + 1000,
                    "过期时间不应晚于创建时间 + TTL + 1s");
        }

        @Test
        @DisplayName("单 Claim（userId）验证")
        void shouldRetrieveSingleClaim() {
            Map<String, Object> claims = Map.of("userId", 42L);
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

            assertEquals(42L, parsed.get("userId", Long.class));
        }

        @Test
        @DisplayName("空 Claims Map 生成和解析")
        void shouldHandleEmptyClaimsMap() {
            Map<String, Object> claims = new HashMap<>();
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

            assertNotNull(parsed);
            assertNotNull(parsed.getExpiration());
        }

        @Test
        @DisplayName("中文 Claims 值正常解析")
        void shouldHandleChineseClaimValues() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("nickname", "张三");
            claims.put("description", "这是一段中文描述");

            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

            assertEquals("张三", parsed.get("nickname", String.class));
            assertEquals("这是一段中文描述", parsed.get("description", String.class));
        }

        @Test
        @DisplayName("特殊字符 Claims 值正常解析")
        void shouldHandleSpecialCharacterClaims() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", "user@example.com");
            claims.put("url", "https://api.test.com/path?key=value&flag=true");

            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

            assertEquals("user@example.com", parsed.get("email", String.class));
            assertEquals("https://api.test.com/path?key=value&flag=true",
                    parsed.get("url", String.class));
        }
    }

    // ==================== Token 过期机制测试 ====================

    @Nested
    @DisplayName("Token 过期机制测试")
    class TokenExpirationTests {

        @Test
        @DisplayName("TTL=0 的 Token 立即过期，解析抛出 ExpiredJwtException")
        void shouldThrowExpiredJwtExceptionForZeroTtl() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = JwtUtil.createJWT(SECRET_KEY, 0L, claims);

            assertThrows(ExpiredJwtException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, token),
                    "TTL=0 的 Token 应立即过期");
        }

        @Test
        @DisplayName("TTL 为负数（已过期）时解析抛出 ExpiredJwtException")
        void shouldThrowExpiredJwtExceptionForNegativeTtl() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = JwtUtil.createJWT(SECRET_KEY, -1000L, claims);

            assertThrows(ExpiredJwtException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, token),
                    "负 TTL 的 Token 应已过期");
        }

        @Test
        @DisplayName("TTL=1ms 的 Token 在等待 2ms 后过期")
        void shouldExpireAfterVeryShortTtl() throws InterruptedException {
            Map<String, Object> claims = Map.of("userId", 1L);
            // 创建 1 毫秒 TTL 的 Token
            String token = JwtUtil.createJWT(SECRET_KEY, 1L, claims);

            // 等待超过 TTL
            Thread.sleep(5);

            assertThrows(ExpiredJwtException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, token),
                    "Token 在 TTL=1ms 后等待超过 1ms 应过期");
        }

        @Test
        @DisplayName("正常 TTL 内 Token 仍可解析")
        void shouldStillParseWithinTtl() {
            Map<String, Object> claims = Map.of("userId", 1L);
            // 5 秒 TTL，立即解析应成功
            String token = JwtUtil.createJWT(SECRET_KEY, 5000L, claims);
            Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

            assertNotNull(parsed);
            assertEquals(1L, parsed.get("userId", Long.class));
        }
    }

    // ==================== 非法 Token 测试 ====================

    @Nested
    @DisplayName("非法 Token 测试")
    class InvalidTokenTests {

        @Test
        @DisplayName("使用错误密钥解析时抛出异常")
        void shouldThrowExceptionForWrongSecretKey() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);

            String wrongKey = "WrongSecretKeyForJWT2024!!Different32Bytes";
            assertThrows(SignatureException.class,
                    () -> JwtUtil.parseJWT(wrongKey, token),
                    "使用错误密钥解析应抛出 SignatureException");
        }

        @Test
        @DisplayName("解析随机乱码 Token 抛出异常")
        void shouldThrowExceptionForRandomGarbageToken() {
            assertThrows(MalformedJwtException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, "this.is.not.a.valid.jwt.token"),
                    "解析非法 Token 应抛出异常");
        }

        @Test
        @DisplayName("解析空字符串 Token 抛出异常")
        void shouldThrowExceptionForEmptyToken() {
            assertThrows(IllegalArgumentException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, ""),
                    "解析空字符串应抛出异常");
        }

        @Test
        @DisplayName("解析只含一个点的字符串抛出异常")
        void shouldThrowExceptionForSingleDotString() {
            assertThrows(MalformedJwtException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, "header.payload"),
                    "解析不符合 JWT 格式的字符串应抛出异常");
        }

        @Test
        @DisplayName("篡改 Payload 后解析抛出签名异常")
        void shouldThrowExceptionForTamperedPayload() {
            Map<String, Object> claims = Map.of("userId", 1L);
            String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);

            // 拆开 Token，修改 payload（第二段）
            String[] parts = token.split("\\.");
            // 将 payload 的最后几个字符改掉
            String tamperedPayload = parts[1].substring(0, parts[1].length() - 4) + "XXXX";
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

            assertThrows(SignatureException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, tamperedToken),
                    "篡改 Payload 后签名验证应失败");
        }
    }

    // ==================== 综合场景测试 ====================

    @Nested
    @DisplayName("综合场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("模拟用户认证流程：生成 → 解析 → 验证身份信息")
        void shouldSimulateAuthenticationFlow() {
            // 1. 登录成功，生成 Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", 8866L);
            claims.put("username", "zhangsan");
            claims.put("role", "user");
            claims.put("email", "zhangsan@mindease.com");
            String token = JwtUtil.createJWT(SECRET_KEY, 3_600_000L, claims);

            // 2. 后续请求携带 Token，服务端解析验证
            Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

            assertEquals(8866L, parsed.get("userId", Long.class));
            assertEquals("zhangsan", parsed.get("username", String.class));
            assertEquals("user", parsed.get("role", String.class));
            assertEquals("zhangsan@mindease.com", parsed.get("email", String.class));

            // 3. 验证过期时间合理（约 1 小时后）
            long now = System.currentTimeMillis();
            assertTrue(parsed.getExpiration().getTime() > now,
                    "Token 不应已过期");
            assertTrue(parsed.getExpiration().getTime() < now + 3_700_000L,
                    "过期时间应在 1 小时左右");
        }

        @Test
        @DisplayName("连续生成 50 个 Token 并全部解析成功")
        void shouldCreateAndParseFiftyTokens() {
            for (int i = 0; i < 50; i++) {
                Map<String, Object> claims = new HashMap<>();
                claims.put("userId", (long) i);
                claims.put("session", "session-" + i);

                String token = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
                Claims parsed = JwtUtil.parseJWT(SECRET_KEY, token);

                assertEquals((long) i, parsed.get("userId", Long.class),
                        "第 " + (i + 1) + " 个 Token 的 userId 不匹配");
                assertEquals("session-" + i, parsed.get("session", String.class));
            }
        }

        @Test
        @DisplayName("不同密钥生成的 Token 互不可解析")
        void shouldNotCrossParseTokensFromDifferentKeys() {
            String anotherKey = "AnotherTestSecretKeyForJWT2024!!32B";

            Map<String, Object> claims = Map.of("userId", 1L);
            String tokenFromKey1 = JwtUtil.createJWT(SECRET_KEY, DEFAULT_TTL, claims);
            String tokenFromKey2 = JwtUtil.createJWT(anotherKey, DEFAULT_TTL, claims);

            // 用 Key1 解析 Key2 的 Token → 抛出签名异常
            assertThrows(SignatureException.class,
                    () -> JwtUtil.parseJWT(SECRET_KEY, tokenFromKey2),
                    "Key1 不能解析 Key2 生成的 Token");

            // 用 Key2 解析 Key1 的 Token → 抛出签名异常
            assertThrows(SignatureException.class,
                    () -> JwtUtil.parseJWT(anotherKey, tokenFromKey1),
                    "Key2 不能解析 Key1 生成的 Token");
        }
    }
}
