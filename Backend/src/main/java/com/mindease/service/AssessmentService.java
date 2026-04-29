package com.mindease.service;

import com.mindease.pojo.dto.AssessmentSubmitDTO;
import com.mindease.pojo.dto.QuestionManageDTO;
import com.mindease.pojo.dto.ScaleSaveDTO;
import com.mindease.pojo.vo.*;

/**
 * 心理测评服务接口
 */
public interface AssessmentService {

    /**
     * 获取量表列表
     *
     * @return
     */
    ScaleListVO getScaleList();

    /**
     * 获取量表详情
     *
     * @param scaleKey
     * @return
     */
    ScaleDetailVO getScaleDetail(String scaleKey);

    /**
     * 提交测评
     *
     * @param userId
     * @param submitDTO
     * @return
     */
    AssessmentSubmitVO submitAssessment(Long userId, AssessmentSubmitDTO submitDTO);

    /**
     * 获取测评历史列表
     *
     * @param userId
     * @param limit
     * @return
     */
    AssessmentRecordListVO getRecordList(Long userId, Integer limit);

    /**
     * 获取单次测评详情
     *
     * @param id
     * @return
     */
    AssessmentRecordDetailVO getRecordDetail(Long id);

    /**
     * 创建或更新量表（管理员）
     *
     * @param scaleSaveDTO
     * @return
     */
    ScaleSaveVO saveScale(ScaleSaveDTO scaleSaveDTO);

    /**
     * 管理量表题目（管理员）
     *
     * @param questionManageDTO
     * @return
     */
    QuestionManageVO manageQuestions(QuestionManageDTO questionManageDTO);
}

