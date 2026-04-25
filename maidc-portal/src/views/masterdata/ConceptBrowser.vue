<template>
  <PageContainer title="概念浏览">
    <!-- Top filter bar -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select v-model:value="filters.codeSystemId" placeholder="选择编码体系" allow-clear
            style="width: 100%" @change="handleFilterChange">
            <a-select-option v-for="cs in codeSystems" :key="cs.id" :value="cs.id">
              {{ cs.name }} ({{ cs.code }})
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="5">
          <a-select v-model:value="filters.domain" placeholder="选择领域" allow-clear
            style="width: 100%" @change="handleFilterChange">
            <a-select-option v-for="d in domains" :key="d" :value="d">{{ d }}</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="8">
          <a-input-search v-model:value="filters.keyword" placeholder="搜索概念编码或名称"
            enter-button @search="handleSearch" allow-clear @clear="handleSearch" />
        </a-col>
        <a-col :span="5">
          <a-space>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <!-- Main table -->
    <a-table :columns="columns" :data-source="tableData" :loading="loading"
      :pagination="pagination" @change="handleTableChange" row-key="id"
      :custom-row="(record: any) => ({ onClick: () => openDetail(record) })"
      style="cursor: pointer">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'ACTIVE' ? 'green' : record.status === 'DRAFT' ? 'orange' : 'default'">
            {{ record.status === 'ACTIVE' ? '启用' : record.status === 'DRAFT' ? '草稿' : record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'domain'">
          <a-tag>{{ record.domain || '-' }}</a-tag>
        </template>
      </template>
    </a-table>

    <!-- Detail drawer -->
    <a-drawer v-model:open="drawerVisible" :title="currentConcept?.name || '概念详情'" width="640" destroy-on-close>
      <template v-if="currentConcept">
        <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="概念编码">{{ currentConcept.conceptCode }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="currentConcept.status === 'ACTIVE' ? 'green' : 'orange'">
              {{ currentConcept.status }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="名称">{{ currentConcept.name }}</a-descriptions-item>
          <a-descriptions-item label="英文名">{{ currentConcept.nameEn || '-' }}</a-descriptions-item>
          <a-descriptions-item label="领域">{{ currentConcept.domain || '-' }}</a-descriptions-item>
          <a-descriptions-item label="编码体系">{{ getCodeSystemName(currentConcept.codeSystemId) }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="2">{{ currentConcept.description || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- Ancestors breadcrumb -->
        <div v-if="ancestors.length > 0" style="margin-bottom: 16px">
          <strong>层级路径：</strong>
          <a-breadcrumb>
            <a-breadcrumb-item v-for="a in ancestors" :key="a.id">{{ a.name }}</a-breadcrumb-item>
            <a-breadcrumb-item>{{ currentConcept.name }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <a-tabs v-model:activeKey="detailTab">
          <a-tab-pane key="properties" tab="属性">
            <div v-if="currentConcept.properties">
              <pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; font-size: 12px; overflow-x: auto">{{
                typeof currentConcept.properties === 'string'
                  ? JSON.stringify(JSON.parse(currentConcept.properties), null, 2)
                  : JSON.stringify(currentConcept.properties, null, 2)
              }}</pre>
            </div>
            <a-empty v-else description="无额外属性" />
          </a-tab-pane>
          <a-tab-pane key="mappings" tab="映射">
            <a-table :columns="mappingColumns" :data-source="conceptMappings" :loading="mappingsLoading"
              row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'mappingType'">
                  <a-tag>{{ record.mappingType }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="synonyms" tab="同义词">
            <a-table :columns="synonymColumns" :data-source="conceptSynonyms" :loading="synonymsLoading"
              row-key="id" size="small" :pagination="false" />
          </a-tab-pane>
          <a-tab-pane key="children" tab="子概念">
            <a-table :columns="childColumns" :data-source="conceptChildren" :loading="childrenLoading"
              row-key="id" size="small" :pagination="false">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'orange'">{{ record.status }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getCodeSystems, getConcepts, searchConcepts, getConcept,
  getConceptChildren, getConceptAncestors, getConceptMappings, getConceptSynonyms,
} from '@/api/masterdata'

defineOptions({ name: 'ConceptBrowser' })
const route = useRoute()

const codeSystems = ref<any[]>([])
const domains = ref<string[]>(['Diagnosis', 'Procedure', 'Laboratory', 'Medication', 'Observation', 'BodySite', 'Specimen', 'Other'])

const filters = reactive({
  codeSystemId: undefined as number | undefined,
  domain: undefined as string | undefined,
  keyword: '',
})

const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '概念编码', dataIndex: 'conceptCode', key: 'conceptCode', width: 150, ellipsis: true },
  { title: '名称', dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
  { title: '英文名', dataIndex: 'nameEn', key: 'nameEn', width: 180, ellipsis: true },
  { title: '领域', key: 'domain', width: 120 },
  { title: '状态', key: 'status', width: 80 },
]

// Drawer
const drawerVisible = ref(false)
const currentConcept = ref<any>(null)
const detailTab = ref('properties')
const ancestors = ref<any[]>([])
const conceptMappings = ref<any[]>([])
const conceptSynonyms = ref<any[]>([])
const conceptChildren = ref<any[]>([])
const mappingsLoading = ref(false)
const synonymsLoading = ref(false)
const childrenLoading = ref(false)

const mappingColumns = [
  { title: '目标编码', dataIndex: 'targetConceptCode', key: 'targetConceptCode', width: 140 },
  { title: '目标名称', dataIndex: 'targetConceptName', key: 'targetConceptName', width: 180 },
  { title: '映射类型', key: 'mappingType', width: 100 },
  { title: '目标体系', dataIndex: 'targetCodeSystem', key: 'targetCodeSystem', width: 120 },
]

const synonymColumns = [
  { title: '同义词', dataIndex: 'synonym', key: 'synonym' },
  { title: '语言', dataIndex: 'language', key: 'language', width: 80 },
]

const childColumns = [
  { title: '概念编码', dataIndex: 'conceptCode', key: 'conceptCode', width: 140 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '状态', key: 'status', width: 80 },
]

function getCodeSystemName(id: number) {
  const cs = codeSystems.value.find(c => c.id === id)
  return cs ? cs.name : String(id)
}

async function fetchCodeSystems() {
  try {
    const res = await getCodeSystems()
    codeSystems.value = res.data.data || []
  } catch { /* ignore */ }
}

async function fetchData(page = 1) {
  loading.value = true
  try {
    const params: any = { page, pageSize: pagination.pageSize }
    if (filters.codeSystemId) params.codeSystemId = filters.codeSystemId
    if (filters.domain) params.domain = filters.domain

    const res = filters.keyword
      ? await searchConcepts({ ...params, keyword: filters.keyword })
      : await getConcepts(params)

    const data = res.data.data
    tableData.value = data?.items || []
    pagination.total = data?.total || 0
    pagination.current = data?.page || page
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData(pag.current)
}

function handleFilterChange() {
  fetchData(1)
}

function handleSearch() {
  fetchData(1)
}

function handleReset() {
  filters.codeSystemId = undefined
  filters.domain = undefined
  filters.keyword = ''
  fetchData(1)
}

async function openDetail(record: any) {
  drawerVisible.value = true
  detailTab.value = 'properties'
  try {
    const res = await getConcept(record.id)
    currentConcept.value = res.data.data
  } catch {
    currentConcept.value = record
  }
  // Load ancestors
  try {
    const res = await getConceptAncestors(record.id)
    ancestors.value = res.data.data || []
  } catch { ancestors.value = [] }
  // Load mappings, synonyms, children in parallel
  loadDetailData(record.id)
}

async function loadDetailData(conceptId: number) {
  mappingsLoading.value = true
  synonymsLoading.value = true
  childrenLoading.value = true
  try {
    const [mapRes, synRes, childRes] = await Promise.allSettled([
      getConceptMappings(conceptId),
      getConceptSynonyms(conceptId),
      getConceptChildren(conceptId),
    ])
    conceptMappings.value = mapRes.status === 'fulfilled' ? (mapRes.value.data.data || []) : []
    conceptSynonyms.value = synRes.status === 'fulfilled' ? (synRes.value.data.data || []) : []
    conceptChildren.value = childRes.status === 'fulfilled' ? (childRes.value.data.data || []) : []
  } finally {
    mappingsLoading.value = false
    synonymsLoading.value = false
    childrenLoading.value = false
  }
}

onMounted(() => {
  fetchCodeSystems()
  // Pre-select code system from query param
  if (route.query.codeSystemId) {
    filters.codeSystemId = Number(route.query.codeSystemId)
  }
  fetchData()
})
</script>
