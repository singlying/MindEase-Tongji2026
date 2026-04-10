// 情绪报告 Mock 数据
// 基于现有情绪日记 Mock 数据自动生成报告

import { mockMoodData } from "@/mock/mood";
import type { EmotionReportData } from "@/api/report";

const getPeriodFromDate = (dateStr: string) => {
  const d = new Date(dateStr);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  return `${y}-${m}`;
};

const buildReport = (): EmotionReportData => {
  const trend = mockMoodData.getMoodTrend.data;
  const stats = mockMoodData.getMoodStats.data;
  const recentLogs = mockMoodData.getMoodLogs(5, 0).data.logs || [];

  const firstLogDate =
    recentLogs.length > 0 && recentLogs[0]?.logDate
      ? recentLogs[0].logDate
      : new Date().toISOString();
  const period = getPeriodFromDate(firstLogDate);

  // 将 distribution 中的字符串数值转 number
  const distribution: Record<string, number> = {};
  Object.entries(stats.distribution).forEach(([k, v]) => {
    distribution[k] = typeof v === "string" ? Number(v) : (v as number);
  });

  // 简单计算正向情绪占比
  const positiveKeys = ["Happy", "Excited", "Calm"];
  const totalCount = Object.values(distribution).reduce(
    (sum, n) => sum + (Number.isFinite(n) ? (n as number) : 0),
    0
  );
  const positiveCount = Object.entries(distribution).reduce((sum, [k, v]) => {
    if (positiveKeys.includes(k)) {
      return sum + (v as number);
    }
    return sum;
  }, 0);
  const positiveRate = totalCount > 0 ? positiveCount / totalCount : 0;

  return {
    period,
    avgScore: stats.avgScore,
    positiveRate,
    continuousDays: trend.continuousDays,
    trendData: trend,
    distribution,
    recentLogs: recentLogs.map((log) => ({
      date: log.logDate,
      moodType: log.moodType,
      score: log.moodScore,
      content: log.content,
      emoji: log.emoji,
    })),
    aiSuggestions: [
      "保持稳定的作息和适度运动，有助于维持情绪水平。",
      "记录情绪触发点，尝试在低谷时做一次深呼吸或短暂散步。",
      "如果连续多天情绪低落，考虑寻求专业咨询支持。",
    ],
  };
};

export const mockReportData = {
  getEmotionReport: () => ({
    code: 200,
    message: "success",
    data: buildReport(),
  }),
};
