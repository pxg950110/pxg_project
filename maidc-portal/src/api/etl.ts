import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ==================== Pipeline ====================
export function getEtlPipelines(params: { page?: number; page_size?: number; keyword?: string; sourceId?: number; status?: string; engineType?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/etl/pipelines', { params })
}
export function getEtlPipeline(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/etl/pipelines/${id}`)
}
export function createEtlPipeline(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/etl/pipelines', data)
}
export function updateEtlPipeline(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/etl/pipelines/${id}`, data)
}
export function deleteEtlPipeline(id: number) {
  return request.delete<ApiResponse<void>>(`/cdr/etl/pipelines/${id}`)
}
export function runEtlPipeline(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/etl/pipelines/${id}/run`)
}
export function validateEtlPipeline(id: number) {
  return request.post<ApiResponse<string[]>>(`/cdr/etl/pipelines/${id}/validate`)
}
export function copyEtlPipeline(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/etl/pipelines/${id}/copy`)
}
export function updateEtlPipelineStatus(id: number, status: string) {
  return request.put<ApiResponse<any>>(`/cdr/etl/pipelines/${id}/status`, { status })
}
export function getEtlPipelineExecutions(id: number, params?: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>(`/cdr/etl/pipelines/${id}/executions`, { params })
}

// ==================== Steps ====================
export function getEtlSteps(pipelineId: number) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/pipelines/${pipelineId}/steps`)
}
export function createEtlStep(pipelineId: number, data: Record<string, any>) {
  return request.post<ApiResponse<any>>(`/cdr/etl/pipelines/${pipelineId}/steps`, data)
}
export function updateEtlStep(pipelineId: number, stepId: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/etl/pipelines/${pipelineId}/steps/${stepId}`, data)
}
export function deleteEtlStep(pipelineId: number, stepId: number) {
  return request.delete<ApiResponse<void>>(`/cdr/etl/pipelines/${pipelineId}/steps/${stepId}`)
}
export function reorderEtlSteps(pipelineId: number, stepIds: number[]) {
  return request.put<ApiResponse<void>>(`/cdr/etl/pipelines/${pipelineId}/steps/reorder`, { stepIds })
}
export function previewEtlStep(pipelineId: number, stepId: number) {
  return request.post<ApiResponse<any[]>>(`/cdr/etl/pipelines/${pipelineId}/steps/${stepId}/preview`)
}

// ==================== Field Mappings ====================
export function getFieldMappings(stepId: number) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/steps/${stepId}/field-mappings`)
}
export function batchUpdateFieldMappings(stepId: number, mappings: any[]) {
  return request.put<ApiResponse<any[]>>(`/cdr/etl/steps/${stepId}/field-mappings`, mappings)
}
export function autoMapFields(stepId: number) {
  return request.post<ApiResponse<any[]>>(`/cdr/etl/steps/${stepId}/field-mappings/auto-map`)
}

// ==================== Executions ====================
export function getEtlExecutions(params?: { page?: number; page_size?: number; pipelineId?: number; stepId?: number; status?: string; triggerType?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/etl/executions', { params })
}
export function getEtlExecution(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/etl/executions/${id}`)
}
export function getEtlExecutionLogs(id: number) {
  return request.get<ApiResponse<string>>(`/cdr/etl/executions/${id}/logs`)
}
export function cancelEtlExecution(id: number) {
  return request.post<ApiResponse<void>>(`/cdr/etl/executions/${id}/cancel`)
}
export function retryEtlExecution(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/etl/executions/${id}/retry`)
}

// ==================== Metadata ====================
export function getEtlSchemas() {
  return request.get<ApiResponse<string[]>>('/cdr/etl/metadata/schemas')
}
export function getEtlTables(schema: string) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/metadata/schemas/${schema}/tables`)
}
export function getEtlColumns(schema: string, table: string) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/metadata/tables/${schema}.${table}/columns`)
}
