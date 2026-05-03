package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI关怀消息VO
 */
@Data
@Builder
public class AICareMessageVO {
    private Boolean triggered;     // 是否成功触发
    private String reason;         // 未触发原因: cooldown/disabled/success
    private Integer contextLength; // 上下文数据量（字符数）
    private String message;        // 生成的关怀消息内容
    private LocalDateTime sentAt;  // 发送时间
}
