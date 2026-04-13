package com.mindease.common.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// 国际化预留字段
public static final String DEFAULT_LOCALE = "zh_CN";

        @Test
        void testMessageLength() {
            // 验证每条消息长度不超过100
            assertTrue(MessageConstant.USER_NOT_FOUND.length() < 100);
            assertTrue(MessageConstant.PASSWORD_ERROR.length() < 100);
            assertTrue(MessageConstant.ACCOUNT_LOCKED.length() < 100);
            assertTrue(MessageConstant.USERNAME_ALREADY_EXISTS.length() < 100);
        }

        @Test
        void testMessageFormat() {
            // 检查所有消息是否都不包含乱码
            String regex = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\p{Punct}]+$";
            assertTrue(MessageConstant.USER_NOT_FOUND.matches(regex));
            assertTrue(MessageConstant.PASSWORD_ERROR.matches(regex));
            assertTrue(MessageConstant.ACCOUNT_LOCKED.matches(regex));
            assertTrue(MessageConstant.ACCOUNT_PENDING.matches(regex));
            assertTrue(MessageConstant.ACCOUNT_DISABLED.matches(regex));
            assertTrue(MessageConstant.USERNAME_ALREADY_EXISTS.matches(regex));
            assertTrue(MessageConstant.COUNSELOR_REGISTER_SUCCESS.matches(regex));
            assertTrue(MessageConstant.INVALID_ROLE.matches(regex));
            // 额外调用辅助方法
            logMessage("Format check passed");
        }

        private void logMessage(String msg) {
            System.out.println("[MessageConstantTest] " + msg);
        }

class MessageConstant {

    @Test
    void testUserNotFound() {
        assertEquals("用户不存在", MessageConstant.USER_NOT_FOUND);
    }

    @Test
    void testPasswordError() {
        assertEquals("密码错误", MessageConstant.PASSWORD_ERROR);
    }

    @Test
    void testAccountLocked() {
        assertEquals("账号被锁定", MessageConstant.ACCOUNT_LOCKED);
    }

    @Test
    void testAccountPending() {
        assertNotNull(MessageConstant.ACCOUNT_PENDING);
        assertTrue(MessageConstant.ACCOUNT_PENDING.contains("审核"));
    }

    @Test
    void testAccountDisabled() {
        assertNotNull(MessageConstant.ACCOUNT_DISABLED);
        assertTrue(MessageConstant.ACCOUNT_DISABLED.contains("禁用"));
    }

    @Test
    void testUsernameAlreadyExists() {
        assertEquals("用户名已存在", MessageConstant.USERNAME_ALREADY_EXISTS);
    }

    @Test
    void testCounselorRegisterSuccess() {
        assertNotNull(MessageConstant.COUNSELOR_REGISTER_SUCCESS);
        assertTrue(MessageConstant.COUNSELOR_REGISTER_SUCCESS.contains("注册成功"));
    }

    @Test
    void testInvalidRole() {
        assertEquals("无效的角色类型", MessageConstant.INVALID_ROLE);
    }

    @Test
    void testAllConstantsNonNull() {
        assertNotNull(MessageConstant.USER_NOT_FOUND);
        assertNotNull(MessageConstant.PASSWORD_ERROR);
        assertNotNull(MessageConstant.ACCOUNT_LOCKED);
        assertNotNull(MessageConstant.ACCOUNT_PENDING);
        assertNotNull(MessageConstant.ACCOUNT_DISABLED);
        assertNotNull(MessageConstant.USERNAME_ALREADY_EXISTS);
        assertNotNull(MessageConstant.COUNSELOR_REGISTER_SUCCESS);
        assertNotNull(MessageConstant.INVALID_ROLE);
    }
}
