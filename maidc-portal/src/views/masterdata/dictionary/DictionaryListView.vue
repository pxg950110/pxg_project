<template>
  <div :class="embedded ? 'dict-list-view dict-list-view--embedded dict-skin-dark' : 'dict-list-view'">
    <div v-if="!embedded" class="dict-header">
      <h2 class="dict-title">{{ schema.title }}</h2>
      <a-space>
        <a-button v-if="schema.showImport" @click="handleImport">
          <template #icon><UploadOutlined /></template>
          导入
        </a-button>
        <a-button v-if="canCreate" type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          {{ schema.newLabel }}
        </a-button>
      </a-space>
    </div>

    <div class="dict-layout">
      <component
        :is="sidePanelComponent"
        v-if="sidePanelComponent"
        ref="sidePanelRef"
        @select="onSideSelect"
        @loaded="onSideLoaded"
      />

      <div class="dict-item-panel">
        <!-- 筛选栏 -->
        <a-card :bordered="false" class="filter-card" size="small">
          <a-row :gutter="12" align="middle">
            <a-col v-for="filter in schema.filters" :key="filter.name" :span="4">
              <a-select v-model:value="filters[filter.name]" :placeholder="filter.placeholder" allow-clear
                style="width: 100%" @change="fetchData(1)">
                <a-select-option v-for="opt in optionsOf(filter)" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-col>
            <a-col :span="8">
              <a-input-search v-model:value="filters.keyword" :placeholder="schema.keywordPlaceholder"
                enter-button @search="fetchData(1)" allow-clear />
            </a-col>
            <a-col :span="3">
              <a-button @click="resetFilters">重置</a-button>
            </a-col>
          </a-row>
        </a-card>

        <!-- 数据表格 -->
        <div class="table-card">
          <a-table class="dict-table" :columns="schema.columns" :data-source="tableData" :loading="loading"
            :pagination="pagination" @change="handleTableChange" row-key="id"
            :scroll="{ x: schema.scrollX, y: 'calc(100vh - 360px)' }" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <a @click="openDetail(record)">{{ record.name }}</a>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusTagOf(record.status).color">{{ statusTagOf(record.status).label }}</a-tag>
            </template>
            <template v-else-if="column.key === 'price'">
              {{ record.price ? `¥${record.price}` : '-' }}
            </template>
            <template v-else-if="column.key === 'insuranceType' || column.key === 'feeType'">
              <a-tag v-if="record[column.key]" :color="TYPE_TAG_COLORS[record[column.key]] || 'default'">
                {{ record[column.key] }}
              </a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'refRange'">
              {{ formatRefRange(record) }}
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-space>
                <a v-if="canUpdate" @click="handleEdit(record)">编辑</a>
                <a-popconfirm v-if="canDelete" title="确认删除?" @confirm="handleDelete(record.id)">
                  <a class="danger-link">删除</a>
                </a-popconfirm>
                <span v-if="!canUpdate && !canDelete">-</span>
              </a-space>
            </template>
            <template v-else>{{ record[column.dataIndex ?? column.key] ?? '-' }}</template>
            </template>
          </a-table>
        </div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="drawerVisible" :title="schema.detailTitle" width="600" destroy-on-close>
      <a-descriptions :column="2" bordered size="small" v-if="currentItem">
        <template v-for="field in schema.detailFields" :key="field.name">
          <a-descriptions-item :label="field.label" :span="field.span || 1">
            <template v-if="field.render === 'tag'">
              <a-tag v-if="tagLabel(currentItem[field.name])" :color="tagColor(currentItem[field.name])">
                {{ tagLabel(currentItem[field.name]) }}
              </a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="field.render === 'price'">
              {{ currentItem[field.name] ? `¥${currentItem[field.name]}` : '-' }}
            </template>
            <template v-else-if="field.render === 'refRange'">
              {{ formatRefRange(currentItem) }}
            </template>
            <template v-else-if="field.render === 'rx'">
              <a-tag :color="currentItem[field.name] ? 'blue' : 'green'">
                {{ currentItem[field.name] ? '处方药' : '非处方药' }}
              </a-tag>
            </template>
            <template v-else>{{ currentItem[field.name] ?? '-' }}</template>
          </a-descriptions-item>
        </template>
      </a-descriptions>
    </a-drawer>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="isEdit ? schema.editLabel : schema.newLabel"
      :width="schema.modalWidth" @ok="handleSubmit" @cancel="handleModalCancel" destroy-on-close
      :confirm-loading="submitting">
      <a-form ref="formRef" :model="formData" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <template v-for="field in gridFields" :key="field.name">
            <a-col :span="12">
              <a-form-item :label="field.label" :name="field.name">
                <a-input v-if="field.type === 'input'" v-model:value="formData[field.name]"
                  :placeholder="field.placeholder" :disabled="isEdit && field.disabledOnEdit" />
                <a-input-number v-else-if="field.type === 'number'" v-model:value="formData[field.name]"
                  :min="0" :precision="field.name === 'price' ? 2 : undefined"
                  style="width: 100%" :placeholder="field.placeholder" />
                <a-select v-else-if="field.type === 'select'" v-model:value="formData[field.name]"
                  :placeholder="field.placeholder" :allow-clear="!field.required">
                  <a-select-option v-for="opt in optionsOf(field)" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </a-select-option>
                </a-select>
                <a-tree-select v-else-if="field.type === 'tree-select'" v-model:value="formData[field.name]"
                  :tree-data="drugCategoryTree"
                  :field-names="{ label: 'name', value: 'id', children: 'children' }"
                  placeholder="选择分类" allow-clear tree-line />
              </a-form-item>
            </a-col>
          </template>
        </a-row>
        <template v-for="field in fullFields" :key="field.name">
          <a-form-item :label="field.label" :name="field.name" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
            <a-textarea v-if="field.type === 'textarea'" v-model:value="formData[field.name]"
              :placeholder="field.placeholder" :rows="2" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { usePermission } from '@/hooks/usePermission'
