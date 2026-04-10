import type { BadgeProps } from 'ant-design-vue'

type BadgeColor = Exclude<BadgeProps['color'], undefined>

interface StatusMeta {
  text: string
  color: BadgeColor
}

// Model status
export const ModelStatusMap: Record<string, StatusMeta> = {
  DRAFT: { text: '草稿', color: 'blue' },
  TRAINING: { text: '训练中', color: 'orange' },
  EVALUATING: { text: '评估中', color: 'purple' },
  ACTIVE: { text: '已激活', color: 'green' },
}

// Version status
export const VersionStatusMap: Record<string, StatusMeta> = {
  DEVELOPING: { text: '开发中', color: 'blue' },
  TESTING: { text: '测试中', color: 'orange' },
  REVIEWING: { text: '审核中', color: 'purple' },
  APPROVED: { text: '已通过', color: 'green' },
  REJECTED: { text: '已拒绝', color: 'red' },
  DEPRECATED: { text: '已废弃', color: 'default' },
}

// Deploy status
export const DeployStatusMap: Record<string, StatusMeta> = {
  CREATING: { text: '创建中', color: 'blue' },
  STARTING: { text: '启动中', color: 'orange' },
  RUNNING: { text: '运行中', color: 'green' },
  STOPPING: { text: '停止中', color: 'orange' },
  STOPPED: { text: '已停止', color: 'default' },
}

// Evaluation status
export const EvalStatusMap: Record<string, StatusMeta> = {
  PENDING: { text: '等待中', color: 'blue' },
  RUNNING: { text: '运行中', color: 'orange' },
  COMPLETED: { text: '已完成', color: 'green' },
  FAILED: { text: '失败', color: 'red' },
}

// Approval status
export const ApprovalStatusMap: Record<string, StatusMeta> = {
  PENDING: { text: '待审批', color: 'orange' },
  APPROVED: { text: '已通过', color: 'green' },
  REJECTED: { text: '已拒绝', color: 'red' },
}

// Alert severity
export const AlertSeverityMap: Record<string, StatusMeta> = {
  INFO: { text: '信息', color: 'blue' },
  WARNING: { text: '警告', color: 'orange' },
  CRITICAL: { text: '严重', color: 'red' },
}

// Medication status
export const MedicationStatusMap: Record<string, StatusMeta> = {
  ACTIVE: { text: '使用中', color: 'green' },
  COMPLETED: { text: '已完成', color: 'blue' },
  DISCONTINUED: { text: '已停用', color: 'red' },
  ON_HOLD: { text: '暂停', color: 'orange' },
}

// Encounter status
export const EncounterStatusMap: Record<string, StatusMeta> = {
  IN_PROGRESS: { text: '进行中', color: 'green' },
  FINISHED: { text: '已完成', color: 'blue' },
  CANCELLED: { text: '已取消', color: 'red' },
  PLANNED: { text: '计划中', color: 'orange' },
}

// Connection status
export const ConnectionStatusMap: Record<string, StatusMeta> = {
  CONNECTED: { text: '已连接', color: 'green' },
  DISCONNECTED: { text: '已断开', color: 'red' },
  CONNECTING: { text: '连接中', color: 'orange' },
  ERROR: { text: '连接异常', color: 'red' },
}

// Sync task status
export const SyncTaskStatusMap: Record<string, StatusMeta> = {
  RUNNING: { text: '运行中', color: 'orange' },
  COMPLETED: { text: '已完成', color: 'green' },
  FAILED: { text: '失败', color: 'red' },
  PENDING: { text: '等待中', color: 'blue' },
  CANCELLED: { text: '已取消', color: 'default' },
}

// Quality check status
export const QualityStatusMap: Record<string, StatusMeta> = {
  PASS: { text: '通过', color: 'green' },
  WARNING: { text: '警告', color: 'orange' },
  FAIL: { text: '不通过', color: 'red' },
}

export type StatusType = 'model' | 'version' | 'deploy' | 'eval' | 'approval' | 'alert' | 'medication' | 'encounter' | 'connection' | 'sync' | 'quality'

const statusMapRegistry: Record<StatusType, Record<string, StatusMeta>> = {
  model: ModelStatusMap,
  version: VersionStatusMap,
  deploy: DeployStatusMap,
  eval: EvalStatusMap,
  approval: ApprovalStatusMap,
  alert: AlertSeverityMap,
  medication: MedicationStatusMap,
  encounter: EncounterStatusMap,
  connection: ConnectionStatusMap,
  sync: SyncTaskStatusMap,
  quality: QualityStatusMap,
}

export function getStatusMeta(status: string, type: StatusType): StatusMeta {
  const map = statusMapRegistry[type]
  return map[status] ?? { text: status, color: 'default' as BadgeColor }
}
