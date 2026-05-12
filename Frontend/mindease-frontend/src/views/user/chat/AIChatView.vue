<template>
  <div class="ai-chat-view">
    <!-- 布局容器：左侧会话列表 + 右侧聊天区 -->
    <div class="chat-layout glass-panel">
      <!-- 左侧侧边栏 -->
      <div class="sessions-sidebar">
        <!-- 侧边栏头部信息 -->
        <div class="sidebar-header">
          <div class="ai-info">
            <div class="status-dot"></div>
            <div>
              <h3 class="header-title">MindEase AI</h3>
              <p class="header-subtitle">深度共情模式 • 在线</p>
            </div>
          </div>
          <el-button link class="home-btn" @click="router.push('/home')">
            <el-icon><HomeFilled /></el-icon>
          </el-button>
        </div>

        <!-- 侧边栏免责声明 (复刻原型) -->
        <div class="sidebar-disclaimer">
          <div class="disclaimer-icon">
            <span class="info-icon">!</span>
          </div>
          <div class="disclaimer-text">
            <p class="disclaimer-title">温馨提示</p>
            <p>
              MindEase AI
              旨在提供情绪支持和倾听，但不能替代专业心理咨询。如遇紧急情况，请拨打24小时心理危机热线：<strong
                >400-161-9995</strong
              >
            </p>
          </div>
        </div>

        <!-- 新建会话按钮 -->
        <el-button
          type="primary"
          class="new-session-btn"
          @click="handleCreateNewSession"
          :loading="isCreatingSession"
        >
          <el-icon><Plus /></el-icon>
          <span>新建会话</span>
        </el-button>

        <!-- 会话列表 -->
        <div class="sessions-list">
          <div
            v-for="session in sessions"
            :key="session.sessionId"
            class="session-item"
            :class="{ active: currentSessionId === session.sessionId }"
            @click="handleSwitchSession(session.sessionId)"
          >
            <div class="session-content">
              <div class="session-title">
                {{ session.sessionTitle || "新会话" }}
              </div>
              <div class="session-time">
                {{ formatSessionTime(session.createTime) }}
              </div>
            </div>
            <el-popconfirm
              title="确定删除此会话吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm.stop="handleDeleteSessionById(session.sessionId)"
            >
              <template #reference>
                <el-button link class="delete-session-btn" @click.stop>
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
          </div>

          <!-- 空状态 -->
          <div v-if="sessions.length === 0" class="empty-sessions">
            <el-icon class="empty-icon"><ChatDotRound /></el-icon>
            <p>暂无会话记录</p>
            <p class="empty-hint">点击上方按钮创建新会话</p>
          </div>
        </div>
      </div>

      <!-- 右侧聊天容器 -->
      <div class="chat-container">
        <!-- 简洁头部 -->
        <div class="chat-header-simple">
          <div class="header-main">
            <span class="current-session-title">{{
              getCurrentSessionTitle
            }}</span>
          </div>
          <div class="header-actions">
            <div class="emotion-chip" :class="`chip-${agentEmotionState}`">
              {{ agentEmotionChip }}
            </div>
            <el-popconfirm
              title="确定要删除当前会话吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDeleteSessionById(currentSessionId)"
            >
              <template #reference>
                <el-button
                  link
                  class="action-btn delete-btn"
                  :disabled="!currentSessionId"
                  title="删除当前会话"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <div class="chat-main-content">
          <div class="conversation-panel">
            <!-- 聊天消息区域 -->
            <div class="chat-messages" ref="messageContainer">
              <div
                v-for="(msg, index) in messages"
                :key="index"
                class="message-wrapper"
                :class="{ 'message-user': msg.sender === 'user' }"
              >
                <!-- AI消息 -->
                <template v-if="msg.sender === 'ai'">
                  <div class="message-item">
                    <div
                      class="message-avatar ai-avatar"
                      :class="`emotion-shell-${msg.emotion || 'steady'}`"
                    >
                      <span class="ai-avatar-core" :class="`core-${msg.emotion || 'steady'}`">
                        <span class="avatar-spark"></span>
                      </span>
                    </div>
                    <div class="message-content-wrapper">
                      <span class="message-sender">MindEase AI</span>
                      <div
                        class="message-bubble bubble-ai"
                        :class="{
                          'has-typing': isAITyping && index === messages.length - 1,
                        }"
                      >
                        <div
                          class="markdown-content"
                          v-if="msg.content"
                          v-html="renderMarkdown(msg.content)"
                        ></div>
                        <!-- 打字动画（仅在最后一条AI消息且正在生成时显示） -->
                        <div
                          v-if="isAITyping && index === messages.length - 1"
                          class="typing-indicator"
                        >
                          <span class="dot"></span>
                          <span class="dot"></span>
                          <span class="dot"></span>
                        </div>
                      </div>
                      <div class="message-meta-row">
                        <span
                          v-if="msg.emotion && msg.content"
                          class="message-emotion-badge"
                          :class="`badge-${msg.emotion}`"
                        >
                          {{ getEmotionBadge(msg.emotion) }}
                        </span>
                        <span class="message-time">{{
                          formatTime(msg.createTime)
                        }}</span>
                        <button
                          v-if="msg.content"
                          type="button"
                          class="speak-btn"
                          :disabled="isTtsGenerating || isPlayingAudio"
                          @click="handleSpeakText(msg.content)"
                        >
                          {{
                            currentSpeakingText === msg.content &&
                            (isTtsGenerating || isPlayingAudio)
                              ? "朗读中..."
                              : "朗读"
                          }}
                        </button>
                      </div>
                    </div>
                  </div>
                </template>

                <!-- 用户消息 -->
                <template v-else>
                  <div class="message-item">
                    <div class="message-content-wrapper">
                      <span class="message-sender">我</span>
                      <div class="message-bubble bubble-user">
                        {{ msg.content }}
                      </div>
                      <span class="message-time">{{
                        formatTime(msg.createTime)
                      }}</span>
                    </div>
                    <div class="message-avatar user-avatar">
                      <el-avatar :src="userAvatar" :size="40" />
                    </div>
                  </div>
                </template>
              </div>
            </div>

            <!-- 输入区域 -->
            <div class="chat-input-area">
              <div class="input-wrapper">
                <el-input
                  v-model="inputMessage"
                  type="textarea"
                  :rows="1"
                  :autosize="{ minRows: 1, maxRows: 4 }"
                  placeholder="在此输入你的想法..."
                  @keydown.enter.exact.prevent="handleSendMessage"
                  :disabled="isSending || isListening || isRecording || isTranscribing"
                />
                <el-button
                  type="primary"
                  class="send-btn"
                  :loading="isSending"
                  :disabled="!inputMessage.trim() || isListening || isRecording || isTranscribing"
                  @click="handleSendMessage"
                >
                  <el-icon><Promotion /></el-icon>
                </el-button>
              </div>
              <div class="input-footer">
                <div class="input-hints">
                  <span class="hint-text" v-if="isRecording">
                    正在录音，再点一次“结束录音”即可上传转写。
                  </span>
                  <span class="hint-text" v-else-if="isTranscribing">
                    录音已上传，后端正在转写，请稍候。
                  </span>
                  <span class="hint-text" v-else-if="isListening">
                    正在听写中，再点一次“停止听写”即可结束。
                  </span>
                  <span class="hint-text" v-else>
                    按 Enter 发送，也可以直接进行语音输入或录音转写。
                  </span>
                  <span v-if="!isSpeechRecognitionSupported" class="hint-text warning-text">
                    当前浏览器不支持即时语音识别，建议使用最新版 Edge 或 Chrome。
                  </span>
                  <span v-if="!isMediaRecorderSupported" class="hint-text warning-text">
                    当前浏览器不支持录音转写。
                  </span>
                </div>
                <div class="voice-actions">
                  <button
                    type="button"
                    class="voice-btn"
                    :class="{ listening: isListening }"
                    :disabled="!isSpeechRecognitionSupported || isBusyWithVoice"
                    @click="toggleVoiceInput"
                  >
                    {{ isListening ? "停止听写" : "语音输入" }}
                  </button>
                  <button
                    type="button"
                    class="record-btn"
                    :class="{ recording: isRecording }"
                    :disabled="!isMediaRecorderSupported || isSending || isListening || isTranscribing"
                    @click="toggleAudioRecording"
                  >
                    {{ isTranscribing ? "转写中..." : isRecording ? "结束录音" : "录音转写" }}
                  </button>
                  <button
                    type="button"
                    class="direct-voice-btn"
                    :class="{ recording: isRecording && isDirectVoiceConversation }"
                    :disabled="
                      !isMediaRecorderSupported ||
                      isListening ||
                      isTranscribing ||
                      (isSending && !isDirectVoiceConversation)
                    "
                    @click="toggleDirectVoiceConversation"
                  >
                    {{
                      isRecording && isDirectVoiceConversation
                        ? "结束语音对话"
                        : "直接语音对话"
                    }}
                  </button>
                  <button
                    type="button"
                    class="conversation-voice-btn"
                    :class="{
                      active: isConversationModeEnabled,
                      recording: isRecording && recordingIntent === 'conversation',
                    }"
                    :disabled="!isMediaRecorderSupported || isListening || isTranscribing"
                    @click="toggleConversationMode"
                  >
                    {{
                      isConversationModeEnabled
                        ? (isRecording && recordingIntent === "conversation"
                            ? "结束本轮并关闭"
                            : "关闭语音会话")
                        : "开启语音会话"
                    }}
                  </button>
                  <button
                    type="button"
                    class="auto-speak-toggle"
                    :class="{ active: autoSpeakEnabled }"
                    @click="toggleAutoSpeak"
                  >
                    {{ autoSpeakEnabled ? "自动朗读已开" : "自动朗读已关" }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <aside class="companion-rail">
            <div class="companion-panel">
              <Live2DCompanion
                :emotion="agentEmotionState"
                :speaking="isPlayingAudio"
                :thinking="isAITyping"
                :title="agentEmotionLabel"
                :description="agentEmotionDescription"
                :motion-cue="agentMotionCue"
                :motion-seed="agentMotionSeed"
              />
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  HomeFilled,
  Delete,
  Promotion,
  Plus,
  ChatDotRound,
} from "@element-plus/icons-vue";
import * as chatApi from "@/api/chat";
import type { ChatMessageVO, ChatSessionVO } from "@/api/chat";
import { useUserStore } from "@/stores/user";
import { marked } from "marked";
import DOMPurify from "dompurify";
import Live2DCompanion from "@/components/chat/Live2DCompanion.vue";

