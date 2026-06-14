package com.mindease.pojo.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作结果VO单元测试
 */
class OperationResultVOTest {

    @Test
    void testBuilder() {
        OperationResultVO vo = OperationResultVO.builder()
                .success(true)
                .build();
        assertTrue(vo.getSuccess());
    }

    @Test
    void testSetSuccess() {
        OperationResultVO vo = new OperationResultVO();
        vo.setSuccess(false);
        assertFalse(vo.getSuccess());
    }

    @Test
    void testDefaultValue() {
        OperationResultVO vo = new OperationResultVO();
        assertNull(vo.getSuccess());
    }

    @Test
    void testSuccessTrue() {
        OperationResultVO vo = OperationResultVO.builder()
                .success(true)
                .build();
        assertTrue(vo.getSuccess());
    }

    @Test
    void testSuccessFalse() {
        OperationResultVO vo = OperationResultVO.builder()
                .success(false)
                .build();
        assertFalse(vo.getSuccess());
    }
}
