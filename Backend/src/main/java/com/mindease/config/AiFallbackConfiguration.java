package com.mindease.config;

import com.mindease.aiservice.ConsultantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
@ConditionalOnProperty(name = "mindease.ai.enabled", havingValue = "false", matchIfMissing = true)
public class AiFallbackConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConsultantService.class)
    public ConsultantService consultantService() {
        return new ConsultantService() {
            @Override
            public Flux<String> chat(String memoryId, String message) {
                return Flux.just("AI 功能当前未配置，聊天服务暂不可用。");
            }

            @Override
            public String analyzeMood(String moodInfo) {
                return "AI 情绪分析当前未配置，已为你保存本次记录。";
            }
        };
    }
}
