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
          <a-button type="primary" @click="roleModal.open()">
            <PlusOutlined /> 新建角色
          </a-button>
        </div>
      </div>

      <!-- Role Table -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="getRowClassName"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'code'">
            <span :class="{ 'code-admin': record.code === 'ADMIN', 'code-bold': true }">
              {{ record.code }}
            </span>
          </template>
          <template v-if="column.key === 'description'">
            <span class="text-muted">{{ record.description }}</span>
          </template>
          <template v-if="column.key === 'user_count'">
            <span>{{ record.user_count }}</span>
          </template>
          <template v-if="column.key === 'built_in'">
            <a-tag color="blue">系统内置</a-tag>
          </template>
          <template v-if="column.key === 'created_at'">
            <span class="text-muted">{{ record.created_at }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="editModal.open(record)">编辑</a>
              <a @click="router.push(`/system/roles/${record.id}`)">查看</a>
            </a-space>
          </template>
        </template>
      </a-table>

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
                <a-checkbox
                  v-for="item in permissionGroups.dashboard"
                  :key="item.value"
                  :checked="selectedPermissions.includes(item.value)"
                  @change="(e: any) => togglePermission(item.value, e.target.checked)"
                >
                  {{ item.label }}
                </a-checkbox>
              </div>
            </div>
            <!-- Model management permissions -->
            <div class="permission-group">
              <div class="permission-group-title">模型管理</div>
              <div class="permission-group-items">
                <a-checkbox
                  v-for="item in permissionGroups.model"
                  :key="item.value"
                  :checked="selectedPermissions.includes(item.value)"
                  @change="(e: any) => togglePermission(item.value, e.target.checked)"
                >
                  {{ item.label }}
                </a-checkbox>
              </div>
            </div>
          </div>
          <div class="permission-column">
            <!-- Data management permissions -->
            <div class="permission-group">
              <div class="permission-group-title">数据管理</div>
              <div class="permission-group-items">
                <a-checkbox
                  v-for="item in permissionGroups.data"
                  :key="item.value"
                  :checked="selectedPermissions.includes(item.value)"
                  @change="(e: any) => togglePermission(item.value, e.target.checked)"
                >
                  {{ item.label }}
                </a-checkbox>
              </div>
            </div>
            <!-- Annotation management permissions -->
            <div class="permission-group">
              <div class="permission-group-title">标注管理</div>
              <div class="permission-group-items">
                <a-checkbox
                  v-for="item in permissionGroups.annotation"
                  :key="item.value"
                  :checked="selectedPermissions.includes(item.value)"
                  @change="(e: any) => togglePermission(item.value, e.target.checked)"
                >
                  {{ item.label }}
                </a-checkbox>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- New Role Modal -->
      <a-modal
        v-model:open="roleModal.visible"
        title="新建角色"
        @ok="handleCreate"
        :confirm-loading="submitting"
      >
        <a-form layout="vertical">
          <a-form-item label="角色名称" required>
            <a-input v-model:value="roleForm.name" />
          </a-form-item>
          <a-form-item label="角色编码" required>
            <a-input v-model:value="roleForm.code" />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea v-model:value="roleForm.description" :rows="2" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- Edit Role Modal -->
      <a-modal
        v-model:open="editModal.visible"
        title="编辑角色"
        @ok="handleUpdate"
        :confirm-loading="submitting"
      >
        <a-form layout="vertical">
          <a-form-item label="角色名称">
            <a-input v-model:value="editForm.name" />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea v-model:value="editForm.description" :rows="2" />
          </a-form-item>
        </a-form>
      </a-modal>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getRoles, createRole, updateRole } from '@/api/system'

const router = useRouter()
const roleModal = useModal()
const editModal = useModal<any>()
const submitting = ref(false)

const columns = [
  { title: '角色编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '角色名称', dataIndex: 'name', key: 'name', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '用户数', dataIndex: 'user_count', key: 'user_count', width: 80, align: 'center' as const },
  { title: '系统内置', key: 'built_in', width: 110, align: 'center' as const },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 120 },
  { title: '操作', key: 'action', width: 80, align: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getRoles({ page: params.page, page_size: params.pageSize })
)

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

function getRowClassName(record: any) {
  return record.code === 'ADMIN' ? 'admin-row' : ''
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
    message.success('角色创建成功')
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
    message.success('角色更新成功')
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
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.role-page-subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

.role-page-header-right {
  display: flex;
  align-items: center;
}

.code-bold {
  font-weight: 600;
}

.code-admin {
  color: #1677ff;
}

.text-muted {
  color: rgba(0, 0, 0, 0.45);
}

:deep(.admin-row) {
  background: rgba(22, 119, 255, 0.04) !important;
}

:deep(.admin-row:hover > td) {
  background: rgba(22, 119, 255, 0.08) !important;
}

/* Permission Assignment Card */
.permission-card {
  margin-top: 24px;
  border: 1px solid #f0f0f0;
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
  border-bottom: 1px solid #f0f0f0;
}

.permission-title-text {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.permission-selected-count {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
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
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 12px;
}

.permission-group-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.permission-group-items :deep(.ant-checkbox-wrapper) {
  margin-left: 0;
}
</style>
