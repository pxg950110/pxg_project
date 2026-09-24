<template>
  <PageContainer title="数据源管理">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建数据源
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="数据源名称" prop="sourceName" width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          <el-tag :type="categoryColorMap[getTypeCategory(row.sourceTypeCode)] || 'info'" size="small">
            {{ getTypeName(row.sourceTypeCode) || row.sourceType || row.source_type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后同步时间" width="170">
        <template #default="{ row }">
          {{ row.lastSyncTime ? formatDateTime(row.lastSyncTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handleTestConnection(row)">测试连接</el-button>
          <el-button link type="primary" size="small" @click="router.push(`/etl/datasources/${row.id}`)">详情</el-button>
          <el-popconfirm title="确定删除此数据源？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
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

    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑数据源' : '新建数据源'"
      width="720px" :destroy-on-close="true">
      <el-form ref="formRef" :model="formState" :rules="formRules"
        label-width="100px">
        <el-form-item label="数据源名称" prop="sourceName">
          <el-input v-model="formState.sourceName" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="sourceTypeCode">
          <el-select v-model="formState.sourceTypeCode" placeholder="请选择类型"
            :disabled="isEdit" style="width: 100%" @change="handleTypeChange">
            <el-option v-for="t in dataSourceTypes" :key="t.typeCode" :value="t.typeCode" :label="t.typeName" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formState.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>

        <template v-if="currentSchema">
          <el-divider content-position="left">连接配置</el-divider>
          <DynamicFormRenderer :schema="currentSchema" v-model="formState.connectionParams" />
        </template>

      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <span v-if="testResult" class="test-result" :class="testResult.success ? 'test-result--ok' : 'test-result--fail'">
            {{ testResult.success ? `连接成功 (${testResult.latencyMs}ms)` : `失败: ${testResult.message}` }}
          </span>
          <el-button v-if="formState.sourceTypeCode" @click="handleTestPreSave" :loading="testLoading">
            测试连接
          </el-button>
          <el-button @click="handleModalCancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElLoading, type FormInstance, type FormItemRule } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import DynamicFormRenderer from '@/components/DynamicFormRenderer/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getDataSources, createDataSource, updateDataSource, deleteDataSource,
  getDataSourceTypes, testConnectionPreSave, testDataSourceConnection as testConnection,
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
const categoryColorMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  DATABASE: 'primary', API: 'success', FILE: 'warning',
}

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

const { tableData, loading, pagination, fetchData } = useTable<any>(
  async (params) => {
    const res = await getDataSources({ page: params.page, page_size: params.pageSize, ...currentSearchParams })
    const page = res.data.data
    return { data: { code: res.data.code, message: res.data.message, data: { ...page, items: page.content, total: page.totalElements, page: page.number + 1, pageSize: page.size }, traceId: res.data.traceId } }
  },
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

const formRules: Record<string, FormItemRule[]> = {
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
  await formRef.value?.validate()
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
      ElMessage.success('更新成功')
    } else {
      await createDataSource(data)
      ElMessage.success('创建成功')
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
  const loadingInstance = ElLoading.service({ fullscreen: true, text: '正在测试连接...' })
  try {
    const res = await testConnection(record.id)
    loadingInstance.close()
    if (res.data.data.success) ElMessage.success(`连接成功 (${res.data.data.latencyMs}ms)`)
    else ElMessage.error(`连接失败: ${res.data.data.message}`)
  } catch { loadingInstance.close() }
}

async function handleDelete(record: any) {
  await deleteDataSource(record.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => { loadTypes(); fetchData() })
</script>

<style scoped>
.test-result {
  font-size: 13px;
  margin-right: auto;
}
.test-result--ok {
  color: #10b981;
}
.test-result--fail {
  color: #ef4444;
}
</style>
