package com.mindease.service;

import com.mindease.pojo.dto.AuditProcessDTO;
import com.mindease.pojo.dto.AuditSubmitDTO;
import com.mindease.pojo.vo.AuditListVO;
import com.mindease.pojo.vo.AuditStatusVO;
import com.mindease.pojo.vo.AuditSubmitVO;

public interface CounselorAuditService {

    /**
     * 提交资质审核
     *
     * @param userId
     * @param submitDTO
     * @return
     */
    AuditSubmitVO submitAudit(Long userId, AuditSubmitDTO submitDTO);

    /**
     * 获取审核状态
     *
     * @param userId
     * @return
     */
    AuditStatusVO getAuditStatus(Long userId);

    /**
     * 获取待审核列表
     *
     * @param page
     * @param pageSize
     * @return
     */
    AuditListVO getPendingAuditList(Integer page, Integer pageSize);

    /**
     * 处理审核（通过/拒绝）
     *
     * @param adminId
     * @param processDTO
     */
    void processAudit(Long adminId, AuditProcessDTO processDTO);
}

