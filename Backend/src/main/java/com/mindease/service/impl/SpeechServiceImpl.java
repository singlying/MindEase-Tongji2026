package com.mindease.service.impl;

import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionTaskResult;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindease.common.utils.AliyunOSSOperator;
import com.mindease.pojo.vo.SpeechTranscriptionVO;
import com.mindease.service.SpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class SpeechServiceImpl implements SpeechService {

    private final AliyunOSSOperator aliyunOSSOperator;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

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

    public SpeechServiceImpl(AliyunOSSOperator aliyunOSSOperator, ObjectMapper objectMapper) {
        this.aliyunOSSOperator = aliyunOSSOperator;
        this.objectMapper = objectMapper;
    }

    @Override
    public SpeechTranscriptionVO transcribe(MultipartFile file) throws Exception {
        validateApiKey();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        String filename = buildFilename(file.getOriginalFilename(), file.getContentType());
        String audioUrl = aliyunOSSOperator.upload(file.getBytes(), filename);
        log.info("语音转写上传完成, url={}", audioUrl);

        Transcription transcription = new Transcription();
        TranscriptionParam param = TranscriptionParam.builder()
                .apiKey(apiKey)
                .model(asrModel)
                .fileUrls(List.of(audioUrl))
                .build();

        TranscriptionResult taskResult = transcription.asyncCall(param);
        if (taskResult == null || taskResult.getTaskId() == null || taskResult.getTaskId().isBlank()) {
            throw new IllegalStateException("语音转写任务创建失败");
        }

        TranscriptionQueryParam queryParam =
                TranscriptionQueryParam.FromTranscriptionParam(param, taskResult.getTaskId());
        TranscriptionResult finalResult = transcription.wait(queryParam);

        List<TranscriptionTaskResult> results = finalResult.getResults();
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("语音转写结果为空");
        }

        String transcriptionUrl = results.get(0).getTranscriptionUrl();
        String text = fetchTranscriptText(transcriptionUrl);

        SpeechTranscriptionVO vo = new SpeechTranscriptionVO();
        vo.setText(text);
        vo.setAudioUrl(audioUrl);
        vo.setFormat(resolveFormat(file.getOriginalFilename(), file.getContentType()));
        return vo;
    }

    @Override
    public byte[] synthesize(String text) throws Exception {
        validateApiKey();

        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本不能为空");
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
            throw new IllegalStateException("语音合成返回为空");
        }

        byte[] audioBytes = new byte[buffer.remaining()];
        buffer.get(audioBytes);
        return audioBytes;
    }

    private String fetchTranscriptText(String transcriptionUrl) throws IOException, InterruptedException {
        if (transcriptionUrl == null || transcriptionUrl.isBlank()) {
            throw new IllegalStateException("未获取到转写结果地址");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(transcriptionUrl))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("下载转写结果失败: HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode transcripts = root.path("transcripts");
        if (transcripts.isArray() && !transcripts.isEmpty()) {
            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode transcript : transcripts) {
                String text = transcript.path("text").asText("");
                if (!text.isBlank()) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append('\n');
                    }
                    textBuilder.append(text.trim());
                }
            }
            if (!textBuilder.isEmpty()) {
                return textBuilder.toString();
            }
        }

        String directText = root.path("text").asText("");
        if (!directText.isBlank()) {
            return directText.trim();
        }

        throw new IllegalStateException("转写结果中未找到文本内容");
    }

    private SpeechSynthesisAudioFormat resolveAudioFormat(String format) {
        if (format == null || format.isBlank()) {
            return SpeechSynthesisAudioFormat.MP3;
        }

        try {
            return SpeechSynthesisAudioFormat.valueOf(format.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("未知语音格式 {}, 回退为 MP3", format);
            return SpeechSynthesisAudioFormat.MP3;
        }
    }

    private String buildFilename(String originalFilename, String contentType) {
        String extension = resolveExtension(originalFilename, contentType);
        return UUID.randomUUID().toString().replace("-", "") + extension;
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
            throw new IllegalStateException("DashScope API Key 未配置");
        }
    }
}
