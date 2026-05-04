package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 社区热门话题VO
 */
@Data
@Builder
public class CommunityTopicVO {
    private String tag;
    private Long postCount;
}
