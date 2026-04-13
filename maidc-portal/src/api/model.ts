import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

export interface ModelListParams {
  page?: number
  page_size?: number
  model_type?: string
  status?: string
  keyword?: string
}

export interface ModelVO {
  id: number
  model_code: string
  model_name: string
  model_type: string
  task_type: string
  framework: string
  status: string
  latest_version: string
  tags: string[]
  owner_name: string
  created_at: string
  updated_at: string
}

export interface ModelDetailVO extends ModelVO {
  description: string
  input_schema: Record<string, any>
  output_schema: Record<string, any>
  version_count: number
}

export function getModels(params: ModelListParams) {
  return request.get<ApiResponse<PageResult<ModelVO>>>('/models', { params })
}

export function getModel(id: number) {
  return request.get<ApiResponse<ModelDetailVO>>(`/models/${id}`)
}

export function createModel(data: Record<string, any>) {
  return request.post<ApiResponse<ModelVO>>('/models', data)
}

export function updateModel(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<ModelVO>>(`/models/${id}`, data)
}

export function deleteModel(id: number) {
  return request.delete<ApiResponse<void>>(`/models/${id}`)
}

export function createVersion(modelId: number, formData: FormData) {
  return request.post<ApiResponse<any>>(`/models/${modelId}/versions`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300000,
  })
}

export function getVersions(modelId: number, params?: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>(`/models/${modelId}/versions`, { params })
}

export function compareVersions(modelId: number, v1: number, v2: number) {
  return request.get<ApiResponse<any>>(`/models/${modelId}/versions/compare`, { params: { v1, v2 } })
}

export function createEvaluation(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/evaluations', data)
}

export function getEvaluation(id: number) {
  return request.get<ApiResponse<any>>(`/evaluations/${id}`)
}

export function createDeployment(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/deployments', data)
}

export function getDeploymentStatus(id: number) {
  return request.get<ApiResponse<any>>(`/deployments/${id}/status`)
}

export function startDeployment(id: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/start`)
}

export function stopDeployment(id: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/stop`)
}

// Evaluation list
export function getEvaluations(params: { page?: number; page_size?: number; status?: string; model_id?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/evaluations', { params })
}

// Approval APIs
export function getApprovals(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/approvals', { params })
}

export function submitApproval(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/approvals', data)
}

export function reviewApproval(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/approvals/${id}/review`, data)
}

export function getApproval(id: number) {
  return request.get<ApiResponse<any>>(`/approvals/${id}`)
}

// Deployment APIs
export function getDeployments(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/deployments', { params })
}

export function getDeployment(id: number) {
  return request.get<ApiResponse<any>>(`/deployments/${id}`)
}

export function scaleDeployment(id: number, replicas: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/scale`, { replicas })
}

export function restartDeployment(id: number) {
  return request.put<ApiResponse<any>>(`/deployments/${id}/restart`)
}

// Monitoring APIs
export function getInferenceLogs(params: { page?: number; page_size?: number; model_id?: number; deployment_id?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/monitoring/inference-logs', { params })
}

export function getMetricsOverview() {
  return request.get<ApiResponse<any>>('/monitoring/metrics')
}
