import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// Patient CDR APIs
export function getPatients(params: { page?: number; page_size?: number; keyword?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/patients', { params })
}

export function getPatient(id: string) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${id}`)
}

export function getPatient360(id: string) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${id}/360`)
}

// Research RDR APIs
export function getProjects(params: { page?: number; page_size?: number; keyword?: string; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/rdr/projects', { params })
}

export function getProject(id: string) {
  return request.get<ApiResponse<any>>(`/rdr/projects/${id}`)
}

export function createProject(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/rdr/projects', data)
}

// Dataset APIs
export function getDatasets(params: { page?: number; page_size?: number; project_id?: string; keyword?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/rdr/datasets', { params })
}

// ETL APIs
export function getEtlTasks(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/etl/tasks', { params })
}

export function createEtlTask(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/etl/tasks', data)
}
