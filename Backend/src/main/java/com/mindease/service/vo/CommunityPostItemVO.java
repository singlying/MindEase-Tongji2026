package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区帖子列表项VO
 */
@Data
@Builder
public class CommunityPostItemVO {
    private Long postId;
    private String title;
    private String contentPreview;
    private String authorName;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
}
