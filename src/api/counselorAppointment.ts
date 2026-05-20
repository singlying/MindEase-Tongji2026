import request from './request'

// 类型定义
export interface AppointmentItem {
  id: number
  targetName: string
  targetAvatar: string
  startTime: string
  endTime: string
  status: 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'
  userNote?: string
}

export interface ScheduleParams {
  workDays: number[] // 1-7
  workHours: Array<{
    start: string
    end: string
  }>
}

// 获取我的预约列表
export const getMyAppointments = (params: { status?: string, page?: number, pageSize?: number }) => {
  return request.get('/appointment/my-appointments', { params })
}

// 确认预约
export const confirmAppointment = (id: number) => {
  return request.put(`/appointment/${id}/confirm`)
}

// 获取预约详情
export const getAppointmentDetail = (id: number) => {
  return request.get(`/appointment/${id}`)
}

// 设置排班
export const setSchedule = (data: ScheduleParams) => {
  return request.post('/appointment/schedule', data)
}

// 获取咨询师个人信息(用于统计数据，如评分)
export const getCounselorInfo = (id: number) => {
  return request.get(`/counselor/${id}`)
}