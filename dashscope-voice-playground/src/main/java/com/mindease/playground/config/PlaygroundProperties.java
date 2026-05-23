package com.mindease.playground.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "playground.dashscope")
public class PlaygroundProperties {

    private String apiKey;
    private String chatModel = "qwen-max";
    private String asrModel = "paraformer-v2";
    private String ttsModel = "sambert-zhichu-v1";
    private String ttsVoice = "longxiaochun";
    private int ttsSampleRate = 22050;
    private String ttsFormat = "mp3";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getAsrModel() {
        return asrModel;
    }

    public void setAsrModel(String asrModel) {
        this.asrModel = asrModel;
    }

    public String getTtsModel() {
        return ttsModel;
    }

    public void setTtsModel(String ttsModel) {
        this.ttsModel = ttsModel;
    }

    public String getTtsVoice() {
        return ttsVoice;
    }

    public void setTtsVoice(String ttsVoice) {
        this.ttsVoice = ttsVoice;
    }

    public int getTtsSampleRate() {
        return ttsSampleRate;
    }

    public void setTtsSampleRate(int ttsSampleRate) {
        this.ttsSampleRate = ttsSampleRate;
    }

    public String getTtsFormat() {
        return ttsFormat;
    }

    public void setTtsFormat(String ttsFormat) {
        this.ttsFormat = ttsFormat;
    }
}
