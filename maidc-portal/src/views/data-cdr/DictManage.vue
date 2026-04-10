<template>
  <PageContainer title="数据字典管理">
    <div class="dict-layout">
      <!-- 左侧: 字典类型列表 -->
      <div class="dict-type-panel">
        <div class="panel-header">
          <h4 style="margin: 0">字典类型</h4>
          <a-button type="link" size="small" @click="handleCreateType">
            <template #icon><PlusOutlined /></template>
            新增
          </a-button>
        </div>
        <div class="type-search">
          <a-input
            v-model:value="typeKeyword"
            placeholder="搜索字典类型"
            allow-clear
            @change="filterTypes"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </div>
        <a-menu
          v-model:selectedKeys="selectedTypeKeys"
          mode="inline"
          style="border-inline-end: none"
        >
          <a-menu-item
            v-for="item in filteredTypes"
            :key="item.id"
            @click="handleSelectType(item)"
          >
            <div class="type-menu-item">
              <div>
                <div class="type-name">{{ item.name }}</div>
                <div class="type-code">{{ item.code }}</div>
              </div>
              <a-dropdown :trigger="['click']">
                <a-button type="text" size="small" @click.stop>
                  <EllipsisOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleEditType(item)">编辑</a-menu-item>
                    <a-menu-item @click="handleDeleteType(item)">
                      <span style="color: #ff4d4f">删除</span>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
          </a-menu-item>
        </a-menu>
      </div>

      <!-- 右侧: 字典项列表 -->
      <div class="dict-item-panel">
        <template v-if="selectedType">
          <div class="panel-header">
            <h4 style="margin: 0">{{ selectedType.name }} - 字典项</h4>
            <a-button type="primary" size="small" @click="handleCreateItem">
              <template #icon><PlusOutlined /></template>
              新增字典项
            </a-button>
          </div>

          <a-table
            :columns="itemColumns"
            :data-source="itemData"
            :loading="itemLoading"
            :pagination="itemPagination"
            @change="handleItemTableChange"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-badge :status="record.status === 'ENABLED' ? 'success' : 'default'" :text="record.status === 'ENABLED' ? '启用' : '禁用'" />
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="handleEditItem(record)">编辑</a-button>
                  <a-popconfirm title="确定删除？" @confirm="handleDeleteItem(record)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </template>
        <a-empty v-else description="请选择左侧字典类型" style="margin-top: 120px" />
      </div>
    </div>

    <!-- 字典类型编辑弹窗 -->
    <a-modal
      v-model:open="typeModalVisible"
      :title="isEditType ? '编辑字典类型' : '新建字典类型'"
      :confirm-loading="typeSubmitLoading"
      @ok="handleTypeSubmit"
      @cancel="typeFormRef?.resetFields()"
      destroy-on-close
    >
      <a-form
        ref="typeFormRef"
        :model="typeFormState"
        :rules="typeFormRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="类型编码" name="code">
          <a-input v-model:value="typeFormState.code" placeholder="如: gender, blood_type" :disabled="isEditType" />
        </a-form-item>
        <a-form-item label="类型名称" name="name">
          <a-input v-model:value="typeFormState.name" placeholder="请输入类型名称" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="typeFormState.remark" :rows="2" placeholder="备注信息（选填）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 字典项编辑弹窗 -->
    <a-modal
      v-model:open="itemModalVisible"
      :title="isEditItem ? '编辑字典项' : '新建字典项'"
      :confirm-loading="itemSubmitLoading"
      @ok="handleItemSubmit"
      @cancel="itemFormRef?.resetFields()"
      destroy-on-close
    >
      <a-form
        ref="itemFormRef"
        :model="itemFormState"
        :rules="itemFormRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="字典编码" name="code">
          <a-input v-model:value="itemFormState.code" placeholder="如: M, F" :disabled="isEditItem" />
        </a-form-item>
        <a-form-item label="字典名称" name="name">
          <a-input v-model:value="itemFormState.name" placeholder="如: 男, 女" />
        </a-form-item>
        <a-form-item label="字典值" name="value">
          <a-input v-model:value="itemFormState.value" placeholder="如: 1, 2" />
        </a-form-item>
        <a-form-item label="排序号" name="sort_order">
          <a-input-number v-model:value="itemFormState.sort_order" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="itemFormState.status">
            <a-radio value="ENABLED">启用</a-radio>
            <a-radio value="DISABLED">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="itemFormState.remark" :rows="2" placeholder="备注信息（选填）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  SearchOutlined,
  EllipsisOutlined,
} from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getDictTypes,
  createDictType,
  updateDictType,
  deleteDictType,
  getDictItems,
  createDictItem,
  updateDictItem,
  deleteDictItem,
} from '@/api/data'

defineOptions({ name: 'DictManage' })

// ===== 字典类型 =====
const typeData = ref<any[]>([])
const typeKeyword = ref('')
const selectedTypeKeys = ref<string[]>([])
const selectedType = ref<any>(null)
const typeModalVisible = ref(false)
const typeSubmitLoading = ref(false)
const typeFormRef = ref<FormInstance>()
const editingTypeId = ref<number | null>(null)
const isEditType = computed(() => editingTypeId.value !== null)

