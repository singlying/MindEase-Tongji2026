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
          <span class="current-session-title">{{
            getCurrentSessionTitle
          }}</span>
          <div class="header-actions">
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
                <div class="message-avatar ai-avatar">
                  <el-icon><Cpu /></el-icon>
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
                  <span class="message-time">{{
                    formatTime(msg.createTime)
                  }}</span>
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
              :disabled="isSending"
            />
            <el-button
              type="primary"
              class="send-btn"
              :loading="isSending"
              :disabled="!inputMessage.trim()"
              @click="handleSendMessage"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
          <div class="input-footer">
            <div class="input-hints">
              <span class="hint-text">按 Enter 发送</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  HomeFilled,
  Delete,
  Cpu,
  User,
  Promotion,
  Plus,
  ChatDotRound,
} from "@element-plus/icons-vue";
import * as chatApi from "@/api/chat";
import type { ChatMessageVO, ChatSessionVO } from "@/api/chat";
import { useUserStore } from "@/stores/user";
import { marked } from "marked";
import DOMPurify from "dompurify";

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
const messages = ref<ChatMessageVO[]>([]);
const sessions = ref<ChatSessionVO[]>([]);
const inputMessage = ref("");
const currentSessionId = ref("");
const isSending = ref(false);
const isAITyping = ref(false);
const isCreatingSession = ref(false);
const isLoadingSessions = ref(false);
const messageContainer = ref<HTMLElement>();

// LocalStorage Key
const STORAGE_KEY = "mindease_current_session_id";

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
    messages.value = [
      {
        sender: "ai",
        content:
          "你好！这里是一个没有评判的空间。如果你感到焦虑或困惑，可以随时告诉我，我会一直在这里倾听。",
        createTime: new Date().toISOString(),
      },
    ];

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
    messages.value = res.data.messages || [];

    // 如果没有消息，添加欢迎消息
    if (messages.value.length === 0) {
      messages.value.push({
        sender: "ai",
        content:
          "你好！这里是一个没有评判的空间。如果你感到焦虑或困惑，可以随时告诉我，我会一直在这里倾听。",
        createTime: new Date().toISOString(),
      });
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

// 发送消息
const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || isSending.value) return;

  const userMessage = inputMessage.value.trim();
  inputMessage.value = "";

  // 先把用户消息展示出来
  messages.value.push({
    sender: "user",
    content: userMessage,
    createTime: new Date().toISOString(),
  });
  scrollToBottom();

  // 先进行敏感词检测，避免在存在严重危机表达时继续对话
  try {
    const checkRes = (await chatApi.checkSensitiveWords({
      sessionId: currentSessionId.value,
      content: userMessage,
    })) as any;

    if (checkRes?.data?.containsSensitiveWord) {
      // 检测到敏感词：不再调用 AI，对用户进行醒目的安全提示
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

      // 终止本次 AI 回复，不修改现有消息列表（只保留用户刚才的那条话）
      return;
    }
  } catch (err) {
    console.error("敏感词检测失败:", err);
    // 检测失败时，为避免误杀，仍继续按原逻辑调用 AI，但提示一次
    ElMessage.warning("安全检测暂时不可用，本次对话将继续进行。");
  }

  // 准备接收AI回复
  isSending.value = true;
  isAITyping.value = true;

  // 创建AI消息占位符
  const aiMessageIndex = messages.value.length;
  messages.value.push({
    sender: "ai",
    content: "",
    createTime: new Date().toISOString(),
  });

  try {
    // 调用流式API
    const stream = await chatApi.sendMessage({
      sessionId: currentSessionId.value,
      content: userMessage,
    });

    // 读取流式数据
    const reader = stream.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      // 解码并追加到AI消息
      const chunk = decoder.decode(value, { stream: true });
      if (messages.value[aiMessageIndex]) {
        messages.value[aiMessageIndex].content += chunk;
      }

      // 自动滚动
      await nextTick();
      scrollToBottom();
    }

    isAITyping.value = false;
  } catch (error) {
    console.error("发送消息失败:", error);
    ElMessage.error("发送消息失败，请重试");
    // 移除失败的AI消息占位符
    messages.value.splice(aiMessageIndex, 1);
    isAITyping.value = false;
  } finally {
    isSending.value = false;
  }
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

/* 简洁头部 */
.chat-header-simple {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.3);
}

.current-session-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-500);
}

.header-actions {
  display: flex;
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
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 24px;
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
  background: linear-gradient(135deg, var(--ease-accent) 0%, #059669 100%);
}

.user-avatar {
  background: var(--ease-warm);
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

/* ========== 输入区域 (复刻原型) ========== */
.chat-input-area {
  padding: 24px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  border-top: 1px solid rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
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

.send-btn:disabled {
  background: var(--gray-300) !important;
  transform: none;
  box-shadow: none;
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
  margin-top: 12px;
  padding: 0 8px;
}

.input-hints {
  display: flex;
  gap: 16px;
}

.hint-text {
  font-size: 12px;
  color: var(--gray-400);
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
}
</style>
