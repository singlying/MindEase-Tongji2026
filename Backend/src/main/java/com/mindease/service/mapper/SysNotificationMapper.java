package com.mindease.mapper;

import com.mindease.pojo.entity.SysNotification;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统通知 Mapper
 */
@Mapper
public interface SysNotificationMapper {

    void insert(SysNotification notification);

    void updateById(SysNotification notification);

    @Delete("DELETE FROM sys_notification WHERE id = #{id}")
    void deleteById(Long id);

    SysNotification selectById(Long id);

    List<SysNotification> selectByCondition(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("isRead") Integer isRead,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );

    Long countByCondition(@Param("userId") Long userId, @Param("type") String type, @Param("isRead") Integer isRead);

    @Select("SELECT COUNT(*) FROM sys_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(@Param("userId") Long userId);

    @Update("UPDATE sys_notification SET is_read = 1, read_time = NOW() WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
