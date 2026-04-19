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

// Encounter CDR APIs
export function getEncounterDetail(patientId: string, encounterId: string) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}`)
}

export function getDiagnoses(patientId: string, encounterId: string, params?: { type?: string }) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}/diagnoses`, { params })
}

export function getLabResults(patientId: string, encounterId: string, params?: { category?: string }) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}/lab-results`, { params })
}

export function getImagingStudies(patientId: string, encounterId: string) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}/imaging`)
}

export function getMedications(patientId: string, encounterId: string, params?: { status?: string }) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}/medications`, { params })
}

export function getVitalSigns(patientId: string, encounterId: string) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}/vital-signs`)
}

export function getClinicalNotes(patientId: string, encounterId: string, params?: { keyword?: string }) {
  return request.get<ApiResponse<any>>(`/cdr/patients/${patientId}/encounters/${encounterId}/notes`, { params })
}

// ========== Data Source APIs ==========
export function getDataSources(params: { page?: number; page_size?: number; keyword?: string; type?: string; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/datasources', { params })
}

export function getDataSource(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/datasources/${id}`)
}

export function createDataSource(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/datasources', data)
}

export function updateDataSource(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/datasources/${id}`, data)
}

export function deleteDataSource(id: number) {
  return request.delete<ApiResponse<void>>(`/cdr/datasources/${id}`)
}

export function testDataSourceConnection(id: number) {
  return request.post<ApiResponse<{ success: boolean; message: string }>>(`/cdr/datasources/${id}/test-connection`)
}

export function syncDataSource(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/datasources/${id}/sync`)
}

export function getDataSourceSyncHistory(id: number, params?: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>(`/cdr/datasources/${id}/sync-history`, { params })
}

export function getDataSourceSchemaMapping(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/datasources/${id}/schema-mapping`)
}

export function getDataSourceStatistics(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/datasources/${id}/statistics`)
}

// ========== Sync Task APIs ==========
export function getSyncTasks(params: { page?: number; page_size?: number; status?: string; source_id?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/sync-tasks', { params })
}

export function getSyncTaskLogs(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/sync-tasks/${id}/logs`)
}

export function retrySyncTask(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/sync-tasks/${id}/retry`)
}

// ========== Data Quality Rule APIs ==========
export function getQualityRules(params: { page?: number; page_size?: number; keyword?: string; type?: string; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/rdr/quality-rules', { params })
}

export function getQualityRule(id: number) {
  return request.get<ApiResponse<any>>(`/rdr/quality-rules/${id}`)
}

export function createQualityRule(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/rdr/quality-rules', data)
}

export function updateQualityRule(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/rdr/quality-rules/${id}`, data)
}

export function deleteQualityRule(id: number) {
  return request.delete<ApiResponse<void>>(`/rdr/quality-rules/${id}`)
}

export function toggleQualityRule(id: number, enabled: boolean) {
  return request.put<ApiResponse<any>>(`/rdr/quality-rules/${id}/toggle`, { enabled })
}

// ========== Quality Check Result APIs ==========
export function getQualityResults(params: { page?: number; page_size?: number; rule_id?: number; status?: string; start_time?: string; end_time?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/rdr/quality-results', { params })
}

export function getQualityResult(id: number) {
  return request.get<ApiResponse<any>>(`/rdr/quality-results/${id}`)
}

// ========== Desensitize Rule APIs ==========
export function getDesensitizeRules(params: { page?: number; page_size?: number; keyword?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/desensitize-rules', { params })
}

export function createDesensitizeRule(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/desensitize-rules', data)
}

export function updateDesensitizeRule(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/desensitize-rules/${id}`, data)
}

export function deleteDesensitizeRule(id: number) {
  return request.delete<ApiResponse<void>>(`/cdr/desensitize-rules/${id}`)
}

export function toggleDesensitizeRule(id: number, enabled: boolean) {
  return request.put<ApiResponse<any>>(`/cdr/desensitize-rules/${id}/toggle`, { enabled })
}

export function previewDesensitize(data: { field: string; strategy: string; params: Record<string, any> }) {
  return request.post<ApiResponse<{ original: string; desensitized: string }>>('/cdr/desensitize-rules/preview', data)
}

// ========== Data Dictionary APIs ==========
export function getDictTypes(params?: { page?: number; page_size?: number; keyword?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/system/dict-types', { params })
}

export function createDictType(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/system/dict-types', data)
}

export function updateDictType(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/system/dict-types/${id}`, data)
}

export function deleteDictType(id: number) {
  return request.delete<ApiResponse<void>>(`/system/dict-types/${id}`)
}

export function getDictItems(typeId: number, params?: { page?: number; page_size?: number; keyword?: string }) {
  return request.get<ApiResponse<PageResult<any>>>(`/system/dict-types/${typeId}/items`, { params })
}

export function createDictItem(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/system/dict-items', data)
}

export function updateDictItem(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/system/dict-items/${id}`, data)
}

export function deleteDictItem(id: number) {
  return request.delete<ApiResponse<void>>(`/system/dict-items/${id}`)
}

// ========== Data Source Type APIs ==========
export function getDataSourceTypes() {
  return request.get<ApiResponse<any[]>>('/cdr/datasource-types')
}

export function getDataSourceType(code: string) {
  return request.get<ApiResponse<any>>(`/cdr/datasource-types/${code}`)
}

export function createDataSourceType(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/datasource-types', data)
}

export function updateDataSourceType(code: string, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/datasource-types/${code}`, data)
}

export function deleteDataSourceType(code: string) {
  return request.delete<ApiResponse<void>>(`/cdr/datasource-types/${code}`)
}

// ========== Data Source Enhanced APIs ==========
export function testConnectionPreSave(data: { type_code: string; connection_params: Record<string, any> }) {
  return request.post<ApiResponse<{ success: boolean; message: string; latencyMs?: number; details?: Record<string, any> }>>('/cdr/datasources/test-connection', data)
}

export function getDataSourceHealth(id: number, limit = 50) {
  return request.get<ApiResponse<any[]>>(`/cdr/datasources/${id}/health`, { params: { limit } })
}

export function getDataSourceHealthStats(id: number, days = 30) {
  return request.get<ApiResponse<{ totalChecks: number; successCount: number; failCount: number; availabilityRate: number; avgLatencyMs: number }>>(`/cdr/datasources/${id}/health/stats`, { params: { days } })
}

// ETL APIs
export function getEtlTasks(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/etl/tasks', { params })
}

export function createEtlTask(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/etl/tasks', data)
}
