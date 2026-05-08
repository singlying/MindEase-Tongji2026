package com.mindease.mapper;

import com.mindease.pojo.entity.MoodLog;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MoodLogMapper {

    /**
     * 插入情绪日志
     * 注意：content, tags, ai_analysis 字段会自动加密
     *
     * @param moodLog
     * @return
     */
    @Insert("insert into mood_log(user_id, mood_type, mood_score, content, tags, ai_analysis, log_date) " +
            "values(#{userId}, #{moodType}, #{moodScore}, " +
            "#{content, typeHandler=com.mindease.common.handler.EncryptedStringTypeHandler}, " +
            "#{tags, typeHandler=com.mindease.common.handler.EncryptedStringTypeHandler}, " +
            "#{aiAnalysis, typeHandler=com.mindease.common.handler.EncryptedStringTypeHandler}, " +
            "#{logDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MoodLog moodLog);

    /**
     * 查询用户最近的情绪日志
     *
     * @param userId
     * @param startDate
     * @return
     */
    @Select("select * from mood_log where user_id = #{userId} and log_date >= #{startDate} order by log_date desc")
    @ResultMap("MoodLogEncryptedResultMap")
    List<MoodLog> getRecentMoodLogs(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * 检查用户是否有情绪日志
     *
     * @param userId
     * @return
     */
    @Select("select count(*) from mood_log where user_id = #{userId}")
    int countByUserId(Long userId);

    /**
     * 根据ID查询情绪日志
     *
     * @param id
     * @return
     */
    @Select("select * from mood_log where id = #{id}")
    @ResultMap("MoodLogEncryptedResultMap")
    MoodLog getById(Long id);

    /**
     * 分页查询用户情绪日志
     *
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    @Select("select * from mood_log where user_id = #{userId} order by log_date desc limit #{limit} offset #{offset}")
    @ResultMap("MoodLogEncryptedResultMap")
    List<MoodLog> getByUserIdWithPagination(@Param("userId") Long userId, @Param("limit") Integer limit, @Param("offset") Integer offset);

    /**
     * 统计用户情绪日志总数
     *
     * @param userId
     * @return
     */
    @Select("select count(*) from mood_log where user_id = #{userId}")
    Long countByUserIdWithTotal(@Param("userId") Long userId);

    /**
     * 删除情绪日志
     *
     * @param id
     * @return
     */
    @Delete("delete from mood_log where id = #{id}")
    int deleteById(Long id);

    /**
     * 查询用户指定时间范围内的情绪日志
     *
     * @param userId
     * @param startDate
     * @param endDate
     * @return
     */
    @Select("select * from mood_log where user_id = #{userId} and log_date between #{startDate} and #{endDate} order by log_date asc")
    @ResultMap("MoodLogEncryptedResultMap")
    List<MoodLog> getByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 计算用户情绪平均分
     *
     * @param userId
     * @return
     */
    @Select("select avg(mood_score) from mood_log where user_id = #{userId}")
    Double getAverageScoreByUserId(@Param("userId") Long userId);

    /**
     * 统计用户情绪类型分布
     *
     * @param userId
     * @return
     */
    List<MoodTypeCount> getMoodTypeDistribution(@Param("userId") Long userId);

    /**
     * 情绪类型统计结果类
     */
    @Data
    static class MoodTypeCount {
        private String moodType;
        private Long count;
    }
}