type BrowserSpeechRecognition = {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onstart: null | (() => void);
  onend: null | (() => void);
  onerror: null | ((event: { error?: string }) => void);
  onresult: null | ((event: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void);
  start: () => void;
  stop: () => void;
};

type SpeechRecognitionConstructor = new () => BrowserSpeechRecognition;
type AgentEmotion =
  | "steady"
  | "listening"
  | "soothing"
  | "encouraging"
  | "warm"
  | "celebrating"
  | "alert";

type AgentMotionCue =
  | "neutral"
  | "concern"
  | "encouragement"
  | "surprise"
  | "shy";

type ChatViewMessage = ChatMessageVO & {
  emotion?: AgentEmotion;
  motionCue?: AgentMotionCue;
};

type MotionParseResult = {
  motionCue: AgentMotionCue;
  content: string;
  waitingForDirective: boolean;
};

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  }
}

const router = useRouter();
const userStore = useUserStore();

// Markdown渲染配置
marked.use({
  breaks: true, // 支持GitHub风格的换行
  gfm: true, // 启用GitHub风格的Markdown
});

// Markdown渲染函数
const renderMarkdown = (content: string) => {
  if (!content) return "";
  const rawHtml = marked.parse(content) as string;
  return DOMPurify.sanitize(rawHtml);
};

