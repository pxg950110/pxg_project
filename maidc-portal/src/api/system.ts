import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// System User APIs
export function getUsers(params: { page?: number; page_size?: number; keyword?: string; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/users', { params })
}

export function getUser(id: number) {
  return request.get<ApiResponse<any>>(`/users/${id}`)
}

export function createUser(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/users', data)
}

export function updateUser(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/users/${id}`, data)
}

export function resetPassword(id: number, data: { new_password: string }) {
  return request.put<ApiResponse<void>>(`/users/${id}/reset-password`, data)
}

// Role APIs
export function getRoles(params?: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/roles', { params })
}

export function getRole(id: number) {
  return request.get<ApiResponse<any>>(`/roles/${id}`)
}

export function createRole(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/roles', data)
}

export function updateRole(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/roles/${id}`, data)
}

export function getPermissionTree() {
  return request.get<ApiResponse<any[]>>('/roles/permissions/tree')
}

export function assignPermissions(roleId: number, permissionIds: number[]) {
  return request.put<ApiResponse<void>>(`/roles/${roleId}/permissions`, { permission_ids: permissionIds })
}

// Config APIs
export function getConfigs(params?: { page?: number; page_size?: number; group?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/system/configs', { params })
}

export function updateConfig(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/system/configs/${id}`, data)
}
