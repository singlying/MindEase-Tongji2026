import axios, { type AxiosRequestConfig } from "axios";

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 15000,
  headers: {
    "Content-Type": "application/json;charset=utf-8"
  }
});

const demoUser = {
  userId: 1001,
  username: "demo_user",
  nickname: "林同学",
  avatar: "",
  role: "user",
  status: 1,
  createTime: "2026-05-01 09:30:00"
};

const moodLogs = [
  { id: 9101, moodType: "Calm", moodScore: 7, content: "下午完成了一次深呼吸练习，注意力比上午更稳定。", tags: ["冥想", "学习"], logDate: "2026-05-17 15:20:00" },
  { id: 9102, moodType: "Anxious", moodScore: 4, content: "临近汇报有一点紧张，和朋友散步后缓解了不少。", tags: ["汇报", "社交"], logDate: "2026-05-16 21:10:00" },
  { id: 9103, moodType: "Happy", moodScore: 8, content: "今天按计划完成了运动，睡眠也恢复到比较舒服的节奏。", tags: ["运动", "睡眠"], logDate: "2026-05-15 22:05:00" }
];

const counselors = [
  { id: 301, realName: "陈予安", avatar: null, title: "国家二级心理咨询师", experienceYears: 9, specialty: ["焦虑调节", "学业压力"], rating: 4.9, pricePerHour: 320, location: "上海", nextAvailableTime: "2026-05-18 14:00", matchReason: "擅长压力管理与情绪稳定训练", tags: ["CBT", "正念", "青少年"] },
  { id: 302, realName: "周知微", avatar: null, title: "高校心理中心督导师", experienceYears: 12, specialty: ["亲密关系", "自我探索"], rating: 4.8, pricePerHour: 380, location: "杭州", nextAvailableTime: "2026-05-19 10:00", matchReason: "适合需要持续性支持的来访者", tags: ["关系议题", "长期陪伴"] },
  { id: 303, realName: "许清和", avatar: null, title: "临床心理方向咨询师", experienceYears: 7, specialty: ["睡眠困扰", "情绪低落"], rating: 4.7, pricePerHour: 300, location: "线上", nextAvailableTime: "2026-05-18 19:30", matchReason: "夜间咨询时段更灵活", tags: ["睡眠", "情绪评估"] }
];

const appointments = [
  { id: 7001, startTime: "2026-05-18 14:00:00", endTime: "2026-05-18 15:00:00", status: "confirmed", targetName: "陈予安", targetAvatar: null, targetRole: "counselor" },
  { id: 7002, startTime: "2026-05-21 19:30:00", endTime: "2026-05-21 20:30:00", status: "pending", targetName: "许清和", targetAvatar: null, targetRole: "counselor" }
];

const isDemo = () => localStorage.getItem("token")?.startsWith("demo_token_") || import.meta.env.VITE_USE_MOCK === "true";
const ok = <T>(data: T, message = "success"): ApiResponse<T> => ({ code: 200, message, data });

let activeRequests = 0;
const emitRequestState = () => {
  window.dispatchEvent(new CustomEvent("mindease:loading", { detail: { loading: activeRequests > 0 } }));
};
const withRequest = async <T>(task: () => Promise<T>) => {
  activeRequests += 1;
  emitRequestState();
  try {
    return await task();
  } finally {
    activeRequests = Math.max(0, activeRequests - 1);
    emitRequestState();
  }
};

