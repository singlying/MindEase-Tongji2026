package com.mindease.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.aiservice.ConsultantService;
import com.mindease.common.exception.BaseException;
import com.mindease.mapper.SysNotificationMapper;
import com.mindease.mapper.NotificationPreferenceMapper;
import com.mindease.mapper.ReminderTaskMapper;
import com.mindease.pojo.dto.NotificationTemplateDTO;
import com.mindease.pojo.entity.SysNotification;
import com.mindease.pojo.entity.NotificationPreference;
import com.mindease.pojo.entity.ReminderTask;
import com.mindease.pojo.vo.*;
import com.mindease.service.NotificationPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能推送通知服务实现类
 * 支持系统通知、定时提醒、AI关怀等多种通知场景
 */
@Service
@Slf4j
public class NotificationPushServiceImpl implements NotificationPushService {

    private static final String UNREAD_COUNT_KEY = "mindease:notify:unread:";
    private static final String AI_CARE_COOLDOWN = "mindease:notify:ai_care_cooldown:";

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private NotificationPreferenceMapper preferenceMapper;

    @Autowired
    private ReminderTaskMapper reminderTaskMapper;

    @Autowired
    private ConsultantService consultantService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NotificationVO sendNotification(Long userId, NotificationTemplateDTO templateDTO) {
        log.info("发送通知给用户 {}, 标题: {}", userId, templateDTO.getTitle());

        // 检查用户通知偏好（是否允许接收此类通知）
        NotificationPreference pref = getOrCreatePreference(userId);
        if (!isAllowedByPreference(pref, templateDTO.getType())) {
            log.info("用户 {} 已关闭该类型通知: {}", userId, templateDTO.getType());
            return null;  // 静默丢弃
        }

        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setType(templateDTO.getType() != null ? templateDTO.getType() : "SYSTEM");
        notification.setTitle(templateDTO.getTitle());
        notification.setContent(templateDTO.getContent());
        notification.setExtraData(templateDTO.getExtraData() != null ?
                toJson(templateDTO.getExtraData()) : null);
        notification.setActionUrl(templateDTO.getActionUrl());
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());

        notificationMapper.insert(notification);

        // 更新Redis中的未读计数缓存
        incrementUnreadCache(userId);

        log.info("通知已发送, ID: {}", notification.getId());

