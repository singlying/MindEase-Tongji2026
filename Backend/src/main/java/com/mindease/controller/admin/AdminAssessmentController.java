package com.mindease.controller.admin;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.QuestionManageDTO;
import com.mindease.pojo.dto.ScaleSaveDTO;
import com.mindease.pojo.vo.QuestionManageVO;
import com.mindease.pojo.vo.ScaleSaveVO;
import com.mindease.service.AssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 心理测评管理控制器（管理员端）
 */
@RestController
@RequestMapping("/admin/assessment")
@Slf4j
public class AdminAssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    /**
     * 创建/更新量表配置
     *
     * @param scaleSaveDTO 量表数据
     * @return
     */
    @PostMapping("/scale")
    public Result<ScaleSaveVO> saveScale(@RequestBody ScaleSaveDTO scaleSaveDTO) {
        log.info("创建/更新量表，数据: {}", scaleSaveDTO);

        ScaleSaveVO result = assessmentService.saveScale(scaleSaveDTO);

        // 根据操作类型返回不同的提示信息
        String message = Boolean.TRUE.equals(result.getIsUpdate()) ? "量表更新成功" : "量表创建成功";

        return Result.success(result, message);
    }

    /**
     * 管理量表题目
     *
     * @param questionManageDTO 题目数据
     * @return
     */
    @PostMapping("/question")
    public Result<QuestionManageVO> manageQuestions(@RequestBody QuestionManageDTO questionManageDTO) {
        log.info("管理量表题目，scaleKey: {}", questionManageDTO.getScaleKey());

        QuestionManageVO result = assessmentService.manageQuestions(questionManageDTO);

        return Result.success(result, "题目保存成功");
    }
}

