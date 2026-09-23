<template>
  <PageContainer>
    <!-- Custom page header (not using #extra slot) -->
    <template #default>
      <div class="role-page-header">
        <div class="role-page-header-left">
          <h2 class="role-page-title">角色管理</h2>
          <span class="role-page-subtitle">管理系统角色与权限分配</span>
        </div>
        <div class="role-page-header-right">
          <el-button type="primary" @click="roleModal.open()">
            <el-icon class="mr-1"><Plus /></el-icon> 新建角色
          </el-button>
        </div>
      </div>

      <!-- Role Table -->
      <el-table
        :data="tableData"
        v-loading="loading"
        :row-class-name="getRowClassName"
        row-key="id"
      >
        <el-table-column label="角色编码" prop="code" width="120">
          <template #default="{ row }">
            <span :class="{ 'code-admin': row.code === 'ADMIN', 'code-bold': true }">
              {{ row.code }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="角色名称" prop="name" width="120" />
        <el-table-column label="描述" prop="description" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-muted">{{ row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户数" prop="user_count" width="80" align="center">
          <template #default="{ row }">
            <span>{{ row.user_count }}</span>
          </template>
        </el-table-column>
        <el-table-column label="系统内置" width="110" align="center">
          <template #default>
            <el-tag type="primary">系统内置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="created_at" width="120">
          <template #default="{ row }">
            <span class="text-muted">{{ row.created_at }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="right">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <a @click="editModal.open(row)">编辑</a>
              <a @click="router.push(`/system/roles/${row.id}`)">查看</a>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="mt-4 justify-end"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        :current-page="pagination.current"
        :page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />

      <!-- Permission Assignment Card -->
      <div class="permission-card">
        <div class="permission-card-header">
          <div class="permission-card-title">
            <span class="permission-title-text">权限分配 - 平台管理员</span>
          </div>
          <span class="permission-selected-count">已选中 {{ selectedPermissionCount }} 项权限</span>
        </div>
        <div class="permission-columns">
          <div class="permission-column">
            <!-- Dashboard permissions -->
            <div class="permission-group">
              <div class="permission-group-title">仪表盘</div>
              <div class="permission-group-items">
                <el-checkbox
                  v-for="item in permissionGroups.dashboard"
                  :key="item.value"
                  :model-value="selectedPermissions.includes(item.value)"
                  @change="(v) => togglePermission(item.value, Boolean(v))"
                >
                  {{ item.label }}
                </el-checkbox>
              </div>
            </div>
            <!-- Model management permissions -->
            <div class="permission-group">
              <div class="permission-group-title">模型管理</div>
              <div class="permission-group-items">
                <el-checkbox
                  v-for="item in permissionGroups.model"
                  :key="item.value"
                  :model-value="selectedPermissions.includes(item.value)"
                  @change="(v) => togglePermission(item.value, Boolean(v))"
                >
                  {{ item.label }}
                </el-checkbox>
              </div>
            </div>
          </div>
          <div class="permission-column">
            <!-- Data management permissions -->
            <div class="permission-group">
              <div class="permission-group-title">数据管理</div>
              <div class="permission-group-items">
                <el-checkbox
                  v-for="item in permissionGroups.data"
                  :key="item.value"
                  :model-value="selectedPermissions.includes(item.value)"
                  @change="(v) => togglePermission(item.value, Boolean(v))"
                >
                  {{ item.label }}
                </el-checkbox>
              </div>
            </div>
            <!-- Annotation management permissions -->
            <div class="permission-group">
              <div class="permission-group-title">标注管理</div>
              <div class="permission-group-items">
                <el-checkbox
                  v-for="item in permissionGroups.annotation"
                  :key="item.value"
                  :model-value="selectedPermissions.includes(item.value)"
                  @change="(v) => togglePermission(item.value, Boolean(v))"
                >
                  {{ item.label }}
                </el-checkbox>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- New Role Modal -->
      <el-dialog
        v-model="roleModal.visible"
        title="新建角色"
      >
        <el-form label-position="top">
          <el-form-item label="角色名称" required>
            <el-input v-model="roleForm.name" />
          </el-form-item>
          <el-form-item label="角色编码" required>
            <el-input v-model="roleForm.code" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="roleForm.description" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="roleModal.close()">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
        </template>
      </el-dialog>

      <!-- Edit Role Modal -->
      <el-dialog
        v-model="editModal.visible"
        title="编辑角色"
      >
        <el-form label-position="top">
          <el-form-item label="角色名称">
            <el-input v-model="editForm.name" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="editForm.description" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editModal.close()">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleUpdate">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getRoles, createRole, updateRole } from '@/api/system'

const router = useRouter()
const roleModal = useModal()
const editModal = useModal<any>()
const submitting = ref(false)

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getRoles({ page: params.page, page_size: params.pageSize })
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

// Permission definitions
const permissionGroups = reactive({
  dashboard: [
    { label: '系统总览', value: 'dashboard:overview' },
    { label: '模型看板', value: 'dashboard:model' },
    { label: '数据看板', value: 'dashboard:data' },
  ],
  model: [
    { label: '模型列表', value: 'model:list' },
    { label: '注册模型', value: 'model:register' },
    { label: '模型评估', value: 'model:evaluate' },
    { label: '审批管理', value: 'model:approve' },
    { label: '部署管理', value: 'model:deploy' },
  ],
  data: [
    { label: '患者管理', value: 'data:patient' },
    { label: '数据源', value: 'data:source' },
    { label: '数据同步', value: 'data:sync' },
    { label: '质量检测', value: 'data:quality' },
  ],
  annotation: [
    { label: '标注任务', value: 'annotation:task' },
    { label: '标注工作台', value: 'annotation:workbench' },
    { label: '标注统计', value: 'annotation:stats' },
  ],
})

const selectedPermissions = ref<string[]>([
  'dashboard:overview', 'dashboard:model', 'dashboard:data',
  'model:list', 'model:register', 'model:evaluate', 'model:approve', 'model:deploy',
  'data:patient', 'data:source', 'data:sync', 'data:quality',
  'annotation:task', 'annotation:workbench', 'annotation:stats',
])

const selectedPermissionCount = computed(() => selectedPermissions.value.length)

function togglePermission(value: string, checked: boolean) {
  if (checked) {
    if (!selectedPermissions.value.includes(value)) {
      selectedPermissions.value.push(value)
    }
  } else {
    selectedPermissions.value = selectedPermissions.value.filter((p) => p !== value)
  }
}

function getRowClassName({ row }: { row: any }) {
  return row.code === 'ADMIN' ? 'admin-row' : ''
}

// Form state for modals
const roleForm = reactive({ name: '', code: '', description: '' })
const editForm = reactive({ name: '', description: '' })
let editingId = 0

watch(() => editModal.visible, (v) => {
  if (v && editModal.currentRecord.value) {
    editingId = editModal.currentRecord.value.id
    editForm.name = editModal.currentRecord.value.name
    editForm.description = editModal.currentRecord.value.description
  }
})

async function handleCreate() {
  submitting.value = true
  try {
    await createRole(roleForm)
    ElMessage.success('角色创建成功')
    roleModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleUpdate() {
  submitting.value = true
  try {
    await updateRole(editingId, editForm)
    ElMessage.success('角色更新成功')
    editModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.role-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.role-page-header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.role-page-title {
  font-size: 22px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.role-page-subtitle {
  font-size: 14px;
  color: #94a3b8;
}

.role-page-header-right {
  display: flex;
  align-items: center;
}

.code-bold {
  font-weight: 600;
}

.code-admin {
  color: #0ea5e9;
}

.text-muted {
  color: #94a3b8;
}

:deep(.admin-row) {
  background: rgba(14, 165, 233, 0.04) !important;
}

:deep(.admin-row:hover > td) {
  background: rgba(14, 165, 233, 0.08) !important;
}

/* Permission Assignment Card */
.permission-card {
  margin-top: 24px;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 20px 24px;
  background: #fff;
}

.permission-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.permission-title-text {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.permission-selected-count {
  font-size: 14px;
  color: #94a3b8;
}

.permission-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}

.permission-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.permission-group-title {
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
  margin-bottom: 12px;
}

.permission-group-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}
</style>
