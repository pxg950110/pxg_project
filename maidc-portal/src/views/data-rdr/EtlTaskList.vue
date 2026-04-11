<template>
  <PageContainer title="ETL 任务">
    <template #extra>
      <a-button type="primary" @click="taskModal.open()">
        <PlusOutlined /> 新建任务
      </a-button>
    </template>

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cron_expression'">
          <a-tag v-if="record.cron_expression" color="default" style="font-family: monospace; font-size: 12px;">{{ record.cron_expression }}</a-tag>
          <span v-else style="color: #999;">手动执行</span>
        </template>
        <template v-if="column.key === 'status'">
          <a-badge :status="statusMap[record.status] || 'default'" :text="record.status" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewLog(record)">日志</a>
            <a v-if="record.status === 'FAILED'" @click="handleRetry(record.id)">重试</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="taskModal.visible" title="新建 ETL 任务" @ok="handleCreate" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="任务名称" required><a-input v-model:value="taskForm.name" /></a-form-item>
        <a-form-item label="源数据"><a-select v-model:value="taskForm.source_type">
          <a-select-option value="HIS">HIS系统</a-select-option>
          <a-select-option value="PACS">PACS系统</a-select-option>
          <a-select-option value="LIS">LIS系统</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="目标"><a-select v-model:value="taskForm.target_type">
          <a-select-option value="CDR">CDR</a-select-option>
          <a-select-option value="RDR">RDR</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="Cron 表达式"><a-input v-model:value="taskForm.cron_expression" placeholder="留空为手动执行" /></a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getEtlTasks, createEtlTask } from '@/api/data'

const taskModal = useModal()
const submitting = ref(false)

const statusMap: Record<string, string> = {
  PENDING: 'default', RUNNING: 'processing', COMPLETED: 'success', FAILED: 'error', PAUSED: 'warning',
}

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name' },
  { title: '源', dataIndex: 'source_type', key: 'source_type', width: 100 },
  { title: '目标', dataIndex: 'target_type', key: 'target_type', width: 80 },
  { title: '调度周期', dataIndex: 'cron_expression', key: 'cron_expression', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '处理记录数', dataIndex: 'records_processed', key: 'records_processed', width: 100 },
  { title: '最后执行', dataIndex: 'last_execution_time', key: 'last_execution_time', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getEtlTasks({ page: params.page, page_size: params.pageSize })
)

const taskForm = reactive({ name: '', source_type: 'HIS', target_type: 'CDR', cron_expression: '' })

async function handleCreate() {
  submitting.value = true
  try {
    await createEtlTask(taskForm)
    message.success('ETL任务创建成功')
    taskModal.close()
    fetchData()
  } finally { submitting.value = false }
}

function viewLog(record: any) { message.info('查看日志: ' + record.name) }
function handleRetry(id: number) { message.info('重试任务 #' + id) }

onMounted(() => fetchData())
</script>
