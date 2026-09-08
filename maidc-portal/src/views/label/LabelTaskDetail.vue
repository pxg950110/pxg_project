<template>
  <div class="page-container">
    <!-- Custom Header (not using PageContainer title slot since it only renders text) -->
    <div class="page-header">
      <div class="task-header">
        <div class="task-header-left">
          <a-button type="text" @click="router.back()" class="back-btn">
            <ArrowLeftOutlined />
          </a-button>
          <div class="task-title-block">
            <h2 class="task-title">{{ taskData?.name || '标注任务' }}</h2>
            <span class="task-subtitle">
              <a-tag v-for="tag in (taskData?.tags || [])" :key="tag" color="blue">{{ tag }}</a-tag>
              <a-tag v-if="taskData?.status" :color="taskData.status === 'IN_PROGRESS' ? 'green' : taskData.status === 'COMPLETED' ? 'blue' : 'orange'">{{ taskData.status }}</a-tag>
            </span>
          </div>
        </div>
        <div class="task-header-right">
          <a-button>
            <EditOutlined /> 编辑任务
          </a-button>
          <a-button danger ghost>
            <DeleteOutlined /> 删除
          </a-button>
        </div>
      </div>
    </div>

    <a-spin :spinning="loading">
      <div class="page-content">
        <!-- 4 Metric Cards -->
        <a-row :gutter="16" style="margin-bottom: 16px">
          <a-col :span="6">
            <MetricCard
              title="标注进度"
              :value="progressValue"
              :suffix="`/${totalItems}`"
              :icon="DashboardOutlined"
            />
          </a-col>
          <a-col :span="6">
            <MetricCard
              title="标注员"
              :value="annotatorCount"
              suffix="人"
              :icon="TeamOutlined"
            />
          </a-col>
          <a-col :span="6">
            <MetricCard
              title="标注数据"
              :value="progressValue"
              suffix="条"
              :icon="DatabaseOutlined"
            />
          </a-col>
          <a-col :span="6">
            <a-card :bordered="false" class="metric-card">
              <div class="metric-card-inner">
                <div class="metric-content">
                  <div class="metric-title">平均一致性</div>
                  <div class="metric-value">
                    <span class="value-number" style="color: #52c41a">{{ avgConsistency.toFixed(2) }}</span>
                  </div>
                </div>
                <div class="metric-icon">
                  <CheckCircleOutlined />
                </div>
              </div>
            </a-card>
          </a-col>
        </a-row>

        <!-- Progress Bar -->
        <a-card style="margin-bottom: 16px">
          <div style="display: flex; align-items: center; gap: 16px">
            <span style="white-space: nowrap; font-weight: 500">总标注进度</span>
            <a-progress :percent="progressPercent" style="flex: 1" />
            <span style="white-space: nowrap; color: rgba(0,0,0,0.45)">{{ progressValue }} / {{ totalItems }}</span>
          </div>
        </a-card>

        <!-- 5 Tabs -->
        <a-card>
          <a-tabs v-model:activeKey="activeTab">
            <!-- Tab 1: 标注进度 -->
            <a-tab-pane key="progress" tab="标注进度">
              <!-- Annotator Progress Table -->
              <a-table
                :columns="annotatorProgressColumns"
                :data-source="annotatorProgressData"
                :pagination="false"
                size="middle"
                row-key="name"
                style="margin-bottom: 16px"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'completionRate'">
                    <span :style="{ color: getCompletionColor(record.completionRate), fontWeight: 500 }">
                      {{ record.completionRate }}%
                    </span>
                  </template>
                  <template v-if="column.key === 'action'">
                    <a @click="router.push(`/label/workspace/${route.params.id}`)">查看</a>
                  </template>
                </template>
              </a-table>
              <div>
                <a-button type="primary" style="margin-right: 12px">分配标注</a-button>
                <a-button @click="handleBatchExport">批量导出</a-button>
              </div>
            </a-tab-pane>

            <!-- Tab 2: 质量控制 -->
            <a-tab-pane key="quality" tab="质量控制">
              <!-- Quality Metric Cards -->
              <a-row :gutter="16" style="margin-bottom: 16px">
                <a-col :span="6">
                  <a-card size="small">
                    <a-statistic title="一致性" :value="0.92" :precision="2" :value-style="{ color: '#52c41a' }" />
                  </a-card>
                </a-col>
                <a-col :span="6">
                  <a-card size="small">
                    <a-statistic title="准确率" :value="0.88" :precision="2" :value-style="{ color: '#1677ff' }" />
                  </a-card>
                </a-col>
                <a-col :span="6">
                  <a-card size="small">
                    <a-statistic title="召回率" :value="0.91" :precision="2" :value-style="{ color: '#1677ff' }" />
                  </a-card>
                </a-col>
                <a-col :span="6">
                  <a-card size="small">
                    <a-statistic title="F1" :value="0.89" :precision="2" :value-style="{ color: '#1677ff' }" />
                  </a-card>
                </a-col>
              </a-row>

              <!-- Quality Trend Chart -->
              <a-card title="质量趋势" size="small" style="margin-bottom: 16px">
                <MetricChart :option="qualityTrendOption" height="280px" />
              </a-card>

              <!-- Recent Quality Issues -->
              <a-card title="近期质量问题" size="small">
                <a-table
                  :columns="qualityIssueColumns"
                  :data-source="qualityIssueData"
                  :pagination="false"
                  size="small"
                  row-key="id"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'severity'">
                      <a-tag :color="record.severity === '高' ? 'red' : record.severity === '中' ? 'orange' : 'blue'">
                        {{ record.severity }}
                      </a-tag>
                    </template>
                    <template v-if="column.key === 'status'">
                      <a-tag :color="record.status === '已解决' ? 'green' : 'volcano'">
                        {{ record.status }}
                      </a-tag>
                    </template>
                  </template>
                </a-table>
              </a-card>
            </a-tab-pane>

            <!-- Tab 3: 标注人员 -->
            <a-tab-pane key="personnel" tab="标注人员">
              <a-row :gutter="16">
                <a-col :span="6" v-for="person in personnelData" :key="person.name">
                  <a-card size="small" hoverable style="text-align: center">
                    <a-avatar :size="56" :style="{ backgroundColor: person.color, marginBottom: '12px' }">
                      {{ person.name.charAt(0) }}
                    </a-avatar>
                    <div style="font-size: 16px; font-weight: 500; margin-bottom: 4px">{{ person.name }}</div>
                    <a-tag :color="person.role === '审核员' ? 'purple' : 'blue'" style="margin-bottom: 12px">
                      {{ person.role }}
                    </a-tag>
                    <div style="font-size: 13px; color: rgba(0,0,0,0.65); textAlign: 'left'">
                      <div style="display: flex; justify-content: space-between; padding: 4px 0">
                        <span>已分配</span>
                        <span>{{ person.assigned }} 条</span>
                      </div>
                      <div style="display: flex; justify-content: space-between; padding: 4px 0">
                        <span>已完成</span>
                        <span>{{ person.completed }} 条</span>
                      </div>
                      <div style="display: flex; justify-content: space-between; padding: 4px 0">
                        <span>状态</span>
                        <span :style="{ color: person.status === '在线' ? '#52c41a' : '#999' }">{{ person.status }}</span>
                      </div>
                    </div>
                  </a-card>
                </a-col>
              </a-row>
            </a-tab-pane>

            <!-- Tab 4: 标注统计 -->
            <a-tab-pane key="statistics" tab="标注统计">
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-card title="标注分布" size="small">
                    <MetricChart :option="labelDistOption" height="300px" />
                  </a-card>
                </a-col>
                <a-col :span="12">
                  <a-card title="每日标注数量" size="small">
                    <MetricChart :option="dailyCountOption" height="300px" />
                  </a-card>
                </a-col>
              </a-row>
            </a-tab-pane>

            <!-- Tab 5: 操作日志 -->
            <a-tab-pane key="logs" tab="操作日志">
              <a-table
                :columns="logColumns"
                :data-source="logData"
                :pagination="{ pageSize: 10 }"
                size="middle"
                row-key="id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'type'">
                    <a-tag :color="logTypeColorMap[record.type] || 'default'">{{ record.type }}</a-tag>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </div>
    </a-spin>

    <!-- Review Modal -->
    <a-modal
      v-model:open="reviewModal.visible"
      title="标注审核"
      width="720px"
      :footer="null"
    >
      <div class="review-modal-body">
        <a-row :gutter="16">
          <!-- Left: Image Preview -->
          <a-col :span="10">
            <div class="image-preview-box">
              <CameraOutlined style="font-size: 48px; color: rgba(0,0,0,0.15)" />
              <div style="margin-top: 8px; color: rgba(0,0,0,0.25)">影像预览区域</div>
            </div>
          </a-col>
          <!-- Right: Annotator Comparison -->
          <a-col :span="14">
            <!-- Annotator A -->
            <a-card size="small" class="annotator-card annotator-card-a" style="margin-bottom: 12px">
              <template #title>
                <span style="color: #1677ff">标注员 A — 李医生</span>
              </template>
              <pre class="annotation-json">{
  "label": "nodule",
  "bbox": [120, 85, 210, 175],
  "confidence": 0.95
}</pre>
            </a-card>
            <!-- Annotator B -->
            <a-card size="small" class="annotator-card annotator-card-b" style="margin-bottom: 12px">
              <template #title>
                <span style="color: #52c41a">标注员 B — 王技师</span>
              </template>
              <pre class="annotation-json">{
  "label": "nodule",
  "bbox": [118, 83, 215, 178],
  "confidence": 0.91
}</pre>
            </a-card>
            <!-- IoU Score -->
            <div class="iou-score">
              <span style="color: rgba(0,0,0,0.65)">IoU / 一致性得分:</span>
              <span style="font-size: 20px; font-weight: 600; color: #52c41a; margin-left: 8px">0.87</span>
            </div>
          </a-col>
        </a-row>
      </div>
      <!-- Custom Footer -->
      <div class="review-modal-footer">
        <a-button danger ghost @click="reviewModal.close()">驳回</a-button>
        <a-button type="primary" style="background-color: #52c41a; border-color: #52c41a" @click="reviewModal.close()">通过</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  EditOutlined,
  DeleteOutlined,
  DashboardOutlined,
  TeamOutlined,
  DatabaseOutlined,
  CheckCircleOutlined,
  CameraOutlined,
} from '@ant-design/icons-vue'
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
const loading = ref(false)
const activeTab = ref('progress')

