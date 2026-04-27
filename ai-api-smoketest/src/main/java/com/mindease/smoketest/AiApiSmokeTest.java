package com.mindease.smoketest;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.tts.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class AiApiSmokeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path OUTPUT_DIR = Path.of("generated-output");
    private static final Path TTS_OUTPUT = OUTPUT_DIR.resolve("tts-output.mp3");

    public static void main(String[] args) {
        Config config = Config.fromArgs(args);
        Map<String, TestResult> results = new LinkedHashMap<>();

        try {
            config.validate();
            Files.createDirectories(OUTPUT_DIR);
        } catch (Exception ex) {
            System.err.println("配置错误: " + ex.getMessage());
            System.exit(2);
            return;
        }

        results.put("chat", run("文本对话", () -> testChat(config)));
        results.put("tts", run("语音合成", () -> testTts(config)));

        if (config.audioFile() == null) {
            results.put("asr", TestResult.skip("未提供 --audio，本次跳过语音转写测试"));
        } else {
            results.put("asr", run("语音转写", () -> testAsr(config)));
        }

        printSummary(results);
        boolean hasFailure = results.values().stream().anyMatch(result -> result.status() == Status.FAIL);
        System.exit(hasFailure ? 1 : 0);
    }

    private static TestResult run(String label, CheckedSupplier<String> supplier) {
        try {
            String details = supplier.get();
            return TestResult.pass(label, details);
        } catch (Exception ex) {
            return TestResult.fail(label, ex);
        }
    }

    private static String testChat(Config config) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        String requestBody = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "你是一个 API 联调测试助手，请简短回复。"},
                    {"role": "user", "content": %s}
                  ],
                  "temperature": 0.2,
                  "stream": false
                }
                """.formatted(
                escapeJson(config.chatModel()),
                quoteJson(config.chatPrompt())
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        JsonNode contentNode = root.at("/choices/0/message/content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("文本对话返回为空: " + response.body());
        }

        return "模型=" + config.chatModel() + "，回复=" + contentNode.asText().trim();
    }

    private static String testTts(Config config) throws Exception {
        SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                .apiKey(config.apiKey())
                .model(config.ttsModel())
                .text(config.ttsText())
                .format(SpeechSynthesisAudioFormat.MP3)
                .sampleRate(22050)
                .parameter("voice", config.ttsVoice())
                .build();

        SpeechSynthesizer synthesizer = new SpeechSynthesizer();
        ByteBuffer buffer = synthesizer.call(param);
        if (buffer == null || !buffer.hasRemaining()) {
            throw new IllegalStateException("语音合成返回空音频");
        }

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Files.write(TTS_OUTPUT, bytes);
        return "模型=" + config.ttsModel() + "，音色=" + config.ttsVoice() + "，输出=" + TTS_OUTPUT.toAbsolutePath();
    }

    private static String testAsr(Config config) throws Exception {
        File audioFile = config.audioFile().toFile();
        String format = resolveFormat(audioFile.toPath());

        RecognitionParam param = RecognitionParam.builder()
                .apiKey(config.apiKey())
                .model(config.asrModel())
                .format(format)
                .sampleRate(16000)
                .build();

        Recognition recognition = new Recognition();
        String text = recognition.call(param, audioFile);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("语音转写返回为空");
        }

        return "模型=" + config.asrModel() + "，格式=" + format + "，转写=" + text.trim();
    }

    private static void printSummary(Map<String, TestResult> results) {
        System.out.println();
        System.out.println("===== AI API Smoke Test Summary =====");
        for (TestResult result : results.values()) {
            System.out.println(result.status().name() + " | " + result.label());
            System.out.println("  " + result.details());
        }
        System.out.println("=====================================");
    }

    private static String resolveFormat(Path audioFile) {
        String filename = audioFile.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "wav";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String quoteJson(String value) {
        return "\"" + escapeJson(value) + "\"";
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private enum Status {
        PASS,
        FAIL,
        SKIP
    }

    private record TestResult(Status status, String label, String details) {
        static TestResult pass(String label, String details) {
            return new TestResult(Status.PASS, label, details);
        }

        static TestResult fail(String label, Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = ex.getClass().getName();
            }
            return new TestResult(Status.FAIL, label, message);
        }

        static TestResult skip(String details) {
            return new TestResult(Status.SKIP, "语音转写", details);
        }
    }

    private record Config(
            String apiKey,
            String chatModel,
            String asrModel,
            String ttsModel,
            String ttsVoice,
            Path audioFile,
            String chatPrompt,
            String ttsText
    ) {
        static Config fromArgs(String[] args) {
            Map<String, String> values = parseArgs(args);
            String apiKey = valueOrDefault(values.get("apiKey"), System.getenv("DASHSCOPE_API_KEY"));
            String chatModel = valueOrDefault(values.get("chatModel"), "qwen-max");
            String asrModel = valueOrDefault(values.get("asrModel"), "paraformer-v2");
            String ttsModel = valueOrDefault(values.get("ttsModel"), "sambert-zhichu-v1");
            String ttsVoice = valueOrDefault(values.get("ttsVoice"), "longxiaochun");
            Path audioFile = values.containsKey("audio") ? Path.of(values.get("audio")) : null;
            String chatPrompt = valueOrDefault(values.get("chatPrompt"), "你好，请用一句话回复“API 正常”。");
            String ttsText = valueOrDefault(values.get("ttsText"), "这是 MindEase 独立 AI 接口测试。");

            return new Config(apiKey, chatModel, asrModel, ttsModel, ttsVoice, audioFile, chatPrompt, ttsText);
        }

        void validate() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("缺少 DashScope API Key，请设置 DASHSCOPE_API_KEY 或传 --apiKey");
            }
            if (audioFile != null && !Files.exists(audioFile)) {
                throw new IllegalArgumentException("音频文件不存在: " + audioFile.toAbsolutePath());
            }
        }

        private static Map<String, String> parseArgs(String[] args) {
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < args.length; i++) {
                String current = args[i];
                if (!current.startsWith("--")) {
                    continue;
                }
                String key = current.substring(2);
                String value = i + 1 < args.length ? args[i + 1] : "";
                if (value.startsWith("--")) {
                    values.put(key, "");
                    continue;
                }
                values.put(key, value);
                i++;
            }
            return values;
        }

        private static String valueOrDefault(String value, String defaultValue) {
            return value == null || value.isBlank() ? defaultValue : value;
        }
    }
}
