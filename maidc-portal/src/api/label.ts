import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

export function getLabelTasks(params: { page?: number; page_size?: number; status?: string; task_type?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/label/tasks', { params })
}

export function getLabelTask(id: number) {
  return request.get<ApiResponse<any>>(`/label/tasks/${id}`)
}

export function createLabelTask(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/label/tasks', data)
}

export function updateLabelTask(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/label/tasks/${id}`, data)
}

export function getLabelTaskStats(id: number) {
  return request.get<ApiResponse<any>>(`/label/tasks/${id}/stats`)
}

export function triggerAiPreAnnotate(id: number) {
  return request.post<ApiResponse<any>>(`/label/tasks/${id}/ai-preannotate`)
}
