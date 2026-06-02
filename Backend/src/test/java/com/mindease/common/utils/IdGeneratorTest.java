package com.mindease.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式 ID 与唯一标识生成工具单元测试
 * <p>
 * 涵盖雪花算法 ID、UUID、时间戳 ID、哈希摘要等多种 ID 生成策略的
 * 正确性验证，确保在多实体关联场景中不会产生冲突。
 * </p>
 */
@DisplayName("IdGenerator 单元测试")
class IdGeneratorTest {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final long EPOCH = 1700000000000L;  // 自定义起始时间戳

    // --------------------------------------------------------
    // 被测方法：各类 ID 生成策略
    // --------------------------------------------------------

    /**
     * 简化的 Snowflake 风格 ID 生成器
     */
    private long nextSnowflakeId(int nodeId) {
        long timestamp = System.currentTimeMillis() - EPOCH;
        long seq = SEQUENCE.incrementAndGet() & 0xFFF;  // 12-bit sequence
        return ((timestamp << 22) | (nodeId << 12) | seq);
    }

    private String generateUuid() {
        return UUID.randomUUID().toString();
    }

    private String generateUuidNoDash() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private long generateTimestampId() {
        return System.currentTimeMillis() * 10000 + (System.nanoTime() % 10000);
    }

    private String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateOrderId(String prefix) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        String timePart = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", (int) (Math.random() * 1_000_000));
        return prefix + timePart + randomPart;
    }

    private String shortId() {
        UUID uuid = UUID.randomUUID();
        long hi = uuid.getMostSignificantBits();
        long lo = uuid.getLeastSignificantBits();
        return Long.toHexString(hi ^ lo).substring(0, 16);
    }

    // ==================== Snowflake ID 测试 ====================

    @Nested
    @DisplayName("Snowflake ID 生成策略测试")
    class SnowflakeTests {

        @Test
        @DisplayName("不同节点生成的 ID 应不同")
        void differentNodesProduceDifferentIds() {
            long id1 = nextSnowflakeId(1);
            long id2 = nextSnowflakeId(2);
            assertNotEquals(id1, id2);
        }

        @Test
        @DisplayName("同一节点连续调用产生递增 ID")
        void sameNodeProducesIncreasingIds() {
            long first = nextSnowflakeId(1);
            long second = nextSnowflakeId(1);
            assertTrue(second > first, "序列号递增保证单调性");
        }

        @Test
        @DisplayName("ID 应始终为正数")
        void shouldBePositive() {
            for (int i = 0; i < 100; i++) {
                assertTrue(nextSnowflakeId(i % 64) > 0);
            }
        }

        @Test
        @DisplayName("高并发环境下不产生重复 ID（抽样检查）")
        void noDuplicatesUnderRapidGeneration() {
            int count = 5000;
            long[] ids = new long[count];
            for (int i = 0; i < count; i++) {
                ids[i] = nextSnowflakeId(i % 32);
            }
            // 简单去重检查
            for (int i = 0; i < count; i++) {
                for (int j = i + 1; j < count; j++) {
                    if (ids[i] == ids[j]) {
                        fail("发现重复 ID: " + ids[i] + " at index " + i + "," + j);
                    }
                }
            }
        }
    }

    // ==================== UUID 生成测试 ====================

    @Nested
    @DisplayName("UUID 生成测试")
    class UuidTests {

        @Test
        @DisplayName("标准 UUID 格式含 4 个连字符")
        void standardUuidHasCorrectFormat() {
            String uuid = generateUuid();
            assertNotNull(uuid);
            assertEquals(36, uuid.length());  // 32 hex chars + 4 dashes
            assertEquals(4, countChar(uuid, '-'));
        }

        @Test
        @DisplayName("去除连字符后长度为 32")
        void compactUuidHas32Chars() {
            String uuid = generateUuidNoDash();
            assertEquals(32, uuid.length());
            assertFalse(uuid.contains("-"));
        }

        @Test
        @DisplayName("连续两次生成的 UUID 不同")
        void consecutiveUuidsAreUnique() {
            String a = generateUuid();
            String b = generateUuid();
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("批量生成大量 UUID 无重复")
        void batchUuidUniqueness() {
            int size = 1000;
            java.util.Set<String> set = new java.util.HashSet<>(size);
            for (int i = 0; i < size; i++) {
                assertTrue(set.add(generateUuid()));
            }
        }

        @Test
        @DisplayName("UUID 第四组首字符为版本标识 '4'")
        void versionFourIdentifier() {
            String uuid = generateUuid();
            // 标准 UUID v4 格式: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
            char versionChar = uuid.charAt(14);
            assertEquals('4', versionChar, "应为 UUID v4 版本标识");
        }
    }

    // ==================== 时间戳 ID 测试 ====================

    @Nested
    @DisplayName("时间戳型 ID 测试")
    class TimestampIdTests {

        @Test
        @DisplayName("基于时间戳的 ID 随时间递增")
        void increasesOverTime() throws InterruptedException {
            long id1 = generateTimestampId();
            Thread.sleep(2);  // 微小延迟确保时间推进
            long id2 = generateTimestampId();
            assertTrue(id2 >= id1);
        }

        @Test
        @DisplayName("时间戳 ID 始终为正长整数")
        void alwaysPositiveLong() {
            for (int i = 0; i < 50; i++) {
                assertTrue(generateTimestampId() > 0);
            }
        }
    }

    // ==================== 业务订单号测试 ====================

    @Nested
    @DisplayName("业务订单号生成测试")
    class OrderIdTests {

        @Test
        @DisplayName("订单号以指定前缀开头")
        void startsWithPrefix() {
            String orderId = generateOrderId("ORD");
            assertTrue(orderId.startsWith("ORD"));
        }

        @Test
        @DisplayName("订单号包含日期时间部分")
        void containsDateTimeSegment() {
            String orderId = generateOrderId("APPT");
            // 格式: APPT + yyyyMMddHHmmss + 6位随机
            assertTrue(orderId.length() > 15);
        }

        @Test
        @DisplayName("连续两次生成结果不相同")
        void uniqueOnConsecutiveCalls() {
            assertNotEquals(generateOrderId("PAY"), generateOrderId("PAY"));
        }

        @Test
        @DisplayName("不同业务前缀互不干扰")
        void differentPrefixesIndependent() {
            String ord = generateOrderId("ORD");
            String pay = generateOrderId("PAY");
            assertTrue(ord.startsWith("ORD"));
            assertTrue(pay.startsWith("PAY"));
        }
    }

    // ==================== MD5 哈希摘要测试 ====================

    @Nested
    @DisplayName("MD5 哈希摘要测试")
    class Md5DigestTests {

        @Test
        @DisplayName("相同输入产生相同哈希值")
        void sameInputSameHash() {
            String h1 = md5Hex("hello");
            String h2 = md5Hex("hello");
            assertEquals(h1, h2);
        }

        @Test
        @DisplayName("不同输入几乎必然产生不同哈希值")
        void differentInputsDifferentHashes() {
            assertNotEquals(md5Hex("abc"), md5Hex("abd"));
        }

        @Test
        @DisplayName("哈希输出固定长度为 32 字符（小写十六进制）")
        void fixedOutputLength() {
            String hash = md5Hex("any string input here");
            assertEquals(32, hash.length());
            assertTrue(hash.matches("[0-9a-f]+"), "应全为小写十六进制字符");
        }

        @Test
        @DisplayName("空字符串也能计算 MD5")
        void emptyStringHashable() {
            String emptyHash = md5Hex("");
            assertNotNull(emptyHash);
            assertEquals(32, emptyHash.length());
        }
    }

    // ==================== 短 ID 测试 ====================

    @Nested
    @DisplayName("短 ID（紧凑型）生成测试")
    class ShortIdTests {

        @Test
        @DisplayName("短 ID 长度为 16 个十六进制字符")
        void hasSixteenHexChars() {
            String sid = shortId();
            assertEquals(16, sid.length());
            assertTrue(sid.matches("[0-9a-f]+"));
        }

        @Test
        @DisplayName("连续生成不重复")
        void noCollisionInBatch() {
            int n = 2000;
            java.util.Set<String> set = new java.util.HashSet<>();
            for (int i = 0; i < n; i++) {
                assertTrue(set.add(shortId()), "短 ID 冲突于第 " + i + " 次");
            }
        }
    }

    // ---------------- 辅助方法 ----------------

    private static int countChar(String str, char target) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == target) count++;
        }
        return count;
    }
}
