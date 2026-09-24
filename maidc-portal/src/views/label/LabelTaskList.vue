<template>
  <PageContainer title="标注任务">

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          v-model="filters.task_type"
          placeholder="类型"
          clearable
          style="width: 130px"
          @change="applyFilters"
        >
          <el-option label="全部" value="" />
          <el-option label="影像标注" value="IMAGE" />
          <el-option label="文本标注" value="TEXT" />
        </el-select>

        <el-select
          v-model="filters.format"
          placeholder="格式"
          clearable
          style="width: 130px"
          @change="applyFilters"
        >
          <el-option label="全部" value="" />
          <el-option label="矩形框" value="矩形框标注" />
          <el-option label="多边形" value="多边形标注" />
          <el-option label="NER" value="NER标注" />
        </el-select>

        <el-select
          v-model="filters.status"
          placeholder="状态"
          clearable
          style="width: 130px"
          @change="applyFilters"
        >
          <el-option label="全部" value="" />
          <el-option label="待标注" value="PENDING" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已暂停" value="PAUSED" />
        </el-select>

        <el-input
          v-model="filters.keyword"
          placeholder="搜索任务名称..."
          style="width: 220px"
          clearable
          @keyup.enter="applyFilters"
        />
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="taskModal.open()">
          <el-icon class="mr-1"><Plus /></el-icon> 新建任务
        </el-button>
      </div>
    </div>

    <!-- Metric Cards Row -->
    <el-row :gutter="16" class="metric-row">
      <el-col :span="6">
        <MetricCard
          title="标注任务"
          :value="summary.totalTasks"
          suffix="个"
          :icon="Document"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          title="进行中"
          :value="summary.inProgress"
          suffix="个"
          :trend="{ value: 3, type: 'up' }"
          :icon="VideoPlay"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          title="已标注数据"
          :value="summary.labeledData"
          suffix="条"
          :trend="{ value: 12, type: 'up' }"
          :icon="CircleCheck"
        />
      </el-col>
      <el-col :span="6">
        <MetricCard
          title="平均一致性"
          :value="summary.avgConsistency"
          :trend="{ value: 3, type: 'up' }"
          :icon="CircleCheckFilled"
        />
      </el-col>
    </el-row>

    <!-- Card Grid -->
    <el-row :gutter="16" class="task-card-grid">
      <el-col v-for="task in tableData" :key="task.id" :span="8">
        <div class="task-card" @click="router.push(`/label/detail/${task.id}`)">
          <div class="task-card-header">
            <span class="task-name">{{ task.name }}</span>
            <el-tag :type="typeColorMap[task.task_type] || 'primary'" class="type-tag">
              {{ task.task_type === 'IMAGE' ? '影像标注' : '文本标注' }}
            </el-tag>
          </div>

          <div class="task-card-format">{{ task.format }}</div>
          <div class="task-card-dataset">{{ task.dataset_name }}</div>

          <div class="task-card-progress">
            <el-progress
              :percentage="task.progress"
              :color="task.progress === 100 ? '#10b981' : '#0ea5e9'"
              :stroke-width="6"
            />
            <span class="progress-text">{{ task.completed }}/{{ task.total }}</span>
          </div>

          <div class="task-card-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>{{ task.assignees.length > 0 ? task.assignees.join(', ') : '未分配' }}</span>
            </div>
            <div v-if="task.deadline" class="meta-item">
              <el-icon><Calendar /></el-icon>
              <span>{{ task.deadline }}</span>
            </div>
          </div>

          <div class="task-card-footer">
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusMap[task.status]?.color || '#94a3b8' }" />
              {{ statusMap[task.status]?.label }}
            </span>
            <a class="view-detail-link" @click.stop="router.push(`/label/detail/${task.id}`)">
              查看详情 &rarr;
            </a>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- Pagination -->
    <div class="pagination-bar">
      <span class="pagination-total">共 {{ pagination.total }} 个任务</span>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="pagination.total"
        :current-page="pagination.current"
        :page-size="pagination.pageSize"
        :page-sizes="[6, 12, 18]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Create Task Modal (two-column) -->
    <el-dialog
      v-model="taskModal.visible"
      title="新建标注任务"
      width="720px"
    >
      <el-form label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" required>
              <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
            </el-form-item>
            <el-form-item label="关联数据集" required>
              <DatasetSelect v-model="taskForm.dataset_id" />
            </el-form-item>
            <el-form-item label="标签列表">
              <el-select
                v-model="taskForm.labels"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="输入标签后回车添加"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="分配审核员">
              <UserSelect v-model="taskForm.reviewer_id" placeholder="选择审核员" />
            </el-form-item>
            <el-form-item label="AI 预标注">
              <el-switch v-model="taskForm.ai_preannotate" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标注类型" required>
              <el-select v-model="taskForm.task_type" placeholder="请选择标注类型">
                <el-option label="影像标注" value="IMAGE" />
                <el-option label="文本标注" value="TEXT" />
              </el-select>
            </el-form-item>
            <el-form-item label="标注格式" required>
              <el-select v-model="taskForm.format" placeholder="请选择标注格式">
                <el-option label="矩形框" value="矩形框标注" />
                <el-option label="多边形" value="多边形标注" />
                <el-option label="椭圆" value="椭圆标注" />
                <el-option label="自由绘制" value="自由绘制" />
                <el-option label="NER" value="NER标注" />
              </el-select>
            </el-form-item>
            <el-form-item label="分配标注员">
              <UserSelect v-model="taskForm.assignee_ids" multiple placeholder="选择标注员" />
            </el-form-item>
            <el-form-item label="截止日期">
              <el-date-picker
                v-model="taskForm.deadline"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择截止日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input
            v-model="taskForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述（选填）"
          />
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
import { useRouter } from 'vue-router'
import {
  Plus, Document, VideoPlay, CircleCheck, CircleCheckFilled, User, Calendar,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import DatasetSelect from '@/components/DatasetSelect/index.vue'
import UserSelect from '@/components/UserSelect/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import { getLabelTasks, createLabelTask, getLabelTaskSummary } from '@/api/label'

const router = useRouter()
const taskModal = useModal()
const submitting = ref(false)

// ============ Status & Type Maps ============
const statusMap: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待标注', color: '#94a3b8' },
  IN_PROGRESS: { label: '进行中', color: '#0ea5e9' },
  COMPLETED: { label: '已完成', color: '#10b981' },
  PAUSED: { label: '已暂停', color: '#f59e0b' },
}

