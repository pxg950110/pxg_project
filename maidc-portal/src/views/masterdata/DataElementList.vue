<template>
  <PageContainer title="数据元管理">
    <template #extra>
      <div class="flex items-center gap-2">
        <el-button @click="importModalRef?.open()">
          <el-icon class="mr-1"><Upload /></el-icon>
          导入
        </el-button>
        <el-button type="primary" @click="handleCreate">
          <el-icon class="mr-1"><Plus /></el-icon>
          新增数据元
        </el-button>
      </div>
    </template>

    <div style="display: flex; gap: 16px; height: calc(100vh - 180px)">
      <!-- Left: Category tree -->
      <div style="width: 240px; flex-shrink: 0; display: flex; flex-direction: column">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
          <span style="font-weight: 600; font-size: 13px; color: #64748b">数据元分类</span>
        </div>
        <div class="cat-list" style="flex: 1; overflow-y: auto">
          <div class="cat-item" :class="{ active: !selectedCategory }" @click="selectCategory(null)">
            <span>全部</span>
            <span v-if="stats.total" class="cat-count">{{ stats.total }}</span>
          </div>
          <div
            v-for="cat in categories"
            :key="cat"
            class="cat-item"
            :class="{ active: selectedCategory === cat }"
            @click="selectCategory(cat)"
          >
            <span>{{ cat }}</span>
            <span v-if="stats[cat]" class="cat-count">{{ stats[cat] }}</span>
          </div>
        </div>
      </div>

      <!-- Right: Data element table -->
      <div style="flex: 1; display: flex; flex-direction: column; min-width: 0">
        <!-- Search bar -->
        <el-card shadow="never" :body-style="{ padding: '8px 16px' }" style="margin-bottom: 12px">
          <div class="flex flex-wrap items-center gap-3">
            <el-select
              v-model="filters.registrationStatus"
              placeholder="注册状态"
              clearable
              style="width: 160px"
              @change="fetchList(1)"
            >
              <el-option value="DRAFT" label="草稿" />
              <el-option value="PUBLISHED" label="已发布" />
              <el-option value="RETIRED" label="已废止" />
            </el-select>
            <el-input
              v-model="filters.keyword"
              placeholder="搜索标识符或名称"
              clearable
              :suffix-icon="Search"
              style="width: 320px"
              @keyup.enter="fetchList(1)"
              @clear="fetchList(1)"
            />
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-card>

        <!-- Table -->
        <div style="flex: 1; min-height: 0">
          <el-table
            :data="dataElements"
            v-loading="loading"
            row-key="id"
            size="small"
            style="cursor: pointer; width: 100%"
            height="100%"
            @row-click="openDetail"
          >
            <el-table-column label="标识符" prop="elementCode" width="150" show-overflow-tooltip />
            <el-table-column label="规范名称" prop="name" show-overflow-tooltip />
            <el-table-column label="对象类" prop="objectClassName" width="120" show-overflow-tooltip />
            <el-table-column label="数据类型" width="100">
              <template #default="{ row }">
                {{ dataTypeLabel(row.dataType) }}
              </template>
            </el-table-column>
            <el-table-column label="表示类" prop="representationClass" width="100" show-overflow-tooltip />
            <el-table-column label="分类" prop="category" width="100" />
            <el-table-column label="版本" prop="version" width="70" />
            <el-table-column label="注册状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusColor(row.registrationStatus)">
                  {{ statusLabel(row.registrationStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-2" @click.stop>
                  <el-button link type="primary" size="small" @click.stop="handleEdit(row)">编辑</el-button>
                  <el-popconfirm title="确认删除该数据元？" @confirm="handleDelete(row)">
                    <template #reference>
                      <el-button link type="danger" size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Pagination -->
        <div class="mt-3 text-right">
          <el-pagination
            v-if="pagination.total > pagination.pageSize"
            size="small"
            background
            layout="total, prev, pager, next, jumper"
            :total="pagination.total"
            :current-page="pagination.current"
            :page-size="pagination.pageSize"
            @current-change="fetchList"
          />
        </div>
      </div>
    </div>

    <!-- Detail drawer -->
    <el-drawer
      v-model="drawerVisible"
      :title="editingId ? '编辑数据元' : '数据元详情'"
      size="720px"
      :destroy-on-close="true"
    >
      <el-tabs v-model="activeTab">
        <!-- Tab 1: Basic info -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form
            ref="formRef"
            :model="formState"
            :rules="formRules"
            label-width="100px"
          >
            <el-form-item label="标识符" prop="elementCode">
              <el-input v-model="formState.elementCode" placeholder="如 DE04.50.001" :disabled="!!editingId" />
            </el-form-item>
            <el-form-item label="规范名称" prop="name">
              <el-input v-model="formState.name" placeholder="数据元规范名称" />
            </el-form-item>
            <el-form-item label="英文名称">
              <el-input v-model="formState.nameEn" placeholder="English name" />
            </el-form-item>
            <el-form-item label="定义" prop="definition">
              <el-input v-model="formState.definition" type="textarea" :rows="3" placeholder="数据元定义描述" />
            </el-form-item>
            <div class="grid grid-cols-2 gap-x-4">
              <el-form-item label="对象类" label-width="90px">
                <el-input v-model="formState.objectClassName" placeholder="对象类名称" />
              </el-form-item>
              <el-form-item label="特性" label-width="90px">
                <el-input v-model="formState.propertyName" placeholder="特性名称" />
              </el-form-item>
            </div>
            <div class="grid grid-cols-2 gap-x-4">
              <el-form-item label="数据类型" prop="dataType" label-width="90px">
                <el-select v-model="formState.dataType" placeholder="选择数据类型" class="w-full">
                  <el-option value="ST" label="字符串 (ST)" />
                  <el-option value="INT" label="整数 (INT)" />
                  <el-option value="REAL" label="实数 (REAL)" />
                  <el-option value="DT" label="日期 (DT)" />
                  <el-option value="DTM" label="日期时间 (DTM)" />
                  <el-option value="TM" label="时间 (TM)" />
                  <el-option value="CD" label="代码 (CD)" />
                  <el-option value="BL" label="布尔 (BL)" />
                  <el-option value="BIN" label="二进制 (BIN)" />
                </el-select>
              </el-form-item>
              <el-form-item label="表示类" label-width="90px">
                <el-input v-model="formState.representationClass" placeholder="表示类" />
              </el-form-item>
            </div>
            <div class="grid grid-cols-2 gap-x-4">
              <el-form-item label="最小长度" label-width="90px">
                <el-input-number v-model="formState.minLength" placeholder="最小长度" class="w-full" :min="0" />
              </el-form-item>
              <el-form-item label="最大长度" label-width="90px">
                <el-input-number v-model="formState.maxLength" placeholder="最大长度" class="w-full" :min="0" />
              </el-form-item>
            </div>
            <div class="grid grid-cols-2 gap-x-4">
              <el-form-item label="格式" label-width="90px">
                <el-input v-model="formState.format" placeholder="格式约束" />
              </el-form-item>
              <el-form-item label="计量单位" label-width="90px">
                <el-input v-model="formState.unitOfMeasure" placeholder="如 mmHg、kg" />
              </el-form-item>
            </div>
            <el-form-item label="分类">
              <el-select v-model="formState.category" placeholder="选择分类" clearable class="w-full">
                <el-option v-for="cat in categories" :key="cat" :value="cat" :label="cat" />
              </el-select>
            </el-form-item>
            <el-form-item label="标准来源">
              <el-input v-model="formState.standardSource" placeholder="如 WS363、GB/T" />
            </el-form-item>
            <div class="grid grid-cols-2 gap-x-4">
              <el-form-item label="注册状态" label-width="90px">
                <el-select v-model="formState.registrationStatus" placeholder="选择状态" class="w-full">
                  <el-option value="DRAFT" label="草稿" />
                  <el-option value="PUBLISHED" label="已发布" />
                  <el-option value="RETIRED" label="已废止" />
                </el-select>
              </el-form-item>
              <el-form-item label="版本" label-width="90px">
                <el-input v-model="formState.version" placeholder="如 1.0" />
              </el-form-item>
            </div>
            <el-form-item>
              <div class="flex items-center gap-2" style="margin-left: 0">
                <el-button type="primary" @click="handleFormSubmit" :loading="submitting">
                  {{ editingId ? '保存修改' : '创建' }}
                </el-button>
                <el-button @click="drawerVisible = false">取消</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 2: Allowed values -->
        <el-tab-pane label="允许值" name="values" :disabled="!editingId">
          <template v-if="editingId && formState.dataType === 'CD'">
            <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
              <span style="color: #94a3b8; font-size: 13px">数据类型为「代码 (CD)」时可管理允许值列表</span>
              <div class="flex items-center gap-2">
                <el-button size="small" @click="addValueRow">
                  <el-icon class="mr-1"><Plus /></el-icon>
                  添加行
                </el-button>
                <el-button size="small" type="primary" :loading="valuesSaving" @click="handleSaveValues">
                  保存
                </el-button>
              </div>
            </div>
            <el-table
              :data="valueRows"
              row-key="_idx"
              size="small"
              border
            >
              <el-table-column label="值编码" width="180">
                <template #default="{ row }">
                  <el-input v-model="row.valueCode" size="small" placeholder="值编码" />
                </template>
              </el-table-column>
              <el-table-column label="值含义">
                <template #default="{ row }">
                  <el-input v-model="row.valueMeaning" size="small" placeholder="值含义" />
                </template>
              </el-table-column>
              <el-table-column label="排序" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.sortOrder" size="small" :min="0" style="width: 100px" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="removeValueRow($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else-if="editingId" description="仅数据类型为「代码 (CD)」时支持允许值管理" :image-size="60" />
          <el-empty v-else description="请先保存数据元基本信息" :image-size="60" />
        </el-tab-pane>

        <!-- Tab 3: Field mappings -->
        <el-tab-pane label="字段映射" name="mappings" :disabled="!editingId">
          <template v-if="editingId">
            <div style="margin-bottom: 12px; display: flex; justify-content: flex-end">
              <el-button size="small" type="primary" @click="mappingModalVisible = true">
                <el-icon class="mr-1"><Plus /></el-icon>
                添加映射
              </el-button>
            </div>
            <el-table
              :data="mappings"
              v-loading="mappingsLoading"
              row-key="id"
              size="small"
            >
              <el-table-column label="字段路径" width="220">
                <template #default="{ row }">
                  {{ row.schemaName }}.{{ row.tableName }}.{{ row.columnName }}
                </template>
              </el-table-column>
              <el-table-column label="映射类型" prop="mappingType" width="100" />
              <el-table-column label="置信度" prop="confidence" width="80" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="mappingStatusColor(row.mappingStatus)">
                    {{ mappingStatusLabel(row.mappingStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="转换规则" prop="transformRule" show-overflow-tooltip />
              <el-table-column label="操作" width="160">
                <template #default="{ row }">
                  <div class="flex items-center gap-2" @click.stop>
                    <el-button
                      v-if="row.mappingStatus === 'PENDING'"
                      link
                      type="primary"
                      size="small"
                      @click.stop="handleConfirmMapping(row.id)"
                    >确认</el-button>
                    <el-button
                      v-if="row.mappingStatus === 'PENDING'"
                      link
                      type="danger"
                      size="small"
                      @click.stop="handleRejectMapping(row.id)"
                    >拒绝</el-button>
                    <el-popconfirm title="确认删除该映射？" @confirm="handleDeleteMapping(row.id)">
                      <template #reference>
                        <el-button link type="danger" size="small">删除</el-button>
                      </template>
                    </el-popconfirm>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
          <el-empty v-else description="请先保存数据元基本信息" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- Add mapping modal -->
    <el-dialog
      v-model="mappingModalVisible"
      title="添加字段映射"
      :destroy-on-close="true"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item label="Schema">
          <el-input v-model="mappingForm.schemaName" placeholder="如 public" />
        </el-form-item>
        <el-form-item label="Table">
          <el-input v-model="mappingForm.tableName" placeholder="表名" />
        </el-form-item>
        <el-form-item label="Column">
          <el-input v-model="mappingForm.columnName" placeholder="列名" />
        </el-form-item>
        <el-form-item label="映射类型">
          <el-select v-model="mappingForm.mappingType" placeholder="选择映射类型" clearable class="w-full">
            <el-option value="DIRECT" label="直接映射" />
            <el-option value="TRANSFORM" label="转换映射" />
            <el-option value="COMPOSITE" label="复合映射" />
          </el-select>
        </el-form-item>
        <el-form-item label="置信度">
          <el-input-number
            v-model="mappingForm.confidence"
            :min="0"
            :max="1"
            :step="0.1"
            class="w-full"
            placeholder="0 ~ 1"
          />
        </el-form-item>
        <el-form-item label="转换规则">
          <el-input v-model="mappingForm.transformRule" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mappingModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddMapping">确定</el-button>
      </template>
    </el-dialog>
    <!-- Import modal -->
    <DataElementImportModal ref="importModalRef" @success="onImportSuccess" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload, Search } from '@element-plus/icons-vue'
import { type FormInstance, type FormItemRule } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getDataElements,
  getDataElement,
  createDataElement,
  updateDataElement,
  deleteDataElement,
  getDataElementCategories,
  getDataElementStats,
  getDataElementValues,
  updateDataElementValues,
  getDataElementMappings,
  addDataElementMapping,
  updateDataElementMapping,
  deleteDataElementMapping,
} from '@/api/masterdata'
import DataElementImportModal from './DataElementImportModal.vue'

defineOptions({ name: 'DataElementList' })

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

// ── Constants ──
const STATUS_MAP: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  RETIRED: '已废止',
}
const STATUS_COLOR: Record<string, TagType> = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  RETIRED: 'danger',
}
const statusLabel = (s: string) => STATUS_MAP[s] || s
const statusColor = (s: string): TagType => STATUS_COLOR[s] || 'info'

const DATA_TYPE_MAP: Record<string, string> = {
  ST: '字符串',
  INT: '整数',
  REAL: '实数',
  DT: '日期',
  DTM: '日期时间',
  TM: '时间',
  CD: '代码',
  BL: '布尔',
  BIN: '二进制',
}
const dataTypeLabel = (t: string) => DATA_TYPE_MAP[t] || t

const MAPPING_STATUS_MAP: Record<string, string> = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  REJECTED: '已拒绝',
}
const MAPPING_STATUS_COLOR: Record<string, TagType> = {
  PENDING: 'info',
  CONFIRMED: 'success',
  REJECTED: 'danger',
}
const mappingStatusLabel = (s: string) => MAPPING_STATUS_MAP[s] || s
const mappingStatusColor = (s: string): TagType => MAPPING_STATUS_COLOR[s] || 'info'

// ── Categories ──
const categories = ref<string[]>([])
const selectedCategory = ref<string | null>(null)
const stats = ref<Record<string, number>>({})

async function fetchCategories() {
  try {
    const res = await getDataElementCategories()
    categories.value = res.data.data || []
  } catch {
    categories.value = []
  }
}

async function fetchStats() {
  try {
    const res = await getDataElementStats()
    stats.value = res.data.data || {}
  } catch {
    stats.value = {}
  }
}

function selectCategory(cat: string | null) {
  selectedCategory.value = cat
  fetchList(1)
}

// ── Data element list ──
const loading = ref(false)
const dataElements = ref<any[]>([])
const filters = reactive({
  registrationStatus: undefined as string | undefined,
  keyword: '',
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function fetchList(page = 1) {
  loading.value = true
  try {
    const params: any = { page, page_size: pagination.pageSize }
    if (selectedCategory.value) params.category = selectedCategory.value
    if (filters.registrationStatus) params.registrationStatus = filters.registrationStatus
    if (filters.keyword) params.keyword = filters.keyword
    const res = await getDataElements(params)
    const data = res.data.data
    dataElements.value = data?.content || data?.items || []
    pagination.total = data?.totalElements || data?.total || 0
    pagination.current = (data?.number ?? page - 1) + 1
  } catch {
    ElMessage.error('加载数据元列表失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.registrationStatus = undefined
  filters.keyword = ''
  fetchList(1)
}

// ── Form state ──
const drawerVisible = ref(false)
const activeTab = ref('basic')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const formState = reactive({
  elementCode: '',
  name: '',
  nameEn: '',
  definition: '',
  objectClassName: '',
  propertyName: '',
  dataType: undefined as string | undefined,
  representationClass: '',
  minLength: undefined as number | undefined,
  maxLength: undefined as number | undefined,
  format: '',
  unitOfMeasure: '',
  category: undefined as string | undefined,
  standardSource: '',
  registrationStatus: 'DRAFT',
  version: '',
})

const formRules: Record<string, FormItemRule[]> = {
  elementCode: [{ required: true, message: '请输入标识符' }],
  name: [{ required: true, message: '请输入规范名称' }],
  definition: [{ required: true, message: '请输入定义' }],
  dataType: [{ required: true, message: '请选择数据类型' }],
}

function resetFormState() {
  Object.assign(formState, {
    elementCode: '',
    name: '',
    nameEn: '',
    definition: '',
    objectClassName: '',
    propertyName: '',
    dataType: undefined,
    representationClass: '',
    minLength: undefined,
    maxLength: undefined,
    format: '',
    unitOfMeasure: '',
    category: undefined,
    standardSource: '',
    registrationStatus: 'DRAFT',
    version: '',
  })
}

// ── CRUD ──
function handleCreate() {
  editingId.value = null
  resetFormState()
  formState.category = selectedCategory.value || undefined
  activeTab.value = 'basic'
  drawerVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    elementCode: record.elementCode || '',
    name: record.name || '',
    nameEn: record.nameEn || '',
    definition: record.definition || '',
    objectClassName: record.objectClassName || '',
    propertyName: record.propertyName || '',
    dataType: record.dataType,
    representationClass: record.representationClass || '',
    minLength: record.minLength,
    maxLength: record.maxLength,
    format: record.format || '',
    unitOfMeasure: record.unitOfMeasure || '',
    category: record.category,
    standardSource: record.standardSource || '',
    registrationStatus: record.registrationStatus || 'DRAFT',
    version: record.version || '',
  })
  activeTab.value = 'basic'
  drawerVisible.value = true
}

async function openDetail(record: any) {
  editingId.value = record.id
  activeTab.value = 'basic'
  drawerVisible.value = true
  try {
    const res = await getDataElement(record.id)
    const detail = res.data.data
    Object.assign(formState, {
      elementCode: detail.elementCode || '',
      name: detail.name || '',
      nameEn: detail.nameEn || '',
      definition: detail.definition || '',
      objectClassName: detail.objectClassName || '',
      propertyName: detail.propertyName || '',
      dataType: detail.dataType,
      representationClass: detail.representationClass || '',
      minLength: detail.minLength,
      maxLength: detail.maxLength,
      format: detail.format || '',
      unitOfMeasure: detail.unitOfMeasure || '',
      category: detail.category,
      standardSource: detail.standardSource || '',
      registrationStatus: detail.registrationStatus || 'DRAFT',
      version: detail.version || '',
    })
  } catch {
    Object.assign(formState, {
      elementCode: record.elementCode || '',
      name: record.name || '',
      nameEn: record.nameEn || '',
      definition: record.definition || '',
      objectClassName: record.objectClassName || '',
      propertyName: record.propertyName || '',
      dataType: record.dataType,
      representationClass: record.representationClass || '',
      minLength: record.minLength,
      maxLength: record.maxLength,
      format: record.format || '',
      unitOfMeasure: record.unitOfMeasure || '',
      category: record.category,
      standardSource: record.standardSource || '',
      registrationStatus: record.registrationStatus || 'DRAFT',
      version: record.version || '',
    })
  }
  loadValues(record.id)
  loadMappings(record.id)
}

async function handleFormSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const data = { ...formState }
    if (editingId.value) {
      await updateDataElement(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      const res = await createDataElement(data)
      const created = res.data.data
      editingId.value = created?.id || null
      ElMessage.success('创建成功')
    }
    fetchList()
    fetchStats()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

function handleDelete(record: any) {
  return deleteDataElement(record.id).then(() => {
    ElMessage.success('删除成功')
    fetchList()
    fetchStats()
  }).catch(() => {
    ElMessage.error('删除失败')
  })
}

// ── Allowed values (Tab 2) ──
const valueRows = ref<any[]>([])
const valuesSaving = ref(false)
let valueRowIdx = 0

async function loadValues(elementId: number) {
  try {
    const res = await getDataElementValues(elementId)
    const list = res.data.data || []
    valueRows.value = list.map((v: any, i: number) => ({ ...v, _idx: i }))
    valueRowIdx = list.length
  } catch {
    valueRows.value = []
  }
}

function addValueRow() {
  valueRows.value.push({ valueCode: '', valueMeaning: '', sortOrder: valueRowIdx, _idx: valueRowIdx++ })
}

function removeValueRow(index: number) {
  valueRows.value.splice(index, 1)
}

async function handleSaveValues() {
  if (!editingId.value) return
  const data = valueRows.value.map((v: any) => ({
    valueCode: v.valueCode,
    valueMeaning: v.valueMeaning,
    sortOrder: v.sortOrder ?? 0,
  }))
  if (data.some((d: any) => !d.valueCode || !d.valueMeaning)) {
    return ElMessage.warning('请填写完整的值编码和值含义')
  }
  valuesSaving.value = true
  try {
    await updateDataElementValues(editingId.value, data)
    ElMessage.success('允许值保存成功')
    loadValues(editingId.value)
  } catch {
    ElMessage.error('保存允许值失败')
  } finally {
    valuesSaving.value = false
  }
}

// ── Field mappings (Tab 3) ──
const mappings = ref<any[]>([])
const mappingsLoading = ref(false)
const mappingModalVisible = ref(false)
const mappingForm = reactive({
  schemaName: '',
  tableName: '',
  columnName: '',
  mappingType: undefined as string | undefined,
  confidence: undefined as number | undefined,
  transformRule: '',
})

async function loadMappings(elementId: number) {
  mappingsLoading.value = true
  try {
    const res = await getDataElementMappings(elementId)
    mappings.value = res.data.data || []
  } catch {
    mappings.value = []
  } finally {
    mappingsLoading.value = false
  }
}

async function handleAddMapping() {
  if (!editingId.value) return
  if (!mappingForm.schemaName || !mappingForm.tableName || !mappingForm.columnName) {
    return ElMessage.warning('请填写完整的 Schema、Table 和 Column')
  }
  try {
    await addDataElementMapping(editingId.value, { ...mappingForm })
    ElMessage.success('映射添加成功')
    mappingModalVisible.value = false
    Object.assign(mappingForm, {
      schemaName: '',
      tableName: '',
      columnName: '',
      mappingType: undefined,
      confidence: undefined,
      transformRule: '',
    })
    loadMappings(editingId.value)
  } catch {
    ElMessage.error('添加映射失败')
  }
}

async function handleConfirmMapping(mappingId: number) {
  try {
    await updateDataElementMapping(mappingId, 'CONFIRMED')
    ElMessage.success('已确认')
    if (editingId.value) loadMappings(editingId.value)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleRejectMapping(mappingId: number) {
  try {
    await updateDataElementMapping(mappingId, 'REJECTED')
    ElMessage.success('已拒绝')
    if (editingId.value) loadMappings(editingId.value)
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDeleteMapping(mappingId: number) {
  try {
    await deleteDataElementMapping(mappingId)
    ElMessage.success('已删除')
    if (editingId.value) loadMappings(editingId.value)
  } catch {
    ElMessage.error('删除失败')
  }
}

// ── Import ──
const importModalRef = ref<InstanceType<typeof DataElementImportModal>>()

function onImportSuccess() {
  fetchList()
  fetchStats()
  fetchCategories()
}

// ── Init ──
onMounted(() => {
  fetchCategories()
  fetchStats()
  fetchList()
})
</script>

<style scoped>
.cat-item {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
  transition: background 0.2s;
}
.cat-item:hover {
  background: #f8fafc;
}
.cat-item.active {
  background: #f0f9ff;
  color: #0ea5e9;
  font-weight: 600;
}
.cat-count {
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 0 6px;
  border-radius: 10px;
  line-height: 20px;
}
.cat-item.active .cat-count {
  background: #e0f2fe;
  color: #0ea5e9;
}
</style>
