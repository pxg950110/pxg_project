import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

/**
 * Get patient encounter list with pagination
 * @param patientId Patient ID
 * @param params Pagination parameters { page: number, size: number }
 * @returns Promise with paginated encounter list
 */
export function getPatientEncounterList(patientId: number | string, params?: { page?: number; size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>(`/api/cdr/patients/${patientId}/encounters`, { params })
}

/**
 * Get encounter detail by encounter ID
 * @param encounterId Encounter ID
 * @returns Promise with encounter detail data
 */
export function getEncounterDetail(encounterId: number | string) {
  return request.get<ApiResponse<any>>(`/api/cdr/encounters/${encounterId}`)
}