// Task data
const taskData = ref<any>(null)
const taskStats = ref<any>(null)
const taskId = computed(() => Number(route.params.id))

// Derived metrics from API data
const progressValue = computed(() => taskStats.value?.labeledCount || 0)
const totalItems = computed(() => taskStats.value?.totalItems || 0)
const progressPercent = computed(() => totalItems.value > 0 ? Math.round(progressValue.value / totalItems.value * 100) : 0)
const annotatorCount = computed(() => taskStats.value?.annotatorCount || 0)
const avgConsistency = computed(() => taskStats.value?.avgConsistency ?? 0)

async function loadTaskData() {
  loading.value = true
  try {
    const [taskRes, statsRes] = await Promise.allSettled([
      getLabelTask(taskId.value),
      getLabelTaskStats(taskId.value),
    ])
    if (taskRes.status === 'fulfilled') taskData.value = taskRes.value.data.data
    if (statsRes.status === 'fulfilled') taskStats.value = statsRes.value.data.data
  } finally {
    loading.value = false
  }
}

// =============================================
// Tab 1: 标注进度 — Annotator Progress Table
// =============================================
const annotatorProgressColumns = [
  { title: '标注员', dataIndex: 'name', key: 'name', width: 120 },
  { title: '已分配', dataIndex: 'assigned', key: 'assigned', width: 90 },
  { title: '已完成', dataIndex: 'completed', key: 'completed', width: 90 },
  { title: '进行中', dataIndex: 'inProgress', key: 'inProgress', width: 90 },
  { title: '待处理', dataIndex: 'pending', key: 'pending', width: 90 },
  { title: '完成率', dataIndex: 'completionRate', key: 'completionRate', width: 100 },
  { title: '操作', key: 'action', width: 80 },
]

