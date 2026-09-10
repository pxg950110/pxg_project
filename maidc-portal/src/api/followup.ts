import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ==================== 随访管理 API（CRS 试点） ====================
// 后端: maidc-data /api/v1/cdr（FollowupProtocolController / PatientFollowupController /
//       FollowupTaskController / ScaleDefinitionController / TreatmentRecordController）

// ---- 随访方案 ----

export function getProtocols(cohortId: number | string) {
  return request.get<ApiResponse<any[]>>(`/cdr/disease-cohorts/${cohortId}/protocols`)
}

export function getLatestProtocol(cohortId: number | string) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${cohortId}/protocols/latest`)
}

export function createProtocolDraft(cohortId: number | string, data: any) {
  return request.post<ApiResponse<any>>(`/cdr/disease-cohorts/${cohortId}/protocols`, data)
}

/** 发布新版本（存量档案快照不受影响） */
export function publishProtocol(cohortId: number | string, data: any) {
  return request.put<ApiResponse<any>>(`/cdr/disease-cohorts/${cohortId}/protocols`, data)
}

// ---- 患者随访档案 ----

export function getFollowups(cohortId: number | string, params: {
  status?: string
  page?: number
  page_size?: number
}) {
  return request.get<ApiResponse<PageResult<any>>>(`/cdr/disease-cohorts/${cohortId}/followups`, { params })
}

/** 建档：按方案全量生成阶段任务 */
export function enrollFollowup(cohortId: number | string, data: {
  patientId: number
  doctorId: number
  nurseId?: number
  enrollDate?: string
}) {
  return request.post<ApiResponse<{ followupId: number; taskCount: number; protocolVersion: number }>>(
    `/cdr/disease-cohorts/${cohortId}/followups`, data)
}

export function closeFollowup(id: number | string, reason: string) {
  return request.post<ApiResponse<any>>(`/cdr/followups/${id}/close`, { reason })
}

export function suspendFollowup(id: number | string) {
  return request.post<ApiResponse<any>>(`/cdr/followups/${id}/suspend`)
}

export function resumeFollowup(id: number | string) {
  return request.post<ApiResponse<any>>(`/cdr/followups/${id}/resume`)
}

/** 升级方案：只重建未来未完成任务 */
export function upgradeFollowupProtocol(id: number | string) {
  return request.post<ApiResponse<{ protocolVersion: number; removedTasks: number; rebuiltTasks: number }>>(
    `/cdr/followups/${id}/upgrade-protocol`)
}

// ---- 任务 / 评估 ----

/** 工作台：今日到期 / 已超期 / 未来7天（OVERDUE 派生） */
export function getMyWorkbench(params: { userId?: number; all?: boolean }) {
  return request.get<ApiResponse<{
    today: any[]; overdue: any[]; upcoming: any[]
    stats: { todayCount: number; overdueCount: number; upcomingCount: number }
  }>>('/cdr/followup-tasks/my', { params })
}

/** 档案聚合详情：档案头（患者/医护名）+ tasks + assessments + treatments */
export function getFollowupDetail(followupId: number | string) {
  return request.get<ApiResponse<any>>(`/cdr/followups/${followupId}/detail`)
}

export function getFollowupTimeline(followupId: number | string) {
  return request.get<ApiResponse<any[]>>(`/cdr/followups/${followupId}/tasks`)
}

export function getAssessments(followupId: number | string) {
  return request.get<ApiResponse<any[]>>(`/cdr/followups/${followupId}/assessments`)
}

/** 执行：必评量表全量 + 可选治疗，一个事务 */
export function completeTask(taskId: number | string, data: {
  assessments: { scaleCode: string; answers: Record<string, any>; bindingSources?: any }[]
  treatments?: { category: string; name: string; occurredDate: string; detail?: any }[]
}, userId?: number) {
  return request.post<ApiResponse<any>>(`/cdr/followup-tasks/${taskId}/complete`, data, {
    params: userId ? { userId } : undefined,
  })
}

/** 跳过（医生，原因必填） */
export function skipTask(taskId: number | string, reason: string, userId?: number) {
  return request.post<ApiResponse<any>>(`/cdr/followup-tasks/${taskId}/skip`, { reason }, {
    params: userId ? { userId } : undefined,
  })
}

/** 计划外评估（医生随时发起，task_id=NULL） */
export function createPlanFreeAssessment(followupId: number | string, data: {
  scaleCode: string
  answers: Record<string, any>
}, userId?: number) {
  return request.post<ApiResponse<any>>(`/cdr/followups/${followupId}/assessments`, data, {
    params: userId ? { userId } : undefined,
  })
}

// ---- 量表定义 ----

export function getScales(params?: { keyword?: string }) {
  return request.get<ApiResponse<any[]>>('/cdr/scales', { params })
}

export function getScaleLatest(scaleCode: string) {
  return request.get<ApiResponse<any>>(`/cdr/scales/${scaleCode}`)
}

/** 按患者解析量表数据绑定（带入值/枚举选项） */
export function resolveScaleBindings(scaleCode: string, patientId: number) {
  return request.get<ApiResponse<Record<string, any>>>(`/cdr/scales/${scaleCode}/bindings`, {
    params: { patientId },
  })
}

export function getScaleVersions(scaleCode: string) {
  return request.get<ApiResponse<any[]>>(`/cdr/scales/${scaleCode}/versions`)
}

export function createScale(data: { scaleCode: string; name: string; definition: any }) {
  return request.post<ApiResponse<any>>('/cdr/scales', data)
}

export function publishScaleNewVersion(scaleCode: string, data: { name: string; definition: any }) {
  return request.put<ApiResponse<any>>(`/cdr/scales/${scaleCode}`, data)
}

export function updateScaleStatus(id: number | string, status: 'ACTIVE' | 'DISABLED') {
  return request.post<ApiResponse<any>>(`/cdr/scales/${id}/status`, { status })
}

// ---- 治疗记录 ----

export function getTreatments(followupId: number | string) {
  return request.get<ApiResponse<any[]>>(`/cdr/followups/${followupId}/treatments`)
}

export function createTreatment(followupId: number | string, data: {
  category: string
  name: string
  occurredDate: string
  detail?: any
  taskId?: number
}) {
  return request.post<ApiResponse<any>>(`/cdr/followups/${followupId}/treatments`, data)
}

export function getCdrTreatmentCandidates(followupId: number | string) {
  return request.get<ApiResponse<any[]>>(`/cdr/followups/${followupId}/treatments/cdr-candidates`)
}

/** CDR 带入：source=CDR 只读快照，重复自动跳过并列出 */
export function importCdrTreatments(followupId: number | string, items: any[]) {
  return request.post<ApiResponse<{ imported: number; skipped: any[] }>>(
    `/cdr/followups/${followupId}/treatments/import-cdr`, { items })
}

// ---- 结局看板 ----

export function getOutcomeStats(cohortId: number | string) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${cohortId}/outcome-stats`)
}

// ---- 提醒（管理/联调） ----

export function runFollowupReminders(date?: string) {
  return request.post<ApiResponse<number>>('/cdr/followup-tasks/reminders/run', null, {
    params: date ? { date } : undefined,
  })
}
