package com.mindease.pojo.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 聊天删除VO单元测试
 */
class ChatDeleteVOTest {

    @Test
    void testSetSuccess() {
        ChatDeleteVO vo = new ChatDeleteVO();
        vo.setSuccess(true);
        assertTrue(vo.getSuccess());
    }

    @Test
    void testSetSuccessFalse() {
        ChatDeleteVO vo = new ChatDeleteVO();
        vo.setSuccess(false);
        assertFalse(vo.getSuccess());
    }

    @Test
    void testDefaultValue() {
        ChatDeleteVO vo = new ChatDeleteVO();
        assertNull(vo.getSuccess());
    }
}