const demoResponse = (method: string, url: string, body?: unknown): ApiResponse<unknown> | null => {
  if (!isDemo()) return null;
  if (url === "/auth/profile") return ok(demoUser);
  if (url === "/user/dashboard") return ok({ moodAvg: 7.1, assessmentCount: 4, appointmentCount: 2 });
  if (url === "/user/notifications") return ok({ total: 3, notifications: [{ id: 1, title: "晚间呼吸练习", content: "今晚 21:30 有一段 5 分钟训练", read: false }, { id: 2, title: "咨询提醒", content: "明天下午有一场预约咨询", read: false }, { id: 3, title: "测评建议", content: "可以复测 GAD-7 观察近期变化", read: true }] });
  if (url === "/mood/trend") return ok({ dates: ["05/11", "05/12", "05/13", "05/14", "05/15", "05/16", "05/17"], scores: [5, 6, 6, 7, 8, 4, 7], avgScore: 6.1, positiveRate: 0.71, continuousDays: 5 });
  if (url === "/mood/statistics") return ok({ totalLogs: 18, averageScore: 6.8, dominantMood: "Calm" });
  if (url === "/mood/logs") return ok({ total: moodLogs.length, logs: moodLogs });
  if (url === "/mood/log" && method === "post") return ok({ logId: 9999, aiAnalysis: "已记录。建议今晚安排一次轻量放松练习。" }, "记录成功");
  if (url.startsWith("/mood/log/")) return ok(moodLogs[0]);
  if (url === "/chat/session") return ok({ sessionId: "demo-session-1" });
  if (url === "/chat/sessions") return ok({ total: 2, sessions: [{ sessionId: "demo-session-1", sessionTitle: "睡前放松", createTime: "2026-05-17T20:00:00" }, { sessionId: "demo-session-2", sessionTitle: "汇报前紧张", createTime: "2026-05-16T19:00:00" }] });
  if (url.startsWith("/chat/history/")) return ok({ sessionId: "demo-session-1", messages: [{ sender: "ai", content: "晚上好，今天最占据你注意力的事情是什么？" }, { sender: "user", content: "明天要汇报，身体有点紧。" }, { sender: "ai", content: "我们可以先做一次 4-6 呼吸，把身体从警觉状态里慢慢带回来。" }] });
  if (url === "/chat/check-sensitive-words") return ok({ containsSensitiveWord: false, sensitiveWords: [], originalText: (body as any)?.content || "" });
  if (url === "/assessment/scales") return ok({ scales: [{ scaleKey: "PHQ9", title: "PHQ-9 抑郁筛查", description: "9 个问题，帮助观察近期情绪低落程度" }, { scaleKey: "GAD7", title: "GAD-7 焦虑筛查", description: "7 个问题，评估焦虑频率与影响" }, { scaleKey: "PSS10", title: "PSS-10 压力量表", description: "了解近期主观压力水平" }] });
  if (url.startsWith("/assessment/scale/")) return ok({ scaleKey: url.split("/").pop(), questions: [] });
  if (url === "/assessment/records") return ok({ total: 2, records: [{ recordId: 1, scaleName: "GAD-7", resultLevel: "轻度焦虑", createTime: "2026-05-12" }, { recordId: 2, scaleName: "PHQ-9", resultLevel: "正常范围", createTime: "2026-05-08" }] });
  if (url === "/counselor/recommend/status") return ok({ hasAssessment: true, hasMoodLog: true, lastAssessmentLevel: "轻度焦虑", recommendationReady: true });
  if (url === "/counselor/recommend") return ok({ recommendContext: { strategy: "综合匹配", basedOn: "测评与近期日记", userTags: ["压力", "睡眠"] }, counselors });
  if (url.match(/^\/counselor\/\d+$/)) return ok(counselors[0]);
  if (url.includes("/reviews")) return ok({ total: 2, avgRating: 4.9, reviews: [] });
  if (url === "/appointment/available-slots") return ok({ date: "2026-05-18", slots: [{ startTime: "2026-05-18 14:00:00", endTime: "2026-05-18 15:00:00", available: true }, { startTime: "2026-05-18 16:00:00", endTime: "2026-05-18 17:00:00", available: true }, { startTime: "2026-05-18 19:30:00", endTime: "2026-05-18 20:30:00", available: false }] });
  if (url === "/appointment/my-appointments") return ok({ total: appointments.length, list: appointments });
  if (url === "/appointment/create") return ok({ appointmentId: 8001, status: "pending" }, "预约已提交");
  if (url.includes("/cancel")) return ok({ success: true }, "已取消");
  if (url.includes("/confirm")) return ok({ success: true }, "已确认");
  if (url === "/appointment/schedule") return ok({ success: true }, "排班已提交");
  if (url === "/report/emotion") return ok({ dominantMood: "Calm", volatility: "中低", totalLogs: 18, summary: "近一周情绪整体稳定，压力峰值集中在汇报前后。建议保留睡前呼吸训练，并在高压日提前安排短时运动。" });
  if (url === "/counselor/audit/status") return ok({ status: 2, message: "资料已进入审核流程" });
  if (url === "/counselor/audit/submit") return ok({ success: true }, "资质已提交");
  if (url === "/admin/audit/list") return ok({ total: 2, list: [{ userId: 201, realName: "梁静", title: "心理咨询师", status: 2 }, { userId: 202, realName: "方远", title: "高校心理老师", status: 2 }] });
  if (url === "/admin/audit/process") return ok({ success: true }, "处理完成");
  if (url.startsWith("/admin/assessment/")) return ok({ success: true }, "保存成功");
  return ok({});
};

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.token = token;
  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse | undefined;
    if (payload && typeof payload.code === "number" && payload.code !== 200) {
      if (payload.code === 401) {
        localStorage.removeItem("token");
        if (window.location.pathname !== "/login") window.location.href = "/login";
      }
      return Promise.reject(new Error(payload.message || "请求失败"));
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      if (window.location.pathname !== "/login") window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export const api = {
  async get<T>(url: string, config?: AxiosRequestConfig) {
    return withRequest(async () => {
      const demo = demoResponse("get", url);
      if (demo) return demo as ApiResponse<T>;
      return (await http.get<ApiResponse<T>>(url, config)).data;
    });
  },
  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return withRequest(async () => {
      const demo = demoResponse("post", url, data);
      if (demo) return demo as ApiResponse<T>;
      return (await http.post<ApiResponse<T>>(url, data, config)).data;
    });
  },
  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return withRequest(async () => {
      const demo = demoResponse("put", url, data);
      if (demo) return demo as ApiResponse<T>;
      return (await http.put<ApiResponse<T>>(url, data, config)).data;
    });
  },
  async delete<T>(url: string, config?: AxiosRequestConfig) {
    return withRequest(async () => {
      const demo = demoResponse("delete", url);
      if (demo) return demo as ApiResponse<T>;
      return (await http.delete<ApiResponse<T>>(url, config)).data;
    });
  }
};

export async function chatStream(sessionId: string, content: string) {
  if (isDemo()) {
    const chunks = ["我听见你提到：", content || "现在有些压力", "。我们先把这件事拆小一点，给身体一个可以执行的下一步。"];
    return new ReadableStream<Uint8Array>({
      start(controller) {
        const encoder = new TextEncoder();
        chunks.forEach((chunk) => controller.enqueue(encoder.encode(chunk)));
        controller.close();
      }
    });
  }
  const baseURL = import.meta.env.VITE_API_BASE_URL || "/api";
  const response = await fetch(`${baseURL}/chat/message`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      token: localStorage.getItem("token") || ""
    },
    body: JSON.stringify({ sessionId, content })
  });
  if (!response.ok || !response.body) throw new Error(`聊天连接失败: ${response.status}`);
  return response.body;
}
