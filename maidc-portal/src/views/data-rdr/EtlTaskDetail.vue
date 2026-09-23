<template>
  <PageContainer :title="task?.name || 'ETL 任务详情'" :loading="loading">
    <template #extra>
      <div class="flex items-center gap-2">
        <el-button
          v-if="task?.status === 'FAILED'"
          type="danger"
          @click="handleRetry"
          :loading="retrying"
        >
          <el-icon class="mr-1"><Refresh /></el-icon> 重试
        </el-button>
        <el-button
          v-if="task?.status === 'RUNNING'"
          @click="handlePause"
        >
          <el-icon class="mr-1"><VideoPause /></el-icon> 暂停
        </el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </template>

    <template v-if="task">
      <!-- Task Info Header -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm mb-4">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="任务名称">{{ task.name }}</el-descriptions-item>
          <el-descriptions-item label="源数据">
            <el-tag type="primary">{{ task.source_type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标">
            <el-tag type="success">{{ task.target_type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusMap[task.status] || '#94a3b8' }" />
              {{ task.status }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="调度">
            <span>{{ task.cron_expression ? `Cron: ${task.cron_expression}` : '手动执行' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="处理记录数">{{ task.records_processed?.toLocaleString() ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后执行时间">{{ formatDateTime(task.last_execution_time) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(task.created_at) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Tabs -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-tabs v-model="activeTab">
          <!-- Execution History Tab -->
          <el-tab-pane label="执行历史" name="history">
            <el-table
              :data="history"
              v-loading="historyLoading"
              size="small"
              row-key="id"
            >
              <el-table-column label="运行ID" prop="run_id" width="100" />
              <el-table-column label="开始时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.start_time) }}</template>
              </el-table-column>
              <el-table-column label="结束时间" width="170">
                <template #default="{ row }">{{ row.end_time ? formatDateTime(row.end_time) : '-' }}</template>
              </el-table-column>
              <el-table-column label="耗时" width="80">
                <template #default="{ row }">{{ row.duration ? row.duration + 's' : '-' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <span class="inline-flex items-center gap-1.5">
                    <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusMap[row.status] || '#94a3b8' }" />
                    {{ row.status }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="记录数" prop="records_processed" width="100" />
            </el-table>
          </el-tab-pane>

          <!-- Current Run Tab -->
          <el-tab-pane label="当前运行" name="current">
            <template v-if="task.status === 'RUNNING'">
              <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
                <template #header>执行进度</template>
                <el-steps :active="currentStep" process-status="process" align-center>
                  <el-step
                    v-for="(step, idx) in runSteps"
                    :key="idx"
                    :title="step.title"
                    :description="step.description"
                  >
                    <template #icon>
                      <el-icon v-if="idx === currentStep" class="is-loading"><Loading /></el-icon>
                      <el-icon v-else-if="idx < currentStep" color="#10b981"><CircleCheck /></el-icon>
                      <el-icon v-else><Clock /></el-icon>
                    </template>
                  </el-step>
                </el-steps>
                <div class="mt-6 text-center">
                  <el-progress
                    :percentage="runProgress"
                    :stroke-width="6"
                    :status="runProgress < 100 ? undefined : 'success'"
                  />
                  <p class="mt-2 text-slate-500">
                    已处理 {{ runRecordsProcessed?.toLocaleString() }} 条记录
                  </p>
                </div>
              </el-card>
            </template>
            <el-empty v-else description="当前没有正在运行的任务" :image-size="60" />
          </el-tab-pane>

          <!-- Transformation Log Tab -->
          <el-tab-pane label="转换日志" name="log">
            <div class="mb-3">
              <el-select v-model="logLevel" style="width: 120px" @change="loadLogs">
                <el-option value="" label="全部" />
                <el-option value="INFO" label="INFO" />
                <el-option value="WARN" label="WARN" />
                <el-option value="ERROR" label="ERROR" />
              </el-select>
            </div>
            <el-table
              :data="pagedLogs"
              v-loading="logsLoading"
              size="small"
              row-key="id"
            >
              <el-table-column label="时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.timestamp) }}</template>
              </el-table-column>
              <el-table-column label="级别" width="80">
                <template #default="{ row }">
                  <el-tag :type="logLevelColorMap[row.level] || 'info'" size="small">{{ row.level }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="消息">
                <template #default="{ row }">
                  <span class="log-message">{{ row.message }}</span>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              class="mt-4 justify-end"
              background
              layout="total, prev, pager, next"
              :total="logs.length"
              :current-page="logPage"
              :page-size="50"
              @current-change="(page: number) => (logPage = page)"
            />
          </el-tab-pane>

          <!-- Config Tab -->
          <el-tab-pane label="配置" name="config">
            <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
              <CodeEditor :model-value="configJson" :read-only="true" language="JSON" />
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Refresh,
  VideoPause,
  Loading,
  CircleCheck,
  Clock,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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
const logPage = ref(1)
// Current run
const runProgress = ref(0)
const runRecordsProcessed = ref(0)
const currentStep = ref(0)
let pollTimer: ReturnType<typeof setInterval> | null = null

const statusMap: Record<string, string> = {
  PENDING: '#94a3b8',
  RUNNING: '#0ea5e9',
  COMPLETED: '#10b981',
  FAILED: '#ef4444',
  PAUSED: '#f59e0b',
}

const logLevelColorMap: Record<string, string> = {
  INFO: 'primary',
  WARN: 'warning',
  ERROR: 'danger',
  DEBUG: 'info',
}

const runSteps = [
  { title: '初始化', description: '连接数据源' },
  { title: '提取', description: '读取源数据' },
  { title: '转换', description: '数据清洗映射' },
  { title: '加载', description: '写入目标库' },
  { title: '验证', description: '校验数据质量' },
]

const pagedLogs = computed(() => {
  const pageSize = 50
  const start = (logPage.value - 1) * pageSize
  return logs.value.slice(start, start + pageSize)
})

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
    logPage.value = 1
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
    ElMessage.success('任务已重新启动')
    loadTask()
  } finally {
    retrying.value = false
  }
}

async function handlePause() {
  try {
    await request.post(`/etl/tasks/${route.params.id}/pause`)
    ElMessage.info('任务已暂停')
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
