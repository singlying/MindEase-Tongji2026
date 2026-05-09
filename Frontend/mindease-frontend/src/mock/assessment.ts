// 前端A负责：心理测评模块本地数据
import type {
  Scale,
  ScaleDetail,
  SubmitAssessmentParams,
  SubmitAssessmentResponse,
} from "@/api/assessment";

const commonOptions = [
  { label: "完全没有", score: 0 },
  { label: "有几天", score: 1 },
  { label: "一半以上时间", score: 2 },
  { label: "几乎每天", score: 3 },
];

const scales: Scale[] = [
  {
    id: 1,
    scaleKey: "phq-9",
    title: "PHQ-9 抑郁症筛查量表",
    description: "用于初步了解最近两周的低落、兴趣下降与精力状态。",
    coverColor: "#7b9e89",
    questionCount: 5,
    estimatedMinutes: 4,
    status: "active",
  },
  {
    id: 2,
    scaleKey: "gad-7",
    title: "GAD-7 焦虑症筛查量表",
    description: "用于初步评估紧张、担忧和难以放松等焦虑相关体验。",
    coverColor: "#c47c6b",
    questionCount: 5,
    estimatedMinutes: 4,
    status: "active",
  },
  {
    id: 3,
    scaleKey: "pss-10",
    title: "PSS-10 压力知觉量表",
    description: "帮助回顾近期压力感受，以及对生活事件的掌控感。",
    coverColor: "#6f8fb8",
    questionCount: 5,
    estimatedMinutes: 5,
    status: "active",
  },
];

const scaleDetails: Record<string, ScaleDetail> = {
  "phq-9": {
    id: 1,
    scaleKey: "phq-9",
    title: "PHQ-9 抑郁症筛查量表",
    description: "请根据最近两周的真实感受作答。",
    instruction: "每道题选择最符合自己的频率，结果仅用于自我了解。",
    questions: [
      { id: 101, text: "做事时提不起劲或没有兴趣", options: commonOptions },
      { id: 102, text: "感到心情低落、沮丧或绝望", options: commonOptions },
      { id: 103, text: "入睡困难、睡不安稳或睡眠过多", options: commonOptions },
      { id: 104, text: "感觉疲倦或没有活力", options: commonOptions },
      { id: 105, text: "对自己感到失望，或觉得自己很失败", options: commonOptions },
    ],
  },
  "gad-7": {
    id: 2,
    scaleKey: "gad-7",
    title: "GAD-7 焦虑症筛查量表",
    description: "请根据最近两周的紧张、担忧与身体反应作答。",
    instruction: "不需要反复斟酌，选择最接近第一感受的选项即可。",
    questions: [
      { id: 201, text: "感觉紧张、焦虑或急切", options: commonOptions },
      { id: 202, text: "不能停止或控制担忧", options: commonOptions },
      { id: 203, text: "对各种各样的事情担忧过多", options: commonOptions },
      { id: 204, text: "很难放松下来", options: commonOptions },
      { id: 205, text: "变得容易烦躁或急躁", options: commonOptions },
    ],
  },
  "pss-10": {
    id: 3,
    scaleKey: "pss-10",
    title: "PSS-10 压力知觉量表",
    description: "请回顾最近一个月对压力事件的主观感受。",
    instruction: "本量表关注压力知觉，不代表临床诊断。",
    questions: [
      { id: 301, text: "觉得生活中重要的事情难以掌控", options: commonOptions },
      { id: 302, text: "因为意外事件而感到烦恼", options: commonOptions },
      { id: 303, text: "觉得自己无法处理必须完成的事情", options: commonOptions },
      { id: 304, text: "觉得困难堆积到无法克服", options: commonOptions },
      { id: 305, text: "感到自己能够有效处理生活问题", options: commonOptions },
    ],
  },
};

function wait<T>(data: T, delay = 240) {
  return new Promise<{ data: T }>((resolve) => {
    window.setTimeout(() => resolve({ data }), delay);
  });
}

function getResult(totalScore: number): Omit<SubmitAssessmentResponse, "recordId" | "totalScore"> {
  if (totalScore <= 4) {
    return {
      resultLevel: "平稳",
      resultDesc: "当前状态整体较为平稳，可以继续保持规律作息和适度运动。",
    };
  }

  if (totalScore <= 9) {
    return {
      resultLevel: "轻度波动",
      resultDesc: "近期可能存在一些情绪或压力波动，建议持续观察并增加放松活动。",
    };
  }

  return {
    resultLevel: "需要关注",
    resultDesc: "近期压力或情绪困扰较明显，建议寻求专业支持或与可信任的人沟通。",
  };
}

export const assessmentMockService = {
  getScaleList() {
    return wait({ scales });
  },

  getScaleDetail(scaleKey: string) {
    const detail = scaleDetails[scaleKey] ?? scaleDetails["gad-7"];
    return wait(detail);
  },

  submitAssessment(payload: SubmitAssessmentParams) {
    const totalScore = payload.answers.reduce((sum, item) => sum + item.score, 0);
    const result = getResult(totalScore);

    return wait<SubmitAssessmentResponse>(
      {
        recordId: Date.now(),
        totalScore,
        ...result,
      },
      360,
    );
  },
};
