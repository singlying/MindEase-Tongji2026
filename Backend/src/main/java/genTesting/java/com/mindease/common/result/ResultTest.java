package com.mindease.common.result;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Result统一返回结果类单元测试
 */
class ResultTest {

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
}
