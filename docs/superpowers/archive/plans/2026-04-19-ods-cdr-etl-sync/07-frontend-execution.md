# Phase 7: 执行监控页 (EtlExecutionList.vue)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 实现执行监控页面，显示所有 ETL 执行记录，支持筛选、查看详情、取消、重试操作。

**Architecture:** 基于现有 SyncTaskList.vue 模式，增加管道名称/步骤名称筛选、进度显示、执行详情抽屉。

**Tech Stack:** Vue 3 / TypeScript / Ant Design Vue

---

## File Structure

```
maidc-portal/src/views/data-etl/
  EtlExecutionList.vue                (修改) 替换占位内容
```

---

### Task 7.1: 实现 EtlExecutionList.vue

**Files:**
- Modify: `maidc-portal/src/views/data-etl/EtlExecutionList.vue`

- [ ] **Step 1: 替换占位内容为完整页面**

```vue
<template>
  <PageContainer title="执行监控">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'pipelineName'">
          {{ record.pipelineName || `管道#${record.pipelineId}` }}
        </template>
        <template v-if="column.key === 'stepName'">
          {{ record.stepName || '管道级' }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status] || 'default'">
            {{ statusMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'triggerType'">
          <a-tag>{{ triggerTypeMap[record.triggerType] || record.triggerType }}</a-tag>
        </template>
        <template v-if="column.key === 'progress'">
          <template v-if="record.status === 'RUNNING'">
            <a-progress :percent="calcProgress(record)" size="small" />
          </template>
          <template v-else-if="record.rowsRead > 0">
            {{ record.rowsWritten }}/{{ record.rowsRead }}
          </template>
          <template v-else>-</template>
        </template>
        <template v-if="column.key === 'duration'">
          {{ calcDuration(record) }}
        </template>
        <template v-if="column.key === 'startTime'">
          {{ record.startTime ? formatDateTime(record.startTime) : '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleDetail(record)">详情</a-button>
            <a-button type="link" size="small" @click="handleViewLogs(record)">日志</a-button>
            <a-button v-if="record.status === 'RUNNING'" type="link" danger size="small"
                      @click="handleCancel(record)">取消</a-button>
            <a-button v-if="record.status === 'FAILED'" type="link" size="small"
                      @click="handleRetry(record)">重试</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 执行详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="执行详情" width="640" destroy-on-close>
      <template v-if="currentExecution">
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="管道">{{ currentExecution.pipelineName }}</a-descriptions-item>
          <a-descriptions-item label="步骤">{{ currentExecution.stepName || '管道级' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[currentExecution.status]">
              {{ statusMap[currentExecution.status] }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="触发方式">{{ triggerTypeMap[currentExecution.triggerType] }}</a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ formatDateTime(currentExecution.startTime) }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ formatDateTime(currentExecution.endTime) }}</a-descriptions-item>
          <a-descriptions-item label="读取行数">{{ currentExecution.rowsRead ?? 0 }}</a-descriptions-item>
          <a-descriptions-item label="写入行数">{{ currentExecution.rowsWritten ?? 0 }}</a-descriptions-item>
          <a-descriptions-item label="跳过行数">{{ currentExecution.rowsSkipped ?? 0 }}</a-descriptions-item>
          <a-descriptions-item label="错误行数">
            <span :style="{ color: currentExecution.rowsError > 0 ? 'red' : '' }">
              {{ currentExecution.rowsError ?? 0 }}
            </span>
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>错误信息</a-divider>
        <a-typography-paragraph
          v-if="currentExecution.errorMessage"
          :content="currentExecution.errorMessage"
          :ellipsis="{ rows: 5, expandable: true }"
          copyable
          type="secondary"
        />
        <a-empty v-else description="无错误信息" />

        <a-divider>引擎配置</a-divider>
        <a-typography-paragraph
          v-if="currentExecution.engineConfig"
          :content="currentExecution.engineConfig"
          :ellipsis="{ rows: 8, expandable: true }"
          copyable
          code
        />
        <a-empty v-else description="无引擎配置" />
      </template>
    </a-drawer>

    <!-- 日志抽屉 -->
    <a-drawer v-model:open="logVisible" title="执行日志" width="640" destroy-on-close>
      <a-spin :spinning="logLoading">
        <pre style="background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 4px;
                    font-size: 12px; max-height: 600px; overflow: auto; white-space: pre-wrap">{{ logContent }}</pre>
      </a-spin>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getEtlExecutions, getEtlExecution, getEtlExecutionLogs,
  cancelEtlExecution, retryEtlExecution,
} from '@/api/etl'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'EtlExecutionList' })

// ===== 常量 =====
const statusMap: Record<string, string> = {
  PENDING: '等待中', RUNNING: '运行中', SUCCESS: '成功',
  FAILED: '失败', CANCELLED: '已取消', SKIPPED: '已跳过',
}
const statusColorMap: Record<string, string> = {
  PENDING: 'default', RUNNING: 'processing', SUCCESS: 'success',
  FAILED: 'error', CANCELLED: 'warning', SKIPPED: 'default',
}
const triggerTypeMap: Record<string, string> = {
  MANUAL: '手动', SCHEDULE: '定时', RETRY: '重试',
}

// ===== 搜索 =====
const searchFields = [
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '等待中', value: 'PENDING' },
    { label: '运行中', value: 'RUNNING' },
    { label: '成功', value: 'SUCCESS' },
    { label: '失败', value: 'FAILED' },
    { label: '已取消', value: 'CANCELLED' },
    { label: '已跳过', value: 'SKIPPED' },
  ] },
  { name: 'triggerType', label: '触发方式', type: 'select' as const, options: [
    { label: '手动', value: 'MANUAL' },
    { label: '定时', value: 'SCHEDULE' },
    { label: '重试', value: 'RETRY' },
  ] },
]

