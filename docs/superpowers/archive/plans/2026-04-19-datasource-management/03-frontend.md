# Plan 03: 前端实现

**Goal:** 动态表单渲染 + 数据源列表重构 + 健康监控 Tab

**依赖:** Plan 02 后端完成

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + ECharts (vue-echarts)

---

## Task 1: API 层更新

**Files:**
- Modify: `maidc-portal/src/api/data.ts`

- [ ] **Step 1: 在 data.ts 末尾添加新 API**

在 `// ETL APIs` 之前添加：

```typescript
// ========== Data Source Type APIs ==========
export function getDataSourceTypes() {
  return request.get<ApiResponse<any[]>>('/cdr/datasource-types')
}

export function getDataSourceType(code: string) {
  return request.get<ApiResponse<any>>(`/cdr/datasource-types/${code}`)
}

export function createDataSourceType(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/datasource-types', data)
}

export function updateDataSourceType(code: string, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/datasource-types/${code}`, data)
}

export function deleteDataSourceType(code: string) {
  return request.delete<ApiResponse<void>>(`/cdr/datasource-types/${code}`)
}

// ========== Data Source Enhanced APIs ==========
export function testConnectionPreSave(data: { type_code: string; connection_params: Record<string, any> }) {
  return request.post<ApiResponse<{ success: boolean; message: string; latencyMs?: number; details?: Record<string, any> }>>('/cdr/datasources/test-connection', data)
}

export function getDataSourceHealth(id: number, limit = 50) {
  return request.get<ApiResponse<any[]>>(`/cdr/datasources/${id}/health`, { params: { limit } })
}

export function getDataSourceHealthStats(id: number, days = 30) {
  return request.get<ApiResponse<{ totalChecks: number; successCount: number; failCount: number; availabilityRate: number; avgLatencyMs: number }>>(`/cdr/datasources/${id}/health/stats`, { params: { days } })
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/api/data.ts
git commit -m "feat(datasource): add datasource type and health API functions"
```

---

## Task 2: DynamicFormRenderer 组件

**Files:**
- Create: `maidc-portal/src/components/DynamicFormRenderer/index.vue`

- [ ] **Step 1: 创建 DynamicFormRenderer**

```vue
<template>
  <template v-for="field in schema.fields" :key="field.key">
    <!-- text -->
    <a-form-item v-if="field.type === 'text'" :label="field.label" :name="['params', field.key]"
      :rules="field.required ? [{ required: true, message: `请输入${field.label}` }] : []">
      <a-input v-model:value="params[field.key]" :placeholder="field.placeholder || `请输入${field.label}`" />
    </a-form-item>

    <!-- password -->
    <a-form-item v-else-if="field.type === 'password'" :label="field.label" :name="['params', field.key]"
      :rules="field.required ? [{ required: true, message: `请输入${field.label}` }] : []">
      <a-input-password v-model:value="params[field.key]" :placeholder="field.placeholder || `请输入${field.label}`" />
    </a-form-item>

    <!-- number -->
    <a-form-item v-else-if="field.type === 'number'" :label="field.label" :name="['params', field.key]"
      :rules="field.required ? [{ required: true, message: `请输入${field.label}` }] : []">
      <a-input-number v-model:value="params[field.key]" :min="field.min" :max="field.max"
        :placeholder="field.placeholder || `请输入${field.label}`" style="width: 100%" />
    </a-form-item>

    <!-- select -->
    <a-form-item v-else-if="field.type === 'select'" :label="field.label" :name="['params', field.key]"
      :rules="field.required ? [{ required: true, message: `请选择${field.label}` }] : []">
      <a-select v-model:value="params[field.key]" :placeholder="`请选择${field.label}`">
        <a-select-option v-for="opt in field.options" :key="opt" :value="opt">{{ opt }}</a-select-option>
      </a-select>
    </a-form-item>

    <!-- textarea -->
    <a-form-item v-else-if="field.type === 'textarea'" :label="field.label" :name="['params', field.key]">
      <a-textarea v-model:value="params[field.key]" :rows="3" :placeholder="field.placeholder" />
    </a-form-item>

    <!-- keyvalue (headers) -->
    <a-form-item v-else-if="field.type === 'keyvalue'" :label="field.label">
      <div v-for="(kv, idx) in getKeyValuePairs(field.key)" :key="idx" style="display: flex; gap: 8px; margin-bottom: 4px;">
        <a-input v-model:value="kv.key" placeholder="Key" style="flex: 1" @change="syncKeyValue(field.key)" />
        <a-input v-model:value="kv.value" placeholder="Value" style="flex: 1" @change="syncKeyValue(field.key)" />
        <a-button type="text" danger size="small" @click="removeKeyValue(field.key, idx)">删除</a-button>
      </div>
      <a-button type="dashed" size="small" @click="addKeyValue(field.key)">+ 添加</a-button>
    </a-form-item>
  </template>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

interface FieldDef {
  key: string
  label: string
  type: string
  required?: boolean
  placeholder?: string
  default?: any
  min?: number
  max?: number
  options?: string[]
}

interface Schema {
  fields: FieldDef[]
}

const props = defineProps<{
  schema: Schema
  modelValue: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>): void
}>()

