package com.mindease.mapper;

import com.mindease.pojo.entity.AssessmentRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AssessmentRecordMapper {

    /**
     * 查询用户最近的测评记录
     *
     * @param userId
     * @return
     */
    @Select("select * from assessment_record where user_id = #{userId} order by create_time desc limit 1")
    AssessmentRecord getLatestByUserId(Long userId);

    /**
     * 检查用户是否有测评记录
     *
     * @param userId
     * @return
     */
    @Select("select count(*) from assessment_record where user_id = #{userId}")
    int countByUserId(Long userId);

    /**
     * 插入测评记录
     *
     * @param record
     */
    @Insert("INSERT INTO assessment_record (user_id, scale_key, total_score, result_level, result_desc) " +
            "VALUES (#{userId}, #{scaleKey}, #{totalScore}, #{resultLevel}, #{resultDesc})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AssessmentRecord record);

    /**
     * 根据ID查询测评记录
     *
     * @param id
     * @return
     */
    @Select("SELECT * FROM assessment_record WHERE id = #{id}")
    AssessmentRecord getById(Long id);

    /**
     * 查询用户的测评记录列表（分页）
     *
     * @param userId
     * @param limit
     * @return
     */
    @Select("SELECT * FROM assessment_record WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<AssessmentRecord> listByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 删除测评记录
     *
     * @param id
     */
    @Delete("DELETE FROM assessment_record WHERE id = #{id}")
    void delete(Long id);
}

