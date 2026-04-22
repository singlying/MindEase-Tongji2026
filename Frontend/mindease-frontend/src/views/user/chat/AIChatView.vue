<!-- 前端A负责：AI 咨询页面 -->
<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ChatDotRound,
  Delete,
  Plus,
  Promotion,
  Service,
  WarningFilled,
} from "@element-plus/icons-vue";

import type { ChatMessage, ChatSession } from "@/api/chat";
import {
  checkSensitiveWords,
  createChatSession,
  deleteChatSession,
  getChatHistory,
  getChatSessionList,
  sendChatMessage,
} from "@/api/chat";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

const sessions = ref<ChatSession[]>([]);
const messages = ref<ChatMessage[]>([]);
const currentSessionId = ref("");
const inputMessage = ref("");
const loadingSessions = ref(false);
const loadingMessages = ref(false);
const isCreating = ref(false);
const isSending = ref(false);
const messageContainer = ref<HTMLElement | null>(null);

const quickPrompts = [
  "我今天有点焦虑，想整理一下原因",
  "最近睡眠不太好，有什么放松方法吗",
  "我感觉压力很大，不知道从哪里开始处理",
];

const currentSession = computed(() =>
  sessions.value.find((item) => item.sessionId === currentSessionId.value),
);

const displayName = computed(() => userStore.profile?.nickname || "我");

function formatTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatSessionDate(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

async function scrollToBottom() {
  await nextTick();
  const container = messageContainer.value;

  if (container) {
    container.scrollTop = container.scrollHeight;
  }
}

async function loadSessions() {
  loadingSessions.value = true;

  try {
    const response = await getChatSessionList();
    sessions.value = response.data.sessions;

    if (!currentSessionId.value && sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0]?.sessionId ?? "";
    }
  } catch {
    ElMessage.error("会话列表加载失败，请稍后再试");
  } finally {
    loadingSessions.value = false;
  }
}

async function loadMessages(sessionId: string) {
  loadingMessages.value = true;

  try {
    const response = await getChatHistory(sessionId);
    messages.value = response.data.messages;
    await scrollToBottom();
  } catch {
    ElMessage.error("聊天记录加载失败，请稍后再试");
  } finally {
    loadingMessages.value = false;
  }
}

async function handleCreateSession() {
  isCreating.value = true;

  try {
    const response = await createChatSession();
    sessions.value = [response.data, ...sessions.value];
    currentSessionId.value = response.data.sessionId;
    await loadMessages(response.data.sessionId);
  } catch {
    ElMessage.error("新建会话失败，请稍后再试");
  } finally {
    isCreating.value = false;
  }
}

async function handleSelectSession(sessionId: string) {
  if (sessionId === currentSessionId.value) {
    return;
  }

  currentSessionId.value = sessionId;
  await loadMessages(sessionId);
}

async function ensureSession() {
  if (currentSessionId.value) {
    return currentSessionId.value;
  }

  const response = await createChatSession();
  sessions.value = [response.data, ...sessions.value];
  currentSessionId.value = response.data.sessionId;
  await loadMessages(response.data.sessionId);

  return response.data.sessionId;
}

async function handleSendMessage() {
  const content = inputMessage.value.trim();

  if (!content || isSending.value) {
    return;
  }

  isSending.value = true;

  try {
    const sessionId = await ensureSession();
    const riskResponse = await checkSensitiveWords({ sessionId, content });

    if (riskResponse.data.containsSensitiveWord) {
      ElMessage.warning("检测到较高风险表达，建议优先联系身边可信任的人或专业支持。");
    }

    inputMessage.value = "";
    const response = await sendChatMessage({ sessionId, content });
    messages.value.push(response.data.userMessage, response.data.aiMessage);

    const index = sessions.value.findIndex((item) => item.sessionId === sessionId);
    if (index >= 0) {
      sessions.value.splice(index, 1, response.data.session);
    }

    sessions.value = sessions.value
      .slice()
      .sort(
        (a, b) =>
          new Date(b.updatedTime).getTime() - new Date(a.updatedTime).getTime(),
      );
    await scrollToBottom();
  } catch {
    ElMessage.error("消息发送失败，请稍后再试");
  } finally {
    isSending.value = false;
  }
}

