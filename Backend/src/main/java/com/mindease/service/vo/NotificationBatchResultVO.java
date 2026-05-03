package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 批量发送结果VO
 */
@Data
@Builder
public class NotificationBatchResultVO {
    private Integer totalTargets;
    private Integer successCount;
    private Integer skippedCount;
    private Integer failedCount;
    private List<Long> failedUserIds;
}
