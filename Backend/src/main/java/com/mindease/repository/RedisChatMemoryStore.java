package com.mindease.repository;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RedisChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        if (memoryId == null) {
            return new ArrayList<>();
        }

        String json = redisTemplate.opsForValue().get(memoryId.toString());
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
        return messages == null ? new ArrayList<>() : messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        if (memoryId == null) {
            return;
        }

        String json = ChatMessageSerializer.messagesToJson(list);
        redisTemplate.opsForValue().set(memoryId.toString(), json, Duration.ofDays(1));
    }

    @Override
    public void deleteMessages(Object memoryId) {
        if (memoryId == null) {
            return;
        }

        redisTemplate.delete(memoryId.toString());
    }
}
