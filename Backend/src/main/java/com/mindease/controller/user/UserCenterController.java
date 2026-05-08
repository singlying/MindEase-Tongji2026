package com.mindease.controller.user;

import com.mindease.common.result.Result;
import com.mindease.pojo.vo.DashboardVO;
import com.mindease.pojo.vo.NotificationListVO;
import com.mindease.pojo.vo.OperationResultVO;
import com.mindease.service.UserCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人中心控制器
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserCenterController {

    @Autowired
    private UserCenterService userCenterService;

    /**
     * 获取用户 Dashboard 数据
     *
     * @param userId 当前用户ID（从token中获取）
     * @return
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(@RequestAttribute Long userId) {
        log.info("获取Dashboard数据，用户ID:{}", userId);

        DashboardVO dashboard = userCenterService.getDashboard(userId);

        return Result.success(dashboard);
    }

    /**
     * 获取通知列表
     *
     * @param userId 当前用户ID（从token中获取）
     * @param limit 返回数量（默认20）
     * @return
     */
    @GetMapping("/notifications")
    public Result<NotificationListVO> getNotifications(@RequestAttribute Long userId,
                                                        @RequestParam(required = false, defaultValue = "20") Integer limit) {
        log.info("获取通知列表，用户ID:{}，数量:{}", userId, limit);

        NotificationListVO notifications = userCenterService.getNotifications(userId, limit);

        return Result.success(notifications);
    }

    /**
     * 标记通知为已读
     *
     * @param userId 当前用户ID（从token中获取）
     * @param id 通知ID
     * @return
     */
    @PutMapping("/notification/{id}/read")
    public Result<OperationResultVO> markNotificationAsRead(@RequestAttribute Long userId,
                                                             @PathVariable Long id) {
        log.info("标记通知已读，用户ID:{}，通知ID:{}", userId, id);

        userCenterService.markNotificationAsRead(userId, id);

        return Result.success(OperationResultVO.builder().success(true).build());
    }

    /**
     * 将当前用户的通知全部标记为已读
     *
     * @param userId 当前用户ID（从token中获取）
     * @return
     */
    @PutMapping("/notifications/read-all")
    public Result<OperationResultVO> markAllNotificationsAsRead(@RequestAttribute Long userId) {
        log.info("标记全部通知已读，用户ID:{}", userId);
        userCenterService.markAllNotificationsAsRead(userId);
        return Result.success(OperationResultVO.builder().success(true).build());
    }
}

