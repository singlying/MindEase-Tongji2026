package com.mindease.mapper;

import com.mindease.pojo.entity.Goal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 心理目标 Mapper
 */
@Mapper
public interface GoalMapper {

    void insert(Goal goal);

    void updateById(Goal goal);

    Goal selectById(Long id);

    List<Goal> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM goal WHERE user_id = #{userId} AND status = #{status}")
    int countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT MAX(longest_streak) FROM goal WHERE user_id = #{userId} AND status IN ('COMPLETED', 'ACTIVE')")
    Integer getMaxLongestStreak(@Param("userId") Long userId);

    /**
     * 按分类统计目标数量
     */
    @Select("SELECT category, COUNT(*) AS cnt FROM goal " +
            "WHERE user_id = #{userId} AND status != 'DELETED' GROUP BY category")
    List<Map<String, Object>> getCountByCategory(@Param("userId") Long userId);
}
