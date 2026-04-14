# Chat, ASR, and TTS API Notes

This document records the MindEase AI companionship endpoints used by the
frontend chat page.

## Authentication

All endpoints below are protected by the backend `JwtTokenInterceptor`.
The frontend sends the JWT in the `token` request header.

## Chat Session

### Create Session

- Method: `POST`
- Path: `/chat/session`
- Response data: `{ "sessionId": "sess_xxx" }`

### List Sessions

- Method: `GET`
- Path: `/chat/sessions?limit=20`
- Response data: `{ "total": 1, "sessions": [...] }`

### Delete Session

- Method: `DELETE`
- Path: `/chat/session/{sessionId}`
- Response data: `{ "success": true }`

## Streaming Chat

### Send Message

- Method: `POST`
- Path: `/chat/message`
- Content-Type: `application/json`
- Request body:

```json
{
  "sessionId": "sess_xxx",
  "content": "今天有点焦虑"
}
```

The response is a streaming text body. The AI service is instructed to emit a
first-line motion directive such as `[[MOTION:concern]]`; the frontend uses this
directive to drive the Live2D companion and stores the cleaned message content.

Supported directives:

- `neutral`
- `concern`
- `encouragement`
- `surprise`
- `shy`

## Crisis Keyword Check

### Check Sensitive Words

- Method: `POST`
- Path: `/chat/check-sensitive-words`
- Content-Type: `application/json`
- Request body:

```json
{
  "sessionId": "sess_xxx",
  "content": "用户输入内容"
}
```

Response data:

```json
{
  "containsSensitiveWord": true,
  "sensitiveWords": ["不想活了"],
  "originalText": "用户输入内容"
}
```

When this endpoint returns `containsSensitiveWord=true`, the frontend blocks the
AI chat send action and displays the crisis-support guidance dialog.

## Speech-To-Text

### ASR

- Method: `POST`
- Path: `/chat/asr`
- Content-Type: `multipart/form-data`
- Form field: `file`
- Supported formats: `webm`, `wav`, `mp3`, `mp4`, `ogg`
- Response data:

```json
{
  "text": "转写文本",
  "audioUrl": null,
  "format": "webm"
}
```

## Text-To-Speech

### TTS

- Method: `POST`
- Path: `/chat/tts`
- Content-Type: `application/json`
- Request body:

```json
{
  "text": "需要朗读的文本"
}
```

Response body is `audio/mpeg`.

## Runtime Configuration

The backend reads DashScope configuration from:

- `DASHSCOPE_API_KEY`
- `mindease.speech.asr-model`
- `mindease.speech.tts-model`
- `mindease.speech.tts-voice`
- `mindease.speech.tts-format`
- `mindease.speech.tts-sample-rate`

