package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子详情VO（含评论）
 */
@Data
@Builder
public class CommunityPostDetailVO {
    private Long postId;
    private String title;
    private String content;
    private List<String> tags;
    private String authorName;
    private Long viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private List<CommunityCommentItemVO> comments;
}
