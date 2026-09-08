<template>
  <PageContainer>
    <template #default>
      <!-- Page Header -->
      <div class="perm-page-header">
        <div class="perm-header-top">
          <h2 class="perm-page-title">权限管理</h2>
          <a-button type="primary" @click="handleAdd">
            <PlusOutlined /> 新增权限
          </a-button>
        </div>
        <p class="perm-page-desc">管理菜单权限、API权限和数据权限。权限通过角色分配给用户。</p>
      </div>

      <!-- Filter Bar -->
      <div class="perm-filter-bar">
        <a-select
          v-model:value="filterType"
          allow-clear
          placeholder="类型: 全部"
          class="perm-filter-select"
        >
          <a-select-option value="MENU">MENU</a-select-option>
          <a-select-option value="API">API</a-select-option>
          <a-select-option value="DATA">DATA</a-select-option>
          <a-select-option value="BUTTON">BUTTON</a-select-option>
        </a-select>

        <a-input-search
          v-model:value="filterKeyword"
          placeholder="搜索权限名称..."
          class="perm-filter-search"
          allow-clear
        />
      </div>

      <!-- Hierarchical Permission Table -->
      <a-table
        :columns="columns"
        :data-source="filteredData"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="true"
        :indent-size="0"
        :loading="loading"
        children-column-name="children"
        class="perm-table"
        :row-class-name="getRowClassName"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span v-if="record.isParent" class="perm-parent-name">
              <span class="perm-icon-folder">&#128193;</span>
              <span class="perm-parent-text">{{ record.name }}</span>
            </span>
            <span v-else class="perm-child-name">
              <span class="perm-icon-file">{{ record.type === 'API' ? '&#128295;' : '&#128196;' }}</span>
              <span>{{ record.name }}</span>
            </span>
          </template>

          <template v-if="column.key === 'code'">
            <span class="perm-code">{{ record.code }}</span>
          </template>

          <template v-if="column.key === 'type'">
            <a-tag v-if="record.type === 'MENU'" color="#1677FF">MENU</a-tag>
            <a-tag v-else-if="record.type === 'API'" color="#8b5cf6">API</a-tag>
            <a-tag v-else-if="record.type === 'DATA'" color="#f59e0b">DATA</a-tag>
            <a-tag v-else-if="record.type === 'BUTTON'" color="#10b981">BUTTON</a-tag>
          </template>

          <template v-if="column.key === 'roleCount'">
            <span class="perm-role-count">
              {{ record.isParent ? '\u2014' : record.roleCount }}
            </span>
          </template>

          <template v-if="column.key === 'action'">
            <div class="perm-action">
              <a v-if="!record.isParent" class="perm-edit-link" @click="handleEdit(record)">编辑</a>
            </div>
          </template>
        </template>
      </a-table>

      <!-- Pagination Row -->
      <div class="perm-pagination-row">
        <span class="perm-total-text">共 {{ totalCount }} 个权限项</span>
      </div>

      <!-- Add/Edit Permission Modal -->
      <a-modal
        v-model:open="modalVisible"
        :title="editingRecord ? '编辑权限' : '新增权限'"
        @ok="handleModalOk"
        :confirm-loading="modalLoading"
        width="520px"
      >
        <a-form layout="vertical" ref="formRef" :model="formData">
          <a-form-item label="权限名称" name="name" :rules="[{ required: true, message: '请输入权限名称' }]">
            <a-input v-model:value="formData.name" placeholder="例如：用户管理" />
          </a-form-item>
          <a-form-item label="权限编码" name="code" :rules="[{ required: true, message: '请输入权限编码' }]">
            <a-input v-model:value="formData.code" placeholder="例如：user:manage" />
          </a-form-item>
          <a-form-item label="权限类型" name="type" :rules="[{ required: true, message: '请选择权限类型' }]">
            <a-select v-model:value="formData.type" placeholder="选择类型">
              <a-select-option value="MENU">MENU</a-select-option>
              <a-select-option value="API">API</a-select-option>
              <a-select-option value="DATA">DATA</a-select-option>
              <a-select-option value="BUTTON">BUTTON</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="上级权限">
            <a-select v-model:value="formData.parentId" allow-clear placeholder="无（顶级权限）">
              <a-select-option v-for="p in permissions" :key="p.id" :value="p.id">
                {{ p.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getPermissionTree, createPermission, updatePermission } from '@/api/system'

interface PermissionItem {
  id: number
  name: string
  code: string
  type: 'MENU' | 'API' | 'DATA' | 'BUTTON'
  roleCount: number
  isParent: boolean
  children?: PermissionItem[]
}

const loading = ref(false)
const permissions = ref<PermissionItem[]>([])

// Filters
const filterType = ref<string | undefined>(undefined)
const filterKeyword = ref('')

// Total count
const totalCount = computed(() => {
  let count = 0
  for (const parent of permissions.value) {
    count += (parent.children?.length || 0)
  }
  return count
})

// Filter data by type and keyword
const filteredData = computed(() => {
  const keyword = filterKeyword.value.trim().toLowerCase()
  const hasTypeFilter = !!filterType.value
  const hasKeyword = !!keyword

  if (!hasTypeFilter && !hasKeyword) return permissions.value

  return permissions.value
    .map((parent) => {
      const parentNameMatch = parent.name.toLowerCase().includes(keyword)

      const filteredChildren = (parent.children || []).filter((child) => {
        if (hasTypeFilter && child.type !== filterType.value) return false
        if (hasKeyword && !child.name.toLowerCase().includes(keyword) && !child.code.toLowerCase().includes(keyword)) return false
        return true
      })

      if (parentNameMatch || filteredChildren.length > 0) {
        return {
          ...parent,
          children: parentNameMatch && !hasTypeFilter
            ? parent.children
            : filteredChildren,
        }
      }

      return null
    })
    .filter(Boolean) as PermissionItem[]
})

// Table columns
const columns = [
  { title: '权限名称', key: 'name', dataIndex: 'name' },
  { title: '编码', key: 'code', dataIndex: 'code' },
  { title: '类型', key: 'type', dataIndex: 'type', width: 100 },
  { title: '关联角色数', key: 'roleCount', dataIndex: 'roleCount', width: 100, align: 'center' as const },
  { title: '操作', key: 'action', width: 80, align: 'right' as const },
]

async function fetchPermissions() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    permissions.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function getRowClassName(record: PermissionItem) {
  return record.isParent ? 'perm-row-parent' : 'perm-row-child'
}

const modalVisible = ref(false)
const modalLoading = ref(false)
const editingRecord = ref<PermissionItem | null>(null)
const formRef = ref()
const formData = reactive({
  name: '',
  code: '',
  type: '' as string,
  parentId: undefined as number | undefined,
})

function handleAdd() {
  editingRecord.value = null
  formData.name = ''
  formData.code = ''
  formData.type = ''
  formData.parentId = undefined
  modalVisible.value = true
}

function handleEdit(record: PermissionItem) {
  editingRecord.value = record
  formData.name = record.name
  formData.code = record.code
  formData.type = record.type
  formData.parentId = undefined
  modalVisible.value = true
}

async function handleModalOk() {
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }
  modalLoading.value = true
  try {
    if (editingRecord.value) {
      await updatePermission(editingRecord.value.id, {
        name: formData.name,
        code: formData.code,
        type: formData.type,
        parent_id: formData.parentId,
      })
    } else {
      await createPermission({
        name: formData.name,
        code: formData.code,
        type: formData.type,
        parent_id: formData.parentId,
      })
    }
    message.success(editingRecord.value ? '权限已更新' : '权限已创建')
    modalVisible.value = false
    fetchPermissions()
  } finally {
    modalLoading.value = false
  }
}

