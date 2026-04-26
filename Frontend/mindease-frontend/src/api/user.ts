// 用户中心相关 API
import request from "./request";
import type { ApiResponse } from "./request";
import { mockUserData } from "@/mock/user";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// Dashboard 数据
export interface DashboardData {
  moodSummary: {
    avgScore: number; // 心情均分
    continuousDays: number; // 连续记录天数
  };
  upcomingAppointments: Array<{
    id: number;
    time: string;
    counselor: string;
  }>;
  unreadNotifications: number; // 未读通知数量
}

// 通知项
export interface Notification {
  id: number;
  type: "system" | "appointment" | "report"; // 通知类型
  title: string;
  content: string;
  isRead: boolean;
  createTime: string;
}

// 通知列表响应
export interface NotificationListResponse {
  notifications: Notification[];
}

// 获取 Dashboard 数据
export const getDashboard = () => {
  if (USE_MOCK) {
    return mockUserData.getDashboard().then((data) => ({
      code: 200,
      message: "success",
      data,
    })) as any;
  }
  return request.get<ApiResponse<DashboardData>>("/user/dashboard");
};

// 获取通知列表
export const getNotifications = (limit: number = 20) => {
  if (USE_MOCK) {
    return mockUserData.getNotifications(limit).then((data) => ({
      code: 200,
      message: "success",
      data,
    })) as any;
  }
  return request.get<ApiResponse<NotificationListResponse>>(
    "/user/notifications",
    { params: { limit } }
  );
};

// 标记通知已读
export const markNotificationRead = (id: number) => {
  if (USE_MOCK) {
    return mockUserData.markNotificationRead(id).then((data) => ({
      code: 200,
      message: "success",
      data,
    })) as any;
  }
  return request.put<ApiResponse<{ success: boolean }>>(
    `/user/notification/${id}/read`
  );
};

// 标记全部通知已读
export const markAllNotificationsRead = () => {
  if (USE_MOCK) {
    return mockUserData.markAllNotificationsRead().then((data) => ({
      code: 200,
      message: "success",
      data,
    })) as any;
  }
  return request.put<ApiResponse<{ success: boolean }>>(
    "/user/notifications/read-all"
  );
};
