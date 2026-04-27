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

// Terminology Domains
export const getDomains = () => request.get<ApiResponse<any[]>>('/masterdata/domains')
export const createDomain = (data: any) => request.post<ApiResponse<any>>('/masterdata/domains', data)
export const updateDomain = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/domains/${id}`, data)
export const deleteDomain = (id: number) => request.delete(`/masterdata/domains/${id}`)

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

// Knowledge
export const getKnowledgeCategories = () => request.get<ApiResponse<any[]>>('/masterdata/knowledge/categories')
export const createKnowledgeCategory = (data: any) => request.post<ApiResponse<any>>('/masterdata/knowledge/categories', data)
export const updateKnowledgeCategory = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/knowledge/categories/${id}`, data)
export const deleteKnowledgeCategory = (id: number) => request.delete(`/masterdata/knowledge/categories/${id}`)
export const getKnowledgeItems = (params: any) => request.get<ApiResponse<any>>('/masterdata/knowledge', { params })
export const getKnowledgeItem = (id: number) => request.get<ApiResponse<any>>(`/masterdata/knowledge/${id}`)
export const createKnowledgeItem = (data: any) => request.post<ApiResponse<any>>('/masterdata/knowledge', data)
export const updateKnowledgeItem = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/knowledge/${id}`, data)
export const deleteKnowledgeItem = (id: number) => request.delete(`/masterdata/knowledge/${id}`)
export const getKnowledgeConcepts = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/knowledge/${id}/concepts`)
export const getKnowledgeByConcept = (conceptId: number) => request.get<ApiResponse<any[]>>(`/masterdata/knowledge/by-concept/${conceptId}`)
export const associateConcept = (id: number, data: { conceptId: number; relevance?: string }) => request.post(`/masterdata/knowledge/${id}/concepts`, data)
export const removeAssociation = (id: number) => request.delete(`/masterdata/knowledge/associations/${id}`)

// Data Elements
export const getDataElements = (params: any) => request.get<ApiResponse<PageResult<any>>>('/masterdata/data-elements', { params })
export const getDataElement = (id: number) => request.get<ApiResponse<any>>(`/masterdata/data-elements/${id}`)
export const createDataElement = (data: any) => request.post<ApiResponse<any>>('/masterdata/data-elements', data)
export const updateDataElement = (id: number, data: any) => request.put<ApiResponse<any>>(`/masterdata/data-elements/${id}`, data)
export const deleteDataElement = (id: number) => request.delete(`/masterdata/data-elements/${id}`)
export const getDataElementCategories = () => request.get<ApiResponse<string[]>>('/masterdata/data-elements/categories')
export const getDataElementStats = () => request.get<ApiResponse<Record<string, number>>>('/masterdata/data-elements/stats')
export const getDataElementValues = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/data-elements/${id}/values`)
export const updateDataElementValues = (id: number, data: any[]) => request.put<ApiResponse<any[]>>(`/masterdata/data-elements/${id}/values`, data)
export const getDataElementMappings = (id: number) => request.get<ApiResponse<any[]>>(`/masterdata/data-elements/${id}/mappings`)
export const addDataElementMapping = (id: number, data: any) => request.post<ApiResponse<any>>(`/masterdata/data-elements/${id}/mappings`, data)
export const updateDataElementMapping = (mappingId: number, mappingStatus: string) => request.put<ApiResponse<any>>(`/masterdata/data-elements/mappings/${mappingId}?mappingStatus=${mappingStatus}`)
export const deleteDataElementMapping = (mappingId: number) => request.delete(`/masterdata/data-elements/mappings/${mappingId}`)
export const getUnmappedDataElements = () => request.get<ApiResponse<any[]>>('/masterdata/data-elements/mappings/unmapped')

// Data Element Import
export const importDataElements = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<any>>('/masterdata/data-elements/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const getDataElementImportStatus = (taskId: number) =>
  request.get<ApiResponse<any>>(`/masterdata/data-elements/import/tasks/${taskId}`)

export const downloadDataElementTemplate = () =>
  request.get('/masterdata/data-elements/import/template', { responseType: 'blob' })
