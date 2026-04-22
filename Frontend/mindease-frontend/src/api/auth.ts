// 前端A负责：登录、注册、用户资料认证接口占位
import request from "./request";

export const loginApi = (payload: Record<string, unknown>) =>
  request.post("/auth/login", payload);

export const registerApi = (payload: Record<string, unknown>) =>
  request.post("/auth/register", payload);

export const getUserProfileApi = () => request.get("/auth/profile");
