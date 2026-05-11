package com.mindease.service.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子VO
 */
@Data
@Builder
public class CommunityPostVO {
    private Long postId;
    private String title;
    private String contentPreview;
    private List<String> tags;
    private String authorName;
    private Integer likeCount;
    private Integer commentCount;
    private Long viewCount;
    private LocalDateTime createTime;
}
