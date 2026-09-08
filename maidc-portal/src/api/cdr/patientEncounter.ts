import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

/**
 * Patient encounter timeline item
 */
export interface EncounterTimelineItem {
  id: number
  encounterId: string
  encounterType: string
  deptName: string
  admissionTime: string
  dischargeTime: string | null
  diagnosis: string | null
  doctorName: string | null
  status: string
}

/**
 * Patient encounter list response (PatientEncounterListDTO)
 */
export interface PatientEncounterListResponse {
  patientId: number
  patientName: string
  gender: string
  age: number
  patientNo: string
  idCard: string
  phone: string
  allergyHistory: string | null
  familyHistory: string | null
  encounters: EncounterTimelineItem[]
}

/**
 * Get patient encounter list with pagination
 * @param patientId Patient ID
 * @param params Pagination parameters { page: number, size: number }
 * @returns Promise with patient encounter list
 */
export function getPatientEncounterList(patientId: number | string, params?: { page?: number; size?: number }) {
  return request.get<ApiResponse<PatientEncounterListResponse>>(`/api/cdr/patients/${patientId}/encounters`, { params })
}

/**
 * Get encounter detail by encounter ID
 * @param encounterId Encounter ID
 * @returns Promise with encounter detail data
 */
export function getEncounterDetail(encounterId: number | string) {
  return request.get<ApiResponse<any>>(`/api/cdr/encounters/${encounterId}`)
}