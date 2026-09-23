<template>
  <PageContainer title="ETL 任务">
    <template #extra>
      <el-button type="primary" @click="taskModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon> 新建任务
      </el-button>
    </template>

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="任务名称" prop="name" />
      <el-table-column label="源" prop="source_type" width="100" />
      <el-table-column label="目标" prop="target_type" width="80" />
      <el-table-column label="调度周期" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.cron_expression" type="info" style="font-family: monospace; font-size: 12px;">{{ row.cron_expression }}</el-tag>
          <span v-else class="text-slate-400">手动执行</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusMap[row.status] || '#94a3b8' }" />
            {{ row.status }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="处理记录数" prop="records_processed" width="100" />
      <el-table-column label="最后执行" prop="last_execution_time" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" @click="viewLog(row)">日志</el-button>
            <el-button v-if="row.status === 'FAILED'" link type="primary" @click="handleRetry(row.id)">重试</el-button>
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

    <el-dialog v-model="taskModal.visible" title="新建 ETL 任务" width="600px">
      <el-form label-position="top">
        <el-form-item label="任务名称" required><el-input v-model="taskForm.name" /></el-form-item>
        <el-form-item label="源数据"><el-select v-model="taskForm.source_type">
          <el-option value="HIS" label="HIS系统" />
          <el-option value="PACS" label="PACS系统" />
          <el-option value="LIS" label="LIS系统" />
        </el-select></el-form-item>
        <el-form-item label="目标"><el-select v-model="taskForm.target_type">
          <el-option value="CDR" label="CDR" />
          <el-option value="RDR" label="RDR" />
        </el-select></el-form-item>
        <el-form-item label="Cron 表达式"><el-input v-model="taskForm.cron_expression" placeholder="留空为手动执行" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getEtlTasks, createEtlTask } from '@/api/data'

defineOptions({ name: 'EtlTaskList' })

const taskModal = useModal()
const submitting = ref(false)

const statusMap: Record<string, string> = {
  PENDING: '#94a3b8', RUNNING: '#0ea5e9', COMPLETED: '#10b981', FAILED: '#ef4444', PAUSED: '#f59e0b',
}

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getEtlTasks({ page: params.page, page_size: params.pageSize })
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

const taskForm = reactive({ name: '', source_type: 'HIS', target_type: 'CDR', cron_expression: '' })

async function handleCreate() {
  submitting.value = true
  try {
    await createEtlTask(taskForm)
    ElMessage.success('ETL任务创建成功')
    taskModal.close()
    fetchData()
  } finally { submitting.value = false }
}

function viewLog(record: any) { ElMessage.info('查看日志: ' + record.name) }
function handleRetry(id: number) { ElMessage.info('重试任务 #' + id) }

onMounted(() => fetchData())
</script>
