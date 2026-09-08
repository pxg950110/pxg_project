import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

export function getAuditLogs(params: {
  page?: number; pageSize?: number; module?: string; operation?: string;
  username?: string; startTime?: string; endTime?: string; status?: number
}) {
  return request.get<ApiResponse<PageResult<any>>>('/audit/operations', { params })
}

export function getAuditLogDetail(id: string) {
  return request.get<ApiResponse<any>>(`/audit/operations/${id}`)
}

export function getDataAccessLogs(params: {
  page?: number; pageSize?: number; userId?: string; dataType?: string;
  patientId?: string; startTime?: string; endTime?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/audit/data-access', { params })
}

export function getSystemEvents(params: {
  page?: number; pageSize?: number; eventType?: string; severity?: string;
  startTime?: string; endTime?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/audit/events', { params })
}

export function getComplianceReport(params: { startTime: string; endTime: string }) {
  return request.get<ApiResponse<any>>('/audit/reports/compliance', { params })
}

export function exportAuditLogs(params: {
  module?: string; operation?: string; username?: string;
  startTime?: string; endTime?: string; status?: number
}) {
  return request.get('/audit/operations/export', { params, responseType: 'blob' })
}

export function exportDataAccessLogs(params: {
  userId?: string; dataType?: string; patientId?: string;
  startTime?: string; endTime?: string
}) {
  return request.get('/audit/data-access/export', { params, responseType: 'blob' })
}

export function exportSystemEvents(params: {
  eventType?: string; severity?: string; startTime?: string; endTime?: string
}) {
  return request.get('/audit/events/export', { params, responseType: 'blob' })
}
