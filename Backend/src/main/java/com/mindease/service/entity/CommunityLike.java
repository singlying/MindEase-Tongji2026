package com.mindease.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞记录实体（通用，用于帖子/评论点赞）
 */
@Data
public class CommunityLike {
    private Long id;
    private Long userId;
    private Long targetId;         // 被点赞的帖子或评论ID
    private String targetType;     // POST / COMMENT
    private LocalDateTime createTime;
}
