package com.mindease.mapper;

import com.mindease.pojo.entity.CommunityPost;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * 社区帖子 Mapper
 */
@Mapper
public interface CommunityPostMapper {

    void insert(CommunityPost post);

    @Update("UPDATE community_post SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE community_post SET like_count = like_count + 1, update_time = NOW() WHERE id = #{id}")
    void incrementLikeCount(Long id);

    @Update("UPDATE community_post SET like_count = GREATEST(like_count - 1, 0), update_time = NOW() WHERE id = #{id}")
    void decrementLikeCount(Long id);

    @Update("UPDATE community_post SET comment_count = comment_count + 1, update_time = NOW() WHERE id = #{id}")
    void incrementCommentCount(Long id);

    @Update("UPDATE community_post SET comment_count = GREATEST(comment_count - 1, 0), update_time = NOW() WHERE id = #{id}")
    void decrementCommentCount(Long id);

    @Update("UPDATE community_post SET view_count = view_count + #{count}, update_time = NOW() WHERE id = #{id}")
    void addViewCount(@Param("id") Long id, @Param("count") long count);

    CommunityPost selectById(@Param("id") Long id);

    List<CommunityPost> selectByCondition(
            @Param("topic") String topic,
            @Param("sortType") String sortType,
            @Param("status") String status,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );

    List<CommunityPost> selectBySearchKeyword(
            @Param("keyword") String keyword,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );

    Long countByCondition(@Param("topic") String topic, @Param("status") String status);
    Long countBySearchKeyword(@Param("keyword") String keyword);

    /**
     * 查询热门话题及帖子数量（按标签聚合）
     */
    List<Map<String, Object>> queryHotTopics(@Param("limit") int limit);

    @Update("UPDATE community_post SET collect_count = collect_count + 1, update_time = NOW() WHERE id = #{id}")
    void incrementCollectCount(Long id);

    @Update("UPDATE community_post SET collect_count = GREATEST(collect_count - 1, 0), update_time = NOW() WHERE id = #{id}")
    void decrementCollectCount(Long id);

    @Update("UPDATE community_post SET is_pinned = #{pinned}, update_time = NOW() WHERE id = #{id}")
    void togglePin(@Param("id") Long id, @Param("pinned") Boolean pinned);

    /**
     * 查询置顶帖子列表
     */
    List<CommunityPost> selectPinnedPosts(@Param("limit") int limit);
}
