package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 打卡日历数据VO
 */
@Data
@Builder
public class GoalCalendarVO {
    private String yearMonth;          // "2024-01"
    private String monthName;          // "2024年01月"
    private List<String> checkedDays;  // 已打卡日期列表 ["2024-01-05", ...]
    private Integer totalCheckedDays;
    private Integer totalDays;
    private List<Map<String, Object>> calendar;  // 完整日历网格
}
