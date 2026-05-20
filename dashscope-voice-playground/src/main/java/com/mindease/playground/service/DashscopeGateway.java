package com.mindease.playground.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.playground.config.PlaygroundProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Locale;

@Service
public class DashscopeGateway {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHAT_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final PlaygroundProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public DashscopeGateway(PlaygroundProperties properties) {
        this.properties = properties;
    }

    public String chat(String message) throws Exception {
        validateApiKey();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }

        String requestBody = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "你是一个温和、自然的中文语音聊天助手，请简洁回应，适合口语交流。"},
                    {"role": "user", "content": %s}
                  ],
                  "temperature": 0.7,
                  "stream": false
                }
                """.formatted(
                escapeJson(properties.getChatModel()),
                quoteJson(message.trim())
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_ENDPOINT))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DashScope Chat 调用失败: HTTP " + response.statusCode() + " " + response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode contentNode = root.at("/choices/0/message/content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("DashScope Chat 返回为空");
        }

        return contentNode.asText().trim();
    }

    public String transcribe(MultipartFile file) throws Exception {
        validateApiKey();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        File tempFile = createTempAudioFile(file);
        try {
            RecognitionParam param = RecognitionParam.builder()
                    .apiKey(properties.getApiKey())
                    .model(properties.getAsrModel())
                    .format(resolveFormat(file.getOriginalFilename(), file.getContentType()))
                    .sampleRate(16000)
                    .build();

            Recognition recognition = new Recognition();
            String text = recognition.call(param, tempFile);
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("DashScope ASR 返回为空");
            }
            return text.trim();
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    public byte[] synthesize(String text) throws Exception {
        validateApiKey();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("TTS 文本不能为空");
        }

        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(properties.getApiKey())
                .model(properties.getTtsModel())
                .text(text.trim())
                .format(resolveAudioFormat(properties.getTtsFormat()))
                .sampleRate(properties.getTtsSampleRate())
                .parameter("voice", properties.getTtsVoice())
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        ByteBuffer buffer = synthesizer.call(param);
        if (buffer == null || !buffer.hasRemaining()) {
            throw new IllegalStateException("DashScope TTS 返回空音频");
        }

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private void validateApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 DASHSCOPE_API_KEY");
        }
    }

    private File createTempAudioFile(MultipartFile file) throws IOException {
        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        File tempFile = Files.createTempFile("dashscope-playground-", extension).toFile();
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

    private String quoteJson(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
