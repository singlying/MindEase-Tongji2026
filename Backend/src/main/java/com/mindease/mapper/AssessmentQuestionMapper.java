package com.mindease.mapper;

import com.mindease.pojo.entity.AssessmentQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 题目Mapper接口
 */
@Mapper
public interface AssessmentQuestionMapper {

    /**
     * 根据scaleKey查询所有题目
     *
     * @param scaleKey
     * @return
     */
    @Select("SELECT * FROM assessment_question WHERE scale_key = #{scaleKey} ORDER BY sort_order, id")
    List<AssessmentQuestion> listByScaleKey(String scaleKey);

    /**
     * 根据ID查询题目
     *
     * @param id
     * @return
     */
    @Select("SELECT * FROM assessment_question WHERE id = #{id}")
    AssessmentQuestion getById(Long id);

    /**
     * 插入题目
     *
     * @param question
     */
    @Insert("INSERT INTO assessment_question (scale_key, question_text, options, sort_order) " +
            "VALUES (#{scaleKey}, #{questionText}, #{options}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AssessmentQuestion question);

    /**
     * 更新题目
     *
     * @param question
     */
    @Update("UPDATE assessment_question SET question_text = #{questionText}, " +
            "options = #{options}, sort_order = #{sortOrder} WHERE id = #{id}")
    void update(AssessmentQuestion question);

    /**
     * 删除题目
     *
     * @param id
     */
    @Delete("DELETE FROM assessment_question WHERE id = #{id}")
    void delete(Long id);

    /**
     * 根据scaleKey删除所有题目
     *
     * @param scaleKey
     */
    @Delete("DELETE FROM assessment_question WHERE scale_key = #{scaleKey}")
    void deleteByScaleKey(String scaleKey);

    /**
     * 批量查询题目
     *
     * @param ids
     * @return
     */
    List<AssessmentQuestion> listByIds(@Param("ids") List<Long> ids);
}

