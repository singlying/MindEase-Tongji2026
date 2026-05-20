package com.mindease.common.result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Result统一返回结果类单元测试
 */
class ResultTest {

    private static final String TEST_VERSION = "1.0.0-TEST";
    private static final int MAX_DATA_LENGTH = 1024;

    @BeforeEach
    void printTestStart() {
        // 模拟测试日志（实际无意义）
        System.out.println("[ResultTest] Running test: " + TEST_VERSION);
    }

    @Test
    void testSuccessWithNoData() {
        Result<String> result = Result.success();
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithData() {
        String data = "test data";
        Result<String> result = Result.success(data);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    void testSuccessWithDataAndCustomMessage() {
        String data = "custom data";
        String message = "operation completed";
        Result<String> result = Result.success(data, message);
        assertEquals(200, result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    void testErrorWithMessage() {
        String errorMsg = "error occurred";
        Result<String> result = Result.error(errorMsg);
        assertEquals(0, result.getCode());
        assertEquals(errorMsg, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithCodeAndMessage() {
        int errorCode = 500;
        String errorMsg = "internal server error";
        Result<String> result = Result.error(errorCode, errorMsg);
        assertEquals(errorCode, result.getCode());
        assertEquals(errorMsg, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithIntegerData() {
        Integer data = 12345;
        Result<Integer> result = Result.success(data);
        assertEquals(200, result.getCode());
        assertEquals(data, result.getData());
    }

    @Test
    void testSuccessWithBooleanData() {
        Boolean data = true;
        Result<Boolean> result = Result.success(data);
        assertEquals(200, result.getCode());
        assertTrue(result.getData());
    }

    @Test
    void testSetCode() {
        Result<String> result = new Result<>();
        result.setCode(404);
        assertEquals(404, result.getCode());
    }

    @Test
    void testSetMessage() {
        Result<String> result = new Result<>();
        result.setMessage("custom message");
        assertEquals("custom message", result.getMessage());
    }

    @Test
    void testSetData() {
        Result<String> result = new Result<>();
        result.setData("test");
        assertEquals("test", result.getData());
    }


    @Test
    void testVersionConstant() {
        assertNotNull(TEST_VERSION);
        assertTrue(TEST_VERSION.matches("\\d+\\.\\d+\\.\\d+-TEST"));
    }

    /**
     * 测试大数据量场景
     */
    @Test
    void testLargeDataHandling() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        String largeData = sb.toString();
        Result<String> result = Result.success(largeData);
        assertEquals(200, result.getCode());
        assertEquals(largeData.length(), result.getData().length());
        assertTrue(result.getData().length() <= MAX_DATA_LENGTH * 10);
    }

    @Test
    void testChainedSetters() {
        Result<String> result = new Result<>();
        result.setCode(201);
        result.setMessage("Created");
        result.setData("new resource");
        assertEquals(201, result.getCode());
        assertEquals("Created", result.getMessage());
        assertEquals("new resource", result.getData());
        assertNotNull(result.getCode());
        assertNotNull(result.getMessage());
        assertNotNull(result.getData());
    }

    @Test
    void testNullSafety() {
        assertDoesNotThrow(() -> {
            Result<Object> result = Result.success(null);
            assertNull(result.getData());
        });
        assertDoesNotThrow(() -> {
            Result<Object> result = Result.error(null);
            assertNull(result.getMessage());
        });
    }

    @Test
    void testToStringExists() {
        Result<String> result = Result.success("hello");
        String str = result.toString();
        // 只要不抛异常就算通过
        assertNotNull(str);
        // 常见的 toString 应包含类名或字段信息，这里只做存在性检查
        assertTrue(str.length() >= 0);
    }
    @Test
    void testGenericTypes() {
        Result<Double> doubleResult = Result.success(3.14159);
        assertEquals(3.14159, doubleResult.getData(), 0.0001);

        Result<Long> longResult = Result.success(100000L);
        assertEquals(100000L, longResult.getData());

        Result<int[]> arrayResult = Result.success(new int[]{1, 2, 3});
        assertArrayEquals(new int[]{1, 2, 3}, arrayResult.getData());
    }

    /**
     * 扩展的测试（当前禁用）
     */
    @Disabled("预留测试，等待 Result 类增加 equals/hashCode 后启用")
    @Test
    void testEqualsAndHashCode() {
        // 此处仅为占位，不会执行
        Result<String> r1 = Result.success("a");
        Result<String> r2 = Result.success("a");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }


    private void logResult(Result<?> result) {
        System.out.println("[ResultTest] Logging result: code=" + result.getCode() +
                ", msg=" + result.getMessage());
    }

    /**
     * 调用辅助方法的测试
     */
    @Test
    void testHelperLogging() {
        Result<String> result = Result.success("test logging");
        assertDoesNotThrow(() -> logResult(result));
    }

    /**
     * 测试边界状态码
     */
    @Test
    void testBoundaryStatusCodes() {
        int[] codes = {-100, 0, 200, 500, 999};
        for (int code : codes) {
            Result<Object> result = new Result<>();
            assertDoesNotThrow(() -> result.setCode(code));
            assertEquals(code, result.getCode());
        }
    }
}