// 前端A负责：情绪模块本地 mock，支撑 week2 首页与情绪日记功能
import type {
  CreateMoodLogParams,
  MoodLogItem,
  MoodStatsResponse,
  MoodTrendItem,
  MoodTrendResponse,
  MoodType,
} from "@/api/mood";
import { MOOD_TYPE_MAP } from "@/api/mood";

const STORAGE_KEY = "mindease-mock-mood-logs";

const seedLogs: MoodLogItem[] = [
  {
    id: 5001,
    logDate: "2026-04-24T21:10:00",
    moodType: "Calm",
    moodScore: 7,
    content: "今天把课程资料整理完了一部分，节奏虽然紧，但心里比前几天平静很多。",
    emoji: "😌",
    tags: ["学习", "整理"],
    aiAnalysis: "你的状态整体比较稳定，保持当前节奏，同时别忘了适当休息。",
  },
  {
    id: 5002,
    logDate: "2026-04-23T19:30:00",
    moodType: "Anxious",
    moodScore: 5,
    content: "想到后面还有很多提交和文档要准备，还是会有些紧张，但至少已经开始推进了。",
    emoji: "😰",
    tags: ["项目", "焦虑"],
    aiAnalysis: "适度焦虑说明你在意结果，建议把任务拆小并按周推进，能明显降低压力感。",
  },
  {
    id: 5003,
    logDate: "2026-04-22T22:00:00",
    moodType: "Happy",
    moodScore: 8,
    content: "今天把前端骨架搭起来了，看到页面可以正常跑起来之后轻松了很多。",
    emoji: "😊",
    tags: ["项目", "进展"],
    aiAnalysis: "完成阶段性目标后获得积极反馈，这是非常健康的激励循环。",
  },
  {
    id: 5004,
    logDate: "2026-04-21T18:15:00",
    moodType: "Tired",
    moodScore: 4,
    content: "白天事情有点多，晚上写东西的时候注意力不太集中，感觉明显疲惫。",
    emoji: "😴",
    tags: ["疲惫"],
    aiAnalysis: "今天的主要问题更像是精力消耗，不一定是情绪本身失控，建议早点休息。",
  },
  {
    id: 5005,
    logDate: "2026-04-20T20:40:00",
    moodType: "Excited",
    moodScore: 8,
    content: "新的两个月复刻计划终于定下来了，虽然任务多，但觉得方向很清楚。",
    emoji: "🎉",
    tags: ["计划", "启动"],
    aiAnalysis: "目标清晰会显著提升行动感，建议继续把这种节奏维持下去。",
  },
  {
    id: 5006,
    logDate: "2026-04-19T23:00:00",
    moodType: "Sad",
    moodScore: 3,
    content: "回头看 old 项目时会有一点压力，担心后面提交节奏掌控不好。",
    emoji: "😢",
    tags: ["压力"],
    aiAnalysis: "你对质量有要求，所以会担心节奏失控。建议优先保证稳定推进，而不是一次完成太多。",
  },
  {
    id: 5007,
    logDate: "2026-04-18T17:20:00",
    moodType: "Happy",
    moodScore: 7,
    content: "和同学讨论之后，分工边界更清晰了，后面沟通应该会顺很多。",
    emoji: "😊",
    tags: ["协作", "分工"],
    aiAnalysis: "分工明确通常能显著降低协作摩擦，这是一个积极信号。",
  },
];

function canUseStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function loadLogs(): MoodLogItem[] {
  if (!canUseStorage()) {
    return [...seedLogs];
  }

  const raw = window.localStorage.getItem(STORAGE_KEY);

  if (!raw) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(seedLogs));
    return [...seedLogs];
  }

  try {
    return JSON.parse(raw) as MoodLogItem[];
  } catch {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(seedLogs));
    return [...seedLogs];
  }
}

function saveLogs(logs: MoodLogItem[]) {
  if (!canUseStorage()) {
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(logs));
}

