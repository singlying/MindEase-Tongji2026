// 情绪报告 API（用户端）
// 真实接口需要 startDate/endDate，Mock 模式下自动忽略时间参数

import request from "./request";
import type { ApiResponse } from "./request";
import { mockReportData } from "@/mock/report";
import axios from "axios";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";
const BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

// ============ 类型定义 ============

export interface EmotionTrendPoint {
  date: string;
  score: number;
}

export interface EmotionDistributionItem {
  moodType: string;
  value: number;
}

export interface EmotionReportData {
  period: string; // 报告周期，例如 2024-11
  avgScore: number;
  positiveRate: number; // 0-1
  continuousDays: number;
  trendData: {
    dates: string[];
    scores: number[];
  };
  distribution: Record<string, number | string>; // 兼容字符串百分比
  recentLogs: Array<{
    date: string;
    moodType: string;
    score: number;
    content: string;
    emoji?: string;
  }>;
  aiSuggestions: string[];
}

// ============ API 函数 ============

/** 获取情绪报告（需要 startDate、endDate） */
export const getEmotionReport = (params: {
  startDate: string;
  endDate: string;
}) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockReportData.getEmotionReport());
      }, 300);
    });
  }
  return request.get<ApiResponse<EmotionReportData>>("/report/emotion", {
    params,
  });
};

/**
 * 导出情绪报告 PDF
 * 说明：后端返回二进制PDF，不走全局拦截器，避免code校验。
 */
export const exportEmotionReport = async (format: "pdf" = "pdf") => {
  if (USE_MOCK) {
    return { code: 200, message: "mock export ok", data: null };
  }
  const token = localStorage.getItem("token") || "";
  const res = await axios.get(`${BASE_URL}/report/export`, {
    params: { format },
    responseType: "blob",
    headers: token ? { token } : {},
  });
  return res;
};

/**
 * 咨询师导出指定用户的情绪报告 PDF
 */
export const exportUserEmotionReport = async (
  userId: number,
  format: "pdf" = "pdf"
) => {
  if (USE_MOCK) {
    return { code: 200, message: "mock export ok", data: null };
  }
  const token = localStorage.getItem("token") || "";
  const res = await axios.get(`${BASE_URL}/report/export/${userId}`, {
    params: { format },
    responseType: "blob",
    headers: token ? { token } : {},
  });
  return res;
};