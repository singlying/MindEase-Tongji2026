package com.mindease.controller.admin;

import com.mindease.common.exception.BaseException;
import com.mindease.common.result.Result;
import com.mindease.pojo.dto.AuditProcessDTO;
import com.mindease.pojo.vo.AuditListVO;
import com.mindease.pojo.vo.OperationResultVO;
import com.mindease.service.CounselorAuditService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员审核控制器
 */
@RestController
@RequestMapping("/admin/audit")
@Slf4j
public class AdminAuditController {

    @Autowired
    private CounselorAuditService auditService;

    /**
     * 获取待审核列表
     *
     * @param role 当前用户角色（从token中获取）
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/list")
    public Result<AuditListVO> getPendingAuditList(@RequestAttribute String role,
                                                     @RequestParam(required = false, defaultValue = "1") Integer page,
                                                     @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        log.info("获取待审核列表，角色:{}，页码:{}，每页:{}", role, page, pageSize);

        // 验证管理员权限
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new BaseException("无权访问，仅管理员可操作");
        }

        AuditListVO auditList = auditService.getPendingAuditList(page, pageSize);

        return Result.success(auditList);
    }

    /**
     * 审核操作（通过/拒绝）
     *
     * @param userId 当前用户ID（从token中获取）
     * @param role 当前用户角色（从token中获取）
     * @param processDTO
     * @return
     */
    @PostMapping("/process")
    public Result<OperationResultVO> processAudit(@RequestAttribute Long userId,
                                                   @RequestAttribute String role,
                                                   @Valid @RequestBody AuditProcessDTO processDTO) {
        log.info("处理审核，管理员ID:{}，角色:{}，审核ID:{}，操作:{}", userId, role, processDTO.getAuditId(), processDTO.getAction());

        // 验证管理员权限
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new BaseException("无权访问，仅管理员可操作");
        }

        auditService.processAudit(userId, processDTO);

        return Result.success(OperationResultVO.builder().success(true).build());
    }
}

