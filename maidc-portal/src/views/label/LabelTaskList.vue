<template>
  <PageContainer title="标注任务">

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="filter-left">
        <a-select
          v-model:value="filters.task_type"
          placeholder="类型"
          allow-clear
          style="width: 130px"
          @change="applyFilters"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="IMAGE">影像标注</a-select-option>
          <a-select-option value="TEXT">文本标注</a-select-option>
        </a-select>

        <a-select
          v-model:value="filters.format"
          placeholder="格式"
          allow-clear
          style="width: 130px"
          @change="applyFilters"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="矩形框标注">矩形框</a-select-option>
          <a-select-option value="多边形标注">多边形</a-select-option>
          <a-select-option value="NER标注">NER</a-select-option>
        </a-select>

        <a-select
          v-model:value="filters.status"
          placeholder="状态"
          allow-clear
          style="width: 130px"
          @change="applyFilters"
        >
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="PENDING">待标注</a-select-option>
          <a-select-option value="IN_PROGRESS">进行中</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
          <a-select-option value="PAUSED">已暂停</a-select-option>
        </a-select>

        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索任务名称..."
          style="width: 220px"
          allow-clear
          @search="applyFilters"
          @pressEnter="applyFilters"
        />
      </div>
      <div class="filter-right">
        <a-button type="primary" @click="taskModal.open()">
          <PlusOutlined /> 新建任务
        </a-button>
      </div>
    </div>

    <!-- Metric Cards Row -->
    <a-row :gutter="[16, 16]" class="metric-row">
      <a-col :span="6">
        <MetricCard
          title="标注任务"
          :value="summary.totalTasks"
          suffix="个"
        >
          <template #icon><FileTextOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="进行中"
          :value="summary.inProgress"
          suffix="个"
          :trend="{ value: 3, type: 'up' }"
        >
          <template #icon><PlayCircleOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="已标注数据"
          :value="summary.labeledData"
          suffix="条"
          :trend="{ value: 12, type: 'up' }"
        >
          <template #icon><CheckCircleOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="平均一致性"
          :value="summary.avgConsistency"
          :trend="{ value: 3, type: 'up' }"
        >
          <template #icon><SafetyCertificateOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Card Grid -->
    <a-row :gutter="[16, 16]" class="task-card-grid">
      <a-col v-for="task in tableData" :key="task.id" :span="8">
        <div class="task-card" @click="router.push(`/label/detail/${task.id}`)">
          <div class="task-card-header">
            <span class="task-name">{{ task.name }}</span>
            <a-tag :color="typeColorMap[task.task_type]" class="type-tag">
              {{ task.task_type === 'IMAGE' ? '影像标注' : '文本标注' }}
            </a-tag>
          </div>

          <div class="task-card-format">{{ task.format }}</div>
          <div class="task-card-dataset">{{ task.dataset_name }}</div>

          <div class="task-card-progress">
            <a-progress
              :percent="task.progress"
              :stroke-color="task.progress === 100 ? '#52c41a' : '#1677ff'"
              size="small"
            />
            <span class="progress-text">{{ task.completed }}/{{ task.total }}</span>
          </div>

          <div class="task-card-meta">
            <div class="meta-item">
              <UserOutlined />
              <span>{{ task.assignees.length > 0 ? task.assignees.join(', ') : '未分配' }}</span>
            </div>
            <div v-if="task.deadline" class="meta-item">
              <CalendarOutlined />
              <span>{{ task.deadline }}</span>
            </div>
          </div>

          <div class="task-card-footer">
            <a-badge
              :status="statusMap[task.status]?.color"
              :text="statusMap[task.status]?.label"
            />
            <a class="view-detail-link" @click.stop="router.push(`/label/detail/${task.id}`)">
              查看详情 &rarr;
            </a>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- Pagination -->
    <div class="pagination-bar">
      <span class="pagination-total">共 {{ pagination.total }} 个任务</span>
      <a-pagination
        v-model:current="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-size-options="['6', '12', '18']"
        show-size-changer
        size="small"
        @change="onPageChange"
      />
    </div>

    <!-- Create Task Modal (two-column) -->
    <a-modal
      v-model:open="taskModal.visible"
      title="新建标注任务"
      width="720px"
      @ok="handleCreate"
      :confirm-loading="submitting"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="任务名称" required>
              <a-input v-model:value="taskForm.name" placeholder="请输入任务名称" />
            </a-form-item>
            <a-form-item label="关联数据集" required>
              <DatasetSelect v-model:value="taskForm.dataset_id" />
            </a-form-item>
            <a-form-item label="标签列表">
              <a-select
                v-model:value="taskForm.labels"
                mode="tags"
                placeholder="输入标签后回车添加"
              />
            </a-form-item>
            <a-form-item label="分配审核员">
              <UserSelect v-model:value="taskForm.reviewer_id" placeholder="选择审核员" />
            </a-form-item>
            <a-form-item label="AI 预标注">
              <a-switch v-model:checked="taskForm.ai_preannotate" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标注类型" required>
              <a-select v-model:value="taskForm.task_type" placeholder="请选择标注类型">
                <a-select-option value="IMAGE">影像标注</a-select-option>
                <a-select-option value="TEXT">文本标注</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="标注格式" required>
              <a-select v-model:value="taskForm.format" placeholder="请选择标注格式">
                <a-select-option value="矩形框标注">矩形框</a-select-option>
                <a-select-option value="多边形标注">多边形</a-select-option>
                <a-select-option value="椭圆标注">椭圆</a-select-option>
                <a-select-option value="自由绘制">自由绘制</a-select-option>
                <a-select-option value="NER标注">NER</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="分配标注员">
              <UserSelect v-model:value="taskForm.assignee_ids" multiple placeholder="选择标注员" />
            </a-form-item>
            <a-form-item label="截止日期">
              <a-date-picker
                v-model:value="taskForm.deadline"
                placeholder="选择截止日期"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="taskForm.description"
            :rows="3"
            placeholder="请输入任务描述（选填）"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined,
  FileTextOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined,
  UserOutlined,
  CalendarOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
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
  PENDING: { label: '待标注', color: 'default' },
  IN_PROGRESS: { label: '进行中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'success' },
  PAUSED: { label: '已暂停', color: 'warning' },
}

const typeColorMap: Record<string, string> = {
  IMAGE: 'blue',
  TEXT: 'green',
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
const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getLabelTasks({
    page: params.page,
    page_size: params.pageSize,
    status: filters.status || undefined,
    task_type: filters.task_type || undefined,
  } as any)
)

function applyFilters() {
  fetchData()
}

function onPageChange(page: number, pageSize: number) {
  fetchData({ page, pageSize })
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
    message.success('标注任务创建成功')
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
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.task-card:hover {
  border-color: #1677ff;
  box-shadow: 0 2px 12px rgba(22, 119, 255, 0.12);
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
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.4;
  flex: 1;
}

.type-tag {
  flex-shrink: 0;
  font-size: 12px;
}

.task-card-format {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 4px;
}

.task-card-dataset {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin-bottom: 14px;
}

.task-card-progress {
  margin-bottom: 14px;
}

.task-card-progress :deep(.ant-progress) {
  margin-bottom: 4px;
}

.progress-text {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
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
  color: rgba(0, 0, 0, 0.55);
}

.meta-item :deep(.anticon) {
  color: rgba(0, 0, 0, 0.35);
  font-size: 14px;
}

.task-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.view-detail-link {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
  transition: color 0.2s;
}

.view-detail-link:hover {
  color: #4096ff;
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
  color: rgba(0, 0, 0, 0.45);
}
</style>
