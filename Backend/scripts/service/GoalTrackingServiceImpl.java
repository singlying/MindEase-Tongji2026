package com.mindease.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.exception.BaseException;
import com.mindease.mapper.GoalMapper;
import com.mindease.mapper.GoalCheckInMapper;
import com.mindease.pojo.dto.GoalCreateDTO;
import com.mindease.pojo.dto.GoalUpdateDTO;
import com.mindease.pojo.entity.Goal;
import com.mindease.pojo.entity.GoalCheckIn;
import com.mindease.pojo.vo.*;
import com.mindease.service.GoalTrackingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 心理健康目标追踪服务实现类
 * 支持目标设定、每日打卡、进度统计、日历展示等功能
 */
@Service
@Slf4j
public class GoalTrackingServiceImpl implements GoalTrackingService {

    private static final String GOAL_CHECKIN_KEY = "mindease:goal:checkin:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private GoalMapper goalMapper;

    @Autowired
    private GoalCheckInMapper checkInMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 预定义的目标类别和对应的建议
    private static final Map<String, List<String>> GOAL_TEMPLATES = new LinkedHashMap<>();
    static {
        GOAL_TEMPLATES.put("sleep", Arrays.asList(
                "每晚23:00前入睡", "睡前30分钟不看手机", "每天睡眠不少于7小时"
        ));
        GOAL_TEMPLATES.put("exercise", Arrays.asList(
                "每天步行30分钟", "每周运动3次，每次30分钟以上", "每天做10分钟拉伸"
        ));
        GOAL_TEMPLATES.put("meditation", Arrays.asList(
                "每天冥想10分钟", "每周完成3次正念练习", "每天进行5分钟深呼吸训练"
        ));
        GOAL_TEMPLATES.put("emotion", Arrays.asList(
                "每天记录一条情绪日记", "每周进行一次自我反思", "学会识别并命名自己的情绪"
        ));
        GOAL_TEMPLATES.put("social", Arrays.asList(
                "每周与朋友联系至少3次", "每月参加1次社交活动", "每天对一个人表达感谢"
        ));
    }

    @Override
    public GoalDetailVO createGoal(GoalCreateDTO goalDTO, Long userId) {
        log.info("用户 {} 创建新目标: {}", userId, goalDTO.getTitle());

        // 参数校验
        if (goalDTO.getTitle() == null || goalDTO.getTitle().trim().isEmpty()) {
            throw new BaseException("目标标题不能为空");
        }
        if (goalDTO.getTargetDate() != null && goalDTO.getTargetDate().isBefore(LocalDate.now())) {
            throw new BaseException("目标截止日期不能早于今天");
        }

        Goal goal = new Goal();
        BeanUtils.copyProperties(goalDTO, goal);
        goal.setUserId(userId);
        goal.setStatus("ACTIVE");
        goal.setCurrentStreak(0);
        goal.setLongestStreak(0);
        goal.setTotalCheckIns(0);
        goal.setProgress(0.0);
        goal.setCreateTime(LocalDateTime.now());
        goal.setUpdateTime(LocalDateTime.now());

        // 根据频率设置默认提醒
        if (goal.getFrequency() == null) {
            goal.setFrequency("daily");  // 默认每日
        }

        // 生成AI辅助建议
        goal.setAiSuggestion(generateGoalSuggestion(goal));

        goalMapper.insert(goal);

        log.info("目标创建成功, ID: {}", goal.getId());
        return convertToDetailVO(goal, Collections.emptyList());
    }

    @Override
    public GoalListVO getGoalList(Long userId, String status) {
        log.info("获取目标列表, userId={}, status={}", userId, status);

        List<Goal> goals = goalMapper.selectByUserIdAndStatus(userId, status);
        
        List<GoalSummaryVO> items = goals.stream()
                .map(this::convertToSummaryVO)
                .collect(Collectors.toList());

        return GoalListVO.builder()
                .total(items.size())
                .items(items)
                .build();
    }

    @Override
    public GoalDetailVO getGoalDetail(Long goalId, Long userId) {
        log.info("获取目标详情, goalId={}, userId={}", goalId, userId);

        Goal goal = goalMapper.selectById(goalId);
        if (goal == null) {
            throw new BaseException("目标不存在");
        }
        if (!goal.getUserId().equals(userId)) {
            throw new BaseException("无权查看此目标");
        }

        // 获取最近的打卡记录（最近14天）
        LocalDate since = LocalDate.now().minusDays(13);
        List<GoalCheckIn> recentCheckIns = checkInMapper.selectByGoalIdSince(goalId, since);

        // 获取今日是否已打卡
        boolean todayChecked = isTodayChecked(goalId, userId);

        return convertToDetailVO(goal, recentCheckIns, todayChecked);
    }

