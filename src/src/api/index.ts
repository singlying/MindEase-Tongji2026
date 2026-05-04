import { api, createChatStream } from "./client";

const recentRange = () => {
  const end = new Date();
  const start = new Date(end);
  start.setDate(end.getDate() - 7);
  return {
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10)
  };
};

export interface UserInfo {
  userId: number;
  username: string;
  nickname: string;
  avatar?: string | null;
  role: "user" | "counselor" | "admin";
  status: number;
  createTime?: string;
}

export interface LoginResponse {
  userId: number;
  username: string;
  nickname?: string;
  role: "user" | "counselor" | "admin";
  token: string;
}

export interface RegisterParams {
  username: string;
  password: string;
  nickname: string;
  phone: string;
  role: "user" | "counselor" | "admin";
  invitationCode?: string;
}

export const authApi = {
  login: (data: { username: string; password: string }) => api.post<LoginResponse>("/auth/login", data),
  register: (data: RegisterParams) => api.post<{ userId: number; role: string; status: number; token?: string }>("/auth/register", data),
  profile: () => api.get<UserInfo>("/auth/profile"),
  updateProfile: (data: { nickname?: string; avatar?: string }) => api.put<{ success: boolean }>("/auth/profile", data)
};

export const moodApi = {
  create: (data: { moodType: string; moodScore: number; content: string; tags: string[]; logDate: string }) => api.post<{ logId: number; aiAnalysis?: string }>("/mood/log", data),
  list: (params = { limit: 10, offset: 0 }) => api.get<{ total: number; logs: any[] }>("/mood/logs", { params }),
  detail: (id: number | string) => api.get<any>(`/mood/log/${id}`),
  update: (id: number | string, data: unknown) => api.put<any>(`/mood/log/${id}`, data),
  remove: (id: number | string) => api.delete<null>(`/mood/log/${id}`),
  trend: (days = 7) => api.get<any>("/mood/trend", { params: { days } }),
  statistics: () => api.get<any>("/mood/statistics")
};

export const chatApi = {
  createSession: () => api.post<{ sessionId: string }>("/chat/session"),
  sessions: (limit = 20) => api.get<{ total: number; sessions: any[] }>("/chat/sessions", { params: { limit } }),
  history: (sessionId: string, limit = 50) => api.get<{ sessionId: string; messages: any[] }>(`/chat/history/${sessionId}`, { params: { limit } }),
  remove: (sessionId: string) => api.delete<{ success: boolean }>(`/chat/session/${sessionId}`),
  checkSensitiveWords: (data: { sessionId: string; content: string }) => api.post<any>("/chat/check-sensitive-words", data),
  stream: createChatStream
};

export const assessmentApi = {
  scales: () => api.get<any>("/assessment/scales"),
  scale: (scaleKey: string) => api.get<any>(`/assessment/scale/${scaleKey}`),
  submit: (data: unknown) => api.post<any>("/assessment/submit", data),
  records: (params = { limit: 10, offset: 0 }) => api.get<any>("/assessment/records", { params }),
  record: (id: number | string) => api.get<any>(`/assessment/record/${id}`)
};

export const counselorApi = {
  recommendStatus: () => api.get<any>("/counselor/recommend/status"),
  recommend: (params?: { keyword?: string; sort?: "smart" | "price_asc" | "rating_desc" }) => api.get<any>("/counselor/recommend", { params }),
  detail: (id: number | string) => api.get<any>(`/counselor/${id}`),
  reviews: (id: number | string, limit = 10, offset = 0) => api.get<any>(`/counselor/${id}/reviews`, { params: { limit, offset } }),
  submitReview: (data: unknown) => api.post<any>("/counselor/review", data),
  submitAudit: (data: unknown) => api.post<any>("/counselor/audit/submit", data),
  auditStatus: () => api.get<any>("/counselor/audit/status")
};

export const appointmentApi = {
  slots: (counselorId: number | string, date: string) => api.get<any>("/appointment/available-slots", { params: { counselorId, date } }),
  create: (data: unknown) => api.post<any>("/appointment/create", data),
  mine: (params?: { status?: string; page?: number; pageSize?: number }) => api.get<any>("/appointment/my-appointments", { params }),
  detail: (id: number | string) => api.get<any>(`/appointment/${id}`),
  cancel: (id: number | string, cancelReason: string) => api.put<any>(`/appointment/${id}/cancel`, { cancelReason }),
  confirm: (id: number | string) => api.put<any>(`/appointment/${id}/confirm`),
  schedule: (data: unknown) => api.post<any>("/appointment/schedule", data)
};

export const reportApi = {
  emotion: (params?: { startDate?: string; endDate?: string }) => api.get<any>("/report/emotion", { params: params || recentRange() })
};

export const userApi = {
  dashboard: () => api.get<any>("/user/dashboard"),
  notifications: (limit = 10) => api.get<any>("/user/notifications", { params: { limit } }),
  readNotification: (id: number | string) => api.put<any>(`/user/notification/${id}/read`),
  readAllNotifications: () => api.put<any>("/user/notifications/read-all")
};

export const adminApi = {
  auditList: (params?: unknown) => api.get<any>("/admin/audit/list", { params }),
  processAudit: (data: unknown) => api.post<any>("/admin/audit/process", data),
  createScale: (data: unknown) => api.post<any>("/admin/assessment/scale", data),
  createQuestion: (data: unknown) => api.post<any>("/admin/assessment/question", data),
  scale: (scaleKey: string) => api.get<any>(`/assessment/scale/${scaleKey}`)
};
