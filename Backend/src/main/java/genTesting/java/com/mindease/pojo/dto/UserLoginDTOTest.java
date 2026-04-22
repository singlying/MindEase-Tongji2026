package com.mindease.pojo.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户登录DTO单元测试
 */
class UserLoginDTOTest {

    @Test
    void testSetUsername() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("testuser");
        assertEquals("testuser", dto.getUsername());
    }

    @Test
    void testSetPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setPassword("password123");
        assertEquals("password123", dto.getPassword());
    }

    @Test
    void testUsernameAndPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");
        assertEquals("admin", dto.getUsername());
        assertEquals("admin123", dto.getPassword());
    }

    @Test
    void testDefaultValues() {
        UserLoginDTO dto = new UserLoginDTO();
        assertNull(dto.getUsername());
        assertNull(dto.getPassword());
    }

    @Test
    void testEmptyString() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("");
        dto.setPassword("");
        assertEquals("", dto.getUsername());
        assertEquals("", dto.getPassword());
    }
}
