package com.mindease.common.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自定义注解元数据完整性单元测试
 * <p>
 * 验证 RequireRole / RateLimiter / LogAction / Cacheable 四个注解的
 * 默认值、属性类型、枚举范围、反射可读性等元数据层面的正确性。
 * 不涉及 AOP 切面或运行时代理逻辑。
 * </p>
 */
@DisplayName("自定义注解元数据验证")
class CustomAnnotationTest {

    // ==================== RequireRole 注解测试 ====================

    @Nested
    @DisplayName("RequireRole 注解元数据")
    class RequireRoleAnnotationTests {

        @Test
        @DisplayName("注解可在方法和类上使用")
        void targetMethodAndType() {
            ElementType[] targets = RequireRole.class.getAnnotation(Target.class).value();
            assertTrue(java.util.Arrays.asList(targets).contains(ElementType.METHOD));
            assertTrue(java.util.Arrays.asList(targets).contains(ElementType.TYPE));
        }

        @Test
        @DisplayName("运行时可通过反射读取注解信息")
        void runtimeRetentionPolicy() {
            assertEquals(RetentionPolicy.RUNTIME,
                    RequireRole.class.getAnnotation(Retention.class).value());
        }

        @Test
        @DisplayName("Logical 枚举含 OR 和 AND 两个值")
        void logicalEnumValues() {
            assertEquals(2, RequireRole.Logical.values().length);
            assertNotNull(RequireRole.Logical.OR);
            assertNotNull(RequireRole.Logical.AND);
        }

        @Test
        @DisplayName("默认 logical 为 OR")
        void defaultLogicalIsOr() throws NoSuchMethodException {
            java.lang.reflect.Method m = RequireRole.class.getDeclaredMethod("logical");
            Object dv = m.getDefaultValue();
            assertSame(RequireRole.Logical.OR, dv);
        }

