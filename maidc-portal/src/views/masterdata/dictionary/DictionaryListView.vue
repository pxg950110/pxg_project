<template>
  <div :class="embedded ? 'dict-list-view dict-list-view--embedded' : 'dict-list-view'">
    <div v-if="!embedded" class="dict-header">
      <h2 class="dict-title">{{ schema.title }}</h2>
      <div class="flex items-center gap-2">
        <el-button v-if="schema.showImport" @click="handleImport">
          <el-icon class="mr-1"><Upload /></el-icon>
          导入
        </el-button>
        <el-button v-if="canCreate" type="primary" @click="handleCreate">
          <el-icon class="mr-1"><Plus /></el-icon>
          {{ schema.newLabel }}
        </el-button>
      </div>
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
        <el-card shadow="never" class="filter-card" :body-style="{ padding: '12px' }">
          <div class="flex flex-wrap items-center gap-3">
            <el-select
              v-for="filter in schema.filters"
              :key="filter.name"
              v-model="filters[filter.name]"
              :placeholder="filter.placeholder"
              clearable
              class="w-44"
              @change="fetchData(1)"
            >
              <el-option v-for="opt in optionsOf(filter)" :key="opt.value" :value="opt.value" :label="opt.label" />
            </el-select>
            <el-input
              v-model="filters.keyword"
              :placeholder="schema.keywordPlaceholder"
              clearable
              :suffix-icon="Search"
              class="min-w-48 flex-1 md:max-w-sm"
              @keyup.enter="fetchData(1)"
              @clear="fetchData(1)"
            />
            <el-button :icon="Search" @click="fetchData(1)">搜索</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </el-card>

        <!-- 数据表格 -->
        <div class="table-card">
          <el-table
            v-loading="loading"
            class="dict-table"
            :data="tableData"
            row-key="id"
            size="small"
            max-height="calc(100vh - 360px)"
          >
            <el-table-column
              v-for="col in schema.columns"
              :key="col.key ?? col.dataIndex"
              :prop="col.dataIndex ?? col.key"
              :label="col.title"
              :width="col.width"
              :fixed="col.fixed"
              :show-overflow-tooltip="col.ellipsis"
            >
              <template #default="{ row }">
                <template v-if="col.key === 'name'">
                  <el-button link type="primary" @click="openDetail(row)">{{ row.name }}</el-button>
                </template>
                <template v-else-if="col.key === 'status'">
                  <el-tag :type="toTagType(statusTagOf(row.status).color)">{{ statusTagOf(row.status).label }}</el-tag>
                </template>
                <template v-else-if="col.key === 'price'">
                  {{ row.price ? `¥${row.price}` : '-' }}
                </template>
                <template v-else-if="col.key === 'insuranceType' || col.key === 'feeType'">
                  <el-tag v-if="row[col.key]" :type="toTagType(TYPE_TAG_COLORS[row[col.key]] || 'default')">
                    {{ row[col.key] }}
                  </el-tag>
                  <span v-else>-</span>
                </template>
                <template v-else-if="col.key === 'refRange'">
                  {{ formatRefRange(row) }}
                </template>
                <template v-else-if="col.key === 'actions'">
                  <div class="flex items-center gap-2">
                    <el-button v-if="canUpdate" link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                    <el-popconfirm v-if="canDelete" title="确认删除?" @confirm="handleDelete(row.id)">
                      <template #reference>
                        <el-button link type="danger" size="small">删除</el-button>
                      </template>
                    </el-popconfirm>
                    <span v-if="!canUpdate && !canDelete">-</span>
                  </div>
                </template>
                <template v-else>{{ row[col.dataIndex ?? col.key ?? ''] ?? '-' }}</template>
              </template>
            </el-table-column>
          </el-table>

          <div class="mt-3 flex justify-end">
            <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.total"
              :current-page="pagination.current"
              :page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="schema.detailTitle" size="600px" :destroy-on-close="true">
      <el-descriptions v-if="currentItem" :column="2" border size="small">
        <template v-for="field in schema.detailFields" :key="field.name">
          <el-descriptions-item :label="field.label" :span="field.span || 1">
            <template v-if="field.render === 'tag'">
              <el-tag v-if="tagLabel(currentItem[field.name])" :type="toTagType(tagColor(currentItem[field.name]))">
                {{ tagLabel(currentItem[field.name]) }}
              </el-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="field.render === 'price'">
              {{ currentItem[field.name] ? `¥${currentItem[field.name]}` : '-' }}
            </template>
            <template v-else-if="field.render === 'refRange'">
              {{ formatRefRange(currentItem) }}
            </template>
            <template v-else-if="field.render === 'rx'">
              <el-tag :type="currentItem[field.name] ? 'primary' : 'success'">
                {{ currentItem[field.name] ? '处方药' : '非处方药' }}
              </el-tag>
            </template>
            <template v-else>{{ currentItem[field.name] ?? '-' }}</template>
          </el-descriptions-item>
        </template>
      </el-descriptions>
    </el-drawer>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="isEdit ? schema.editLabel : schema.newLabel"
      :width="schema.modalWidth"
      destroy-on-close
      @close="handleModalCancel"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <div class="grid grid-cols-1 gap-x-6 md:grid-cols-2">
          <el-form-item v-for="field in gridFields" :key="field.name" :label="field.label" :prop="field.name">
            <el-input v-if="field.type === 'input'" v-model="formData[field.name]"
              :placeholder="field.placeholder" :disabled="isEdit && field.disabledOnEdit" />
            <el-input-number v-else-if="field.type === 'number'" v-model="formData[field.name]"
              :min="0" :precision="field.name === 'price' ? 2 : undefined"
              class="!w-full" :placeholder="field.placeholder" />
            <el-select v-else-if="field.type === 'select'" v-model="formData[field.name]"
              :placeholder="field.placeholder" :clearable="!field.required">
              <el-option v-for="opt in optionsOf(field)" :key="opt.value" :value="opt.value" :label="opt.label" />
            </el-select>
            <el-tree-select v-else-if="field.type === 'tree-select'" v-model="formData[field.name]"
              :data="drugCategoryTree"
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              placeholder="选择分类" clearable />
          </el-form-item>
        </div>
        <el-form-item v-for="field in fullFields" :key="field.name" :label="field.label" :prop="field.name">
          <el-input v-if="field.type === 'textarea'" v-model="formData[field.name]"
            type="textarea" :rows="2" :placeholder="field.placeholder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, type Component } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Plus, Upload, Search } from '@element-plus/icons-vue'
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

function handlePageChange(page: number) {
  fetchData(page)
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  fetchData(1)
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
  ElMessage.info('导入功能开发中')
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

/** AntD 色值名 → el-tag 类型（config 中 STATUS_TAG / TYPE_TAG_COLORS 仍为 AntD 色名） */
function toTagType(color?: string): 'danger' | 'warning' | 'success' | 'primary' | 'info' {
  switch (color) {
    case 'red': return 'danger'
    case 'orange':
    case 'gold':
    case 'volcano': return 'warning'
    case 'green': return 'success'
    case 'blue':
    case 'geekblue': return 'primary'
    default: return 'info'
  }
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
      ElMessage.success('更新成功')
    } else {
      await api.create({ ...formData })
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchData(pagination.current)
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
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
    ElMessage.success('删除成功')
    fetchData(pagination.current)
  } catch {
    ElMessage.error('删除失败')
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
    color: #0f172a;
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
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

:deep(.table-card) {
  flex: 1;
  min-height: 0;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 8px;
}
</style>
