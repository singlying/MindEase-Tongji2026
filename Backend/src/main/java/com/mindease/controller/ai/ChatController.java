package com.mindease.controller.ai;

import com.mindease.common.result.Result;
import com.mindease.pojo.dto.ChatMessageSendDTO;
import com.mindease.pojo.dto.SpeechSynthesisDTO;
import com.mindease.pojo.vo.ChatHistoryVO;
import com.mindease.pojo.vo.ChatSessionCreateVO;
import com.mindease.pojo.vo.ChatSessionListVO;
import com.mindease.pojo.vo.ChatDeleteVO;
import com.mindease.pojo.vo.SpeechTranscriptionVO;
import com.mindease.pojo.vo.SensitiveWordCheckVO;
import com.mindease.service.ChatService;
import com.mindease.service.SpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SpeechService speechService;

    @PostMapping("/session")
    public Result<ChatSessionCreateVO> createSession(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);
        
        log.info("创建AI会话，用户ID: {}", userId);
        ChatSessionCreateVO chatSessionCreateVO = chatService.createSession(userId);
        return Result.success(chatSessionCreateVO);
    }

    @GetMapping("/sessions")
    public Result<ChatSessionListVO> getSessionList(@RequestParam(defaultValue = "20") Integer limit,
                                                     HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);
        
        log.info("获取用户会话列表，用户ID: {}, 限制数量: {}", userId, limit);
        ChatSessionListVO chatSessionListVO = chatService.getSessionList(userId, limit);
        return Result.success(chatSessionListVO);
    }

    @PostMapping("/message")
    public Flux<String> sendMessage(@RequestBody ChatMessageSendDTO chatMessageSendDTO,
                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);
        
        log.info("发送消息，会话ID: {}, 用户ID: {}, 内容: {}", 
                chatMessageSendDTO.getSessionId(), userId, chatMessageSendDTO.getContent());
        
        return chatService.sendMessage(chatMessageSendDTO, userId);
    }

    @GetMapping("/history/{sessionId}")
    public Result<ChatHistoryVO> getHistory(@PathVariable String sessionId,
                                           @RequestParam(defaultValue = "50") Integer limit,
                                           HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);
        
        log.info("获取会话历史，会话ID: {}, 用户ID: {}, 限制数量: {}", sessionId, userId, limit);
        ChatHistoryVO chatHistoryVO = chatService.getHistory(sessionId, userId, limit);
        return Result.success(chatHistoryVO);
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<ChatDeleteVO> deleteSession(@PathVariable String sessionId,
                                            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);
        
        log.info("删除会话，会话ID: {}, 用户ID: {}", sessionId, userId);
        ChatDeleteVO chatDeleteVO = chatService.deleteSession(sessionId, userId);
        return Result.success(chatDeleteVO);
    }
    
    @PostMapping("/check-sensitive-words")
    public Result<SensitiveWordCheckVO> checkSensitiveWords(@RequestBody ChatMessageSendDTO chatMessageSendDTO,
                                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("当前用户ID: {}", userId);
        
        log.info("检测敏感词，用户ID: {}, 内容: {}", userId, chatMessageSendDTO.getContent());
        SensitiveWordCheckVO result = chatService.checkSensitiveWords(chatMessageSendDTO.getContent());
        return Result.success(result);
    }

    @PostMapping("/asr")
    public Result<SpeechTranscriptionVO> transcribeAudio(@RequestParam("file") MultipartFile file,
                                                         HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        log.info("接收语音转写请求, userId={}, filename={}", userId, file != null ? file.getOriginalFilename() : null);
        SpeechTranscriptionVO result = speechService.transcribe(file);
        return Result.success(result);
    }

    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestBody SpeechSynthesisDTO dto,
                                                   HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getAttribute("userId");
        log.info("接收语音合成请求, userId={}", userId);
        byte[] audio = speechService.synthesize(dto.getText());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentDisposition(ContentDisposition.inline().filename("mindease-tts.mp3").build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(audio);
    }
}
