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

export function searchClinicalNotes(params: {
  encounterId?: number; patientId?: number; noteType?: string; noteCategory?: string;
  signStatus?: string; urgency?: string; source?: string; keyword?: string;
  page?: number; size?: number
}) {
  return request.get<ApiResponse<any>>('/cdr/clinical-notes/search', { params })
}

export function listClinicalNotesByEncounter(encounterId: number, params?: { page?: number; size?: number }) {
  return request.get<ApiResponse<any>>('/cdr/clinical-notes/by-encounter', { params: { encounterId, ...params } })
}

export function createClinicalNote(data: any) {
  return request.post<ApiResponse<any>>('/cdr/clinical-notes', data)
}

export function updateClinicalNote(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/cdr/clinical-notes/${id}`, data)
}

export function signClinicalNote(id: number, signedBy: string) {
  return request.post<ApiResponse<any>>(`/cdr/clinical-notes/${id}/sign`, null, { params: { signedBy } })
}

export function countersignClinicalNote(id: number, signedBy: string) {
  return request.post<ApiResponse<any>>(`/cdr/clinical-notes/${id}/countersign`, null, { params: { signedBy } })
}

export function deleteClinicalNote(id: number) {
  return request.delete(`/cdr/clinical-notes/${id}`)
}

// Document Templates
export function getDocumentTemplates(params?: { noteType?: string; page?: number; size?: number }) {
  return request.get<ApiResponse<any>>('/cdr/document-templates', { params })
}

export function getDocumentTemplate(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/document-templates/${id}`)
}

export function createDocumentTemplate(data: any) {
  return request.post<ApiResponse<any>>('/cdr/document-templates', data)
}

export function updateDocumentTemplate(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/cdr/document-templates/${id}`, data)
}

export function deleteDocumentTemplate(id: number) {
  return request.delete(`/cdr/document-templates/${id}`)
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
  return request.post<ApiResponse<{ success: boolean; message: string; latencyMs?: number }>>(`/cdr/datasources/${id}/test-connection`)
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
// 后端字段（typeCode/typeName、dictCode/dictLabel/dictValue/isEnabled/sortOrder）
// 与页面字段（code/name、code/name/value/status/sort_order）在此映射，页面零感知。

function mapDictType(t: any) {
  return { id: t.id, code: t.typeCode, name: t.typeName, remark: t.remark }
}

function mapDictItemToFront(i: any) {
  return {
    id: i.id,
    code: i.dictCode,
    name: i.dictLabel,
    value: i.dictValue,
    sort_order: i.sortOrder ?? 0,
    status: i.isEnabled === false ? 'DISABLED' : 'ENABLED',
    remark: i.remark,
  }
}

function mapDictItemToBack(d: Record<string, any>) {
  return {
    dictCode: d.code,
    dictLabel: d.name,
    dictValue: d.value,
    sortOrder: d.sort_order ?? 0,
    isEnabled: d.status !== 'DISABLED',
    remark: d.remark,
  }
}

export async function getDictTypes(params?: { page?: number; page_size?: number; keyword?: string }) {
  const res = await request.get<ApiResponse<any>>('/system/dict-types', { params })
  const page: any = res.data.data
  const items = (page?.content ?? []).map(mapDictType)
  res.data.data = { items, total: page?.totalElements ?? items.length }
  return res
}

export function createDictType(data: Record<string, any>) {
  return request.post<ApiResponse<any>>(
    '/system/dict-types',
    { typeCode: data.code, typeName: data.name, remark: data.remark },
  )
}

export function updateDictType(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(
    `/system/dict-types/${id}`,
    { typeName: data.name, remark: data.remark },
  )
}

export function deleteDictType(id: number) {
  return request.delete<ApiResponse<void>>(`/system/dict-types/${id}`)
}

export async function getDictItems(typeId: number, params?: { page?: number; page_size?: number; keyword?: string }) {
  const res = await request.get<ApiResponse<any>>(`/system/dict-types/${typeId}/items`, { params })
  const list = (res.data.data ?? []).map(mapDictItemToFront)
  res.data.data = { items: list, total: list.length }
  return res
}

export function createDictItem(data: Record<string, any>) {
  const typeId = data.type_id
  return request.post<ApiResponse<any>>(`/system/dict-types/${typeId}/items`, mapDictItemToBack(data))
}

export function updateDictItem(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/system/dict-items/${id}`, mapDictItemToBack(data))
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

// ========== Clinical Search API ==========
export function clinicalSearch(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/search', data)
}

// Smart Search API
export function smartSearch(data: { keyword: string; domains?: string[]; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number }) {
  return request.post<ApiResponse<any>>('/cdr/smart-search', data)
}

// ETL APIs
export function getEtlTasks(params: { page?: number; page_size?: number; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/etl/tasks', { params })
}

export function createEtlTask(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/etl/tasks', data)
}

// ========== Disease Cohort APIs ==========
export function getDiseaseCohorts(params: { page?: number; page_size?: number; keyword?: string; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/disease-cohorts', { params })
}

export function getDiseaseCohort(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${id}`)
}

export function createDiseaseCohort(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/disease-cohorts', data)
}

export function updateDiseaseCohort(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/disease-cohorts/${id}`, data)
}

export function deleteDiseaseCohort(id: number) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-cohorts/${id}`)
}

export function syncDiseaseCohort(id: number) {
  return request.post<ApiResponse<void>>(`/cdr/disease-cohorts/${id}/sync`)
}

export function previewDiseaseCohort(id: number) {
  return request.get<ApiResponse<{ patientCount: number }>>(`/cdr/disease-cohorts/${id}/match-preview`)
}

export function getDiseaseCohortPatients(id: number, params: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${id}/patients`, { params })
}

export function addDiseaseCohortPatient(id: number, patientId: number) {
  return request.post<ApiResponse<void>>(`/cdr/disease-cohorts/${id}/patients/${patientId}`)
}

export function removeDiseaseCohortPatient(id: number, patientId: number) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-cohorts/${id}/patients/${patientId}`)
}

export function getDiseaseCohortStatistics(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${id}/statistics`)
}

export function exportDiseaseCohort(id: number) {
  return request.get(`/cdr/disease-cohorts/${id}/export`, { responseType: 'blob' })
}

export function searchDiseaseTemplates(q: string) {
  return request.get<ApiResponse<any[]>>('/dict/disease-templates', { params: { q } })
}

export function aiSuggestDiseaseRules(diseaseName: string) {
  return request.post<ApiResponse<{ groups: any[]; confidence: number; source: string }>>('/cdr/disease-cohorts/ai-suggest', { disease_name: diseaseName })
}

// ========== Statistics APIs ==========
export function getDataGrowthTrend(params?: { months?: number }) {
  return request.get<ApiResponse<any>>('/cdr/statistics/data-growth-trend', { params })
}

export function getDataSourceDistribution() {
  return request.get<ApiResponse<any>>('/cdr/statistics/source-distribution')
}