import DrugCategoryTree from './DrugCategoryTree.vue'
import IcdChapterMenu from './IcdChapterMenu.vue'
import {
  dictionaryApis, dictionarySchemas, dynamicOptionLoaders, STATUS_TAG, TYPE_TAG_COLORS,
  type DictionaryRecord, type DictionaryTypeKey, type FormFieldDef, type OptionsKey,
  type SidePanelKey,
} from './config'

defineOptions({ name: 'DictionaryListView' })

const props = withDefaults(
  defineProps<{
    type: DictionaryTypeKey
    embedded?: boolean
  }>(),
  { embedded: false },
)

const schema = dictionarySchemas[props.type]
const api = dictionaryApis[props.type]

const sidePanels: Record<SidePanelKey, Component> = {
  drugCategoryTree: DrugCategoryTree,
  icdChapterMenu: IcdChapterMenu,
}
const sidePanelComponent = schema.sidePanel ? sidePanels[schema.sidePanel] : undefined

// ==================== 权限 ====================
const { hasPermission } = usePermission()
const canCreate = computed(() => hasPermission('masterdata:create'))
const canUpdate = computed(() => hasPermission('masterdata:update'))
const canDelete = computed(() => hasPermission('masterdata:delete'))

// ==================== 筛选与列表 ====================
const filters = reactive<Record<string, any>>({ keyword: '' })
schema.filters.forEach(f => { filters[f.name] = undefined })

const sidePanelRef = ref<{ clearSelection: () => void } | null>(null)
const sideValue = ref<any>(undefined)
const drugCategoryTree = ref<any[]>([])

