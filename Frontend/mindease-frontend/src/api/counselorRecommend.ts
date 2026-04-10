// 咨询师推荐相关API（用户端使用）
// 包含：咨询师推荐、详情、评价等普通用户需要使用的接口
import request from "./request";
import type { ApiResponse } from "./request";
import { mockCounselorRecommendData } from "@/mock/counselorRecommend";

// 读取Mock开关
const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// ============ 类型定义 ============

/**
 * 推荐状态
 */
export interface RecommendStatus {
  hasAssessment: boolean;
  hasMoodLog: boolean;
  lastAssessmentLevel: string | null;
  recommendationReady: boolean;
}

/**
 * 推荐上下文
 */
export interface RecommendContext {
  strategy: string;
  basedOn: string;
  userTags: string[];
}

/**
 * 咨询师推荐卡片信息
 */
export interface CounselorRecommend {
  id: number;
  realName: string;
  avatar: string | null;
  title: string;
  experienceYears: number;
  specialty: string[];
  rating: number;
  pricePerHour: number;
  location: string | null;
  nextAvailableTime: string | null;
  matchReason: string | null;
  tags: string[];
}

/**
 * 咨询师推荐结果
 */
export interface RecommendResult {
  recommendContext: RecommendContext | null;
  counselors: CounselorRecommend[];
}

/**
 * 咨询师详情
 */
export interface CounselorDetail {
  id: number;
  realName: string;
  avatar: string | null;
  title: string;
  experienceYears: number;
  specialty: string[];
  bio: string | null;
  qualificationUrl: string | null;
  rating: number;
  reviewCount: number;
  pricePerHour: number;
  location: string | null;
  isOnline: boolean;
  tags: string[];
}

/**
 * 咨询师评价
 */
export interface CounselorReview {
  id: number;
  userId: number;
  nickname: string;
  avatar: string | null;
  rating: number;
  content: string;
  createTime: string;
}

/**
 * 评价列表
 */
export interface ReviewList {
  total: number;
  avgRating: number;
  reviews: CounselorReview[];
}

/**
 * 提交评价参数
 */
export interface ReviewSubmitParams {
  counselorId: number;
  appointmentId: number;
  rating: number;
  content: string;
}

// ============ API函数 ============

/**
 * 获取推荐状态（检查是否满足推荐条件）
 * GET /counselor/recommend/status
 */
export const getRecommendStatus = () => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: "success",
          data: mockCounselorRecommendData.getRecommendStatus(),
        });
      }, 300);
    });
  }
  return request.get<ApiResponse<RecommendStatus>>(
    "/counselor/recommend/status"
  );
};

/**
 * 获取推荐咨询师列表
 * GET /counselor/recommend
 * @param keyword 搜索关键词（后端仅保留此入口，模糊匹配姓名/职称/简介/地区/专长）
 * @param sort 排序方式: smart, price_asc, rating_desc
 */
export const getRecommendCounselors = (params?: {
  keyword?: string;
  sort?: "smart" | "price_asc" | "rating_desc";
}) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: "success",
          data: mockCounselorRecommendData.getRecommendCounselors(params),
        });
      }, 500);
    });
  }
  return request.get<ApiResponse<RecommendResult>>("/counselor/recommend", {
    params,
  });
};

/**
 * 获取咨询师详情
 * GET /counselor/{id}
 * @param id 咨询师ID
 */
export const getCounselorDetail = (id: number) => {
  if (USE_MOCK) {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const data = mockCounselorRecommendData.getCounselorDetail(id);
        if (data) {
          resolve({
            code: 200,
            message: "success",
            data,
          });
        } else {
          reject(new Error("咨询师不存在"));
        }
      }, 300);
    });
  }
  return request.get<ApiResponse<CounselorDetail>>(`/counselor/${id}`);
};

/**
 * 获取咨询师评价列表
 * GET /counselor/{id}/reviews
 * @param id 咨询师ID
 * @param limit 每页数量
 * @param offset 偏移量
 */
export const getCounselorReviews = (
  id: number,
  limit: number = 10,
  offset: number = 0
) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: "success",
          data: mockCounselorRecommendData.getCounselorReviews(
            id,
            limit,
            offset
          ),
        });
      }, 300);
    });
  }
  return request.get<ApiResponse<ReviewList>>(`/counselor/${id}/reviews`, {
    params: { limit, offset },
  });
};

/**
 * 提交评价
 * POST /counselor/review
 */
export const submitReview = (data: ReviewSubmitParams) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: "评价提交成功",
          data: mockCounselorRecommendData.submitReview(data),
        });
      }, 400);
    });
  }
  return request.post<ApiResponse<{ reviewId: number }>>(
    "/counselor/review",
    data
  );
};
