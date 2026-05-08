package com.mindease.service;

import com.mindease.pojo.vo.DashboardVO;
import com.mindease.pojo.vo.NotificationListVO;

public interface UserCenterService {

    /**
     * 获取用户 Dashboard 数据
     *
     * @param userId
     * @return
     */
    DashboardVO getDashboard(Long userId);

    /**
     * 获取通知列表
     *
     * @param userId
     * @param limit
     * @return
     */
    NotificationListVO getNotifications(Long userId, Integer limit);

    /**
     * 标记通知为已读
     *
     * @param userId
     * @param notificationId
     */
    void markNotificationAsRead(Long userId, Long notificationId);

    /**
     * 将当前用户的通知全部标记为已读
     *
     * @param userId
     */
    void markAllNotificationsAsRead(Long userId);
}

