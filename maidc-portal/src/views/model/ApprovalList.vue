<template>
  <PageContainer title="审批管理">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="PENDING">
        <template #tab>
          <a-badge :count="pendingCount" :offset="[6, 0]" size="small">
            待审批
          </a-badge>
        </template>
      </a-tab-pane>
      <a-tab-pane key="APPROVED" tab="已审批" />
      <a-tab-pane key="ALL" tab="全部" />
    </a-tabs>

    <a-table :columns="columns" :data-source="filteredData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="approval" />
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a v-if="record.status === 'PENDING'" type="primary" @click="openApproveModal(record)">审批</a>
          <span v-else class="text-gray-400">--</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'
import { useTable } from '@/hooks/useTable'
import { getApprovals, reviewApproval } from '@/api/model'
import { formatDateTime } from '@/utils/date'

const approveModal = useModal<any>()
const submitting = ref(false)
const activeTab = ref('PENDING')

interface ApprovalRecord {
  id: number
  model_name: string
  version_no: string
  approval_type: string
  submitter_name: string
  status: string
  created_at: string
}

// API data via useTable
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<ApprovalRecord>(
  (params) => getApprovals({ page: params.page, page_size: params.pageSize, status: activeTab.value === 'ALL' ? undefined : activeTab.value })
)

const pendingCount = computed(() => tableData.value.filter((r: ApprovalRecord) => r.status === 'PENDING').length)

const filteredData = computed(() => {
  if (activeTab.value === 'ALL') return tableData.value
  if (activeTab.value === 'APPROVED') return tableData.value.filter((r: ApprovalRecord) => r.status === 'APPROVED' || r.status === 'REJECTED')
  return tableData.value.filter((r: ApprovalRecord) => r.status === 'PENDING')
})

function onTabChange() {
  fetchData({ page: 1 })
}

const columns = [
  { title: '模型名称', dataIndex: 'model_name', key: 'model_name' },
  { title: '版本', dataIndex: 'version_no', key: 'version_no', width: 100 },
  { title: '审批类型', dataIndex: 'approval_type', key: 'approval_type', width: 120 },
  { title: '申请人', dataIndex: 'submitter_name', key: 'submitter_name', width: 100 },
  { title: '提交时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 100 },
]

const approveForm = reactive({ action: 'APPROVED', comment: '' })
let approvingId = 0

function openApproveModal(record: any) {
  approvingId = record.id
  approveForm.action = 'APPROVED'
  approveForm.comment = ''
  approveModal.open(record)
}

async function handleApprove() {
  submitting.value = true
  try {
    await reviewApproval(approvingId, {
      status: approveForm.action,
      comment: approveForm.comment,
    })
    message.success('审批完成')
    approveModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => fetchData())
</script>
