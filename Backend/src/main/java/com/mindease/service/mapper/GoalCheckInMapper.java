package com.mindease.mapper;

import com.mindease.pojo.entity.GoalCheckIn;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 目标打卡记录 Mapper
 */
@Mapper
public interface GoalCheckInMapper {

    void insert(GoalCheckIn checkIn);

    List<GoalCheckIn> selectByGoalIdSince(@Param("goalId") Long goalId, @Param("since") LocalDate since);

    List<GoalCheckIn> selectByGoalIdBetweenDates(
            @Param("goalId") Long goalId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Select("SELECT COUNT(*) FROM goal_check_in WHERE goal_id = #{goalId} AND user_id = #{userId} AND check_date = #{date}")
    int existsByGoalAndDate(@Param("goalId") Long goalId, @Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM goal_check_in WHERE user_id = #{userId} AND check_date >= #{since}")
    int countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDate since);

    @Select("SELECT COUNT(*) FROM goal_check_in WHERE user_id = #{userId} AND check_date = #{date}")
    int countByUserOnDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
