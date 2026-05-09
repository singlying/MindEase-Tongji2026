package com.mindease.mapper;

import com.mindease.pojo.entity.SysNotification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysNotificationMapper {

    /**
     * 查询用户的通知列表
     *
     * @param userId
     * @param limit
     * @return
     */
    @Select("select * from sys_notification where user_id = #{userId} order by create_time desc limit #{limit}")
    List<SysNotification> getByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 查询用户的未读通知数量
     *
     * @param userId
     * @return
     */
    @Select("select count(*) from sys_notification where user_id = #{userId} and is_read = 0")
    int countUnread(Long userId);

    /**
     * 标记通知为已读
     *
     * @param id
     */
    @Update("update sys_notification set is_read = 1 where id = #{id}")
    void markAsRead(Long id);

    /**
     * 将用户的所有通知标记为已读
     *
     * @param userId
     */
    @Update("update sys_notification set is_read = 1 where user_id = #{userId} and is_read = 0")
    void markAllAsRead(Long userId);

    /**
     * 根据ID查询通知
     *
     * @param id
     * @return
     */
    @Select("select * from sys_notification where id = #{id}")
    SysNotification getById(Long id);

    /**
     * 插入通知
     *
     * @param notification
     */
    @Insert("insert into sys_notification(user_id, type, title, content, is_read, create_time) " +
            "values(#{userId}, #{type}, #{title}, #{content}, #{isRead}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysNotification notification);
}

