// 咨询师端相关API（使用者：咨询师）
// 包含：资质审核等咨询师自己需要使用的接口
import request from "./request";
import type { ApiResponse } from "./request";

// ============ 类型定义 ============

/**
 * 提交审核资料
 */
export interface AuditSubmitParams {
  realName: string;
  qualificationUrl: string;
  idCardUrl?: string;
  title?: string;
  experienceYears?: number;
  specialty?: string[];
  bio?: string;
  location?: string;
  pricePerHour?: number;
}

export interface AuditSubmitResponse {
  auditId: number;
}

/**
 * 审核状态（严格对齐后端AuditStatusVO）
 */
export interface AuditStatus {
  latestStatus: "PENDING" | "APPROVED" | "REJECTED";
  auditRemark?: string;
  submitTime: string;
}

// ============ 咨询师端API ============

/**
 * 提交资质审核
 */
export const submitAudit = (data: AuditSubmitParams) => {
  return request.post<ApiResponse<AuditSubmitResponse>>(
    "/counselor/audit/submit",
    data
  );
};

/**
 * 获取审核状态
 */
export const getAuditStatus = () => {
  return request.get<ApiResponse<AuditStatus>>("/counselor/audit/status");
};
