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
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'
import { formatDateTime } from '@/utils/date'

const approveModal = useModal<any>()
const submitting = ref(false)
const loading = ref(false)
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

const mockData = ref<ApprovalRecord[]>([
  { id: 1, model_name: '肺结节检测模型', version_no: 'v2.3.1', approval_type: '上线审批', submitter_name: '张医生', status: 'PENDING', created_at: '2026-04-09 10:00' },
  { id: 2, model_name: '病理分类模型', version_no: 'v3.0.0', approval_type: '发布审批', submitter_name: '李工', status: 'PENDING', created_at: '2026-04-08 16:30' },
  { id: 3, model_name: '心电异常检测', version_no: 'v1.5.1', approval_type: '临床使用', submitter_name: '王医生', status: 'PENDING', created_at: '2026-04-08 09:15' },
  { id: 4, model_name: 'NLP实体识别', version_no: 'v1.0.0', approval_type: '上线审批', submitter_name: '赵工', status: 'PENDING', created_at: '2026-04-07 14:00' },
  { id: 5, model_name: '基因变异分类', version_no: 'v3.1.0', approval_type: '发布审批', submitter_name: '陈博士', status: 'PENDING', created_at: '2026-04-06 11:30' },
  { id: 6, model_name: '糖尿病预测模型', version_no: 'v2.0.0', approval_type: '上线审批', submitter_name: '张医生', status: 'APPROVED', created_at: '2026-04-05 15:20' },
  { id: 7, model_name: '骨折检测模型', version_no: 'v1.2.0', approval_type: '临床使用', submitter_name: '李工', status: 'REJECTED', created_at: '2026-04-04 10:45' },
])

const pendingCount = computed(() => mockData.value.filter(r => r.status === 'PENDING').length)

const filteredData = computed(() => {
  if (activeTab.value === 'ALL') return mockData.value
  if (activeTab.value === 'APPROVED') return mockData.value.filter(r => r.status === 'APPROVED' || r.status === 'REJECTED')
  return mockData.value.filter(r => r.status === 'PENDING')
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: computed(() => filteredData.value.length),
  showSizeChanger: false,
  showTotal: (total: number) => `共 ${total} 条`,
})

function onTabChange() {
  pagination.current = 1
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
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
    const idx = mockData.value.findIndex(r => r.id === approvingId)
    if (idx !== -1) {
      mockData.value[idx].status = approveForm.action
    }
    message.success('审批完成')
    approveModal.close()
  } finally {
    submitting.value = false
  }
}
</script>
