package com.mindease.service.vo;

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
    private String coverImage;       // 封面图
    private String authorName;
    private Long viewCount;
    private Integer likeCount;
    private Integer collectCount;    // 收藏数
    private Boolean isPinned;        // 是否置顶
    private Boolean isCollected;     // 当前用户是否已收藏
    private Boolean isLiked;         // 当前用户是否已点赞
    private Integer commentCount;
    private LocalDateTime createTime;
    private List<CommunityCommentItemVO> comments;
}
