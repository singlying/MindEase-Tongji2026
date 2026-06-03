package com.mindease.service;

import com.mindease.pojo.dto.NotificationTemplateDTO;
import com.mindease.pojo.vo.*;

import java.util.List;

/**
 * 智能推送通知服务接口
 * 管理系统通知、定时提醒、AI关怀推送等
 */
public interface NotificationPushService {

    /**
     * 发送自定义通知给指定用户
     *
     * @param userId        接收者ID
     * @param templateDTO   通知模板DTO
     * @return 通知VO
     */
    NotificationVO sendNotification(Long userId, NotificationTemplateDTO templateDTO);

    /**
     * 批量发送通知（管理员功能）
     *
     * @param userIds       接收者ID列表
     * @param title         通知标题
     * @param content       通知内容
     * @param type          类型: SYSTEM/APPOINTMENT/GOAL/COMMUNITY/AI_CARE
     * @return 发送结果统计
     */
    NotificationBatchResultVO sendBatchNotification(List<Long> userIds,
                                                      String title, String content, String type);

    /**
     * 获取用户的通知列表（支持分页和类型筛选）
     *
     * @param userId        用户ID
     * @param type          通知类型筛选，null表示全部
     * @param isRead        已读状态筛选，null表示全部
     * @param page          页码
     * @param pageSize      每页大小
     * @return 通知列表VO
     */
    NotificationListVO getUserNotifications(Long userId, String type, Boolean isRead,
                                             Integer page, Integer pageSize);

    /**
     * 标记单条通知为已读
     *
     * @param notificationId    通知ID
     * @param userId            用户ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 将用户所有通知标记为已读
     *
     * @param userId    用户ID
     * @return 实际标记的未读数量
     */
    Integer markAllAsRead(Long userId);

    /**
     * 删除通知
     *
     * @param notificationId    通知ID
     * @param userId            用户ID
     * @return 是否成功
     */
    Boolean deleteNotification(Long notificationId, Long userId);

    /**
     * 设置用户的提醒偏好
     *
     * @param userId        用户ID
     * @param preferences   偏好设置DTO
     * @return 更新后的偏好设置
     */
    NotificationPreferenceVO updatePreferences(Long userId, NotificationPreferenceVO preferences);

    /**
     * 获取用户当前的提醒偏好
     *
     * @param userId    用户ID
     * @return 偏好设置
     */
    NotificationPreferenceVO getPreferences(Long userId);

    /**
     * 创建定时提醒任务（如目标打卡提醒）
     *
     * @param userId    用户ID
     * @param taskDTO   定时任务DTO
     * @return 任务VO
     }
     ReminderTaskVO createReminderTask(Long userId, ReminderTaskCreateDTO taskDTO);

    /**
     * 取消定时提醒任务
     *
     * @param taskId    任务ID
     * @param userId    用户ID
     * @return 是否成功
     */
    Boolean cancelReminderTask(Long taskId, Long userId);

    /**
     * 获取用户的所有提醒任务列表
     *
     * @param userId    用户ID
     * @param activeOnly    是否仅返回活跃任务
     * @return 提醒任务列表
     */
    List<ReminderTaskVO> getReminderTasks(Long userId, Boolean activeOnly);

    /**
     * 触发AI关怀推送（基于用户行为数据自动生成）
     *
     * @param userId    目标用户ID
     * @return 生成的关怀内容
     */
    AICareMessageVO triggerAICare(Long userId);
}
