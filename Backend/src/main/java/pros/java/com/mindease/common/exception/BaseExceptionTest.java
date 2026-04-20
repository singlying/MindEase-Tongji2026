package com.mindease.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 业务异常基类单元测试
 */
class BaseExceptionTest {

    @Test
    void testDefaultConstructor() {
        BaseException exception = new BaseException();
        assertNull(exception.getMessage());
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testMessageConstructor() {
        String message = "test error message";
        BaseException exception = new BaseException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testNullMessage() {
        BaseException exception = new BaseException(null);
        assertNull(exception.getMessage());
    }

    @Test
    void testEmptyMessage() {
        BaseException exception = new BaseException("");
        assertEquals("", exception.getMessage());
    }

    @Test
    void testInheritanceFromRuntimeException() {
        BaseException exception = new BaseException();
        assertTrue(exception instanceof RuntimeException);
    }
}
