<template>
  <PageContainer title="数据字典管理">
    <div class="dict-layout">
      <!-- 左侧: 字典类型列表 -->
      <div class="dict-type-panel">
        <div class="panel-header">
          <h4 style="margin: 0">字典类型</h4>
          <el-button link type="primary" size="small" @click="handleCreateType">
            <el-icon class="mr-0.5"><Plus /></el-icon>
            新增
          </el-button>
        </div>
        <div class="type-search">
          <el-input
            v-model="typeKeyword"
            placeholder="搜索字典类型"
            clearable
            :prefix-icon="Search"
          />
        </div>
        <div class="type-list">
          <div
            v-for="item in filteredTypes"
            :key="item.id"
            class="type-list-item"
            :class="{ 'type-list-item--active': selectedType?.id === item.id }"
            @click="handleSelectType(item)"
          >
            <div class="type-menu-item">
              <div>
                <div class="type-name">{{ item.name }}</div>
                <div class="type-code">{{ item.code }}</div>
              </div>
              <el-dropdown trigger="click" @command="(cmd: string | number | object) => cmd === 'edit' ? handleEditType(item) : handleDeleteType(item)">
                <span class="type-more" @click.stop>
                  <el-icon><More /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="delete">
                      <span style="color: #ef4444">删除</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧: 字典项列表 -->
      <div class="dict-item-panel">
        <template v-if="selectedType">
          <div class="panel-header">
            <h4 style="margin: 0">{{ selectedType.name }} - 字典项</h4>
            <el-button type="primary" size="small" @click="handleCreateItem">
              <el-icon class="mr-0.5"><Plus /></el-icon>
              新增字典项
            </el-button>
          </div>

          <el-table :data="itemData" v-loading="itemLoading" row-key="id" size="small">
            <el-table-column label="编码" prop="code" width="120" />
            <el-table-column label="名称" prop="name" width="140" />
            <el-table-column label="值" prop="value" width="100" />
            <el-table-column label="排序号" prop="sort_order" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span class="status-cell">
                  <span
                    class="status-dot"
                    :style="{ background: row.status === 'ENABLED' ? '#10b981' : '#cbd5e1' }"
                  />
                  {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center gap-2">
                  <el-button link type="primary" size="small" @click="handleEditItem(row)">编辑</el-button>
                  <el-popconfirm title="确定删除？" @confirm="handleDeleteItem(row)">
                    <template #reference>
                      <el-button link type="danger" size="small">删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="mt-4 justify-end"
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="itemPagination.total"
            :current-page="itemPagination.current"
            :page-size="itemPagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="handleItemPageChange"
            @size-change="handleItemSizeChange"
          />
        </template>
        <el-empty v-else description="请选择左侧字典类型" :image-size="60" style="margin-top: 120px" />
      </div>
    </div>

    <!-- 字典类型编辑弹窗 -->
    <el-dialog
      v-model="typeModalVisible"
      :title="isEditType ? '编辑字典类型' : '新建字典类型'"
      width="520px"
      :destroy-on-close="true"
    >
      <el-form
        ref="typeFormRef"
        :model="typeFormState"
        :rules="typeFormRules"
        label-width="100px"
      >
        <el-form-item label="类型编码" name="code">
          <el-input v-model="typeFormState.code" placeholder="如: gender, blood_type" :disabled="isEditType" />
        </el-form-item>
        <el-form-item label="类型名称" name="name">
          <el-input v-model="typeFormState.name" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="备注" name="remark">
          <el-input v-model="typeFormState.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeModalVisible = false; typeFormRef?.resetFields()">取消</el-button>
        <el-button type="primary" :loading="typeSubmitLoading" @click="handleTypeSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典项编辑弹窗 -->
    <el-dialog
      v-model="itemModalVisible"
      :title="isEditItem ? '编辑字典项' : '新建字典项'"
      width="520px"
      :destroy-on-close="true"
    >
      <el-form
        ref="itemFormRef"
        :model="itemFormState"
        :rules="itemFormRules"
        label-width="100px"
      >
        <el-form-item label="字典编码" name="code">
          <el-input v-model="itemFormState.code" placeholder="如: M, F" :disabled="isEditItem" />
        </el-form-item>
        <el-form-item label="字典名称" name="name">
          <el-input v-model="itemFormState.name" placeholder="如: 男, 女" />
        </el-form-item>
        <el-form-item label="字典值" name="value">
          <el-input v-model="itemFormState.value" placeholder="如: 1, 2" />
        </el-form-item>
        <el-form-item label="排序号" name="sort_order">
          <el-input-number v-model="itemFormState.sort_order" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" name="status">
          <el-radio-group v-model="itemFormState.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" name="remark">
          <el-input v-model="itemFormState.remark" type="textarea" :rows="2" placeholder="备注信息（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemModalVisible = false; itemFormRef?.resetFields()">取消</el-button>
        <el-button type="primary" :loading="itemSubmitLoading" @click="handleItemSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormItemRule } from 'element-plus'
import { Plus, Search, More } from '@element-plus/icons-vue'
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

async function loadTypes() {
  try {
    const res = await getDictTypes({ page: 1, page_size: 500 })
    typeData.value = res.data.data.items ?? []
  } catch {
    // error handled by request interceptor
  }
}

function handleSelectType(item: any) {
  selectedType.value = item
  itemPagination.current = 1
  loadItems()
}

// ===== 字典类型 CRUD =====
const typeFormState = reactive({
  code: '',
  name: '',
  remark: '',
})

const typeFormRules: Record<string, FormItemRule[]> = {
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
  await typeFormRef.value?.validate()
  typeSubmitLoading.value = true
  try {
    const data = { ...typeFormState }
    if (isEditType.value) {
      await updateDictType(editingTypeId.value!, data)
      ElMessage.success('更新成功')
    } else {
      await createDictType(data)
      ElMessage.success('创建成功')
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
  ElMessage.success('删除成功')
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
})

async function loadItems() {
  if (!selectedType.value) return
  itemLoading.value = true
  try {
    const res = await getDictItems(selectedType.value.id, {
      page: itemPagination.current,
      page_size: itemPagination.pageSize,
    })
    const data = (res.data as any)?.data || {}
    itemData.value = data.items ?? []
    itemPagination.total = data.total ?? 0
  } finally {
    itemLoading.value = false
  }
}

function handleItemPageChange(page: number) {
  itemPagination.current = page
  loadItems()
}

function handleItemSizeChange(size: number) {
  itemPagination.pageSize = size
  itemPagination.current = 1
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

const itemFormRules: Record<string, FormItemRule[]> = {
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
  await itemFormRef.value?.validate()
  itemSubmitLoading.value = true
  try {
    const data = {
      ...itemFormState,
      type_id: selectedType.value.id,
    }
    if (isEditItem.value) {
      await updateDictItem(editingItemId.value!, data)
      ElMessage.success('更新成功')
    } else {
      await createDictItem(data)
      ElMessage.success('创建成功')
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
  ElMessage.success('删除成功')
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
  border: 1px solid #f1f5f9;
  border-radius: 8px;
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
  border-bottom: 1px solid #f1f5f9;
}

.type-search {
  padding: 8px 12px;
  border-bottom: 1px solid #f1f5f9;
}

.type-list {
  flex: 1;
  overflow-y: auto;
}

.type-list-item {
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.type-list-item:hover {
  background: #f8fafc;
}

.type-list-item--active {
  background: #f0f9ff;
}

.type-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.type-name {
  font-size: 14px;
  color: #0f172a;
}

.type-code {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.type-more {
  display: inline-flex;
  color: #94a3b8;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
}

.type-more:hover {
  background: rgba(15, 23, 42, 0.06);
  color: #0f172a;
}

.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
