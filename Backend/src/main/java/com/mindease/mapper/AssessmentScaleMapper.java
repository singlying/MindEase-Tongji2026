package com.mindease.mapper;

import com.mindease.pojo.entity.AssessmentScale;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 量表Mapper接口
 */
@Mapper
public interface AssessmentScaleMapper {

    /**
     * 查询所有量表（仅活跃状态）
     *
     * @return
     */
    @Select("SELECT id, scale_key, title, cover_url, description, status FROM assessment_scale WHERE status = 'active' ORDER BY id")
    List<AssessmentScale> listActive();

    /**
     * 根据scaleKey查询量表
     *
     * @param scaleKey
     * @return
     */
    @Select("SELECT * FROM assessment_scale WHERE scale_key = #{scaleKey}")
    AssessmentScale getByScaleKey(String scaleKey);

    /**
     * 根据ID查询量表
     *
     * @param id
     * @return
     */
    @Select("SELECT * FROM assessment_scale WHERE id = #{id}")
    AssessmentScale getById(Long id);

    /**
     * 插入量表
     *
     * @param scale
     */
    @Insert("INSERT INTO assessment_scale (scale_key, title, cover_url, description, scoring_rules, status) " +
            "VALUES (#{scaleKey}, #{title}, #{coverUrl}, #{description}, #{scoringRules}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AssessmentScale scale);

    /**
     * 更新量表
     *
     * @param scale
     */
    @Update("UPDATE assessment_scale SET title = #{title}, cover_url = #{coverUrl}, " +
            "description = #{description}, scoring_rules = #{scoringRules}, status = #{status} " +
            "WHERE id = #{id}")
    void update(AssessmentScale scale);

    /**
     * 删除量表
     *
     * @param id
     */
    @Delete("DELETE FROM assessment_scale WHERE id = #{id}")
    void delete(Long id);
}

