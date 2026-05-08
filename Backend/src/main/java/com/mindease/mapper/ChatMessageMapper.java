package com.mindease.mapper;

import com.mindease.pojo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    
    /**
     * 插入新的消息
     */
    void insert(ChatMessage chatMessage);
    
    /**
     * 根据sessionId查询消息列表
     */
    List<ChatMessage> selectBySessionId(@Param("sessionId") String sessionId, @Param("limit") Integer limit);
    
    /**
     * 根据sessionId和userId查询消息列表
     */
    List<ChatMessage> selectBySessionIdAndUserId(@Param("sessionId") String sessionId, 
                                               @Param("userId") Long userId,
                                               @Param("limit") Integer limit);
    
    /**
     * 根据sessionId删除所有消息
     */
    void deleteBySessionId(@Param("sessionId") String sessionId);
}