// 用户头像（对齐用户中心逻辑）
const userAvatar = computed(() => {
  // 如果有真实头像，使用真实头像
  if (userStore.userInfo?.avatar) {
    return userStore.userInfo.avatar;
  }
  // 否则使用 ui-avatars.com 生成首字母头像（颜色与用户中心一致）
  const name =
    userStore.userInfo?.nickname || userStore.userInfo?.username || "User";
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(
    name
  )}&background=E8E1D9&color=5F7A6A&size=128`;
});

// 获取当前会话标题
const getCurrentSessionTitle = computed(() => {
  const session = sessions.value.find(
    (s) => s.sessionId === currentSessionId.value
  );
  return session ? session.sessionTitle || "新会话" : "MindEase AI";
});

// 状态管理
const messages = ref<ChatViewMessage[]>([]);
const sessions = ref<ChatSessionVO[]>([]);
const inputMessage = ref("");
const currentSessionId = ref("");
const isSending = ref(false);
const isAITyping = ref(false);
const isCreatingSession = ref(false);
const isLoadingSessions = ref(false);
const messageContainer = ref<HTMLElement>();
const isListening = ref(false);
const isRecording = ref(false);
const isTranscribing = ref(false);
const isSpeechRecognitionSupported = ref(false);
const isMediaRecorderSupported = ref(false);
const autoSpeakEnabled = ref(false);
const isTtsGenerating = ref(false);
const isPlayingAudio = ref(false);
const currentSpeakingText = ref("");
const isDirectVoiceConversation = ref(false);
const isConversationModeEnabled = ref(false);
const recordingIntent = ref<"manual" | "direct" | "conversation" | null>(null);
const agentMotionSeed = ref(0);

// LocalStorage Key
const STORAGE_KEY = "mindease_current_session_id";
const AUTO_SPEAK_KEY = "mindease_auto_speak_enabled";
const GREETING_MESSAGE =
  "你好！这里是一个没有评判的空间。如果你感到焦虑或困惑，可以随时告诉我，我会一直在这里倾听。";

const EMOTION_COPY: Record<
  AgentEmotion,
  { label: string; description: string; chip: string; badge: string }
> = {
  steady: {
    label: "Calm and steady",
    description: "Holding a stable, non-judgmental space for the conversation.",
    chip: "Steady Presence",
    badge: "steady",
  },
  listening: {
    label: "Deep listening",
    description: "The agent is focusing closely on your message and emotional cues.",
    chip: "Listening",
    badge: "listening",
  },
  soothing: {
    label: "Gentle soothing",
    description: "The response is leaning toward calming, grounding, and emotional reassurance.",
    chip: "Soothing",
    badge: "soothing",
  },
  encouraging: {
    label: "Supportive encouragement",
    description: "The agent is offering affirmation, confidence, and a gentle push forward.",
    chip: "Encouraging",
    badge: "encouraging",
  },
  warm: {
    label: "Warm connection",
    description: "The conversation currently carries a warmer, more human sense of companionship.",
    chip: "Warm",
    badge: "warm",
  },
  celebrating: {
    label: "Positive uplift",
    description: "The reply contains more positive energy, relief, or light celebration.",
    chip: "Positive",
    badge: "smile",
  },
  alert: {
    label: "Careful attention",
    description: "The content requires a more careful, explicit, and safety-aware response style.",
    chip: "Careful",
    badge: "alert",
  },
};

let speechRecognition: BrowserSpeechRecognition | null = null;
let mediaRecorder: MediaRecorder | null = null;
let recordingChunks: Blob[] = [];
let currentAudio: HTMLAudioElement | null = null;

const inferEmotionFromText = (
  content: string,
  sender: "ai" | "user" = "ai"
): AgentEmotion => {
  const text = content.trim();
  if (!text) return sender === "user" ? "listening" : "steady";

  if (
    /紧急|立刻|马上|危机|热线|安全|专业帮助|拨打|医院|报警|120|110/.test(text)
  ) {
    return "alert";
  }

  if (
    /抱抱|没关系|别担心|慢一点|先休息|呼吸|你已经很不容易|辛苦了|我在这里|陪你/.test(
      text
    )
  ) {
    return "soothing";
  }

  if (/很好|做得很棒|继续|你可以|试试看|相信自己|有进步|愿意面对/.test(text)) {
    return "encouraging";
  }

  if (/开心|轻松|太好了|真不错|为你高兴|值得庆祝|真替你开心/.test(text)) {
    return "celebrating";
  }

  if (/谢谢你分享|听起来|我理解|我能感受到|愿意说出来|我在认真听/.test(text)) {
    return "warm";
  }

  return sender === "user" ? "listening" : "steady";
};

const parseMotionTaggedContent = (rawContent: string): MotionParseResult => {
  const trimmedStart = rawContent.replace(/^\s*/, "");
  const directiveMatch = trimmedStart.match(
    /^\[\[MOTION:(neutral|concern|encouragement|surprise|shy)\]\]\s*/i
  );

  if (directiveMatch) {
    return {
      motionCue: (directiveMatch[1]?.toLowerCase() || "neutral") as AgentMotionCue,
      content: trimmedStart.slice(directiveMatch[0].length),
      waitingForDirective: false,
    };
  }

  if (/^\[\[MOTION:[a-z]*$/i.test(trimmedStart) || /^\[\[MOTION:[a-z]+\]\]?$/i.test(trimmedStart)) {
    return {
      motionCue: "neutral",
      content: "",
      waitingForDirective: true,
    };
  }

  return {
    motionCue: "neutral",
    content: rawContent,
    waitingForDirective: false,
  };
};

const createMessage = (
  sender: "ai" | "user",
  content: string,
  createTime = new Date().toISOString()
): ChatViewMessage => ({
  sender,
  content,
  createTime,
  emotion: inferEmotionFromText(content, sender),
  motionCue:
    sender === "ai" && content === GREETING_MESSAGE
      ? "shy"
      : "neutral",
});

const agentEmotionState = computed<AgentEmotion>(() => {
  if (isAITyping.value) return "listening";

  const lastAiMessage = [...messages.value]
    .reverse()
    .find((message) => message.sender === "ai" && message.content?.trim());

  return lastAiMessage?.emotion || "steady";
});

const agentEmotionLabel = computed(
  () => EMOTION_COPY[agentEmotionState.value].label
);
const agentEmotionDescription = computed(
  () => EMOTION_COPY[agentEmotionState.value].description
);
const agentEmotionChip = computed(
  () => EMOTION_COPY[agentEmotionState.value].chip
);

const getEmotionBadge = (emotion: AgentEmotion) => EMOTION_COPY[emotion].badge;

const agentMotionCue = computed<AgentMotionCue>(() => {
  const lastAiMessage = [...messages.value]
    .reverse()
    .find((message) => message.sender === "ai" && message.content?.trim());

  return lastAiMessage?.motionCue || "neutral";
});

const isBusyWithVoice = computed(
  () => isSending.value || isRecording.value || isTranscribing.value
);

const startConversationTurn = () => {
  if (
    !isConversationModeEnabled.value ||
    !isMediaRecorderSupported.value ||
    !mediaRecorder ||
    isRecording.value ||
    isTranscribing.value ||
    isSending.value ||
    isTtsGenerating.value ||
    isPlayingAudio.value
  ) {
    return;
  }

  recordingChunks = [];
  isDirectVoiceConversation.value = false;
  recordingIntent.value = "conversation";
  mediaRecorder.start();
};

const stopAudioPlayback = () => {
  if (currentAudio) {
    currentAudio.pause();
    currentAudio.src = "";
    currentAudio = null;
  }
  if ("speechSynthesis" in window) {
    window.speechSynthesis.cancel();
  }
  currentSpeakingText.value = "";
  isPlayingAudio.value = false;
  isTtsGenerating.value = false;
};

const speakWithBrowser = (text: string) => {
  if (!("speechSynthesis" in window)) {
    throw new Error("浏览器不支持语音朗读");
  }

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "zh-CN";
  utterance.onstart = () => {
    currentSpeakingText.value = text;
    isPlayingAudio.value = true;
  };
  utterance.onend = () => {
    currentSpeakingText.value = "";
    isPlayingAudio.value = false;
  };
  utterance.onerror = () => {
    currentSpeakingText.value = "";
    isPlayingAudio.value = false;
  };
  window.speechSynthesis.speak(utterance);
};

const handleSpeakText = async (text: string) => {
  if (!text.trim()) return;

  stopAudioPlayback();
  currentSpeakingText.value = text;
  isTtsGenerating.value = true;

  try {
    const audioBlob = await chatApi.synthesizeSpeech(text);
    const audioUrl = URL.createObjectURL(audioBlob);
    const audio = new Audio(audioUrl);
    currentAudio = audio;

    audio.onplay = () => {
      isTtsGenerating.value = false;
      isPlayingAudio.value = true;
    };
    audio.onended = () => {
      URL.revokeObjectURL(audioUrl);
      stopAudioPlayback();
    };
    audio.onerror = () => {
      URL.revokeObjectURL(audioUrl);
      stopAudioPlayback();
      ElMessage.error("语音播放失败，请重试");
    };

    const playPromise = audio.play();
    if (playPromise) {
      await playPromise;
    }
  } catch (error) {
    isTtsGenerating.value = false;
    speakWithBrowser(text);
  }
};

const maybeAutoSpeak = async (text: string) => {
  if (!autoSpeakEnabled.value || !text.trim()) return;
  await handleSpeakText(text);
};

const setupSpeechRecognition = () => {
  const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!Recognition) {
    isSpeechRecognitionSupported.value = false;
    return;
  }

  isSpeechRecognitionSupported.value = true;
  speechRecognition = new Recognition();
  speechRecognition.lang = "zh-CN";
  speechRecognition.continuous = false;
  speechRecognition.interimResults = false;
  speechRecognition.onstart = () => {
    isListening.value = true;
  };
  speechRecognition.onend = () => {
    isListening.value = false;
  };
  speechRecognition.onerror = () => {
    isListening.value = false;
    ElMessage.warning("语音识别没有成功，请再试一次");
  };
  speechRecognition.onresult = (event) => {
    const text = Array.from(event.results)
      .flatMap((result) => Array.from(result))
      .map((item) => item.transcript)
      .join("")
      .trim();
    if (text) {
      inputMessage.value = inputMessage.value
        ? `${inputMessage.value} ${text}`
        : text;
    }
  };
};

const setupMediaRecorder = async () => {
  if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
    isMediaRecorderSupported.value = false;
    return;
  }

  isMediaRecorderSupported.value = true;
  const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  mediaRecorder = new MediaRecorder(stream);

  mediaRecorder.onstart = () => {
    isRecording.value = true;
  };
  mediaRecorder.ondataavailable = (event) => {
    if (event.data.size > 0) {
      recordingChunks.push(event.data);
    }
  };
  mediaRecorder.onstop = async () => {
    isRecording.value = false;
    if (!recordingChunks.length) return;

    const finishedIntent = recordingIntent.value;
    recordingIntent.value = null;
    isTranscribing.value = true;
    try {
      const audioBlob = new Blob(recordingChunks, {
        type: mediaRecorder?.mimeType || "audio/webm",
      });
      const result = await chatApi.transcribeAudio(audioBlob);
      const text = result.text?.trim();
      if (text) {
        if (
          finishedIntent === "direct" ||
          finishedIntent === "conversation"
        ) {
          await sendChatContent(text, { forceAutoSpeak: true });
        } else {
          inputMessage.value = inputMessage.value
            ? `${inputMessage.value} ${text}`
            : text;
        }
      }
      ElMessage.success(
        finishedIntent === "manual" ? "录音已转写" : "语音已发送"
      );
    } catch (error) {
      ElMessage.error(
        finishedIntent === "manual"
          ? "录音转写失败，请稍后再试"
          : "语音对话失败，请稍后再试"
      );
      if (finishedIntent === "conversation") {
        isConversationModeEnabled.value = false;
      }
    } finally {
      recordingChunks = [];
      isTranscribing.value = false;
      isDirectVoiceConversation.value = false;
    }
  };
};

// 加载会话列表
const loadSessions = async () => {
  isLoadingSessions.value = true;
  try {
    const res = (await chatApi.getSessionList(20)) as any;
    sessions.value = res.data.sessions || [];
  } catch (error) {
    console.error("加载会话列表失败:", error);
  } finally {
    isLoadingSessions.value = false;
  }
};

// 创建新会话
const handleCreateNewSession = async () => {
  isCreatingSession.value = true;
  try {
    const res = (await chatApi.createSession()) as any;
    const newSessionId = res.data.sessionId;

    // 切换到新会话
    currentSessionId.value = newSessionId;
    localStorage.setItem(STORAGE_KEY, newSessionId);

    // 清空消息
    messages.value = [createMessage("ai", GREETING_MESSAGE)];

    // 刷新会话列表
    await loadSessions();

    ElMessage.success("新会话已创建");
  } catch (error) {
    console.error("创建会话失败:", error);
    ElMessage.error("创建会话失败");
  } finally {
    isCreatingSession.value = false;
  }
};

// 切换会话
const handleSwitchSession = async (sessionId: string) => {
  if (sessionId === currentSessionId.value) return;

  currentSessionId.value = sessionId;
  localStorage.setItem(STORAGE_KEY, sessionId);

  // 加载该会话的历史消息
  try {
    const res = (await chatApi.getHistory(sessionId, 50)) as any;
    messages.value = (res.data.messages || []).map((message: ChatMessageVO) =>
      createMessage(
        message.sender as "ai" | "user",
        message.content,
        message.createTime
      )
    );

    // 如果没有消息，添加欢迎消息
    if (messages.value.length === 0) {
      messages.value.push(createMessage("ai", GREETING_MESSAGE));
    }

    // 滚动到底部
    await nextTick();
    scrollToBottom();
  } catch (error) {
    console.error("加载会话历史失败:", error);
    ElMessage.error("加载会话历史失败");
  }
};

// 删除指定会话
const handleDeleteSessionById = async (sessionId: string) => {
  try {
    await chatApi.deleteSession(sessionId);
    ElMessage.success("会话已删除");

    // 刷新会话列表
    await loadSessions();

    // 如果删除的是当前会话，创建新会话
    if (sessionId === currentSessionId.value) {
      if (sessions.value.length > 0 && sessions.value[0]) {
        // 切换到第一个会话
        await handleSwitchSession(sessions.value[0].sessionId);
      } else {
        // 没有会话了，创建新的
        await handleCreateNewSession();
      }
    }
  } catch (error) {
    console.error("删除会话失败:", error);
    ElMessage.error("删除会话失败");
  }
};

const sendChatContent = async (
  userMessage: string,
  options?: { forceAutoSpeak?: boolean }
) => {
  if (!userMessage.trim() || isSending.value) return;

  messages.value.push(createMessage("user", userMessage));
  scrollToBottom();

  try {
    const checkRes = (await chatApi.checkSensitiveWords({
      sessionId: currentSessionId.value,
      content: userMessage,
    })) as any;

    if (checkRes?.data?.containsSensitiveWord) {
      const words = checkRes.data.sensitiveWords || [];
      const wordsText =
        words.length > 0 ? `（检测到关键表达：${words.join("、")}）` : "";

      await ElMessageBox.alert(
        `<div style="text-align:left;line-height:1.6;">
          <p>我们在你的内容中看到了可能涉及<strong>自伤或轻生</strong>的表达${wordsText}。</p>
          <p>你此刻的感受一定很不容易，<strong>但你的安全比一切都重要</strong>。</p>
          <p>建议你优先联系身边可信任的人（家人、朋友、学校老师），或尽快寻求专业帮助：</p>
          <ul style="padding-left:1.2em;margin:0.4em 0;">
            <li>24小时心理危机干预热线：<strong>400-161-9995</strong></li>
            <li>如情况紧急，请立即拨打<strong>120 / 110</strong> 或就近前往医院急诊科</li>
          </ul>
          <p>在确保安全的前提下，你也可以继续使用 MindEase 的日记、测评和咨询师预约等功能寻求长期支持。</p>
        </div>`,
        "安全提示",
        {
          confirmButtonText: "我知道了",
          type: "warning",
          customClass: "safety-alert-dialog",
          dangerouslyUseHTMLString: true,
        }
      );
      return;
    }
  } catch (err) {
    console.error("敏感词检测失败:", err);
    ElMessage.warning("安全检测暂时不可用，本次对话将继续进行。");
  }

  isSending.value = true;
  isAITyping.value = true;

  const aiMessageIndex = messages.value.length;
  messages.value.push(createMessage("ai", ""));

  try {
    const stream = await chatApi.sendMessage({
      sessionId: currentSessionId.value,
      content: userMessage,
    });

    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let rawAiContent = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      rawAiContent += chunk;
      const parsed = parseMotionTaggedContent(rawAiContent);
      if (messages.value[aiMessageIndex]) {
        messages.value[aiMessageIndex].motionCue = parsed.motionCue;
        if (!parsed.waitingForDirective) {
          messages.value[aiMessageIndex].content = parsed.content;
          messages.value[aiMessageIndex].emotion = inferEmotionFromText(
            messages.value[aiMessageIndex].content,
            "ai"
          );
        }
      }

      await nextTick();
      scrollToBottom();
    }

    isAITyping.value = false;
    const parsedFinal = parseMotionTaggedContent(rawAiContent);
    const aiContent = parsedFinal.content || messages.value[aiMessageIndex]?.content || "";
    if (messages.value[aiMessageIndex]) {
      messages.value[aiMessageIndex].content = aiContent;
      messages.value[aiMessageIndex].motionCue =
        aiContent === GREETING_MESSAGE ? "shy" : parsedFinal.motionCue;
      messages.value[aiMessageIndex].emotion = inferEmotionFromText(aiContent, "ai");
    }
    agentMotionSeed.value += 1;
    if (aiContent && (autoSpeakEnabled.value || options?.forceAutoSpeak)) {
      await handleSpeakText(aiContent);
    }
    if (options?.forceAutoSpeak && isConversationModeEnabled.value) {
      window.setTimeout(() => {
        startConversationTurn();
      }, 500);
    }
  } catch (error) {
    console.error("发送消息失败:", error);
    ElMessage.error("发送消息失败，请重试");
    messages.value.splice(aiMessageIndex, 1);
    isAITyping.value = false;
    if (isConversationModeEnabled.value) {
      isConversationModeEnabled.value = false;
    }
  } finally {
    isSending.value = false;
  }
};

const toggleVoiceInput = () => {
  if (!speechRecognition) return;
  if (isListening.value) {
    speechRecognition.stop();
  } else {
    speechRecognition.start();
  }
};

const toggleAudioRecording = async () => {
  if (!isMediaRecorderSupported.value || !mediaRecorder) return;

  if (isRecording.value) {
    mediaRecorder.stop();
    return;
  }

  recordingChunks = [];
  isDirectVoiceConversation.value = false;
  recordingIntent.value = "manual";
  mediaRecorder.start();
};

const toggleDirectVoiceConversation = async () => {
  if (!isMediaRecorderSupported.value || !mediaRecorder) return;

  if (isRecording.value && isDirectVoiceConversation.value) {
    mediaRecorder.stop();
    return;
  }

  if (isRecording.value) return;

  recordingChunks = [];
  isDirectVoiceConversation.value = true;
  recordingIntent.value = "direct";
  mediaRecorder.start();
};

const toggleConversationMode = () => {
  if (!isMediaRecorderSupported.value || !mediaRecorder) return;

  if (isConversationModeEnabled.value) {
    isConversationModeEnabled.value = false;
    if (isRecording.value && recordingIntent.value === "conversation") {
      mediaRecorder.stop();
    }
    return;
  }

  isConversationModeEnabled.value = true;
  startConversationTurn();
};

const toggleAutoSpeak = () => {
  autoSpeakEnabled.value = !autoSpeakEnabled.value;
  localStorage.setItem(AUTO_SPEAK_KEY, String(autoSpeakEnabled.value));
};

// 格式化会话时间
const formatSessionTime = (time: string) => {
  if (!time) return "";

  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // 今天
  if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  // 昨天
  if (diff < 48 * 60 * 60 * 1000) {
    const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    if (date.getDate() === yesterday.getDate()) {
      return "昨天";
    }
  }

  // 其他
  return date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" });
};

// 初始化：加载会话列表并恢复或创建会话
onMounted(async () => {
  autoSpeakEnabled.value = localStorage.getItem(AUTO_SPEAK_KEY) === "true";

  if (!userStore.userInfo && userStore.isLoggedIn) {
    try {
      await userStore.fetchUserInfo();
    } catch (error) {
      console.error(error);
    }
  }

  setupSpeechRecognition();
  await setupMediaRecorder().catch(() => {
    isMediaRecorderSupported.value = false;
  });

  // 先加载会话列表
  await loadSessions();

  // 尝试恢复上次的会话ID
  const savedSessionId = localStorage.getItem(STORAGE_KEY);

  if (
    savedSessionId &&
    sessions.value.some((s) => s.sessionId === savedSessionId)
  ) {
    // 恢复已有会话
    await handleSwitchSession(savedSessionId);
  } else if (sessions.value.length > 0 && sessions.value[0]) {
    // 有会话但没有保存的ID，使用第一个会话
    await handleSwitchSession(sessions.value[0].sessionId);
  } else {
    // 没有任何会话，创建新的
    await handleCreateNewSession();
  }
});

onBeforeUnmount(() => {
  speechRecognition?.stop();
  mediaRecorder?.stream.getTracks().forEach((track) => track.stop());
  stopAudioPlayback();
  if ("speechSynthesis" in window) {
    window.speechSynthesis.cancel();
  }
});

// 发送消息
const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || isSending.value) return;

  const userMessage = inputMessage.value.trim();
  inputMessage.value = "";
  await sendChatContent(userMessage);
};

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return "刚刚";

  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // 1分钟内
  if (diff < 60 * 1000) return "刚刚";
  // 1小时内
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`;
  // 1天内
  if (diff < 24 * 60 * 60 * 1000)
    return `${Math.floor(diff / (60 * 60 * 1000))}小时前`;

  // 超过1天显示具体时间
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// 滚动到底部
const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
  }
};
</script>

