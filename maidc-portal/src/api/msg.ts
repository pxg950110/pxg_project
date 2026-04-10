import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// Message APIs
export function getMessages(params: { page?: number; page_size?: number; type?: string; is_read?: boolean }) {
  return request.get<ApiResponse<PageResult<any>>>('/messages', { params })
}

export function markAsRead(id: number) {
  return request.put<ApiResponse<void>>(`/messages/${id}/read`)
}

export function markAllAsRead() {
  return request.put<ApiResponse<void>>('/messages/read-all')
}

export function getUnreadCount() {
  return request.get<ApiResponse<{ count: number }>>('/messages/unread-count')
}

// Notification APIs
export function getNotificationSettings() {
  return request.get<ApiResponse<any[]>>('/notifications/settings')
}

export function updateNotificationSetting(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/notifications/settings/${id}`, data)
}

export function getTemplates() {
  return request.get<ApiResponse<any[]>>('/notifications/templates')
}

export function createTemplate(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/notifications/templates', data)
}

export function updateTemplate(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/notifications/templates/${id}`, data)
}
