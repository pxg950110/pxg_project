<template>
  <div class="page-container">
    <!-- Custom Header (not using PageContainer title slot since it only renders text) -->
    <div class="page-header">
      <div class="task-header">
        <div class="task-header-left">
          <el-button text @click="router.back()" class="back-btn">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <div class="task-title-block">
            <h2 class="task-title">{{ taskData?.name || '标注任务' }}</h2>
            <span class="task-subtitle">
              <el-tag v-for="tag in (taskData?.tags || [])" :key="tag" type="primary" class="mr-1">{{ tag }}</el-tag>
              <el-tag v-if="taskData?.status" :type="taskData.status === 'IN_PROGRESS' ? 'success' : taskData.status === 'COMPLETED' ? 'primary' : 'warning'">{{ taskData.status }}</el-tag>
            </span>
          </div>
        </div>
        <div class="task-header-right">
          <el-button>
            <el-icon class="mr-1"><Edit /></el-icon> 编辑任务
          </el-button>
          <el-button type="danger" plain>
            <el-icon class="mr-1"><Delete /></el-icon> 删除
          </el-button>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="page-content">
      <!-- 4 Metric Cards -->
      <el-row :gutter="16" class="mb-4">
        <el-col :span="6">
          <MetricCard
            title="标注进度"
            :value="progressValue"
            :suffix="`/${totalItems}`"
            :icon="DataBoard"
          />
        </el-col>
        <el-col :span="6">
          <MetricCard
            title="标注员"
            :value="annotatorCount"
            suffix="人"
            :icon="User"
          />
        </el-col>
        <el-col :span="6">
          <MetricCard
            title="标注数据"
            :value="progressValue"
            suffix="条"
            :icon="Coin"
          />
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <div class="metric-card-inner">
              <div class="metric-content">
                <div class="metric-title">平均一致性</div>
                <div class="metric-value">
                  <span class="value-number" style="color: #10b981">{{ avgConsistency.toFixed(2) }}</span>
                </div>
              </div>
              <div class="metric-icon">
                <el-icon :size="28"><CircleCheck /></el-icon>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Progress Bar -->
      <el-card shadow="never" class="mb-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <div class="flex items-center gap-4">
          <span class="whitespace-nowrap font-medium">总标注进度</span>
          <el-progress :percentage="progressPercent" class="flex-1" />
          <span class="whitespace-nowrap text-slate-400">{{ progressValue }} / {{ totalItems }}</span>
        </div>
      </el-card>

      <!-- 5 Tabs -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-tabs v-model="activeTab">
          <!-- Tab 1: 标注进度 -->
          <el-tab-pane label="标注进度" name="progress">
            <!-- Annotator Progress Table -->
            <el-table
              :data="annotatorProgressData"
              :show-header="true"
              size="default"
              row-key="name"
              class="mb-4"
            >
              <el-table-column label="标注员" prop="name" width="120" />
              <el-table-column label="已分配" prop="assigned" width="90" />
              <el-table-column label="已完成" prop="completed" width="90" />
              <el-table-column label="进行中" prop="inProgress" width="90" />
              <el-table-column label="待处理" prop="pending" width="90" />
              <el-table-column label="完成率" width="100">
                <template #default="{ row }">
                  <span :style="{ color: getCompletionColor(row.completionRate), fontWeight: 500 }">
                    {{ row.completionRate }}%
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button link type="primary" @click="router.push(`/label/workspace/${route.params.id}`)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div>
              <el-button type="primary" class="mr-3">分配标注</el-button>
              <el-button @click="handleBatchExport">批量导出</el-button>
            </div>
          </el-tab-pane>

          <!-- Tab 2: 质量控制 -->
          <el-tab-pane label="质量控制" name="quality">
            <!-- Quality Metric Cards -->
            <el-row :gutter="16" class="mb-4">
              <el-col :span="6">
                <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
                  <div class="stat-title">一致性</div>
                  <div class="stat-value" style="color: #10b981">0.92</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
                  <div class="stat-title">准确率</div>
                  <div class="stat-value" style="color: #0ea5e9">0.88</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
                  <div class="stat-title">召回率</div>
                  <div class="stat-value" style="color: #0ea5e9">0.91</div>
                </el-card>
              </el-col>
              <el-col :span="6">
                <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
                  <div class="stat-title">F1</div>
                  <div class="stat-value" style="color: #0ea5e9">0.89</div>
                </el-card>
              </el-col>
            </el-row>

            <!-- Quality Trend Chart -->
            <el-card shadow="never" size="small" class="mb-4 !rounded-lg !border-slate-200/80">
              <template #header>
                <span class="font-semibold text-slate-900">质量趋势</span>
              </template>
              <MetricChart :option="qualityTrendOption" height="280px" />
            </el-card>

            <!-- Recent Quality Issues -->
            <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
              <template #header>
                <span class="font-semibold text-slate-900">近期质量问题</span>
              </template>
              <el-table :data="qualityIssueData" size="small" row-key="id">
                <el-table-column label="ID" prop="id" width="60" />
                <el-table-column label="标注员" prop="annotator" width="100" />
                <el-table-column label="问题类型" prop="issueType" width="120" />
                <el-table-column label="严重程度" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.severity === '高' ? 'danger' : row.severity === '中' ? 'warning' : 'primary'">
                      {{ row.severity }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status === '已解决' ? 'success' : 'warning'">
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="时间" prop="time" width="170" />
              </el-table>
            </el-card>
          </el-tab-pane>

          <!-- Tab 3: 标注人员 -->
          <el-tab-pane label="标注人员" name="personnel">
            <el-row :gutter="16">
              <el-col :span="6" v-for="person in personnelData" :key="person.name">
                <el-card shadow="hover" size="small" class="!rounded-lg !border-slate-200/80 text-center">
                  <el-avatar :size="56" :style="{ backgroundColor: person.color, marginBottom: '12px' }">
                    {{ person.name.charAt(0) }}
                  </el-avatar>
                  <div class="mb-1 text-base font-medium">{{ person.name }}</div>
                  <el-tag :type="person.role === '审核员' ? 'warning' : 'primary'" class="mb-3">
                    {{ person.role }}
                  </el-tag>
                  <div class="text-left text-[13px] text-slate-600">
                    <div class="flex justify-between py-1">
                      <span>已分配</span>
                      <span>{{ person.assigned }} 条</span>
                    </div>
                    <div class="flex justify-between py-1">
                      <span>已完成</span>
                      <span>{{ person.completed }} 条</span>
                    </div>
                    <div class="flex justify-between py-1">
                      <span>状态</span>
                      <span :style="{ color: person.status === '在线' ? '#10b981' : '#94a3b8' }">{{ person.status }}</span>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- Tab 4: 标注统计 -->
          <el-tab-pane label="标注统计" name="statistics">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
                  <template #header>
                    <span class="font-semibold text-slate-900">标注分布</span>
                  </template>
                  <MetricChart :option="labelDistOption" height="300px" />
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never" size="small" class="!rounded-lg !border-slate-200/80">
                  <template #header>
                    <span class="font-semibold text-slate-900">每日标注数量</span>
                  </template>
                  <MetricChart :option="dailyCountOption" height="300px" />
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- Tab 5: 操作日志 -->
          <el-tab-pane label="操作日志" name="logs">
            <el-table :data="pagedLogs" size="default" row-key="id">
              <el-table-column label="时间" prop="time" width="170" />
              <el-table-column label="操作人" prop="operator" width="100" />
              <el-table-column label="操作类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="logTypeColorMap[row.type] || 'info'">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="详情" prop="detail" min-width="200" show-overflow-tooltip />
            </el-table>
            <el-pagination
              v-if="logData.length > 10"
              class="mt-4 justify-end"
              background
              layout="total, prev, pager, next"
              :total="logData.length"
              :page-size="10"
              :current-page="logPage"
              @current-change="(p: number) => (logPage = p)"
            />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- Review Modal -->
    <el-dialog
      v-model="reviewModal.visible"
      title="标注审核"
      width="720px"
    >
      <div class="review-modal-body">
        <el-row :gutter="16">
          <!-- Left: Image Preview -->
          <el-col :span="10">
            <div class="image-preview-box">
              <el-icon :size="48" color="#cbd5e1"><Camera /></el-icon>
              <div class="mt-2 text-slate-300">影像预览区域</div>
            </div>
          </el-col>
          <!-- Right: Annotator Comparison -->
          <el-col :span="14">
            <!-- Annotator A -->
            <el-card size="small" shadow="never" class="annotator-card annotator-card-a mb-3 !rounded-lg !border-slate-200/80">
              <template #header>
                <span style="color: #0ea5e9">标注员 A — 李医生</span>
              </template>
              <pre class="annotation-json">{
  "label": "nodule",
  "bbox": [120, 85, 210, 175],
  "confidence": 0.95
}</pre>
            </el-card>
            <!-- Annotator B -->
            <el-card size="small" shadow="never" class="annotator-card annotator-card-b mb-3 !rounded-lg !border-slate-200/80">
              <template #header>
                <span style="color: #10b981">标注员 B — 王技师</span>
              </template>
              <pre class="annotation-json">{
  "label": "nodule",
  "bbox": [118, 83, 215, 178],
  "confidence": 0.91
}</pre>
            </el-card>
            <!-- IoU Score -->
            <div class="iou-score">
              <span class="text-slate-600">IoU / 一致性得分:</span>
              <span style="font-size: 20px; font-weight: 600; color: #10b981; margin-left: 8px">0.87</span>
            </div>
          </el-col>
        </el-row>
      </div>
      <!-- Custom Footer -->
      <div class="review-modal-footer">
        <el-button type="danger" plain @click="reviewModal.close()">驳回</el-button>
        <el-button type="success" @click="reviewModal.close()">通过</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Edit, Delete, DataBoard, User, Coin, CircleCheck, Camera,
} from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { useModal } from '@/hooks/useModal'
import { getLabelTask, getLabelTaskStats } from '@/api/label'

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
const annotatorProgressData = computed(() => taskStats.value?.annotatorProgress || [])

