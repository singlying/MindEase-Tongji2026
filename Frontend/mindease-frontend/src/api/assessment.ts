import request from "./request";
import type { ApiResponse } from "./request";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// 量表信息（严格对齐后端ScaleListVO.ScaleItem）
export interface Scale {
  id: number;
  scaleKey: string;
  title: string;
  coverUrl: string;
  description: string;
  status: string;
}

// 量表列表响应
export interface ScaleListVO {
  scales: Scale[];
}

// 题目选项
export interface QuestionOption {
  label: string;
  score: number;
}

// 题目
export interface Question {
  id: number;
  text: string;
  options: QuestionOption[];
}

// 量表详情（严格对齐后端ScaleDetailVO）
export interface ScaleDetailVO {
  id: number;
  scaleKey: string;
  title: string;
  description: string;
  questions: Question[];
}

// 提交答案
export interface AssessmentSubmitDTO {
  scaleKey: string;
  answers: {
    questionId: number;
    score: number;
  }[];
}

// 提交结果（严格对齐后端AssessmentSubmitVO）
export interface AssessmentSubmitVO {
  recordId: number;
  totalScore: number;
  resultLevel: string;
  resultDesc: string;
}

// 测评记录（严格对齐后端AssessmentRecordListVO.RecordItem）
export interface AssessmentRecord {
  id: number;
  scaleKey: string;
  scaleTitle: string;
  totalScore: number;
  resultLevel: string;
  createTime: string;
}

// 测评记录列表（严格对齐后端AssessmentRecordListVO）
export interface AssessmentRecordListVO {
  records: AssessmentRecord[];
}

// 测评记录详情（严格对齐后端AssessmentRecordDetailVO）
export interface AssessmentRecordDetailVO {
  id: number;
  scaleKey: string;
  scaleTitle: string;
  totalScore: number;
  resultLevel: string;
  resultDesc: string;
  answersDetail: {
    questionId: number;
    questionText: string;
    score: number;
    answerText: string;
  }[];
  createTime: string;
}

// ==================== Mock 数据 ====================
let mockAssessmentData: any = null;