async function handleDeleteSession(sessionId: string) {
  if (!sessionId) {
    return;
  }

  try {
    await ElMessageBox.confirm("删除后将无法在本地恢复，确定继续吗？", "删除会话", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });

    await deleteChatSession(sessionId);
    sessions.value = sessions.value.filter((item) => item.sessionId !== sessionId);

    if (currentSessionId.value === sessionId) {
      currentSessionId.value = sessions.value[0]?.sessionId ?? "";
      messages.value = [];

      if (currentSessionId.value) {
        await loadMessages(currentSessionId.value);
      }
    }
  } catch {
    // 用户取消删除时保持当前状态即可。
  }
}

function usePrompt(prompt: string) {
  inputMessage.value = prompt;
}

onMounted(async () => {
  await loadSessions();

  if (currentSessionId.value) {
    await loadMessages(currentSessionId.value);
  }
});
</script>

<template>
  <div class="chat-page">
    <section class="page-head">
      <div>
        <div class="eyebrow">AI 咨询</div>
        <h2>把此刻的心情慢慢说出来</h2>
        <p>这里适合进行日常情绪整理与自我关怀练习，紧急情况请及时寻求线下帮助。</p>
      </div>
      <el-button type="primary" :loading="isCreating" @click="handleCreateSession">
        <el-icon><Plus /></el-icon>
        新建会话
      </el-button>
    </section>

    <section class="safety-card glass-card">
      <el-icon><WarningFilled /></el-icon>
      <span>
        MindEase 提供情绪支持与陪伴建议，不能替代专业诊断或治疗；如遇危机，请立即联系可信任的人或当地紧急救助渠道。
      </span>
    </section>

    <section class="chat-shell glass-card">
      <aside class="session-panel">
        <div class="panel-title">
          <el-icon><ChatDotRound /></el-icon>
          会话记录
        </div>

        <div v-if="loadingSessions" class="quiet-text">正在加载...</div>
        <div v-else-if="sessions.length === 0" class="empty-panel">
          暂无会话，点击右上角开始一次新的整理。
        </div>
        <button
          v-for="session in sessions"
          v-else
          :key="session.sessionId"
          class="session-item"
          :class="{ active: session.sessionId === currentSessionId }"
          type="button"
          @click="handleSelectSession(session.sessionId)"
        >
          <span>
            <strong>{{ session.sessionTitle }}</strong>
            <small>{{ formatSessionDate(session.updatedTime) }}</small>
          </span>
          <el-button
            link
            type="danger"
            aria-label="删除会话"
            @click.stop="handleDeleteSession(session.sessionId)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </button>
      </aside>

      <main class="conversation">
        <header class="conversation-head">
          <div>
            <strong>{{ currentSession?.sessionTitle || "新的对话" }}</strong>
            <span>与 MindEase 助手对话</span>
          </div>
          <span class="online-dot">在线</span>
        </header>

        <div ref="messageContainer" class="message-list">
          <div v-if="loadingMessages" class="quiet-text">正在加载聊天记录...</div>
          <template v-else>
            <article
              v-for="message in messages"
              :key="message.id"
              class="message-row"
              :class="{ mine: message.sender === 'user' }"
            >
              <div class="avatar">
                <el-icon v-if="message.sender === 'ai'"><Service /></el-icon>
                <span v-else>{{ displayName.slice(0, 1) }}</span>
              </div>
              <div class="bubble">
                <div class="message-meta">
                  <span>{{ message.sender === "ai" ? "MindEase" : displayName }}</span>
                  <small>{{ formatTime(message.createTime) }}</small>
                </div>
                <p>{{ message.content }}</p>
              </div>
            </article>
          </template>
        </div>

        <div class="prompt-list">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            @click="usePrompt(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <footer class="input-area">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="写下你现在最想整理的一件事..."
            :disabled="isSending"
            @keydown.enter.exact.prevent="handleSendMessage"
          />
          <el-button
            type="primary"
            :loading="isSending"
            :disabled="!inputMessage.trim()"
            @click="handleSendMessage"
          >
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </footer>
      </main>
    </section>
  </div>
