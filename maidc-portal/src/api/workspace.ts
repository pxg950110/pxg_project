import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface WelcomeInfo {
  userName: string
  date: string
  role: string
  /** CLINICAL / RESEARCH / DATA / GOVERNANCE */
  roleGroup?: string
}

export interface MetricsInfo {
  modelCount: number
  activeDeployments: number
  dailyInferences: number
  pendingApprovals: number
}

/** 角色化指标卡，服务端按角色组下发 */
export interface MetricCard {
  key: string
  label: string
  value: number
  suffix: string
  icon: string
  route?: string
  /** primary / danger / success / warning */
  tone?: string
}

export interface TodoStats {
  today: number
  overdue: number
  total: number
}

/** 工作台待办：通用待办 + 随访待办（随访扩展字段仅 taskType=FOLLOWUP 时有值） */
export interface WorkspaceTodo {
  id: number
  taskType: string
  title: string
  priority: string
  status: string
  sourceId?: number
  sourceType?: string
  dueDate?: string
  createdAt?: string
  patientId?: number
  patientName?: string
  stageName?: string
  scales?: string[]
  overdueDays?: number
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  isRead: boolean
  createdAt: string
  bizId?: number
  bizType?: string
}

export interface QuickAction {
  key: string
  label: string
  icon: string
  route: string
  /** 前端 hasPermission 兜底过滤用 */
  permission?: string
}

export interface WorkspaceDashboardVO {
  welcome: WelcomeInfo
  metrics: MetricsInfo
  /** v2 角色化卡片；旧响应无此字段时前端回退 metrics 渲染 */
  cards?: MetricCard[] | null
  todos: WorkspaceTodo[]
  todoStats?: TodoStats | null
  notifications: NotificationItem[]
  quickActions: QuickAction[]
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

export function getWorkspaceDashboard() {
  return request.get<ApiResponse<WorkspaceDashboardVO>>('/workspace/dashboard')
}

export function completeTodo(id: number) {
  return request.put<ApiResponse<PersonalTaskVO>>(`/workspace/todos/${id}/complete`)
}
