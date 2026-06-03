package com.mindease.controller.mood;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.MoodLogDTO;
import com.mindease.pojo.vo.*;
import com.mindease.service.MoodService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mood")
@Slf4j
public class MoodController {

    @Autowired
    private MoodService moodService;

    /**
     * 提交情绪日记
     */
    @PostMapping("/log")
    public Result<MoodLogVO> submitMoodLog(@RequestBody MoodLogDTO moodLogDTO, HttpServletRequest request) {
        log.info("提交情绪日记：{}", moodLogDTO);

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID：{}", userId);

        MoodLogVO moodLogVO = moodService.submitMoodLog(moodLogDTO, userId);
        
        return Result.success(moodLogVO);
    }

    /**
     * 获取情绪日记列表
     */
    @GetMapping("/logs")
    public Result<MoodLogListVO> getMoodLogList(@RequestParam(defaultValue = "10") Integer limit,
                                                @RequestParam(defaultValue = "0") Integer offset,
                                                HttpServletRequest request) {
        log.info("获取情绪日记列表，limit: {}, offset: {}", limit, offset);

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID：{}", userId);

        MoodLogListVO result = moodService.getMoodLogList(userId, limit, offset);
        
        return Result.success(result);
    }

    /**
     * 获取单条日记详情
     */
    @GetMapping("/log/{id}")
    public Result<MoodLogDetailVO> getMoodLogDetail(@PathVariable Long id) {
        log.info("获取情绪日记详情，ID: {}", id);

        MoodLogDetailVO result = moodService.getMoodLogDetail(id);
        
        return Result.success(result);
    }

    /**
     * 删除日记
     */
    @DeleteMapping("/log/{id}")
    public Result<Boolean> deleteMoodLog(@PathVariable Long id) {
        log.info("删除情绪日记，ID: {}", id);

        Boolean result = moodService.deleteMoodLog(id);
        
        return Result.success(result);
    }

    /**
     * 获取情绪趋势
     */
    @GetMapping("/trend")
    public Result<MoodTrendVO> getMoodTrend(@RequestParam(defaultValue = "7") Integer days,
                                            HttpServletRequest request) {
        log.info("获取情绪趋势，天数: {}", days);

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID：{}", userId);

        MoodTrendVO result = moodService.getMoodTrend(userId, days);
        
        return Result.success(result);
    }

    /**
     * 获取情绪统计
     */
    @GetMapping("/statistics")
    public Result<MoodStatisticsVO> getMoodStatistics(HttpServletRequest request) {
        log.info("获取情绪统计");

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID：{}", userId);

        MoodStatisticsVO result = moodService.getMoodStatistics(userId);
        
        return Result.success(result);
    }
}