// 用户中心 Mock 数据
import type { DashboardData, Notification } from "@/api/user";

// Mock Dashboard 数据
const mockDashboardData: DashboardData = {
  moodSummary: {
    avgScore: 7.5,
    continuousDays: 12,
  },
  upcomingAppointments: [
    {
      id: 3005,
      time: "明天 10:00",
      counselor: "李医生",
    },
  ],
  unreadNotifications: 3,
};

// Mock 通知列表
let mockNotifications: Notification[] = [
  {
    id: 9001,
    type: "appointment",
    title: "预约提醒",
    content: "您的预约将在1小时后开始，请提前做好准备。",
    isRead: false,
    createTime: "2023-10-27 09:00:00",
  },
  {
    id: 9002,
    type: "system",
    title: "系统更新",
    content: "MindEase 已更新至 V4.0，新增了 AI 语音咨询功能。",
    isRead: true,
    createTime: "2023-10-26 15:30:00",
  },
  {
    id: 9003,
    type: "report",
    title: "测评完成",
    content: "您的 GAD-7 焦虑测评报告已生成，点击查看详情。",
    isRead: true,
    createTime: "2023-10-25 10:20:00",
  },
  {
    id: 9004,
    type: "system",
    title: "欢迎使用 MindEase",
    content: "感谢您注册 MindEase，开始您的心理健康之旅吧！",
    isRead: true,
    createTime: "2023-10-01 12:00:00",
  },
];

// Mock API 实现
export const mockUserData = {
  // 获取 Dashboard 数据
  getDashboard: (): Promise<DashboardData> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(mockDashboardData);
      }, 300);
    });
  },

  // 获取通知列表
  getNotifications: (
    limit: number = 20
  ): Promise<{ notifications: Notification[] }> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const notifications = mockNotifications.slice(0, limit);
        resolve({ notifications });
      }, 300);
    });
  },

  // 标记通知已读
  markNotificationRead: (id: number): Promise<{ success: boolean }> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const notification = mockNotifications.find((n) => n.id === id);
        if (notification) {
          notification.isRead = true;
          // 更新未读数量
          const unreadCount = mockNotifications.filter((n) => !n.isRead).length;
          mockDashboardData.unreadNotifications = unreadCount;
        }
        resolve({ success: true });
      }, 200);
    });
  },

  // 标记全部已读
  markAllNotificationsRead: (): Promise<{ success: boolean }> => {
    return new Promise((resolve) => {
      setTimeout(() => {
        mockNotifications.forEach((n) => {
          n.isRead = true;
        });
        mockDashboardData.unreadNotifications = 0;
        resolve({ success: true });
      }, 200);
    });
  },
};
