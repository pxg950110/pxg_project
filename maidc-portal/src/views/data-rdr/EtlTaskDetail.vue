<template>
  <PageContainer :title="task?.name || 'ETL 任务详情'" :loading="loading">
    <template #extra>
      <a-space>
        <a-button
          v-if="task?.status === 'FAILED'"
          type="primary"
          danger
          @click="handleRetry"
          :loading="retrying"
        >
          <ReloadOutlined /> 重试
        </a-button>
        <a-button
          v-if="task?.status === 'RUNNING'"
          @click="handlePause"
        >
          <PauseOutlined /> 暂停
        </a-button>
        <a-button @click="router.back()">返回</a-button>
      </a-space>
    </template>

    <template v-if="task">
      <!-- Task Info Header -->
      <a-card style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="任务名称">{{ task.name }}</a-descriptions-item>
          <a-descriptions-item label="源数据">
            <a-tag color="blue">{{ task.source_type }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="目标">
            <a-tag color="green">{{ task.target_type }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="statusMap[task.status] || 'default'" :text="task.status" />
          </a-descriptions-item>
          <a-descriptions-item label="调度">
            <span>{{ task.cron_expression ? `Cron: ${task.cron_expression}` : '手动执行' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="处理记录数">{{ task.records_processed?.toLocaleString() ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="最后执行时间">{{ formatDateTime(task.last_execution_time) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(task.created_at) }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Tabs -->
      <a-card>
        <a-tabs v-model:activeKey="activeTab">
          <!-- Execution History Tab -->
          <a-tab-pane key="history" tab="执行历史">
            <a-table
              :columns="historyColumns"
              :data-source="history"
              :loading="historyLoading"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-badge :status="statusMap[record.status] || 'default'" :text="record.status" />
                </template>
                <template v-if="column.key === 'start_time'">
                  {{ formatDateTime(record.start_time) }}
                </template>
                <template v-if="column.key === 'end_time'">
                  {{ record.end_time ? formatDateTime(record.end_time) : '-' }}
                </template>
                <template v-if="column.key === 'duration'">
                  {{ record.duration ? record.duration + 's' : '-' }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Current Run Tab -->
          <a-tab-pane key="current" tab="当前运行">
            <template v-if="task.status === 'RUNNING'">
              <a-card title="执行进度" size="small">
                <a-steps :current="currentStep" status="process">
                  <a-step v-for="(step, idx) in runSteps" :key="idx" :title="step.title" :description="step.description">
                    <template #icon>
                      <LoadingOutlined v-if="idx === currentStep" spin />
                      <CheckCircleOutlined v-else-if="idx < currentStep" style="color: #52c41a" />
                      <ClockCircleOutlined v-else />
                    </template>
                  </a-step>
                </a-steps>
                <div style="margin-top: 24px; text-align: center">
                  <a-progress :percent="runProgress" :status="runProgress < 100 ? 'active' : 'success'" />
                  <p style="margin-top: 8px; color: rgba(0,0,0,0.45)">
                    已处理 {{ runRecordsProcessed?.toLocaleString() }} 条记录
                  </p>
                </div>
              </a-card>
            </template>
            <a-empty v-else description="当前没有正在运行的任务" />
          </a-tab-pane>

          <!-- Transformation Log Tab -->
          <a-tab-pane key="log" tab="转换日志">
            <div style="margin-bottom: 12px">
              <a-select v-model:value="logLevel" style="width: 120px" @change="loadLogs">
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="INFO">INFO</a-select-option>
                <a-select-option value="WARN">WARN</a-select-option>
                <a-select-option value="ERROR">ERROR</a-select-option>
              </a-select>
            </div>
            <a-table
              :columns="logColumns"
              :data-source="logs"
              :loading="logsLoading"
              size="small"
              row-key="id"
              :pagination="{ pageSize: 50 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'timestamp'">
                  {{ formatDateTime(record.timestamp) }}
                </template>
                <template v-if="column.key === 'level'">
                  <a-tag :color="logLevelColorMap[record.level] || 'default'" size="small">{{ record.level }}</a-tag>
                </template>
                <template v-if="column.key === 'message'">
                  <span class="log-message">{{ record.message }}</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Config Tab -->
          <a-tab-pane key="config" tab="配置">
            <a-card size="small">
              <CodeEditor :model-value="configJson" :read-only="true" language="JSON" />
            </a-card>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ReloadOutlined,
  PauseOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import CodeEditor from '@/components/CodeEditor/index.vue'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'EtlTaskDetail' })

const route = useRoute()
const router = useRouter()

const task = ref<any>(null)
const loading = ref(false)
const retrying = ref(false)
const activeTab = ref('history')

// History
const history = ref<any[]>([])
const historyLoading = ref(false)
// Logs
const logs = ref<any[]>([])
const logsLoading = ref(false)
const logLevel = ref('')
// Current run
const runProgress = ref(0)
const runRecordsProcessed = ref(0)
const currentStep = ref(0)
let pollTimer: ReturnType<typeof setInterval> | null = null

const statusMap: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  COMPLETED: 'success',
  FAILED: 'error',
  PAUSED: 'warning',
}

const logLevelColorMap: Record<string, string> = {
  INFO: 'blue',
  WARN: 'orange',
  ERROR: 'red',
  DEBUG: 'default',
}

const runSteps = [
  { title: '初始化', description: '连接数据源' },
  { title: '提取', description: '读取源数据' },
  { title: '转换', description: '数据清洗映射' },
  { title: '加载', description: '写入目标库' },
  { title: '验证', description: '校验数据质量' },
]

const historyColumns = [
  { title: '运行ID', dataIndex: 'run_id', key: 'run_id', width: 100 },
  { title: '开始时间', dataIndex: 'start_time', key: 'start_time', width: 170 },
  { title: '结束时间', dataIndex: 'end_time', key: 'end_time', width: 170 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '记录数', dataIndex: 'records_processed', key: 'records_processed', width: 100 },
]

const logColumns = [
  { title: '时间', dataIndex: 'timestamp', key: 'timestamp', width: 170 },
  { title: '级别', dataIndex: 'level', key: 'level', width: 80 },
  { title: '消息', dataIndex: 'message', key: 'message' },
]

const configJson = computed(() => {
  if (!task.value?.config) return '{\n  \n}'
  return JSON.stringify(task.value.config, null, 2)
})

async function loadTask() {
  loading.value = true
  try {
    const res = await request.get(`/etl/tasks/${route.params.id}`)
    task.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await request.get(`/etl/tasks/${route.params.id}/runs`)
    history.value = res.data.data?.items || res.data.data || []
  } finally {
    historyLoading.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const params: Record<string, any> = {}
    if (logLevel.value) params.level = logLevel.value
    const res = await request.get(`/etl/tasks/${route.params.id}/logs`, { params })
    logs.value = res.data.data?.items || res.data.data || []
  } finally {
    logsLoading.value = false
  }
}

async function pollCurrentRun() {
  if (task.value?.status !== 'RUNNING') return
  try {
    const res = await request.get(`/etl/tasks/${route.params.id}/current-run`)
    const data = res.data.data
    if (data) {
      runProgress.value = data.progress || 0
      runRecordsProcessed.value = data.records_processed || 0
      currentStep.value = data.current_step || 0
    }
  } catch {
    // polling errors are non-critical
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(pollCurrentRun, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleRetry() {
  retrying.value = true
  try {
    await request.post(`/etl/tasks/${route.params.id}/retry`)
    message.success('任务已重新启动')
    loadTask()
  } finally {
    retrying.value = false
  }
}

async function handlePause() {
  try {
    await request.post(`/etl/tasks/${route.params.id}/pause`)
    message.info('任务已暂停')
    stopPolling()
    loadTask()
  } catch {
    // error handled by request interceptor
  }
}

watch(activeTab, (tab) => {
  if (tab === 'history' && !history.value.length) loadHistory()
  else if (tab === 'log') loadLogs()
  else if (tab === 'current' && task.value?.status === 'RUNNING') {
    pollCurrentRun()
    startPolling()
  }
})

onMounted(async () => {
  await loadTask()
  loadHistory()
  if (task.value?.status === 'RUNNING') {
    pollCurrentRun()
    startPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.log-message {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
