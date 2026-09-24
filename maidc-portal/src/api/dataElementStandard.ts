/**
 * 数据元标准化 API
 * 基于 WS/T 303-2023 《卫生健康信息数据元标准化规则》
 */

import request from '@/utils/request'

// ==================== 概念域 API ====================

/**
 * 概念域查询参数
 */
export interface ConceptDomainQuery {
  keyword?: string
  domainType?: 'ENUMERABLE' | 'NON_ENUMERABLE'
  status?: string
  page?: number
  page_size?: number
}

/**
 * 概念域实体
 */
export interface ConceptDomain {
  id: number
  code: string
  name: string
  nameEn?: string
  definition: string
  domainType: 'ENUMERABLE' | 'NON_ENUMERABLE'
  descriptionRule?: string
  dimension?: string
  parentId?: number
  version: string
  status: string
  registrationAuthority?: string
  remark?: string
  createdAt: string
  updatedAt: string
}

/**
 * 值含义实体
 */
export interface ValueMeaning {
  id: number
  conceptDomainId: number
  code: string
  name: string
  nameEn?: string
  definition?: string
  sortOrder: number
  isActive: boolean
}

export const conceptDomainApi = {
  list: (params: ConceptDomainQuery) =>
    request.get<{ content: ConceptDomain[]; totalElements: number }>('/masterdata/concept-domains', { params }),

  get: (id: number) =>
    request.get<ConceptDomain>(`/masterdata/concept-domains/${id}`),

  create: (data: Partial<ConceptDomain>) =>
    request.post<ConceptDomain>('/masterdata/concept-domains', data),

  update: (id: number, data: Partial<ConceptDomain>) =>
    request.put<ConceptDomain>(`/masterdata/concept-domains/${id}`, data),

  delete: (id: number) =>
    request.delete(`/masterdata/concept-domains/${id}`),

  // 值含义管理
  listValueMeanings: (conceptDomainId: number) =>
    request.get<ValueMeaning[]>(`/masterdata/concept-domains/${conceptDomainId}/value-meanings`),

  createValueMeaning: (conceptDomainId: number, data: Partial<ValueMeaning>) =>
    request.post<ValueMeaning>(`/masterdata/concept-domains/${conceptDomainId}/value-meanings`, data),

  updateValueMeaning: (id: number, data: Partial<ValueMeaning>) =>
    request.put<ValueMeaning>(`/masterdata/concept-domains/value-meanings/${id}`, data),

  deleteValueMeaning: (id: number) =>
    request.delete(`/masterdata/concept-domains/value-meanings/${id}`),
}

// ==================== 值域 API ====================

/**
 * 值域查询参数
 */
export interface ValueDomainQuery {
  keyword?: string
  domainType?: 'ENUMERABLE' | 'NON_ENUMERABLE'
  conceptDomainId?: number
  status?: string
  page?: number
  page_size?: number
}

/**
 * 值域实体
 */
export interface ValueDomain {
  id: number
  code: string
  name: string
  nameEn?: string
  definition: string
  domainType: 'ENUMERABLE' | 'NON_ENUMERABLE'
  description?: string
  dataType: string
  maxLength?: number
  minLength?: number
  format?: string
  unitOfMeasure?: string
  unitName?: string
  representationClass?: string
  conceptDomainId: number
  conceptDomainName?: string
  version: string
  status: string
  remark?: string
  createdAt: string
  updatedAt: string
}

/**
 * 允许值实体
 */
export interface PermissibleValue {
  id: number
  valueDomainId: number
  value: string
  code?: string
  valueMeaningId?: number
  valueMeaningName?: string
  sortOrder: number
  isActive: boolean
  effectiveDate?: string
  expiryDate?: string
}

export const valueDomainApi = {
  list: (params: ValueDomainQuery) =>
    request.get<{ content: ValueDomain[]; totalElements: number }>('/masterdata/value-domains', { params }),

  get: (id: number) =>
    request.get<ValueDomain>(`/masterdata/value-domains/${id}`),

  create: (data: Partial<ValueDomain>) =>
    request.post<ValueDomain>('/masterdata/value-domains', data),

  update: (id: number, data: Partial<ValueDomain>) =>
    request.put<ValueDomain>(`/masterdata/value-domains/${id}`, data),

  delete: (id: number) =>
    request.delete(`/masterdata/value-domains/${id}`),

  // 允许值管理
  listPermissibleValues: (valueDomainId: number) =>
    request.get<PermissibleValue[]>(`/masterdata/value-domains/${valueDomainId}/permissible-values`),

  createPermissibleValue: (valueDomainId: number, data: Partial<PermissibleValue>) =>
    request.post<PermissibleValue>(`/masterdata/value-domains/${valueDomainId}/permissible-values`, data),

  updatePermissibleValue: (id: number, data: Partial<PermissibleValue>) =>
    request.put<PermissibleValue>(`/masterdata/value-domains/permissible-values/${id}`, data),

  deletePermissibleValue: (id: number) =>
    request.delete(`/masterdata/value-domains/permissible-values/${id}`),

  // 批量导入允许值
  importPermissibleValues: (valueDomainId: number, values: Partial<PermissibleValue>[]) =>
    request.post(`/masterdata/value-domains/${valueDomainId}/permissible-values/batch`, values),
}

// ==================== 数据元概念 API ====================

/**
 * 数据元概念查询参数
 */
