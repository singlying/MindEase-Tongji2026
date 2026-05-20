package com.mindease.service.impl;

import com.mindease.common.exception.BaseException;
import com.mindease.mapper.AppointmentMapper;
import com.mindease.mapper.CounselorProfileMapper;
import com.mindease.mapper.MoodLogMapper;
import com.mindease.mapper.SysNotificationMapper;
import com.mindease.mapper.UserMapper;
import com.mindease.pojo.entity.Appointment;
import com.mindease.pojo.entity.CounselorProfile;
import com.mindease.pojo.entity.MoodLog;
import com.mindease.pojo.entity.SysNotification;
import com.mindease.pojo.entity.User;
import com.mindease.pojo.vo.*;
import com.mindease.service.UserCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserCenterServiceImpl implements UserCenterService {

    @Autowired
    private MoodLogMapper moodLogMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private CounselorProfileMapper counselorProfileMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取用户 Dashboard 数据
     */
    @Override
    public DashboardVO getDashboard(Long userId) {
        log.info("获取Dashboard数据，用户ID:{}", userId);

        // 1. 获取情绪摘要
        MoodSummaryVO moodSummary = getMoodSummary(userId);

        // 2. 获取即将到来的预约
        List<UpcomingAppointmentVO> upcomingAppointments = getUpcomingAppointments(userId);

        // 3. 获取未读通知数量
        int unreadCount = notificationMapper.countUnread(userId);

        return DashboardVO.builder()
                .moodSummary(moodSummary)
                .upcomingAppointments(upcomingAppointments)
                .unreadNotifications(unreadCount)
                .build();
    }

    /**
     * 获取通知列表
     */
    @Override
    public NotificationListVO getNotifications(Long userId, Integer limit) {
        log.info("获取通知列表，用户ID:{}，数量:{}", userId, limit);

        List<SysNotification> notifications = notificationMapper.getByUserId(userId, limit);

        List<NotificationVO> notificationVOs = notifications.stream()
                .map(n -> NotificationVO.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .content(n.getContent())
                        .isRead(n.getIsRead() == 1)
                        .createTime(n.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        return NotificationListVO.builder()
                .notifications(notificationVOs)
                .build();
    }

    /**
     * 标记通知为已读
     */
    @Override
    public void markNotificationAsRead(Long userId, Long notificationId) {
        log.info("标记通知已读，用户ID:{}，通知ID:{}", userId, notificationId);

        SysNotification notification = notificationMapper.getById(notificationId);
        if (notification == null) {
            throw new BaseException("通知不存在");
        }

        // 验证权限
        if (!notification.getUserId().equals(userId)) {
            throw new BaseException("无权操作此通知");
        }

        notificationMapper.markAsRead(notificationId);
    }

    /**
     * 将当前用户的通知全部标记为已读
     */
    @Override
    public void markAllNotificationsAsRead(Long userId) {
        log.info("标记用户全部通知为已读，用户ID:{}", userId);
        notificationMapper.markAllAsRead(userId);
    }

    /**
     * 获取情绪摘要
     */
    private MoodSummaryVO getMoodSummary(Long userId) {
        // 查询最近30天的情绪日志
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<MoodLog> recentLogs = moodLogMapper.getRecentMoodLogs(userId, thirtyDaysAgo);

        if (recentLogs == null || recentLogs.isEmpty()) {
            return MoodSummaryVO.builder()
                    .avgScore(BigDecimal.ZERO)
                    .continuousDays(0)
                    .build();
        }

        // 计算平均分
        double avgScore = recentLogs.stream()
                .mapToInt(MoodLog::getMoodScore)
                .average()
                .orElse(0.0);

        // 计算连续记录天数
        int continuousDays = calculateContinuousDays(recentLogs);

        return MoodSummaryVO.builder()
                .avgScore(BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP))
                .continuousDays(continuousDays)
                .build();
    }

    /**
     * 计算连续记录天数
     */
    private int calculateContinuousDays(List<MoodLog> logs) {
        if (logs.isEmpty()) {
            return 0;
        }

        // 按日期降序排序（最新的在前）
        List<LocalDate> dates = logs.stream()
                .map(log -> log.getLogDate().toLocalDate())
                .distinct()
                .sorted((d1, d2) -> d2.compareTo(d1))
                .collect(Collectors.toList());

        // 从今天开始计算连续天数
        LocalDate today = LocalDate.now();
        int continuousDays = 0;

        for (int i = 0; i < dates.size(); i++) {
            LocalDate expectedDate = today.minusDays(i);
            if (dates.size() > i && dates.get(i).equals(expectedDate)) {
                continuousDays++;
            } else {
                break;
            }
        }

        return continuousDays;
    }

    /**
     * 获取即将到来的预约
     */
    private List<UpcomingAppointmentVO> getUpcomingAppointments(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 获取当前用户信息，判断角色
        User currentUser = userMapper.getById(userId);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        List<Appointment> appointments;
        boolean isCounselor = "COUNSELOR".equalsIgnoreCase(currentUser.getRole());

        if (isCounselor) {
            // 咨询师：查询自己的预约（counselor_id = userId）
            appointments = appointmentMapper.getByCounselorId(userId, "CONFIRMED", 10, 0);
        } else {
            // 普通用户：查询自己作为客户的预约（user_id = userId）
            appointments = appointmentMapper.getByUserId(userId, "CONFIRMED", 10, 0);
        }

        return appointments.stream()
                .filter(apt -> apt.getStartTime().isAfter(now))
                .limit(3)
                .map(apt -> {
                    String displayName;
                    if (isCounselor) {
                        // 咨询师显示客户名称
                        User client = userMapper.getById(apt.getUserId());
                        displayName = client != null ? client.getNickname() : "未知用户";
                    } else {
                        // 普通用户显示咨询师名称
                        CounselorProfile profile = counselorProfileMapper.getByUserId(apt.getCounselorId());
                        displayName = profile != null ? profile.getRealName() : "未知咨询师";
                    }

                    // 格式化时间显示
                    String timeDisplay = formatAppointmentTime(apt.getStartTime());

                    return UpcomingAppointmentVO.builder()
                            .id(apt.getId())
                            .time(timeDisplay)
                            .counselor(displayName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 格式化预约时间显示
     */
    private String formatAppointmentTime(LocalDateTime appointmentTime) {
        LocalDate today = LocalDate.now();
        LocalDate appointmentDate = appointmentTime.toLocalDate();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String time = appointmentTime.format(timeFormatter);

        if (appointmentDate.equals(today)) {
            return "今天 " + time;
        } else if (appointmentDate.equals(today.plusDays(1))) {
            return "明天 " + time;
        } else if (appointmentDate.equals(today.plusDays(2))) {
            return "后天 " + time;
        } else {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd");
            return appointmentDate.format(dateFormatter) + " " + time;
        }
    }
}

