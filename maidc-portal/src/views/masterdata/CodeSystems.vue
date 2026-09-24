<template>
  <PageContainer title="编码体系">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增编码体系
      </el-button>
    </template>

    <div style="display: flex; gap: 16px; height: calc(100vh - 180px)">
      <!-- Left: Code System List -->
      <div style="width: 320px; flex-shrink: 0; display: flex; flex-direction: column">
        <el-input v-model="csSearch" placeholder="检索编码体系" clearable :suffix-icon="Search"
          style="margin-bottom: 12px" />
        <div class="cs-list" style="flex: 1; overflow-y: auto">
          <div v-loading="csLoading" style="min-height: 200px">
            <div
              v-for="cs in filteredCodeSystems" :key="cs.id"
              class="cs-card" :class="{ active: selectedCs?.id === cs.id }"
              @click="selectCodeSystem(cs)">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px">
                <span style="font-weight: 600; font-size: 14px">{{ cs.name }}</span>
                <div style="display: flex; gap: 4px">
                  <el-tag :type="cs.category === 'LOCAL' ? 'warning' : 'primary'" size="small">
                    {{ cs.category === 'LOCAL' ? '非标' : cs.category === 'STANDARD' ? '标准' : cs.category || '标准' }}
                  </el-tag>
                  <el-tag v-if="cs.status" :type="cs.status === 'ACTIVE' ? 'success' : cs.status === 'DRAFT' ? 'warning' : 'info'" size="small">
                    {{ cs.status === 'ACTIVE' ? '启用' : cs.status === 'DRAFT' ? '草稿' : cs.status }}
                  </el-tag>
                </div>
              </div>
              <div style="color: #94a3b8; font-size: 12px; margin-bottom: 4px">{{ cs.code }} · v{{ cs.version || '-' }}</div>
              <div style="display: flex; align-items: center; gap: 12px; font-size: 12px; color: #64748b">
                <span>概念: <b>{{ statsMap[cs.id]?.conceptCount ?? '-' }}</b></span>
                <span style="display: inline-flex; align-items: center">
                  <el-icon v-if="cs.hierarchySupport" style="color: #0ea5e9"><Share /></el-icon>
                  {{ cs.hierarchySupport ? '层级' : '' }}
                </span>
                <a style="margin-left: auto; font-size: 12px; color: #0ea5e9" @click.stop="handleEdit(cs)">编辑</a>
              </div>
            </div>
            <el-empty v-if="!csLoading && filteredCodeSystems.length === 0" description="无匹配编码体系" :image-size="40" />
          </div>
        </div>
      </div>

      <!-- Right: Concepts -->
      <div style="flex: 1; display: flex; flex-direction: column; min-width: 0">
        <template v-if="selectedCs">
          <!-- Filter bar -->
          <el-card shadow="never" :body-style="{ padding: '8px 16px' }" style="margin-bottom: 12px">
            <div class="flex flex-wrap items-center gap-3">
              <el-select v-model="conceptFilters.domain" placeholder="选择领域" clearable
                style="width: 180px" @change="fetchConcepts(1)">
                <el-option v-for="d in domains" :key="d" :value="d" :label="domainMap[d] || d" />
              </el-select>
              <el-input v-model="conceptFilters.keyword" placeholder="搜索概念编码或名称"
                clearable :suffix-icon="Search" style="width: 320px"
                @keyup.enter="fetchConcepts(1)" @clear="fetchConcepts(1)" />
              <el-button @click="resetConceptFilters">重置</el-button>
              <span class="ml-auto text-sm" style="color: #94a3b8">
                {{ selectedCs.name }} · 共 {{ pagination.total }} 条概念
              </span>
            </div>
          </el-card>

          <!-- Concept table -->
          <div style="flex: 1; min-height: 0">
            <el-table :data="tableData" v-loading="conceptLoading" row-key="id" size="small"
              style="cursor: pointer; width: 100%" height="100%"
              @row-click="openDetail">
              <el-table-column label="概念编码" prop="conceptCode" width="150" show-overflow-tooltip />
              <el-table-column label="名称" prop="name" show-overflow-tooltip />
              <el-table-column label="英文名" prop="nameEn" width="180" show-overflow-tooltip />
              <el-table-column label="领域" width="110">
                <template #default="{ row }">
                  <el-tag>{{ row.domain || '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'DRAFT' ? 'warning' : 'info'">
                    {{ row.status === 'ACTIVE' ? '启用' : row.status === 'DRAFT' ? '草稿' : row.status }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-pagination
            class="mt-3 justify-end"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="pagination.total"
            :current-page="pagination.current"
            :page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </template>
        <el-empty v-else description="请从左侧选择一个编码体系" :image-size="60" style="margin-top: 120px" />
      </div>
    </div>

    <!-- Concept detail drawer -->
    <el-drawer v-model="drawerVisible" :title="currentConcept?.name || '概念详情'" size="640px" :destroy-on-close="true">
      <template v-if="currentConcept">
        <el-descriptions border :column="2" size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="概念编码">{{ currentConcept.conceptCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentConcept.status === 'ACTIVE' ? 'success' : 'warning'">{{ currentConcept.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="名称">{{ currentConcept.name }}</el-descriptions-item>
          <el-descriptions-item label="英文名">{{ currentConcept.nameEn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="领域">{{ currentConcept.domain || '-' }}</el-descriptions-item>
          <el-descriptions-item label="编码体系">{{ selectedCs?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentConcept.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="ancestors.length > 0" style="margin-bottom: 16px">
          <strong>层级路径：</strong>
          <el-breadcrumb separator="/" style="display: inline-block; margin-left: 4px">
            <el-breadcrumb-item v-for="a in ancestors" :key="a.id">{{ a.name }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentConcept.name }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <el-tabs v-model="detailTab">
          <el-tab-pane label="属性" name="properties">
            <div v-if="currentConcept.properties">
              <pre style="background: #f8fafc; padding: 12px; border-radius: 4px; font-size: 12px; overflow-x: auto">{{
                typeof currentConcept.properties === 'string'
                  ? JSON.stringify(JSON.parse(currentConcept.properties), null, 2)
                  : JSON.stringify(currentConcept.properties, null, 2)
              }}</pre>
            </div>
            <el-empty v-else description="无额外属性" :image-size="60" />
          </el-tab-pane>
          <el-tab-pane label="映射" name="mappings">
            <el-table :data="conceptMappings" v-loading="mappingsLoading" row-key="id" size="small">
              <el-table-column label="目标编码" prop="targetConceptCode" width="140" />
              <el-table-column label="目标名称" prop="targetConceptName" width="180" />
              <el-table-column label="映射类型" width="100">
                <template #default="{ row }">
                  <el-tag>{{ row.mappingType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="目标体系" prop="targetCodeSystem" width="120" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="同义词" name="synonyms">
            <el-table :data="conceptSynonyms" v-loading="synonymsLoading" row-key="id" size="small">
              <el-table-column label="同义词" prop="synonym" />
              <el-table-column label="语言" prop="language" width="80" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="子概念" name="children">
            <el-table :data="conceptChildren" v-loading="childrenLoading" row-key="id" size="small">
              <el-table-column label="概念编码" prop="conceptCode" width="140" />
              <el-table-column label="名称" prop="name" width="180" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- CodeSystem create/edit modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑编码体系' : '新增编码体系'"
      width="600px" :destroy-on-close="true">
      <el-form ref="formRef" :model="formState" :rules="formRules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="formState.code" placeholder="如 ICD-10, LOINC, SNOMED" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formState.name" placeholder="编码体系名称" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="formState.version" placeholder="如 2024 版" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formState.description" type="textarea" :rows="3" placeholder="编码体系描述" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formState.category" placeholder="选择分类" class="w-full">
            <el-option value="STANDARD" label="标准编码" />
            <el-option value="LOCAL" label="非标编码" />
          </el-select>
        </el-form-item>
        <el-form-item label="层级支持" prop="hierarchySupport">
          <el-switch v-model="formState.hierarchySupport" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleModalCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Share } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
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
})

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

function handlePageChange(page: number) {
  pagination.current = page
  fetchConcepts(page)
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchConcepts(1)
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
const formRules: Record<string, FormItemRule[]> = {
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
  await formRef.value?.validate()
  const data = { ...formState }
  if (isEdit.value) {
    await updateCodeSystem(editingId.value!, data)
    ElMessage.success('更新成功')
  } else {
    await createCodeSystem(data)
    ElMessage.success('创建成功')
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
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.cs-card:hover {
  border-color: #7dd3fc;
  background: #f0f9ff;
}
.cs-card.active {
  border-color: #0ea5e9;
  background: #f0f9ff;
}
</style>
