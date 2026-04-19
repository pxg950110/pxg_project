<template>
  <PageContainer title="管道管理">
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
        <template v-if="column.key === 'name'">
          <a @click="handleConfig(record)">{{ record.name }}</a>
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
          <a-badge :count="record.stepCount || 0" :number-style="{ backgroundColor: '#1890ff' }" />
        </template>
        <template v-if="column.key === 'lastExecutionTime'">
          {{ record.lastExecutionTime ? formatDateTime(record.lastExecutionTime) : '-' }}
        </template>
        <template v-if="column.key === 'createdAt'">
          {{ record.createdAt ? formatDateTime(record.createdAt) : '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-tooltip title="配置">
              <a-button type="link" size="small" @click="handleConfig(record)">
                <template #icon><SettingOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="执行">
              <a-button
                type="link"
                size="small"
                :disabled="record.status !== 'ACTIVE'"
                @click="handleRun(record)"
              >
                <template #icon><PlayCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="校验">
              <a-button type="link" size="small" @click="handleValidate(record)">
                <template #icon><CheckCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="复制">
              <a-button type="link" size="small" @click="handleCopy(record)">
                <template #icon><CopyOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown>
              <a-button type="link" size="small">
                <template #icon><MoreOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }: { key: string }) => handleMenuClick(key, record)">
                  <a-menu-item key="toggleStatus">
                    {{ record.status === 'ACTIVE' ? '禁用' : '启用' }}
                  </a-menu-item>
                  <a-menu-item key="edit">编辑</a-menu-item>
                  <a-menu-item key="delete" danger>
                    <span style="color: #ff4d4f">删除</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑管道弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId !== null ? '编辑管道' : '新建管道'"
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
        <a-form-item label="管道名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入管道名称" />
        </a-form-item>

        <a-form-item label="数据源" name="sourceId">
          <a-select
            v-model:value="formState.sourceId"
            placeholder="请选择数据源"
            :loading="dataSourceLoading"
          >
            <a-select-option v-for="ds in dataSourceOptions" :key="ds.id" :value="ds.id">
              {{ ds.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="引擎类型" name="engineType">
          <a-radio-group v-model:value="formState.engineType">
            <a-radio value="EMBULK">Embulk</a-radio>
            <a-radio value="SPARK">Spark</a-radio>
            <a-radio value="PYTHON">Python</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="同步模式" name="syncMode">
          <a-radio-group v-model:value="formState.syncMode">
            <a-radio value="MANUAL">手动</a-radio>
            <a-radio value="INCREMENTAL">增量</a-radio>
            <a-radio value="FULL">全量</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="formState.syncMode === 'INCREMENTAL'" label="Cron表达式" name="cronExpression">
          <a-input v-model:value="formState.cronExpression" placeholder="如: 0 0 2 * * ? (每天凌晨2点)" />
          <div class="form-help">格式: 秒 分 时 日 月 周</div>
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" placeholder="管道描述（可选）" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, type MenuInfo } from 'ant-design-vue'
import {
  PlusOutlined,
  SettingOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  CopyOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getEtlPipelines,
  createEtlPipeline,
  updateEtlPipeline,
  deleteEtlPipeline,
  runEtlPipeline,
  validateEtlPipeline,
  copyEtlPipeline,
  updateEtlPipelineStatus,
} from '@/api/etl'
import { getDataSources } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'EtlPipelineList' })

const router = useRouter()

// ===== 常量 =====
const statusMap: Record<string, string> = {
  DRAFT: '草稿',
  ACTIVE: '启用',
  DISABLED: '禁用',
}

const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  ACTIVE: 'green',
  DISABLED: 'red',
}

const engineColorMap: Record<string, string> = {
  EMBULK: 'blue',
  SPARK: 'orange',
  PYTHON: 'green',
}

// ===== 搜索 =====
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '启用', value: 'ACTIVE' },
  { label: '禁用', value: 'DISABLED' },
]

