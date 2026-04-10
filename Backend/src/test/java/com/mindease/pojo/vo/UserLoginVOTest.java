package com.mindease.pojo.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户登录VO单元测试
 */
class UserLoginVOTest {

    @Test
    void testBuilderWithAllFields() {
        UserLoginVO vo = UserLoginVO.builder()
                .userId(1L)
                .username("testuser")
                .nickname("测试用户")
                .token("jwt-token-123")
                .role("user")
                .build();
        
        assertEquals(1L, vo.getUserId());
        assertEquals("testuser", vo.getUsername());
        assertEquals("测试用户", vo.getNickname());
        assertEquals("jwt-token-123", vo.getToken());
        assertEquals("user", vo.getRole());
    }

    @Test
    void testSetUserId() {
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(100L);
        assertEquals(100L, vo.getUserId());
    }

    @Test
    void testSetUsername() {
        UserLoginVO vo = new UserLoginVO();
        vo.setUsername("admin");
        assertEquals("admin", vo.getUsername());
    }

    @Test
    void testSetNickname() {
        UserLoginVO vo = new UserLoginVO();
        vo.setNickname("管理员");
        assertEquals("管理员", vo.getNickname());
    }

    @Test
    void testSetToken() {
        UserLoginVO vo = new UserLoginVO();
        vo.setToken("token123");
        assertEquals("token123", vo.getToken());
    }

    @Test
    void testSetRole() {
        UserLoginVO vo = new UserLoginVO();
        vo.setRole("admin");
        assertEquals("admin", vo.getRole());
    }

    @Test
    void testDefaultValues() {
        UserLoginVO vo = new UserLoginVO();
        assertNull(vo.getUserId());
        assertNull(vo.getUsername());
        assertNull(vo.getNickname());
        assertNull(vo.getToken());
        assertNull(vo.getRole());
    }
}