let currentSearchParams: Record<string, any> = {}

function handleSearch(values: Record<string, any>) {
  currentSearchParams = values
  fetchData({ page: 1 })
}

function handleReset() {
  currentSearchParams = {}
  fetchData({ page: 1 })
}

// ===== 表格 =====
const columns = [
  { title: '管道', key: 'pipelineName', width: 160, ellipsis: true },
  { title: '步骤', key: 'stepName', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '触发', key: 'triggerType', width: 80 },
  { title: '进度', key: 'progress', width: 140 },
  { title: '耗时', key: 'duration', width: 80 },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getEtlExecutions({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

// ===== 详情抽屉 =====
const detailVisible = ref(false)
const currentExecution = ref<any>(null)

async function handleDetail(record: any) {
  try {
    const res = await getEtlExecution(record.id)
    currentExecution.value = res.data?.data
    detailVisible.value = true
  } catch { /* handled */ }
}

// ===== 日志抽屉 =====
const logVisible = ref(false)
const logLoading = ref(false)
const logContent = ref('')

async function handleViewLogs(record: any) {
  logVisible.value = true
  logLoading.value = true
  try {
    const res = await getEtlExecutionLogs(record.id)
    logContent.value = res.data?.data || '暂无日志'
  } catch {
    logContent.value = '日志加载失败'
  } finally {
    logLoading.value = false
  }
}

// ===== 操作 =====
async function handleCancel(record: any) {
  try {
    await cancelEtlExecution(record.id)
    message.success('已取消')
    fetchData()
  } catch { /* handled */ }
}

async function handleRetry(record: any) {
  try {
    await retryEtlExecution(record.id)
    message.success('重试已启动')
    fetchData()
  } catch { /* handled */ }
}

// ===== 辅助函数 =====
function calcProgress(record: any): number {
  if (!record.rowsRead || record.rowsRead === 0) return 0
  return Math.round((record.rowsWritten / record.rowsRead) * 100)
}

function calcDuration(record: any): string {
  const start = record.startTime ? new Date(record.startTime).getTime() : null
  const end = record.endTime ? new Date(record.endTime).getTime() : null
  if (!start) return '-'
  const ms = (end || Date.now()) - start
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${Math.round(ms / 1000)}s`
  return `${Math.round(ms / 60000)}m ${Math.round((ms % 60000) / 1000)}s`
}
</script>
```

- [ ] **Step 2: 验证前端编译**

Run: `cd maidc-portal && npm run build 2>&1 | tail -5`

Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add maidc-portal/src/views/data-etl/EtlExecutionList.vue
git commit -m "feat(etl): implement EtlExecutionList page with detail drawer and log viewer"
```

---

## Phase 7 完成标准

- [x] 执行监控页完整实现
- [x] 搜索：状态 + 触发方式筛选
- [x] 表格：管道名称、步骤名称、状态、触发方式、进度、耗时、开始时间
- [x] 进度显示：运行中显示进度条，完成显示 written/read
- [x] 详情抽屉：完整执行信息 + 错误信息 + 引擎配置
- [x] 日志抽屉：查看执行日志
- [x] 操作：取消（运行中）、重试（失败）
- [x] 耗时计算
- [x] 前端编译通过