<style scoped>
/* ========== 全局容器与背景 ========== */
.ai-chat-view {
  width: 100%;
  height: 100%;
  padding: var(--spacing-xl);
  overflow: hidden;
  position: relative;
  /* 确保背景色与原型一致 */
  background: #f2f5f3;
}

/* 噪点纹理 */
.ai-chat-view::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  opacity: 0.04;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
}

/* 浮动圆球 */
.ai-chat-view::after {
  content: "";
  position: absolute;
  top: -10%;
  left: -10%;
  width: 500px;
  height: 500px;
  background: rgba(216, 180, 254, 0.8);
  border-radius: 50%;
  filter: blur(90px);
  z-index: 0;
  opacity: 0.7;
  animation: blob 10s infinite ease-in-out;
}

/* ========== 布局容器 ========== */
.chat-layout {
  position: relative;
  z-index: 10;
  width: 100%;
  height: 100%;
  display: flex;
  gap: var(--spacing-md);
  border-radius: 2rem; /* 原型: rounded-[2rem] */
  overflow: hidden;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.05);
}

/* ========== 左侧会话列表 ========== */
.sessions-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.5);
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  overflow: hidden;
}

/* 侧边栏头部 */
.sidebar-header {
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.ai-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-dot {
  width: 10px;
  height: 10px;
  background: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.5);
  animation: pulse-glow 2s infinite;
}

