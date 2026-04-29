// 前端A负责：心理测评接口占位
import request from "./request";

export const getAssessmentListApi = () => request.get("/assessment/list");

export const getAssessmentDetailApi = (scaleKey: string) =>
  request.get(`/assessment/${scaleKey}`);

export const submitAssessmentApi = (payload: Record<string, unknown>) =>
  request.post("/assessment/submit", payload);
