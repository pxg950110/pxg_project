<template>
  <PageContainer :title="task?.name || '标注任务详情'" :loading="loading">
    <template #extra>
      <a-space>
        <a-button @click="router.push(`/label/workspace/${route.params.id}`)">
          <EditOutlined /> 进入工作台
        </a-button>
        <a-button @click="router.back()">返回</a-button>
      </a-space>
    </template>

    <template v-if="task">
      <!-- Task Info Header -->
      <a-card style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="任务名称">{{ task.name }}</a-descriptions-item>
          <a-descriptions-item label="标注类型">
            <a-tag :color="task.task_type === 'TEXT' ? 'blue' : 'green'">{{ task.task_type }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="数据集">{{ task.dataset_name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="标注人">{{ task.assignee_name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[task.status] || 'default'">{{ task.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(task.created_at) }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Metric Cards -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <MetricCard title="总数" :value="stats.total" :loading="statsLoading" />
        </a-col>
        <a-col :span="6">
          <MetricCard title="已完成" :value="stats.completed" :loading="statsLoading" />
        </a-col>
        <a-col :span="6">
          <MetricCard title="进行中" :value="stats.in_progress" :loading="statsLoading" />
        </a-col>
        <a-col :span="6">
          <MetricCard title="已审核" :value="stats.reviewed" :loading="statsLoading" />
        </a-col>
      </a-row>

      <!-- Progress Bar -->
      <a-card style="margin-bottom: 16px">
        <div style="display: flex; align-items: center; gap: 16px">
          <span style="white-space: nowrap; color: rgba(0,0,0,0.65)">标注进度</span>
          <a-progress :percent="progressPercent" :status="progressStatus" style="flex: 1" />
          <span style="white-space: nowrap; color: rgba(0,0,0,0.45)">
            {{ stats.completed }} / {{ stats.total }}
          </span>
        </div>
      </a-card>

      <!-- Tabs -->
      <a-card>
        <a-tabs v-model:activeKey="activeTab">
          <!-- Annotations Tab -->
          <a-tab-pane key="annotations" tab="标注列表">
            <a-table
              :columns="annotationColumns"
              :data-source="annotations"
              :loading="annotationsLoading"
              :pagination="{ pageSize: 20 }"
              size="small"
              row-key="id"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="annotationStatusMap[record.status] || 'default'">{{ record.status }}</a-tag>
                </template>
                <template v-if="column.key === 'created_at'">
                  {{ formatDateTime(record.created_at) }}
                </template>
                <template v-if="column.key === 'action'">
                  <a-space>
                    <a @click="router.push(`/label/workspace/${route.params.id}?item=${record.id}`)">标注</a>
                    <a v-if="record.status === 'COMPLETED'" @click="openReview(record)">审核</a>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Statistics Tab -->
          <a-tab-pane key="statistics" tab="统计">
            <a-row :gutter="16">
              <a-col :span="14">
                <a-card title="标注人统计" size="small">
                  <a-table
                    :columns="annotatorStatColumns"
                    :data-source="annotatorStats"
                    size="small"
                    row-key="user_id"
                    :pagination="false"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'progress'">
                        <a-progress :percent="record.progress" size="small" />
                      </template>
                    </template>
                  </a-table>
                </a-card>
              </a-col>
              <a-col :span="10">
                <a-card title="标注分布" size="small">
                  <MetricChart :option="labelDistOption" height="280px" />
                </a-card>
              </a-col>
            </a-row>
          </a-tab-pane>

          <!-- Settings Tab -->
          <a-tab-pane key="settings" tab="设置">
            <a-form layout="vertical" style="max-width: 600px">
              <a-form-item label="任务名称">
                <a-input v-model:value="settingsForm.name" />
              </a-form-item>
              <a-form-item label="标签列表">
                <a-select
                  v-model:value="settingsForm.labels"
                  mode="tags"
                  placeholder="输入标签后回车添加"
                />
              </a-form-item>
              <a-form-item label="允许多标签">
                <a-switch v-model:checked="settingsForm.multi_label" />
              </a-form-item>
              <a-form-item label="要求审核">
                <a-switch v-model:checked="settingsForm.require_review" />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" @click="handleSaveSettings" :loading="savingSettings">保存设置</a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>

    <!-- Annotation Review Modal -->
    <a-modal
      v-model:open="reviewModal.visible"
      title="标注审核"
      @ok="handleReview"
      :confirm-loading="reviewing"
      width="500px"
    >
      <template v-if="reviewModal.currentRecord?.value">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="标注内容">{{ reviewModal.currentRecord.value.content }}</a-descriptions-item>
          <a-descriptions-item label="标注标签">{{ reviewModal.currentRecord.value.label }}</a-descriptions-item>
          <a-descriptions-item label="标注人">{{ reviewModal.currentRecord.value.annotator_name }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="审核结果" required>
            <a-radio-group v-model:value="reviewForm.action">
              <a-radio value="APPROVE">通过</a-radio>
              <a-radio value="REJECT">驳回</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="审核意见">
            <a-textarea v-model:value="reviewForm.comment" :rows="2" placeholder="可选填写审核意见" />
          </a-form-item>
        </a-form>
      </template>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { EditOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { useModal } from '@/hooks/useModal'
import { getLabelTask, getLabelTaskStats } from '@/api/label'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'LabelTaskDetail' })

const route = useRoute()
const router = useRouter()
const reviewModal = useModal()
const reviewing = ref(false)
const savingSettings = ref(false)

const task = ref<any>(null)
const loading = ref(false)
const activeTab = ref('annotations')

// Stats
const stats = reactive({ total: 0, completed: 0, in_progress: 0, reviewed: 0 })
const statsLoading = ref(false)

// Annotations
const annotations = ref<any[]>([])
const annotationsLoading = ref(false)

// Annotator stats
const annotatorStats = ref<any[]>([])

const statusColorMap: Record<string, string> = {
  PENDING: 'default',
  IN_PROGRESS: 'processing',
  COMPLETED: 'success',
}

const annotationStatusMap: Record<string, string> = {
  PENDING: 'default',
  ANNOTATED: 'blue',
  REVIEWED: 'green',
  REJECTED: 'red',
}

const progressPercent = computed(() => {
  if (!stats.total) return 0
  return Math.round((stats.completed / stats.total) * 100)
})

const progressStatus = computed(() => {
  if (progressPercent.value >= 100) return 'success' as const
  if (task.value?.status === 'COMPLETED') return 'success' as const
  return 'active' as const
})

const labelDistOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['35%', '65%'],
    data: [] as { value: number; name: string }[],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
  }],
})

const annotationColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '数据项', dataIndex: 'data_item', key: 'data_item', ellipsis: true },
  { title: '标注标签', dataIndex: 'label', key: 'label', width: 120 },
  { title: '标注人', dataIndex: 'annotator_name', key: 'annotator_name', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

const annotatorStatColumns = [
  { title: '标注人', dataIndex: 'user_name', key: 'user_name', width: 120 },
  { title: '已完成', dataIndex: 'completed', key: 'completed', width: 80 },
  { title: '已审核', dataIndex: 'reviewed', key: 'reviewed', width: 80 },
  { title: '驳回数', dataIndex: 'rejected', key: 'rejected', width: 80 },
  { title: '进度', dataIndex: 'progress', key: 'progress' },
]

const settingsForm = reactive({
  name: '',
  labels: [] as string[],
  multi_label: false,
  require_review: true,
})

const reviewForm = reactive({ action: 'APPROVE', comment: '' })

async function loadTask() {
  loading.value = true
  try {
    const res = await getLabelTask(Number(route.params.id))
    task.value = res.data.data
    settingsForm.name = task.value.name
    settingsForm.labels = task.value.labels || []
    settingsForm.multi_label = task.value.multi_label || false
    settingsForm.require_review = task.value.require_review !== false
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getLabelTaskStats(Number(route.params.id))
    const data = res.data.data
    stats.total = data.total || 0
    stats.completed = data.completed || 0
    stats.in_progress = data.in_progress || 0
    stats.reviewed = data.reviewed || 0

    // Annotator stats
    annotatorStats.value = data.annotator_stats || []

    // Label distribution chart
    const labelDist = data.label_distribution || []
    labelDistOption.value = {
      ...labelDistOption.value,
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        data: labelDist.map((item: any) => ({ value: item.count, name: item.label })),
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      }],
    }
  } finally {
    statsLoading.value = false
  }
}

async function loadAnnotations() {
  annotationsLoading.value = true
  try {
    const res = await request.get(`/label/tasks/${route.params.id}/annotations`)
    annotations.value = res.data.data?.items || res.data.data || []
  } finally {
    annotationsLoading.value = false
  }
}

function openReview(record: any) {
  reviewForm.action = 'APPROVE'
  reviewForm.comment = ''
  reviewModal.open(record)
}

async function handleReview() {
  reviewing.value = true
  try {
    const record = reviewModal.currentRecord!.value!
    await request.post(`/label/annotations/${record.id}/review`, reviewForm)
    message.success(reviewForm.action === 'APPROVE' ? '审核通过' : '已驳回')
    reviewModal.close()
    loadAnnotations()
    loadStats()
  } finally {
    reviewing.value = false
  }
}

async function handleSaveSettings() {
  savingSettings.value = true
  try {
    await request.put(`/label/tasks/${route.params.id}`, settingsForm)
    message.success('设置已保存')
    loadTask()
  } finally {
    savingSettings.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'annotations' && !annotations.value.length) loadAnnotations()
  else if (tab === 'statistics') loadStats()
})

onMounted(async () => {
  await loadTask()
  loadStats()
  loadAnnotations()
})
</script>
