// 前端A负责：AI 咨询接口占位
import request from "./request";

export const createChatSessionApi = () => request.post("/chat/session");

export const sendChatMessageApi = (payload: Record<string, unknown>) =>
  request.post("/chat/message", payload);
