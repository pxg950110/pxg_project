import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

export function getScheduleTasks(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/task/schedules', { params })
}

export function createScheduleTask(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/task/schedules', data)
}

export function updateScheduleTask(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/task/schedules/${id}`, data)
}

export function deleteScheduleTask(id: number) {
  return request.delete<ApiResponse<void>>(`/task/schedules/${id}`)
}

export function triggerTask(id: number) {
  return request.post<ApiResponse<any>>(`/task/schedules/${id}/trigger`)
}

export function pauseTask(id: number) {
  return request.put<ApiResponse<any>>(`/task/schedules/${id}/pause`)
}

export function resumeTask(id: number) {
  return request.put<ApiResponse<any>>(`/task/schedules/${id}/resume`)
}

export function getTaskExecutions(taskId: number, params: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>(`/task/schedules/${taskId}/executions`, { params })
}
