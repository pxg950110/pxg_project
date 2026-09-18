import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ==================== 药品字典 ====================

export interface Drug {
  id: number
  drugCode: string
  name: string
  tradeName?: string
  nameEn?: string
  namePinyin?: string
  categoryId?: number
  categoryName?: string
  atcCode?: string
  dosageForm?: string
  specification?: string
  unit?: string
  price?: number
  insuranceCode?: string
  insuranceType?: string
  insuranceRatio?: number
  manufacturer?: string
  status: string
  isOtc?: boolean
  isPrescription?: boolean
  isControlled?: string
  remark?: string
}

export interface DrugCategory {
  id: number
  code: string
  name: string
  nameEn?: string
  parentId?: number
  level: number
  sortOrder: number
  status: string
  children?: DrugCategory[]
}

export const getDrugs = (params?: {
  categoryId?: number
  status?: string
  insuranceType?: string
  keyword?: string
  page?: number
  page_size?: number
}) => request.get<ApiResponse<PageResult<Drug>>>('/masterdata/drugs', { params })

export const getDrug = (id: number) =>
  request.get<ApiResponse<Drug>>(`/masterdata/drugs/${id}`)

export const getDrugByCode = (code: string) =>
  request.get<ApiResponse<Drug>>(`/masterdata/drugs/code/${code}`)

export const searchDrugs = (params: { keyword: string; page?: number; page_size?: number }) =>
  request.get<ApiResponse<PageResult<Drug>>>('/masterdata/drugs/search', { params })

export const getDrugsByAtc = (atcCode: string) =>
  request.get<ApiResponse<Drug[]>>(`/masterdata/drugs/by-atc/${atcCode}`)

export const createDrug = (data: Partial<Drug>) =>
  request.post<ApiResponse<Drug>>('/masterdata/drugs', data)

export const updateDrug = (id: number, data: Partial<Drug>) =>
  request.put<ApiResponse<Drug>>(`/masterdata/drugs/${id}`, data)

export const deleteDrug = (id: number) =>
  request.delete(`/masterdata/drugs/${id}`)

export const getDrugCategories = () =>
  request.get<ApiResponse<DrugCategory[]>>('/masterdata/drugs/categories')

export const getRootDrugCategories = () =>
  request.get<ApiResponse<DrugCategory[]>>('/masterdata/drugs/categories/root')

export const createDrugCategory = (data: Partial<DrugCategory>) =>
  request.post<ApiResponse<DrugCategory>>('/masterdata/drugs/categories', data)

export const deleteDrugCategory = (id: number) =>
  request.delete(`/masterdata/drugs/categories/${id}`)

// ==================== 诊断字典 ====================

export interface Diagnosis {
  id: number
  diagnosisCode: string
  name: string
  nameEn?: string
  namePinyin?: string
  icd10Code?: string
  icd10Name?: string
  chapterCode?: string
  chapterName?: string
  parentId?: number
  level?: number
  isMain?: boolean
  diagnosisType?: string
  status: string
  remark?: string
}

export const getDiagnoses = (params?: {
  chapterCode?: string
  status?: string
  keyword?: string
  page?: number
  page_size?: number
}) => request.get<ApiResponse<PageResult<Diagnosis>>>('/masterdata/diagnoses', { params })

export const getDiagnosis = (id: number) =>
  request.get<ApiResponse<Diagnosis>>(`/masterdata/diagnoses/${id}`)

export const getDiagnosisByIcd10 = (icd10Code: string) =>
  request.get<ApiResponse<Diagnosis>>(`/masterdata/diagnoses/by-icd10/${icd10Code}`)

export const searchDiagnoses = (params: { keyword: string; page?: number; page_size?: number }) =>
  request.get<ApiResponse<PageResult<Diagnosis>>>('/masterdata/diagnoses/search', { params })

export const getDiagnosisTree = () =>
  request.get<ApiResponse<Diagnosis[]>>('/masterdata/diagnoses/tree')

export const getDiagnosisByChapter = (chapterCode: string) =>
  request.get<ApiResponse<Diagnosis[]>>(`/masterdata/diagnoses/chapter/${chapterCode}`)

export const createDiagnosis = (data: Partial<Diagnosis>) =>
  request.post<ApiResponse<Diagnosis>>('/masterdata/diagnoses', data)

export const updateDiagnosis = (id: number, data: Partial<Diagnosis>) =>
  request.put<ApiResponse<Diagnosis>>(`/masterdata/diagnoses/${id}`, data)

export const deleteDiagnosis = (id: number) =>
  request.delete(`/masterdata/diagnoses/${id}`)

// ==================== 收费项目字典 ====================