const engineOptions = [
  { label: 'Embulk', value: 'EMBULK' },
]

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '管道名称' },
  { name: 'status', label: '状态', type: 'select' as const, options: statusOptions },
  { name: 'engineType', label: '引擎', type: 'select' as const, options: engineOptions },
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
  { title: '管道名称', dataIndex: 'pipelineName', key: 'name', width: 180, ellipsis: true },
  { title: '引擎', key: 'engineType', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '步骤数', key: 'stepCount', width: 90, align: 'center' as const },
  { title: '最后执行时间', key: 'lastExecutionTime', width: 170 },
  { title: '创建时间', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getEtlPipelines({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

// ===== 数据源选项 =====
const dataSourceOptions = ref<any[]>([])
const dataSourceLoading = ref(false)

async function loadDataSources() {
  dataSourceLoading.value = true
  try {
    const res = await getDataSources({ page: 1, page_size: 200 })
    dataSourceOptions.value = res.data?.data?.items || []
  } catch {
    // error handled by request interceptor
  } finally {
    dataSourceLoading.value = false
  }
}

// ===== 弹窗 =====
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const formState = reactive({
  name: '',
  sourceId: undefined as number | undefined,
  engineType: 'EMBULK' as string,
  syncMode: 'MANUAL' as string,
  cronExpression: '',
  description: '',
})

const formRules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入管道名称' }],
  sourceId: [{ required: true, message: '请选择数据源' }],
  engineType: [{ required: true, message: '请选择引擎类型' }],
  syncMode: [{ required: true, message: '请选择同步模式' }],
  cronExpression: [{
    validator: (_rule: Rule, value: string) => {
      if (formState.syncMode === 'INCREMENTAL' && !value) {
        return Promise.reject('增量模式下请填写Cron表达式')
      }
      return Promise.resolve()
    },
  }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, {
    name: '', sourceId: undefined, engineType: 'EMBULK',
    syncMode: 'MANUAL', cronExpression: '', description: '',
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    name: record.pipelineName || record.name,
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
    const data: Record<string, any> = {
      pipelineName: formState.name,
      sourceId: formState.sourceId,
      engineType: formState.engineType,
      syncMode: formState.syncMode,
      description: formState.description,
    }
    if (formState.syncMode === 'INCREMENTAL') {
      data.cronExpression = formState.cronExpression
    }
    if (editingId.value !== null) {
      await updateEtlPipeline(editingId.value, data)
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
    message.success('管道执行已启动')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleValidate(record: any) {
  const hide = message.loading('正在校验管道配置...', 0)
  try {
    const res = await validateEtlPipeline(record.id)
    hide()
    const errors = res.data?.data
    if (errors && errors.length > 0) {
      message.warning(`校验发现 ${errors.length} 个问题`)
    } else {
      message.success('校验通过')
    }
  } catch {
    hide()
  }
}

async function handleCopy(record: any) {
  try {
    await copyEtlPipeline(record.id)
    message.success('管道已复制')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleToggleStatus(record: any) {
  const newStatus = record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await updateEtlPipelineStatus(record.id, newStatus)
    message.success(newStatus === 'ACTIVE' ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  try {
    await deleteEtlPipeline(record.id)
    message.success('删除成功')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

function handleMenuClick(key: string, record: any) {
  switch (key) {
    case 'toggleStatus':
      handleToggleStatus(record)
      break
    case 'edit':
      handleEdit(record)
      break
    case 'delete':
      handleDeleteWithConfirm(record)
      break
  }
}

function handleDeleteWithConfirm(record: any) {
  // Use Modal.confirm for delete confirmation
  import('ant-design-vue').then(({ Modal }) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定删除管道「${record.name}」？`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => handleDelete(record),
    })
  })
}

// ===== 初始化 =====
onMounted(() => {
  fetchData()
  loadDataSources()
})
</script>

<style scoped>
.form-help {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
