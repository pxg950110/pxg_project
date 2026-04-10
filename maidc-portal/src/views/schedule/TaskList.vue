<template>
  <PageContainer title="定时任务">
    <template #extra>
      <a-button type="primary" @click="taskModal.open()">
        <PlusOutlined /> 新建任务
      </a-button>
    </template>

    <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-badge :status="record.status === 'RUNNING' ? 'processing' : record.status === 'PAUSED' ? 'warning' : 'default'" :text="record.status" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="handleTrigger(record.id)">手动触发</a>
            <a v-if="record.status === 'RUNNING'" @click="handlePause(record.id)">暂停</a>
            <a v-if="record.status === 'PAUSED'" @click="handleResume(record.id)">恢复</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Create Task Modal -->
    <a-modal v-model:open="taskModal.visible" title="新建定时任务" @ok="handleCreate" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="任务名称" required>
          <a-input v-model:value="taskForm.name" />
        </a-form-item>
        <a-form-item label="任务类型">
          <a-select v-model:value="taskForm.task_type">
            <a-select-option value="DATA_SYNC">数据同步</a-select-option>
            <a-select-option value="MODEL_RETRAIN">模型重训练</a-select-option>
            <a-select-option value="REPORT_GENERATE">报告生成</a-select-option>
            <a-select-option value="DATA_CLEANUP">数据清理</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Cron 表达式" required>
          <a-input v-model:value="taskForm.cron_expression" placeholder="0 0 2 * * ?" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="taskForm.description" :rows="2" />
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
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getScheduleTasks, createScheduleTask, deleteScheduleTask, triggerTask, pauseTask, resumeTask } from '@/api/task'

const taskModal = useModal()
const submitting = ref(false)

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'task_type', key: 'task_type', width: 120 },
  { title: 'Cron', dataIndex: 'cron_expression', key: 'cron_expression', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '上次执行', dataIndex: 'last_execution_time', key: 'last_execution_time', width: 170 },
  { title: '下次执行', dataIndex: 'next_execution_time', key: 'next_execution_time', width: 170 },
  { title: '操作', key: 'action', width: 200 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getScheduleTasks({ page: params.page, page_size: params.pageSize })
)

const taskForm = reactive({ name: '', task_type: 'DATA_SYNC', cron_expression: '', description: '' })

async function handleCreate() {
  submitting.value = true
  try {
    await createScheduleTask(taskForm)
    message.success('任务创建成功')
    taskModal.close()
    fetchData()
  } finally { submitting.value = false }
}

async function handleTrigger(id: number) { await triggerTask(id); message.success('已触发执行'); fetchData() }
async function handlePause(id: number) { await pauseTask(id); message.success('已暂停'); fetchData() }
async function handleResume(id: number) { await resumeTask(id); message.success('已恢复'); fetchData() }
async function handleDelete(id: number) { await deleteScheduleTask(id); message.success('已删除'); fetchData() }

onMounted(() => fetchData())
</script>

<style scoped>
.danger-link { color: #ff4d4f; }
</style>
