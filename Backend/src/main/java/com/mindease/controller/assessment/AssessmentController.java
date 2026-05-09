package com.mindease.controller.assessment;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.AssessmentSubmitDTO;
import com.mindease.pojo.vo.*;
import com.mindease.service.AssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 心理测评控制器（用户端）
 */
@RestController
@RequestMapping("/assessment")
@Slf4j
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    /**
     * 获取量表列表
     *
     * @return
     */
    @GetMapping("/scales")
    public Result<ScaleListVO> getScaleList() {
        log.info("获取量表列表");

        ScaleListVO result = assessmentService.getScaleList();

        return Result.success(result);
    }

    /**
     * 获取量表详情
     *
     * @param scaleKey 量表Key
     * @return
     */
    @GetMapping("/scale/{scaleKey}")
    public Result<ScaleDetailVO> getScaleDetail(@PathVariable String scaleKey) {
        log.info("获取量表详情，scaleKey: {}", scaleKey);

        ScaleDetailVO result = assessmentService.getScaleDetail(scaleKey);

        return Result.success(result);
    }

    /**
     * 提交测评
     *
     * @param userId 当前用户ID（从token中获取）
     * @param submitDTO 提交的答案
     * @return
     */
    @PostMapping("/submit")
    public Result<AssessmentSubmitVO> submitAssessment(@RequestAttribute Long userId,
                                                        @RequestBody AssessmentSubmitDTO submitDTO) {
        log.info("提交测评，用户ID: {}, scaleKey: {}", userId, submitDTO.getScaleKey());

        AssessmentSubmitVO result = assessmentService.submitAssessment(userId, submitDTO);

        return Result.success(result);
    }

    /**
     * 获取测评历史列表
     *
     * @param userId 当前用户ID（从token中获取）
     * @param limit 查询数量限制
     * @return
     */
    @GetMapping("/records")
    public Result<AssessmentRecordListVO> getRecordList(@RequestAttribute Long userId,
                                                         @RequestParam(required = false, defaultValue = "10") Integer limit) {
        log.info("获取测评历史列表，用户ID: {}, limit: {}", userId, limit);

        AssessmentRecordListVO result = assessmentService.getRecordList(userId, limit);

        return Result.success(result);
    }

    /**
     * 获取单次测评详情
     *
     * @param id 测评记录ID
     * @return
     */
    @GetMapping("/record/{id}")
    public Result<AssessmentRecordDetailVO> getRecordDetail(@PathVariable Long id) {
        log.info("获取测评详情，ID: {}", id);

        AssessmentRecordDetailVO result = assessmentService.getRecordDetail(id);

        return Result.success(result);
    }
}

