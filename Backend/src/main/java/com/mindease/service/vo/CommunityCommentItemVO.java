package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区评论项VO
 */
@Data
@Builder
public class CommunityCommentItemVO {
    private Long commentId;
    private String content;
    private String authorName;
    private Integer likeCount;
    private LocalDateTime createTime;
}
