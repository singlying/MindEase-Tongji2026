package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 最近打卡记录VO
 */
@Data
@Builder
public class GoalRecentCheckInVO {
    private String date;   // "yyyy-MM-dd"
    private String note;
}
