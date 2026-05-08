import request from './request'

// 审核记录类型
export interface AuditItem {
  auditId: number
  userId: number
  username: string
  realName: string
  qualificationUrl: string
  idCardUrl?: string
  submitTime: string
  title?: string
  experienceYears?: number
  specialty?: string[] | string // 兼容 string 或 string[]
  bio?: string
  location?: string
  pricePerHour?: number
}

// 获取待审核列表
export const getAuditList = (params: { page: number, pageSize: number }) => {
  return request.get('/admin/audit/list', { params })
}

// 审核操作
export const processAudit = (data: { auditId: number, action: 'PASS' | 'REJECT', remark?: string }) => {
  return request.post('/admin/audit/process', data)
}

// --- 新增：量表管理接口 ---

// 1. 创建/更新量表配置
export const saveScaleConfig = (data: any) => {
  return request.post('/admin/assessment/scale', data)
}

// 2. 管理量表题目
export const saveScaleQuestions = (data: { scaleKey: string, questions: any[] }) => {
  return request.post('/admin/assessment/question', data)
}

// 辅助：获取量表题目详情 (复用用户端接口)
export const getScaleDetail = (scaleKey: string) => {
  return request.get(`/assessment/scale/${scaleKey}`)
}