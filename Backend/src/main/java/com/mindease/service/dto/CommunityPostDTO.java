package com.mindease.service.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子创建DTO
 */
@Data
public class CommunityPostDTO {
    private String title;
    private String content;
    private List<String> tags;
    private Boolean isAnonymous;  // 是否匿名发布，默认false
    private String coverImage;   // 封面图片URL
    private Integer visibility;  // 可见范围: 0-公开 1-仅关注者 2-私密
    private LocalDateTime scheduledAt;  // 定时发布时间，为空则立即发布
}
