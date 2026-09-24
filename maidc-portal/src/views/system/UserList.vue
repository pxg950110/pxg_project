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
          <el-button :icon="Search" disabled />
          <el-button type="primary" @click="openCreateModal">
            <el-icon class="mr-1"><Plus /></el-icon> 新建用户
          </el-button>
        </div>
      </div>

      <!-- Filter Bar -->
      <div class="filter-bar">
        <el-select
          v-model="filters.status"
          clearable
          placeholder="状态"
          class="filter-item"
        >
          <el-option value="启用" label="启用" />
          <el-option value="禁用" label="禁用" />
        </el-select>

        <el-select
          v-model="filters.role"
          clearable
          placeholder="角色"
          class="filter-item"
        >
          <el-option value="管理员" label="管理员" />
          <el-option value="AI工程师" label="AI工程师" />
          <el-option value="研究员" label="研究员" />
          <el-option value="数据管理员" label="数据管理员" />
          <el-option value="临床医生" label="临床医生" />
        </el-select>

        <el-select
          v-model="filters.org"
          clearable
          placeholder="组织"
          class="filter-item"
        >
          <el-option value="放射科" label="放射科" />
          <el-option value="心内科" label="心内科" />
          <el-option value="呼吸内科" label="呼吸内科" />
        </el-select>

        <el-input
          v-model="filters.keyword"
          placeholder="搜索用户名/姓名/邮箱..."
          class="filter-search"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- Table -->
      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="id"
      >
        <el-table-column label="#" width="60">
          <template #default="{ $index }">
            {{ (pagination.current - 1) * pagination.pageSize + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="用户名" prop="username" width="120" />
        <el-table-column label="姓名" prop="realName" width="120" />
        <el-table-column label="邮箱" prop="email" />
        <el-table-column label="角色" prop="role" width="120">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="80">
          <template #default="{ row }">
            <span class="status-badge">
              <span
                class="status-dot"
                :class="row.status === '启用' ? 'status-dot-success' : 'status-dot-error'"
              ></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <div class="action-links">
              <a @click="editModal.open(row)">编辑</a>
              <a @click="handleView(row)">查看</a>
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
    <!-- Create User Modal -->
  <el-dialog
    v-model="userModal.visible"
    title="新建用户"
    width="600px"
  >
    <el-form :model="userForm" label-position="top">
      <el-form-item label="用户名" required>
        <el-input v-model="userForm.username" />
      </el-form-item>
      <el-form-item label="姓名" required>
        <el-input v-model="userForm.real_name" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="userForm.email" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="userForm.phone" />
      </el-form-item>
      <el-form-item label="角色" required>
        <el-select v-model="userForm.role_ids" multiple placeholder="请选择角色">
          <el-option v-for="role in roleOptions" :key="role.id" :value="role.id" :label="role.name" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="userModal.close()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleCreateUser">确定</el-button>
    </template>
  </el-dialog>

  <!-- Edit User Modal -->
  <el-dialog
    v-model="editModal.visible"
    title="编辑用户"
    width="600px"
  >
    <el-form :model="editForm" label-position="top">
      <el-form-item label="姓名">
        <el-input v-model="editForm.real_name" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="editForm.email" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="editForm.phone" />
      </el-form-item>
      <el-form-item label="状态">
        <el-switch
          :model-value="editForm.status === '启用'"
          @change="(v: string | number | boolean) => editForm.status = v ? '启用' : '禁用'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editModal.close()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleUpdateUser">确定</el-button>
    </template>
  </el-dialog>

  <!-- Reset Password Modal -->
  <el-dialog
    v-model="resetPwdModal.visible"
    title="重置密码"
  >
    <el-form label-position="top">
      <el-form-item label="新密码" required>
        <el-input v-model="pwdForm.new_password" type="password" show-password placeholder="请输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="resetPwdModal.close()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleResetPwd">确定</el-button>
    </template>
  </el-dialog>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getUsers({
    page: params.page,
    page_size: params.pageSize,
    keyword: filters.keyword || undefined,
    status: filters.status || undefined,
  })
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
    ElMessage.success('用户创建成功')
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
    ElMessage.success('用户更新成功')
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
    ElMessage.success('密码重置成功')
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
    roleOptions.value = res.data.data.items ?? []
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
  color: #0f172a;
  margin: 0;
}

.page-header-subtitle {
  font-size: 14px;
  color: #94a3b8;
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

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot-success {
  background: #10b981;
}

.status-dot-error {
  background: #ef4444;
}
</style>
