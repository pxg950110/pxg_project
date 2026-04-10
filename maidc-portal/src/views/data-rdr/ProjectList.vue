<template>
  <PageContainer title="研究项目">
    <template #extra>
      <a-button type="primary" @click="projectModal.open()">
        <PlusOutlined /> 新建项目
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'ACTIVE' ? 'green' : 'default'">{{ record.status }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/data/rdr/projects/${record.id}`)">详情</a>
            <a @click="openInvite(record)">邀请成员</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="projectModal.visible" title="新建研究项目" @ok="handleCreate" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="项目名称" required><a-input v-model:value="projectForm.name" /></a-form-item>
        <a-form-item label="研究类型"><a-select v-model:value="projectForm.research_type">
          <a-select-option value="CLINICAL">临床研究</a-select-option>
          <a-select-option value="EPIDEMIOLOGICAL">流行病学研究</a-select-option>
          <a-select-option value="BASIC">基础研究</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="projectForm.description" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="inviteVisible" title="邀请成员" @ok="handleInvite" :confirm-loading="inviting">
      <UserSelect v-model:value="inviteUserId" placeholder="选择用户" />
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import UserSelect from '@/components/UserSelect/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getProjects, createProject } from '@/api/data'
import request from '@/utils/request'

const router = useRouter()
const projectModal = useModal()
const submitting = ref(false)
const inviteVisible = ref(false)
const inviting = ref(false)
const inviteUserId = ref<string>()
let invitingProjectId = 0

const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '项目名称' },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '进行中', value: 'ACTIVE' }, { label: '已完成', value: 'COMPLETED' },
  ]},
]

const columns = [
  { title: '项目名称', dataIndex: 'name', key: 'name' },
  { title: '研究类型', dataIndex: 'research_type', key: 'research_type', width: 120 },
  { title: '负责人', dataIndex: 'pi_name', key: 'pi_name', width: 100 },
  { title: '成员数', dataIndex: 'member_count', key: 'member_count', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 150 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getProjects({ page: params.page, page_size: params.pageSize })
)

const projectForm = reactive({ name: '', research_type: 'CLINICAL', description: '' })

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

async function handleCreate() {
  submitting.value = true
  try {
    await createProject(projectForm)
    message.success('项目创建成功')
    projectModal.close()
    fetchData()
  } finally { submitting.value = false }
}

function openInvite(record: any) { invitingProjectId = record.id; inviteVisible.value = true }

async function handleInvite() {
  inviting.value = true
  try {
    await request.post(`/rdr/projects/${invitingProjectId}/members`, { user_id: inviteUserId.value })
    message.success('邀请成功')
    inviteVisible.value = false
    fetchData()
  } finally { inviting.value = false }
}

onMounted(() => fetchData())
</script>
