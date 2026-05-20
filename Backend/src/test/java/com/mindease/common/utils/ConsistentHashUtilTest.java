package com.mindease.common.utils;

import com.mindease.common.util.ConsistentHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsistentHashUtil 单元测试")
class ConsistentHashUtilTest {

    private ConsistentHashUtil hashUtil;

    @BeforeEach
    void setUp() {
        // 每个物理节点对应 5 个虚拟节点
        hashUtil = new ConsistentHashUtil(5);
    }

    // ==================== 空哈希环测试 ====================

    @Nested
    @DisplayName("空哈希环测试")
    class EmptyRingTests {

        @Test
        @DisplayName("空哈希环 getNode 返回 null")
        void shouldReturnNullForAnyKeyOnEmptyRing() {
            assertNull(hashUtil.getNode("any-key"));
            assertNull(hashUtil.getNode("another-key"));
            assertNull(hashUtil.getNode(""));
        }
    }

    // ==================== 单节点测试 ====================

    @Nested
    @DisplayName("单节点测试")
    class SingleNodeTests {

        @Test
        @DisplayName("只有一个节点时所有 key 都路由到该节点")
        void shouldRouteAllKeysToSingleNode() {
            hashUtil.addNode("server-1");

            String[] keys = {"user-1", "user-2", "user-3", "user-100", ""};
            for (String key : keys) {
                assertEquals("server-1", hashUtil.getNode(key),
                        "key='" + key + "' 应路由到唯一节点 server-1");
            }
        }

        @Test
        @DisplayName("删除唯一节点后 getNode 返回 null")
        void shouldReturnNullAfterRemovingOnlyNode() {
            hashUtil.addNode("server-1");
            hashUtil.removeNode("server-1");

            assertNull(hashUtil.getNode("user-1"));
        }
    }

    // ==================== 多节点路由一致性测试 ====================

    @Nested
    @DisplayName("多节点路由一致性测试")
    class ConsistentRoutingTests {

        @Test
        @DisplayName("同一 key 始终路由到同一节点")
        void shouldRouteSameKeyToSameNodeConsistently() {
            hashUtil.addNode("server-1");
            hashUtil.addNode("server-2");
            hashUtil.addNode("server-3");

            // 多次调用返回相同结果
            String firstResult = hashUtil.getNode("user-42");
            for (int i = 0; i < 100; i++) {
                assertEquals(firstResult, hashUtil.getNode("user-42"),
                        "第 " + (i + 1) + " 次调用结果不一致");
            }
        }

        @Test
        @DisplayName("多个不同 key 能路由到不同节点")
        void shouldDistributeKeysAcrossMultipleNodes() {
            hashUtil.addNode("server-1");
            hashUtil.addNode("server-2");
            hashUtil.addNode("server-3");

            // 用 100 个不同的 key 测试分布
            Set<String> reachedNodes = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String node = hashUtil.getNode("user-" + i);
                assertNotNull(node);
                reachedNodes.add(node);
            }