.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ease-dark);
  line-height: 1.2;
}

.header-subtitle {
  font-size: 12px;
  color: var(--gray-500);
}

.home-btn {
  color: var(--gray-500);
  transition: all 0.3s;
}

.home-btn:hover {
  color: var(--ease-accent);
  background: rgba(0, 0, 0, 0.05);
  border-radius: 50%;
}

/* 侧边栏免责声明 (复刻原型) */
.sidebar-disclaimer {
  margin: var(--spacing-md);
  padding: 16px;
  background: rgba(255, 251, 235, 0.8); /* amber-50/80 */
  backdrop-filter: blur(4px);
  border-left: 4px solid #fbbf24; /* amber-400 */
  border-radius: 12px; /* rounded-xl */
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.disclaimer-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.info-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: #d97706; /* amber-600 */
  color: white;
  border-radius: 50%;
  font-size: 14px;
  font-weight: bold;
}

.disclaimer-text {
  font-size: 14px;
  color: #92400e; /* amber-800 */
  line-height: 1.5;
}

.disclaimer-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.new-session-btn {
  margin: 0 var(--spacing-md) var(--spacing-md);
  height: 44px;
  border-radius: 12px;
  font-weight: 600;
  background: var(--ease-accent);
  border: none;
  box-shadow: 0 4px 12px rgba(123, 158, 137, 0.3);
  transition: all 0.3s ease;
}

.new-session-btn:hover {
  background: var(--ease-accent-dark);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(123, 158, 137, 0.4);
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 var(--spacing-md) var(--spacing-md);
}

.sessions-list::-webkit-scrollbar {
  width: 4px;
}

.sessions-list::-webkit-scrollbar-thumb {
  background: rgba(123, 158, 137, 0.3);
  border-radius: 2px;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.session-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(2px);
}

