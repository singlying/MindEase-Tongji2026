// 前端B负责：咨询师资质与工作台接口
import request from "./request";

export interface AuditStatus {
  latestStatus: "PENDING" | "APPROVED" | "REJECTED";
  submitTime?: string;
  auditRemark?: string;
}

export interface SubmitAuditPayload {
  realName: string;
  qualificationUrl: string;
  idCardUrl?: string;
  title: string;
  experienceYears?: number;
  specialty: string[];
  bio?: string;
  location: string;
  pricePerHour?: number;
}

export const getAuditStatus = () => request.get<AuditStatus>("/counselor/audit/status");

export const submitAudit = (payload: SubmitAuditPayload) =>
  request.post("/counselor/audit/submit", payload);

export const getAuditStatusApi = getAuditStatus;

export const submitAuditApi = (payload: Record<string, unknown>) =>
  submitAudit(payload as unknown as SubmitAuditPayload);