</template>

<style scoped>
.chat-page {
  display: grid;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.eyebrow {
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

h2,
p {
  margin: 0;
}

h2 {
  margin-top: 6px;
  font-size: 30px;
}

.page-head p {
  margin-top: 8px;
  color: var(--ease-muted);
  line-height: 1.7;
}

.safety-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  color: #7a5846;
  background: rgba(255, 247, 237, 0.86);
}

.chat-shell {
  min-height: 640px;
  display: grid;
  grid-template-columns: 280px 1fr;
  overflow: hidden;
}

.session-panel {
  padding: 20px;
  border-right: 1px solid var(--ease-border);
  background: rgba(255, 255, 255, 0.58);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-weight: 800;
}

.session-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: transparent;
  color: var(--ease-text);
  cursor: pointer;
  text-align: left;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.session-item:hover,
.session-item.active {
  background: rgba(123, 158, 137, 0.1);
  border-color: rgba(123, 158, 137, 0.2);
}

.session-item span {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.session-item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item small,
.quiet-text,
.empty-panel {
  color: var(--ease-muted);
}

.empty-panel {
  padding: 18px;
  border-radius: 16px;
  background: rgba(123, 158, 137, 0.08);
  line-height: 1.7;
}

.conversation {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr auto auto;
}

.conversation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--ease-border);
}

.conversation-head div {
  display: grid;
  gap: 4px;
}

.conversation-head span:not(.online-dot) {
  color: var(--ease-muted);
  font-size: 13px;
}

.online-dot {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(123, 158, 137, 0.12);
  color: var(--ease-primary-dark);
  font-size: 13px;
  font-weight: 800;
}

.message-list {
  max-height: 430px;
  overflow: auto;
  padding: 22px;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.message-row.mine {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(123, 158, 137, 0.14);
  color: var(--ease-primary-dark);
  font-weight: 800;
}

.message-row.mine .avatar {
  background: rgba(196, 124, 107, 0.13);
  color: #9a5d4d;
}

.bubble {
  max-width: min(620px, 82%);
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: var(--ease-muted);
  font-size: 12px;
}

.message-row.mine .message-meta {
  justify-content: flex-end;
}

.bubble p {
  padding: 14px 16px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--ease-border);
  box-shadow: 0 10px 24px rgba(47, 65, 57, 0.06);
  line-height: 1.7;
  white-space: pre-wrap;
}

.message-row.mine .bubble p {
  background: linear-gradient(135deg, #7b9e89, #5f7a6a);
  color: #fff;
  border-color: transparent;
}

.prompt-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 22px 16px;
}

.prompt-list button {
  border: 1px solid rgba(123, 158, 137, 0.24);
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(123, 158, 137, 0.08);
  color: var(--ease-primary-dark);
  cursor: pointer;
}

.input-area {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  padding: 18px 22px 22px;
  border-top: 1px solid var(--ease-border);
}

@media (max-width: 960px) {
  .chat-shell {
    grid-template-columns: 1fr;
  }

  .session-panel {
    border-right: 0;
    border-bottom: 1px solid var(--ease-border);
  }
}

@media (max-width: 720px) {
  .page-head,
  .input-area {
    grid-template-columns: 1fr;
    display: grid;
  }

  .message-list {
    max-height: 520px;
  }
}
</style>