        @Test
        @DisplayName("默认 message 为权限不足")
        void defaultMessage() throws NoSuchMethodException {
            java.lang.reflect.Method m = RequireRole.class.getDeclaredMethod("message");
            assertEquals("权限不足", m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 priority 为 0")
        void defaultPriorityZero() throws NoSuchMethodException {
            java.lang.reflect.Method m = RequireRole.class.getDeclaredMethod("priority");
            assertEquals(0, m.getDefaultValue());
        }

        @Test
        @DisplayName("value 属性为 String 数组类型")
        void valueIsStringArray() throws NoSuchMethodException {
            assertEquals(String[].class,
                    RequireRole.class.getDeclaredMethod("value").getReturnType());
        }
    }

    // ==================== RateLimiter 注解测试 ====================

    @Nested
    @DisplayName("RateLimiter 注解元数据")
    class RateLimiterAnnotationTests {

        @Test
        @DisplayName("仅允许标注于方法上")
        void targetMethodOnly() {
            ElementType[] targets = RateLimiter.class.getAnnotation(Target.class).value();
            assertEquals(1, targets.length);
            assertEquals(ElementType.METHOD, targets[0]);
        }

        @Test
        @DisplayName("FallbackAction 枚举含 4 个策略值")
        void fallbackActionEnumHasFourValues() {
            assertEquals(4, RateLimiter.FallbackAction.values().length);
        }

        @Test
        @DisplayName("各 FallbackAction 值名称符合预期")
        void fallbackActionNamesCorrect() {
            String[] names = {"REJECT", "REJECT_WITH_CODE", "WARN_AND_PASS", "QUEUE_AND_WAIT"};
            for (String name : names) {
                assertNotNull(RateLimiter.FallbackAction.valueOf(name));
            }
        }

        @Test
        @DisplayName("默认 window 为 60")
        void defaultWindowSixty() throws Exception {
            java.lang.reflect.Method m = RateLimiter.class.getDeclaredMethod("window");
            assertEquals(60L, m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 timeUnit 为 SECONDS")
        void defaultTimeUnitSeconds() throws Exception {
            java.lang.reflect.Method m = RateLimiter.class.getDeclaredMethod("timeUnit");
            assertEquals(TimeUnit.SECONDS, m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 burst 为 0（不启用突发）")
        void defaultBurstZero() throws Exception {
            java.lang.reflect.Method m = RateLimiter.class.getDeclaredMethod("burst");
            assertEquals(0, m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 adminBypass 为 false")
        void defaultAdminBypassFalse() throws Exception {
            java.lang.reflect.Method m = RateLimiter.class.getDeclaredMethod("adminBypass");
            assertEquals(false, m.getDefaultValue());
        }

        @Test
        @DisplayName("limit 无默认值（必填）")
        void limitIsRequired() throws Exception {
            java.lang.reflect.Method m = RateLimiter.class.getDeclaredMethod("limit");
            assertNull(m.getDefaultValue(), "limit 应为必填属性");
        }
    }

    // ==================== LogAction 注解测试 ====================

    @Nested
    @DisplayName("LogAction 注解元数据")
    class LogActionAnnotationTests {

        @Test
        @DisplayName("AuditLevel 枚举含 4 个级别")
        void auditLevelEnumHasFourValues() {
            assertEquals(4, LogAction.AuditLevel.values().length);
        }

        @Test
        @DisplayName("AuditLevel 等级顺序从低到高排列")
        void auditLevelOrdering() {
            LogAction.AuditLevel[] levels = LogAction.AuditLevel.values();
            assertEquals(LogAction.AuditLevel.LOW, levels[0]);
            assertEquals(LogAction.AuditLevel.NORMAL, levels[1]);
            assertEquals(LogAction.AuditLevel.HIGH, levels[2]);
            assertEquals(LogAction.AuditLevel.CRITICAL, levels[3]);
        }

        @Test
        @DisplayName("LogStore 枚举含 3 种策略")
        void logStoreEnumHasThreeValues() {
            assertEquals(3, LogAction.LogStore.values().length);
        }

        @Test
        @DisplayName("默认 store 为 DATABASE")
        void defaultStoreDatabase() throws Exception {
            java.lang.reflect.Method m = LogAction.class.getDeclaredMethod("store");
            assertEquals(LogAction.LogStore.DATABASE, m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 recordParams 为 true")
        void defaultRecordParamsTrue() throws Exception {
            java.lang.reflect.Method m = LogAction.class.getDeclaredMethod("recordParams");
            assertEquals(true, m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 level 为 NORMAL")
        void defaultLevelNormal() throws Exception {
            java.lang.reflect.Method m = LogAction.class.getDeclaredMethod("level");
            assertEquals(LogAction.AuditLevel.NORMAL, m.getDefaultValue());
        }

        @Test
        @DisplayName("默认 excludeParams 为空数组")
        void defaultExcludeParamsEmpty() throws Exception {
            java.lang.reflect.Method m = LogAction.class.getDeclaredMethod("excludeParams");
            Object defVal = m.getDefaultValue();
            assertNotNull(defVal);
            assertTrue(defVal instanceof String[]);
            assertEquals(0, ((String[]) defVal).length);
        }

        @Test
        @DisplayName("action 为必填属性")
        void actionIsRequired() throws Exception {
            java.lang.reflect.Method m = LogAction.class.getDeclaredMethod("action");
            assertNull(m.getDefaultValue());
        }
    }

    // ==================== 注解组合模拟测试 ====================

    @Nested
    @DisplayName("注解组合使用场景模拟")
    class AnnotationCombinationTests {

        /**
         * 模拟一个同时使用多个注解的方法
         */
        @RequireRole(value = "ADMIN", priority = 5)
        @RateLimiter(limit = 20, window = 60, fallback = RateLimiter.FallbackAction.REJECT_WITH_CODE)
        @LogAction(action = "导出数据", module = "数据分析", level = LogAction.AuditLevel.CRITICAL,
                   recordParams = false, excludeParams = {"token"})
        void simulatedAnnotatedMethod() {}

        @Test
        @DisplayName("方法上可同时叠加三个自定义注解")
        void multipleAnnotationsCoexist() throws Exception {
            var method = getClass().getDeclaredMethod("simulatedAnnotatedMethod");
            assertNotNull(method.getAnnotation(RequireRole.class));
            assertNotNull(method.getAnnotation(RateLimiter.class));
            assertNotNull(method.getAnnotation(LogAction.class));
        }

        @Test
        @DisplayName("RequireRole 组合值可正确读取")
        void readCombinedRequireRole() throws Exception {
            var method = getClass().getDeclaredMethod("simulatedAnnotatedMethod");
            RequireRole rr = method.getAnnotation(RequireRole.class);
            assertArrayEquals(new String[]{"ADMIN"}, rr.value());
            assertEquals(5, rr.priority());
            assertEquals(RequireRole.Logical.OR, rr.logical());
        }

        @Test
        @DisplayName("RateLimiter 组合值可正确读取")
        void readCombinedRateLimiter() throws Exception {
            var method = getClass().getDeclaredMethod("simulatedAnnotatedMethod");
            RateLimiter rl = method.getAnnotation(RateLimiter.class);
            assertEquals(20, rl.limit());
            assertEquals(60L, rl.window());
            assertEquals(TimeUnit.SECONDS, rl.timeUnit());
            assertEquals(RateLimiter.FallbackAction.REJECT_WITH_CODE, rl.fallback());
        }

        @Test
        @DisplayName("LogAction 组合值可正确读取")
        void readCombinedLogAction() throws Exception {
            var method = getClass().getDeclaredMethod("simulatedAnnotatedMethod");
            LogAction la = method.getAnnotation(LogAction.class);
            assertEquals("导出数据", la.action());
            assertEquals("数据分析", la.module());
            assertEquals(LogAction.AuditLevel.CRITICAL, la.level());
            assertFalse(la.recordParams());
            assertArrayEquals(new String[]{"token"}, la.excludeParams());
        }
    }
}
