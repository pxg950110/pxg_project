# Plan 03: 前端实现

**Goal:** API 函数 + 重写 ClinicalSearch.vue 为智能搜索页面（单搜索框 + 统一结果列表 + 域标签 + 高亮）

**依赖:** Plan 02 完成（后端 SmartSearchController API 就绪）

---

## Task 7: API 函数

**Files:**
- Modify: `maidc-portal/src/api/data.ts`

- [ ] **Step 1: 添加 smartSearch 函数**

在 `data.ts` 中 `clinicalSearch` 函数后面添加：

```typescript
// Smart Search API
export function smartSearch(data: { keyword: string; domains?: string[]; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number }) {
  return request.post<ApiResponse<any>>('/cdr/smart-search', data)
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/api/data.ts
git commit -m "feat(search): add smartSearch API function"
```

---

## Task 8: 重写 ClinicalSearch.vue

**Files:**
- Modify: `maidc-portal/src/views/data-cdr/ClinicalSearch.vue`（完全重写）

- [ ] **Step 1: 重写为智能搜索页面**

完整替换 `ClinicalSearch.vue` 内容：

```vue
<template>
  <PageContainer title="智能检索">
    <!-- 搜索区 -->
    <div class="search-box">
      <a-input-search
        v-model:value="keyword"
        placeholder="输入关键词，跨域智能搜索..."
        enter-button="搜索"
        size="large"
        :loading="loading"
        @search="handleSearch"
        @pressEnter="handleSearch"
        style="max-width: 600px"
      />
      <a-button type="link" @click="showAdvanced = !showAdvanced">
        {{ showAdvanced ? '收起筛选' : '高级筛选' }}
      </a-button>
    </div>

    <!-- 高级筛选区 -->
    <div v-if="showAdvanced" class="advanced-filter">
      <a-form layout="inline" :model="filterForm">
        <a-form-item label="搜索域">
          <a-select
            v-model:value="selectedDomains"
            mode="multiple"
            placeholder="全部域"
            style="width: 400px"
            allow-clear
            :options="domainOptions"
          />
        </a-form-item>
        <a-form-item label="日期范围">
          <a-range-picker v-model:value="filterForm.dateRange" />
        </a-form-item>
      </a-form>
    </div>

    <!-- 统计栏 -->
    <div v-if="searched" class="stats-bar">
      <span>共找到 <b>{{ total }}</b> 条结果</span>
      <a-divider type="vertical" />
      <a-tag
        v-for="(count, domain) in aggregations"
        :key="domain"
        :color="domainColorMap[domain as string] || 'default'"
        class="agg-tag"
      >
        {{ domainLabelMap[domain as string] || domain }} {{ count }}
      </a-tag>
    </div>

    <!-- 结果列表 -->
    <a-list
      v-if="searched"
      :loading="loading"
      :data-source="results"
      :pagination="pagination"
      item-layout="vertical"
    >
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta>
            <template #title>
              <span>
                <a-tag :color="domainColorMap[item.domain]" style="margin-right: 8px">
                  {{ domainLabelMap[item.domain] || item.domain }}
                </a-tag>
                <span class="result-title">{{ item.title || '-' }}</span>
                <span class="result-subtitle">{{ item.subtitle }}</span>
              </span>
            </template>
            <template #description>
              <div v-if="item.patientId" class="result-patient">
                患者：<a @click="router.push(`/data/cdr/patients/${item.patientId}`)">{{ item.patientName || '-' }}</a>
              </div>
              <div v-if="item.headline" class="result-headline" v-html="item.headline" />
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>

    <!-- 空状态 -->
    <a-empty v-if="searched && results.length === 0" description="未找到匹配结果" style="margin-top: 40px" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import { smartSearch } from '@/api/data'

defineOptions({ name: 'ClinicalSearch' })
const router = useRouter()

// 域标签颜色映射
const domainColorMap: Record<string, string> = {
  PATIENT: 'blue',
  ENCOUNTER: 'green',
  DIAGNOSIS: 'orange',
  LAB: 'cyan',
  MEDICATION: 'purple',
  IMAGING: 'geekblue',
  SURGERY: 'red',
  PATHOLOGY: 'magenta',
  VITAL: 'lime',
  ALLERGY: 'volcano',
  NOTE: 'gold',
  PROJECT: 'teal',
  DATASET: 'blue',
}

// 域标签名称映射
const domainLabelMap: Record<string, string> = {
  PATIENT: '患者',
  ENCOUNTER: '就诊',
  DIAGNOSIS: '诊断',
  LAB: '检验',
  MEDICATION: '用药',
  IMAGING: '影像',
  SURGERY: '手术',
  PATHOLOGY: '病理',
  VITAL: '体征',
  ALLERGY: '过敏',
  NOTE: '文书',
  PROJECT: '科研项目',
  DATASET: '数据集',
}

const domainOptions = Object.entries(domainLabelMap).map(([value, label]) => ({ value, label }))

const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const total = ref(0)
const results = ref<any[]>([])
const aggregations = ref<Record<string, number>>({})

const showAdvanced = ref(false)
const selectedDomains = ref<string[]>([])
const filterForm = reactive({
  dateRange: null as any,
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`,
  onChange: (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
    doSearch()
  },
})

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const body: Record<string, any> = {
      keyword: keyword.value.trim(),
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (selectedDomains.value.length > 0) {
      body.domains = selectedDomains.value
    }
    if (filterForm.dateRange?.length === 2) {
      body.dateFrom = filterForm.dateRange[0].format('YYYY-MM-DD')
      body.dateTo = filterForm.dateRange[1].format('YYYY-MM-DD')
    }
    const res = await smartSearch(body)
    const data = res.data.data
    results.value = data.items || []
    total.value = data.total || 0
    aggregations.value = data.aggregations || {}
    pagination.total = data.total || 0
    searched.value = true
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  doSearch()
}
</script>

<style scoped>
.search-box {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.advanced-filter {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
}
.stats-bar {
  margin-bottom: 16px;
  color: #666;
}
.agg-tag {
  cursor: default;
}
.result-title {
  font-size: 15px;
  font-weight: 500;
}
.result-subtitle {
  color: #999;
  margin-left: 8px;
  font-size: 13px;
}
.result-patient {
  margin-top: 4px;
  font-size: 13px;
}
.result-headline {
  margin-top: 4px;
  color: #666;
  font-size: 13px;
}
.result-headline :deep(b) {
  color: #f50;
  font-weight: bold;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/views/data-cdr/ClinicalSearch.vue
git commit -m "feat(search): rewrite ClinicalSearch.vue as smart search with unified result list"
```

---

## Task 9: 前端构建验证

- [ ] **Step 1: 编译检查**

```bash
cd maidc-portal && npx vue-tsc --noEmit 2>&1 | head -20
```

Expected: 无错误（可能有少量类型警告）

- [ ] **Step 2: Vite 构建**

```bash
cd maidc-portal && npx vite build 2>&1 | tail -10
```

Expected: 构建成功

- [ ] **Step 3: 如有编译错误，修复后重新提交**

常见问题：
- `smartSearch` 导入路径错误
- Ant Design Vue 组件属性类型不匹配
- `v-html` 安全警告（ts_headline 输出已转义，安全使用）
