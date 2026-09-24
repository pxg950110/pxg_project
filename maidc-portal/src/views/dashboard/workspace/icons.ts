/**
 * 工作台指标卡/快捷入口的 icon key → 组件映射
 * key 与后端 WorkspaceDashboardVO.MetricCard.icon / QuickAction.icon 约定一致
 */
import {
  Calendar,
  Warning,
  UserFilled,
  CircleCheck,
  Coin,
  DocumentChecked,
  Menu,
  DataAnalysis,
  Promotion,
  Lightning,
  Lock,
  Search,
  Tickets,
  Reading,
  FirstAidKit,
  Files,
  Refresh,
  Plus,
  User,
  Document,
  Odometer,
} from '@element-plus/icons-vue'

export const workspaceIconMap: Record<string, any> = {
  schedule: Calendar,
  alert: Warning,
  team: UserFilled,
  'check-circle': CircleCheck,
  database: Coin,
  audit: DocumentChecked,
  appstore: Menu,
  experiment: DataAnalysis,
  rocket: Promotion,
  thunderbolt: Lightning,
  shield: Lock,
  search: Search,
  profile: Tickets,
  book: Reading,
  'medicine-box': FirstAidKit,
  project: Files,
  sync: Refresh,
  plus: Plus,
  user: User,
  'file-search': Document,
  dashboard: Odometer,
}

export const toneColorMap: Record<string, string> = {
  primary: '#0284c7',
  danger: '#ef4444',
  success: '#10b981',
  warning: '#f59e0b',
}
