package com.mindease.service;

import com.mindease.pojo.dto.MoodLogDTO;
import com.mindease.pojo.vo.*;

public interface MoodService {
    
    /**
     * 提交情绪日记
     *
     * @param moodLogDTO
     * @return
     */
    MoodLogVO submitMoodLog(MoodLogDTO moodLogDTO, Long userId);

    /**
     * 获取情绪日记列表
     *
     * @param userId
     * @param limit
     * @param offset
     * @return
     */
    MoodLogListVO getMoodLogList(Long userId, Integer limit, Integer offset);

    /**
     * 获取单条日记详情
     *
     * @param id
     * @return
     */
    MoodLogDetailVO getMoodLogDetail(Long id);

    /**
     * 删除日记
     *
     * @param id
     * @return
     */
    Boolean deleteMoodLog(Long id);

    /**
     * 获取情绪趋势
     *
     * @param userId
     * @param days
     * @return
     */
    MoodTrendVO getMoodTrend(Long userId, Integer days);

    /**
     * 获取情绪统计
     *
     * @param userId
     * @return
     */
    MoodStatisticsVO getMoodStatistics(Long userId);
}