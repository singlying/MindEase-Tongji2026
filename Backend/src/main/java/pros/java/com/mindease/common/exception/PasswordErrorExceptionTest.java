package com.mindease.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码错误异常单元测试
 */
class PasswordErrorExceptionTest {

    @Test
    void testDefaultConstructor() {
        PasswordErrorException exception = new PasswordErrorException();
        assertNull(exception.getMessage());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    void testMessageConstructor() {
        String message = "密码错误";
        PasswordErrorException exception = new PasswordErrorException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testInheritanceChain() {
        PasswordErrorException exception = new PasswordErrorException();
        assertTrue(exception instanceof BaseException);
        assertTrue(exception instanceof RuntimeException);
    }
}