.session-item.active {
  background: #ffffff;
  border-color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transform: scale(1.02);
}

.session-content {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ease-dark);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.session-time {
  font-size: 12px;
  color: var(--gray-400);
}

.delete-session-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: var(--gray-400);
}

.session-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  color: #ef4444;
}

.empty-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-2xl) var(--spacing-lg);
  text-align: center;
  color: var(--gray-400);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
  color: var(--gray-300);
}

.empty-hint {
  font-size: 12px;
  margin-top: var(--spacing-xs);
}

/* ========== 右侧聊天容器 ========== */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-main-content {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 20px;
  padding: 20px 20px 20px 24px;
}

.conversation-panel {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.22);
}

.companion-rail {
  width: 292px;
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
}

.companion-panel {
  width: 100%;
  position: sticky;
  top: 0;
}

/* 简洁头部 */
.chat-header-simple {
  min-height: 74px;
  padding: 18px 24px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.3);
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.current-session-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-500);
}

.agent-presence {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(31, 38, 135, 0.06);
  transition: transform 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
}

.presence-avatar-shell {
  position: relative;
  width: 128px;
  height: 142px;
  flex-shrink: 0;
}

.presence-avatar-shell::after {
  content: "";
  position: absolute;
  inset: -8px;
  border-radius: 22px;
  background: radial-gradient(
    circle,
    rgba(123, 158, 137, 0.22) 0%,
    rgba(123, 158, 137, 0) 72%
  );
  animation: companion-aura 4s ease-in-out infinite;
}

.presence-avatar,
.avatar-agent {
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #fefefe 0%, #edf5f0 100%);
}

.presence-avatar {
  width: 128px;
  height: 142px;
  border-radius: 28px;
  border: 1px solid rgba(123, 158, 137, 0.16);
  box-shadow: 0 20px 36px rgba(123, 158, 137, 0.16);
  background:
    radial-gradient(circle at 50% 18%, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0) 42%),
    linear-gradient(180deg, #fcfffd 0%, #eef5f0 55%, #e4eee8 100%);
}

.presence-copy {
  min-width: 0;
}

.presence-title {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--gray-400);
  margin-bottom: 2px;
}

.presence-emotion {
  font-size: 15px;
  font-weight: 700;
  color: var(--ease-dark);
  margin-bottom: 3px;
}

.presence-description {
  font-size: 12px;
  line-height: 1.45;
  color: var(--gray-500);
}

.emotion-chip {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-accent-dark);
  white-space: nowrap;
}

.chip-listening,
.presence-listening {
  background: rgba(147, 197, 253, 0.18);
}

.chip-soothing,
.presence-soothing {
  background: rgba(196, 181, 253, 0.2);
}

.chip-encouraging,
.presence-encouraging {
  background: rgba(134, 239, 172, 0.2);
}

.chip-warm,
.presence-warm {
  background: rgba(251, 191, 36, 0.18);
}

.chip-celebrating,
.presence-celebrating {
  background: rgba(249, 168, 212, 0.2);
}

.chip-alert,
.presence-alert {
  background: rgba(252, 165, 165, 0.22);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: var(--gray-400);
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  color: var(--ease-accent);
}

.delete-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