function getCompletionColor(rate: number): string {
  if (rate >= 80) return '#10b981'
  if (rate >= 50) return '#f59e0b'
  return '#ef4444'
}

// =============================================
// Tab 2: 质量控制
// =============================================
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
      { name: '一致性', type: 'bar', data: trendData.consistency || [], itemStyle: { color: '#10b981' } },
      { name: '准确率', type: 'bar', data: trendData.accuracy || [], itemStyle: { color: '#0ea5e9' } },
      { name: 'F1', type: 'bar', data: trendData.f1 || [], itemStyle: { color: '#8b5cf6' } },
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
            { offset: 0, color: '#0ea5e9' },
            { offset: 1, color: '#7dd3fc' },
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
const logTypeColorMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  '创建任务': 'primary',
  '分配标注': 'primary',
  '完成标注': 'success',
  '审核通过': 'success',
  '审核驳回': 'danger',
  '修改设置': 'warning',
  '添加标注员': 'warning',
  '导出数据': 'primary',
}

const logData = computed(() => taskStats.value?.logs || [])
const logPage = ref(1)
const pagedLogs = computed(() => logData.value.slice((logPage.value - 1) * 10, logPage.value * 10))

function handleBatchExport() {
  const data = annotatorProgressData.value
  if (!data.length) { ElMessage.warning('暂无数据可导出'); return }
  const rows = [
    ['标注员', '已分配', '已完成', '进行中', '待处理', '完成率(%)'],
    ...data.map((r: any) => [r.name, r.assigned, r.completed, r.inProgress, r.pending, r.completionRate]),
  ]
  const csv = '﻿' + rows.map(r => r.join(',')).join('\n')
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
  color: #0f172a;
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
  color: #94a3b8;
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
  background: #f0f9ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #0ea5e9;
  flex-shrink: 0;
}

/* Quality statistic cards */
.stat-title {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 6px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

/* Review Modal */
.review-modal-body {
  min-height: 300px;
}
.image-preview-box {
  width: 100%;
  height: 260px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.annotator-card-a {
  border-left: 3px solid #0ea5e9;
}
.annotator-card-b {
  border-left: 3px solid #10b981;
}
.annotation-json {
  background: #f8fafc;
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
