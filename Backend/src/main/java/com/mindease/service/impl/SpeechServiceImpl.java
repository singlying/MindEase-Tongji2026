package com.mindease.service.impl;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.mindease.pojo.vo.SpeechTranscriptionVO;
import com.mindease.service.SpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Locale;

@Service
@Slf4j
public class SpeechServiceImpl implements SpeechService {

    @Value("${langchain4j.community.dashscope.chat-model.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${mindease.speech.asr-model:paraformer-v2}")
    private String asrModel;

    @Value("${mindease.speech.tts-model:sambert-zhichu-v1}")
    private String ttsModel;

    @Value("${mindease.speech.tts-voice:longxiaochun}")
    private String ttsVoice;

    @Value("${mindease.speech.tts-format:mp3}")
    private String ttsFormat;

    @Value("${mindease.speech.tts-sample-rate:22050}")
    private Integer ttsSampleRate;

    @Override
    public SpeechTranscriptionVO transcribe(MultipartFile file) throws Exception {
        validateApiKey();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("闊抽鏂囦欢涓嶈兘涓虹┖");
        }

        RecognitionParam param = RecognitionParam.builder()
                .apiKey(apiKey)
                .model(asrModel)
                .format(resolveFormat(file.getOriginalFilename(), file.getContentType()))
                .sampleRate(16000)
                .build();

        File tempFile = createTempAudioFile(file);
        String text;
        try {
            Recognition recognition = new Recognition();
            text = recognition.call(param, tempFile);
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }

        if (text == null || text.isBlank()) {
            throw new IllegalStateException("璇煶杞啓缁撴灉涓虹┖");
        }

        SpeechTranscriptionVO vo = new SpeechTranscriptionVO();
        vo.setText(text.trim());
        vo.setAudioUrl(null);
        vo.setFormat(resolveFormat(file.getOriginalFilename(), file.getContentType()));
        return vo;
    }

    @Override
    public byte[] synthesize(String text) throws Exception {
        validateApiKey();

        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("鏂囨湰涓嶈兘涓虹┖");
        }

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(apiKey)
                .model(ttsModel)
                .text(text.trim())
                .format(resolveAudioFormat(ttsFormat))
                .sampleRate(ttsSampleRate)
                .parameter("voice", ttsVoice)
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        ByteBuffer buffer = synthesizer.call(param);
        if (buffer == null) {
            throw new IllegalStateException("璇煶鍚堟垚杩斿洖涓虹┖");
        }

        byte[] audioBytes = new byte[buffer.remaining()];
        buffer.get(audioBytes);
        return audioBytes;
    }

    private File createTempAudioFile(MultipartFile file) throws IOException {
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        File tempFile = Files.createTempFile("mindease-asr-", extension).toFile();
        file.transferTo(tempFile);
        return tempFile;
    }

    private SpeechSynthesisAudioFormat resolveAudioFormat(String format) {
        if (format == null || format.isBlank()) {
            return SpeechSynthesisAudioFormat.MP3;
        }

        try {
            return SpeechSynthesisAudioFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("鏈煡璇煶鏍煎紡 {}, 鍥為€€涓?MP3", format);
            return SpeechSynthesisAudioFormat.MP3;
        }
    }

    private String resolveFormat(String originalFilename, String contentType) {
        String extension = resolveExtension(originalFilename, contentType);
        return extension.startsWith(".") ? extension.substring(1) : extension;
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        if (contentType != null) {
            return switch (contentType) {
                case "audio/webm", "video/webm" -> ".webm";
                case "audio/wav", "audio/x-wav" -> ".wav";
                case "audio/mpeg" -> ".mp3";
                case "audio/mp4", "video/mp4" -> ".mp4";
                case "audio/ogg" -> ".ogg";
                default -> ".webm";
            };
        }

        return ".webm";
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DashScope API Key 鏈厤缃?");
        }
    }
}
