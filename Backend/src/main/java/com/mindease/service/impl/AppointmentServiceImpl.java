package com.mindease.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.exception.BaseException;
import com.mindease.mapper.AppointmentMapper;
import com.mindease.mapper.CounselorProfileMapper;
import com.mindease.mapper.SysNotificationMapper;
import com.mindease.mapper.UserMapper;
import com.mindease.pojo.dto.AppointmentCancelDTO;
import com.mindease.pojo.dto.AppointmentCreateDTO;
import com.mindease.pojo.dto.ScheduleSetDTO;
import com.mindease.pojo.entity.Appointment;
import com.mindease.pojo.entity.CounselorProfile;
import com.mindease.pojo.entity.SysNotification;
import com.mindease.pojo.entity.User;
import com.mindease.pojo.vo.*;
import com.mindease.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CounselorProfileMapper counselorProfileMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 设置排班
     */
    @Override
    @Transactional
    public void setSchedule(Long counselorId, ScheduleSetDTO scheduleSetDTO) {
        log.info("设置排班，咨询师ID:{}，排班数据:{}", counselorId, scheduleSetDTO);

        // 验证咨询师资料是否存在
        CounselorProfile profile = counselorProfileMapper.getByUserId(counselorId);
        if (profile == null) {
            throw new BaseException("咨询师资料不存在，请先完善资料");
        }

        try {
            // 构建完整的排班配置 JSON
            String workScheduleJson = objectMapper.writeValueAsString(scheduleSetDTO);
            
            // 更新排班配置
            counselorProfileMapper.updateWorkSchedule(counselorId, workScheduleJson);
            
        } catch (JsonProcessingException e) {
            log.error("排班数据JSON转换失败", e);
            throw new BaseException("排班数据格式错误");
        }
    }

    /**
     * 查询可用时段
     */
    @Override
    public AvailableSlotsVO getAvailableSlots(Long counselorId, String date) {
        log.info("查询可用时段，咨询师ID:{}，日期:{}", counselorId, date);

        // 1. 查询咨询师资料和排班
        CounselorProfile profile = counselorProfileMapper.getByUserId(counselorId);
        if (profile == null) {
            throw new BaseException("咨询师不存在");
        }

        if (profile.getWorkSchedule() == null || profile.getWorkSchedule().trim().isEmpty()) {
            throw new BaseException("咨询师未设置排班");
        }

        // 2. 解析日期
        LocalDate targetDate = LocalDate.parse(date);
        int dayOfWeek = targetDate.getDayOfWeek().getValue(); // 1-7，Monday=1

        // 3. 解析排班数据
        ScheduleSetDTO scheduleData;
        try {
            scheduleData = objectMapper.readValue(profile.getWorkSchedule(), ScheduleSetDTO.class);
        } catch (JsonProcessingException e) {
            log.error("解析排班数据失败", e);
            throw new BaseException("排班数据格式错误");
        }

        List<Integer> workDays = scheduleData.getWorkDays();
        List<ScheduleSetDTO.WorkHour> workHours = scheduleData.getWorkHours();

        // 4. 检查当天是否工作
        if (!workDays.contains(dayOfWeek)) {
            return AvailableSlotsVO.builder()
                    .date(date)
                    .slots(new ArrayList<>())
                    .build();
        }

        // 5. 查询当天已有的预约
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();
        List<Appointment> existingAppointments = appointmentMapper.getByDate(counselorId, dayStart, dayEnd);

        // 6. 生成时段列表（使用 LinkedHashMap 去重并保持顺序）
        java.util.LinkedHashMap<String, TimeSlotVO> slotMap = new java.util.LinkedHashMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (ScheduleSetDTO.WorkHour workHour : workHours) {
            LocalTime start = LocalTime.parse(workHour.getStart(), timeFormatter);
            LocalTime end = LocalTime.parse(workHour.getEnd(), timeFormatter);

            // 按1小时分割时段
            LocalTime currentStart = start;
            while (currentStart.plusHours(1).isBefore(end) || currentStart.plusHours(1).equals(end)) {
                LocalTime currentEnd = currentStart.plusHours(1);
                LocalDateTime slotStart = LocalDateTime.of(targetDate, currentStart);
                LocalDateTime slotEnd = LocalDateTime.of(targetDate, currentEnd);

                // 使用时段的开始时间作为唯一key，避免重复
                String slotKey = currentStart.format(timeFormatter) + "-" + currentEnd.format(timeFormatter);
                
                // 只添加未存在的时段
                if (!slotMap.containsKey(slotKey)) {
                    // 获取当前时间
                    LocalDateTime now = LocalDateTime.now();
                    
                    // 检查时间段是否已过去：如果时间段的结束时间已经小于或等于当前时间，则不可用
                    boolean isPast = slotEnd.isBefore(now) || slotEnd.isEqual(now);
                    
                    // 检查是否已被预约
                    boolean isBooked = existingAppointments.stream()
                            .anyMatch(apt ->
                                    (apt.getStartTime().isBefore(slotEnd) && apt.getEndTime().isAfter(slotStart))
                            );
                    
                    // 时间段可用 = 未过去 && 未被预约
                    boolean isAvailable = !isPast && !isBooked;

                    slotMap.put(slotKey, TimeSlotVO.builder()
                            .startTime(currentStart.format(timeFormatter))
                            .endTime(currentEnd.format(timeFormatter))
                            .available(isAvailable)
                            .build());
                }

                currentStart = currentEnd;
            }
        }

        return AvailableSlotsVO.builder()
                .date(date)
                .slots(new ArrayList<>(slotMap.values()))
                .build();
    }

    /**
     * 创建预约
     */
    @Override
    @Transactional
    public AppointmentCreateVO createAppointment(Long userId, AppointmentCreateDTO createDTO) {
        log.info("创建预约，用户ID:{}，预约数据:{}", userId, createDTO);

        // 1. 验证咨询师是否存在
        CounselorProfile profile = counselorProfileMapper.getByUserId(createDTO.getCounselorId());
        if (profile == null) {
            throw new BaseException("咨询师不存在");
        }

        // 2. 检查时段是否可用
        int conflictCount = appointmentMapper.countByTimeRange(
                createDTO.getCounselorId(),
                createDTO.getStartTime(),
                createDTO.getEndTime()
        );

        if (conflictCount > 0) {
            throw new BaseException("该时段已被预约");
        }

        // 3. 创建预约
        Appointment appointment = Appointment.builder()
                .userId(userId)
                .counselorId(createDTO.getCounselorId())
                .startTime(createDTO.getStartTime())
                .endTime(createDTO.getEndTime())
                .status("PENDING")
                .userNote(createDTO.getUserNote())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        appointmentMapper.insert(appointment);

        // 4. 发送通知给咨询师
        sendNewAppointmentNotification(userId, createDTO.getCounselorId(), createDTO.getStartTime());

        return AppointmentCreateVO.builder()
                .appointmentId(appointment.getId())
                .status(appointment.getStatus())
                .build();
    }

    /**
     * 获取我的预约列表
     */
    @Override
    public AppointmentListVO getMyAppointments(Long userId, String userRole, String status, Integer page, Integer pageSize) {
        log.info("获取预约列表，用户ID:{}，角色:{}，状态:{}，页码:{}，每页:{}", userId, userRole, status, page, pageSize);

        // 在查询预约列表前，自动更新已过期的预约状态
        autoCompleteExpiredAppointments(userId);

        int offset = (page - 1) * pageSize;

        List<Appointment> appointments;
        int total;

        // 根据用户角色查询
        if ("COUNSELOR".equals(userRole)) {
            // 咨询师查询自己的预约
            appointments = appointmentMapper.getByCounselorId(userId, status, pageSize, offset);
            total = appointmentMapper.count(null, userId, status);
        } else {
            // 用户查询自己的预约
            appointments = appointmentMapper.getByUserId(userId, status, pageSize, offset);
            total = appointmentMapper.count(userId, null, status);
        }

        // 构建VO
        List<AppointmentListItemVO> items = appointments.stream().map(apt -> {
            String targetName;
            String targetAvatar;
            String targetRole;

            if ("COUNSELOR".equals(userRole)) {
                // 咨询师看到用户信息
                User user = userMapper.getById(apt.getUserId());
                targetName = user != null ? user.getNickname() : "未知用户";
                targetAvatar = user != null ? user.getAvatar() : null;
                targetRole = "user";
            } else {
                // 用户看到咨询师信息
                CounselorProfile profile = counselorProfileMapper.getByUserId(apt.getCounselorId());
                targetName = profile != null ? profile.getRealName() : "未知咨询师";
                User counselorUser = userMapper.getById(apt.getCounselorId());
                targetAvatar = counselorUser != null ? counselorUser.getAvatar() : null;
                targetRole = "counselor";
            }

            return AppointmentListItemVO.builder()
                    .id(apt.getId())
                    .startTime(apt.getStartTime())
                    .endTime(apt.getEndTime())
                    .status(apt.getStatus())
                    .targetName(targetName)
                    .targetAvatar(targetAvatar)
                    .targetRole(targetRole)
                    .userId(apt.getUserId())  // 添加用户ID字段（咨询师需要此字段调用报告导出接口）
                    .build();
        }).collect(Collectors.toList());

        return AppointmentListVO.builder()
                .total(total)
                .list(items)
                .build();
    }

    /**
     * 获取预约详情
     */
    @Override
    public AppointmentDetailVO getAppointmentDetail(Long appointmentId) {
        log.info("获取预约详情，ID:{}", appointmentId);

        Appointment appointment = appointmentMapper.getById(appointmentId);
        if (appointment == null) {
            throw new BaseException("预约不存在");
        }

        CounselorProfile profile = counselorProfileMapper.getByUserId(appointment.getCounselorId());
        String counselorName = profile != null ? profile.getRealName() : "未知咨询师";

        return AppointmentDetailVO.builder()
                .id(appointment.getId())
                .counselorId(appointment.getCounselorId())
                .counselorName(counselorName)
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .userNote(appointment.getUserNote())
                .cancelReason(appointment.getCancelReason())
                .userId(appointment.getUserId())  // 添加用户ID字段（咨询师需要此字段调用报告导出接口）
                .build();
    }

    /**
     * 取消预约
     */
    @Override
    @Transactional
    public void cancelAppointment(Long userId, Long appointmentId, AppointmentCancelDTO cancelDTO) {
        log.info("取消预约，用户ID:{}，预约ID:{}，原因:{}", userId, appointmentId, cancelDTO.getCancelReason());

        Appointment appointment = appointmentMapper.getById(appointmentId);
        if (appointment == null) {
            throw new BaseException("预约不存在");
        }

        // 验证权限（用户或咨询师都可以取消）
        if (!appointment.getUserId().equals(userId) && !appointment.getCounselorId().equals(userId)) {
            throw new BaseException("无权操作此预约");
        }

        // 验证状态
        if ("CANCELLED".equals(appointment.getStatus()) || "COMPLETED".equals(appointment.getStatus())) {
            throw new BaseException("预约已" + ("CANCELLED".equals(appointment.getStatus()) ? "取消" : "完成") + "，无法再次取消");
        }

        appointmentMapper.updateStatus(appointmentId, "CANCELLED", cancelDTO.getCancelReason(), LocalDateTime.now());

        // 发送通知给对方
        Long receiverId = appointment.getUserId().equals(userId) ? appointment.getCounselorId() : appointment.getUserId();
        sendCancelNotification(receiverId, appointment.getStartTime(), cancelDTO.getCancelReason());
    }

    /**
     * 确认预约（咨询师）
     */
    @Override
    @Transactional
    public void confirmAppointment(Long counselorId, Long appointmentId) {
        log.info("确认预约，咨询师ID:{}，预约ID:{}", counselorId, appointmentId);

        Appointment appointment = appointmentMapper.getById(appointmentId);
        if (appointment == null) {
            throw new BaseException("预约不存在");
        }

        // 验证权限
        if (!appointment.getCounselorId().equals(counselorId)) {
            throw new BaseException("无权操作此预约");
        }

        // 验证状态
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BaseException("预约状态不是待确认，无法确认");
        }

        appointmentMapper.updateStatus(appointmentId, "CONFIRMED", null, LocalDateTime.now());

        // 发送通知给用户
        sendConfirmNotification(appointment.getUserId(), counselorId, appointment.getStartTime());
    }

    /**
     * 自动更新已过期的确认预约为已完成状态
     * 检查条件：
     * 1. 预约状态为CONFIRMED（已确认）
     * 2. 预约结束时间已经过去
     */
    @Override
    @Transactional
    public int autoCompleteExpiredAppointments(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 查询需要更新的预约
        List<Appointment> expiredAppointments = appointmentMapper.getExpiredConfirmedAppointments(userId, now);
        
        if (expiredAppointments.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (Appointment appointment : expiredAppointments) {
            try {
                // 更新状态为已完成
                appointmentMapper.updateStatus(appointment.getId(), "COMPLETED", null, now);
                
                // 发送通知给用户，提醒可以评价
                sendCompletionNotification(appointment.getUserId(), appointment.getCounselorId(), appointment.getStartTime());
                
                updatedCount++;
                log.info("自动完成预约，预约ID: {}, 用户ID: {}, 咨询师ID: {}", 
                        appointment.getId(), appointment.getUserId(), appointment.getCounselorId());
            } catch (Exception e) {
                log.error("自动完成预约失败，预约ID: {}", appointment.getId(), e);
            }
        }

        log.info("自动完成预约任务执行完毕，总共更新 {} 条记录", updatedCount);
        return updatedCount;
    }

    /**
     * 发送新预约通知给咨询师
     */
    private void sendNewAppointmentNotification(Long userId, Long counselorId, LocalDateTime startTime) {
        try {
            User user = userMapper.getById(userId);
            String userNickname = user != null ? user.getNickname() : "用户";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String timeStr = startTime.format(formatter);

            SysNotification notification = SysNotification.builder()
                    .userId(counselorId)
                    .type("appointment")
                    .title("新预约提醒")
                    .content(String.format("用户 %s 预约了您的咨询服务，时间：%s，请及时确认。", userNickname, timeStr))
                    .isRead(0)
                    .createTime(LocalDateTime.now())
                    .build();

            notificationMapper.insert(notification);
            log.info("已发送新预约通知给咨询师，咨询师ID:{}", counselorId);
        } catch (Exception e) {
            log.error("发送新预约通知失败", e);
            // 不影响主流程，只记录日志
        }
    }

    /**
     * 发送预约确认通知给用户
     */
    private void sendConfirmNotification(Long userId, Long counselorId, LocalDateTime startTime) {
        try {
            CounselorProfile profile = counselorProfileMapper.getByUserId(counselorId);
            String counselorName = profile != null ? profile.getRealName() : "咨询师";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
            String timeStr = startTime.format(formatter);

            SysNotification notification = SysNotification.builder()
                    .userId(userId)
                    .type("appointment")
                    .title("预约已确认")
                    .content(String.format("%s已确认您的预约（%s），请准时参加。", counselorName, timeStr))
                    .isRead(0)
                    .createTime(LocalDateTime.now())
                    .build();

            notificationMapper.insert(notification);
            log.info("已发送预约确认通知给用户，用户ID:{}", userId);
        } catch (Exception e) {
            log.error("发送预约确认通知失败", e);
            // 不影响主流程，只记录日志
        }
    }

    /**
     * 发送预约取消通知
     */
    private void sendCancelNotification(Long receiverId, LocalDateTime startTime, String reason) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
            String timeStr = startTime.format(formatter);

            String content = String.format("原本定于 %s 的预约已被取消。", timeStr);
            if (reason != null && !reason.isEmpty()) {
                content += "原因：" + reason;
            }

            SysNotification notification = SysNotification.builder()
                    .userId(receiverId)
                    .type("appointment")
                    .title("预约已取消")
                    .content(content)
                    .isRead(0)
                    .createTime(LocalDateTime.now())
                    .build();

            notificationMapper.insert(notification);
            log.info("已发送预约取消通知，接收人ID:{}", receiverId);
        } catch (Exception e) {
            log.error("发送预约取消通知失败", e);
            // 不影响主流程，只记录日志
        }
    }

    /**
     * 发送咨询完成通知给用户
     */
    private void sendCompletionNotification(Long userId, Long counselorId, LocalDateTime startTime) {
        try {
            CounselorProfile profile = counselorProfileMapper.getByUserId(counselorId);
            String counselorName = profile != null ? profile.getRealName() : "咨询师";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
            String timeStr = startTime.format(formatter);

            SysNotification notification = SysNotification.builder()
                    .userId(userId)
                    .type("appointment")
                    .title("咨询已完成")
                    .content(String.format("您与 %s 的咨询（%s）已完成，欢迎对本次咨询进行评价。", counselorName, timeStr))
                    .isRead(0)
                    .createTime(LocalDateTime.now())
                    .build();

            notificationMapper.insert(notification);
            log.info("已发送咨询完成通知给用户，用户ID:{}", userId);
        } catch (Exception e) {
            log.error("发送咨询完成通知失败", e);
            // 不影响主流程，只记录日志
        }
    }
}