export interface DataElementConceptQuery {
  keyword?: string
  objectClassCode?: string
  propertyCode?: string
  conceptDomainId?: number
  status?: string
  page?: number
  page_size?: number
}

/**
 * 数据元概念实体
 */
export interface DataElementConcept {
  id: number
  code: string
  name: string
  nameEn?: string
  definition: string
  objectClassCode?: string
  objectClassName?: string
  objectClassDefinition?: string
  propertyCode?: string
  propertyName?: string
  propertyDefinition?: string
  conceptDomainId?: number
  conceptDomainName?: string
  version: string
  status: string
  remark?: string
  createdAt: string
  updatedAt: string
}

export const dataElementConceptApi = {
  list: (params: DataElementConceptQuery) =>
    request.get<{ content: DataElementConcept[]; totalElements: number }>('/masterdata/data-element-concepts', { params }),

  get: (id: number) =>
    request.get<DataElementConcept>(`/masterdata/data-element-concepts/${id}`),

  create: (data: Partial<DataElementConcept>) =>
    request.post<DataElementConcept>('/masterdata/data-element-concepts', data),

  update: (id: number, data: Partial<DataElementConcept>) =>
    request.put<DataElementConcept>(`/masterdata/data-element-concepts/${id}`, data),

  delete: (id: number) =>
    request.delete(`/masterdata/data-element-concepts/${id}`),
}

// ==================== 数据元 API ====================

/**
 * 数据元查询参数
 */
export interface DataElementQuery {
  keyword?: string
  dataElementConceptId?: number
  valueDomainId?: number
  registrationStatus?: string
  status?: string
  page?: number
  page_size?: number
}

/**
 * 数据元实体
 */
export interface DataElement {
  id: number
  code: string
  name: string
  nameEn?: string
  identifier?: string
  version: string
  registrationAuthority?: string
  synonymName?: string
  context?: string
  definition: string
  classificationScheme?: string
  keywords?: string
  relatedDataRef?: string
  relationshipType?: string
  representationCategory?: string
  representationForm?: string
  dataType: string
  maxLength?: number
  minLength?: number
  format?: string
  dataElementConceptId?: number
  dataElementConceptName?: string
  valueDomainId?: number
  valueDomainName?: string
  governingBody?: string
  registrationStatus: string
  submittingOrganization?: string
  remark?: string
  collectionMethod?: string
  dataSource?: string
  status: string
  createdAt: string
  updatedAt: string
}

export const dataElementApi = {
  list: (params: DataElementQuery) =>
    request.get<{ content: DataElement[]; totalElements: number }>('/masterdata/data-elements', { params }),

  get: (id: number) =>
    request.get<DataElement>(`/masterdata/data-elements/${id}`),

  create: (data: Partial<DataElement>) =>
    request.post<DataElement>('/masterdata/data-elements', data),

  update: (id: number, data: Partial<DataElement>) =>
    request.put<DataElement>(`/masterdata/data-elements/${id}`, data),

  delete: (id: number) =>
    request.delete(`/masterdata/data-elements/${id}`),

  // 获取完整信息(包含概念域、值域等)
  getFull: (id: number) =>
    request.get<DataElement & {
      conceptDomainName?: string
      objectClassName?: string
      propertyName?: string
      permissibleValues?: PermissibleValue[]
    }>(`/masterdata/data-elements/${id}/full`),
}

// ==================== 对象类 API ====================

export interface ObjectClass {
  id: number
  code: string
  name: string
  nameEn?: string
  definition: string
  conceptType: 'GENERAL' | 'INDIVIDUAL'
  parentId?: number
  version: string
  status: string
  remark?: string
}

export const objectClassApi = {
  list: (params?: { keyword?: string; status?: string }) =>
    request.get<ObjectClass[]>('/masterdata/object-classes', { params }),

  get: (id: number) =>
    request.get<ObjectClass>(`/masterdata/object-classes/${id}`),

  create: (data: Partial<ObjectClass>) =>
    request.post<ObjectClass>('/masterdata/object-classes', data),

  update: (id: number, data: Partial<ObjectClass>) =>
    request.put<ObjectClass>(`/masterdata/object-classes/${id}`, data),

  delete: (id: number) =>
    request.delete(`/masterdata/object-classes/${id}`),
}

// ==================== 特性 API ====================

export interface Property {
  id: number
  code: string
  name: string
  nameEn?: string
  definition: string
  conceptType: 'GENERAL' | 'INDIVIDUAL'
  parentId?: number
  version: string
  status: string
  remark?: string
}

export const propertyApi = {
  list: (params?: { keyword?: string; status?: string }) =>
    request.get<Property[]>('/masterdata/properties', { params }),

  get: (id: number) =>
    request.get<Property>(`/masterdata/properties/${id}`),

  create: (data: Partial<Property>) =>
    request.post<Property>('/masterdata/properties', data),

  update: (id: number, data: Partial<Property>) =>
    request.put<Property>(`/masterdata/properties/${id}`, data),

  delete: (id: number) =>
    request.delete(`/masterdata/properties/${id}`),
}

// ==================== 表示类字典 API ====================

export interface RepresentationClass {
  id: number
  code: string
  name: string
  nameEn?: string
  definition?: string
  sortOrder: number
  isActive: boolean
}

export const representationClassApi = {
  list: () =>
    request.get<RepresentationClass[]>('/masterdata/representation-classes'),
}
