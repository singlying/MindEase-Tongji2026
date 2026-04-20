package com.mindease.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 账号被锁定异常单元测试
 */
class AccountLockedExceptionTest {

    @Test
    void testDefaultConstructor() {
        AccountLockedException exception = new AccountLockedException();
        assertNull(exception.getMessage());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    void testMessageConstructor() {
        String message = "账号已被锁定";
        AccountLockedException exception = new AccountLockedException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testNullMessage() {
        AccountLockedException exception = new AccountLockedException(null);
        assertNull(exception.getMessage());
    }

    @Test
    void testInheritanceChain() {
        AccountLockedException exception = new AccountLockedException();
        assertTrue(exception instanceof BaseException);
        assertTrue(exception instanceof RuntimeException);
    }
}
