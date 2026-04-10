<template>
  <PageContainer title="审批管理">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="approval" />
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewDetail(record)">查看</a>
            <a v-if="record.status === 'PENDING' && record.current_step === myStep" type="primary" @click="openApproveModal(record)">审批</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Approve Modal -->
    <a-modal v-model:open="approveModal.visible" title="审批操作" @ok="handleApprove" :confirm-loading="submitting">
      <a-form layout="vertical">
        <a-form-item label="审批结果">
          <a-radio-group v-model:value="approveForm.action">
            <a-radio value="APPROVED">通过</a-radio>
            <a-radio value="REJECTED">驳回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见" required>
          <a-textarea v-model:value="approveForm.comment" :rows="3" placeholder="请输入审批意见" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const approveModal = useModal<any>()
const submitting = ref(false)
const myStep = ref('TECHNICAL')

const searchFields = [
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '待审批', value: 'PENDING' }, { label: '已通过', value: 'APPROVED' }, { label: '已驳回', value: 'REJECTED' },
  ]},
]

const columns = [
  { title: '审批ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模型名称', dataIndex: 'model_name', key: 'model_name' },
  { title: '版本', dataIndex: 'version_no', key: 'version_no', width: 100 },
  { title: '审批类型', dataIndex: 'approval_type', key: 'approval_type', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '当前步骤', dataIndex: 'current_step', key: 'current_step', width: 100 },
  { title: '提交人', dataIndex: 'submitter_name', key: 'submitter_name', width: 100 },
  { title: '提交时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 140 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/approvals', { params: { page: params.page, page_size: params.pageSize } })
)

const approveForm = reactive({ action: 'APPROVED', comment: '' })
let approvingId = 0

function handleSearch() { fetchData() }
function handleReset() { fetchData() }

function viewDetail(record: any) {
  message.info('查看审批详情 #' + record.id)
}

function openApproveModal(record: any) {
  approvingId = record.id
  approveForm.action = 'APPROVED'
  approveForm.comment = ''
  approveModal.open(record)
}

async function handleApprove() {
  submitting.value = true
  try {
    await request.put(`/approvals/${approvingId}/review`, approveForm)
    message.success('审批完成')
    approveModal.close()
    fetchData()
  } finally { submitting.value = false }
}

onMounted(() => fetchData())
</script>
