package com.mindease.service;

import com.mindease.pojo.dto.GoalCreateDTO;
import com.mindease.pojo.dto.GoalUpdateDTO;
import com.mindease.pojo.vo.*;

import java.util.List;

/**
 * 心理健康目标追踪服务接口
 * 帮助用户设定和跟踪个人心理健康改善目标
 */
public interface GoalTrackingService {

    /**
     * 创建新的心理目标
     *
     * @param goalDTO   目标信息DTO
     * @param userId    用户ID
     * @return 创建的目标VO
     */
    GoalDetailVO createGoal(GoalCreateDTO goalDTO, Long userId);

    /**
     * 获取用户的全部目标列表
     *
     * @param userId    用户ID
     * @param status    筛选状态: ACTIVE/COMPLETED/PAUSED/CANCELLED，null表示全部
     * @return 目标列表VO
     */
    GoalListVO getGoalList(Long userId, String status);

    /**
     * 获取目标详情（含打卡记录）
     *
     * @param goalId    目标ID
     * @param userId    用户ID (用于权限校验)
     * @return 目标详情VO
     */
    GoalDetailVO getGoalDetail(Long goalId, Long userId);

    /**
     * 更新目标信息
     *
     * @param goalId    目标ID
     * @param updateDTO 更新内容DTO
     * @param userId    操作用户ID
     * @return 更新后的目标VO
     */
    GoalDetailVO updateGoal(Long goalId, GoalUpdateDTO updateDTO, Long userId);

    /**
     * 执行目标打卡
     *
     * @param goalId    目标ID
     * @param userId    用户ID
     * @param note      打卡备注/感受记录
     * @return 打卡结果VO
     }
     GoalCheckInVO checkIn(Long goalId, Long userId, String note);

    /**
     * 暂停目标
     *
     * @param goalId    目标ID
     * @param reason    暂停原因
     * @param userId    用户ID
     * @return 是否成功
     */
    Boolean pauseGoal(Long goalId, String reason, Long userId);

    /**
     * 完成目标（标记为已达成）
     *
     * @param goalId    目标ID
     * @param summary   总结感想
     * @param userId    用户ID
     * @return 是否成功
     */
    Boolean completeGoal(Long goalId, String summary, Long userId);

    /**
     * 删除目标（软删除）
     *
     * @param goalId    目标ID
     * @param userId    用户ID
     * @return 是否成功
     */
    Boolean deleteGoal(Long goalId, Long userId);

    /**
     * 获取目标统计数据概览
     *
     * @param userId    用户ID
     * @return 统计概览VO
     */
    GoalStatisticsVO getGoalStatistics(Long userId);

    /**
     * 获取目标的打卡日历数据
     *
     * @param goalId    目标ID
     * @param yearMonth 年月 "yyyy-MM"
     * @param userId    用户ID
     * @return 日历数据VO
     */
     GoalCalendarVO getCheckInCalendar(Long goalId, String yearMonth, Long userId);
}
