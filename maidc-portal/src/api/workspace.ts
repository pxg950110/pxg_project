import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface WelcomeInfo {
  userName: string
  date: string
  role: string
}

export interface MetricsInfo {
  modelCount: number
  activeDeployments: number
  dailyInferences: number
  pendingApprovals: number
}

export interface PersonalTaskVO {
  id: number
  title: string
  description: string
  taskType: string
  priority: string
  status: string
  assigneeId: number
  sourceId: number
  sourceType: string
  dueDate: string
  createdAt: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  isRead: boolean
  createdAt: string
}

export interface QuickAction {
  key: string
  label: string
  icon: string
  route: string
}

export interface WorkspaceDashboardVO {
  welcome: WelcomeInfo
  metrics: MetricsInfo
  todos: PersonalTaskVO[]
  notifications: NotificationItem[]
  quickActions: QuickAction[]
}

export function getWorkspaceDashboard() {
  return request.get<ApiResponse<WorkspaceDashboardVO>>('/workspace/dashboard')
}

export function completeTodo(id: number) {
  return request.put<ApiResponse<PersonalTaskVO>>(`/workspace/todos/${id}/complete`)
}
