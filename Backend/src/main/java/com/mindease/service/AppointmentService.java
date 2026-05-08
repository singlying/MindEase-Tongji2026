package com.mindease.service;

import com.mindease.pojo.dto.AppointmentCancelDTO;
import com.mindease.pojo.dto.AppointmentCreateDTO;
import com.mindease.pojo.dto.ScheduleSetDTO;
import com.mindease.pojo.vo.*;

public interface AppointmentService {

    /**
     * 设置排班
     *
     * @param counselorId
     * @param scheduleSetDTO
     */
    void setSchedule(Long counselorId, ScheduleSetDTO scheduleSetDTO);

    /**
     * 查询可用时段
     *
     * @param counselorId
     * @param date
     * @return
     */
    AvailableSlotsVO getAvailableSlots(Long counselorId, String date);

    /**
     * 创建预约
     *
     * @param userId
     * @param createDTO
     * @return
     */
    AppointmentCreateVO createAppointment(Long userId, AppointmentCreateDTO createDTO);

    /**
     * 获取我的预约列表
     *
     * @param userId
     * @param userRole
     * @param status
     * @param page
     * @param pageSize
     * @return
     */
    AppointmentListVO getMyAppointments(Long userId, String userRole, String status, Integer page, Integer pageSize);

    /**
     * 获取预约详情
     *
     * @param appointmentId
     * @return
     */
    AppointmentDetailVO getAppointmentDetail(Long appointmentId);

    /**
     * 取消预约
     *
     * @param userId
     * @param appointmentId
     * @param cancelDTO
     */
    void cancelAppointment(Long userId, Long appointmentId, AppointmentCancelDTO cancelDTO);

    /**
     * 确认预约（咨询师）
     *
     * @param counselorId
     * @param appointmentId
     */
    void confirmAppointment(Long counselorId, Long appointmentId);

    /**
     * 自动更新已过期的确认预约为已完成状态
     * 检查条件：预约状态为CONFIRMED且结束时间已过
     *
     * @param userId 用户ID（可选，如果提供则只更新该用户相关的预约）
     * @return 更新的预约数量
     */
    int autoCompleteExpiredAppointments(Long userId);
}