/* ========== 消息区域 ========== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 26px 28px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-height: 0;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(123, 158, 137, 0.3);
  border-radius: 3px;
}

.message-wrapper {
  display: flex;
  animation: slide-up 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.message-wrapper.message-user {
  justify-content: flex-end;
}

.message-item {
  display: flex;
  gap: 16px;
  max-width: 80%;
  align-items: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 4px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.ai-avatar {
  background: transparent;
  box-shadow: none;
}

.user-avatar {
  background: var(--ease-warm);
}

.ai-avatar-core {
  position: relative;
  display: inline-flex;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #fbfffc 0%, #edf4ef 100%);
  border: 1px solid rgba(123, 158, 137, 0.16);
  box-shadow: 0 10px 18px rgba(123, 158, 137, 0.14);
}

.avatar-spark {
  width: 16px;
  height: 16px;
  border-radius: 6px;
  transform: rotate(45deg);
  background: linear-gradient(135deg, #7ba88d 0%, #4a7a64 100%);
  box-shadow: 0 0 16px rgba(123, 168, 141, 0.32);
}

.core-listening .avatar-spark {
  background: linear-gradient(135deg, #93c5fd 0%, #3b82f6 100%);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.28);
}

.core-soothing .avatar-spark {
  background: linear-gradient(135deg, #c4b5fd 0%, #8b5cf6 100%);
  box-shadow: 0 0 16px rgba(139, 92, 246, 0.28);
}

.core-encouraging .avatar-spark {
  background: linear-gradient(135deg, #86efac 0%, #22c55e 100%);
  box-shadow: 0 0 16px rgba(34, 197, 94, 0.24);
}

.core-warm .avatar-spark {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  box-shadow: 0 0 16px rgba(245, 158, 11, 0.26);
}

.core-celebrating .avatar-spark {
  background: linear-gradient(135deg, #f9a8d4 0%, #ec4899 100%);
  box-shadow: 0 0 16px rgba(236, 72, 153, 0.28);
}

.core-alert .avatar-spark {
  background: linear-gradient(135deg, #fca5a5 0%, #ef4444 100%);
  box-shadow: 0 0 16px rgba(239, 68, 68, 0.28);
}

.emotion-shell-soothing,
.emotion-shell-listening,
.emotion-shell-encouraging,
.emotion-shell-warm,
.emotion-shell-celebrating,
.emotion-shell-alert {
  position: relative;
}

.emotion-shell-soothing::after,
.emotion-shell-listening::after,
.emotion-shell-encouraging::after,
.emotion-shell-warm::after,
.emotion-shell-celebrating::after,
.emotion-shell-alert::after {
  content: "";
  position: absolute;
  inset: -5px;
  border-radius: 18px;
  z-index: -1;
  opacity: 0.75;
  filter: blur(8px);
}

.emotion-shell-soothing::after {
  background: rgba(196, 181, 253, 0.35);
}

.emotion-shell-listening::after {
  background: rgba(147, 197, 253, 0.3);
}

.emotion-shell-encouraging::after {
  background: rgba(134, 239, 172, 0.34);
}

.emotion-shell-warm::after {
  background: rgba(251, 191, 36, 0.28);
}

.emotion-shell-celebrating::after {
  background: rgba(249, 168, 212, 0.32);
}

.emotion-shell-alert::after {
  background: rgba(252, 165, 165, 0.34);
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-user .message-content-wrapper {
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  color: var(--gray-400);
  padding: 0 4px;
}

.message-bubble {
  padding: 20px;
  border-radius: 20px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
  font-size: 15px;
}

.bubble-ai {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-top-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.9);
  color: var(--gray-700);
}

.bubble-user {
  background: linear-gradient(
    135deg,
    var(--ease-accent) 0%,
    var(--ease-accent-dark) 100%
  );
  border-top-right-radius: 4px;
  color: white;
  box-shadow: 0 4px 12px rgba(123, 158, 137, 0.3);
}

.message-time {
  font-size: 12px;
  color: var(--gray-400);
  padding: 0 4px;
}

.message-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 4px;
}

.avatar-scene {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.scene-orb,
.scene-star {
  position: absolute;
  display: block;
}

.scene-orb {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.65;
}

.orb-left {
  top: 18px;
  left: 12px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.55), rgba(251, 191, 36, 0));
  animation: orb-float 5.4s ease-in-out infinite;
}

.orb-right {
  top: 30px;
  right: 10px;
  background: radial-gradient(circle, rgba(167, 139, 250, 0.55), rgba(167, 139, 250, 0));
  animation: orb-float 6.1s ease-in-out infinite reverse;
}

.scene-star {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 12px rgba(255, 255, 255, 0.7);
}

.star-one {
  top: 26px;
  right: 28px;
  animation: sparkle 3.8s ease-in-out infinite;
}

.star-two {
  top: 40px;
  left: 26px;
  animation: sparkle 4.4s ease-in-out infinite 0.6s;
}

.character-body {
  position: absolute;
  left: 50%;
  bottom: 8px;
  width: 102px;
  height: 64px;
  margin-left: -51px;
}

.body-shadow {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: -1px;
  height: 14px;
  border-radius: 999px;
  background: rgba(96, 120, 109, 0.12);
  filter: blur(8px);
}

.shoulder,
.mini-shoulder {
  position: absolute;
  bottom: 0;
  width: 54px;
  height: 38px;
  background: linear-gradient(180deg, #7da68f 0%, #668a76 100%);
}

.shoulder-left {
  left: 0;
  border-radius: 26px 16px 18px 20px;
  transform: rotate(6deg);
}

.shoulder-right {
  right: 0;
  border-radius: 16px 26px 20px 18px;
  transform: rotate(-6deg);
}

.neck,
.mini-neck {
  position: absolute;
  left: 50%;
  top: 6px;
  width: 18px;
  height: 22px;
  margin-left: -9px;
  background: linear-gradient(180deg, #f3d5c8 0%, #e6bfae 100%);
  border-radius: 10px;
}

.collar {
  position: absolute;
  top: 18px;
  width: 26px;
  height: 20px;
  background: #f8fbf9;
}

.collar-left {
  left: 27px;
  border-radius: 0 0 16px 0;
  transform: skewY(20deg);
}

.collar-right {
  right: 27px;
  border-radius: 0 0 0 16px;
  transform: skewY(-20deg);
}

.character-head,
.mini-character-head {
  position: absolute;
  left: 50%;
  top: 18px;
  width: 78px;
  height: 88px;
  margin-left: -39px;
  border-radius: 38px 38px 34px 34px;
  background: linear-gradient(180deg, #f8ddd3 0%, #efc8b7 100%);
  box-shadow: inset 0 -8px 14px rgba(220, 178, 158, 0.18);
  overflow: hidden;
}

.mini-character-head {
  top: 4px;
  width: 28px;
  height: 28px;
  margin-left: -14px;
  border-radius: 14px;
}

.hair-back,
.hair-base,
.hair-top,
.hair-bang {
  position: absolute;
  display: block;
  background: linear-gradient(180deg, #5b463d 0%, #3c2c26 100%);
}

.hair-back {
  inset: -4px -2px 18px -2px;
  border-radius: 40px 40px 28px 28px;
  opacity: 0.9;
}

.hair-base {
  inset: 0 0 42% 0;
  border-radius: 38px 38px 24px 24px;
}

.hair-top {
  top: -4px;
  left: 8px;
  right: 8px;
  height: 18px;
  border-radius: 18px 18px 10px 10px;
}

.hair-bang {
  top: 10px;
  width: 20px;
  height: 22px;
  border-radius: 0 0 18px 18px;
}

.bang-left {
  left: 6px;
  transform: rotate(14deg);
}

.bang-center {
  left: 50%;
  margin-left: -10px;
  height: 18px;
}

.bang-right {
  right: 6px;
  transform: rotate(-14deg);
}

.ear {
  position: absolute;
  top: 42px;
  width: 12px;
  height: 18px;
  background: #efc8b7;
  border-radius: 999px;
}

.ear-left {
  left: -5px;
}

.ear-right {
  right: -5px;
}

.message-emotion-badge {
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.badge-steady {
  background: rgba(123, 158, 137, 0.1);
  color: var(--ease-accent-dark);
}

.badge-listening {
  background: rgba(147, 197, 253, 0.18);
  color: #1d4ed8;
}

.badge-soothing {
  background: rgba(196, 181, 253, 0.22);
  color: #6d28d9;
}

.badge-encouraging {
  background: rgba(134, 239, 172, 0.24);
  color: #15803d;
}

.badge-warm {
  background: rgba(251, 191, 36, 0.2);
  color: #b45309;
}

.badge-celebrating {
  background: rgba(249, 168, 212, 0.24);
  color: #be185d;
}

.badge-alert {
  background: rgba(252, 165, 165, 0.24);
  color: #b91c1c;
}

/* ========== 输入区域 (复刻原型) ========== */
.chat-input-area {
  padding: 22px 24px 24px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
  border-radius: 0 0 24px 24px;
}

.input-wrapper {
  position: relative;
  /* 增加阴影和圆角 */
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  border-radius: 16px;
}

/* 覆盖Element Plus输入框默认样式 */
:deep(.el-textarea__inner) {
  padding: 16px 60px 16px 20px !important; /* 右侧留出按钮空间 */
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.7) !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
  box-shadow: none !important;
  font-size: 15px;
  transition: all 0.3s;
  min-height: 56px !important;
}

:deep(.el-textarea__inner:focus) {
  background: #ffffff !important;
  border-color: var(--ease-accent) !important;
  box-shadow: 0 0 0 2px rgba(123, 158, 137, 0.2) !important;
}

.send-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 40px;
  height: 40px !important;
  padding: 0 !important;
  border-radius: 12px !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  background: var(--ease-accent) !important;
  border: none !important;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.send-btn:hover {
  background: var(--ease-accent-dark) !important;
  transform: scale(1.05);
}

.send-btn:disabled,
.voice-btn:disabled,
.record-btn:disabled,
.auto-speak-toggle:disabled,
.speak-btn:disabled {
  background: var(--gray-300) !important;
  transform: none;
  box-shadow: none;
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== 安全提示弹窗样式（敏感词检测） ========== */
:deep(.el-message-box.safety-alert-dialog) {
  max-width: 640px;
  padding: 20px 28px 22px;
  border-radius: 20px;
  border: 1px solid rgba(248, 113, 113, 0.4); /* 柔和红色描边 */
  background-color: #fff5f2; /* 低饱和暖橘偏红，避免刺眼 */
}

:deep(.el-message-box.safety-alert-dialog .el-message-box__header) {
  padding-bottom: 8px;
}

:deep(.el-message-box.safety-alert-dialog .el-message-box__title) {
  font-size: 18px;
  font-weight: 700;
  color: #b91c1c; /* 深红棕色文字，突出“安全提示” */
}

:deep(.el-message-box.safety-alert-dialog .el-message-box__content) {
  font-size: 15px;
  color: #4b5563;
}

:deep(.el-message-box.safety-alert-dialog strong) {
  color: #b91c1c;
}

:deep(.el-message-box.safety-alert-dialog .el-message-box__btns) {
  margin-top: 14px;
}

:deep(.el-message-box.safety-alert-dialog .el-button--primary) {
  background-color: #f97373;
  border-color: #f97373;
  border-radius: 999px;
}

:deep(.el-message-box.safety-alert-dialog .el-button--primary:hover) {
  background-color: #f05252;
  border-color: #f05252;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-top: 12px;
  padding: 0 8px;
}

.input-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  flex: 1;
}

.hint-text {
  font-size: 12px;
  color: var(--gray-400);
}

.warning-text {
  color: #b45309;
}

