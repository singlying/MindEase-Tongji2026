import request from "./request";
import type { ApiResponse } from "./request";

/**
 * AI聊天模块API
 * 对齐后端VO定义
 */

// ========== VO类型定义 ==========

/**
 * 会话创建响应
 */
export interface ChatSessionCreateVO {
  sessionId: string;
}

/**
 * 会话信息
 */
export interface ChatSessionVO {
  sessionId: string;
  sessionTitle: string; // 注意：后端VO是驼峰命名，不是session_title
  createTime: string; // ISO 8601格式: "2023-10-27T16:00:00"
}

/**
 * 会话列表响应
 */
export interface ChatSessionListVO {
  total: number;
  sessions: ChatSessionVO[];
}

/**
 * 聊天消息
 */
export interface ChatMessageVO {
  sender: string; // "user" | "ai" (后端返回小写)
  content: string;
  createTime: string; // ISO 8601格式
}

/**
 * 历史记录响应
 */
export interface ChatHistoryVO {
  sessionId: string;
  messages: ChatMessageVO[];
}

/**
 * 删除会话响应
 */
export interface ChatDeleteVO {
  success: boolean;
}

/**
 * 敏感词检测结果
 * 对齐后端 SensitiveWordCheckVO
 */
export interface SensitiveWordCheckVO {
  containsSensitiveWord: boolean;
  sensitiveWords: string[];
  originalText: string;
}

export interface SpeechTranscriptionVO {
  text: string;
  audioUrl: string;
  format: string;
}

// ========== DTO类型定义 ==========

/**
 * 发送消息请求
 */
export interface ChatMessageSendDTO {
  sessionId: string;
  content: string;
}

// ========== API方法 ==========

/**
 * 创建AI会话
 */
export const createSession = () => {
  return request.post<ApiResponse<ChatSessionCreateVO>>("/chat/session");
};

/**
 * 获取会话列表
 * @param limit 限制数量，默认20
 */
export const getSessionList = (limit: number = 20) => {
  return request.get<ApiResponse<ChatSessionListVO>>("/chat/sessions", {
    params: { limit },
  });
};

/**
 * 发送消息（SSE流式响应）
 * 注意：此方法返回ReadableStream，需要特殊处理
 * @param dto 消息发送DTO
 * @returns ReadableStream用于接收流式文本
 */
export const sendMessage = async (
  dto: ChatMessageSendDTO
): Promise<ReadableStream<Uint8Array>> => {
  const token = localStorage.getItem("token");
  const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  const response = await fetch(`${baseURL}/chat/message`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      token: token || "", // 注意：后端使用token请求头，不需要Bearer前缀
    },
    body: JSON.stringify(dto),
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  if (!response.body) {
    throw new Error("No response body");
  }

  return response.body;
};

/**
 * 获取会话历史记录
 * @param sessionId 会话ID
 * @param limit 限制数量，默认50
 */
export const getHistory = (sessionId: string, limit: number = 50) => {
  return request.get<ApiResponse<ChatHistoryVO>>(`/chat/history/${sessionId}`, {
    params: { limit },
  });
};

/**
 * 删除会话
 * @param sessionId 会话ID
 */
export const deleteSession = (sessionId: string) => {
  return request.delete<ApiResponse<ChatDeleteVO>>(
    `/chat/session/${sessionId}`
  );
};

/**
 * 检测聊天内容中的敏感词（例如自杀/自残相关表达）
 */
export const checkSensitiveWords = (dto: ChatMessageSendDTO) => {
  return request.post<ApiResponse<SensitiveWordCheckVO>>(
    "/chat/check-sensitive-words",
    dto
  );
};

export const transcribeAudio = async (file: Blob): Promise<SpeechTranscriptionVO> => {
  const formData = new FormData();
  formData.append("file", file, "speech.webm");

  const response = (await request.post(
    "/chat/asr",
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }
  )) as any;

  return response.data as SpeechTranscriptionVO;
};

export const synthesizeSpeech = async (text: string): Promise<Blob> => {
  const token = localStorage.getItem("token");
  const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

  const response = await fetch(`${baseURL}/chat/tts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      token: token || "",
    },
    body: JSON.stringify({ text }),
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return await response.blob();
};
