// 情绪日记相关 API
import request from "./request";
import type { ApiResponse } from "./request";
import { mockMoodData } from "@/mock/mood";

// 读取Mock开关（从环境变量）
const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// ==================== 类型定义 ====================

// 情绪类型
export type MoodType =
  | "Happy"
  | "Sad"
  | "Anxious"
  | "Calm"
  | "Angry"
  | "Tired"
  | "Excited";

// 情绪选项接口
export interface MoodOption {
  value: MoodType;
  label: string;
  emoji: string;
}

// ⭐ 情绪选项配置（全局统一定义）
export const MOOD_OPTIONS: MoodOption[] = [
  { value: "Happy", label: "开心", emoji: "😊" },
  { value: "Sad", label: "难过", emoji: "😢" },
  { value: "Anxious", label: "焦虑", emoji: "😰" },
  { value: "Calm", label: "平静", emoji: "😌" },
  { value: "Angry", label: "愤怒", emoji: "😠" },
  { value: "Tired", label: "疲惫", emoji: "😴" },
  { value: "Excited", label: "兴奋", emoji: "🎉" },
];

// ⭐ 情绪类型映射表（快速查找）
export const MOOD_TYPE_MAP: Record<MoodType, MoodOption> = MOOD_OPTIONS.reduce(
  (map, option) => {
    map[option.value] = option;
    return map;
  },
  {} as Record<MoodType, MoodOption>
);

// 情绪日记项（严格对齐API文档）
export interface MoodLogItem {
  id: number;
  userId?: number; // API有，但列表接口可能不返回
  moodType: MoodType;
  moodScore: number;
  content: string;
  tags?: string[];
  aiAnalysis?: string;
  logDate: string;
  createTime?: string; // API有，详情接口返回
  emoji?: string; // 列表接口返回，用于快速显示
}

// 情绪日记列表响应
export interface MoodLogsResponse {
  total: number;
  logs: MoodLogItem[];
}

// 创建情绪日记参数
export interface CreateMoodLogParams {
  moodType: MoodType;
  moodScore: number;
  content: string;
  tags?: string[];
  logDate: string;
}

// 提交响应（严格对齐后端MoodLogVO）
export interface CreateMoodResponse {
  logId: number;
  aiAnalysis: string;
}

// 情绪趋势数据项（前端转换使用）
export interface MoodTrendItem {
  date: string;
  score: number;
}

// 情绪趋势响应（严格对齐后端MoodTrendVO）
export interface MoodTrendResponse {
  dates: string[]; // 日期数组
  scores: number[]; // 分数数组
  avgScore: number; // 平均分
  positiveRate: number; // 积极情绪占比
  continuousDays: number; // 连续记录天数
}

// 情绪统计响应（严格对齐后端MoodStatisticsVO）
export interface MoodStatsResponse {
  distribution: Record<string, string>; // 情绪分布，注意值是string类型
  totalLogs: number; // 总记录数
  avgScore: number; // 平均分
}

// ==================== API函数 ====================

/**
 * 提交情绪日记
 * POST /mood/log
 */
export function createMoodLog(data: CreateMoodLogParams) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.createMoodLog(data) as any);
      }, 500);
    });
  }

  // 真实请求
  return request.post<ApiResponse<CreateMoodResponse>>("/mood/log", data);
}

/**
 * 获取情绪日记列表
 * GET /mood/logs?limit=10&offset=0
 */
export function getMoodLogs(limit = 10, offset = 0) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.getMoodLogs(limit, offset) as any);
      }, 300);
    });
  }

  // 真实请求
  return request.get<ApiResponse<MoodLogsResponse>>("/mood/logs", {
    params: { limit, offset },
  });
}

/**
 * 获取单条日记详情
 * GET /mood/log/{id}
 */
export function getMoodLogDetail(id: number) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.getMoodLogDetail(id) as any);
      }, 300);
    });
  }

  // 真实请求
  return request.get<ApiResponse<MoodLogItem>>(`/mood/log/${id}`);
}

/**
 * 更新情绪日记
 * PUT /mood/log/{id}
 */
export function updateMoodLog(id: number, data: CreateMoodLogParams) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.updateMoodLog(id, data) as any);
      }, 400);
    });
  }

  // 真实请求
  return request.put<ApiResponse<any>>(`/mood/log/${id}`, data);
}

/**
 * 删除情绪日记
 * DELETE /mood/log/{id}
 */
export function deleteMoodLog(id: number) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.deleteMoodLog(id) as any);
      }, 300);
    });
  }

  // 真实请求
  return request.delete<ApiResponse<null>>(`/mood/log/${id}`);
}

/**
 * 获取情绪趋势（用于图表）
 * GET /mood/trend?days=7
 */
export function getMoodTrend(days = 7) {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.getMoodTrend as any);
      }, 300);
    });
  }

  // 真实请求
  return request.get<ApiResponse<MoodTrendResponse>>("/mood/trend", {
    params: { days },
  });
}

/**
 * 获取情绪统计
 * GET /mood/statistics (注意：后端路径是statistics不是stats)
 */
export function getMoodStats() {
  // Mock数据
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockMoodData.getMoodStats as any);
      }, 300);
    });
  }

  // 真实请求
  return request.get<ApiResponse<MoodStatsResponse>>("/mood/statistics");
}