export interface FeeItem {
  id: number
  feeCode: string
  name: string
  nameEn?: string
  namePinyin?: string
  feeCategory: string
  feeType?: string
  price?: number
  unit?: string
  insuranceCode?: string
  insuranceType?: string
  executingDept?: string
  executingDeptName?: string
  status: string
  remark?: string
}

export const getFeeItems = (params?: {
  feeCategory?: string
  feeType?: string
  keyword?: string
  page?: number
  page_size?: number
}) => request.get<ApiResponse<PageResult<FeeItem>>>('/masterdata/fee-items', { params })

export const getFeeItem = (id: number) =>
  request.get<ApiResponse<FeeItem>>(`/masterdata/fee-items/${id}`)

export const searchFeeItems = (params: { keyword: string; page?: number; page_size?: number }) =>
  request.get<ApiResponse<PageResult<FeeItem>>>('/masterdata/fee-items/search', { params })

export const getFeeCategories = () =>
  request.get<ApiResponse<string[]>>('/masterdata/fee-items/categories')

export const createFeeItem = (data: Partial<FeeItem>) =>
  request.post<ApiResponse<FeeItem>>('/masterdata/fee-items', data)

export const updateFeeItem = (id: number, data: Partial<FeeItem>) =>
  request.put<ApiResponse<FeeItem>>(`/masterdata/fee-items/${id}`, data)

export const deleteFeeItem = (id: number) =>
  request.delete(`/masterdata/fee-items/${id}`)

// ==================== 检验项目字典 ====================

export interface LabItem {
  id: number
  labCode: string
  name: string
  nameEn?: string
  loincCode?: string
  specimenType?: string
  method?: string
  resultType?: string
  unit?: string
  refRangeLow?: number
  refRangeHigh?: number
  refRangeText?: string
  criticalLow?: number
  criticalHigh?: number
  labCategory?: string
  parentId?: number
  status: string
  remark?: string
}

export const getLabItems = (params?: {
  labCategory?: string
  specimenType?: string
  keyword?: string
  page?: number
  page_size?: number
}) => request.get<ApiResponse<PageResult<LabItem>>>('/masterdata/lab-items', { params })

export const getLabItem = (id: number) =>
  request.get<ApiResponse<LabItem>>(`/masterdata/lab-items/${id}`)

export const getLabItemByLoinc = (loincCode: string) =>
  request.get<ApiResponse<LabItem>>(`/masterdata/lab-items/by-loinc/${loincCode}`)

export const searchLabItems = (params: { keyword: string; page?: number; page_size?: number }) =>
  request.get<ApiResponse<PageResult<LabItem>>>('/masterdata/lab-items/search', { params })

export const getLabCategories = () =>
  request.get<ApiResponse<string[]>>('/masterdata/lab-items/categories')

export const createLabItem = (data: Partial<LabItem>) =>
  request.post<ApiResponse<LabItem>>('/masterdata/lab-items', data)

export const updateLabItem = (id: number, data: Partial<LabItem>) =>
  request.put<ApiResponse<LabItem>>(`/masterdata/lab-items/${id}`, data)

export const deleteLabItem = (id: number) =>
  request.delete(`/masterdata/lab-items/${id}`)

// ==================== 检查项目字典 ====================

export interface ExamItem {
  id: number
  examCode: string
  name: string
  nameEn?: string
  examType: string
  bodySite?: string
  method?: string
  contrastType?: string
  equipmentType?: string
  modality?: string
  examCategory?: string
  status: string
  remark?: string
}

export const getExamItems = (params?: {
  examType?: string
  bodySite?: string
  keyword?: string
  page?: number
  page_size?: number
}) => request.get<ApiResponse<PageResult<ExamItem>>>('/masterdata/exam-items', { params })

export const getExamItem = (id: number) =>
  request.get<ApiResponse<ExamItem>>(`/masterdata/exam-items/${id}`)

export const searchExamItems = (params: { keyword: string; page?: number; page_size?: number }) =>
  request.get<ApiResponse<PageResult<ExamItem>>>('/masterdata/exam-items/search', { params })

export const getExamTypes = () =>
  request.get<ApiResponse<string[]>>('/masterdata/exam-items/exam-types')

export const createExamItem = (data: Partial<ExamItem>) =>
  request.post<ApiResponse<ExamItem>>('/masterdata/exam-items', data)

export const updateExamItem = (id: number, data: Partial<ExamItem>) =>
  request.put<ApiResponse<ExamItem>>(`/masterdata/exam-items/${id}`, data)

export const deleteExamItem = (id: number) =>
  request.delete(`/masterdata/exam-items/${id}`)