    @Override
    public GoalDetailVO updateGoal(Long goalId, GoalUpdateDTO updateDTO, Long userId) {
        log.info("更新目标, goalId={}", goalId);

        Goal goal = verifyOwnership(goalId, userId);

        if (updateDTO.getTitle() != null) {
            goal.setTitle(updateDTO.getTitle().trim());
        }
        if (updateDTO.getDescription() != null) {
            goal.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getTargetDate() != null) {
            if (updateDTO.getTargetDate().isBefore(LocalDate.now())) {
                throw new BaseException("截止日期不能早于今天");
            }
            goal.setTargetDate(updateDTO.getTargetDate());
        }
        if (updateDTO.getFrequency() != null) {
            goal.setFrequency(updateDTO.getFrequency());
        }
        if (updateDTO.getReminderTime() != null) {
            goal.setReminderTime(updateDTO.getReminderTime());
        }

        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标更新成功, goalId={}", goalId);
        return getGoalDetail(goalId, userId);
    }

    @Override
    public GoalCheckInVO checkIn(Long goalId, Long userId, String note) {
        log.info("目标打卡, goalId={}, userId={}", goalId, userId);

        Goal goal = verifyOwnership(goalId, userId);
        if (!"ACTIVE".equals(goal.getStatus())) {
            throw new BaseException("当前目标状态不支持打卡");
        }

        LocalDate today = LocalDate.now();

        // 检查今日是否已打卡
        if (isTodayChecked(goalId, userId)) {
            throw new BaseException("今日已完成打卡，无需重复操作");
        }

        // 创建打卡记录
        GoalCheckIn checkIn = new GoalCheckIn();
        checkIn.setGoalId(goalId);
        checkIn.setUserId(userId);
        checkIn.setCheckDate(today);
        checkIn.setNote(note != null ? note.trim() : "");
        checkIn.setCreateTime(LocalDateTime.now());

        checkInMapper.insert(checkIn);

        // 更新目标的连续天数等统计数据
        updateGoalStatsAfterCheckIn(goal, today);

        log.info("打卡成功, goalId={}, date={}", goalId, today);

        return GoalCheckInVO.builder()
                .success(true)
                .checkDate(today)
                .currentStreak(goal.getCurrentStreak())
                .totalCheckIns(goal.getTotalCheckIns())
                .message("打卡成功！坚持就是胜利 💪")
                .build();
    }

    @Override
    public Boolean pauseGoal(Long goalId, String reason, Long userId) {
        log.info("暂停目标, goalId={}", goalId);

        Goal goal = verifyOwnership(goalId, userId);
        goal.setStatus("PAUSED");
        goal.setPauseReason(reason);
        goal.setPauseTime(LocalDateTime.now());
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标已暂停, goalId={}", goalId);
        return true;
    }

    @Override
    public Boolean completeGoal(Long goalId, String summary, Long userId) {
        log.info("完成目标, goalId={}", goalId);

        Goal goal = verifyOwnership(goalId, userId);
        goal.setStatus("COMPLETED");
        goal.setCompletionSummary(summary);
        goal.setCompleteTime(LocalDateTime.now());
        goal.setProgress(100.0);
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        // 记录完成成就（可扩展：发放徽章、增加积分等）
        logAchievement(userId, goal);

        log.info("恭喜! 目标已完成, goalId={}, 总打卡{}次",
                 goalId, goal.getTotalCheckIns());
        return true;
    }

    @Override
    public Boolean deleteGoal(Long goalId, Long userId) {
        log.info("删除目标, goalId={}", goalId);

        Goal goal = verifyOwnership(goalId, userId);
        goal.setStatus("DELETED");
        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        log.info("目标已删除, goalId={}", goalId);
        return true;
    }

