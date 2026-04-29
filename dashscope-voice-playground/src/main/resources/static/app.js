const backendStatus = document.getElementById("backendStatus");
const recognitionStatus = document.getElementById("recognitionStatus");
const sessionStatus = document.getElementById("sessionStatus");
const hint = document.getElementById("hint");
const messagesEl = document.getElementById("messages");
const messageInput = document.getElementById("messageInput");
const sendButton = document.getElementById("sendButton");
const asrButton = document.getElementById("asrButton");
const oneShotVoiceButton = document.getElementById("oneShotVoiceButton");
const conversationButton = document.getElementById("conversationButton");
const stopButton = document.getElementById("stopButton");
const speakLastButton = document.getElementById("speakLastButton");
const autoSpeakToggle = document.getElementById("autoSpeakToggle");
const imageInput = document.getElementById("imageInput");
const imagePreview = document.getElementById("imagePreview");
const messageTemplate = document.getElementById("messageTemplate");

let mediaRecorder = null;
let stream = null;
let audioChunks = [];
let currentMode = null;
let continuousConversation = false;
let lastAssistantMessage = "";
let currentAudio = null;
let awaitingConversationReplay = false;
let discardCurrentRecording = false;

const SpeechRecognitionApi = window.SpeechRecognition || window.webkitSpeechRecognition;

bootstrap();

async function bootstrap() {
  addMessage("ai", "欢迎来到独立 DashScope 语音测试页。这里会用真实的 ASR、Chat 和 TTS，但不会依赖主应用。");

  recognitionStatus.textContent = SpeechRecognitionApi ? "浏览器支持" : "未使用";

  sendButton.addEventListener("click", () => sendTextMessage());
  asrButton.addEventListener("click", () => startRecording("asr"));
  oneShotVoiceButton.addEventListener("click", () => startRecording("oneshot"));
  conversationButton.addEventListener("click", toggleConversationMode);
  stopButton.addEventListener("click", stopAllVoiceActions);
  speakLastButton.addEventListener("click", replayLastAssistantMessage);
  imageInput.addEventListener("change", handleImagePreview);
  messageInput.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      sendTextMessage();
    }
  });

  await checkBackendHealth();
}

async function checkBackendHealth() {
  try {
    const response = await fetch("/api/health");
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    backendStatus.textContent = "正常";
  } catch (error) {
    backendStatus.textContent = "不可用";
    hint.textContent = `后端状态检测失败：${error.message}`;
  }
}

async function sendTextMessage(forcedText, fromConversation = false) {
  const text = (forcedText ?? messageInput.value).trim();
  if (!text) {
    hint.textContent = "请输入内容，或者先录音。";
    return;
  }

  addMessage("user", text);
  messageInput.value = "";
  hint.textContent = "正在请求 DashScope 对话接口...";

  try {
    const formData = new URLSearchParams();
    formData.append("message", text);

    const response = await fetch("/api/chat", {
      method: "POST",
      body: formData,
    });

    const payload = await response.json();
    if (!response.ok) {
      throw new Error(payload.message || "对话请求失败");
    }

    const reply = payload.reply || "";
    addMessage("ai", reply);
    lastAssistantMessage = reply;
    hint.textContent = "回复完成。";

    if (autoSpeakToggle.checked) {
      awaitingConversationReplay = fromConversation && continuousConversation;
      await speakViaDashscope(reply);
    } else if (fromConversation && continuousConversation) {
      startRecording("conversation");
    }
  } catch (error) {
    hint.textContent = `对话失败：${error.message}`;
  }
}

function toggleConversationMode() {
  if (continuousConversation) {
    stopAllVoiceActions();
    return;
  }

  continuousConversation = true;
  sessionStatus.textContent = "进行中";
  conversationButton.textContent = "关闭持续语音会话";
  hint.textContent = "持续语音会话已开启，请开始说话。";
  startRecording("conversation");
}

async function startRecording(mode) {
  if (mediaRecorder && mediaRecorder.state === "recording") {
    return;
  }

  try {
    currentMode = mode;
    audioChunks = [];
    discardCurrentRecording = false;
    stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder = new MediaRecorder(stream, { mimeType: pickMimeType() });

    mediaRecorder.ondataavailable = event => {
      if (event.data && event.data.size > 0) {
        audioChunks.push(event.data);
      }
    };

    mediaRecorder.onstop = async () => {
      const blobType = mediaRecorder.mimeType || "audio/webm";
      const blob = new Blob(audioChunks, { type: blobType });
      stopStreamTracks();

      if (discardCurrentRecording) {
        discardCurrentRecording = false;
        hint.textContent = "本轮录音已取消。";
        return;
      }

      if (blob.size === 0) {
        hint.textContent = "没有录到有效音频，请再试一次。";
        return;
      }

      await handleRecordedAudio(blob, currentMode);
    };

    mediaRecorder.start();
    hint.textContent = mode === "asr"
      ? "正在录音，点击停止后会转写到输入框。"
      : "正在录音，点击停止后会发送到 DashScope。";
  } catch (error) {
    hint.textContent = `无法启动录音：${error.message}`;
  }
}

