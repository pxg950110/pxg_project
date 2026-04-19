<template>
  <PageContainer title="数据源管理">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新建数据源
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading"
      :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sourceTypeCode'">
          <a-tag :color="categoryColorMap[getTypeCategory(record.sourceTypeCode)] || 'default'">
            {{ getTypeName(record.sourceTypeCode) || record.sourceType || record.source_type }}
          </a-tag>
        </template>
        <template v-if="column.key === 'lastSyncTime'">
          {{ record.lastSyncTime ? formatDateTime(record.lastSyncTime) : '-' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="handleTestConnection(record)">测试连接</a-button>
            <a-button type="link" size="small" @click="router.push(`/etl/datasources/${record.id}`)">详情</a-button>
            <a-popconfirm title="确定删除此数据源？" @confirm="handleDelete(record)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑数据源' : '新建数据源'"
      :confirm-loading="submitLoading" :width="720" @ok="handleSubmit" @cancel="handleModalCancel"
      destroy-on-close>
      <a-form ref="formRef" :model="formState" :rules="formRules"
        :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="数据源名称" name="sourceName">
          <a-input v-model:value="formState.sourceName" placeholder="请输入数据源名称" />
        </a-form-item>
        <a-form-item label="数据源类型" name="sourceTypeCode">
          <a-select v-model:value="formState.sourceTypeCode" placeholder="请选择类型"
            :disabled="isEdit" @change="handleTypeChange">
            <a-select-option v-for="t in dataSourceTypes" :key="t.typeCode" :value="t.typeCode">
              {{ t.typeName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="可选描述" />
        </a-form-item>

        <template v-if="currentSchema">
          <a-divider orientation="left">连接配置</a-divider>
          <DynamicFormRenderer :schema="currentSchema" v-model="formState.connectionParams" />
        </template>

        <div v-if="formState.sourceTypeCode" style="text-align: center; margin: 12px 0;">
          <a-button @click="handleTestPreSave" :loading="testLoading">
            测试连接
          </a-button>
          <span v-if="testResult" :style="{ marginLeft: '12px', color: testResult.success ? '#52c41a' : '#ff4d4f' }">
            {{ testResult.success ? `连接成功 (${testResult.latencyMs}ms)` : `失败: ${testResult.message}` }}
          </span>
        </div>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import DynamicFormRenderer from '@/components/DynamicFormRenderer/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getDataSources, createDataSource, updateDataSource, deleteDataSource,
  getDataSourceTypes, testConnectionPreSave,
} from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'DataSourceList' })
const router = useRouter()

const dataSourceTypes = ref<any[]>([])
const typeMap = computed(() => {
  const m: Record<string, any> = {}
  dataSourceTypes.value.forEach(t => m[t.typeCode] = t)
  return m
})
function getTypeName(code: string) { return typeMap.value[code]?.typeName }
function getTypeCategory(code: string) { return typeMap.value[code]?.category }
const categoryColorMap: Record<string, string> = { DATABASE: 'blue', API: 'green', FILE: 'orange' }

async function loadTypes() {
  const res = await getDataSourceTypes()
  dataSourceTypes.value = res.data.data
}
const typeOptions = computed(() => dataSourceTypes.value.map(t => ({ label: t.typeName, value: t.typeCode })))

const searchFields = computed(() => [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '数据源名称' },
  { name: 'type', label: '类型', type: 'select' as const, options: typeOptions.value },
])
let currentSearchParams: Record<string, any> = {}
function handleSearch(values: Record<string, any>) { currentSearchParams = values; fetchData({ page: 1 }) }
function handleReset() { currentSearchParams = {}; fetchData({ page: 1 }) }

const columns = [
  { title: '数据源名称', dataIndex: 'sourceName', key: 'sourceName', width: 180, ellipsis: true },
  { title: '类型', key: 'sourceTypeCode', width: 120 },
  { title: '最后同步时间', key: 'lastSyncTime', width: 170 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const },
]
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  async (params) => {
    const res = await getDataSources({ page: params.page, page_size: params.pageSize, ...currentSearchParams })
    const page = res.data.data
    return { data: { code: res.data.code, message: res.data.message, data: { items: page.content, total: page.totalElements, page: page.number + 1, pageSize: page.size, totalPages: page.totalPages }, traceId: res.data.traceId } }
  },
)

const modalVisible = ref(false)
const submitLoading = ref(false)
const testLoading = ref(false)
const testResult = ref<{ success: boolean; message: string; latencyMs?: number } | null>(null)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  sourceName: '',
  sourceTypeCode: undefined as string | undefined,
  description: '',
  connectionParams: {} as Record<string, any>,
})

const formRules: Record<string, Rule[]> = {
  sourceName: [{ required: true, message: '请输入数据源名称' }],
  sourceTypeCode: [{ required: true, message: '请选择数据源类型' }],
}

const currentSchema = computed(() => {
  if (!formState.sourceTypeCode) return null
  const t = typeMap.value[formState.sourceTypeCode]
  return t?.paramSchema || null
})

function handleTypeChange() {
  formState.connectionParams = {}
  testResult.value = null
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, { sourceName: '', sourceTypeCode: undefined, description: '', connectionParams: {} })
  testResult.value = null
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  formState.sourceName = record.sourceName
  formState.sourceTypeCode = record.sourceTypeCode || record.source_type_code
  formState.description = record.description || ''
  formState.connectionParams = record.connectionParams ? JSON.parse(JSON.stringify(record.connectionParams)) : {}
  testResult.value = null
  modalVisible.value = true
}

async function handleTestPreSave() {
  if (!formState.sourceTypeCode) return
  testLoading.value = true
  try {
    const res = await testConnectionPreSave({
      type_code: formState.sourceTypeCode,
      connection_params: { ...formState.connectionParams },
    })
    testResult.value = res.data.data
  } catch { testResult.value = null } finally { testLoading.value = false }
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  submitLoading.value = true
  try {
    const data = {
      sourceName: formState.sourceName,
      sourceTypeCode: formState.sourceTypeCode,
      description: formState.description,
      connectionParams: formState.connectionParams,
    }
    if (isEdit.value) {
      await updateDataSource(editingId.value!, data)
      message.success('更新成功')
    } else {
      await createDataSource(data)
      message.success('创建成功')
    }
    handleModalCancel()
    fetchData()
  } finally { submitLoading.value = false }
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
  editingId.value = null
  testResult.value = null
}

async function handleTestConnection(record: any) {
  const hide = message.loading('正在测试连接...', 0)
  try {
    const res = await testConnectionPreSave({
      type_code: record.sourceTypeCode || record.source_type_code,
      connection_params: record.connectionParams || {},
    })
    hide()
    if (res.data.data.success) message.success(`连接成功 (${res.data.data.latencyMs}ms)`)
    else message.error(`连接失败: ${res.data.data.message}`)
  } catch { hide() }
}

async function handleDelete(record: any) {
  await deleteDataSource(record.id)
  message.success('删除成功')
  fetchData()
}

onMounted(() => { loadTypes(); fetchData() })
</script>