.speak-btn,
.voice-btn,
.record-btn,
.direct-voice-btn,
.conversation-voice-btn,
.auto-speak-toggle {
  border: none;
  transition: all 0.2s ease;
  cursor: pointer;
}

.speak-btn {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-accent-dark);
  font-size: 12px;
  font-weight: 600;
}

.voice-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.voice-btn,
.record-btn,
.direct-voice-btn,
.conversation-voice-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.voice-btn {
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-accent-dark);
}

.record-btn {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.direct-voice-btn {
  background: rgba(236, 72, 153, 0.12);
  color: #be185d;
}

.conversation-voice-btn {
  background: rgba(168, 85, 247, 0.12);
  color: #7e22ce;
}

.voice-btn.listening,
.record-btn.recording,
.direct-voice-btn.recording,
.conversation-voice-btn.recording {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

.conversation-voice-btn.active {
  background: rgba(168, 85, 247, 0.2);
  color: #6b21a8;
}

.auto-speak-toggle {
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: var(--gray-500);
  font-size: 12px;
  font-weight: 600;
}

.auto-speak-toggle.active {
  background: rgba(123, 158, 137, 0.16);
  color: var(--ease-accent-dark);
}

/* Markdown样式 */
.markdown-content :deep(p) {
  margin-bottom: 8px;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin-bottom: 8px;
  padding-left: 20px;
}

.markdown-content :deep(li) {
  margin-bottom: 4px;
}

.markdown-content :deep(strong) {
  font-weight: 600;
  color: var(--ease-accent-dark);
}

.markdown-content :deep(a) {
  color: var(--ease-accent);
  text-decoration: underline;
}

.markdown-content :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 4px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 0.9em;
}

.markdown-content :deep(pre) {
  background: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 8px;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
}

/* 打字指示器 */
.typing-indicator {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  margin-left: 4px;
  vertical-align: middle;
}

/* 当消息气泡包含打字动画时，确保内部布局正确 */
.bubble-ai.has-typing {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.typing-indicator .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ease-accent);
  animation: typing 1.4s infinite;
}

.typing-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

.face {
  position: absolute;
  display: block;
}

.face-eye {
  top: 38%;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #37514b;
  transition: transform 0.25s ease, height 0.25s ease, border-radius 0.25s ease;
}

.left-eye {
  left: 28%;
}

.right-eye {
  right: 28%;
}

.face-mouth {
  left: 50%;
  bottom: 24%;
  width: 18px;
  height: 8px;
  margin-left: -9px;
  border-bottom: 3px solid #37514b;
  border-radius: 0 0 16px 16px;
  transition: all 0.25s ease;
}

.face-blush {
  top: 54%;
  width: 10px;
  height: 6px;
  border-radius: 999px;
  background: rgba(244, 114, 182, 0.22);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.left-blush {
  left: 18%;
}

.right-blush {
  right: 18%;
}

.face-highlight {
  top: 18px;
  right: 14px;
  width: 16px;
  height: 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.34);
  filter: blur(1px);
}

.emotion-listening .face-eye {
  height: 6px;
  border-radius: 999px;
}

.emotion-listening .face-mouth {
  width: 14px;
  border-bottom-width: 2px;
}

.emotion-soothing .face-mouth {
  width: 18px;
  height: 10px;
  border-bottom-width: 3px;
  border-radius: 0 0 18px 18px;
}

.emotion-soothing .face-blush,
.emotion-warm .face-blush,
.emotion-celebrating .face-blush {
  opacity: 1;
}

.emotion-encouraging .face-mouth,
.emotion-celebrating .face-mouth {
  width: 18px;
  height: 11px;
  border-bottom-width: 3px;
}

.emotion-celebrating .face-eye {
  height: 4px;
  border-radius: 999px;
}

.emotion-alert .face-eye {
  top: 36%;
  height: 9px;
}

.emotion-alert .face-mouth {
  width: 12px;
  height: 4px;
  border-bottom-width: 2px;
  border-radius: 0;
}

.emotion-steady .face-mouth,
.emotion-warm .face-mouth {
  width: 16px;
}

.presence-avatar .face-eye {
  width: 8px;
  height: 8px;
}

.presence-avatar .left-eye {
  left: 25px;
}

.presence-avatar .right-eye {
  right: 25px;
}

.presence-avatar .face-mouth {
  bottom: 20px;
}

.avatar-agent .mini-character-body {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 30px;
  height: 14px;
  margin-left: -15px;
}

.avatar-agent .mini-shoulder {
  width: 16px;
  height: 10px;
  bottom: 0;
  background: linear-gradient(180deg, #7da68f 0%, #668a76 100%);
}

.avatar-agent .mini-neck {
  top: 0;
  width: 6px;
  height: 8px;
  margin-left: -3px;
}

.avatar-agent .face-eye {
  top: 38%;
  width: 4px;
  height: 4px;
}

.avatar-agent .left-eye {
  left: 9px;
}

.avatar-agent .right-eye {
  right: 9px;
}

.avatar-agent .face-mouth {
  bottom: 6px;
  width: 8px;
  height: 4px;
  margin-left: -4px;
  border-bottom-width: 2px;
}

.avatar-agent .face-blush {
  top: 16px;
  width: 4px;
  height: 3px;
}

.avatar-agent .left-blush {
  left: 4px;
}

.avatar-agent .right-blush {
  right: 4px;
}

.speaking {
  animation: avatar-speak 1s ease-in-out infinite;
}

.thinking .face-eye {
  animation: blink-think 1.4s ease-in-out infinite;
}

@keyframes typing {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-8px);
  }
}

@keyframes pulse-glow {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0);
  }
}

@keyframes blob {
  0% {
    transform: translate(0px, 0px) scale(1);
  }
  33% {
    transform: translate(30px, -50px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
  100% {
    transform: translate(0px, 0px) scale(1);
  }
}

@keyframes slide-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .ai-chat-view {
    padding: 16px;
  }

  .chat-layout {
    flex-direction: column;
  }

  .sessions-sidebar {
    width: 100%;
    height: 300px;
    border-right: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  }

  .message-item {
    max-width: 90%;
  }

  .chat-header-simple {
    padding: 16px 18px;
    flex-direction: column;
    align-items: flex-start;
  }

  .header-main {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .agent-presence {
    width: 100%;
  }

  .chat-main-content {
    flex-direction: column;
    padding: 16px;
  }

  .companion-rail {
    width: 100%;
    order: -1;
  }

  .companion-panel {
    position: static;
  }

  .input-footer {
    flex-direction: column;
  }

  .voice-actions {
    width: 100%;
    justify-content: stretch;
  }

  .voice-actions > * {
    flex: 1 1 100%;
  }
}

@keyframes companion-aura {
  0%,
  100% {
    transform: scale(0.96);
    opacity: 0.55;
  }
  50% {
    transform: scale(1.04);
    opacity: 0.95;
  }
}

@keyframes orb-float {
  0%,
  100% {
    transform: translateY(0px) scale(1);
  }
  50% {
    transform: translateY(-8px) scale(1.08);
  }
}

@keyframes sparkle {
  0%,
  100% {
    opacity: 0.25;
    transform: scale(0.8);
  }
  45% {
    opacity: 1;
    transform: scale(1.15);
  }
}

@keyframes avatar-speak {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-2px) scale(1.02);
  }
}

@keyframes blink-think {
  0%,
  100% {
    transform: scaleY(1);
  }
  45% {
    transform: scaleY(0.5);
  }
  50% {
    transform: scaleY(0.2);
  }
  55% {
    transform: scaleY(0.5);
  }
}
</style>
