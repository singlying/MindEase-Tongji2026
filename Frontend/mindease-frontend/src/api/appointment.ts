// 预约管理API（用户端）
import request from "./request";
import type { ApiResponse } from "./request";
import { mockAppointmentData } from "@/mock/appointment";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

// ============ 类型定义 ============

export interface TimeSlot {
  startTime: string;
  endTime: string;
  available: boolean;
}

export interface AvailableSlots {
  date: string;
  slots: TimeSlot[];
}

export interface AppointmentListItem {
  id: number;
  startTime: string;
  endTime: string;
  status: string;
  targetName: string;
  targetAvatar: string | null;
  targetRole: string;
}

export interface AppointmentList {
  total: number;
  list: AppointmentListItem[];
}

export interface AppointmentDetail {
  id: number;
  counselorId: number;
  counselorName: string;
  startTime: string;
  endTime: string;
  status: string;
  userNote: string | null;
  cancelReason: string | null;
}

export interface AppointmentCreateParams {
  counselorId: number;
  startTime: string;
  endTime: string;
  userNote?: string;
}

export interface AppointmentCreateResult {
  appointmentId: number;
  status: string;
}

// ============ API函数 ============

/** 查询可用时段 */
export const getAvailableSlots = (counselorId: number, date: string) => {
  if (USE_MOCK) {
    return Promise.resolve({
      code: 200,
      message: "success",
      data: mockAppointmentData.getAvailableSlots(counselorId, date),
    });
  }
  return request.get<ApiResponse<AvailableSlots>>(
    "/appointment/available-slots",
    {
      params: { counselorId, date },
    }
  );
};

/** 创建预约 */
export const createAppointment = (data: AppointmentCreateParams) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          message: "预约成功",
          data: mockAppointmentData.createAppointment(data),
        });
      }, 500);
    });
  }
  return request.post<ApiResponse<AppointmentCreateResult>>(
    "/appointment/create",
    data
  );
};

/** 获取我的预约列表 */
export const getMyAppointments = (
  status?: string,
  page: number = 1,
  pageSize: number = 10
) => {
  if (USE_MOCK) {
    return Promise.resolve({
      code: 200,
      message: "success",
      data: mockAppointmentData.getMyAppointments(status, page, pageSize),
    });
  }
  return request.get<ApiResponse<AppointmentList>>(
    "/appointment/my-appointments",
    {
      params: { status, page, pageSize },
    }
  );
};

/** 获取预约详情 */
export const getAppointmentDetail = (id: number) => {
  if (USE_MOCK) {
    const data = mockAppointmentData.getAppointmentDetail(id);
    if (data) {
      return Promise.resolve({ code: 200, message: "success", data });
    }
    return Promise.reject(new Error("预约不存在"));
  }
  return request.get<ApiResponse<AppointmentDetail>>(`/appointment/${id}`);
};

/** 取消预约 */
export const cancelAppointment = (id: number, cancelReason: string) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      setTimeout(() => {
        mockAppointmentData.cancelAppointment(id, cancelReason);
        resolve({ code: 200, message: "取消成功", data: { success: true } });
      }, 300);
    });
  }
  return request.put<ApiResponse<{ success: boolean }>>(
    `/appointment/${id}/cancel`,
    {
      cancelReason,
    }
  );
};
