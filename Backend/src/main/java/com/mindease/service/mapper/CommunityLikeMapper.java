package com.mindease.mapper;

import com.mindease.pojo.entity.CommunityLike;
import org.apache.ibatis.annotations.*;

/**
 * 点赞记录 Mapper
 */
@Mapper
public interface CommunityLikeMapper {

    void insert(CommunityLike likeRecord);

    @Delete("DELETE FROM community_like WHERE user_id = #{userId} AND target_id = #{targetId} AND target_type = #{targetType}")
    void deleteByUserAndPost(
            @Param("userId") Long userId,
            @Param("targetId") Long targetId,
            @Param("targetType") String targetType
    );

    @Select("SELECT COUNT(*) FROM community_like WHERE user_id = #{userId} AND target_id = #{targetId} AND target_type = 'POST'")
    int existsByUserAndPost(@Param("userId") Long userId, @Param("targetId") Long targetId);
}
