package com.mindease.common.utils;

import com.mindease.common.util.LRUCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LRUCache 单元测试")
class LRUCacheTest {

    private LRUCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>(3);
    }

    // ==================== 基础存取测试 ====================

    @Nested
    @DisplayName("基础存取测试")
    class BasicPutGetTests {

        @Test
        @DisplayName("put 后 get 可获取到存入的值")
        void shouldGetStoredValue() {
            cache.put("a", "A");
            assertEquals("A", cache.get("a"));
        }

        @Test
        @DisplayName("存入多条数据后均能正确获取")
        void shouldGetMultipleStoredValues() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            assertEquals("A", cache.get("a"));
            assertEquals("B", cache.get("b"));
            assertEquals("C", cache.get("c"));
        }

        @Test
        @DisplayName("相同 key 重复 put 会覆盖旧值")
        void shouldOverwriteValueWhenSameKeyPutAgain() {
            cache.put("a", "old");
            cache.put("a", "new");
            assertEquals("new", cache.get("a"));
            assertEquals(1, cache.size());
        }

        @Test
        @DisplayName("空缓存 size 为 0")
        void shouldHaveZeroSizeWhenEmpty() {
            assertEquals(0, cache.size());
        }
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况测试")
    class BoundaryTests {

        @Test
        @DisplayName("获取不存在的 key 返回 null")
        void shouldReturnNullForNonExistentKey() {
            assertNull(cache.get("nonexistent"));
        }

        @Test
        @DisplayName("空缓存 containsKey 返回 false")
        void shouldReturnFalseForNonExistentKeyCheck() {
            assertFalse(cache.containsKey("missing"));
        }

        @Test
        @DisplayName("已存在的 key 调用 containsKey 返回 true")
        void shouldReturnTrueForExistentKeyCheck() {
            cache.put("a", "A");
            assertTrue(cache.containsKey("a"));
        }

        @Test
        @DisplayName("用完容量后再获取不会抛出异常")
        void shouldNotThrowWhenGettingAtCapacity() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            // 已满，获取应正常返回
            assertNotNull(cache.get("a"));
            assertNotNull(cache.get("b"));
            assertNotNull(cache.get("c"));
        }

        @Test
        @DisplayName("容量为 1 的缓存能正常工作")
        void shouldWorkWithCapacityOfOne() {
            LRUCache<Integer, String> tinyCache = new LRUCache<>(1);
            tinyCache.put(1, "one");
            assertEquals("one", tinyCache.get(1));
            assertEquals(1, tinyCache.size());

            tinyCache.put(2, "two");
            assertEquals(1, tinyCache.size());
            assertNull(tinyCache.get(1), "旧值应被淘汰");
            assertEquals("two", tinyCache.get(2));
        }
    }

    // ==================== LRU 淘汰机制测试 ====================

    @Nested
    @DisplayName("LRU 淘汰机制测试")
    class LruEvictionTests {

        @Test
        @DisplayName("超过容量时淘汰最久未使用的条目")
        void shouldEvictLeastRecentlyUsedWhenExceedingCapacity() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            // a 是最久未使用的
            cache.put("d", "D");

            assertEquals(3, cache.size());
            assertNull(cache.get("a"), "a 应被淘汰");
            assertNotNull(cache.get("b"));
            assertNotNull(cache.get("c"));
            assertNotNull(cache.get("d"));
        }

        @Test
        @DisplayName("访问条目会重置其 LRU 位置，使其不被淘汰")
        void shouldPromoteEntryOnAccess() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");

            // 访问 a，使其从最久未使用变为最近使用
            cache.get("a");

            // 此时 b 是最久未使用的
            cache.put("d", "D");

            assertEquals(3, cache.size());
            assertNotNull(cache.get("a"), "a 因被访问过不应被淘汰");
            assertNull(cache.get("b"), "b 应被淘汰");
            assertNotNull(cache.get("c"));
            assertNotNull(cache.get("d"));
        }

        @Test
        @DisplayName("多次访问不同条目正确维护 LRU 顺序")
        void shouldMaintainCorrectLruOrderAfterMultipleAccesses() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");

            cache.get("a"); // a 最近使用
            cache.get("c"); // c 最近使用
            cache.get("a"); // a 最近使用
            // 此时 b 是最久未使用

            cache.put("d", "D");
            assertNull(cache.get("b"), "b 应被淘汰");
            assertNotNull(cache.get("a"));
            assertNotNull(cache.get("c"));
            assertNotNull(cache.get("d"));
        }

        @Test
        @DisplayName("put 相同 key 会将其视为最近使用")
        void shouldPromoteOnOverwrite() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");

            // 覆盖写入 a，将其变为最近使用
            cache.put("a", "A2");
            // b 是最久未使用
            cache.put("d", "D");

            assertNotNull(cache.get("a"));
            assertNull(cache.get("b"), "b 应被淘汰");
            assertNotNull(cache.get("c"));
            assertNotNull(cache.get("d"));
            assertEquals("A2", cache.get("a"));
        }

        @Test
        @DisplayName("containsKey 访问不会改变 LRU 顺序")
        void shouldNotPromoteOnContainsKey() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");

            // containsKey 查询 a，但不应改变 LRU 顺序
            assertTrue(cache.containsKey("a"));

            // a 仍是最久未使用
            cache.put("d", "D");

            assertNull(cache.get("a"), "containsKey 不应影响 LRU 顺序，a 仍应被淘汰");
            assertNotNull(cache.get("b"));
            assertNotNull(cache.get("c"));
            assertNotNull(cache.get("d"));
        }

        @Test
        @DisplayName("依次淘汰多条数据")
        void shouldEvictEntriesSequentially() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            cache.put("d", "D"); // 淘汰 a
            cache.put("e", "E"); // 淘汰 b
            cache.put("f", "F"); // 淘汰 c

            assertEquals(3, cache.size());
            assertNull(cache.get("a"));
            assertNull(cache.get("b"));
            assertNull(cache.get("c"));
            assertNotNull(cache.get("d"));
            assertNotNull(cache.get("e"));
            assertNotNull(cache.get("f"));
        }
    }

    // ==================== clear 测试 ====================

    @Nested
    @DisplayName("clear 清空测试")
    class ClearTests {

        @Test
        @DisplayName("clear 后 size 为 0")
        void shouldHaveZeroSizeAfterClear() {
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");

            cache.clear();

            assertEquals(0, cache.size());
        }

        @Test
        @DisplayName("clear 后原有 key 均不可访问")
        void shouldNotAccessOldKeysAfterClear() {
            cache.put("a", "A");
            cache.put("b", "B");

            cache.clear();

            assertNull(cache.get("a"));
            assertNull(cache.get("b"));
            assertFalse(cache.containsKey("a"));
        }

        @Test
        @DisplayName("clear 后可以重新存入新数据")
        void shouldBeReusableAfterClear() {
            cache.put("a", "A");
            cache.clear();
            cache.put("x", "X");
            cache.put("y", "Y");

            assertEquals(2, cache.size());
            assertEquals("X", cache.get("x"));
            assertEquals("Y", cache.get("y"));
        }
    }
}