const typeColorMap: Record<string, 'primary' | 'success'> = {
  IMAGE: 'primary',
  TEXT: 'success',
}

// ============ Summary Stats ============
const summary = reactive({
  totalTasks: 0,
  inProgress: 0,
  labeledData: 0,
  avgConsistency: 0,
})

async function fetchSummary() {
  try {
    const res = await getLabelTaskSummary()
    const data = res.data.data
    summary.totalTasks = data.totalTasks
    summary.inProgress = data.inProgress
    summary.labeledData = data.labeledData
    summary.avgConsistency = data.avgConsistency
  } catch {
    // Summary fetch failure should not block the page
  }
}

// ============ Filters ============
const filters = reactive({
  task_type: undefined as string | undefined,
  format: undefined as string | undefined,
  status: undefined as string | undefined,
  keyword: undefined as string | undefined,
})

// ============ Table hook ============
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getLabelTasks({
    page: params.page,
    page_size: params.pageSize,
    status: filters.status || undefined,
    task_type: filters.task_type || undefined,
  } as any)
)

// Card grid defaults to 6 per page (matching the 6/12/18 page-size options)
pagination.pageSize = 6

function applyFilters() {
  fetchData()
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

// ============ Task Form ============
const taskForm = reactive({
  name: '',
  task_type: 'IMAGE' as string,
  format: '矩形框标注' as string,
  dataset_id: undefined as any,
  assignee_ids: undefined as any,
  assignee_id: undefined as any,
  reviewer_id: undefined as any,
  labels: [] as string[],
  deadline: undefined as any,
  description: '',
  ai_preannotate: false,
})

async function handleCreate() {
  submitting.value = true
  try {
    await createLabelTask(taskForm)
    ElMessage.success('标注任务创建成功')
    taskModal.close()
    fetchData()
    fetchSummary()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchSummary()
  fetchData()
})
</script>

<style scoped>
/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-right {
  flex-shrink: 0;
}

/* Metric Row */
.metric-row {
  margin-bottom: 20px;
}

/* Task Card Grid */
.task-card-grid {
  margin-top: 4px;
}

.task-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.task-card:hover {
  border-color: #0ea5e9;
  box-shadow: 0 2px 12px rgba(14, 165, 233, 0.12);
  transform: translateY(-2px);
}

.task-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.4;
  flex: 1;
}

.type-tag {
  flex-shrink: 0;
  font-size: 12px;
}

.task-card-format {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.task-card-dataset {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 14px;
}

.task-card-progress {
  margin-bottom: 14px;
}

.task-card-progress :deep(.el-progress) {
  margin-bottom: 4px;
}

.progress-text {
  font-size: 12px;
  color: #94a3b8;
}

.task-card-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
  flex: 1;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.meta-item .el-icon {
  color: #cbd5e1;
  font-size: 14px;
}

.task-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.view-detail-link {
  font-size: 13px;
  color: #0ea5e9;
  cursor: pointer;
  transition: color 0.2s;
}

.view-detail-link:hover {
  color: #38bdf8;
}

/* Pagination */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.pagination-total {
  font-size: 14px;
  color: #94a3b8;
}
</style>
