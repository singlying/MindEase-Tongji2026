// 前端A负责：AI 咨询接口与类型定义
import { chatMockService } from "@/mock/chat";

import request from "./request";

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== "false";

export type ChatSender = "user" | "ai";

export interface ChatSession {
  sessionId: string;
  sessionTitle: string;
  createTime: string;
  updatedTime: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  sender: ChatSender;
  content: string;
  createTime: string;
}

export interface ChatSessionListResponse {
  total: number;
  sessions: ChatSession[];
}

export interface ChatHistoryResponse {
  sessionId: string;
  messages: ChatMessage[];
}

export interface ChatMessageParams {
  sessionId: string;
  content: string;
}

export interface ChatReplyResponse {
  session: ChatSession;
  userMessage: ChatMessage;
  aiMessage: ChatMessage;
}

export interface SensitiveWordResult {
  containsSensitiveWord: boolean;
  sensitiveWords: string[];
  originalText: string;
  suggestion: string;
}

type ApiResult<T> = Promise<{ data: T }>;

export function createChatSession(): ApiResult<ChatSession> {
  if (USE_MOCK) {
    return chatMockService.createChatSession();
  }

  return request.post("/chat/session") as unknown as ApiResult<ChatSession>;
}

export function getChatSessionList(
  limit = 20,
): ApiResult<ChatSessionListResponse> {
  if (USE_MOCK) {
    return chatMockService.getChatSessionList(limit);
  }

  return request.get("/chat/sessions", {
    params: { limit },
  }) as unknown as ApiResult<ChatSessionListResponse>;
}

export function getChatHistory(
  sessionId: string,
  limit = 50,
): ApiResult<ChatHistoryResponse> {
  if (USE_MOCK) {
    return chatMockService.getChatHistory(sessionId, limit);
  }

  return request.get(`/chat/history/${sessionId}`, {
    params: { limit },
  }) as unknown as ApiResult<ChatHistoryResponse>;
}

export function sendChatMessage(
  payload: ChatMessageParams,
): ApiResult<ChatReplyResponse> {
  if (USE_MOCK) {
    return chatMockService.sendChatMessage(payload);
  }

  return request.post("/chat/message", payload) as unknown as ApiResult<ChatReplyResponse>;
}

export function checkSensitiveWords(
  payload: ChatMessageParams,
): ApiResult<SensitiveWordResult> {
  if (USE_MOCK) {
    return chatMockService.checkSensitiveWords(payload);
  }

  return request.post(
    "/chat/check-sensitive-words",
    payload,
  ) as unknown as ApiResult<SensitiveWordResult>;
}

export function deleteChatSession(sessionId: string): ApiResult<{ success: boolean }> {
  if (USE_MOCK) {
    return chatMockService.deleteChatSession(sessionId);
  }

  return request.delete(`/chat/session/${sessionId}`) as unknown as ApiResult<{
    success: boolean;
  }>;
}

export const createChatSessionApi = createChatSession;
export const sendChatMessageApi = sendChatMessage;
