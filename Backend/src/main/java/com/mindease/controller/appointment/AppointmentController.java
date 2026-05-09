package com.mindease.controller.appointment;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.AppointmentCancelDTO;
import com.mindease.pojo.dto.AppointmentCreateDTO;
import com.mindease.pojo.dto.ScheduleSetDTO;
import com.mindease.pojo.vo.*;
import com.mindease.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 预约管理控制器
 */
@RestController
@RequestMapping("/appointment")
@Slf4j
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    /**
     * 设置排班（咨询师端）
     *
     * @param userId 当前用户ID（从token中获取）
     * @param scheduleSetDTO 排班数据
     * @return
     */
    @PostMapping("/schedule")
    public Result<Void> setSchedule(@RequestAttribute Long userId,
                                     @RequestBody ScheduleSetDTO scheduleSetDTO) {
        log.info("设置排班，咨询师ID:{}，数据:{}", userId, scheduleSetDTO);

        appointmentService.setSchedule(userId, scheduleSetDTO);

        return Result.success(null, "排班设置成功");
    }

    /**
     * 查询可用时段
     *
     * @param counselorId 咨询师ID
     * @param date 日期（格式：yyyy-MM-dd）
     * @return
     */
    @GetMapping("/available-slots")
    public Result<AvailableSlotsVO> getAvailableSlots(@RequestParam Long counselorId,
                                                       @RequestParam String date) {
        log.info("查询可用时段，咨询师ID:{}，日期:{}", counselorId, date);

        AvailableSlotsVO result = appointmentService.getAvailableSlots(counselorId, date);

        return Result.success(result);
    }

    /**
     * 创建预约
     *
     * @param userId 当前用户ID（从token中获取）
     * @param createDTO 预约信息
     * @return
     */
    @PostMapping("/create")
    public Result<AppointmentCreateVO> createAppointment(@RequestAttribute Long userId,
                                                          @RequestBody AppointmentCreateDTO createDTO) {
        log.info("创建预约，用户ID:{}，数据:{}", userId, createDTO);

        AppointmentCreateVO result = appointmentService.createAppointment(userId, createDTO);

        return Result.success(result);
    }

    /**
     * 获取我的预约列表
     *
     * @param userId 当前用户ID（从token中获取）
     * @param role 用户角色（从token中获取）
     * @param status 状态筛选（可选）
     * @param page 页码
     * @param pageSize 每页数量
     * @return
     */
    @GetMapping("/my-appointments")
    public Result<AppointmentListVO> getMyAppointments(@RequestAttribute Long userId,
                                                        @RequestAttribute String role,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false, defaultValue = "1") Integer page,
                                                        @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        log.info("获取预约列表，用户ID:{}，角色:{}，状态:{}，页码:{}，每页:{}", userId, role, status, page, pageSize);

        AppointmentListVO result = appointmentService.getMyAppointments(userId, role, status, page, pageSize);

        return Result.success(result);
    }

    /**
     * 获取预约详情
     *
     * @param id 预约ID
     * @return
     */
    @GetMapping("/{id}")
    public Result<AppointmentDetailVO> getAppointmentDetail(@PathVariable Long id) {
        log.info("获取预约详情，ID:{}", id);

        AppointmentDetailVO result = appointmentService.getAppointmentDetail(id);

        return Result.success(result);
    }

    /**
     * 取消预约
     *
     * @param userId 当前用户ID（从token中获取）
     * @param id 预约ID
     * @param cancelDTO 取消原因
     * @return
     */
    @PutMapping("/{id}/cancel")
    public Result<OperationResultVO> cancelAppointment(@RequestAttribute Long userId,
                                                        @PathVariable Long id,
                                                        @RequestBody AppointmentCancelDTO cancelDTO) {
        log.info("取消预约，用户ID:{}，预约ID:{}，原因:{}", userId, id, cancelDTO.getCancelReason());

        appointmentService.cancelAppointment(userId, id, cancelDTO);

        return Result.success(OperationResultVO.builder().success(true).build());
    }

    /**
     * 确认预约（咨询师）
     *
     * @param userId 当前用户ID（从token中获取）
     * @param id 预约ID
     * @return
     */
    @PutMapping("/{id}/confirm")
    public Result<OperationResultVO> confirmAppointment(@RequestAttribute Long userId,
                                                         @PathVariable Long id) {
        log.info("确认预约，咨询师ID:{}，预约ID:{}", userId, id);

        appointmentService.confirmAppointment(userId, id);

        return Result.success(OperationResultVO.builder().success(true).build());
    }
}

