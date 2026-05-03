package com.mindease.pojo.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 社区帖子列表VO
 */
@Data
@Builder
public class CommunityPostListVO {
    private Long total;
    private List<CommunityPostItemVO> items;
    private Integer page;
    private Integer pageSize;
}
