package com.mindease.pojo.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户注册DTO单元测试
 */
class UserRegisterDTOTest {

    @Test
    void testSetUsername() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newuser");
        assertEquals("newuser", dto.getUsername());
    }

    @Test
    void testSetPassword() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setPassword("secure123");
        assertEquals("secure123", dto.getPassword());
    }

    @Test
    void testSetNickname() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setNickname("新用户");
        assertEquals("新用户", dto.getNickname());
    }

    @Test
    void testSetPhone() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setPhone("13800138000");
        assertEquals("13800138000", dto.getPhone());
    }

    @Test
    void testSetRole() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setRole("user");
        assertEquals("user", dto.getRole());
    }

    @Test
    void testAllFields() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("user1");
        dto.setPassword("pass123");
        dto.setNickname("用户一");
        dto.setPhone("13900001111");
        dto.setRole("counselor");
        
        assertEquals("user1", dto.getUsername());
        assertEquals("pass123", dto.getPassword());
        assertEquals("用户一", dto.getNickname());
        assertEquals("13900001111", dto.getPhone());
        assertEquals("counselor", dto.getRole());
    }

    @Test
    void testDefaultValues() {
        UserRegisterDTO dto = new UserRegisterDTO();
        assertNull(dto.getUsername());
        assertNull(dto.getPassword());
        assertNull(dto.getNickname());
        assertNull(dto.getPhone());
        assertNull(dto.getRole());
    }
}
