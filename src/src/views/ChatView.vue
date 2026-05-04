<template>
  <div class="chat-layout">
    <aside class="panel conversation-rail">
      <div class="row-between">
        <div>
          <span class="section-kicker">会话</span>
          <h2>陪伴记录</h2>
        </div>
        <button class="btn secondary" @click="newSession">
          <span v-if="creating" class="spinner"></span>
          新建
        </button>
      </div>
      <div class="list session-list">
        <button
          v-for="item in sessions"
          :key="item.sessionId"
          :class="['list-row session-row', { active: item.sessionId === activeSession }]"
          @click="openSession(item.sessionId)"
        >
          <strong>{{ item.sessionTitle || "新的对话" }}</strong>
          <span>{{ formatDate(item.createTime) }}</span>
        </button>
      </div>
    </aside>

    <section class="panel chat-panel">
      <div class="chat-window" ref="chatWindow">
        <div v-if="!messages.length" class="chat-empty">
          <div class="empty-mark">ME</div>
          <h2>先把此刻最真实的一句话放在这里</h2>
          <p class="muted">可以从身体感受、压力来源、睡眠状态或今天发生的一件小事开始。</p>
          <div class="prompt-grid">
            <button v-for="prompt in prompts" :key="prompt" @click="draft = prompt">{{ prompt }}</button>
          </div>
        </div>

        <div v-for="(item, index) in messages" :key="index" :class="['bubble', item.sender === 'user' ? 'user' : 'ai']">
          <div v-html="renderMarkdown(item.content || (item.streaming ? '正在组织回应...' : ''))"></div>
        </div>
      </div>

      <p v-if="error" class="feedback-note danger-note">{{ error }}</p>
      <form class="composer" @submit.prevent="send">
        <input v-model="draft" class="input" placeholder="输入想说的话，支持连续追问" :disabled="sending" />
        <button class="btn" :disabled="sending || !draft.trim()">
          <span v-if="sending" class="spinner"></span>
          {{ sending ? "回应中" : "发送" }}
        </button>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { chatApi } from "@/api";

const sessions = ref<any[]>([]);
const messages = ref<any[]>([]);
const activeSession = ref("");
const draft = ref("");
const sending = ref(false);
const creating = ref(false);
const error = ref("");
const chatWindow = ref<HTMLElement | null>(null);
const prompts = ["今天一直紧绷，想先放松下来", "我有点焦虑，想梳理一下原因", "睡前脑子停不下来怎么办？", "帮我把压力拆成下一步行动"];

const renderMarkdown = (text: string) => {
  const escaped = String(text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
  return escaped
    .replace(/^### (.*)$/gm, "<h3>$1</h3>")
    .replace(/^## (.*)$/gm, "<h2>$1</h2>")
    .replace(/^# (.*)$/gm, "<h1>$1</h1>")
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/^\- (.*)$/gm, "<li>$1</li>")
    .replace(/\n/g, "<br />");
};

const waitFrame = () => new Promise((resolve) => requestAnimationFrame(resolve));
const formatDate = (value?: string) => String(value || "").replace("T", " ").slice(0, 16);
const scrollBottom = async () => {
  await nextTick();
  if (chatWindow.value) chatWindow.value.scrollTop = chatWindow.value.scrollHeight;
};

const loadSessions = async () => {
  sessions.value = ((await chatApi.sessions(20)).data.sessions || []);
};

const newSession = async () => {
  creating.value = true;
  error.value = "";
  try {
    const response = await chatApi.createSession();
    activeSession.value = response.data.sessionId;
    messages.value = [];
    await loadSessions();
    return activeSession.value;
  } catch (err) {
    error.value = err instanceof Error ? err.message : "新建会话失败";
    return "";
  } finally {
    creating.value = false;
  }
};

const openSession = async (sessionId: string) => {
  error.value = "";
  activeSession.value = sessionId;
  messages.value = ((await chatApi.history(sessionId)).data.messages || []);
  await scrollBottom();
};

const send = async () => {
  if (!draft.value.trim() || sending.value) return;
  const content = draft.value.trim();
  draft.value = "";
  sending.value = true;
  error.value = "";
  try {
    const sessionId = activeSession.value || await newSession();
    if (!sessionId) return;
    messages.value.push({ sender: "user", content });
    const ai = { sender: "ai", content: "", streaming: true };
    messages.value.push(ai);
    await scrollBottom();
    const reader = (await chatApi.stream(sessionId, content)).getReader();
    const decoder = new TextDecoder();
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      for (const char of chunk) {
        ai.content += char;
        await scrollBottom();
        await waitFrame();
      }
    }
    ai.streaming = false;
  } catch (err) {
    error.value = err instanceof Error ? err.message : "消息发送失败";
  } finally {
    sending.value = false;
  }
};

onMounted(() => loadSessions().catch(() => { sessions.value = []; }));
</script>
