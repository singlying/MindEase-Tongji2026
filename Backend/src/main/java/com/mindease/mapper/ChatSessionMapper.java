package com.mindease.mapper;

import com.mindease.pojo.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    
    /**
     * 插入新的会话
     */
    void insert(ChatSession chatSession);
    
    /**
     * 根据sessionId查询会话
     */
    ChatSession selectBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * 根据userId查询会话列表
     */
    List<ChatSession> selectByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
    
    /**
     * 根据userId统计会话数量
     */
    Integer countByUserId(@Param("userId") Long userId);
    
    /**
     * 更新会话标题
     */
    void updateTitle(@Param("sessionId") String sessionId, @Param("title") String title);
    
    /**
     * 根据sessionId删除会话
     */
    void deleteBySessionId(@Param("sessionId") String sessionId);
}