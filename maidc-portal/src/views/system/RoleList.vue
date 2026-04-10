<template>
  <PageContainer title="角色管理">
    <template #extra>
      <a-button type="primary" @click="roleModal.open()">
        <PlusOutlined /> 新建角色
      </a-button>
    </template>

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/system/roles/${record.id}`)">权限配置</a>
            <a @click="editModal.open(record)">编辑</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- New Role Modal -->
    <a-modal v-model:open="roleModal.visible" title="新建角色" @ok="handleCreate" :confirm-loading="submitting">
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
    <a-modal v-model:open="editModal.visible" title="编辑角色" @ok="handleUpdate" :confirm-loading="submitting">
      <a-form layout="vertical">
        <a-form-item label="角色名称"><a-input v-model:value="editForm.name" /></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="editForm.description" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
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
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '角色编码', dataIndex: 'code', key: 'code' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '用户数', dataIndex: 'user_count', key: 'user_count', width: 80 },
  { title: '操作', key: 'action', width: 180 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getRoles({ page: params.page, page_size: params.pageSize })
)

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
  } finally { submitting.value = false }
}

async function handleUpdate() {
  submitting.value = true
  try {
    await updateRole(editingId, editForm)
    message.success('角色更新成功')
    editModal.close()
    fetchData()
  } finally { submitting.value = false }
}

onMounted(() => fetchData())
</script>
