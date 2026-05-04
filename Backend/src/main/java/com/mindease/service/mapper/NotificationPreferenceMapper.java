package com.mindease.mapper;

import com.mindease.pojo.entity.NotificationPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 通知偏好设置 Mapper
 */
@Mapper
public interface NotificationPreferenceMapper {

    void insert(NotificationPreference pref);

    void updateById(NotificationPreference pref);

    @Select("SELECT * FROM notification_preference WHERE user_id = #{userId}")
    NotificationPreference selectByUserId(@Param("userId") Long userId);
}
