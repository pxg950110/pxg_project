<template>
  <PageContainer>
    <!-- Custom header (NOT inside extra slot) -->
    <template #default>
      <!-- Page Header -->
      <div class="page-header-custom">
        <div class="page-header-left">
          <h2 class="page-header-title">用户管理</h2>
          <span class="page-header-subtitle">管理系统用户账号、角色分配与权限控制</span>
        </div>
        <div class="page-header-right">
          <a-button :icon="h(SearchOutlined)" disabled />
          <a-button type="primary" @click="openCreateModal">
            <PlusOutlined /> 新建用户
          </a-button>
        </div>
      </div>

      <!-- Filter Bar -->
      <div class="filter-bar">
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="状态"
          class="filter-item"
        >
          <a-select-option value="启用">启用</a-select-option>
          <a-select-option value="禁用">禁用</a-select-option>
        </a-select>

        <a-select
          v-model:value="filters.role"
          allow-clear
          placeholder="角色"
          class="filter-item"
        >
          <a-select-option value="管理员">管理员</a-select-option>
          <a-select-option value="AI工程师">AI工程师</a-select-option>
          <a-select-option value="研究员">研究员</a-select-option>
          <a-select-option value="数据管理员">数据管理员</a-select-option>
          <a-select-option value="临床医生">临床医生</a-select-option>
        </a-select>

        <a-select
          v-model:value="filters.org"
          allow-clear
          placeholder="组织"
          class="filter-item"
        >
          <a-select-option value="放射科">放射科</a-select-option>
          <a-select-option value="心内科">心内科</a-select-option>
          <a-select-option value="呼吸内科">呼吸内科</a-select-option>
        </a-select>

        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索用户名/姓名/邮箱..."
          class="filter-search"
          allow-clear
        />
      </div>

      <!-- Table -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'index'">
            {{ (pagination.current - 1) * pagination.pageSize + index + 1 }}
          </template>
          <template v-if="column.key === 'role'">
            <a-tag color="blue">{{ record.role }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === '启用' ? 'success' : 'error'"
              :text="record.status"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="action-links">
              <a @click="editModal.open(record)">编辑</a>
              <a @click="handleView(record)">查看</a>
            </div>
          </template>
        </template>
      </a-table>
    </template>
  </PageContainer>

  <!-- Create User Modal -->
  <a-modal
    v-model:open="userModal.visible"
    title="新建用户"
    @ok="handleCreateUser"
    :confirm-loading="submitting"
    width="600px"
  >
    <a-form :model="userForm" layout="vertical">
      <a-form-item label="用户名" required>
        <a-input v-model:value="userForm.username" />
      </a-form-item>
      <a-form-item label="姓名" required>
        <a-input v-model:value="userForm.real_name" />
      </a-form-item>
      <a-form-item label="邮箱">
        <a-input v-model:value="userForm.email" />
      </a-form-item>
      <a-form-item label="手机号">
        <a-input v-model:value="userForm.phone" />
      </a-form-item>
      <a-form-item label="角色" required>
        <a-select v-model:value="userForm.role_ids" mode="multiple" placeholder="请选择角色">
          <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
            {{ role.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- Edit User Modal -->
  <a-modal
    v-model:open="editModal.visible"
    title="编辑用户"
    @ok="handleUpdateUser"
    :confirm-loading="submitting"
    width="600px"
  >
    <a-form :model="editForm" layout="vertical">
      <a-form-item label="姓名">
        <a-input v-model:value="editForm.real_name" />
      </a-form-item>
      <a-form-item label="邮箱">
        <a-input v-model:value="editForm.email" />
      </a-form-item>
      <a-form-item label="手机号">
        <a-input v-model:value="editForm.phone" />
      </a-form-item>
      <a-form-item label="状态">
        <a-switch
          :checked="editForm.status === '启用'"
          @change="(v: boolean) => editForm.status = v ? '启用' : '禁用'"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- Reset Password Modal -->
  <a-modal
    v-model:open="resetPwdModal.visible"
    title="重置密码"
    @ok="handleResetPwd"
    :confirm-loading="submitting"
  >
    <a-form layout="vertical">
      <a-form-item label="新密码" required>
        <a-input-password v-model:value="pwdForm.new_password" placeholder="请输入新密码" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getUsers, createUser, updateUser, resetPassword, getRoles } from '@/api/system'

const router = useRouter()

// Modals
const userModal = useModal()
const editModal = useModal<any>()
const resetPwdModal = useModal<any>()
const submitting = ref(false)
const roleOptions = ref<any[]>([])

// Filters
const filters = reactive({
  status: undefined as string | undefined,
  role: undefined as string | undefined,
  org: undefined as string | undefined,
  keyword: '' as string,
})

// Table hook with API
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getUsers({
    page: params.page,
    page_size: params.pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
  })
)

// Table columns
const columns = [
  { title: '#', key: 'index', width: 60 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '角色', dataIndex: 'role', key: 'role', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 100, align: 'right' as const },
]

// Forms
const userForm = reactive({
  username: '',
  real_name: '',
  email: '',
  phone: '',
  role_ids: [] as number[],
})

const editForm = reactive({
  real_name: '',
  email: '',
  phone: '',
  status: '启用' as string,
})

let editingUserId = 0

const pwdForm = reactive({ new_password: '' })

// Open create modal and reset form
function openCreateModal() {
  userForm.username = ''
  userForm.real_name = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.role_ids = []
  userModal.open()
}

// View user detail
function handleView(record: any) {
  router.push(`/system/users/${record.id}`)
}

// Watch edit modal to populate form
watch(() => editModal.visible, (v) => {
  if (v && editModal.currentRecord.value) {
    const r = editModal.currentRecord.value
    editingUserId = r.id
    editForm.real_name = r.real_name
    editForm.email = r.email || ''
    editForm.phone = r.phone || ''
    editForm.status = r.status
  }
})

// Watch reset password modal to clear form
watch(() => resetPwdModal.visible, (v) => {
  if (v) pwdForm.new_password = ''
})

// Handlers
async function handleCreateUser() {
  submitting.value = true
  try {
    await createUser(userForm)
    message.success('用户创建成功')
    userModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleUpdateUser() {
  submitting.value = true
  try {
    await updateUser(editingUserId, editForm)
    message.success('用户更新成功')
    editModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleResetPwd() {
  submitting.value = true
  try {
    await resetPassword(resetPwdModal.currentRecord.value!.id, pwdForm)
    message.success('密码重置成功')
    resetPwdModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

// Load roles
async function loadRoles() {
  try {
    const res = await getRoles({ page: 1, page_size: 100 })
    roleOptions.value = res.data.data.items
  } catch {
    roleOptions.value = []
  }
}

onMounted(() => {
  fetchData()
  loadRoles()
})

// Watch filters to reload from API
watch(filters, () => {
  fetchData({ page: 1 })
})
</script>

<style scoped>
.page-header-custom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-header-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.page-header-subtitle {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-item {
  min-width: 120px;
  font-size: 13px;
}

.filter-search {
  flex: 1;
  font-size: 13px;
}

.action-links {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.action-links a {
  font-size: 13px;
}
</style>
