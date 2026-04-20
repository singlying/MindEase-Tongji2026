package com.mindease.common.constant;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态常量单元测试
 */
class StatusConstantTest {

    @Test
    void testEnableConstant() {
        assertEquals(1, StatusConstant.ENABLE);
    }

    @Test
    void testPendingConstant() {
        assertEquals(2, StatusConstant.PENDING);
    }

    @Test
    void testDisableConstant() {
        assertEquals(0, StatusConstant.DISABLE);
    }

    @Test
    void testConstantValues() {
        assertTrue(StatusConstant.ENABLE > StatusConstant.DISABLE);
        assertTrue(StatusConstant.PENDING > StatusConstant.ENABLE);
    }
}
