<template>
  <PageContainer title="定时任务">
    <template #extra>
      <el-button type="primary" @click="taskModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建任务
      </el-button>
    </template>

    <el-table :data="tableData" v-loading="loading" row-key="id">
      <el-table-column label="任务名称" prop="name" min-width="160" />
      <el-table-column label="类型" prop="task_type" width="140" />
      <el-table-column label="Cron" prop="cron_expression" width="130" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusDots[row.status] || '#94a3b8' }" />
            {{ row.status }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="上次执行" prop="last_execution_time" width="170" />
      <el-table-column label="下次执行" prop="next_execution_time" width="170" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleTrigger(row.id)">手动触发</el-button>
          <el-button v-if="row.status === 'RUNNING'" link type="warning" @click="handlePause(row.id)">暂停</el-button>
          <el-button v-if="row.status === 'PAUSED'" link type="primary" @click="handleResume(row.id)">恢复</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
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

    <!-- Create Task Modal -->
    <el-dialog v-model="taskModal.visible" title="新建定时任务" width="600px">
      <el-form label-position="top">
        <el-form-item label="任务名称" required>
          <el-input v-model="taskForm.name" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="taskForm.task_type" style="width: 100%">
            <el-option label="数据同步" value="DATA_SYNC" />
            <el-option label="模型重训练" value="MODEL_RETRAIN" />
            <el-option label="报告生成" value="REPORT_GENERATE" />
            <el-option label="数据清理" value="DATA_CLEANUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron 表达式" required>
          <el-input v-model="taskForm.cron_expression" placeholder="0 0 2 * * ?" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="2" />
        </el-form-item>
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
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getScheduleTasks, createScheduleTask, deleteScheduleTask, triggerTask, pauseTask, resumeTask } from '@/api/task'

const taskModal = useModal()
const submitting = ref(false)

const statusDots: Record<string, string> = {
  RUNNING: '#3b82f6',
  PAUSED: '#f59e0b',
}

const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getScheduleTasks({ page: params.page, page_size: params.pageSize })
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

const taskForm = reactive({ name: '', task_type: 'DATA_SYNC', cron_expression: '', description: '' })

async function handleCreate() {
  submitting.value = true
  try {
    await createScheduleTask(taskForm)
    ElMessage.success('任务创建成功')
    taskModal.close()
    fetchData()
  } finally { submitting.value = false }
}

async function handleTrigger(id: number) { await triggerTask(id); ElMessage.success('已触发执行'); fetchData() }
async function handlePause(id: number) { await pauseTask(id); ElMessage.success('已暂停'); fetchData() }
async function handleResume(id: number) { await resumeTask(id); ElMessage.success('已恢复'); fetchData() }
async function handleDelete(id: number) { await deleteScheduleTask(id); ElMessage.success('已删除'); fetchData() }

onMounted(() => fetchData())
</script>
