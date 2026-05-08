package com.mindease.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface ConsultantService {

    @SystemMessage(fromResource = "system.txt")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);

    @SystemMessage("你是一位专业的心理咨询师，请根据用户的情绪记录提供温暖、专业的分析和建议。回复要简洁明了，不超过100字。")
    String analyzeMood(@UserMessage String moodInfo);
}
