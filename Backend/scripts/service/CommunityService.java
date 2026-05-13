package com.mindease.service;

import com.mindease.pojo.dto.CommunityPostDTO;
import com.mindease.pojo.vo.*;

import java.util.List;

/**
 * 心理社区服务接口
 * 提供匿名心理社区功能，包括发帖、评论、点赞等
 */
public interface CommunityService {

    /**
     * 发布社区帖子
     *
     * @param postDTO   帖子内容DTO
     * @param userId    发布者ID
     * @return 创建的帖子VO
     */
    CommunityPostVO createPost(CommunityPostDTO postDTO, Long userId);

    /**
     * 获取帖子列表（支持分页和筛选）
     *
     * @param topic     话题标签，null表示全部
     * @param sortType  排序方式: hot/latest
     * @param page      页码
     * @param pageSize  每页大小
     * @return 帖子列表VO
     */
    CommunityPostListVO getPostList(String topic, String sortType, Integer page, Integer pageSize);

    /**
     * 获取帖子详情（含评论列表）
     *
     * @param postId    帖子ID
     * @return 帖子详情VO
     */
    CommunityPostDetailVO getPostDetail(Long postId);

    /**
     * 对帖子进行点赞或取消点赞
     *
     * @param postId    帖子ID
     * @param userId    操作用户ID
     * @return 操作后的点赞状态
     */
    CommunityLikeVO toggleLike(Long postId, Long userId);

    /**
     * 发表评论
     *
     * @param postId    帖子ID
     * @param userId    评论者ID
     * @param content   评论内容
     * @return 新增的评论VO
     */
    CommunityCommentVO addComment(Long postId, Long userId, String content);

    /**
     * 删除自己的帖子或评论
     *
     * @param targetId  帖子或评论ID
     * @param userId    操作用户ID
     * @param type      类型: post/comment
     * @return 是否删除成功
     */
    Boolean deleteContent(Long targetId, Long userId, String type);

    /**
     * 获取热门话题标签
     *
     * @param limit     返回数量限制
     * @return 热门话题列表
     */
    List<CommunityTopicVO> getHotTopics(Integer limit);

    /**
     * 搜索社区内容
     *
     * @param keyword   搜索关键词
     * @param page      页码
     * @param pageSize  每页大小
     * @return 搜索结果
     }
     CommunityPostListVO searchPosts(String keyword, Integer page, Integer pageSize);
}
