package com.mindease.mapper;

import com.mindease.pojo.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDate;
import java.util.List;

/**
 * 社区评论 Mapper
 */
@Mapper
public interface CommunityCommentMapper {

    void insert(CommunityComment comment);

    @Update("UPDATE community_comment SET status = 'DELETED', content = '[已删除]' WHERE id = #{id} AND user_id = #{userId}")
    int updateStatus(@Param("id") Long id, @Param("userId") Long userId); // 实际应校验权限

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    CommunityComment selectById(Long id);

    List<CommunityComment> selectByPostId(@Param("postId") Long postId);

    List<CommunityComment> selectByPostIdBetweenDates(
            @Param("goalId") Long goalId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
