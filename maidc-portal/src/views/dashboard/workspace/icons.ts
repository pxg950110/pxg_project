/**
 * 工作台指标卡/快捷入口的 icon key → 组件映射
 * key 与后端 WorkspaceDashboardVO.MetricCard.icon / QuickAction.icon 约定一致
 */
import {
  ScheduleOutlined, AlertOutlined, TeamOutlined, CheckCircleOutlined, DatabaseOutlined,
  AuditOutlined, AppstoreOutlined, ExperimentOutlined, RocketOutlined, ThunderboltOutlined,
  SafetyOutlined, SearchOutlined, ProfileOutlined, BookOutlined, MedicineBoxOutlined,
  ProjectOutlined, SyncOutlined, PlusOutlined, UserOutlined, FileSearchOutlined,
  DashboardOutlined,
} from '@ant-design/icons-vue'

export const workspaceIconMap: Record<string, any> = {
  schedule: ScheduleOutlined,
  alert: AlertOutlined,
  team: TeamOutlined,
  'check-circle': CheckCircleOutlined,
  database: DatabaseOutlined,
  audit: AuditOutlined,
  appstore: AppstoreOutlined,
  experiment: ExperimentOutlined,
  rocket: RocketOutlined,
  thunderbolt: ThunderboltOutlined,
  shield: SafetyOutlined,
  search: SearchOutlined,
  profile: ProfileOutlined,
  book: BookOutlined,
  'medicine-box': MedicineBoxOutlined,
  project: ProjectOutlined,
  sync: SyncOutlined,
  plus: PlusOutlined,
  user: UserOutlined,
  'file-search': FileSearchOutlined,
  dashboard: DashboardOutlined,
}

export const toneColorMap: Record<string, string> = {
  primary: '#1677ff',
  danger: '#cf1322',
  success: '#389e0d',
  warning: '#fa8c16',
}
