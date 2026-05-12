// 预约模块Mock数据
import type {
  AvailableSlots,
  AppointmentList,
  AppointmentDetail,
  AppointmentCreateResult,
} from "@/api/appointment";

// Mock可用时段
const generateMockSlots = (date: string) => {
  const slots = [
    { startTime: "09:00", endTime: "10:00", available: true },
    { startTime: "10:00", endTime: "11:00", available: true },
    { startTime: "11:00", endTime: "12:00", available: false },
    { startTime: "14:00", endTime: "15:00", available: true },
    { startTime: "15:00", endTime: "16:00", available: true },
    { startTime: "16:00", endTime: "17:00", available: false },
    { startTime: "18:00", endTime: "19:00", available: true },
    { startTime: "19:00", endTime: "20:00", available: true },
    { startTime: "20:00", endTime: "21:00", available: false },
  ];
  return { date, slots };
};

// Mock预约列表数据
let mockAppointments: Array<{
  id: number;
  startTime: string;
  endTime: string;
  status: string;
  targetName: string;
  targetAvatar: string | null;
  targetRole: string;
}> = [
  {
    id: 3001,
    startTime: "2024-12-10 10:00:00",
    endTime: "2024-12-10 11:00:00",
    status: "CONFIRMED",
    targetName: "李明",
    targetAvatar:
      "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=100",
    targetRole: "COUNSELOR",
  },
  {
    id: 3002,
    startTime: "2024-12-08 14:00:00",
    endTime: "2024-12-08 15:00:00",
    status: "COMPLETED",
    targetName: "王芳",
    targetAvatar:
      "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=100",
    targetRole: "COUNSELOR",
  },
  {
    id: 3003,
    startTime: "2024-12-15 09:00:00",
    endTime: "2024-12-15 10:00:00",
    status: "PENDING",
    targetName: "张伟",
    targetAvatar:
      "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=100",
    targetRole: "COUNSELOR",
  },
];

let nextAppointmentId = 3004;

export const mockAppointmentData = {
  // 获取可用时段
  getAvailableSlots: (counselorId: number, date: string): AvailableSlots => {
    return generateMockSlots(date);
  },

  // 创建预约
  createAppointment: (data: {
    counselorId: number;
    startTime: string;
    endTime: string;
    userNote?: string;
  }): AppointmentCreateResult => {
    const newAppointment = {
      id: nextAppointmentId++,
      startTime: data.startTime,
      endTime: data.endTime,
      status: "PENDING",
      targetName: "咨询师",
      targetAvatar: null,
      targetRole: "COUNSELOR",
    };
    mockAppointments.unshift(newAppointment);
    return { appointmentId: newAppointment.id, status: "PENDING" };
  },

  // 获取预约列表
  getMyAppointments: (
    status?: string,
    page: number = 1,
    pageSize: number = 10
  ): AppointmentList => {
    let filtered = [...mockAppointments];
    if (status) {
      filtered = filtered.filter((a) => a.status === status);
    }
    const start = (page - 1) * pageSize;
    const list = filtered.slice(start, start + pageSize);
    return { total: filtered.length, list };
  },

  // 获取预约详情
  getAppointmentDetail: (id: number): AppointmentDetail | null => {
    const appointment = mockAppointments.find((a) => a.id === id);
    if (!appointment) return null;
    return {
      id: appointment.id,
      counselorId: 101,
      counselorName: appointment.targetName,
      startTime: appointment.startTime,
      endTime: appointment.endTime,
      status: appointment.status,
      userNote: "希望咨询关于焦虑问题",
      cancelReason: null,
    };
  },

  // 取消预约
  cancelAppointment: (id: number, reason: string): boolean => {
    const appointment = mockAppointments.find((a) => a.id === id);
    if (appointment) {
      appointment.status = "CANCELLED";
      return true;
    }
    return false;
  },
};
