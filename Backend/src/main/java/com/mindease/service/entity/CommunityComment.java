package com.mindease.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区评论实体
 */
@Data
public class CommunityComment {
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Long parentId;         // 父评论ID（支持回复）
    private Boolean isAnonymous;
    private String authorNickname;
    private Integer likeCount;
    private String status;         // NORMAL/DELETED
    private LocalDateTime createTime;
}
