<template>
  <span
    class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium border transition-colors select-none"
    :class="tagClasses"
  >
    <!-- 呼吸灯状态圆点（用于正在运行、危急值、待处理等动态状态） -->
    <span
      v-if="hasDot"
      class="w-1.5 h-1.5 rounded-full"
      :class="[dotClass, { 'animate-pulse': isPulsing }]"
    />
    <span>{{ displayText }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type StatusCategory =
  | 'model'
  | 'version'
  | 'deploy'
  | 'eval'
  | 'approval'
  | 'alert'
  | 'medication'
  | 'encounter'
  | 'connection'
  | 'sync'
  | 'quality'
  | 'clinical'
  | 'followup'

interface StatusMeta {
  text: string
  color: 'emerald' | 'sky' | 'amber' | 'red' | 'slate' | 'violet'
  hasDot?: boolean
  isPulsing?: boolean
}

const props = withDefaults(
  defineProps<{
    status?: string | number
    category?: StatusCategory
    text?: string
    color?: 'emerald' | 'sky' | 'amber' | 'red' | 'slate' | 'violet'
    dot?: boolean
    pulse?: boolean
  }>(),
  {
    status: '',
    category: 'clinical',
    text: '',
    color: undefined,
    dot: undefined,
    pulse: undefined,
  }
)

const statusRegistry: Record<StatusCategory, Record<string, StatusMeta>> = {
  // 临床危急度与质控
  clinical: {
    NORMAL: { text: '正常', color: 'emerald' },
    HIGH: { text: '偏高', color: 'amber' },
    LOW: { text: '偏低', color: 'amber' },
    CRITICAL: { text: '危急值', color: 'red', hasDot: true, isPulsing: true },
    PENDING: { text: '待审核', color: 'sky' },
  },
  // 模型生命周期
  model: {
    DRAFT: { text: '草稿', color: 'slate' },
    TRAINING: { text: '训练中', color: 'amber', hasDot: true, isPulsing: true },
    EVALUATING: { text: '评估中', color: 'violet', hasDot: true, isPulsing: true },
    ACTIVE: { text: '已激活', color: 'emerald' },
    REGISTERED: { text: '已注册', color: 'sky' },
    PUBLISHED: { text: '已发布', color: 'emerald' },
    DEPRECATED: { text: '已弃用', color: 'red' },
  },
  // 版本状态
  version: {
    DEVELOPING: { text: '开发中', color: 'sky' },
    TESTING: { text: '测试中', color: 'amber' },
    REVIEWING: { text: '审核中', color: 'violet' },
    APPROVED: { text: '已通过', color: 'emerald' },
    REJECTED: { text: '已驳回', color: 'red' },
    DEPRECATED: { text: '已废弃', color: 'slate' },
  },
  // 部署服务状态
  deploy: {
    CREATING: { text: '创建中', color: 'sky', hasDot: true },
    STARTING: { text: '启动中', color: 'amber', hasDot: true, isPulsing: true },
    RUNNING: { text: '运行中', color: 'emerald', hasDot: true },
    STOPPING: { text: '停止中', color: 'amber' },
    STOPPED: { text: '已停止', color: 'slate' },
  },
  // 评估状态
  eval: {
    PENDING: { text: '等待中', color: 'sky' },
    RUNNING: { text: '评估中', color: 'amber', hasDot: true, isPulsing: true },
    COMPLETED: { text: '已完成', color: 'emerald' },
    FAILED: { text: '失败', color: 'red' },
  },
  // 伦理与上线审批
  approval: {
    PENDING: { text: '待审批', color: 'amber', hasDot: true },
    APPROVED: { text: '已批准', color: 'emerald' },
    REJECTED: { text: '已拒绝', color: 'red' },
  },
  // 告警级别
  alert: {
    INFO: { text: '提示', color: 'sky' },
    WARNING: { text: '警告', color: 'amber', hasDot: true },
    CRITICAL: { text: '严重告警', color: 'red', hasDot: true, isPulsing: true },
  },
  // 用药状态
  medication: {
    ACTIVE: { text: '在用', color: 'emerald' },
    COMPLETED: { text: '已停药', color: 'sky' },
    DISCONTINUED: { text: '医嘱终止', color: 'red' },
    ON_HOLD: { text: '暂停', color: 'amber' },
  },
  // 就诊进程
  encounter: {
    IN_PROGRESS: { text: '就诊中', color: 'sky', hasDot: true },
    FINISHED: { text: '已结诊', color: 'emerald' },
    CANCELLED: { text: '已取消', color: 'slate' },
    PLANNED: { text: '已预约', color: 'amber' },
  },
  // 连接状态
  connection: {
    CONNECTED: { text: '已连接', color: 'emerald', hasDot: true },
    DISCONNECTED: { text: '断开', color: 'slate' },
    CONNECTING: { text: '重连中', color: 'amber', hasDot: true, isPulsing: true },
    ERROR: { text: '异常', color: 'red' },
  },
  // 数据同步
  sync: {
    RUNNING: { text: '同步中', color: 'sky', hasDot: true, isPulsing: true },
    COMPLETED: { text: '同步成功', color: 'emerald' },
    FAILED: { text: '同步失败', color: 'red' },
    PENDING: { text: '排队中', color: 'slate' },
    CANCELLED: { text: '已取消', color: 'slate' },
  },
  // 质控状态
  quality: {
    PASS: { text: '质检合格', color: 'emerald' },
    WARNING: { text: '质检预警', color: 'amber' },
    FAIL: { text: '质检不合格', color: 'red' },
  },
  // 随访状态
  followup: {
    PENDING: { text: '待随访', color: 'amber', hasDot: true },
    OVERDUE: { text: '已逾期', color: 'red', hasDot: true, isPulsing: true },
    COMPLETED: { text: '已完成', color: 'emerald' },
    LOST: { text: '失访', color: 'slate' },
  },
}

const currentMeta = computed<StatusMeta>(() => {
  const code = String(props.status).toUpperCase()
  const cat = props.category
  const found = statusRegistry[cat]?.[code]
  if (found) return found

  // 默认兜底
  return {
    text: props.text || String(props.status),
    color: props.color || 'slate',
  }
})

const displayText = computed(() => props.text || currentMeta.value.text)
const activeColor = computed(() => props.color || currentMeta.value.color)

const hasDot = computed(() => {
  if (props.dot !== undefined) return props.dot
  return currentMeta.value.hasDot ?? false
})

const isPulsing = computed(() => {
  if (props.pulse !== undefined) return props.pulse
  return currentMeta.value.isPulsing ?? false
})

const colorStyles: Record<string, { tag: string; dot: string }> = {
  emerald: {
    tag: 'bg-emerald-50 text-emerald-700 border-emerald-200/80',
    dot: 'bg-emerald-500',
  },
  sky: {
    tag: 'bg-sky-50 text-sky-700 border-sky-200/80',
    dot: 'bg-sky-500',
  },
  amber: {
    tag: 'bg-amber-50 text-amber-700 border-amber-200/80',
    dot: 'bg-amber-500',
  },
  red: {
    tag: 'bg-rose-50 text-rose-700 border-rose-200/80',
    dot: 'bg-rose-500',
  },
  slate: {
    tag: 'bg-slate-100 text-slate-700 border-slate-200',
    dot: 'bg-slate-400',
  },
  violet: {
    tag: 'bg-violet-50 text-violet-700 border-violet-200/80',
    dot: 'bg-violet-500',
  },
}

const tagClasses = computed(() => colorStyles[activeColor.value]?.tag || colorStyles.slate.tag)
const dotClass = computed(() => colorStyles[activeColor.value]?.dot || colorStyles.slate.dot)
</script>
