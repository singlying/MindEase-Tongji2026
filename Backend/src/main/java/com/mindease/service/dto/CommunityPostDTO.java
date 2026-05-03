package com.mindease.pojo.dto;

import lombok.Data;
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
}
