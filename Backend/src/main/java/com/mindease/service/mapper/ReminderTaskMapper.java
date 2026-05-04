package com.mindease.mapper;

import com.mindease.pojo.entity.ReminderTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 定时提醒任务 Mapper
 */
@Mapper
public interface ReminderTaskMapper {

    void insert(ReminderTask task);

    void updateById(ReminderTask task);

    ReminderTask selectById(Long id);

    @Select("SELECT * FROM reminder_task WHERE user_id = #{userId} AND status = 'ACTIVE' ORDER BY next_execute_at ASC")
    List<ReminderTask> selectActiveByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM reminder_task WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ReminderTask> selectAllByUserId(@Param("userId") Long userId);
}
