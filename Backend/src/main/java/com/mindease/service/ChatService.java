package com.mindease.service;

import com.mindease.pojo.dto.ChatMessageSendDTO;
import com.mindease.pojo.vo.ChatHistoryVO;
import com.mindease.pojo.vo.ChatSessionCreateVO;
import com.mindease.pojo.vo.ChatSessionListVO;
import com.mindease.pojo.vo.ChatDeleteVO;
import com.mindease.pojo.vo.SensitiveWordCheckVO;
import reactor.core.publisher.Flux;

public interface ChatService {
    
    /**
     * 创建新的AI会话
     */
    ChatSessionCreateVO createSession(Long userId);
    
    /**
     * 获取用户的会话列表
     */
    ChatSessionListVO getSessionList(Long userId, Integer limit);
    
    /**
     * 发送消息并获取AI回复（流式）
     */
    Flux<String> sendMessage(ChatMessageSendDTO chatMessageSendDTO, Long userId);
    
    /**
     * 获取会话历史记录
     */
    ChatHistoryVO getHistory(String sessionId, Long userId, Integer limit);
    
    /**
     * 删除会话
     */
    ChatDeleteVO deleteSession(String sessionId, Long userId);
    
    /**
     * 检测文本中的敏感词
     */
    SensitiveWordCheckVO checkSensitiveWords(String content);
}