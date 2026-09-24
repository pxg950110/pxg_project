<template>
  <PageContainer title="审批管理">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane name="PENDING">
        <template #label>
          <span class="inline-flex items-center gap-1.5">
            待审批
            <span
              v-if="pendingCount > 0"
              class="inline-flex items-center justify-center h-[18px] min-w-[18px] px-1 rounded-full bg-red-500 text-white text-xs leading-none"
            >{{ pendingCount }}</span>
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="已审批" name="APPROVED" />
      <el-tab-pane label="全部" name="ALL" />
    </el-tabs>

    <el-table :data="filteredData" v-loading="loading" row-key="id">
      <el-table-column label="模型名称" prop="model_name" />
      <el-table-column label="版本" prop="version_no" width="100" />
      <el-table-column label="审批类型" prop="approval_type" width="120" />
      <el-table-column label="申请人" prop="submitter_name" width="100" />
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status" type="approval" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openApproveModal(row)">审批</el-button>
          <span v-else class="text-slate-400">--</span>
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

    <!-- Approve Modal -->
    <el-dialog v-model="approveModal.visible" title="审批操作" width="520px">
      <el-form label-width="100px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.action">
            <el-radio value="APPROVED">通过</el-radio>
            <el-radio value="REJECTED">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" required>
          <el-input v-model="approveForm.comment" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleApprove">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
const { tableData, loading, pagination, fetchData } = useTable<ApprovalRecord>(
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

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

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
    ElMessage.success('审批完成')
    approveModal.close()
    fetchData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => fetchData())
</script>
