// 前端A负责：咨询师推荐接口占位
import request from "./request";

export const getCounselorRecommendApi = () =>
  request.get("/counselor/recommend");
