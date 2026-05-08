package com.mindease.controller.counselor;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.AuditSubmitDTO;
import com.mindease.pojo.vo.AuditStatusVO;
import com.mindease.pojo.vo.AuditSubmitVO;
import com.mindease.service.CounselorAuditService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 咨询师资质审核控制器（用户端）
 */
@RestController
@RequestMapping("/counselor/audit")
@Slf4j
public class CounselorAuditController {

    @Autowired
    private CounselorAuditService auditService;

    /**
     * 提交资质审核
     *
     * @param userId 当前用户ID（从token中获取）
     * @param submitDTO
     * @return
     */
    @PostMapping("/submit")
    public Result<AuditSubmitVO> submitAudit(@RequestAttribute Long userId,
                                              @Valid @RequestBody AuditSubmitDTO submitDTO) {
        log.info("提交资质审核，用户ID:{}，数据:{}", userId, submitDTO);

        AuditSubmitVO result = auditService.submitAudit(userId, submitDTO);

        return Result.success(result, "资质提交成功，请等待管理员审核");
    }

    /**
     * 获取审核状态
     *
     * @param userId 当前用户ID（从token中获取）
     * @return
     */
    @GetMapping("/status")
    public Result<AuditStatusVO> getAuditStatus(@RequestAttribute Long userId) {
        log.info("获取审核状态，用户ID:{}", userId);

        AuditStatusVO status = auditService.getAuditStatus(userId);

        return Result.success(status);
    }
}

