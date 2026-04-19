# Phase 5: ETL 管道列表页 (EtlPipelineList.vue)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 实现管道列表页面，支持搜索、新建、编辑、执行、复制、删除、校验操作。

**Architecture:** 遵循 DataSourceList.vue 模式：PageContainer + SearchForm + Table + Modal。

**Tech Stack:** Vue 3 / TypeScript / Ant Design Vue

---

## File Structure

```
maidc-portal/src/views/data-etl/
  EtlPipelineList.vue                 (修改) 替换占位内容
```

---

### Task 5.1: 实现 EtlPipelineList.vue

**Files:**
- Modify: `maidc-portal/src/views/data-etl/EtlPipelineList.vue`

- [ ] **Step 1: 替换占位内容为完整页面**

```vue
<template>
  <PageContainer title="ETL管道管理">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新建管道
      </a-button>
    </template>

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
          <a @click="handleConfig(record)">{{ record.pipelineName }}</a>
        </template>
        <template v-if="column.key === 'engineType'">
          <a-tag :color="engineColorMap[record.engineType] || 'default'">
            {{ record.engineType }}
          </a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status] || 'default'">
            {{ statusMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'stepCount'">
          <a-badge :count="record.stepCount" :number-style="{ backgroundColor: '#1890ff' }" />
        </template>
        <template v-if="column.key === 'lastRunTime'">
          {{ record.lastRunTime ? formatDateTime(record.lastRunTime) : '-' }}
        </template>
        <template v-if="column.key === 'createdAt'">
          {{ formatDateTime(record.createdAt) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-tooltip title="配置">
              <a-button type="link" size="small" @click="handleConfig(record)">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="执行">
              <a-button type="link" size="small" @click="handleRun(record)" :disabled="record.status !== 'ACTIVE'">
                <PlayCircleOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="校验">
              <a-button type="link" size="small" @click="handleValidate(record)">
                <CheckCircleOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="复制">
              <a-button type="link" size="small" @click="handleCopy(record)">
                <CopyOutlined />
              </a-button>
            </a-tooltip>
            <a-dropdown>
              <a-button type="link" size="small"><MoreOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleToggleStatus(record)">
                    {{ record.status === 'ACTIVE' ? '禁用' : '启用' }}
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record)">编辑</a-menu-item>
                  <a-menu-item danger>
                    <a-popconfirm title="确定删除此管道？" @confirm="handleDelete(record)">
                      删除
                    </a-popconfirm>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑管道' : '新建管道'"
      :confirm-loading="submitLoading"
      :width="640"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
      destroy-on-close
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="管道名称" name="pipelineName">
          <a-input v-model:value="formState.pipelineName" placeholder="请输入管道名称" />
        </a-form-item>

        <a-form-item label="数据源" name="sourceId">
          <a-select v-model:value="formState.sourceId" placeholder="请选择数据源" :disabled="isEdit">
            <a-select-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">
              {{ ds.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="引擎类型" name="engineType">
          <a-radio-group v-model:value="formState.engineType">
            <a-radio value="EMBULK">Embulk</a-radio>
            <a-radio value="SPARK" disabled>Spark</a-radio>
            <a-radio value="PYTHON" disabled>Python</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="同步模式" name="syncMode">
          <a-radio-group v-model:value="formState.syncMode">
            <a-radio value="MANUAL">手动</a-radio>
            <a-radio value="INCREMENTAL">增量</a-radio>
            <a-radio value="FULL">全量</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="formState.syncMode === 'INCREMENTAL'" label="Cron 表达式" name="cronExpression">
          <a-input v-model:value="formState.cronExpression" placeholder="如: 0 0 2 * * ?" />
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="管道描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, SettingOutlined, PlayCircleOutlined,
  CheckCircleOutlined, CopyOutlined, MoreOutlined,
} from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getEtlPipelines, createEtlPipeline, updateEtlPipeline,
  deleteEtlPipeline, runEtlPipeline, validateEtlPipeline,
  copyEtlPipeline, updateEtlPipelineStatus,
} from '@/api/etl'
import { getDataSources } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'EtlPipelineList' })

const router = useRouter()

// ===== 常量 =====
const statusMap: Record<string, string> = {
  DRAFT: '草稿', ACTIVE: '启用', DISABLED: '禁用',
}
const statusColorMap: Record<string, string> = {
  DRAFT: 'default', ACTIVE: 'success', DISABLED: 'error',
}
const engineColorMap: Record<string, string> = {
  EMBULK: 'blue', SPARK: 'orange', PYTHON: 'green',
}

// ===== 搜索 =====
const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '管道名称' },
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'DRAFT' },
    { label: '启用', value: 'ACTIVE' },
    { label: '禁用', value: 'DISABLED' },
  ] },
  { name: 'engineType', label: '引擎', type: 'select' as const, options: [
    { label: 'Embulk', value: 'EMBULK' },
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
  { title: '管道名称', dataIndex: 'pipelineName', key: 'pipelineName', width: 180, ellipsis: true },
  { title: '引擎', key: 'engineType', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '步骤数', key: 'stepCount', width: 80 },
  { title: '最后执行', key: 'lastRunTime', width: 160 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getEtlPipelines({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

// ===== 数据源列表（弹窗用） =====
const dataSources = ref<any[]>([])

async function loadDataSources() {
  try {
    const res = await getDataSources({ page: 1, page_size: 200 })
    dataSources.value = res.data?.data?.items || []
  } catch { /* ignore */ }
}

// ===== 弹窗 =====
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  pipelineName: '',
  sourceId: undefined as number | undefined,
  engineType: 'EMBULK',
  syncMode: 'MANUAL',
  cronExpression: '',
  description: '',
})

const formRules: Record<string, Rule[]> = {
  pipelineName: [{ required: true, message: '请输入管道名称' }],
  sourceId: [{ required: true, message: '请选择数据源' }],
  engineType: [{ required: true, message: '请选择引擎类型' }],
  syncMode: [{ required: true, message: '请选择同步模式' }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, {
    pipelineName: '', sourceId: undefined, engineType: 'EMBULK',
    syncMode: 'MANUAL', cronExpression: '', description: '',
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    pipelineName: record.pipelineName,
    sourceId: record.sourceId,
    engineType: record.engineType || 'EMBULK',
    syncMode: record.syncMode || 'MANUAL',
    cronExpression: record.cronExpression || '',
    description: record.description || '',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  submitLoading.value = true
  try {
    const data = { ...formState }
    if (isEdit.value) {
      await updateEtlPipeline(editingId.value!, data)
      message.success('更新成功')
    } else {
      await createEtlPipeline(data)
      message.success('创建成功')
    }
    handleModalCancel()
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
  editingId.value = null
}

// ===== 操作 =====
function handleConfig(record: any) {
  router.push({ name: 'EtlPipelineConfig', params: { id: record.id } })
}

async function handleRun(record: any) {
  try {
    await runEtlPipeline(record.id)
    message.success('执行已启动')
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function handleValidate(record: any) {
  try {
    const res = await validateEtlPipeline(record.id)
    const errors = res.data?.data || []
    if (errors.length === 0) {
      message.success('校验通过')
    } else {
      message.warning(`校验发现问题: ${errors.join('; ')}`)
    }
  } catch { /* handled */ }
}

async function handleCopy(record: any) {
  try {
    await copyEtlPipeline(record.id)
    message.success('复制成功')
    fetchData()
  } catch { /* handled */ }
}

async function handleToggleStatus(record: any) {
  const newStatus = record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await updateEtlPipelineStatus(record.id, newStatus)
  message.success('状态已更新')
  fetchData()
}

async function handleDelete(record: any) {
  await deleteEtlPipeline(record.id)
  message.success('删除成功')
  fetchData()
}

onMounted(() => {
  fetchData()
  loadDataSources()
})
</script>
```

- [ ] **Step 2: 验证前端编译**

Run: `cd maidc-portal && npm run build 2>&1 | tail -5`

Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add maidc-portal/src/views/data-etl/EtlPipelineList.vue
git commit -m "feat(etl): implement EtlPipelineList page with CRUD, run, validate, copy"
```

---

## Phase 5 完成标准

- [x] 管道列表页完整实现
- [x] 搜索：关键词 + 状态 + 引擎类型
- [x] 表格：管道名称（可点击进入配置）、引擎、状态、步骤数、最后执行时间
- [x] 操作：配置、执行、校验、复制、启用/禁用、编辑、删除
- [x] 新建/编辑弹窗：管道名称、数据源选择、引擎类型、同步模式、Cron、描述
- [x] 前端编译通过