const annotatorProgressData = computed(() => taskStats.value?.annotatorProgress || [])

function getCompletionColor(rate: number): string {
  if (rate >= 80) return '#52c41a'
  if (rate >= 50) return '#faad14'
  return '#ff4d4f'
}

// =============================================
// Tab 2: 质量控制
// =============================================
const qualityIssueColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '标注员', dataIndex: 'annotator', key: 'annotator', width: 100 },
  { title: '问题类型', dataIndex: 'issueType', key: 'issueType', width: 120 },
  { title: '严重程度', dataIndex: 'severity', key: 'severity', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '时间', dataIndex: 'time', key: 'time', width: 170 },
]

const qualityIssueData = computed(() => taskStats.value?.qualityIssues || [])

const qualityTrendOption = computed(() => {
  const trendData = taskStats.value?.qualityTrend || {}
  const dates = trendData.dates || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['一致性', '准确率', 'F1'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', min: 0.7, max: 1.0 },
    series: [
      { name: '一致性', type: 'bar', data: trendData.consistency || [], itemStyle: { color: '#52c41a' } },
      { name: '准确率', type: 'bar', data: trendData.accuracy || [], itemStyle: { color: '#1677ff' } },
      { name: 'F1', type: 'bar', data: trendData.f1 || [], itemStyle: { color: '#722ed1' } },
    ],
  }
})

