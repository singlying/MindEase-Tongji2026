package com.mindease.playground.controller;

import com.mindease.playground.service.DashscopeGateway;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlaygroundApiController {

    private final DashscopeGateway gateway;

    public PlaygroundApiController(DashscopeGateway gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "message", "dashscope-voice-playground is running");
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestParam("message") String message) throws Exception {
        return Map.of("reply", gateway.chat(message));
    }

    @PostMapping(value = "/asr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> asr(@RequestPart("file") MultipartFile file) throws Exception {
        return Map.of("text", gateway.transcribe(file));
    }

    @PostMapping("/tts")
    public ResponseEntity<byte[]> tts(@RequestParam("text") String text) throws Exception {
        byte[] audio = gateway.synthesize(text);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=tts-output.mp3")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(Map.of("message", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
    }
}
