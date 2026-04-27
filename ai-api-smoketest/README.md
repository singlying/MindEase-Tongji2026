# AI API Smoke Test

这是一个和主应用完全分离的 AI 测试小程序，用来单独验证 DashScope 相关能力是否可用。

当前支持：

- 文本对话：`qwen-max`
- 语音合成：`sambert-zhichu-v1`
- 语音转写：`paraformer-v2`

## 准备

先设置环境变量：

```powershell
$env:DASHSCOPE_API_KEY='你的 DashScope API Key'
```

如果你还要测语音转写，再准备一个本地音频文件，推荐 `wav`、`mp3`、`webm` 之一。

## 运行

只测文本和 TTS：

```powershell
cd D:\code\Mindease\ai-api-smoketest
mvn -q exec:java
```

附带 ASR 一起测：

```powershell
cd D:\code\Mindease\ai-api-smoketest
mvn -q exec:java "-Dexec.args=--audio C:\tmp\sample.wav"
```

也可以自定义测试文本：

```powershell
mvn -q exec:java "-Dexec.args=--chatPrompt 你好，请简单回复OK --ttsText 这是一段语音合成测试"
```

## 输出

程序会打印每项测试的结果：

- `PASS`：接口调用成功
- `FAIL`：接口调用失败
- `SKIP`：本次未提供所需参数，因此跳过

TTS 成功后，会把音频输出到：

`D:\code\Mindease\ai-api-smoketest\generated-output\tts-output.mp3`

## 参数

- `--apiKey`：直接传入 API Key，不传则读取 `DASHSCOPE_API_KEY`
- `--chatModel`：默认 `qwen-max`
- `--asrModel`：默认 `paraformer-v2`
- `--ttsModel`：默认 `sambert-zhichu-v1`
- `--ttsVoice`：默认 `longxiaochun`
- `--audio`：本地音频文件路径
- `--chatPrompt`：文本对话测试提示词
- `--ttsText`：语音合成测试文本

## 说明

这个工具不会依赖主应用数据库、Redis、OSS、Spring Boot，也不会调用主应用代码路径。它只直接测试 DashScope API 本身是否可用。
