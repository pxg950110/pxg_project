<template>
  <PageContainer>
    <template #default>
      <!-- Page Header -->
      <div class="perm-page-header">
        <div class="perm-header-top">
          <h2 class="perm-page-title">权限管理</h2>
          <el-button type="primary" @click="handleAdd">
            <el-icon class="mr-1"><Plus /></el-icon> 新增权限
          </el-button>
        </div>
        <p class="perm-page-desc">管理菜单权限、API权限和数据权限。权限通过角色分配给用户。</p>
      </div>

      <!-- Filter Bar -->
      <div class="perm-filter-bar">
        <el-select
          v-model="filterType"
          clearable
          placeholder="类型: 全部"
          class="perm-filter-select"
        >
          <el-option value="MENU" label="MENU" />
          <el-option value="API" label="API" />
          <el-option value="DATA" label="DATA" />
          <el-option value="BUTTON" label="BUTTON" />
        </el-select>

        <el-input
          v-model="filterKeyword"
          placeholder="搜索权限名称..."
          class="perm-filter-search"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- Hierarchical Permission Table -->
      <el-table
        :data="filteredData"
        row-key="id"
        default-expand-all
        :indent="0"
        v-loading="loading"
        :tree-props="{ children: 'children' }"
        class="perm-table"
        :row-class-name="getRowClassName"
      >
        <el-table-column label="权限名称" prop="name">
          <template #default="{ row }">
            <span v-if="row.isParent" class="perm-parent-name">
              <span class="perm-icon-folder">&#128193;</span>
              <span class="perm-parent-text">{{ row.name }}</span>
            </span>
            <span v-else class="perm-child-name">
              <span class="perm-icon-file">{{ row.type === 'API' ? '&#128295;' : '&#128196;' }}</span>
              <span>{{ row.name }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="编码" prop="code">
          <template #default="{ row }">
            <span class="perm-code">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" prop="type" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.type === 'MENU'" :style="{ backgroundColor: '#0ea5e9', borderColor: '#0ea5e9', color: '#fff' }">MENU</el-tag>
            <el-tag v-else-if="row.type === 'API'" :style="{ backgroundColor: '#8b5cf6', borderColor: '#8b5cf6', color: '#fff' }">API</el-tag>
            <el-tag v-else-if="row.type === 'DATA'" :style="{ backgroundColor: '#f59e0b', borderColor: '#f59e0b', color: '#fff' }">DATA</el-tag>
            <el-tag v-else-if="row.type === 'BUTTON'" :style="{ backgroundColor: '#10b981', borderColor: '#10b981', color: '#fff' }">BUTTON</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联角色数" prop="roleCount" width="100" align="center">
          <template #default="{ row }">
            <span class="perm-role-count">
              {{ row.isParent ? '—' : row.roleCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="right">
          <template #default="{ row }">
            <div class="perm-action">
              <a v-if="!row.isParent" class="perm-edit-link" @click="handleEdit(row)">编辑</a>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination Row -->
      <div class="perm-pagination-row">
        <span class="perm-total-text">共 {{ totalCount }} 个权限项</span>
      </div>

      <!-- Add/Edit Permission Modal -->
      <el-dialog
        v-model="modalVisible"
        :title="editingRecord ? '编辑权限' : '新增权限'"
        width="520px"
      >
        <el-form label-position="top" ref="formRef" :model="formData">
          <el-form-item label="权限名称" prop="name" :rules="[{ required: true, message: '请输入权限名称' }]">
            <el-input v-model="formData.name" placeholder="例如：用户管理" />
          </el-form-item>
          <el-form-item label="权限编码" prop="code" :rules="[{ required: true, message: '请输入权限编码' }]">
            <el-input v-model="formData.code" placeholder="例如：user:manage" />
          </el-form-item>
          <el-form-item label="权限类型" prop="type" :rules="[{ required: true, message: '请选择权限类型' }]">
            <el-select v-model="formData.type" placeholder="选择类型">
              <el-option value="MENU" label="MENU" />
              <el-option value="API" label="API" />
              <el-option value="DATA" label="DATA" />
              <el-option value="BUTTON" label="BUTTON" />
            </el-select>
          </el-form-item>
          <el-form-item label="上级权限">
            <el-select v-model="formData.parentId" clearable placeholder="无（顶级权限）">
              <el-option v-for="p in permissions" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="modalVisible = false">取消</el-button>
          <el-button type="primary" :loading="modalLoading" @click="handleModalOk">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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

async function fetchPermissions() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    permissions.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function getRowClassName({ row }: { row: PermissionItem }) {
  return row.isParent ? 'perm-row-parent' : 'perm-row-child'
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
    await formRef.value?.validate()
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
    ElMessage.success(editingRecord.value ? '权限已更新' : '权限已创建')
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
  color: #0f172a;
  margin: 0;
}

.perm-page-desc {
  font-size: 13px;
  color: #94a3b8;
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
  color: #0f172a;
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
  color: #0f172a;
}

.perm-icon-file {
  font-size: 14px;
}

/* Code column */
.perm-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 12px;
  color: #94a3b8;
}

/* Role count column */
.perm-role-count {
  display: inline-block;
  text-align: center;
  width: 100%;
  color: #64748b;
}

/* Action column */
.perm-action {
  display: flex;
  justify-content: flex-end;
}

.perm-edit-link {
  font-size: 13px;
  color: #0ea5e9;
  cursor: pointer;
}

.perm-edit-link:hover {
  color: #38bdf8;
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
  color: #94a3b8;
}
</style>
