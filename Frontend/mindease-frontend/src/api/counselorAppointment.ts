// 前端B负责：咨询师预约管理接口占位
import request from "./request";

export const getCounselorAppointmentsApi = () =>
  request.get("/counselor/appointment/list");

export const updateCounselorScheduleApi = (payload: Record<string, unknown>) =>
  request.post("/counselor/schedule", payload);
