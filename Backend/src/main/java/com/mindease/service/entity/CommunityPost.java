package com.mindease.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区帖子实体
 */
@Data
public class CommunityPost {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String tagsJson;       // JSON数组: ["焦虑", "抑郁"]
    private Boolean isAnonymous;   // 是否匿名
    private String authorNickname; // 冗余存储（非匿名时）
    private Long viewCount;        // 浏览量
    private Integer likeCount;     // 点赞数
    private Integer commentCount;  // 评论数
    private String status;         // PUBLISHED/DELETED/HIDDEN
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