if (USE_MOCK) {
  mockAssessmentData = {
    // 量表列表
    getScaleList: (): Promise<ScaleListVO> => {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({
            scales: [
              {
                id: 1,
                scaleKey: "phq-9",
                title: "PHQ-9 抑郁症筛查量表",
                coverUrl:
                  "https://images.unsplash.com/photo-1544027993-37dbfe43562a?w=400",
                description: "用于评估抑郁症状的严重程度，共9个问题",
                status: "active",
              },
              {
                id: 2,
                scaleKey: "gad-7",
                title: "GAD-7 焦虑症筛查量表",
                coverUrl:
                  "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400",
                description: "用于评估焦虑症状的严重程度，共7个问题",
                status: "active",
              },
              {
                id: 3,
                scaleKey: "sas",
                title: "SAS 焦虑自评量表",
                coverUrl:
                  "https://images.unsplash.com/photo-1499209974431-9dddcece7f88?w=400",
                description: "用于评估焦虑水平的标准化量表，共20个问题",
                status: "active",
              },
            ],
          });
        }, 300);
      });
    },

    // 获取量表详情
    getScaleDetail: (scaleKey: string): Promise<ScaleDetailVO> => {
      return new Promise((resolve) => {
        setTimeout(() => {
          if (scaleKey === "phq-9") {
            resolve({
              id: 1,
              scaleKey: "phq-9",
              title: "PHQ-9 抑郁症筛查量表",
              description:
                "患者健康问卷-9（PHQ-9）是一个简短的自评量表，用于筛查抑郁症。在过去两周内，你有多频繁地受到以下问题的困扰？",
              questions: [
                {
                  id: 1,
                  text: "做事时提不起劲或没有兴趣",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
                {
                  id: 2,
                  text: "感到心情低落、沮丧或绝望",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
                {
                  id: 3,
                  text: "入睡困难、睡不安稳或睡眠过多",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
                {
                  id: 4,
                  text: "感觉疲倦或没有活力",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
                {
                  id: 5,
                  text: "食欲不振或吃太多",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
              ],
            });
          } else {
            resolve({
              id: 2,
              scaleKey: "gad-7",
              title: "GAD-7 焦虑症筛查量表",
              description:
                "广泛性焦虑量表-7（GAD-7）用于筛查焦虑症。在过去两周内，你有多频繁地受到以下问题的困扰？",
              questions: [
                {
                  id: 1,
                  text: "感觉紧张、焦虑或急切",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
                {
                  id: 2,
                  text: "不能停止或控制担忧",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
                {
                  id: 3,
                  text: "对各种各样的事情担忧过多",
                  options: [
                    { label: "完全不会", score: 0 },
                    { label: "好几天", score: 1 },
                    { label: "一半以上的天数", score: 2 },
                    { label: "几乎每天", score: 3 },
                  ],
                },
              ],
            });
          }
        }, 300);
      });
    },

    // 提交测评
    submitAssessment: (
      data: AssessmentSubmitDTO
    ): Promise<AssessmentSubmitVO> => {
      return new Promise((resolve) => {
        setTimeout(() => {
          const totalScore = data.answers.reduce(
            (sum, answer) => sum + answer.score,
            0
          );
          let resultLevel = "正常";
          let resultDesc = "";

          if (data.scaleKey === "phq-9") {
            if (totalScore <= 4) {
              resultLevel = "正常";
              resultDesc =
                "您的抑郁症状在正常范围内，无需过度担心。建议：保持良好的生活习惯，适当运动和社交。";
            } else if (totalScore <= 9) {
              resultLevel = "轻度";
              resultDesc =
                "您可能存在轻度抑郁症状，建议关注自己的情绪变化。建议：增加户外活动，与朋友家人多交流，保持规律作息。";
            } else if (totalScore <= 14) {
              resultLevel = "中度";
              resultDesc =
                "您可能存在中度抑郁症状，建议寻求专业帮助。建议：考虑咨询专业心理医生，保持社交活动，避免独处过久。";
            } else {
              resultLevel = "重度";
              resultDesc =
                "您可能存在较严重的抑郁症状，强烈建议尽快寻求专业帮助。建议：立即联系心理医生，告知家人或朋友，避免做重大决定。";
            }
          } else {
            if (totalScore <= 4) {
              resultLevel = "正常";
              resultDesc =
                "您的焦虑水平在正常范围内。建议：保持现有的生活方式。";
            } else if (totalScore <= 9) {
              resultLevel = "轻度";
              resultDesc = "您可能存在轻度焦虑。建议：尝试放松技巧，规律运动。";
            } else {
              resultLevel = "中度";
              resultDesc =
                "您可能存在中度焦虑，建议寻求帮助。建议：咨询心理医生，学习压力管理技巧。";
            }
          }

          resolve({
            recordId: Date.now(),
            totalScore,
            resultLevel,
            resultDesc,
          });
        }, 500);
      });
    },

    // 获取测评历史
    getRecordList: (limit: number): Promise<AssessmentRecordListVO> => {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({
            records: [
              {
                id: 1001,
                scaleKey: "phq-9",
                scaleTitle: "PHQ-9 抑郁症筛查量表",
                totalScore: 8,
                resultLevel: "轻度",
                createTime: "2024-12-04 14:30:00",
              },
              {
                id: 1002,
                scaleKey: "gad-7",
                scaleTitle: "GAD-7 焦虑症筛查量表",
                totalScore: 5,
                resultLevel: "轻度",
                createTime: "2024-12-03 10:15:00",
              },
              {
                id: 1003,
                scaleKey: "phq-9",
                scaleTitle: "PHQ-9 抑郁症筛查量表",
                totalScore: 12,
                resultLevel: "中度",
                createTime: "2024-11-28 16:45:00",
              },
            ],
          });
        }, 300);
      });
    },

    // 获取记录详情
    getRecordDetail: (id: number): Promise<AssessmentRecordDetailVO> => {
      return new Promise((resolve) => {
        setTimeout(() => {
          resolve({
            id,
            scaleKey: "phq-9",
            scaleTitle: "PHQ-9 抑郁症筛查量表",
            totalScore: 8,
            resultLevel: "轻度",
            resultDesc:
              "您可能存在轻度抑郁症状，建议关注自己的情绪变化。建议：增加户外活动，与朋友家人多交流，保持规律作息。",
            answersDetail: [
              {
                questionId: 1,
                questionText: "做事时提不起劲或没有兴趣",
                score: 1,
                answerText: "好几天",
              },
              {
                questionId: 2,
                questionText: "感到心情低落、沮丧或绝望",
                score: 2,
                answerText: "一半以上的天数",
              },
            ],
            createTime: "2024-12-04 14:30:00",
          });
        }, 300);
      });
    },
  };
}

// ==================== API 接口 ====================

// 获取量表列表
export const getScaleList = () => {
  if (USE_MOCK) {
    return mockAssessmentData.getScaleList().then((data: ScaleListVO) => ({
      code: 200,
      message: "success",
      data,
    })) as any;
  }
  return request.get<ApiResponse<ScaleListVO>>("/assessment/scales");
};

// 获取量表详情
export const getScaleDetail = (scaleKey: string) => {
  if (USE_MOCK) {
    return mockAssessmentData
      .getScaleDetail(scaleKey)
      .then((data: ScaleDetailVO) => ({
        code: 200,
        message: "success",
        data,
      })) as any;
  }
  return request.get<ApiResponse<ScaleDetailVO>>(
    `/assessment/scale/${scaleKey}`
  );
};

// 提交测评
export const submitAssessment = (data: AssessmentSubmitDTO) => {
  if (USE_MOCK) {
    return mockAssessmentData
      .submitAssessment(data)
      .then((result: AssessmentSubmitVO) => ({
        code: 200,
        message: "success",
        data: result,
      })) as any;
  }
  return request.post<ApiResponse<AssessmentSubmitVO>>(
    "/assessment/submit",
    data
  );
};

// 获取测评历史列表
export const getRecordList = (limit: number = 10) => {
  if (USE_MOCK) {
    return mockAssessmentData
      .getRecordList(limit)
      .then((data: AssessmentRecordListVO) => ({
        code: 200,
        message: "success",
        data,
      })) as any;
  }
  return request.get<ApiResponse<AssessmentRecordListVO>>(
    "/assessment/records",
    {
      params: { limit },
    }
  );
};

// 获取测评记录详情
export const getRecordDetail = (id: number) => {
  if (USE_MOCK) {
    return mockAssessmentData
      .getRecordDetail(id)
      .then((data: AssessmentRecordDetailVO) => ({
        code: 200,
        message: "success",
        data,
      })) as any;
  }
  return request.get<ApiResponse<AssessmentRecordDetailVO>>(
    `/assessment/record/${id}`
  );
};