function getEmoji(type: MoodType) {
  return MOOD_TYPE_MAP[type]?.emoji || "🙂";
}

function sortByDateDesc(logs: MoodLogItem[]) {
  return [...logs].sort(
    (a, b) => new Date(b.logDate).getTime() - new Date(a.logDate).getTime()
  );
}

function buildTrendItems(logs: MoodLogItem[], days: number): MoodTrendItem[] {
  const sorted = [...logs].sort(
    (a, b) => new Date(a.logDate).getTime() - new Date(b.logDate).getTime()
  );
  return sorted.slice(-days).map((item) => ({
    date: item.logDate,
    score: item.moodScore,
  }));
}

export const moodMockService = {
  async createMoodLog(payload: CreateMoodLogParams) {
    const logs = sortByDateDesc(loadLogs());
    const nextId = logs.length ? Math.max(...logs.map((item) => item.id)) + 1 : 5001;

    const newLog: MoodLogItem = {
      id: nextId,
      logDate: payload.logDate,
      moodType: payload.moodType,
      moodScore: payload.moodScore,
      content: payload.content.trim(),
      tags: payload.tags || [],
      emoji: getEmoji(payload.moodType),
      aiAnalysis:
        payload.moodScore >= 7
          ? "你今天记录下了比较积极的状态，建议保持这种觉察和表达。"
          : payload.moodScore >= 5
            ? "今天的情绪总体可控，适合通过休息和整理思路来继续稳定状态。"
            : "今天的压力感比较明显，建议适当放慢节奏，并考虑向信任的人倾诉。",
    };

    const nextLogs = sortByDateDesc([newLog, ...logs]);
    saveLogs(nextLogs);

    return {
      code: 200,
      message: "success",
      data: {
        logId: newLog.id,
        aiAnalysis: newLog.aiAnalysis,
      },
    };
  },

  async getMoodLogs(limit = 10, offset = 0) {
    const logs = sortByDateDesc(loadLogs());
    return {
      code: 200,
      message: "success",
      data: {
        total: logs.length,
        logs: logs.slice(offset, offset + limit),
      },
    };
  },

  async getMoodLogDetail(id: number) {
    const logs = loadLogs();
    const log = logs.find((item) => item.id === id);

    if (!log) {
      throw new Error("日志不存在");
    }

    return {
      code: 200,
      message: "success",
      data: log,
    };
  },

  async deleteMoodLog(id: number) {
    const logs = loadLogs();
    const nextLogs = logs.filter((item) => item.id !== id);
    saveLogs(nextLogs);

    return {
      code: 200,
      message: "success",
      data: null,
    };
  },

  async getMoodTrend(days = 7) {
    const logs = sortByDateDesc(loadLogs());
    const trendItems = buildTrendItems(logs, days);
    const scores = trendItems.map((item) => item.score);
    const avgScore =
      scores.length > 0 ? scores.reduce((sum, score) => sum + score, 0) / scores.length : 0;
    const positiveRate =
      scores.length > 0 ? scores.filter((score) => score >= 7).length / scores.length : 0;

    const data: MoodTrendResponse = {
      dates: trendItems.map((item) => item.date),
      scores,
      avgScore: Number(avgScore.toFixed(2)),
      positiveRate: Number(positiveRate.toFixed(2)),
      continuousDays: trendItems.length,
    };

    return {
      code: 200,
      message: "success",
      data,
    };
  },

  async getMoodStats() {
    const logs = loadLogs();
    const scores = logs.map((item) => item.moodScore);
    const averageScore =
      scores.length > 0 ? scores.reduce((sum, score) => sum + score, 0) / scores.length : 0;

    const data: MoodStatsResponse = {
      averageScore: Number(averageScore.toFixed(2)),
      positiveRate:
        scores.length > 0 ? Number((scores.filter((score) => score >= 7).length / scores.length).toFixed(2)) : 0,
      totalLogs: logs.length,
    };

    return {
      code: 200,
      message: "success",
      data,
    };
  },
};
