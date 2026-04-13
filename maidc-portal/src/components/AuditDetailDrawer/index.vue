<template>
  <a-drawer
    :open="visible"
    title="审计详情"
    :width="640"
    @close="handleClose"
  >
    <template v-if="record">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="操作ID">{{ record.id }}</a-descriptions-item>
        <a-descriptions-item label="操作模块">{{ record.module }}</a-descriptions-item>
        <a-descriptions-item label="操作类型">{{ record.operation }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ record.username }}</a-descriptions-item>
        <a-descriptions-item label="操作时间">{{ formatTime(record.created_at) }}</a-descriptions-item>
        <a-descriptions-item label="IP地址">{{ record.ip_address }}</a-descriptions-item>
        <a-descriptions-item label="User-Agent">{{ record.user_agent }}</a-descriptions-item>
        <a-descriptions-item label="目标类型">{{ record.target_type }}</a-descriptions-item>
        <a-descriptions-item label="目标ID">{{ record.target_id }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-badge
            :color="(record.status ?? 0) >= 200 && (record.status ?? 0) < 300 ? 'green' : 'red'"
            :text="`${record.status ?? '-'}`"
          />
        </a-descriptions-item>
      </a-descriptions>

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
  </a-drawer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import JsonViewer from '@/components/JsonViewer/index.vue'

interface AuditRecord {
  id?: string | number
  module?: string
  operation?: string
  username?: string
  created_at?: string
  ip_address?: string
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

defineProps<Props>()
const emit = defineEmits<Emits>()

function handleClose() {
  emit('update:visible', false)
}

function formatTime(timestamp?: string): string {
  if (!timestamp) return '-'
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
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
</style>