// =============================================
// Tab 3: 标注人员
// =============================================
const personnelData = computed(() => taskStats.value?.personnel || [])

// =============================================
// Tab 4: 标注统计
// =============================================
const labelDistOption = computed(() => {
  const distData = taskStats.value?.labelDistribution || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      data: distData,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    }],
  }
})

const dailyCountOption = computed(() => {
  const dailyData = taskStats.value?.dailyCount || {}
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dailyData.dates || [] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: dailyData.counts || [],
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#1677ff' },
            { offset: 1, color: '#69b1ff' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
    }],
  }
})

// =============================================
// Tab 5: 操作日志
// =============================================
const logColumns = [
  { title: '时间', dataIndex: 'time', key: 'time', width: 170 },
  { title: '操作人', dataIndex: 'operator', key: 'operator', width: 100 },
  { title: '操作类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '详情', dataIndex: 'detail', key: 'detail' },
]

const logTypeColorMap: Record<string, string> = {
  '创建任务': 'blue',
  '分配标注': 'cyan',
  '完成标注': 'green',
  '审核通过': 'green',
  '审核驳回': 'red',
  '修改设置': 'orange',
  '添加标注员': 'purple',
  '导出数据': 'geekblue',
}

const logData = computed(() => taskStats.value?.logs || [])

function handleBatchExport() {
  const data = annotatorProgressData.value
  if (!data.length) { message.warning('暂无数据可导出'); return }
  const rows = [
    ['标注员', '已分配', '已完成', '进行中', '待处理', '完成率(%)'],
    ...data.map((r: any) => [r.name, r.assigned, r.completed, r.inProgress, r.pending, r.completionRate]),
  ]
  const csv = '\uFEFF' + rows.map(r => r.join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${taskData.value?.name || 'label_task'}_progress.csv`
  a.click()
  window.URL.revokeObjectURL(url)
}

onMounted(loadTaskData)
</script>

<style scoped>
.page-container {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  min-height: 100%;
}
.page-header {
  margin-bottom: 20px;
}
.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.task-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.back-btn {
  padding: 4px 8px;
}
.task-title-block {
  display: flex;
  align-items: center;
  gap: 12px;
}
.task-title {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}
.task-subtitle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.task-header-right {
  display: flex;
  gap: 8px;
}
.page-content {
  min-height: 200px;
}

/* Fourth Metric Card — consistency with green text */
.metric-card {
  border-radius: 8px;
}
.metric-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.metric-content {
  flex: 1;
}
.metric-title {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}
.metric-value {
  font-size: 0;
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.value-number {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}
.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: rgba(22, 119, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #1677ff;
  flex-shrink: 0;
}

/* Review Modal */
.review-modal-body {
  min-height: 300px;
}
.image-preview-box {
  width: 100%;
  height: 260px;
  background: #fafafa;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.annotator-card-a {
  border-left: 3px solid #1677ff;
}
.annotator-card-b {
  border-left: 3px solid #52c41a;
}
.annotation-json {
  background: #f5f5f5;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.iou-score {
  display: flex;
  align-items: center;
  padding: 8px 0;
}
.review-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
