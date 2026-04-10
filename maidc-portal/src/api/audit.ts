import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

export function getAuditLogs(params: {
  page?: number; page_size?: number; module?: string; operation?: string;
  username?: string; start_time?: string; end_time?: string; status?: number
}) {
  return request.get<ApiResponse<PageResult<any>>>('/audit/operations', { params })
}

export function getAuditLogDetail(id: string) {
  return request.get<ApiResponse<any>>(`/audit/operations/${id}`)
}

export function getDataAccessLogs(params: {
  page?: number; page_size?: number; user_id?: string; data_type?: string;
  patient_id?: string; start_time?: string; end_time?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/audit/data-access', { params })
}

export function getSystemEvents(params: {
  page?: number; page_size?: number; event_type?: string; severity?: string;
  start_time?: string; end_time?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/audit/events', { params })
}

export function getComplianceReport(params: { start_time: string; end_time: string }) {
  return request.get<ApiResponse<any>>('/audit/reports/compliance', { params })
}