        return convertToVO(notification);
    }

    @Override
    public NotificationBatchResultVO sendBatchNotification(List<Long> userIds,
                                                            String title, String content,
                                                            String type) {
        log.info("批量发送通知, 目标用户数: {}", userIds.size());

        int successCount = 0;
        int skippedCount = 0;
        List<Long> failedUsers = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                NotificationTemplateDTO dto = new NotificationTemplateDTO();
                dto.setTitle(title);
                dto.setContent(content);
                dto.setType(type);

                NotificationVO result = sendNotification(userId, dto);
                if (result != null) {
                    successCount++;
                } else {
                    skippedCount++;  // 用户关闭了该类型通知
                }
            } catch (Exception e) {
                log.warn("批量发送失败, userId={}: {}", userId, e.getMessage());
                failedUsers.add(userId);
            }
        }

        log.info("批量发送完成, 成功: {}, 跳过: {}, 失败: {}",
                 successCount, skippedCount, failedUsers.size());

        return NotificationBatchResultVO.builder()
                .totalTargets(userIds.size())
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(failedUsers.size())
                .failedUserIds(failedUsers)
                .build();
    }

    @Override
    public NotificationListVO getUserNotifications(Long userId, String type,
                                                    Boolean isRead, Integer page,
                                                    Integer pageSize) {
        int offset = (page - 1) * pageSize;

        Long total = notificationMapper.countByCondition(userId, type, isRead);
        List<SysNotification> notifications = notificationMapper.selectByCondition(
                userId, type, isRead, pageSize, offset
        );

        List<NotificationItemVO> items = notifications.stream()
                .map(this::convertToItemVO)
                .collect(Collectors.toList());

        return NotificationListVO.builder()
                .total(total)
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        SysNotification notification = verifyNotificationOwnership(notificationId, userId);
        if (notification.getIsRead() == 1) return;  // 已经是已读

        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);

        decrementUnreadCache(userId);

        log.info("通知已标记为已读, id={}", notificationId);
    }

    @Override
    public Integer markBatchRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        log.info("批量标记已读, userId={}, 数量={}", userId, notificationIds.size());

        // 校验归属权并过滤无效ID
        List<Long> validIds = new ArrayList<>();
        for (Long nid : notificationIds) {
            try {
                verifyNotificationOwnership(nid, userId);
                validIds.add(nid);
            } catch (BaseException e) {
                log.warn("跳过无权操作的通知: id={}", nid);
            }
        }

        if (validIds.isEmpty()) {
            return 0;
        }

        notificationMapper.markBatchRead(userId, validIds);

        // 批量更新缓存中的未读计数
        decrementUnreadCacheByCount(userId, validIds.size());

        log.info("批量标记已读完成, 成功{}条", validIds.size());
        return validIds.size();
    }

    @Override
    public Integer markAllAsRead(Long userId) {
        Integer unreadCount = getUnreadCountFromCache(userId);
        if (unreadCount == null || unreadCount == 0) {
            unreadCount = notificationMapper.countUnreadByUserId(userId);
        }

        notificationMapper.markAllAsReadByUserId(userId);

        stringRedisTemplate.opsForValue().set(UNREAD_COUNT_KEY + userId, "0", 24, TimeUnit.HOURS);

        log.info("用户 {} 的所有通知标记为已读, 共{}条", userId, unreadCount);
        return unreadCount;
    }

    @Override
    public Boolean deleteNotification(Long notificationId, Long userId) {
        verifyNotificationOwnership(notificationId, userId);

        SysNotification notification = notificationMapper.selectById(notificationId);
        if (notification.getIsRead() == 0) {
            decrementUnreadCache(userId);  // 删除未读消息时也要减计数
        }

        notificationMapper.deleteById(notificationId);

        log.info("通知已删除, id={}", notificationId);
        return true;
    }

    @Override
    public NotificationPreferenceVO updatePreferences(Long userId, NotificationPreferenceVO preferences) {
        log.info("更新用户通知偏好, userId={}", userId);

        NotificationPreference existing = preferenceMapper.selectByUserId(userId);
        if (existing == null) {
            existing = createDefaultPreference(userId);
        }

        // 只更新传入的字段，其他保持不变
        if (preferences.getEnableSystemNotify() != null) {
            existing.setEnableSystemNotify(preferences.getEnableSystemNotify());
        }
        if (preferences.getEnableAppointmentNotify() != null) {
            existing.setEnableAppointmentNotify(preferences.getEnableAppointmentNotify());
        }
        if (preferences.getEnableGoalRemind() != null) {
            existing.setEnableGoalRemind(preferences.getEnableGoalRemind());
        }
        if (preferences.getEnableAiCare() != null) {
            existing.setEnableAiCare(preferences.getEnableAiCare());
        }
        if (preferences.getEnableCommunityNotify() != null) {
            existing.setEnableCommunityNotify(preferences.getEnableCommunityNotify());
        }
        if (preferences.getQuietStartTime() != null) {
            existing.setQuietStartTime(preferences.getQuietStartTime());
        }
        if (preferences.getQuietEndTime() != null) {
            existing.setQuietEndTime(preferences.getQuietEndTime());
        }
        if (preferences.getMutedUntil() != null) {
            existing.setMutedUntil(preferences.getMutedUntil());
        }
        if (preferences.getDailySummaryTime() != null) {
            existing.setDailySummaryTime(preferences.getDailySummaryTime());
        }
        if (preferences.getMaxDailyPushCount() != null) {
            existing.setMaxDailyPushCount(preferences.getMaxDailyPushCount());
        }

        existing.setUpdateTime(LocalDateTime.now());
        preferenceMapper.updateById(existing);

        log.info("偏好设置已更新");
        return convertToPreferenceVO(existing);
    }

    @Override
    public NotificationPreferenceVO getPreferences(Long userId) {
        NotificationPreference pref = preferenceMapper.selectByUserId(userId);
        if (pref == null) {
            pref = createDefaultPreference(userId);
        }
        return convertToPreferenceVO(pref);
    }

    @Override
    public ReminderTaskVO createReminderTask(Long userId, ReminderTaskCreateDTO taskDTO) {
        log.info("创建提醒任务, userId={}, title={}", userId, taskDTO.getTitle());

        NotificationPreference pref = getOrCreatePreference(userId);

        // 校验：目标打卡提醒需要先开启该功能
        if ("GOAL_CHECKIN".equals(taskDTO.getTaskType()) && !Boolean.TRUE.equals(pref.getEnableGoalRemind())) {
            throw new BaseException("请先在设置中开启目标提醒功能");
        }

        ReminderTask task = new ReminderTask();
        task.setUserId(userId);
        task.setTitle(taskDTO.getTitle());
        task.setTaskType(taskDTO.getTaskType() != null ? taskDTO.getTaskType() : "CUSTOM");
        task.setTargetId(taskDTO.getTargetId());  // 关联的目标/预约等ID
        task.setRemindTime(taskDTO.getRemindTime());
        task.setMessage(taskDTO.getMessage() != null ? taskDTO.getMessage() : "您有一个待完成的任务");
        task.setFrequency(taskDTO.getFrequency() != null ? taskDTO.getFrequency() : "once");
        task.setStatus("ACTIVE");
        task.setNextExecuteAt(calculateNextExecuteAt(
                LocalDateTime.now(), taskDTO.getRemindTime(), taskDTO.getFrequency()
        ));
        task.setCreateTime(LocalDateTime.now());

        reminderTaskMapper.insert(task);

        log.info("提醒任务创建成功, ID: {}, 下次执行时间: {}",
                 task.getId(), task.getNextExecuteAt());

        return convertToTaskVO(task);
    }

    @Override
    public Boolean cancelReminderTask(Long taskId, Long userId) {
        ReminderTask task = verifyTaskOwnership(taskId, userId);
        task.setStatus("CANCELLED");
        task.setCancelTime(LocalDateTime.now());
        reminderTaskMapper.updateById(task);

        log.info("提醒任务已取消, taskId={}", taskId);
        return true;
    }

    @Override
    public List<ReminderTaskVO> getReminderTasks(Long userId, Boolean activeOnly) {
        List<ReminderTask> tasks;
        if (activeOnly != null && activeOnly) {
            tasks = reminderTaskMapper.selectActiveByUserId(userId);
        } else {
            tasks = reminderTaskMapper.selectAllByUserId(userId);
        }
        return tasks.stream().map(this::convertToTaskVO).collect(Collectors.toList());
    }

    @Override
    @Async
    public AICareMessageVO triggerAICare(Long userId) {
        log.info("触发AI关怀推送, userId={}", userId);

        // 冷却检查：同一用户每6小时最多触发一次
        String cooldownKey = AI_CARE_COOLDOWN + userId;
        Boolean onCooldown = stringRedisTemplate.hasKey(cooldownKey);
        if (onCooldown != null && onCooldown) {
            log.info("用户 {} AI关怀冷却中，跳过本次推送", userId);
            return AICareMessageVO.builder()
                    .triggered(false)
                    .reason("cooldown")
                    .message(null)
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        NotificationPreference pref = getOrCreatePreference(userId);
        if (!Boolean.TRUE.equals(pref.getEnableAiCare())) {
            log.info("用户 {} 未开启AI关怀", userId);
            return AICareMessageVO.builder()
                    .triggered(false)
                    .reason("disabled")
                    .message(null)
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        // 检查是否在免打扰时段内
        LocalTime now = LocalTime.now();
        if (now.isAfter(pref.getQuietStartTime()) && now.isBefore(pref.getQuietEndTime())) {
            log.info("当前处于免打扰时段，跳过AI关怀推送");
            return AICareMessageVO.builder()
                    .triggered(false)
                    .reason("quiet_hours")
                    .message(null)
                    .sentAt(LocalDateTime.now())
                    .build();
        }

        // 收集用户的近期行为数据作为上下文
        String context = buildAICareContext(userId);

        // 根据时间选择消息类型策略
        String messageType = determineMessageType(now);

        // 调用AI服务生成关怀内容
        String aiMessage;
        long startTime = System.currentTimeMillis();
        try {
            aiMessage = consultantService.analyzeMood(
                    "你是一个温暖专业的心理咨询助手。请根据以下用户数据生成一条简短（不超过100字）、"
                    + "真诚的关怀消息。不要使用emoji，不要过于正式。\n\n" + context
            );

            if (aiMessage == null || aiMessage.trim().isEmpty()) {
                aiMessage = generateFallbackCareMessage();
            }
        } catch (Exception e) {
            log.error("AI关怀内容生成失败", e);
            aiMessage = generateFallbackCareMessage();
        }

        long aiCostMs = System.currentTimeMillis() - startTime;

        // 构建建议列表
        List<String> suggestions = buildCareSuggestions(messageType);

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("aiModel", "mindease-care-v2");
        metadata.put("generationTimeMs", aiCostMs);
        metadata.put("contextLength", context.length());

        // 发送通知
        NotificationTemplateDTO careDTO = new NotificationTemplateDTO();
        careDTO.setType("AI_CARE");
        careDTO.setTitle("来自MindEase的关怀");
        careDTO.setContent(aiMessage);
        sendNotification(userId, careDTO);

        // 设置冷却时间
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", 6, TimeUnit.HOURS);

        log.info("AI关怀推送成功, userId={}, 耗时{}ms", userId, aiCostMs);

        // 根据内容分析情绪标签
        String emotionTag = analyzeEmotionTag(context);

        return AICareMessageVO.builder()
                .triggered(true)
                .reason("success")
                .contextLength(context.length())
                .messageType(messageType)
                .emotionTag(emotionTag)
                .message(aiMessage)
                .suggestions(suggestions)
                .metadata(metadata)
                .sentAt(LocalDateTime.now())
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验通知归属权
     */
    private SysNotification verifyNotificationOwnership(Long notificationId, Long userId) {
        SysNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BaseException("通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException("无权操作此通知");
        }
        return notification;
    }

    /**
     * 校验提醒任务归属权
     */
    private ReminderTask verifyTaskOwnership(Long taskId, Long userId) {
        ReminderTask task = reminderTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException("提醒任务不存在");
        }
        if (!task.getUserId().equals(userId)) {
            throw new BaseException("无权操作此提醒任务");
        }
        return task;
    }

    /**
     * 获取或创建默认偏好设置
     */
    private NotificationPreference getOrCreatePreference(Long userId) {
        NotificationPreference pref = preferenceMapper.selectByUserId(userId);
        if (pref == null) {
            pref = createDefaultPreference(userId);
        }
        return pref;
    }

    /**
     * 创建默认通知偏好
     */
    private NotificationPreference createDefaultPreference(Long userId) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUserId(userId);
        pref.setEnableSystemNotify(true);
        pref.setEnableAppointmentNotify(true);
        pref.setEnableGoalRemind(true);
        pref.setEnableAiCare(true);
        pref.setEnableCommunityNotify(true);
        pref.setQuietStartTime(LocalTime.of(22, 0));   // 免打扰开始 22:00
        pref.setQuietEndTime(LocalTime.of(8, 0));     // 免打扰结束 08:00
        pref.setMutedUntil(null);
        pref.setDailySummaryTime(LocalTime.of(21, 0));  // 每日总结推送 21:00
        pref.setMaxDailyPushCount(50);                  // 每日默认最多50条
        pref.setCreateTime(LocalDateTime.now());
        preferenceMapper.insert(pref);
        return pref;
    }

    /**
     * 检查是否允许根据偏好发送通知
     */
    private boolean isAllowedByPreference(NotificationPreference pref, String type) {
        LocalTime now = LocalTime.now();

        // 检查全局静音
        if (pref.getMutedUntil() != null && now.isBefore(pref.getMutedUntil().toLocalTime())) {
            return false;
        }

        // 检查免打扰时段
        if (now.isAfter(pref.getQuietStartTime()) && now.isBefore(pref.getQuietEndTime())) {
            return false;
        }

        // 按类型检查开关
        switch (type) {
            case "SYSTEM":
                return Boolean.TRUE.equals(pref.getEnableSystemNotify());
            case "APPOINTMENT":
                return Boolean.TRUE.equals(pref.getEnableAppointmentNotify());
            case "GOAL":
                return Boolean.TRUE.equals(pref.getEnableGoalRemind());
            case "AI_CARE":
                return Boolean.TRUE.equals(pref.getEnableAiCare());
            case "COMMUNITY":
                return Boolean.TRUE.equals(pref.getEnableCommunityNotify());
            default:
                return true;  // 其他类型默认放行
        }
    }

    /**
     * 增加未读数缓存
     */
    private void incrementUnreadCache(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    /**
     * 减少未读数缓存
     */
    private void decrementUnreadCache(Long userId) {
        String key = UNREAD_COUNT_KEY + userId;
        Long count = stringRedisTemplate.opsForValue().decrement(key);
        if (count != null && count < 0) {
            stringRedisTemplate.opsForValue().set(key, "0", 24, TimeUnit.HOURS);
        } else {
            stringRedisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
    }

    /**
     * 从缓存获取未读数
     */
    private Integer getUnreadCountFromCache(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(UNREAD_COUNT_KEY + userId);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * 计算下次执行时间
     */
    private LocalDateTime calculateNextExecuteAt(LocalDateTime now, LocalTime remindTime,
                                                   String frequency) {
        LocalDateTime next = now.toLocalDate().atTime(remindTime);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }

        switch (frequency) {
            case "weekly":
                return next.plusWeeks(1);
            case "monthly":
                return next.plusMonths(1);
            default:
                return next;  // daily / once
        }
    }

    /**
     * 构建AI关怀的上下文数据
     */
    private String buildAICareContext(Long userId) {
        StringBuilder ctx = new StringBuilder();

        // 今日心情
        ctx.append("当前日期: ").append(LocalDate.now()).append("\n");

        // TODO: 可扩展 - 从MoodService/GoalService获取更多上下文
        // 例如：最近情绪趋势、目标完成情况、连续打卡天数等

        return ctx.toString();
    }

    /**
     * 备用关怀消息（当AI服务不可用时）
     */
    private String generateFallbackCareMessage() {
        String[] messages = {
                "今天也辛苦了。记得给自己一点时间和空间，好好休息。",
                "无论今天发生了什么，你已经做得很好了。明天又是新的开始。",
                "偶尔感到疲惫是很正常的。允许自己休息，这不是软弱，而是自我关爱。",
                "在这个忙碌的世界里，感谢你愿意关注自己的心理健康。晚安。",
                "每一个小小的进步都值得被看见。今天的你，比昨天更懂得照顾自己了。",
        };
        Random random = new Random();
        return messages[random.nextInt(messages.length)];
    }

    /**
     * 对象转JSON字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }

    // ==================== VO转换方法 ====================

    private NotificationVO convertToVO(SysNotification n) {
        Map<String, Object> extra = null;
        if (n.getExtraData() != null) {
            try {
                extra = objectMapper.readValue(n.getExtraData(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.warn("解析extraData失败", e);
            }
        }

        return NotificationVO.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .actionUrl(n.getActionUrl())
                .extraData(extra)
                .isRead(n.getIsRead() == 1)
                .createTime(n.getCreateTime())
                .build();
    }

    private NotificationItemVO convertToItemVO(SysNotification n) {
        String iconType = resolveIconType(n.getType());
        return NotificationItemVO.builder()
                .notificationId(n.getId())
                .type(n.getType())
                .iconType(iconType)
                .title(n.getTitle())
                .summary(truncate(n.getContent(), 60))
                .content(n.getContent())
                .actionUrl(n.getActionUrl())
                .isRead(n.getIsRead() == 1)
                .expireAt(n.getExpireAt())
                .createTime(n.getCreateTime())
                .build();
    }

    /**
     * 根据通知类型解析对应的图标标识
     */
    private String resolveIconType(String type) {
        if (type == null) return "bell";
        switch (type) {
            case "APPOINTMENT": return "calendar";
            case "GOAL": return "star";
            case "COMMUNITY": return "chat";
            case "AI_CARE": return "heart";
            case "REMINDER": return "clock";
            default: return "bell";
        }
    }

    private NotificationPreferenceVO convertToPreferenceVO(NotificationPreference p) {
        return NotificationPreferenceVO.builder()
                .enableSystemNotify(p.getEnableSystemNotify())
                .enableAppointmentNotify(p.getEnableAppointmentNotify())
                .enableGoalRemind(p.getEnableGoalRemind())
                .enableAiCare(p.getEnableAiCare())
                .enableCommunityNotify(p.getEnableCommunityNotify())
                .quietStartTime(p.getQuietStartTime())
                .quietEndTime(p.getQuietEndTime())
                .mutedUntil(p.getMutedUntil())
                .dailySummaryTime(p.getDailySummaryTime())
                .maxDailyPushCount(p.getMaxDailyPushCount())
                .build();
    }

    private ReminderTaskVO convertToTaskVO(ReminderTask t) {
        return ReminderTaskVO.builder()
                .taskId(t.getId())
                .title(t.getTitle())
                .taskType(t.getTaskType())
                .remindTime(t.getRemindTime())
                .frequency(t.getFrequency())
                .status(t.getStatus())
                .nextExecuteAt(t.getNextExecuteAt())
                .message(t.getMessage())
                .createTime(t.getCreateTime())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 按指定数量减少未读数缓存（用于批量标记已读场景）
     */
    private void decrementUnreadCacheByCount(Long userId, int count) {
        String key = UNREAD_COUNT_KEY + userId;
        Long current = stringRedisTemplate.opsForValue().get(key) != null
                ? Long.parseLong(stringRedisTemplate.opsForValue().get(key)) : 0L;
        long newValue = Math.max(0, current - count);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(newValue), 24, TimeUnit.HOURS);
    }

    /**
     * 根据当前时间确定AI关怀消息类型
     */
    private String determineMessageType(LocalTime now) {
        if (now.isBefore(LocalTime.of(11, 0))) {
            return "ENCOURAGEMENT";      // 上午：鼓励型消息
        } else if (now.isBefore(LocalTime.of(17, 0))) {
            return "TIP";                // 下午：实用小贴士
        } else {
            return "BREATHING";          // 晚上：放松引导
        }
    }

    /**
     * 根据消息类型构建附带建议列表
     */
    private List<String> buildCareSuggestions(String messageType) {
        switch (messageType) {
            case "ENCOURAGEMENT":
                return Arrays.asList("尝试记录一件今天值得感恩的小事", "做3次深呼吸，感受空气进入身体");
            case "TIP":
                return Arrays.asList("每小时站起来活动5分钟", "喝一杯温水，保持身体水分充足");
            case "BREATHING":
                return Arrays.asList("4-7-8呼吸法：吸气4秒、屏息7秒、呼气8秒", "播放一段白噪音或轻音乐");
            default:
                return Arrays.asList("关注当下，允许自己有情绪波动");
        }
    }

    /**
     * 根据上下文数据简单分析情绪标签
     */
    private String analyzeEmotionTag(String context) {
        // 基于关键词的简单情绪识别，实际生产环境应使用NLP模型
        if (context.contains("焦虑") || context.contains("紧张")) return "anxious";
        if (context.contains("难过") || context.contains("低落")) return "sad";
        if (context.contains("压力")) return "stressed";
        if (context.contains("开心") || context.contains("满足")) return "hopeful";
        return "neutral";
    }
}
