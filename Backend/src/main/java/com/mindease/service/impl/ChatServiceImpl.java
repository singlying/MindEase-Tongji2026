package com.mindease.service.impl;

import com.mindease.mapper.ChatMessageMapper;
import com.mindease.mapper.ChatSessionMapper;
import com.mindease.pojo.dto.ChatMessageSendDTO;
import com.mindease.pojo.entity.ChatMessage;
import com.mindease.pojo.entity.ChatSession;
import com.mindease.pojo.vo.*;
import com.mindease.service.ChatService;
import com.mindease.aiservice.ConsultantService;
import com.mindease.repository.RedisChatMemoryStore;
import com.mindease.common.utils.SensitiveWordFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionMapper chatSessionMapper;
    
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    
    @Autowired
    private ConsultantService consultantService;
    
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Override
    public ChatSessionCreateVO createSession(Long userId) {
        // 生成唯一的会话ID
        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "");
        
        // 创建会话实体
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setUserId(userId);
        chatSession.setCreateTime(LocalDateTime.now());
        // 初始时不设置标题，等用户发送第一条消息后再设置
        
        // 保存到数据库
        chatSessionMapper.insert(chatSession);
        
        // 返回结果
        ChatSessionCreateVO chatSessionCreateVO = new ChatSessionCreateVO();
        chatSessionCreateVO.setSessionId(sessionId);
        
        return chatSessionCreateVO;
    }

    @Override
    public ChatSessionListVO getSessionList(Long userId, Integer limit) {
        // 查询会话列表
        List<ChatSession> chatSessions = chatSessionMapper.selectByUserId(userId, limit);
        
        // 转换为VO
        List<ChatSessionVO> chatSessionVOs = chatSessions.stream().map(chatSession -> {
            ChatSessionVO chatSessionVO = new ChatSessionVO();
            BeanUtils.copyProperties(chatSession, chatSessionVO);
            return chatSessionVO;
        }).collect(Collectors.toList());
        
        // 统计总数
        Integer total = chatSessionMapper.countByUserId(userId);
        
        // 封装结果
        ChatSessionListVO chatSessionListVO = new ChatSessionListVO();
        chatSessionListVO.setTotal(total);
        chatSessionListVO.setSessions(chatSessionVOs);
        
        return chatSessionListVO;
    }

    @Override
    public Flux<String> sendMessage(ChatMessageSendDTO chatMessageSendDTO, Long userId) {
        String sessionId = chatMessageSendDTO.getSessionId();
        String content = chatMessageSendDTO.getContent();
        
        // 验证会话是否存在且属于当前用户
        ChatSession chatSession = chatSessionMapper.selectBySessionId(sessionId);
        if (chatSession == null || !userId.equals(chatSession.getUserId())) {
            return Flux.error(new RuntimeException("会话不存在或无权限访问"));
        }
        
        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setMessageRole("USER");
        userMessage.setContent(content);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setUpdateTime(LocalDateTime.now());
        chatMessageMapper.insert(userMessage);
        
        // 如果这是第一条消息，则设置会话标题
        if (chatSession.getSessionTitle() == null || chatSession.getSessionTitle().isEmpty()) {
            String title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
            chatSessionMapper.updateTitle(sessionId, title);
        }
        
        // 调用AI服务获取回复，并收集完整内容
        StringBuilder fullResponse = new StringBuilder();
        
        Flux<String> aiResponse = consultantService.chat(sessionId, content)
                .doOnNext(fullResponse::append);
        
        // 在流式响应完成后，保存AI消息到数据库
        return aiResponse.doOnComplete(() -> {
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSessionId(sessionId);
            aiMessage.setUserId(userId);
            aiMessage.setMessageRole("AI");
            aiMessage.setContent(fullResponse.toString());
            aiMessage.setCreateTime(LocalDateTime.now());
            aiMessage.setUpdateTime(LocalDateTime.now());
            chatMessageMapper.insert(aiMessage);
        });
    }

    @Override
    public ChatHistoryVO getHistory(String sessionId, Long userId, Integer limit) {
        // 验证会话是否存在且属于当前用户
        ChatSession chatSession = chatSessionMapper.selectBySessionId(sessionId);
        if (chatSession == null || !userId.equals(chatSession.getUserId())) {
            throw new RuntimeException("会话不存在或无权限访问");
        }
        
        // 查询消息历史
        List<ChatMessage> chatMessages = chatMessageMapper.selectBySessionIdAndUserId(sessionId, userId, limit);
        
        // 转换为VO
        List<ChatMessageVO> chatMessageVOs = chatMessages.stream().map(chatMessage -> {
            ChatMessageVO chatMessageVO = new ChatMessageVO();
            chatMessageVO.setSender(chatMessage.getMessageRole().toLowerCase());
            chatMessageVO.setContent(chatMessage.getContent());
            chatMessageVO.setCreateTime(chatMessage.getCreateTime());
            return chatMessageVO;
        }).collect(Collectors.toList());
        
        // 封装结果
        ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
        chatHistoryVO.setSessionId(sessionId);
        chatHistoryVO.setMessages(chatMessageVOs);
        
        return chatHistoryVO;
    }

    @Override
    public ChatDeleteVO deleteSession(String sessionId, Long userId) {
        // 验证会话是否存在且属于当前用户
        ChatSession chatSession = chatSessionMapper.selectBySessionId(sessionId);
        if (chatSession == null || !userId.equals(chatSession.getUserId())) {
            throw new RuntimeException("会话不存在或无权限访问");
        }
        
        // 删除会话相关的所有消息
        chatMessageMapper.deleteBySessionId(sessionId);
        
        // 删除会话
        chatSessionMapper.deleteBySessionId(sessionId);
        
        // 删除Redis中的对话上下文
        try {
            redisChatMemoryStore.deleteMessages(sessionId);
            log.info("已删除Redis中sessionId为{}的对话上下文", sessionId);
        } catch (Exception e) {
            log.error("删除Redis中sessionId为{}的对话上下文失败", sessionId, e);
        }
        
        // 返回结果
        ChatDeleteVO chatDeleteVO = new ChatDeleteVO();
        chatDeleteVO.setSuccess(true);
        
        return chatDeleteVO;
    }
    
    @Override
    public SensitiveWordCheckVO checkSensitiveWords(String content) {
        SensitiveWordCheckVO result = new SensitiveWordCheckVO();
        result.setOriginalText(content);
        
        if (content == null || content.trim().isEmpty()) {
            result.setContainsSensitiveWord(false);
            result.setSensitiveWords(new ArrayList<>());
            return result;
        }
        
        // 检测是否包含敏感词
        boolean containsSensitive = sensitiveWordFilter.containsSensitiveWord(content);
        result.setContainsSensitiveWord(containsSensitive);
        
        // 获取所有敏感词
        List<String> sensitiveWords = sensitiveWordFilter.getAllSensitiveWords(content);
        result.setSensitiveWords(sensitiveWords);
        
        return result;
    }
}