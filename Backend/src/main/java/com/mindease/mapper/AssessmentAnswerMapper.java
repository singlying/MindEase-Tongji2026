package com.mindease.mapper;

import com.mindease.pojo.entity.AssessmentAnswer;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 答案Mapper接口
 */
@Mapper
public interface AssessmentAnswerMapper {

    /**
     * 根据recordId查询答案列表
     *
     * @param recordId
     * @return
     */
    @Select("SELECT * FROM assessment_answer WHERE record_id = #{recordId} ORDER BY question_id")
    List<AssessmentAnswer> listByRecordId(Long recordId);

    /**
     * 批量插入答案
     *
     * @param answers
     */
    void batchInsert(@Param("answers") List<AssessmentAnswer> answers);

    /**
     * 插入答案
     *
     * @param answer
     */
    @Insert("INSERT INTO assessment_answer (record_id, question_id, score, answer_text) " +
            "VALUES (#{recordId}, #{questionId}, #{score}, #{answerText})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AssessmentAnswer answer);

    /**
     * 删除答案
     *
     * @param recordId
     */
    @Delete("DELETE FROM assessment_answer WHERE record_id = #{recordId}")
    void deleteByRecordId(Long recordId);
}

