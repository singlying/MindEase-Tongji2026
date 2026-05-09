package com.mindease.pojo.vo;

import lombok.Data;

@Data
public class SpeechTranscriptionVO {

    private String text;

    private String audioUrl;

    private String format;
}
