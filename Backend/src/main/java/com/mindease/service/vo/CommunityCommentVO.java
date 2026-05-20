package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区评论VO（新增返回）
 */
@Data
@Builder
public class CommunityCommentVO {
    private Long commentId;
    private String content;
    private LocalDateTime createTime;
}
