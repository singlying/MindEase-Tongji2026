// 前端A负责：情绪日记接口占位
import request from "./request";

export const getMoodListApi = () => request.get("/mood");

export const getMoodDetailApi = (id: string) => request.get(`/mood/${id}`);

export const createMoodApi = (payload: Record<string, unknown>) =>
  request.post("/mood", payload);
