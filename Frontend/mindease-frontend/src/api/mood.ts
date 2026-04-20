// 前端A负责：情绪日记模块接口、类型与本地 mock 适配
import request from "./request";
import { moodMockService } from "@/mock/mood";

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== "false";

export type MoodType =
  | "Happy"
  | "Sad"
  | "Anxious"
  | "Calm"
  | "Angry"
  | "Tired"
  | "Excited";

export interface MoodOption {
  value: MoodType;
  label: string;
  emoji: string;
}

export const MOOD_OPTIONS: MoodOption[] = [
  { value: "Happy", label: "开心", emoji: "😊" },
  { value: "Sad", label: "难过", emoji: "😢" },
  { value: "Anxious", label: "焦虑", emoji: "😰" },
  { value: "Calm", label: "平静", emoji: "😌" },
  { value: "Angry", label: "愤怒", emoji: "😠" },
  { value: "Tired", label: "疲惫", emoji: "😴" },
  { value: "Excited", label: "兴奋", emoji: "🎉" },
];

export const MOOD_TYPE_MAP: Record<MoodType, MoodOption> = MOOD_OPTIONS.reduce(
  (map, option) => {
    map[option.value] = option;
    return map;
  },
  {} as Record<MoodType, MoodOption>
);

export interface MoodLogItem {
  id: number;
  moodType: MoodType;
  moodScore: number;
  content: string;
  tags: string[];
  emoji: string;
  logDate: string;
  aiAnalysis?: string;
}

export interface CreateMoodLogParams {
  moodType: MoodType;
  moodScore: number;
  content: string;
  tags?: string[];
  logDate: string;
}

export interface MoodTrendItem {
  date: string;
  score: number;
}

export interface MoodLogsResponse {
  total: number;
  logs: MoodLogItem[];
}

export interface MoodTrendResponse {
  dates: string[];
  scores: number[];
  avgScore: number;
  positiveRate: number;
  continuousDays: number;
}

export interface MoodStatsResponse {
  averageScore: number;
  positiveRate: number;
  totalLogs: number;
}

export async function createMoodLog(payload: CreateMoodLogParams) {
  if (USE_MOCK) {
    return moodMockService.createMoodLog(payload);
  }

  return request.post("/mood/log", payload);
}

export async function getMoodLogs(limit = 10, offset = 0) {
  if (USE_MOCK) {
    return moodMockService.getMoodLogs(limit, offset);
  }

  return request.get("/mood/logs", { params: { limit, offset } });
}

export async function getMoodLogDetail(id: number) {
  if (USE_MOCK) {
    return moodMockService.getMoodLogDetail(id);
  }

  return request.get(`/mood/log/${id}`);
}

export async function deleteMoodLog(id: number) {
  if (USE_MOCK) {
    return moodMockService.deleteMoodLog(id);
  }

  return request.delete(`/mood/log/${id}`);
}

export async function getMoodTrend(days = 7) {
  if (USE_MOCK) {
    return moodMockService.getMoodTrend(days);
  }

  return request.get("/mood/trend", { params: { days } });
}

export async function getMoodStats() {
  if (USE_MOCK) {
    return moodMockService.getMoodStats();
  }

  return request.get("/mood/statistics");
}
