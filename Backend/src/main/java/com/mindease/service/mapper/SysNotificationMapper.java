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

    /**
     * 批量删除已过期的通知
     */
    @Delete("DELETE FROM sys_notification WHERE expire_at IS NOT NULL AND expire_at < NOW()")
    int deleteExpiredNotifications();

    /**
     * 批量标记已读
     */
    int markBatchRead(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    /**
     * 按类型分组统计未读数
     */
    @Select("SELECT type, COUNT(*) AS cnt FROM sys_notification " +
            "WHERE user_id = #{userId} AND is_read = 0 GROUP BY type")
    List<Map<String, Object>> countUnreadGroupByType(@Param("userId") Long userId);
}