const params = reactive<Record<string, any>>({})

const kvStore = reactive<Record<string, Array<{ key: string; value: string }>>>({})

// Initialize defaults from schema
watch(() => props.schema, (schema) => {
  if (!schema?.fields) return
  schema.fields.forEach(field => {
    if (field.default !== undefined && params[field.key] === undefined) {
      params[field.key] = field.default
    }
    if (field.type === 'keyvalue') {
      kvStore[field.key] = []
    }
  })
}, { immediate: true })

// Sync params back to parent
watch(params, () => {
  emit('update:modelValue', { ...params })
}, { deep: true })

// Also initialize from modelValue
watch(() => props.modelValue, (val) => {
  if (val) {
    Object.keys(val).forEach(k => {
      if (params[k] === undefined) params[k] = val[k]
    })
  }
}, { immediate: true })

function getKeyValuePairs(fieldKey: string) {
  if (!kvStore[fieldKey]) kvStore[fieldKey] = []
  return kvStore[fieldKey]
}

function addKeyValue(fieldKey: string) {
  if (!kvStore[fieldKey]) kvStore[fieldKey] = []
  kvStore[fieldKey].push({ key: '', value: '' })
  syncKeyValue(fieldKey)
}

function removeKeyValue(fieldKey: string, idx: number) {
  kvStore[fieldKey]?.splice(idx, 1)
  syncKeyValue(fieldKey)
}

function syncKeyValue(fieldKey: string) {
  const pairs = kvStore[fieldKey] || []
  const obj: Record<string, string> = {}
  pairs.forEach(kv => {
    if (kv.key) obj[kv.key] = kv.value
  })
  params[fieldKey] = Object.keys(obj).length > 0 ? obj : undefined
  emit('update:modelValue', { ...params })
}
</script>
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/components/DynamicFormRenderer/
git commit -m "feat(datasource): add DynamicFormRenderer component for schema-driven forms"
```

---

## Task 3: HealthMonitor 组件

**Files:**
- Create: `maidc-portal/src/components/HealthMonitor/index.vue`

- [ ] **Step 1: 创建 HealthMonitor**

```vue
<template>
  <a-spin :spinning="loading">
    <!-- 状态卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-statistic title="连接状态" :value="stats.availabilityRate >= 0.9 ? '正常' : '异常'"
          :value-style="{ color: stats.availabilityRate >= 0.9 ? '#52c41a' : '#ff4d4f' }" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="平均延迟" :value="Math.round(stats.avgLatencyMs || 0)" suffix="ms" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="30天可用率" :value="(stats.availabilityRate * 100).toFixed(1)" suffix="%" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="总检查次数" :value="stats.totalChecks || 0" />
      </a-col>
    </a-row>

    <!-- 延迟趋势图 -->
    <a-card title="延迟趋势" size="small" style="margin-bottom: 16px">
      <MetricChart :option="latencyChartOption" height="250px" />
    </a-card>

    <!-- 最近检查记录 -->
    <a-table :columns="healthColumns" :data-source="healthData" row-key="id" size="small"
      :pagination="{ pageSize: 10, showTotal: (t: number) => `共 ${t} 条` }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :color="record.status === 'SUCCESS' ? 'green' : 'red'"
            :text="record.status === 'SUCCESS' ? '成功' : record.status === 'TIMEOUT' ? '超时' : '失败'" />
        </template>
        <template v-if="column.key === 'latency'">
          {{ record.latency_ms != null ? `${record.latency_ms}ms` : '-' }}
        </template>
        <template v-if="column.key === 'time'">
          {{ formatDateTime(record.checked_at) }}
        </template>
      </template>
    </a-table>
  </a-spin>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getDataSourceHealth, getDataSourceHealthStats } from '@/api/data'