const loading = ref(false)
const tableData = ref<DictionaryRecord[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const dynamicOptions = reactive<Record<string, Array<{ label: string; value: any }>>>({})

function optionsOf(field: { options?: any[]; optionsKey?: OptionsKey }) {
  if (field.options) return field.options
  if (field.optionsKey) return dynamicOptions[field.optionsKey] || []
  return []
}

async function loadDynamicOptions() {
  const keys = new Set<OptionsKey>()
  schema.filters.forEach(f => { if (f.optionsKey) keys.add(f.optionsKey) })
  schema.fields.forEach(f => { if (f.optionsKey && f.optionsKey !== 'drugCategoryTree') keys.add(f.optionsKey) })
  await Promise.all([...keys].map(async key => {
    try {
      const res = await dynamicOptionLoaders[key]()
      dynamicOptions[key] = (res.data.data || []).map((v: string) => ({ label: v, value: v }))
    } catch {
      dynamicOptions[key] = []
    }
  }))
}

async function fetchData(page = 1) {
  loading.value = true
  try {
    const params: any = { page, page_size: pagination.pageSize }
    if (filters.keyword) params.keyword = filters.keyword
    schema.filters.forEach(f => { if (filters[f.name]) params[f.name] = filters[f.name] })
    if (schema.sideParam && sideValue.value !== undefined) params[schema.sideParam] = sideValue.value

    const res = await api.list(params)
    const data = res.data.data
    tableData.value = data?.content || []
    pagination.total = data?.totalElements || 0
    pagination.current = (data?.number ?? page - 1) + 1
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.pageSize = pag.pageSize
  fetchData(pag.current)
}

function resetFilters() {
  schema.filters.forEach(f => { filters[f.name] = undefined })
  filters.keyword = ''
  sideValue.value = undefined
  sidePanelRef.value?.clearSelection()
  fetchData(1)
}

function onSideSelect(value: any) {
  sideValue.value = value
  fetchData(1)
}

function onSideLoaded(tree: any[]) {
  drugCategoryTree.value = tree
}

function handleImport() {
  message.info('导入功能开发中')
}

// ==================== 详情抽屉 ====================
const drawerVisible = ref(false)
const currentItem = ref<DictionaryRecord | null>(null)

async function openDetail(record: DictionaryRecord) {
  drawerVisible.value = true
  try {
    const res = await api.get(record.id)
    currentItem.value = res.data.data
  } catch {
    currentItem.value = record
  }
}

function statusTagOf(status: string) {
  return STATUS_TAG[status] || { color: 'default', label: status || '-' }
}

function tagLabel(value: any) {
  if (value === null || value === undefined || value === '') return ''
  return STATUS_TAG[value]?.label ?? value
}

function tagColor(value: any) {
  return STATUS_TAG[value]?.color ?? TYPE_TAG_COLORS[value] ?? 'default'
}

function formatRefRange(record: DictionaryRecord) {
  if (record.refRangeLow !== null && record.refRangeLow !== undefined
    && record.refRangeHigh !== null && record.refRangeHigh !== undefined) {
    return `${record.refRangeLow}–${record.refRangeHigh} ${record.unit || ''}`.trim()
  }
  return record.refRangeText || '-'
}

// ==================== 新增/编辑 ====================
const modalVisible = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const editingId = ref<number>()
const isEdit = computed(() => !!editingId.value)

const gridFields = computed(() => schema.fields.filter(f => !f.full))
const fullFields = computed(() => schema.fields.filter(f => f.full))

function defaultOf(field: FormFieldDef) {
  if (field.default !== undefined) return field.default
  if (field.type === 'input' || field.type === 'textarea') return ''
  return undefined
}

const formData = reactive<Record<string, any>>({})
schema.fields.forEach(f => { formData[f.name] = defaultOf(f) })

const rules = computed(() => {
  const result: Record<string, any[]> = {}
  schema.fields.forEach(f => {
    if (f.required) {
      result[f.name] = [{ required: true, message: f.requiredMessage || `请输入${f.label}` }]
    }
  })
  return result
})

function handleCreate() {
  editingId.value = undefined
  schema.fields.forEach(f => { formData[f.name] = defaultOf(f) })
  modalVisible.value = true
}

function handleEdit(record: DictionaryRecord) {
  editingId.value = record.id
  schema.fields.forEach(f => {
    formData[f.name] = record[f.name] ?? defaultOf(f)
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value && editingId.value) {
      await api.update(editingId.value, { ...formData })
      message.success('更新成功')
    } else {
      await api.create({ ...formData })
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData(pagination.current)
  } catch {
    message.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

function handleModalCancel() {
  modalVisible.value = false
  formRef.value?.resetFields()
}

async function handleDelete(id: number) {
  try {
    await api.remove(id)
    message.success('删除成功')
    fetchData(pagination.current)
  } catch {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadDynamicOptions()
  fetchData()
})
</script>

<style lang="scss" scoped>
/* ==================== 浅色皮肤（默认，与全站内容页一致） ==================== */
.dict-list-view {
  min-height: 100%;
}

.dict-list-view--embedded {
  min-height: auto;
}

.dict-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  .dict-title {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.88);
  }
}

.dict-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 250px);
}

.dict-list-view--embedded .dict-layout {
  height: auto;
  min-height: 480px;
}

.dict-item-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.filter-card) {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

:deep(.table-card) {
  flex: 1;
  min-height: 0;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 8px;
}

.danger-link {
  color: #ff4d4f;
}

/* ==================== 深色皮肤（仅内嵌主数据管理页时） ==================== */
.dict-skin-dark {
  :deep(.dict-type-panel),
  :deep(.filter-card),
  :deep(.table-card) {
    background: var(--tech-panel-bg);
    border-color: var(--tech-panel-border);
  }

  :deep(.dict-header .dict-title),
  :deep(.table-card),
  :deep(.filter-card) {
    color: var(--tech-text-body, #e6edf6);
  }

  /* 深色表格 */
  :deep(.dict-table) {
    background: transparent;

    .ant-table {
      background: transparent;
      color: var(--tech-text-body, #e6edf6);
    }

    .ant-table-thead > tr > th {
      background: rgba(15, 26, 46, 0.8) !important;
      color: var(--tech-text-muted, #94a3b8) !important;
      border-bottom: 1px solid rgba(148, 163, 184, 0.12) !important;

      &::before {
        background-color: transparent !important;
      }
    }

    .ant-table-tbody > tr > td {
      background: transparent;
      color: var(--tech-text-body, #e6edf6);
      border-bottom: 1px solid rgba(148, 163, 184, 0.12);
    }

    .ant-table-tbody > tr:hover > td,
    .ant-table-tbody > tr.ant-table-row-selected > td {
      background: rgba(34, 211, 238, 0.06) !important;
    }

    .ant-table-tbody > tr td.ant-table-cell-fix-right {
      background: rgba(15, 26, 46, 0.72);
    }

    .ant-table-tbody > tr:hover td.ant-table-cell-fix-right {
      background: rgba(20, 34, 60, 0.85);
    }

    a {
      color: var(--tech-primary, #22d3ee);
    }

    .ant-pagination .ant-pagination-item,
    .ant-pagination .ant-pagination-prev .ant-pagination-item-link,
    .ant-pagination .ant-pagination-next .ant-pagination-item-link {
      background: rgba(15, 26, 46, 0.6);
      border-color: rgba(148, 163, 184, 0.16);

      a {
        color: var(--tech-text-body, #e6edf6);
      }
    }

    .ant-pagination .ant-pagination-item-active {
      border-color: var(--tech-primary, #22d3ee);

      a {
        color: var(--tech-primary, #22d3ee);
      }
    }
  }

  /* 深色筛选输入 */
  :deep(.filter-card) {
    .ant-select .ant-select-selector,
    .ant-input-affix-wrapper,
    .ant-input {
      background: var(--tech-input-bg, rgba(6, 11, 22, 0.6)) !important;
      border-color: var(--tech-input-border, rgba(148, 163, 184, 0.2)) !important;
      color: var(--tech-text-body, #e6edf6) !important;
    }

    .ant-select-selection-placeholder,
    .ant-input::placeholder {
      color: var(--tech-text-dim, #64748b);
    }

    .ant-select-arrow,
    .ant-input-search-button .anticon {
      color: var(--tech-text-muted, #94a3b8);
    }

    .ant-btn {
      background: rgba(15, 26, 46, 0.6);
      border-color: rgba(148, 163, 184, 0.16);
      color: var(--tech-text-body, #e6edf6);
    }
  }

  /* 深色侧栏（分类树 / ICD 章节菜单） */
  :deep(.dict-type-panel) {
    color: var(--tech-text-body, #e6edf6);

    .ant-input-affix-wrapper,
    .ant-input {
      background: var(--tech-input-bg, rgba(6, 11, 22, 0.6)) !important;
      border-color: var(--tech-input-border, rgba(148, 163, 184, 0.2)) !important;
      color: var(--tech-text-body, #e6edf6) !important;
    }

    .ant-input::placeholder {
      color: var(--tech-text-dim, #64748b);
    }

    .ant-input-search-button .anticon {
      color: var(--tech-text-muted, #94a3b8);
    }

    .category-code {
      color: var(--tech-text-dim, #64748b);
    }

    .ant-tree {
      background: transparent;
      color: var(--tech-text-body, #e6edf6);

      .ant-tree-node-content-wrapper:hover {
        background: rgba(34, 211, 238, 0.08);
        color: var(--tech-primary, #22d3ee);
      }

      .ant-tree-node-content-wrapper.ant-tree-node-selected {
        background: rgba(34, 211, 238, 0.16) !important;
        color: var(--tech-primary, #22d3ee);
      }

      .ant-tree-switcher {
        color: var(--tech-text-muted, #94a3b8);
      }

      .ant-tree-indent-unit::before {
        border-color: rgba(148, 163, 184, 0.2);
      }
    }

    .chapter-list .ant-menu {
      background: transparent;
      border-inline-end: none !important;

      .ant-menu-item {
        color: var(--tech-text-body, #cbd5e1);

        &:hover {
          background: rgba(34, 211, 238, 0.08);
          color: var(--tech-primary, #22d3ee);
        }

        &-selected {
          background: rgba(34, 211, 238, 0.16);
          color: var(--tech-primary, #22d3ee);
        }
      }
    }

    .chapter-code {
      color: #38bdf8;
    }

    .chapter-name {
      color: #f1f5f9;
    }
  }
}
</style>
