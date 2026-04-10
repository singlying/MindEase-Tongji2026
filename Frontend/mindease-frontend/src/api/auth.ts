// 用户认证相关 API
import request from "./request";
import type { ApiResponse } from "./request";

// 登录请求参数
export interface LoginParams {
  username: string;
  password: string;
}

// 注册请求参数
export interface RegisterParams {
  username: string;
  password: string;
  nickname: string;
  phone: string;
  role: "user" | "counselor" | "admin";
  invitationCode?: string; // admin注册需要邀请码
}

// 用户信息（严格对齐后端UserProfileVO）
export interface UserInfo {
  userId: number;
  username: string;
  nickname: string;
  avatar: string;
  role: "user" | "counselor" | "admin";
  status: number; // 1=启用, 2=待审核, 0=禁用
  createTime?: string;
}

// 登录响应（严格对齐后端UserLoginVO）
export interface LoginResponse {
  userId: number;
  username: string;
  nickname?: string;
  role: "user" | "counselor" | "admin";
  token: string;
}

// 用户登录
export const login = (data: LoginParams) => {
  return request.post<ApiResponse<LoginResponse>>("/auth/login", data);
};

// 注册响应
export interface RegisterResponse {
  userId: number;
  role: "user" | "counselor" | "admin";
  status: number;
  token?: string; // 咨询师注册可能不返回token
}

// 用户注册
export const register = (data: RegisterParams) => {
  return request.post<ApiResponse<RegisterResponse>>("/auth/register", data);
};

// 获取用户信息
export const getUserProfile = () => {
  return request.get<ApiResponse<UserInfo>>("/auth/profile");
};

// 更新用户信息
export const updateUserProfile = (data: {
  nickname?: string;
  avatar?: string;
}) => {
  return request.put<ApiResponse<{ success: boolean }>>("/auth/profile", data);
};
