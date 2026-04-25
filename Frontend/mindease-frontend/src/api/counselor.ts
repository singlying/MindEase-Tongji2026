// 前端B负责：咨询师资质与工作台接口占位
import request from "./request";

export const getAuditStatusApi = () => request.get("/counselor/audit/status");

export const submitAuditApi = (payload: Record<string, unknown>) =>
  request.post("/counselor/audit/submit", payload);
