import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// CodeSystem
export const getCodeSystems = () => request.get<ApiResponse<any[]>>('/masterdata/code-systems')
export const getCodeSystem = (id: number) => request.get<ApiResponse<any>>(`/masterdata/code-systems/${id}`)
export const getCodeSystemStats = (id: number) => request.get<ApiResponse<any>>(`/masterdata/code-systems/${id}/stats`)
export const createCodeSystem = (data: any) => request.post<ApiResponse<any>>('/masterdata/code-systems', data)
export const updateCodeSystem = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/code-systems/${id}`, data)

// Concept
export const getConcepts = (params: any) => request.get<ApiResponse<PageResult<any>>>('/masterdata/concepts', { params })
export const getConcept = (id: number) => request.get<ApiResponse<any>>(`/masterdata/concepts/${id}`)
export const searchConcepts = (params: any) => request.get<ApiResponse<PageResult<any>>>('/masterdata/concepts/search', { params })
export const getConceptChildren = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/children`)
export const getConceptAncestors = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/ancestors`)
export const getConceptMappings = (id: number, params?: any) => request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/mappings`, { params })
export const getConceptSynonyms = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/synonyms`)
export const createConcept = (data: any) => request.post<ApiResponse<any>>('/masterdata/concepts', data)
export const updateConcept = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/concepts/${id}`, data)
export const deleteConcept = (id: number) => request.delete(`/masterdata/concepts/${id}`)

// Mappings
export const createMapping = (data: any) => request.post<ApiResponse<any>>('/masterdata/mappings', data)
export const batchCreateMappings = (data: any[]) => request.post<ApiResponse<any[]>>('/masterdata/mappings/batch', data)
export const deleteMapping = (id: number) => request.delete(`/masterdata/mappings/${id}`)

// Reference Ranges
export const getReferenceRanges = (params: any) => request.get<ApiResponse<any[]>>('/masterdata/reference-ranges', { params })
export const evaluateReferenceRange = (params: any) => request.get<ApiResponse<any>>('/masterdata/reference-ranges/evaluate', { params })
export const createReferenceRange = (data: any) => request.post<ApiResponse<any>>('/masterdata/reference-ranges', data)
export const updateReferenceRange = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/reference-ranges/${id}`, data)

// Drug Interactions
export const getDrugInteractions = (params: any) => request.get<ApiResponse<any[]>>('/masterdata/drug-interactions', { params })
export const checkDrugInteraction = (drug1: number, drug2: number) => request.get<ApiResponse<any[]>>('/masterdata/drug-interactions/check', { params: { drug1, drug2 } })
export const checkDrugList = (drugIds: number[]) => request.post<ApiResponse<any[]>>('/masterdata/drug-interactions/check-list', drugIds)
export const createDrugInteraction = (data: any) => request.post<ApiResponse<any>>('/masterdata/drug-interactions', data)

// Institutions
export const getInstitutions = () => request.get<ApiResponse<any[]>>('/masterdata/institutions')
export const createInstitution = (data: any) => request.post<ApiResponse<any>>('/masterdata/institutions', data)
export const updateInstitution = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/institutions/${id}`, data)

// Local Concepts
export const getLocalConcepts = (params: any) => request.get<ApiResponse<PageResult<any>>>('/masterdata/local-concepts', { params })
export const getUnmappedLocalConcepts = (params: any) => request.get<ApiResponse<PageResult<any>>>('/masterdata/local-concepts/unmapped', { params })
export const translateLocalCode = (params: any) => request.get<ApiResponse<any>>('/masterdata/local-concepts/translate', { params })
export const getLocalConceptStats = (institutionId: number, codeSystemId: number) => request.get<ApiResponse<Record<string, number>>>('/masterdata/local-concepts/stats', { params: { institutionId, codeSystemId } })
export const createLocalConcept = (data: any) => request.post<ApiResponse<any>>('/masterdata/local-concepts', data)
export const batchCreateLocalConcepts = (data: any[]) => request.post<ApiResponse<any[]>>('/masterdata/local-concepts/batch', data)
export const updateLocalConcept = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/local-concepts/${id}`, data)

// Import
export const uploadMasterData = (file: File, codeSystemId: number) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<any>>(`/masterdata/import/upload?codeSystemId=${codeSystemId}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const getImportTaskStatus = (taskId: number) => request.get<ApiResponse<any>>(`/masterdata/import/tasks/${taskId}`)