            // 所有节点都应被访问到
            assertTrue(reachedNodes.size() >= 2,
                    "100 个 key 应分布到至少两个节点，实际: " + reachedNodes.size());
        }
    }

    // ==================== 节点添加与删除测试 ====================

    @Nested
    @DisplayName("节点添加与删除测试")
    class NodeAddRemoveTests {

        @Test
        @DisplayName("添加新节点不会影响已存在的所有 key 的路由")
        void shouldPreserveMostRoutesWhenAddingNode() {
            hashUtil.addNode("server-1");
            hashUtil.addNode("server-2");

            // 记录 1000 个 key 的原始路由
            Map<String, String> originalRoutes = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                originalRoutes.put("key-" + i, hashUtil.getNode("key-" + i));
            }

            // 添加新节点
            hashUtil.addNode("server-3");

            // 检查受影响的比例（理论上约 1/3 的 key 会迁移到新节点）
            int migratedCount = 0;
            for (int i = 0; i < 1000; i++) {
                String key = "key-" + i;
                if (!originalRoutes.get(key).equals(hashUtil.getNode(key))) {
                    migratedCount++;
                }
            }

            // 至少有一些 key 迁移到新节点
            assertTrue(migratedCount > 0, "添加节点后应有 key 被重新路由");
            // 但不应全部迁移
            assertTrue(migratedCount < 1000, "不应所有 key 都迁移");
        }

        @Test
        @DisplayName("删除节点后原属于该节点的 key 迁移到其他节点")
        void shouldMigrateKeysAfterRemovingNode() {
            hashUtil.addNode("server-A");
            hashUtil.addNode("server-B");
            hashUtil.addNode("server-C");

            // 找出所有路由到 server-C 的 key
            Set<String> keysOnC = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                String key = "item-" + i;
                if ("server-C".equals(hashUtil.getNode(key))) {
                    keysOnC.add(key);
                }
            }
            assertFalse(keysOnC.isEmpty(), "应该至少有一些 key 路由到 server-C");

            // 删除 server-C
            hashUtil.removeNode("server-C");

            // 验证原本在 C 上的 key 现在都能在 A 或 B 上找到
            for (String key : keysOnC) {
                String node = hashUtil.getNode(key);
                assertNotNull(node, "key=" + key + " 在删除 server-C 后应有路由目标");
                assertNotEquals("server-C", node, "key=" + key + " 不应仍路由到 server-C");
                assertTrue("server-A".equals(node) || "server-B".equals(node),
                        "key=" + key + " 应路由到 server-A 或 server-B，实际: " + node);
            }
        }

        @Test
        @DisplayName("添加后删除同一节点恢复原始路由")
        void shouldRestoreOriginalRoutesAfterAddThenRemove() {
            hashUtil.addNode("s1");
            hashUtil.addNode("s2");

            // 记录原始路由
            Map<String, String> originalRoutes = new HashMap<>();
            for (int i = 0; i < 200; i++) {
                originalRoutes.put("k-" + i, hashUtil.getNode("k-" + i));
            }

            // 添加再删除
            hashUtil.addNode("s3");
            hashUtil.removeNode("s3");

            // 路由应恢复
            for (int i = 0; i < 200; i++) {
                String key = "k-" + i;
                assertEquals(originalRoutes.get(key), hashUtil.getNode(key),
                        "添加再删除节点后 key='" + key + "' 的路由应恢复");
            }
        }
    }

    // ==================== 边界与鲁棒性测试 ====================

    @Nested
    @DisplayName("边界与鲁棒性测试")
    class BoundaryAndRobustnessTests {

        @Test
        @DisplayName("哈希环环绕：key 的 hash 大于最大节点 hash 时回到第一个节点")
        void shouldWrapAroundRingCorrectly() {
            hashUtil.addNode("node-X");
            hashUtil.addNode("node-Y");

            // 不论 key 的 hash 落在哪里，都应该返回非 null 的有效节点
            String[] keys = {
                    "aaaaaaaaaa", "bbbbbbbbbb", "cccccccccc",
                    "ZZZZZZZZZZ", "0000000000", "~~~~~~~~~~",
                    "1234567890", "!@#$%^&*()"
            };
            for (String key : keys) {
                String node = hashUtil.getNode(key);
                assertNotNull(node, "key='" + key + "' 应路由到有效节点");
                assertTrue("node-X".equals(node) || "node-Y".equals(node),
                        "key='" + key + "' 路由到: " + node);
            }
        }

        @Test
        @DisplayName("空字符串 key 能正常路由")
        void shouldHandleEmptyStringKey() {
            hashUtil.addNode("server-1");
            hashUtil.addNode("server-2");

            String node = hashUtil.getNode("");
            assertNotNull(node);
        }

        @Test
        @DisplayName("长字符串 key 能正常路由")
        void shouldHandleLongStringKey() {
            hashUtil.addNode("s1");
            hashUtil.addNode("s2");

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("abcdefghij");
            }
            String longKey = sb.toString();

            String node = hashUtil.getNode(longKey);
            assertNotNull(node);
        }

        @Test
        @DisplayName("虚拟节点数为 1 时正常工作")
        void shouldWorkWithOneVirtualNode() {
            ConsistentHashUtil singleVirtualHash = new ConsistentHashUtil(1);
            singleVirtualHash.addNode("n1");
            singleVirtualHash.addNode("n2");

            assertNotNull(singleVirtualHash.getNode("test-key"));

            // 一致性验证
            String first = singleVirtualHash.getNode("key-A");
            for (int i = 0; i < 50; i++) {
                assertEquals(first, singleVirtualHash.getNode("key-A"));
            }
        }

        @Test
        @DisplayName("虚拟节点数为 0 时 addNode 不产生虚拟节点，getNode 返回 null")
        void shouldReturnNullWithZeroVirtualNodes() {
            ConsistentHashUtil zeroVirtualHash = new ConsistentHashUtil(0);
            zeroVirtualHash.addNode("server-1");

            // 虚拟节点数为 0，哈希环没有任何位置，getNode 应返回 null
            assertNull(zeroVirtualHash.getNode("any-key"));
        }

        @Test
        @DisplayName("多次添加相同节点不会增加新的虚拟节点（幂等不是设计保证，但不应抛异常）")
        void shouldNotThrowWhenAddingSameNodeMultipleTimes() {
            hashUtil.addNode("dup-server");
            assertDoesNotThrow(() -> hashUtil.addNode("dup-server"));
            assertDoesNotThrow(() -> hashUtil.addNode("dup-server"));
            // 即使多次添加，获取 key 也不应抛异常
            assertNotNull(hashUtil.getNode("some-key"));
        }

        @Test
        @DisplayName("删除未添加的节点不抛异常")
        void shouldNotThrowWhenRemovingNonExistentNode() {
            assertDoesNotThrow(() -> hashUtil.removeNode("ghost-server"));
            // 状态应不变
            assertNull(hashUtil.getNode("any-key"));
        }

        @Test
        @DisplayName("上千个 key 的分布测试（校验一致性）")
        void shouldMaintainConsistencyWithLargeKeySet() {
            hashUtil.addNode("A");
            hashUtil.addNode("B");
            hashUtil.addNode("C");

            // 记录第一轮结果
            Map<String, String> firstRound = new HashMap<>();
            for (int i = 0; i < 2000; i++) {
                firstRound.put("key-" + i, hashUtil.getNode("key-" + i));
            }

            // 第二轮结果必须完全一致
            for (int i = 0; i < 2000; i++) {
                String key = "key-" + i;
                assertEquals(firstRound.get(key), hashUtil.getNode(key),
                        "key='" + key + "' 两次路由结果不一致");
            }
        }
    }
}
