<template>
  <el-drawer
    :model-value="visible"
    title="审计详情"
    :width="640"
    @close="handleClose"
  >
    <template v-if="record">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="操作ID">{{ record.id }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ record.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ record.operation }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ record.username }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatTime(record.created_at) }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ record.ip_address }}</el-descriptions-item>
        <el-descriptions-item label="Trace ID">
          <span class="trace-id" title="点击复制" @click="copyTraceId(record.trace_id)">{{ record.trace_id ?? '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent">{{ record.user_agent }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ record.target_type }}</el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ record.target_id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="status-inline">
            <span class="status-dot" :style="{ background: statusColor }" />
            {{ record.status ?? '-' }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="record.request_data" class="detail-section">
        <h4>请求数据</h4>
        <JsonViewer :data="record.request_data" :collapsed="true" />
      </div>

      <div v-if="record.response_data" class="detail-section">
        <h4>响应数据</h4>
        <JsonViewer :data="record.response_data" :collapsed="true" />
      </div>

      <div v-if="record.comment" class="detail-section">
        <h4>备注</h4>
        <p class="detail-comment">{{ record.comment }}</p>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import JsonViewer from '@/components/JsonViewer/index.vue'

interface AuditRecord {
  id?: string | number
  module?: string
  operation?: string
  username?: string
  created_at?: string
  ip_address?: string
  trace_id?: string
  user_agent?: string
  target_type?: string
  target_id?: string
  status?: number
  request_data?: any
  response_data?: any
  comment?: string
  [key: string]: any
}

interface Props {
  visible: boolean
  record: AuditRecord | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const statusColor = computed(() => {
  const s = props.record?.status ?? 0
  return s >= 200 && s < 300 ? '#52c41a' : '#ff4d4f'
})

function handleClose() {
  emit('update:visible', false)
}

function formatTime(timestamp?: string): string {
  if (!timestamp) return '-'
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

function copyTraceId(traceId?: string) {
  if (!traceId) return
  if (navigator.clipboard?.writeText) {
    navigator.clipboard
      .writeText(traceId)
      .then(() => ElMessage.success('已复制 Trace ID'))
      .catch(() => fallbackCopy(traceId))
  } else {
    fallbackCopy(traceId)
  }
}

function fallbackCopy(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    ElMessage.success('已复制 Trace ID')
  } finally {
    document.body.removeChild(textarea)
  }
}
</script>

<style scoped>
.detail-section {
  margin-top: 20px;
}
.detail-section h4 {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 8px;
}
.detail-comment {
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}
.status-inline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.trace-id {
  color: #08979c;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  cursor: pointer;
}
</style>
