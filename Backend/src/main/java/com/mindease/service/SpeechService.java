package com.mindease.service;

import com.mindease.pojo.vo.SpeechTranscriptionVO;
import org.springframework.web.multipart.MultipartFile;

public interface SpeechService {

    SpeechTranscriptionVO transcribe(MultipartFile file) throws Exception;

    byte[] synthesize(String text) throws Exception;
}
