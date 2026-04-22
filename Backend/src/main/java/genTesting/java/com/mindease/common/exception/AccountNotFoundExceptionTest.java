package com.mindease.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 账号不存在异常单元测试
 */
class AccountNotFoundExceptionTest {

    @Test
    void testDefaultConstructor() {
        AccountNotFoundException exception = new AccountNotFoundException();
        assertNull(exception.getMessage());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    void testMessageConstructor() {
        String message = "用户不存在";
        AccountNotFoundException exception = new AccountNotFoundException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testInheritanceChain() {
        AccountNotFoundException exception = new AccountNotFoundException();
        assertTrue(exception instanceof BaseException);
        assertTrue(exception instanceof RuntimeException);
    }
}
