<template>
  <PageContainer title="编码体系">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增编码体系
      </a-button>
    </template>

    <div style="display: flex; gap: 16px; height: calc(100vh - 180px)">
      <!-- Left: Code System List -->
      <div style="width: 320px; flex-shrink: 0; display: flex; flex-direction: column">
        <a-input-search v-model:value="csSearch" placeholder="检索编码体系" allow-clear
          style="margin-bottom: 12px" />
        <div class="cs-list" style="flex: 1; overflow-y: auto">
          <a-spin :spinning="csLoading">
            <div
              v-for="cs in filteredCodeSystems" :key="cs.id"
              class="cs-card" :class="{ active: selectedCs?.id === cs.id }"
              @click="selectCodeSystem(cs)">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px">
                <span style="font-weight: 600; font-size: 14px">{{ cs.name }}</span>
                <div style="display: flex; gap: 4px">
                  <a-tag :color="cs.category === 'LOCAL' ? 'orange' : 'blue'" size="small">
                    {{ cs.category === 'LOCAL' ? '非标' : cs.category === 'STANDARD' ? '标准' : cs.category || '标准' }}
                  </a-tag>
                  <a-tag v-if="cs.status" :color="cs.status === 'ACTIVE' ? 'green' : cs.status === 'DRAFT' ? 'orange' : 'default'" size="small">
                    {{ cs.status === 'ACTIVE' ? '启用' : cs.status === 'DRAFT' ? '草稿' : cs.status }}
                  </a-tag>
                </div>
              </div>
              <div style="color: #999; font-size: 12px; margin-bottom: 4px">{{ cs.code }} · v{{ cs.version || '-' }}</div>
              <div style="display: flex; align-items: center; gap: 12px; font-size: 12px; color: #666">
                <span>概念: <b>{{ statsMap[cs.id]?.conceptCount ?? '-' }}</b></span>
                <span>
                  <ApartmentOutlined v-if="cs.hierarchySupport" style="color: #1890ff" />
                  {{ cs.hierarchySupport ? '层级' : '' }}
                </span>
                <a style="margin-left: auto; font-size: 12px" @click.stop="handleEdit(cs)">编辑</a>
              </div>
            </div>
            <a-empty v-if="!csLoading && filteredCodeSystems.length === 0" description="无匹配编码体系" :image-style="{ height: '40px' }" />
          </a-spin>
        </div>
      </div>

      <!-- Right: Concepts -->
      <div style="flex: 1; display: flex; flex-direction: column; min-width: 0">
        <template v-if="selectedCs">
          <!-- Filter bar -->
          <a-card :bordered="false" style="margin-bottom: 12px; padding: 8px 16px" size="small">
            <a-row :gutter="12" align="middle">
              <a-col :span="5">
                <a-select v-model:value="conceptFilters.domain" placeholder="选择领域" allow-clear
                  style="width: 100%" @change="fetchConcepts(1)">
                  <a-select-option v-for="d in domains" :key="d" :value="d">{{ domainMap[d] || d }}</a-select-option>
                </a-select>
              </a-col>
              <a-col :span="9">
                <a-input-search v-model:value="conceptFilters.keyword" placeholder="搜索概念编码或名称"
                  enter-button @search="fetchConcepts(1)" allow-clear @clear="fetchConcepts(1)" />
              </a-col>
              <a-col :span="4">
                <a-button @click="resetConceptFilters">重置</a-button>
              </a-col>
              <a-col :span="6" style="text-align: right; color: #999">
                {{ selectedCs.name }} · 共 {{ pagination.total }} 条概念
              </a-col>
            </a-row>
          </a-card>

          <!-- Concept table -->
          <a-table :columns="conceptColumns" :data-source="tableData" :loading="conceptLoading"
            :pagination="pagination" @change="handleTableChange" row-key="id"
            :custom-row="(record: any) => ({ onClick: () => openDetail(record) })"
            style="cursor: pointer; flex: 1" size="small">
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
        </template>
        <a-empty v-else description="请从左侧选择一个编码体系" style="margin-top: 120px" />
      </div>
    </div>

    <!-- Concept detail drawer -->
    <a-drawer v-model:open="drawerVisible" :title="currentConcept?.name || '概念详情'" width="640" destroy-on-close>
      <template v-if="currentConcept">
        <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="概念编码">{{ currentConcept.conceptCode }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="currentConcept.status === 'ACTIVE' ? 'green' : 'orange'">{{ currentConcept.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="名称">{{ currentConcept.name }}</a-descriptions-item>
          <a-descriptions-item label="英文名">{{ currentConcept.nameEn || '-' }}</a-descriptions-item>
          <a-descriptions-item label="领域">{{ currentConcept.domain || '-' }}</a-descriptions-item>
          <a-descriptions-item label="编码体系">{{ selectedCs?.name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="2">{{ currentConcept.description || '-' }}</a-descriptions-item>
        </a-descriptions>

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

    <!-- CodeSystem create/edit modal -->
    <a-modal v-model:open="modalVisible" :title="isEdit ? '编辑编码体系' : '新增编码体系'"
      :width="600" @ok="handleSubmit" @cancel="handleModalCancel" destroy-on-close>
      <a-form ref="formRef" :model="formState" :rules="formRules"
        :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="编码" name="code">
          <a-input v-model:value="formState.code" placeholder="如 ICD-10, LOINC, SNOMED" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="名称" name="name">
          <a-input v-model:value="formState.name" placeholder="编码体系名称" />
        </a-form-item>
        <a-form-item label="版本" name="version">
          <a-input v-model:value="formState.version" placeholder="如 2024 版" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" placeholder="编码体系描述" />
        </a-form-item>
        <a-form-item label="分类" name="category">
          <a-select v-model:value="formState.category" placeholder="选择分类">
            <a-select-option value="STANDARD">标准编码</a-select-option>
            <a-select-option value="LOCAL">非标编码</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="层级支持" name="hierarchySupport">
          <a-switch v-model:checked="formState.hierarchySupport" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ApartmentOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getCodeSystems, getCodeSystemStats, createCodeSystem, updateCodeSystem,
  getConcepts, searchConcepts, getConcept,
  getConceptChildren, getConceptAncestors, getConceptMappings, getConceptSynonyms,
  getDomains,
} from '@/api/masterdata'

defineOptions({ name: 'CodeSystems' })

// ── Left panel: Code Systems ──
const csLoading = ref(false)
const csSearch = ref('')
const codeSystems = ref<any[]>([])
const statsMap = ref<Record<number, any>>({})
const selectedCs = ref<any>(null)

const filteredCodeSystems = computed(() => {
  if (!csSearch.value) return codeSystems.value
  const kw = csSearch.value.toLowerCase()
  return codeSystems.value.filter(cs =>
    cs.name?.toLowerCase().includes(kw) || cs.code?.toLowerCase().includes(kw)
  )
})

async function fetchCodeSystems() {
  csLoading.value = true
  try {
    const res = await getCodeSystems()
    codeSystems.value = res.data.data || []
    for (const cs of codeSystems.value) {
      try {
        const statsRes = await getCodeSystemStats(cs.id)
        statsMap.value[cs.id] = statsRes.data.data
      } catch {
        statsMap.value[cs.id] = { conceptCount: 0 }
      }
    }
  } finally {
    csLoading.value = false
  }
}

function selectCodeSystem(cs: any) {
  selectedCs.value = cs
  conceptFilters.domain = undefined
  conceptFilters.keyword = ''
  fetchConcepts(1)
}

// ── Right panel: Concepts ──
const domains = ref<string[]>([])
const domainMap = ref<Record<string, string>>({})

async function fetchDomains() {
  try {
    const res = await getDomains()
    const data = res.data.data || []
    domainMap.value = Object.fromEntries(data.map((d: any) => [d.code, d.name]))
    domains.value = data.map((d: any) => d.code)
  } catch {
    domains.value = ['Diagnosis', 'Procedure', 'Laboratory', 'Medication', 'Observation', 'BodySite', 'Specimen', 'Other']
  }
}
const conceptFilters = reactive({ domain: undefined as string | undefined, keyword: '' })
const conceptLoading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const conceptColumns = [
  { title: '概念编码', dataIndex: 'conceptCode', key: 'conceptCode', width: 150, ellipsis: true },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '英文名', dataIndex: 'nameEn', key: 'nameEn', width: 180, ellipsis: true },
  { title: '领域', key: 'domain', width: 110 },
  { title: '状态', key: 'status', width: 80 },
]

async function fetchConcepts(page = 1) {
  if (!selectedCs.value) return
  conceptLoading.value = true
  try {
    const params: any = { page, pageSize: pagination.pageSize, codeSystemId: selectedCs.value.id }
    if (conceptFilters.domain) params.domain = conceptFilters.domain

    const res = conceptFilters.keyword
      ? await searchConcepts({ ...params, keyword: conceptFilters.keyword })
      : await getConcepts(params)

    const data = res.data.data
    tableData.value = data?.content || data?.items || []
    pagination.total = data?.totalElements || data?.total || 0
    pagination.current = (data?.number ?? data?.page ?? page - 1) + 1
  } finally {
    conceptLoading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchConcepts(pag.current)
}

function resetConceptFilters() {
  conceptFilters.domain = undefined
  conceptFilters.keyword = ''
  fetchConcepts(1)
}

// ── Concept detail drawer ──
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

async function openDetail(record: any) {
  drawerVisible.value = true
  detailTab.value = 'properties'
  try {
    const res = await getConcept(record.id)
    currentConcept.value = res.data.data
  } catch {
    currentConcept.value = record
  }
  try {
    const res = await getConceptAncestors(record.id)
    ancestors.value = res.data.data || []
  } catch { ancestors.value = [] }
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

// ── CodeSystem create/edit modal ──
const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  code: '',
  name: '',
  version: '',
  description: '',
  category: 'STANDARD',
  hierarchySupport: false,
})
const formRules: Record<string, Rule[]> = {
  code: [{ required: true, message: '请输入编码' }],
  name: [{ required: true, message: '请输入名称' }],
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, { code: '', name: '', version: '', description: '', category: 'STANDARD', hierarchySupport: false })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    code: record.code,
    name: record.name,
    version: record.version || '',
    description: record.description || '',
    category: record.category || 'STANDARD',
    hierarchySupport: record.hierarchySupport || false,
  })
  modalVisible.value = true
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
  editingId.value = null
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  const data = { ...formState }
  if (isEdit.value) {
    await updateCodeSystem(editingId.value!, data)
    message.success('更新成功')
  } else {
    await createCodeSystem(data)
    message.success('创建成功')
  }
  handleModalCancel()
  fetchCodeSystems()
}

onMounted(() => {
  fetchDomains()
  fetchCodeSystems()
})
</script>

<style scoped>
.cs-card {
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.cs-card:hover {
  border-color: #91caff;
  background: #f6f9ff;
}
.cs-card.active {
  border-color: #1677ff;
  background: #e8f4ff;
}
</style>