    @Override
    public GoalStatisticsVO getGoalStatistics(Long userId) {
        log.info("获取目标统计数据, userId={}", userId);

        // 各状态目标数量
        int activeCount = goalMapper.countByUserIdAndStatus(userId, "ACTIVE");
        int completedCount = goalMapper.countByUserIdAndStatus(userId, "COMPLETED");
        int pausedCount = goalMapper.countByUserIdAndStatus(userId, "PAUSED");

        // 本周打卡情况
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int weeklyCheckIns = checkInMapper.countByUserIdSince(userId, weekStart);

        // 本月打卡情况
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        int monthlyCheckIns = checkInMapper.countByUserIdSince(userId, monthStart);

        // 当前最长连续记录
        Integer longestStreak = goalMapper.getMaxLongestStreak(userId);
        if (longestStreak == null) longestStreak = 0;

        // 今日打卡数
        int todayChecked = countTodayCheckIns(userId);

        // 获取各分类目标数量
        Map<String, Integer> categoryStats = goalMapper.getCountByCategory(userId);

        // 完成率
        int totalCreated = activeCount + completedCount + pausedCount;
        double completionRate = totalCreated > 0 ?
                Math.round((double) completedCount / totalCreated * 10000.0) / 100.0 : 0.0;

        return GoalStatisticsVO.builder()
                .activeGoals(activeCount)
                .completedGoals(completedCount)
                .pausedGoals(pausedCount)
                .weeklyCheckIns(weeklyCheckIns)
                .monthlyCheckIns(monthlyCheckIns)
                .todayCheckIns(todayChecked)
                .longestStreak(longestStreak)
                .categoryDistribution(categoryStats)
                .completionRate(completionRate)
                .build();
    }

    @Override
    public GoalCalendarVO getCheckInCalendar(Long goalId, String yearMonth, Long userId) {
        log.info("获取打卡日历, goalId={}, yearMonth={}", goalId, yearMonth);

        verifyOwnership(goalId, userId);

        // 解析年月
        LocalDate targetMonth;
        try {
            targetMonth = LocalDate.parse(yearMonth + "-01", DATE_FORMATTER);
        } catch (Exception e) {
            throw new BaseException("日期格式错误，请使用 yyyy-MM 格式");
        }

        // 获取该月的所有打卡记录
        LocalDate monthStart = targetMonth.withDayOfMonth(1);
        LocalDate monthEnd = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth());

        List<GoalCheckIn> monthlyRecords = checkInMapper.selectByGoalIdBetweenDates(
                goalId, monthStart, monthEnd
        );

        Set<LocalDate> checkedDates = monthlyRecords.stream()
                .map(GoalCheckIn::getCheckDate)
                .collect(Collectors.toSet());

