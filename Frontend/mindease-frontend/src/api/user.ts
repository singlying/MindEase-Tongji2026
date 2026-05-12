// 前端A负责：用户中心接口占位
import request from "./request";

export const getUserInfoApi = () => request.get("/user/profile");

export const updateUserInfoApi = (payload: Record<string, unknown>) =>
  request.put("/user/profile", payload);
