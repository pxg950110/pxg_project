<template>
  <PageContainer title="管道管理">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建管道
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="管道名称" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="handleConfig(row)">{{ row.name }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="引擎" width="100">
        <template #default="{ row }">
          <el-tag :type="engineTypeMap[row.engineType] || 'info'">
            {{ row.engineType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status] || 'info'">
            {{ statusMap[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="步骤数" width="90" align="center">
        <template #default="{ row }">
          <span class="step-count">{{ row.stepCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最后执行时间" width="170">
        <template #default="{ row }">
          {{ row.lastExecutionTime ? formatDateTime(row.lastExecutionTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ row.createdAt ? formatDateTime(row.createdAt) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-tooltip content="配置" placement="top">
            <el-button link type="primary" size="small" @click="handleConfig(row)">
              <el-icon><Setting /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="执行" placement="top">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 'ACTIVE'"
              @click="handleRun(row)"
            >
              <el-icon><VideoPlay /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="校验" placement="top">
            <el-button link type="primary" size="small" @click="handleValidate(row)">
              <el-icon><CircleCheck /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="复制" placement="top">
            <el-button link type="primary" size="small" @click="handleCopy(row)">
              <el-icon><CopyDocument /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown trigger="click" @command="(cmd: string | number | object) => handleMenuClick(String(cmd), row)">
            <el-button link type="primary" size="small">
              <el-icon><More /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="toggleStatus">
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                </el-dropdown-item>
                <el-dropdown-item command="edit">编辑</el-dropdown-item>
                <el-dropdown-item command="delete" class="danger-item">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="mt-4 justify-end"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      :current-page="pagination.current"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 新建/编辑管道弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="editingId !== null ? '编辑管道' : '新建管道'"
      :width="640"
      :destroy-on-close="true"
    >
      <el-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="管道名称" prop="name">
          <el-input v-model="formState.name" placeholder="请输入管道名称" />
        </el-form-item>

        <el-form-item label="数据源" prop="sourceId">
          <el-select
            v-model="formState.sourceId"
            placeholder="请选择数据源"
            :loading="dataSourceLoading"
            style="width: 100%"
          >
            <el-option v-for="ds in dataSourceOptions" :key="ds.id" :value="ds.id" :label="ds.name" />
          </el-select>
        </el-form-item>

        <el-form-item label="引擎类型" prop="engineType">
          <el-radio-group v-model="formState.engineType">
            <el-radio value="EMBULK">Embulk</el-radio>
            <el-radio value="SPARK">Spark</el-radio>
            <el-radio value="PYTHON">Python</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="同步模式" prop="syncMode">
          <el-radio-group v-model="formState.syncMode">
            <el-radio value="MANUAL">手动</el-radio>
            <el-radio value="INCREMENTAL">增量</el-radio>
            <el-radio value="FULL">全量</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="formState.syncMode === 'INCREMENTAL'" label="Cron表达式" prop="cronExpression">
          <el-input v-model="formState.cronExpression" placeholder="如: 0 0 2 * * ? (每天凌晨2点)" />
          <div class="form-help">格式: 秒 分 时 日 月 周</div>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="formState.description" type="textarea" placeholder="管道描述（可选）" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleModalCancel">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElLoading, type FormInstance, type FormItemRule } from 'element-plus'
import {
  Plus,
  Setting,
  VideoPlay,
  CircleCheck,
  CopyDocument,
  More,
} from '@element-plus/icons-vue'
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

const statusTypeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  DRAFT: 'info',
  ACTIVE: 'success',
  DISABLED: 'danger',
}

const engineTypeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  EMBULK: 'primary',
  SPARK: 'warning',
  PYTHON: 'success',
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
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getEtlPipelines({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

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

const formRules: Record<string, FormItemRule[]> = {
  name: [{ required: true, message: '请输入管道名称' }],
  sourceId: [{ required: true, message: '请选择数据源' }],
  engineType: [{ required: true, message: '请选择引擎类型' }],
  syncMode: [{ required: true, message: '请选择同步模式' }],
  cronExpression: [{
    validator: (_rule: FormItemRule, value: string) => {
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
  await formRef.value?.validate()
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
      ElMessage.success('更新成功')
    } else {
      await createEtlPipeline(data)
      ElMessage.success('创建成功')
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
    ElMessage.success('管道执行已启动')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleValidate(record: any) {
  const loadingInstance = ElLoading.service({ fullscreen: true, text: '正在校验管道配置...' })
  try {
    const res = await validateEtlPipeline(record.id)
    loadingInstance.close()
    const errors = res.data?.data
    if (errors && errors.length > 0) {
      ElMessage.warning(`校验发现 ${errors.length} 个问题`)
    } else {
      ElMessage.success('校验通过')
    }
  } catch {
    loadingInstance.close()
  }
}

async function handleCopy(record: any) {
  try {
    await copyEtlPipeline(record.id)
    ElMessage.success('管道已复制')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleToggleStatus(record: any) {
  const newStatus = record.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await updateEtlPipelineStatus(record.id, newStatus)
    ElMessage.success(newStatus === 'ACTIVE' ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  try {
    await deleteEtlPipeline(record.id)
    ElMessage.success('删除成功')
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
  ElMessageBox.confirm(
    `确定删除管道「${record.name}」？`,
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    },
  )
    .then(() => handleDelete(record))
    .catch(() => { /* user cancelled */ })
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
  color: #94a3b8;
}

.step-count {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.danger-item {
  color: #ef4444;
}
</style>
