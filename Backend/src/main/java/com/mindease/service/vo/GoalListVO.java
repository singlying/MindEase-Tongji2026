package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 目标列表VO
 */
@Data
@Builder
public class GoalListVO {
    private Integer total;
    private List<GoalSummaryVO> items;
}