import { formatDateTime } from '@/utils/date'

const props = defineProps<{ sourceId: number }>()

const loading = ref(false)
const healthData = ref<any[]>([])
const stats = ref<Record<string, any>>({})

const healthColumns = [
  { title: '检查时间', key: 'time', width: 170 },
  { title: '状态', key: 'status', width: 100 },
  { title: '延迟', key: 'latency', width: 100 },
  { title: '错误信息', dataIndex: 'error_message', ellipsis: true },
]

const latencyChartOption = computed(() => {
  const data = [...healthData.value].reverse()
  return {
    tooltip: { trigger: 'axis', formatter: '{b}: {c}ms' },
    xAxis: { type: 'category', data: data.map(h => formatDateTime(h.checked_at).slice(11, 16)) },
    yAxis: { type: 'value', name: '延迟(ms)' },
    series: [{
      type: 'line', data: data.map(h => h.latency_ms),
      smooth: true, areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#1677ff' }
    }],
    grid: { left: 50, right: 20, top: 20, bottom: 30 }
  }
})

async function loadData() {
  loading.value = true
  try {
    const [healthRes, statsRes] = await Promise.all([
      getDataSourceHealth(props.sourceId, 100),
      getDataSourceHealthStats(props.sourceId, 30)
    ])
    healthData.value = healthRes.data.data
    stats.value = statsRes.data.data
  } finally {
    loading.value = false
  }
}

watch(() => props.sourceId, () => loadData(), { immediate: true })
</script>
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/components/HealthMonitor/
git commit -m "feat(datasource): add HealthMonitor component with latency chart and stats"
```

---

## Task 4: 重写 DataSourceList.vue

**Files:**
- Modify: `maidc-portal/src/views/data-cdr/DataSourceList.vue`

- [ ] **Step 1: 重写为支持动态类型的列表**

关键变更点：
1. 类型列表从 API 动态获取（不再硬编码）
2. 弹窗表单使用 DynamicFormRenderer
3. 支持创建前测试连接
4. 搜索条件的类型选项动态加载

```vue
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
          <a-tag :color="categoryColorMap[getTypeCategory(record.source_type_code)] || 'default'">
            {{ getTypeName(record.source_type_code) || record.source_type_code || record.source_type }}
          </a-tag>
        </template>
        <template v-if="column.key === 'lastSyncTime'">
          {{ record.last_sync_time ? formatDateTime(record.last_sync_time) : '-' }}
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
            <a-select-option v-for="t in dataSourceTypes" :key="t.type_code" :value="t.type_code">
              {{ t.type_name }}
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

// 类型数据
const dataSourceTypes = ref<any[]>([])
const typeMap = computed(() => {
  const m: Record<string, any> = {}
  dataSourceTypes.value.forEach(t => m[t.type_code] = t)
  return m
})
function getTypeName(code: string) { return typeMap.value[code]?.type_name }
function getTypeCategory(code: string) { return typeMap.value[code]?.category }
const categoryColorMap: Record<string, string> = { DATABASE: 'blue', API: 'green', FILE: 'orange' }