const filteredTypes = computed(() => {
  if (!typeKeyword.value) return typeData.value
  const kw = typeKeyword.value.toLowerCase()
  return typeData.value.filter(
    (item: any) => item.name.toLowerCase().includes(kw) || item.code.toLowerCase().includes(kw),
  )
})

function filterTypes() {
  // Computed property handles filtering reactively
}

async function loadTypes() {
  try {
    const res = await getDictTypes({ page: 1, page_size: 500 })
    typeData.value = res.data.data.items
  } catch {
    // error handled by request interceptor
  }
}

function handleSelectType(item: any) {
  selectedType.value = item
  loadItems()
}

// ===== 字典类型 CRUD =====
const typeFormState = reactive({
  code: '',
  name: '',
  remark: '',
})

const typeFormRules: Record<string, Rule[]> = {
  code: [{ required: true, message: '请输入类型编码' }],
  name: [{ required: true, message: '请输入类型名称' }],
}

function handleCreateType() {
  editingTypeId.value = null
  Object.assign(typeFormState, { code: '', name: '', remark: '' })
  typeModalVisible.value = true
}

function handleEditType(item: any) {
  editingTypeId.value = item.id
  Object.assign(typeFormState, {
    code: item.code,
    name: item.name,
    remark: item.remark || '',
  })
  typeModalVisible.value = true
}

async function handleTypeSubmit() {
  await typeFormRef.value?.validateFields()
  typeSubmitLoading.value = true
  try {
    const data = { ...typeFormState }
    if (isEditType.value) {
      await updateDictType(editingTypeId.value!, data)
      message.success('更新成功')
    } else {
      await createDictType(data)
      message.success('创建成功')
    }
    typeModalVisible.value = false
    typeFormRef.value?.resetFields()
    loadTypes()
  } finally {
    typeSubmitLoading.value = false
  }
}

async function handleDeleteType(item: any) {
  await deleteDictType(item.id)
  message.success('删除成功')
  if (selectedType.value?.id === item.id) {
    selectedType.value = null
  }
  loadTypes()
}

// ===== 字典项 =====
const itemData = ref<any[]>([])
const itemLoading = ref(false)
const itemPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
})

const itemColumns = [
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', width: 140 },
  { title: '值', dataIndex: 'value', width: 100 },
  { title: '排序号', dataIndex: 'sort_order', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
]

async function loadItems() {
  if (!selectedType.value) return
  itemLoading.value = true
  try {
    const res = await getDictItems(selectedType.value.id, {
      page: itemPagination.current,
      page_size: itemPagination.pageSize,
    })
    itemData.value = res.data.data.items
    itemPagination.total = res.data.data.total
  } finally {
    itemLoading.value = false
  }
}

function handleItemTableChange(pag: any) {
  itemPagination.current = pag.current
  itemPagination.pageSize = pag.pageSize
  loadItems()
}

// ===== 字典项 CRUD =====
const itemModalVisible = ref(false)
const itemSubmitLoading = ref(false)
const itemFormRef = ref<FormInstance>()
const editingItemId = ref<number | null>(null)
const isEditItem = computed(() => editingItemId.value !== null)

const itemFormState = reactive({
  code: '',
  name: '',
  value: '',
  sort_order: 0,
  status: 'ENABLED',
  remark: '',
})

const itemFormRules: Record<string, Rule[]> = {
  code: [{ required: true, message: '请输入字典编码' }],
  name: [{ required: true, message: '请输入字典名称' }],
}

function handleCreateItem() {
  editingItemId.value = null
  Object.assign(itemFormState, {
    code: '', name: '', value: '', sort_order: 0, status: 'ENABLED', remark: '',
  })
  itemModalVisible.value = true
}

function handleEditItem(record: any) {
  editingItemId.value = record.id
  Object.assign(itemFormState, {
    code: record.code,
    name: record.name,
    value: record.value,
    sort_order: record.sort_order ?? 0,
    status: record.status || 'ENABLED',
    remark: record.remark || '',
  })
  itemModalVisible.value = true
}

async function handleItemSubmit() {
  await itemFormRef.value?.validateFields()
  itemSubmitLoading.value = true
  try {
    const data = {
      ...itemFormState,
      type_id: selectedType.value.id,
    }
    if (isEditItem.value) {
      await updateDictItem(editingItemId.value!, data)
      message.success('更新成功')
    } else {
      await createDictItem(data)
      message.success('创建成功')
    }
    itemModalVisible.value = false
    itemFormRef.value?.resetFields()
    loadItems()
  } finally {
    itemSubmitLoading.value = false
  }
}

async function handleDeleteItem(record: any) {
  await deleteDictItem(record.id)
  message.success('删除成功')
  loadItems()
}

onMounted(() => loadTypes())
</script>

<style scoped>
.dict-layout {
  display: flex;
  gap: 0;
  min-height: 600px;
}

.dict-type-panel {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dict-item-panel {
  flex: 1;
  min-width: 0;
  padding-left: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.type-search {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.type-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.type-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}

.type-code {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}

.dict-type-panel :deep(.ant-menu-item) {
  height: auto;
  line-height: normal;
  padding: 10px 16px;
}
</style>
