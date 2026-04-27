# DashScope Voice Playground

这是一个与主应用完全无关的独立多模态对话程序，直接对接 DashScope，用来测试：

- 语音输入
- 语音转写
- 文本对话
- 语音播报
- 图片本地附件预览

它不会复用主应用的 Spring Bean、数据库、Redis、OSS 或业务接口。

## 功能结构

- 前端：
  - 浏览器录音
  - 文本发送
  - 连续语音会话
  - 图片本地预览
- 后端：
  - `/api/asr` 调 DashScope ASR
  - `/api/chat` 调 DashScope Chat Completions
  - `/api/tts` 调 DashScope TTS

## 运行前准备

设置环境变量：

```powershell
$env:DASHSCOPE_API_KEY='你的 DashScope API Key'
```

## 启动

```powershell
cd D:\code\Mindease\dashscope-voice-playground
mvn spring-boot:run
```

默认地址：

[http://localhost:8091](http://localhost:8091)

## 可选配置

在 `src/main/resources/application.yml` 里可以调整：

- 端口
- Chat 模型
- ASR 模型
- TTS 模型
- TTS 音色

## 使用说明

- `发送`：发送输入框文字
- `语音输入`：录音后走 DashScope ASR，只把文本回填输入框
- `直接语音对话`：录音后自动转写、自动发送、自动朗读
- `开启持续语音会话`：一轮轮说话，AI 回复后自动朗读，再等待下一轮输入
- `朗读最后回复`：重新播放最近一条 AI 回复

## 说明

- 这个项目已经按真实 DashScope 链路接好，但我没有替你做真实接口调用测试。
- 浏览器录音建议用 Edge 或 Chrome。
