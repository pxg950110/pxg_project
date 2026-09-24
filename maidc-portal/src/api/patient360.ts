import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

/** 患者就诊 360（patient-360 聚合服务）API */

export interface PatientBasicInfo {
  patient_no: string
  name: string
  gender: string
  birth_date: string
  age: number
  blood_type: string
  phone: string
  id_card_no: string
  [key: string]: unknown
}

export interface EncounterStats {
  total_encounters: number
  inpatient_count: number
  outpatient_count: number
  emergency_count: number
  total_los_days: number
  first_encounter: string
  last_encounter: string
}

export interface DiagnosisMapDiagnosis {
  code: string
  name: string
  visitCount: number
  lastVisit: string | null
  isPrimary: boolean
}

/** 按身体系统聚合的诊断组（hotspot 为人体 SVG 百分比坐标，无坐标时不定位） */
export interface DiagnosisMapGroup {
  system: string
  hotspotX?: number
  hotspotY?: number
  diagnoses: DiagnosisMapDiagnosis[]
}

export interface AbnormalIndicator {
  item_name: string
  result_value: string
  unit: string
  reference_range: string
  checkup_date: string
  /** 偏高 / 偏低 / 异常（结果值对比参考范围推导，无法解析时为"异常"） */
  direction: string
}

export interface AllergyItem {
  allergen: string
  reaction: string
  severity: string
  onset_date: string
}

export interface FamilyHistoryItem {
  relation: string
  disease: string
  onset_age: number | null
  is_deceased: boolean
}

export interface TimelineItem {
  encounter_id: number
  admission_time: string
  discharge_time: string | null
  encounter_type: string
  department: string
  attending_doctor: string
  icd_code: string
  icd_name: string
  diagnosis_type: string
}

export function getPatientBasicInfo(patientId: number | string) {
  return request.get<ApiResponse<PatientBasicInfo>>(`/cdr/patient-360/${patientId}/basic-info`)
}

export function getEncounterStats(patientId: number | string) {
  return request.get<ApiResponse<EncounterStats>>(`/cdr/patient-360/${patientId}/encounter-stats`)
}

export function getDiagnosisMap(patientId: number | string) {
  return request.get<ApiResponse<DiagnosisMapGroup[]>>(`/cdr/patient-360/${patientId}/diagnosis-map`)
}

export function getAbnormalIndicators(patientId: number | string, limit = 8) {
  return request.get<ApiResponse<{ total: number; items: AbnormalIndicator[] }>>(
    `/cdr/patient-360/${patientId}/abnormal-indicators`, { params: { limit } })
}

export function getAllergies(patientId: number | string) {
  return request.get<ApiResponse<AllergyItem[]>>(`/cdr/patient-360/${patientId}/allergies`)
}

export function getFamilyHistory(patientId: number | string) {
  return request.get<ApiResponse<FamilyHistoryItem[]>>(`/cdr/patient-360/${patientId}/family-history`)
}

export function getCompletenessScore(patientId: number | string) {
  return request.get<ApiResponse<number>>(`/cdr/patient-360/${patientId}/completeness-score`)
}

export function getTimeline(patientId: number | string, params?: { startDate?: string; endDate?: string }) {
  return request.get<ApiResponse<TimelineItem[]>>(`/cdr/patient-360/${patientId}/timeline`, { params })
}
