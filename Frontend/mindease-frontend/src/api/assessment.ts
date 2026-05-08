// 前端A负责：心理测评模块接口、类型与本地 mock 适配
import request from "./request";
import { assessmentMockService } from "@/mock/assessment";

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== "false";

export interface Scale {
  id: number;
  scaleKey: string;
  title: string;
  description: string;
  coverColor: string;
  questionCount: number;
  estimatedMinutes: number;
  status: "active" | "inactive";
}

export interface QuestionOption {
  label: string;
  score: number;
}

export interface AssessmentQuestion {
  id: number;
  text: string;
  options: QuestionOption[];
}

export interface ScaleDetail {
  id: number;
  scaleKey: string;
  title: string;
  description: string;
  instruction: string;
  questions: AssessmentQuestion[];
}

export interface AssessmentAnswer {
  questionId: number;
  score: number;
}

export interface SubmitAssessmentParams {
  scaleKey: string;
  answers: AssessmentAnswer[];
}

export interface SubmitAssessmentResponse {
  recordId: number;
  totalScore: number;
  resultLevel: string;
  resultDesc: string;
}

export async function getScaleList() {
  if (USE_MOCK) {
    return assessmentMockService.getScaleList();
  }

  return request.get("/assessment/scales");
}

export async function getScaleDetail(scaleKey: string) {
  if (USE_MOCK) {
    return assessmentMockService.getScaleDetail(scaleKey);
  }

  return request.get(`/assessment/scale/${scaleKey}`);
}

export async function submitAssessment(payload: SubmitAssessmentParams) {
  if (USE_MOCK) {
    return assessmentMockService.submitAssessment(payload);
  }

  return request.post("/assessment/submit", payload);
}
