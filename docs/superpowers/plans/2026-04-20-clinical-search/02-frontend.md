# Plan 02: 前端实现

**Goal:** API 函数 + ClinicalSearch.vue 搜索页 + 路由注册

**依赖:** Plan 01 后端完成

---

## Task 6: API 层

**Files:**
- Modify: `maidc-portal/src/api/data.ts`

- [ ] **Step 1: 在 data.ts 末尾（`// ETL APIs` 之前）添加搜索 API**

```typescript
// ========== Clinical Search API ==========
export function clinicalSearch(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/search', data)
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/api/data.ts
git commit -m "feat(cdr-search): add clinicalSearch API function"
```

---

## Task 7: ClinicalSearch.vue

**Files:**
- Create: `maidc-portal/src/views/data-cdr/ClinicalSearch.vue`

- [ ] **Step 1: 创建搜索页面**

```vue
<template>
  <PageContainer title="临床数据检索">
    <a-form layout="inline" :model="searchForm" class="search-form" @finish="handleSearch">
      <a-form-item label="数据域">
        <a-select v-model:value="searchForm.domain" style="width: 140px">
          <a-select-option v-for="d in domains" :key="d.value" :value="d.value">{{ d.label }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="关键词">
        <a-input v-model:value="searchForm.keyword" placeholder="姓名/编号/关键词" allow-clear style="width: 200px" />
      </a-form-item>
      <a-form-item label="日期范围">
        <a-range-picker v-model:value="searchForm.dateRange" />
      </a-form-item>
      <a-form-item label="科室">
        <a-input v-model:value="searchForm.department" placeholder="科室" allow-clear style="width: 140px" />
      </a-form-item>
      <a-form-item label="诊断">
        <a-input v-model:value="searchForm.diagnosis" placeholder="诊断关键词" allow-clear style="width: 160px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="searchForm.status" style="width: 120px" allow-clear placeholder="状态">
          <a-select-option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit" :loading="loading">搜索</a-button>
          <a-button @click="handleReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <div style="margin: 16px 0; color: #666">
      <template v-if="searched">
        共找到 <b>{{ total }}</b> 条结果
      </template>
      <template v-else>
        请选择数据域并输入搜索条件
      </template>
    </div>

    <a-table :columns="currentColumns" :data-source="tableData" :loading="loading" row-key="id"
      :pagination="pagination" @change="handleTableChange" :scroll="{ x: 1000 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'patientName'">
          <a @click="router.push(`/data/cdr/patients/${record.patientId}`)">{{ record.patientName }}</a>
        </template>
        <template v-if="column.key === 'gender'">
          {{ record.gender === 'M' ? '男' : record.gender === 'F' ? '女' : '未知' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="router.push(`/data/cdr/patients/${record.patientId}`)">患者详情</a-button>
        </template>
        <template v-if="column.type === 'date'">
          {{ record[column.dataIndex] ? formatDateTime(record[column.dataIndex]) : '-' }}
        </template>
      </template>
    </a-table>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import { clinicalSearch } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'ClinicalSearch' })
const router = useRouter()

const domains = [
  { value: 'PATIENT', label: '患者' },
  { value: 'ENCOUNTER', label: '就诊记录' },
  { value: 'DIAGNOSIS', label: '诊断' },
  { value: 'LAB', label: '检验报告' },
  { value: 'MEDICATION', label: '用药记录' },
  { value: 'IMAGING', label: '影像检查' },
  { value: 'SURGERY', label: '手术记录' },
  { value: 'PATHOLOGY', label: '病理报告' },
  { value: 'VITAL', label: '体征数据' },
  { value: 'ALLERGY', label: '过敏记录' },
  { value: 'NOTE', label: '临床文书' },
]

const statusOptions = ['门诊', '住院', '急诊']

// 各域列定义
const columnMap: Record<string, any[]> = {
  PATIENT: [
    { title: '患者姓名', key: 'patientName', width: 120 },
    { title: '性别', dataIndex: 'gender', key: 'gender', width: 80 },
    { title: '出生日期', dataIndex: 'birthDate', key: 'birthDate', width: 120 },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  ENCOUNTER: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '就诊类型', dataIndex: 'encounterType', key: 'encounterType', width: 100 },
    { title: '科室', dataIndex: 'department', key: 'department', width: 100 },
    { title: '主治医生', dataIndex: 'attendingDoctor', key: 'attendingDoctor', width: 100 },
    { title: '诊断摘要', dataIndex: 'diagnosisSummary', key: 'diagnosisSummary', ellipsis: true },
    { title: '入院时间', dataIndex: 'admissionTime', key: 'admissionTime', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  DIAGNOSIS: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '诊断名称', dataIndex: 'diagnosisName', key: 'diagnosisName', ellipsis: true },
    { title: 'ICD编码', dataIndex: 'diagnosisCode', key: 'diagnosisCode', width: 120 },
    { title: '诊断类型', dataIndex: 'diagnosisType', key: 'diagnosisType', width: 100 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  LAB: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '检验项目', dataIndex: 'testName', key: 'testName', width: 150 },
    { title: '标本类型', dataIndex: 'specimenType', key: 'specimenType', width: 100 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
    { title: '报告时间', dataIndex: 'reportedAt', key: 'reportedAt', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  MEDICATION: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '药品名称', dataIndex: 'medName', key: 'medName', width: 150 },
    { title: '剂量', dataIndex: 'dosage', key: 'dosage', width: 100 },
    { title: '用法', dataIndex: 'route', key: 'route', width: 80 },
    { title: '频次', dataIndex: 'frequency', key: 'frequency', width: 80 },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', type: 'date', width: 170 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  IMAGING: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '检查类型', dataIndex: 'examType', key: 'examType', width: 120 },
    { title: '检查部位', dataIndex: 'bodyPart', key: 'bodyPart', width: 100 },
    { title: '设备', dataIndex: 'modality', key: 'modality', width: 80 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
    { title: '检查日期', dataIndex: 'studyDate', key: 'studyDate', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  SURGERY: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '手术名称', dataIndex: 'operationName', key: 'operationName', ellipsis: true },
    { title: '主刀医生', dataIndex: 'surgeon', key: 'surgeon', width: 100 },
    { title: '麻醉方式', dataIndex: 'anesthesiaType', key: 'anesthesiaType', width: 100 },
    { title: '手术时间', dataIndex: 'operatedAt', key: 'operatedAt', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  PATHOLOGY: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '标本类型', dataIndex: 'specimenType', key: 'specimenType', width: 100 },
    { title: '病理诊断', dataIndex: 'diagnosisDesc', key: 'diagnosisDesc', ellipsis: true },
    { title: '分级', dataIndex: 'grade', key: 'grade', width: 80 },
    { title: '分期', dataIndex: 'stage', key: 'stage', width: 80 },
    { title: '报告日期', dataIndex: 'reportDate', key: 'reportDate', type: 'date', width: 120 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  VITAL: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '体征类型', dataIndex: 'signType', key: 'signType', width: 120 },
    { title: '测量值', dataIndex: 'signValue', key: 'signValue', width: 100 },
    { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
    { title: '测量时间', dataIndex: 'measuredAt', key: 'measuredAt', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  ALLERGY: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '过敏原', dataIndex: 'allergen', key: 'allergen', width: 150 },
    { title: '过敏类型', dataIndex: 'allergenType', key: 'allergenType', width: 100 },
    { title: '反应', dataIndex: 'reaction', key: 'reaction', width: 120 },
    { title: '严重程度', dataIndex: 'severity', key: 'severity', width: 100 },
    { title: '确认时间', dataIndex: 'confirmedAt', key: 'confirmedAt', type: 'date', width: 170 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
  NOTE: [
    { title: '患者', key: 'patientName', width: 100 },
    { title: '文书类型', dataIndex: 'noteType', key: 'noteType', width: 100 },
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '作者', dataIndex: 'author', key: 'author', width: 100 },
    { title: '文书日期', dataIndex: 'noteDate', key: 'noteDate', type: 'date', width: 120 },
    { title: '操作', key: 'action', width: 100, fixed: 'right' as const },
  ],
}

const searchForm = reactive({
  domain: 'PATIENT' as string,
  keyword: '',
  dateRange: null as any,
  department: '',
  diagnosis: '',
  status: undefined as string | undefined,
})

const loading = ref(false)
const searched = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])

const currentColumns = computed(() => columnMap[searchForm.domain] || [])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

async function doSearch(page?: number) {
  loading.value = true
  try {
    const body: Record<string, any> = {
      domain: searchForm.domain,
      keyword: searchForm.keyword || undefined,
      department: searchForm.department || undefined,
      diagnosis: searchForm.diagnosis || undefined,
      status: searchForm.status || undefined,
      page: page || pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchForm.dateRange?.length === 2) {
      body.dateFrom = searchForm.dateRange[0].format('YYYY-MM-DD')
      body.dateTo = searchForm.dateRange[1].format('YYYY-MM-DD')
    }
    const res = await clinicalSearch(body)
    const data = res.data.data
    tableData.value = data.items
    total.value = data.total
    pagination.total = data.total
    searched.value = true
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  doSearch(1)
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.dateRange = null
  searchForm.department = ''
  searchForm.diagnosis = ''
  searchForm.status = undefined
  tableData.value = []
  total.value = 0
  searched.value = false
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  doSearch(pag.current)
}
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}
.search-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/views/data-cdr/ClinicalSearch.vue
git commit -m "feat(cdr-search): add ClinicalSearch page with dynamic columns for 11 domains"
```

---

## Task 8: 路由注册

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

- [ ] **Step 1: 在 CDR children 中添加搜索路由**

在 `cdr/patients` 路由之前添加：

```typescript
{ path: 'cdr/search', name: 'ClinicalSearch', meta: { title: '临床检索' }, component: () => import('@/views/data-cdr/ClinicalSearch.vue') },
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/router/asyncRoutes.ts
git commit -m "feat(cdr-search): add /data/cdr/search route"
```

---

## Task 9: 集成验证

- [ ] **Step 1: 启动后端**

```bash
cd maidc-parent && mvn spring-boot:run -pl maidc-data
```

验证启动无报错，控制台无 mapping 冲突。

- [ ] **Step 2: 启动前端**

```bash
cd maidc-portal && npm run dev
```

- [ ] **Step 3: 功能验证**

1. 访问 `/data/cdr/search`，页面正常渲染
2. 默认域为「患者」，点击搜索
3. 切换到「就诊记录」域，输入科室，搜索
4. 切换到「诊断」域，输入诊断关键词
5. 验证各域表格列正确切换
6. 点击「患者详情」跳转到患者详情页
7. 验证分页正常