onMounted(() => {
  fetchPermissions()
})
</script>

<style scoped>
/* Page Header */
.perm-page-header {
  margin-bottom: 20px;
}

.perm-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.perm-page-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.perm-page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  margin: 6px 0 0;
}

/* Filter Bar */
.perm-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.perm-filter-select {
  min-width: 140px;
  font-size: 13px;
}

.perm-filter-search {
  flex: 1;
  font-size: 13px;
}

/* Table overrides */
.perm-table {
  font-size: 13px;
}

.perm-table :deep(.perm-row-parent) {
  background: #fff;
  height: 48px;
}

.perm-table :deep(.perm-row-parent td) {
  font-weight: 600;
}

.perm-table :deep(.perm-row-child) {
  background: #f8fafc;
  height: 44px;
}

/* Name column */
.perm-parent-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.perm-parent-text {
  font-size: 14px;
}

.perm-icon-folder {
  font-size: 15px;
}

.perm-child-name {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 24px;
  color: rgba(0, 0, 0, 0.88);
}

.perm-icon-file {
  font-size: 14px;
}

/* Code column */
.perm-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

/* Role count column */
.perm-role-count {
  display: inline-block;
  text-align: center;
  width: 100%;
  color: rgba(0, 0, 0, 0.65);
}

/* Action column */
.perm-action {
  display: flex;
  justify-content: flex-end;
}

.perm-edit-link {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
}

.perm-edit-link:hover {
  color: #4096ff;
}

/* Pagination Row */
.perm-pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
}

.perm-total-text {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
