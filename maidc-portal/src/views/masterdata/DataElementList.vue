<template>
  <PageContainer title="数据元管理">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增数据元
      </a-button>
    </template>

    <div style="display: flex; gap: 16px; height: calc(100vh - 180px)">
      <!-- Left: Category tree -->
      <div style="width: 240px; flex-shrink: 0; display: flex; flex-direction: column">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
          <span style="font-weight: 600; font-size: 13px; color: #666">数据元分类</span>
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
        <a-card :bordered="false" style="margin-bottom: 12px; padding: 8px 16px" size="small">
          <a-row :gutter="12" align="middle">
            <a-col :span="5">
              <a-select
                v-model:value="filters.registrationStatus"
                placeholder="注册状态"
                allow-clear
                style="width: 100%"
                @change="fetchList(1)"
              >
                <a-select-option value="DRAFT">草稿</a-select-option>
                <a-select-option value="PUBLISHED">已发布</a-select-option>
                <a-select-option value="RETIRED">已废止</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="9">
              <a-input-search
                v-model:value="filters.keyword"
                placeholder="搜索标识符或名称"
                enter-button
                @search="fetchList(1)"
                allow-clear
                @clear="fetchList(1)"
              />
            </a-col>
            <a-col :span="4">
              <a-button @click="resetFilters">重置</a-button>
            </a-col>
          </a-row>
        </a-card>

        <!-- Table -->
        <a-table
          :columns="columns"
          :data-source="dataElements"
          :loading="loading"
          row-key="id"
          size="small"
          :pagination="false"
          :scroll="{ y: 'calc(100vh - 360px)' }"
          @row="(record: any) => ({ onClick: () => openDetail(record) })"
          style="cursor: pointer"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'registrationStatus'">
              <a-tag :color="statusColor(record.registrationStatus)">
                {{ statusLabel(record.registrationStatus) }}
              </a-tag>
            </template>
            <template v-if="column.key === 'dataType'">
              {{ dataTypeLabel(record.dataType) }}
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click.stop="handleEdit(record)">编辑</a-button>
                <a-popconfirm title="确认删除该数据元？" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger @click.stop>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>

        <!-- Pagination -->
        <div style="margin-top: 12px; text-align: right">
          <a-pagination
            v-if="pagination.total > pagination.pageSize"
            :current="pagination.current"
            :total="pagination.total"
            :page-size="pagination.pageSize"
            @change="fetchList"
            size="small"
            show-quick-jumper
            :show-total="(total: number) => `共 ${total} 条`"
          />
        </div>
      </div>
    </div>

    <!-- Detail drawer -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="currentElement ? '数据元详情' : '数据元详情'"
      width="720"
      destroy-on-close
    >
      <a-tabs v-model:activeKey="activeTab">
        <!-- Tab 1: Basic info -->
        <a-tab-pane key="basic" tab="基本信息">
          <a-form
            ref="formRef"
            :model="formState"
            :rules="formRules"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 18 }"
          >
            <a-form-item label="标识符" name="elementCode">
              <a-input v-model:value="formState.elementCode" placeholder="如 DE04.50.001" :disabled="!!editingId" />
            </a-form-item>
            <a-form-item label="规范名称" name="name">
              <a-input v-model:value="formState.name" placeholder="数据元规范名称" />
            </a-form-item>
            <a-form-item label="英文名称">
              <a-input v-model:value="formState.nameEn" placeholder="English name" />
            </a-form-item>
            <a-form-item label="定义" name="definition">
              <a-textarea v-model:value="formState.definition" :rows="3" placeholder="数据元定义描述" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="对象类" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input v-model:value="formState.objectClassName" placeholder="对象类名称" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="特性" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input v-model:value="formState.propertyName" placeholder="特性名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="数据类型" name="dataType" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-select v-model:value="formState.dataType" placeholder="选择数据类型">
                    <a-select-option value="ST">字符串 (ST)</a-select-option>
                    <a-select-option value="INT">整数 (INT)</a-select-option>
                    <a-select-option value="REAL">实数 (REAL)</a-select-option>
                    <a-select-option value="DT">日期 (DT)</a-select-option>
                    <a-select-option value="DTM">日期时间 (DTM)</a-select-option>
                    <a-select-option value="TM">时间 (TM)</a-select-option>
                    <a-select-option value="CD">代码 (CD)</a-select-option>
                    <a-select-option value="BL">布尔 (BL)</a-select-option>
                    <a-select-option value="BIN">二进制 (BIN)</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="表示类" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input v-model:value="formState.representationClass" placeholder="表示类" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="最小长度" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input-number v-model:value="formState.minLength" placeholder="最小长度" style="width: 100%" :min="0" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="最大长度" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input-number v-model:value="formState.maxLength" placeholder="最大长度" style="width: 100%" :min="0" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="格式" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input v-model:value="formState.format" placeholder="格式约束" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="计量单位" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input v-model:value="formState.unitOfMeasure" placeholder="如 mmHg、kg" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="分类">
              <a-select v-model:value="formState.category" placeholder="选择分类" allow-clear>
                <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="标准来源">
              <a-input v-model:value="formState.standardSource" placeholder="如 WS363、GB/T" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="注册状态" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-select v-model:value="formState.registrationStatus" placeholder="选择状态">
                    <a-select-option value="DRAFT">草稿</a-select-option>
                    <a-select-option value="PUBLISHED">已发布</a-select-option>
                    <a-select-option value="RETIRED">已废止</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="版本" :label-col="{ span: 10 }" :wrapper-col="{ span: 13 }">
                  <a-input v-model:value="formState.version" placeholder="如 1.0" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item :wrapper-col="{ offset: 5, span: 18 }">
              <a-space>
                <a-button type="primary" @click="handleFormSubmit" :loading="submitting">
                  {{ editingId ? '保存修改' : '创建' }}
                </a-button>
                <a-button @click="drawerVisible = false">取消</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- Tab 2: Allowed values -->
        <a-tab-pane key="values" tab="允许值" :disabled="!editingId">
          <template v-if="editingId && formState.dataType === 'CD'">
            <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center">
              <span style="color: #999; font-size: 13px">数据类型为「代码 (CD)」时可管理允许值列表</span>
              <a-space>
                <a-button size="small" @click="addValueRow">
                  <template #icon><PlusOutlined /></template>
                  添加行
                </a-button>
                <a-button size="small" type="primary" :loading="valuesSaving" @click="handleSaveValues">
                  保存
                </a-button>
              </a-space>
            </div>
            <a-table
              :columns="valueColumns"
              :data-source="valueRows"
              :pagination="false"
              row-key="_idx"
              size="small"
              bordered
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'valueCode'">
                  <a-input v-model:value="record.valueCode" size="small" placeholder="值编码" />
                </template>
                <template v-if="column.key === 'valueMeaning'">
                  <a-input v-model:value="record.valueMeaning" size="small" placeholder="值含义" />
                </template>
                <template v-if="column.key === 'sortOrder'">
                  <a-input-number v-model:value="record.sortOrder" size="small" :min="0" style="width: 80px" />
                </template>
                <template v-if="column.key === 'valueAction'">
                  <a-button type="link" danger size="small" @click="removeValueRow(index)">删除</a-button>
                </template>
              </template>
            </a-table>
          </template>
          <a-empty v-else-if="editingId" description="仅数据类型为「代码 (CD)」时支持允许值管理" />
          <a-empty v-else description="请先保存数据元基本信息" />
        </a-tab-pane>

        <!-- Tab 3: Field mappings -->
        <a-tab-pane key="mappings" tab="字段映射" :disabled="!editingId">
          <template v-if="editingId">
            <div style="margin-bottom: 12px; display: flex; justify-content: flex-end">
              <a-button size="small" type="primary" @click="mappingModalVisible = true">
                <template #icon><PlusOutlined /></template>
                添加映射
              </a-button>
            </div>
            <a-table
              :columns="mappingColumns"
              :data-source="mappings"
              :loading="mappingsLoading"
              :pagination="false"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'columnPath'">
                  {{ record.schemaName }}.{{ record.tableName }}.{{ record.columnName }}
                </template>
                <template v-if="column.key === 'mappingStatus'">
                  <a-tag :color="mappingStatusColor(record.mappingStatus)">
                    {{ mappingStatusLabel(record.mappingStatus) }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'mappingAction'">
                  <a-space>
                    <a-button
                      v-if="record.mappingStatus === 'PENDING'"
                      type="link"
                      size="small"
                      @click.stop="handleConfirmMapping(record.id)"
                    >确认</a-button>
                    <a-button
                      v-if="record.mappingStatus === 'PENDING'"
                      type="link"
                      size="small"
                      danger
                      @click.stop="handleRejectMapping(record.id)"
                    >拒绝</a-button>
                    <a-popconfirm title="确认删除该映射？" @confirm="handleDeleteMapping(record.id)">
                      <a-button type="link" size="small" danger @click.stop>删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </template>
              </template>
            </a-table>
          </template>
          <a-empty v-else description="请先保存数据元基本信息" />
        </a-tab-pane>
      </a-tabs>
    </a-drawer>

    <!-- Add mapping modal -->
    <a-modal
      v-model:open="mappingModalVisible"
      title="添加字段映射"
      @ok="handleAddMapping"
      destroy-on-close
      width="500"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="Schema">
          <a-input v-model:value="mappingForm.schemaName" placeholder="如 public" />
        </a-form-item>
        <a-form-item label="Table">
          <a-input v-model:value="mappingForm.tableName" placeholder="表名" />
        </a-form-item>
        <a-form-item label="Column">
          <a-input v-model:value="mappingForm.columnName" placeholder="列名" />
        </a-form-item>
        <a-form-item label="映射类型">
          <a-select v-model:value="mappingForm.mappingType" placeholder="选择映射类型" allow-clear>
            <a-select-option value="DIRECT">直接映射</a-select-option>
            <a-select-option value="TRANSFORM">转换映射</a-select-option>
            <a-select-option value="COMPOSITE">复合映射</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="置信度">
          <a-input-number
            v-model:value="mappingForm.confidence"
            :min="0"
            :max="1"
            :step="0.1"
            style="width: 100%"
            placeholder="0 ~ 1"
          />
        </a-form-item>
        <a-form-item label="转换规则">
          <a-textarea v-model:value="mappingForm.transformRule" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
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

defineOptions({ name: 'DataElementList' })

// ── Constants ──
const STATUS_MAP: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  RETIRED: '已废止',
}
const STATUS_COLOR: Record<string, string> = {
  DRAFT: 'orange',
  PUBLISHED: 'green',
  RETIRED: 'red',
}
const statusLabel = (s: string) => STATUS_MAP[s] || s
const statusColor = (s: string) => STATUS_COLOR[s] || 'default'

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
const MAPPING_STATUS_COLOR: Record<string, string> = {
  PENDING: 'default',
  CONFIRMED: 'green',
  REJECTED: 'red',
}
const mappingStatusLabel = (s: string) => MAPPING_STATUS_MAP[s] || s
const mappingStatusColor = (s: string) => MAPPING_STATUS_COLOR[s] || 'default'

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

