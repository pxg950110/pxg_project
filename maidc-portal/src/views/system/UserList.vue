<template>
  <PageContainer title="用户管理">
    <template #extra>
      <a-button type="primary" @click="userModal.open()">
        <PlusOutlined /> 新建用户
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '禁用'" />
        </template>
        <template v-if="column.key === 'roles'">
          <a-tag v-for="role in record.roles" :key="role" color="blue">{{ role }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="editModal.open(record)">编辑</a>
            <a @click="resetPwdModal.open(record)">重置密码</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- New/Edit User Modal -->
    <a-modal v-model:open="userModal.visible" title="新建用户" @ok="handleCreateUser" :confirm-loading="submitting" width="600px">
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
            <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">{{ role.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Edit User Modal -->
    <a-modal v-model:open="editModal.visible" title="编辑用户" @ok="handleUpdateUser" :confirm-loading="submitting" width="600px">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="姓名"><a-input v-model:value="editForm.real_name" /></a-form-item>
        <a-form-item label="邮箱"><a-input v-model:value="editForm.email" /></a-form-item>
        <a-form-item label="手机号"><a-input v-model:value="editForm.phone" /></a-form-item>
        <a-form-item label="状态">
          <a-switch :checked="editForm.status === 1" @change="(v: boolean) => editForm.status = v ? 1 : 0" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Reset Password Modal -->
    <a-modal v-model:open="resetPwdModal.visible" title="重置密码" @ok="handleResetPwd" :confirm-loading="submitting">
      <a-form layout="vertical">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="pwdForm.new_password" placeholder="请输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getUsers, createUser, updateUser, resetPassword, getRoles } from '@/api/system'

const userModal = useModal()
const editModal = useModal<any>()
const resetPwdModal = useModal<any>()
const submitting = ref(false)
const roleOptions = ref<any[]>([])

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '用户名/姓名' },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '正常', value: '1' }, { label: '禁用', value: '0' },
  ]},
]

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '姓名', dataIndex: 'real_name', key: 'real_name', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 180 },
  { title: '角色', dataIndex: 'roles', key: 'roles' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '最后登录', dataIndex: 'last_login', key: 'last_login', width: 170 },
  { title: '操作', key: 'action', width: 150 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getUsers({ page: params.page, page_size: params.pageSize })
)

const userForm = reactive({ username: '', real_name: '', email: '', phone: '', role_ids: [] as number[] })
const editForm = reactive({ real_name: '', email: '', phone: '', status: 1 })
let editingUserId = 0
const pwdForm = reactive({ new_password: '' })

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

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

watch(() => resetPwdModal.visible, (v) => {
  if (v) pwdForm.new_password = ''
})

async function handleCreateUser() {
  submitting.value = true
  try {
    await createUser(userForm)
    message.success('用户创建成功')
    userModal.close()
    fetchData()
  } finally { submitting.value = false }
}

async function handleUpdateUser() {
  submitting.value = true
  try {
    await updateUser(editingUserId, editForm)
    message.success('用户更新成功')
    editModal.close()
    fetchData()
  } finally { submitting.value = false }
}

async function handleResetPwd() {
  submitting.value = true
  try {
    await resetPassword(resetPwdModal.currentRecord.value!.id, pwdForm)
    message.success('密码重置成功')
    resetPwdModal.close()
  } finally { submitting.value = false }
}

async function loadRoles() {
  const res = await getRoles({ page: 1, page_size: 100 })
  roleOptions.value = res.data.data.items
}

onMounted(() => { fetchData(); loadRoles() })
</script>