async function handleRecordedAudio(blob, mode) {
  try {
    hint.textContent = "正在调用 DashScope ASR...";
    const transcript = await transcribeAudio(blob);
    messageInput.value = transcript;

    if (mode === "asr") {
      hint.textContent = "转写完成，文本已回填到输入框。";
      return;
    }

    await sendTextMessage(transcript, mode === "conversation");
  } catch (error) {
    hint.textContent = `语音处理失败：${error.message}`;
    if (mode === "conversation") {
      continuousConversation = false;
      sessionStatus.textContent = "异常结束";
      conversationButton.textContent = "开启持续语音会话";
    }
  }
}

async function transcribeAudio(blob) {
  const extension = blob.type.includes("wav") ? "wav" : blob.type.includes("mp3") ? "mp3" : "webm";
  const formData = new FormData();
  formData.append("file", blob, `speech.${extension}`);

  const response = await fetch("/api/asr", {
    method: "POST",
    body: formData,
  });

  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.message || "ASR 调用失败");
  }
  return payload.text || "";
}

async function speakViaDashscope(text) {
  try {
    hint.textContent = "正在调用 DashScope TTS...";
    const formData = new URLSearchParams();
    formData.append("text", text);

    const response = await fetch("/api/tts", {
      method: "POST",
      body: formData,
    });

    if (!response.ok) {
      let message = "TTS 调用失败";
      try {
        const payload = await response.json();
        message = payload.message || message;
      } catch (_error) {
        // ignore
      }
      throw new Error(message);
    }

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);

    if (currentAudio) {
      currentAudio.pause();
      currentAudio = null;
    }

    currentAudio = new Audio(url);
    currentAudio.onended = () => {
      URL.revokeObjectURL(url);
      currentAudio = null;
      hint.textContent = "朗读完成。";

      if (awaitingConversationReplay && continuousConversation) {
        awaitingConversationReplay = false;
        startRecording("conversation");
      }
    };
    currentAudio.onerror = () => {
      URL.revokeObjectURL(url);
      currentAudio = null;
      awaitingConversationReplay = false;
      hint.textContent = "音频播放失败。";
    };

    await currentAudio.play();
  } catch (error) {
    awaitingConversationReplay = false;
    hint.textContent = `朗读失败：${error.message}`;
  }
}

function replayLastAssistantMessage() {
  if (!lastAssistantMessage) {
    hint.textContent = "当前还没有 AI 回复可播放。";
    return;
  }
  speakViaDashscope(lastAssistantMessage);
}

function stopAllVoiceActions() {
  continuousConversation = false;
  awaitingConversationReplay = false;
  sessionStatus.textContent = "未开启";
  conversationButton.textContent = "开启持续语音会话";
  hint.textContent = "已停止当前语音流程。";

  if (mediaRecorder && mediaRecorder.state === "recording") {
    discardCurrentRecording = true;
    mediaRecorder.stop();
  }
  stopStreamTracks();

  if (currentAudio) {
    currentAudio.pause();
    currentAudio = null;
  }
}

function stopStreamTracks() {
  if (stream) {
    stream.getTracks().forEach(track => track.stop());
    stream = null;
  }
}

function pickMimeType() {
  const candidates = ["audio/webm;codecs=opus", "audio/webm", "audio/mp4"];
  for (const candidate of candidates) {
    if (window.MediaRecorder && MediaRecorder.isTypeSupported(candidate)) {
      return candidate;
    }
  }
  return "";
}

function handleImagePreview(event) {
  const [file] = event.target.files;
  if (!file) {
    imagePreview.textContent = "暂无图片";
    imagePreview.classList.add("empty");
    return;
  }

  const reader = new FileReader();
  reader.onload = loadEvent => {
    imagePreview.innerHTML = `<img src="${loadEvent.target.result}" alt="preview" />`;
    imagePreview.classList.remove("empty");
  };
  reader.readAsDataURL(file);
}

function addMessage(role, content) {
  const fragment = messageTemplate.content.cloneNode(true);
  const messageEl = fragment.querySelector(".message");
  const senderEl = fragment.querySelector(".sender");
  const timeEl = fragment.querySelector(".time");
  const bubbleEl = fragment.querySelector(".bubble");

  messageEl.classList.add(role);
  senderEl.textContent = role === "user" ? "你" : "DashScope 助手";
  timeEl.textContent = new Date().toLocaleTimeString("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
  });
  bubbleEl.textContent = content;

  messagesEl.appendChild(fragment);
  messagesEl.scrollTop = messagesEl.scrollHeight;
}