const columns = [
  { title: '标识符', dataIndex: 'elementCode', key: 'elementCode', width: 150, ellipsis: true },
  { title: '规范名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '对象类', dataIndex: 'objectClassName', key: 'objectClassName', width: 120, ellipsis: true },
  { title: '数据类型', key: 'dataType', width: 100 },
  { title: '表示类', dataIndex: 'representationClass', key: 'representationClass', width: 100, ellipsis: true },
  { title: '分类', dataIndex: 'category', key: 'category', width: 100 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 70 },
  { title: '注册状态', key: 'registrationStatus', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

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
    message.error('加载数据元列表失败')
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

const formRules: Record<string, Rule[]> = {
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
  await formRef.value?.validateFields()
  submitting.value = true
  try {
    const data = { ...formState }
    if (editingId.value) {
      await updateDataElement(editingId.value, data)
      message.success('更新成功')
    } else {
      const res = await createDataElement(data)
      const created = res.data.data
      editingId.value = created?.id || null
      message.success('创建成功')
    }
    fetchList()
    fetchStats()
  } catch {
    message.error('保存失败')
  } finally {
    submitting.value = false
  }
}

function handleDelete(record: any) {
  return deleteDataElement(record.id).then(() => {
    message.success('删除成功')
    fetchList()
    fetchStats()
  }).catch(() => {
    message.error('删除失败')
  })
}

// ── Allowed values (Tab 2) ──
const valueRows = ref<any[]>([])
const valuesSaving = ref(false)
let valueRowIdx = 0

const valueColumns = [
  { title: '值编码', key: 'valueCode', width: 180 },
  { title: '值含义', key: 'valueMeaning' },
  { title: '排序', key: 'sortOrder', width: 100 },
  { title: '操作', key: 'valueAction', width: 80 },
]

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
    return message.warning('请填写完整的值编码和值含义')
  }
  valuesSaving.value = true
  try {
    await updateDataElementValues(editingId.value, data)
    message.success('允许值保存成功')
    loadValues(editingId.value)
  } catch {
    message.error('保存允许值失败')
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

const mappingColumns = [
  { title: '字段路径', key: 'columnPath', width: 220 },
  { title: '映射类型', dataIndex: 'mappingType', key: 'mappingType', width: 100 },
  { title: '置信度', dataIndex: 'confidence', key: 'confidence', width: 80 },
  { title: '状态', key: 'mappingStatus', width: 90 },
  { title: '转换规则', dataIndex: 'transformRule', key: 'transformRule', ellipsis: true },
  { title: '操作', key: 'mappingAction', width: 160 },
]

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
    return message.warning('请填写完整的 Schema、Table 和 Column')
  }
  try {
    await addDataElementMapping(editingId.value, { ...mappingForm })
    message.success('映射添加成功')
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
    message.error('添加映射失败')
  }
}

async function handleConfirmMapping(mappingId: number) {
  try {
    await updateDataElementMapping(mappingId, 'CONFIRMED')
    message.success('已确认')
    if (editingId.value) loadMappings(editingId.value)
  } catch {
    message.error('操作失败')
  }
}

async function handleRejectMapping(mappingId: number) {
  try {
    await updateDataElementMapping(mappingId, 'REJECTED')
    message.success('已拒绝')
    if (editingId.value) loadMappings(editingId.value)
  } catch {
    message.error('操作失败')
  }
}

async function handleDeleteMapping(mappingId: number) {
  try {
    await deleteDataElementMapping(mappingId)
    message.success('已删除')
    if (editingId.value) loadMappings(editingId.value)
  } catch {
    message.error('删除失败')
  }
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
  background: #f5f5f5;
}
.cat-item.active {
  background: #e8f4ff;
  color: #1677ff;
  font-weight: 600;
}
.cat-count {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 0 6px;
  border-radius: 10px;
  line-height: 20px;
}
.cat-item.active .cat-count {
  background: #bae0ff;
  color: #1677ff;
}
</style>
