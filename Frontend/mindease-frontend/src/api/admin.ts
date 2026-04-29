// 前端B负责：管理员工作台接口占位
import request from "./request";

export const getAuditListApi = () => request.get("/admin/audit/list");

export const processAuditApi = (payload: Record<string, unknown>) =>
  request.post("/admin/audit/process", payload);
