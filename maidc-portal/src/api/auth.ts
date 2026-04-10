import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  access_token: string
  refresh_token: string
  token_type: string
  expires_in: number
  user: {
    id: number
    username: string
    real_name: string
    roles: string[]
    org_id: number
  }
}

export interface UserInfo {
  id: number
  username: string
  real_name: string
  roles: string[]
  org_id: number
  permissions: string[]
}

export function login(data: LoginParams) {
  return request.post<ApiResponse<LoginResult>>('/auth/login', data)
}

export function refreshTokenApi(refresh_token: string) {
  return request.post<ApiResponse<{ access_token: string; expires_in: number }>>('/auth/refresh', { refresh_token })
}

export function logout() {
  return request.post<ApiResponse<void>>('/auth/logout')
}

export function getUserInfo() {
  return request.get<ApiResponse<UserInfo>>('/users/me')
}
