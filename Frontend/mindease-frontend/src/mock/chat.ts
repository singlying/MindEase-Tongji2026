// 前端A负责：AI 咨询模块本地数据
import type {
  ChatMessage,
  ChatMessageParams,
  ChatReplyResponse,
  ChatSession,
  ChatSessionListResponse,
  SensitiveWordResult,
} from "@/api/chat";

const sessions: ChatSession[] = [
  {
    sessionId: "session-welcome",
    sessionTitle: "今日状态整理",
    createTime: new Date(Date.now() - 1000 * 60 * 26).toISOString(),
    updatedTime: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
  },
];

const messages: Record<string, ChatMessage[]> = {
  "session-welcome": [
    {
      id: "msg-welcome-ai",
      sessionId: "session-welcome",
      sender: "ai",
      content:
        "你好，我在这里陪你慢慢梳理今天的感受。你可以从一个词、一句话，或者一个具体场景开始。",
      createTime: new Date(Date.now() - 1000 * 60 * 26).toISOString(),
    },
  ],
};

const riskWords = ["自杀", "轻生", "自残", "不想活", "结束生命"];

function wait<T>(data: T, delay = 260) {
  return new Promise<{ data: T }>((resolve) => {
    window.setTimeout(() => resolve({ data }), delay);
  });
}

function createId(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

function getSession(sessionId: string) {
  return sessions.find((item) => item.sessionId === sessionId);
}

function buildSessionTitle(content: string) {
  const compact = content.trim().replace(/\s+/g, " ");
  return compact.length > 14 ? `${compact.slice(0, 14)}...` : compact || "新的对话";
}

function getDetectedWords(content: string) {
  return riskWords.filter((word) => content.includes(word));
}

function getReply(content: string) {
  if (getDetectedWords(content).length > 0) {
    return "我注意到你提到了一些让人担心的内容。请先把自己放到安全的位置，联系身边可信任的人，必要时及时拨打当地紧急救助电话或心理危机热线。你不需要一个人扛着。";
  }

  if (/睡|失眠|困/.test(content)) {
    return "睡眠困扰会明显消耗情绪能量。今晚可以先把目标放低：远离屏幕十分钟、做几轮缓慢呼吸，再记录一个让身体稍微放松的动作。";
  }

  if (/焦虑|担心|紧张|慌/.test(content)) {
    return "焦虑常常像把未来的风险提前拉到眼前。我们可以先把它拆小：现在最具体、最可控的一件事是什么？先完成这一小步就好。";
  }

  if (/压力|累|崩溃|忙/.test(content)) {
    return "听起来你已经承受了不少压力。可以先给自己一分钟，把任务写成“必须今天完成”和“可以延后”两类，让大脑从混乱里拿回一点秩序。";
  }

  if (/难过|低落|沮丧|委屈/.test(content)) {
    return "这些感受值得被认真看见。你可以试着描述一下：这种难过更像疲惫、失望，还是孤单？给感受命名，往往是照顾自己的第一步。";
  }

  return "谢谢你愿意说出来。我会陪你一起梳理：这件事带给你的主要感受是什么？如果把强度从 1 到 10 打分，现在大概是多少？";
}

export const chatMockService = {
  createChatSession() {
    const now = new Date().toISOString();
    const session: ChatSession = {
      sessionId: createId("session"),
      sessionTitle: "新的对话",
      createTime: now,
      updatedTime: now,
    };

    sessions.unshift(session);
    messages[session.sessionId] = [
      {
        id: createId("msg"),
        sessionId: session.sessionId,
        sender: "ai",
        content: "今天想从哪里开始聊起？我会先倾听，再陪你一起整理。",
        createTime: now,
      },
    ];

    return wait(session);
  },

  getChatSessionList(limit: number) {
    const data: ChatSessionListResponse = {
      total: sessions.length,
      sessions: sessions
        .slice()
        .sort(
          (a, b) =>
            new Date(b.updatedTime).getTime() - new Date(a.updatedTime).getTime(),
        )
        .slice(0, limit),
    };

    return wait(data);
  },

  getChatHistory(sessionId: string, limit: number) {
    const history = messages[sessionId] ?? [];

    return wait({
      sessionId,
      messages: history.slice(-limit),
    });
  },

  checkSensitiveWords(payload: ChatMessageParams) {
    const sensitiveWords = getDetectedWords(payload.content);
    const data: SensitiveWordResult = {
      containsSensitiveWord: sensitiveWords.length > 0,
      sensitiveWords,
      originalText: payload.content,
      suggestion:
        "如果你正处于危险中，请立即联系身边可信任的人、当地急救电话或心理危机干预热线。",
    };

    return wait(data, 160);
  },

  sendChatMessage(payload: ChatMessageParams) {
    const now = new Date().toISOString();
    const session = getSession(payload.sessionId);

    if (!session) {
      throw new Error("会话不存在");
    }

    if (session.sessionTitle === "新的对话") {
      session.sessionTitle = buildSessionTitle(payload.content);
    }

    session.updatedTime = now;

    const userMessage: ChatMessage = {
      id: createId("msg-user"),
      sessionId: payload.sessionId,
      sender: "user",
      content: payload.content,
      createTime: now,
    };
    const aiMessage: ChatMessage = {
      id: createId("msg-ai"),
      sessionId: payload.sessionId,
      sender: "ai",
      content: getReply(payload.content),
      createTime: new Date(Date.now() + 800).toISOString(),
    };

    messages[payload.sessionId] = [
      ...(messages[payload.sessionId] ?? []),
      userMessage,
      aiMessage,
    ];

    const data: ChatReplyResponse = {
      session,
      userMessage,
      aiMessage,
    };

    return wait(data, 520);
  },

  deleteChatSession(sessionId: string) {
    const index = sessions.findIndex((item) => item.sessionId === sessionId);

    if (index >= 0) {
      sessions.splice(index, 1);
      delete messages[sessionId];
    }

    return wait({ success: true }, 180);
  },
};