        // 构建日历数据
        List<Map<String, Object>> calendarDays = new ArrayList<>();
        for (int day = 1; day <= targetMonth.lengthOfMonth(); day++) {
            LocalDate date = targetMonth.withDayOfMonth(day);
            boolean isChecked = checkedDates.contains(date);
            boolean isFuture = date.isAfter(LocalDate.now());
            boolean isToday = date.isEqual(LocalDate.now());

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(DATE_FORMATTER));
            dayData.put("dayOfMonth", day);
            dayData.put("checked", isChecked);
            dayData.put("isFuture", isFuture);
            dayData.put("isToday", isToday);
            calendarDays.add(dayData);
        }

        return GoalCalendarVO.builder()
                .yearMonth(yearMonth)
                .monthName(targetMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")))
                .checkedDays(checkedDates.stream()
                        .map(d -> d.format(DATE_FORMATTER))
                        .collect(Collectors.toList()))
                .totalCheckedDays(checkedDates.size())
                .totalDays(targetMonth.lengthOfMonth())
                .calendar(calendarDays)
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验目标归属权
     */
    private Goal verifyOwnership(Long goalId, Long userId) {
        Goal goal = goalMapper.selectById(goalId);
        if (goal == null || "DELETED".equals(goal.getStatus())) {
            throw new BaseException("目标不存在或已被删除");
        }
        if (!goal.getUserId().equals(userId)) {
            throw new BaseException("无权操作此目标");
        }
        return goal;
    }

    /**
     * 检查今日是否已打卡
     */
    private boolean isTodayChecked(Long goalId, Long userId) {
        String redisKey = GOAL_CHECKIN_KEY + goalId + ":" + userId + ":" +
                          LocalDate.now().format(DATE_FORMATTER);
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (exists != null && exists) {
            return true;
        }
        // Redis未命中则查DB
        int count = checkInMapper.existsByGoalAndDate(goalId, userId, LocalDate.now());
        if (count > 0) {
            stringRedisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
            return true;
        }
        return false;
    }

    /**
     * 统计用户今日总打卡数
     */
    private int countTodayCheckIns(Long userId) {
        return checkInMapper.countByUserOnDate(userId, LocalDate.now());
    }

    /**
     * 打卡后更新目标统计
     */
    private void updateGoalStatsAfterCheckIn(Goal goal, LocalDate checkDate) {
        // 更新总打卡次数
        goal.setTotalCheckIns(goal.getTotalCheckIns() + 1);

        // 更新连续天数
        LocalDate yesterday = checkDate.minusDays(1);
        boolean wasYesterdayChecked = checkInMapper.existsByGoalAndDate(
                goal.getId(), goal.getUserId(), yesterday
        ) > 0;

        if (wasYesterdayChecked) {
            goal.setCurrentStreak(goal.getCurrentStreak() + 1);
        } else {
            goal.setCurrentStreak(1);  // 重新开始计算连续天数
        }

        // 更新最长连续记录
        if (goal.getCurrentStreak() > goal.getLongestStreak()) {
            goal.setLongestStreak(goal.getCurrentStreak());
        }

        // 计算进度百分比
        if (goal.getTargetDate() != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                    goal.getCreateTime().toLocalDate(), goal.getTargetDate()
            );
            if (daysBetween > 0) {
                double progress = Math.min(100.0,
                        (double) goal.getTotalCheckIns() / daysBetween * 100.0);
                goal.setProgress(Math.round(progress * 100.0) / 100.0);
            }
        } else {
            // 无截止日期时，按累计打卡数估算进度
            double progress = Math.min(100.0,
                    (double) goal.getTotalCheckIns() * 2.0);  // 每2次=1%
            goal.setProgress(Math.round(progress * 100.0) / 100.0);
        }

        goal.setUpdateTime(LocalDateTime.now());
        goalMapper.updateById(goal);

        // 缓存今日打卡标记
        String redisKey = GOAL_CHECKIN_KEY + goal.getId() + ":" + goal.getUserId() + ":" +
                          checkDate.format(DATE_FORMATTER);
        stringRedisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
    }

    /**
     * 生成AI辅助建议
     */
    private String generateGoalSuggestion(Goal goal) {
        String category = goal.getCategory();
        List<String> suggestions = GOAL_TEMPLATES.getOrDefault(category, Collections.emptyList());

        StringBuilder sb = new StringBuilder();
        sb.append("基于「").append(goal.getTitle()).append("」这个目标，建议你：\n\n");

        if (!suggestions.isEmpty()) {
            sb.append("**推荐做法**：\n");
            for (int i = 0; i < suggestions.size(); i++) {
                sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("**小贴士**：\n");
        sb.append("- 从小目标开始，逐步建立习惯\n");
        sb.append("- 设定固定的时间和地点来执行目标\n");
        sb.append("- 找一个伙伴互相监督，效果更好哦\n");

        return sb.toString();
    }

    /**
     * 记录成就（预留扩展）
     */
    private void logAchievement(Long userId, Goal goal) {
        log.info("🎉 用户 {} 完成目标: [{}] 连续{}天, 共打卡{}次",
                userId, goal.getTitle(), goal.getLongestStreak(), goal.getTotalCheckIns());
        // TODO: 可扩展 - 写入成就表、触发通知、发放积分等
    }

    /**
     * 转换为目标详情VO
     */
    private GoalDetailVO convertToDetailVO(Goal goal, List<GoalCheckIn> recentCheckIns) {
        return convertToDetailVO(goal, recentCheckIns, false);
    }

    private GoalDetailVO convertToDetailVO(Goal goal, List<GoalCheckIn> recentCheckIns,
                                           boolean todayChecked) {
        return GoalDetailVO.builder()
                .goalId(goal.getId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .category(goal.getCategory())
                .frequency(goal.getFrequency())
                .targetDate(goal.getTargetDate())
                .status(goal.getStatus())
                .progress(goal.getProgress())
                .currentStreak(goal.getCurrentStreak())
                .longestStreak(goal.getLongestStreak())
                .totalCheckIns(goal.getTotalCheckIns())
                .aiSuggestion(goal.getAiSuggestion())
                .todayChecked(todayChecked)
                .recentCheckIns(recentCheckIns.stream()
                        .map(ci -> GoalRecentCheckInVO.builder()
                                .date(ci.getCheckDate().format(DATE_FORMATTER))
                                .note(ci.getNote())
                                .build())
                        .collect(Collectors.toList()))
                .createTime(goal.getCreateTime())
                .updateTime(goal.getUpdateTime())
                .build();
    }

    /**
     * 转换为目标摘要VO
     */
    private GoalSummaryVO convertToSummaryVO(Goal goal) {
        return GoalSummaryVO.builder()
                .goalId(goal.getId())
                .title(goal.getTitle())
                .category(goal.getCategory())
                .status(goal.getStatus())
                .progress(goal.getProgress())
                .currentStreak(goal.getCurrentStreak())
                .totalCheckIns(goal.getTotalCheckIns())
                .targetDate(goal.getTargetDate())
                .build();
    }
}
