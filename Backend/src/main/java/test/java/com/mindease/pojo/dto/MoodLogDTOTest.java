package com.mindease.pojo.dto;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 心情日志DTO单元测试
 */
class MoodLogDTOTest {

    @Test
    void testSetMoodType() {
        MoodLogDTO dto = new MoodLogDTO();
        dto.setMoodType("happy");
        assertEquals("happy", dto.getMoodType());
    }

    @Test
    void testSetMoodScore() {
        MoodLogDTO dto = new MoodLogDTO();
        dto.setMoodScore(85);
        assertEquals(85, dto.getMoodScore());
    }

    @Test
    void testSetContent() {
        MoodLogDTO dto = new MoodLogDTO();
        dto.setContent("今天心情很好");
        assertEquals("今天心情很好", dto.getContent());
    }

    @Test
    void testSetTags() {
        MoodLogDTO dto = new MoodLogDTO();
        List<String> tags = Arrays.asList("开心", "工作顺利");
        dto.setTags(tags);
        assertEquals(2, dto.getTags().size());
        assertTrue(dto.getTags().contains("开心"));
    }

    @Test
    void testSetLogDate() {
        MoodLogDTO dto = new MoodLogDTO();
        LocalDateTime now = LocalDateTime.now();
        dto.setLogDate(now);
        assertEquals(now, dto.getLogDate());
    }

    @Test
    void testAllFields() {
        MoodLogDTO dto = new MoodLogDTO();
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 10, 30);
        List<String> tags = Arrays.asList("平静", "阅读");
        
        dto.setMoodType("calm");
        dto.setMoodScore(70);
        dto.setContent("安静的一天");
        dto.setTags(tags);
        dto.setLogDate(date);
        
        assertEquals("calm", dto.getMoodType());
        assertEquals(70, dto.getMoodScore());
        assertEquals("安静的一天", dto.getContent());
        assertEquals(2, dto.getTags().size());
        assertEquals(date, dto.getLogDate());
    }

    @Test
    void testMoodScoreRange() {
        MoodLogDTO dto = new MoodLogDTO();
        dto.setMoodScore(100);
        assertEquals(100, dto.getMoodScore());
        
        dto.setMoodScore(0);
        assertEquals(0, dto.getMoodScore());
    }
}