async function loadTypes() {
  const res = await getDataSourceTypes()
  dataSourceTypes.value = res.data.data
}
const typeOptions = computed(() => dataSourceTypes.value.map(t => ({ label: t.type_name, value: t.type_code })))

// 搜索
const searchFields = computed(() => [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '数据源名称' },
  { name: 'type', label: '类型', type: 'select' as const, options: typeOptions.value },
])
let currentSearchParams: Record<string, any> = {}
function handleSearch(values: Record<string, any>) { currentSearchParams = values; fetchData({ page: 1 }) }
function handleReset() { currentSearchParams = {}; fetchData({ page: 1 }) }

// 表格
const columns = [
  { title: '数据源名称', dataIndex: 'sourceName', key: 'sourceName', width: 180, ellipsis: true },
  { title: '类型', key: 'sourceTypeCode', width: 120 },
  { title: '最后同步时间', key: 'lastSyncTime', width: 170 },
  { title: '操作', key: 'action', width: 280, fixed: 'right' as const },
]
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDataSources({ page: params.page, page_size: params.pageSize, ...currentSearchParams }),
)

// 弹窗
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
  return t?.param_schema || null
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
  formState.sourceTypeCode = record.sourceTypeCode
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
      type_code: record.sourceTypeCode,
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
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/views/data-cdr/DataSourceList.vue
git commit -m "feat(datasource): rewrite DataSourceList with dynamic type selection and pre-save testing"
```

---

## Task 5: 增强 DataSourceDetail.vue

**Files:**
- Modify: `maidc-portal/src/views/data-cdr/DataSourceDetail.vue`

- [ ] **Step 1: 添加健康监控 Tab**

在现有 `<a-tabs>` 中新增一个 tab-pane：

```vue
<a-tab-pane key="health" tab="健康监控">
  <HealthMonitor :source-id="sourceId" />
</a-tab-pane>
```

在 `<script setup>` 中添加 import：
```typescript
import HealthMonitor from '@/components/HealthMonitor/index.vue'
```

同时修改基本信息卡片，从固定字段改为根据 connectionParams 动态显示：

将原有的 `sourceData.config?.host` 等硬编码字段替换为：

```vue
<!-- 替换原有的 主机/端口/数据库 描述项 -->
<a-descriptions-item label="数据源类型">
  {{ sourceData.sourceTypeCode || sourceData.source_type_code }}
</a-descriptions-item>
<a-descriptions-item :span="2" label="连接参数">
  <template v-if="sourceData.connectionParams">
    <a-tag v-for="(val, key) in (typeof sourceData.connectionParams === 'string'
      ? JSON.parse(sourceData.connectionParams) : sourceData.connectionParams)"
      :key="key" style="margin: 2px">
      {{ key }}: {{ key === 'password' ? '***' : val }}
    </a-tag>
  </template>
  <span v-else>-</span>
</a-descriptions-item>
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/views/data-cdr/DataSourceDetail.vue
git commit -m "feat(datasource): add health monitoring tab to DataSourceDetail"
```

---

## Task 6: 集成验证

- [ ] **Step 1: 启动前端开发服务器**

```bash
cd maidc-portal && npm run dev
```

- [ ] **Step 2: 验证功能**

1. 访问数据源管理页面，确认类型列表从 API 加载
2. 点击新建，选择不同类型，确认表单动态渲染
3. 填写参数后点击测试连接
4. 创建数据源
5. 进入详情页，查看健康监控 Tab
6. 返回列表，测试列表中的"测试连接"按钮

- [ ] **Step 3: 最终提交**

如果有修复，提交后打 tag：
```bash
git tag -a v0.2.0-datasource -m "feat: datasource management with dynamic types, connection testing, health monitoring"
```
