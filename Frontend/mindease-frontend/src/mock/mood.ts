// 情绪日记模块的Mock数据
// 后端未完成时使用这些假数据进行开发

import type { MoodLogItem, CreateMoodLogParams } from "@/api/mood";

// 动态Mock数据存储（可增删改查）
let mockLogsStorage: MoodLogItem[] = [
  {
    id: 5001,
    logDate: "2024-11-27T14:00:00",
    moodType: "Happy",
    moodScore: 8,
    content:
      "今天天气很好，心情不错。完成了所有工作任务，还去公园散了步，看到了漂亮的晚霞。",
    emoji: "😄",
    tags: ["天气", "工作", "运动"],
  },
  {
    id: 5002,
    logDate: "2024-11-26T10:30:00",
    moodType: "Calm",
    moodScore: 7,
    content: "工作顺利完成，感觉平静。中午和同事一起吃了午饭，聊得很开心。",
    emoji: "😌",
    tags: ["工作", "社交"],
  },
  {
    id: 5003,
    logDate: "2024-11-25T16:00:00",
    moodType: "Anxious",
    moodScore: 4,
    content:
      "明天有个重要会议，有点焦虑。需要准备好PPT和演讲内容，压力有点大。",
    emoji: "😰",
    tags: ["工作", "焦虑"],
  },
  {
    id: 5004,
    logDate: "2024-11-24T20:15:00",
    moodType: "Excited",
    moodScore: 9,
    content: "收到了升职的好消息！太开心了，这段时间的努力终于有了回报。",
    emoji: "🎉",
    tags: ["工作", "成就"],
  },
  {
    id: 5005,
    logDate: "2024-11-23T08:00:00",
    moodType: "Tired",
    moodScore: 5,
    content:
      "昨晚加班到很晚，今天早上起不来。感觉身体有点疲惫，需要好好休息一下。",
    emoji: "😴",
    tags: ["疲惫", "工作"],
  },
  {
    id: 5006,
    logDate: "2024-11-22T19:30:00",
    moodType: "Happy",
    moodScore: 8,
    content:
      "周末和家人一起吃了顿大餐，聊了很多有趣的话题。家人的陪伴总是让人感到温暖。",
    emoji: "😊",
    tags: ["家庭", "社交"],
  },
  {
    id: 5007,
    logDate: "2024-11-21T15:45:00",
    moodType: "Sad",
    moodScore: 3,
    content: "项目被否决了，之前的努力都白费了。感觉很沮丧，需要调整一下心态。",
    emoji: "😢",
    tags: ["工作", "挫折"],
  },
  {
    id: 5008,
    logDate: "2024-11-20T11:00:00",
    moodType: "Calm",
    moodScore: 6,
    content:
      "做了一次冥想，感觉内心平静了很多。最近压力有点大，需要找到放松的方法。",
    emoji: "🧘",
    tags: ["冥想", "放松"],
  },
];

// 下一个ID（自增）
let nextMockId = 5009;

// 情绪类型对应的emoji
const moodEmojiMap: Record<string, string> = {
  Happy: "😊",
  Sad: "😢",
  Anxious: "😰",
  Calm: "😌",
  Angry: "😠",
  Tired: "😴",
  Excited: "🎉",
};

// Mock数据生成函数
export const mockMoodData = {
  // 提交情绪日记 - 动态添加到列表
  createMoodLog: (params: CreateMoodLogParams) => {
    const newLog: MoodLogItem = {
      id: nextMockId++,
      logDate: params.logDate,
      moodType: params.moodType,
      moodScore: params.moodScore,
      content: params.content,
      emoji: moodEmojiMap[params.moodType] || "😊",
      tags: params.tags || [],
      aiAnalysis:
        "这是模拟的AI分析。检测到你的情绪状态，建议保持积极心态。记录情绪有助于更好地了解自己。",
    };

    // 添加到列表开头（最新的在前面）
    mockLogsStorage.unshift(newLog);

    return {
      code: 200,
      message: "success",
      data: {
        logId: newLog.id,
        aiAnalysis: newLog.aiAnalysis,
      },
    };
  },

  // 获取情绪日记列表 - 返回动态数据
  getMoodLogs: (limit = 10, offset = 0) => {
    const logs = mockLogsStorage.slice(offset, offset + limit);
    return {
      code: 200,
      message: "success",
      data: {
        total: mockLogsStorage.length,
        logs: logs,
      },
    };
  },

  // 获取单条日记详情 - 从动态列表中查找（严格对齐API文档）
  getMoodLogDetail: (id: number) => {
    const log = mockLogsStorage.find((item) => item.id === id);
    if (!log) {
      return {
        code: 404,
        message: "日记不存在",
        data: null,
      };
    }

    // API文档字段：id, userId, moodType, moodScore, content, tags, aiAnalysis, logDate, createTime
    return {
      code: 200,
      message: "success",
      data: {
        ...log,
        userId: 1001, // 演示用户ID
        aiAnalysis:
          log.moodScore >= 7
            ? "检测到积极情绪，建议保持良好心态，继续保持！"
            : log.moodScore >= 5
            ? "情绪较为平稳，可以尝试做一些喜欢的事情放松心情。"
            : "检测到消极情绪，建议进行适当调节，如运动、听音乐或与朋友交流。",
        createTime: log.logDate, // Mock环境下createTime和logDate相同
      },
    };
  },

  // 更新日记 - 修改动态列表中的数据
  updateMoodLog: (id: number, params: CreateMoodLogParams) => {
    const log = mockLogsStorage.find((item) => item.id === id);
    if (log) {
      log.moodType = params.moodType;
      log.moodScore = params.moodScore;
      log.content = params.content;
      log.tags = params.tags || [];
      log.logDate = params.logDate;
      log.emoji = moodEmojiMap[params.moodType] || log.emoji;

      return {
        code: 200,
        message: "更新成功",
        data: {
          logId: id,
          aiAnalysis: "日记已更新，继续保持记录的好习惯！",
        },
      };
    }
    return {
      code: 404,
      message: "日记不存在",
      data: null,
    };
  },

  // 删除日记 - 从动态列表中移除
  deleteMoodLog: (id: number) => {
    const index = mockLogsStorage.findIndex((item) => item.id === id);
    if (index > -1) {
      mockLogsStorage.splice(index, 1);
    }
    return {
      code: 200,
      message: "删除成功",
      data: null,
    };
  },

  // 获取情绪趋势（7天）- 严格对齐后端MoodTrendVO
  getMoodTrend: {
    code: 200,
    message: "success",
    data: {
      dates: [
        "2024-11-21",
        "2024-11-22",
        "2024-11-23",
        "2024-11-24",
        "2024-11-25",
        "2024-11-26",
        "2024-11-27",
      ],
      scores: [6, 7, 6, 7, 5, 7, 8], // 整数数组
      avgScore: 6.86,
      positiveRate: 0.71,
      continuousDays: 7,
    },
  },

  // 获取情绪统计 - 严格对齐后端MoodStatisticsVO
  getMoodStats: {
    code: 200,
    message: "success",
    data: {
      distribution: {
        Happy: "18",
        Calm: "12",
        Anxious: "6",
        Sad: "3",
        Tired: "2",
        Excited: "1",
      },
      totalLogs: 42,
      avgScore: 6.8,
    },
  },
};
