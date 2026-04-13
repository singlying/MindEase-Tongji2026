// 前端A负责：用户预约接口占位
import request from "./request";

export const createAppointmentApi = (payload: Record<string, unknown>) =>
  request.post("/appointment", payload);

export const getMyAppointmentsApi = () => request.get("/appointment/my");
