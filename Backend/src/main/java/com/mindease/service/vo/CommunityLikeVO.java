package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区点赞结果VO
 */
@Data
@Builder
public class CommunityLikeVO {
    private Long postId;
    private Boolean isLiked;
    private Integer likeCount;
}
