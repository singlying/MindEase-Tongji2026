package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI关怀消息VO
 */
@Data
@Builder
public class AICareMessageVO {
    private Boolean triggered;     // 是否成功触发
    private String reason;         // 未触发原因: cooldown/disabled/success
    private Integer contextLength; // 上下文数据量（字符数）
    private String messageType;    // 消息类型: ENCOURAGEMENT/TIP/BREATHING/CHECKIN
    private String emotionTag;     // 关联情绪标签: anxious/sad/stressed/hopeful
    private String message;        // 生成的关怀消息内容
    private List<String> suggestions; // 附带建议条目列表
    private Map<String, Object> metadata; // 额外元数据（模型版本、耗时等）
    private LocalDateTime sentAt;  // 发送时间
}
