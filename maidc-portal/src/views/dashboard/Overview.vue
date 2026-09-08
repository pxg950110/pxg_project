<template>
  <PageContainer title="系统总览" subtitle="MAIDC 医疗 AI 数据中心运行概览">
    <a-spin :spinning="loading" tip="加载中...">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="welcome-info">
        <h2 class="welcome-greeting">{{ greeting }}，{{ userName }}</h2>
        <p class="welcome-date">今天是{{ currentDate }}</p>
        <p class="welcome-tasks">
          您有 <span class="task-count">{{ pendingApprovals }}</span> 条待办事项
        </p>
      </div>
      <div class="welcome-actions">
        <a-button type="primary" ghost @click="$router.push('/model/list')">
          <template #icon><PlusOutlined /></template>
          注册模型
        </a-button>
        <a-button type="primary" ghost @click="$router.push('/model/evaluations')">
          <template #icon><LineChartOutlined /></template>
          新建评估
        </a-button>
        <a-button type="primary" ghost @click="$router.push('/model/deployments')">
          <template #icon><AuditOutlined /></template>
          提交审批
        </a-button>
      </div>
    </div>

    <!-- Metric Cards Row 1: 3 cards -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="8">
        <MetricCard
          title="模型总数"
          :value="modelCount"
          suffix="个"
          :trend="{ value: 12, type: 'up' }"
        >
          <template #icon><ExperimentOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="8">
        <MetricCard
          title="活跃部署"
          :value="activeDeployments"
          suffix="个"
          :trend="{ value: 3, type: 'up' }"
        >
          <template #icon><RocketOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="8">
        <MetricCard
          title="今日推理次数"
          :value="dailyInferences"
          suffix="次"
          :trend="{ value: 8, type: 'up' }"
        >
          <template #icon><ThunderboltOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Metric Cards Row 2: 3 cards -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="8">
        <MetricCard
          title="患者记录"
          :value="patientRecords"
          suffix="条"
          :trend="{ value: 5, type: 'up' }"
        >
          <template #icon><TeamOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="8">
        <MetricCard
          title="研究项目"
          :value="researchProjects"
          suffix="个"
        >
          <template #icon><ProjectOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="8">
        <MetricCard
          title="待审批"
          :value="pendingApprovals"
          suffix="条"
          :trend="{ value: 2, type: 'down' }"
        >
          <template #icon><AuditOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Charts Row: Model Status + Recent Alerts -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="模型状态分布" :bordered="false">
          <template #extra>
            <a class="view-all-link" @click="$router.push('/model/list')">查看全部</a>
          </template>
          <MetricChart :option="modelStatusOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="最近告警" :bordered="false">
          <template #extra>
            <a class="view-all-link" @click="$router.push('/alert/active')">查看全部</a>
          </template>
          <div class="alert-list">
            <div
              v-for="(alert, index) in recentAlerts"
              :key="index"
              class="alert-item"
            >
              <div class="alert-left">
                <a-tag
                  :color="alert.severity === 'CRITICAL' ? 'red' : 'orange'"
                  class="alert-tag"
                >
                  {{ alert.severity }}
                </a-tag>
                <span class="alert-message">{{ alert.message }}</span>
              </div>
              <span class="alert-time">{{ alert.time }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Activity Feed + Data Source Status -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近活动" :bordered="false">
          <template #extra>
            <a class="view-all-link" @click="$router.push('/audit/logs')">查看全部</a>
          </template>
          <div class="activity-list">
            <div
              v-for="(item, index) in recentActivities"
              :key="index"
              class="activity-item"
            >
              <span class="activity-dot" :style="{ background: item.dotColor }"></span>
              <span class="activity-text">{{ item.text }}</span>
              <a-tag :color="item.tagColor" class="activity-tag">{{ item.category }}</a-tag>
              <span class="activity-time">{{ item.time }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="数据源连接状态" :bordered="false">
          <div class="datasource-list">
            <div
              v-for="(ds, index) in dataSources"
              :key="index"
              class="datasource-item"
            >
              <div class="datasource-info">
                <span class="datasource-name">{{ ds.name }}</span>
                <span class="datasource-desc">{{ ds.description }}</span>
              </div>
              <a-badge
                :status="ds.connected ? 'success' : 'error'"
                :text="ds.connected ? '已连接' : '断开'"
              />
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  ExperimentOutlined,
  RocketOutlined,
  LineChartOutlined,
  PlusOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  ProjectOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { useAuthStore } from '@/stores/auth'
import { getModels, getDeployments, getApprovals, getAlerts, getMetricsOverview } from '@/api/model'
import { getAuditLogs } from '@/api/audit'
import { getDataSources, getPatients, getProjects } from '@/api/data'

const router = useRouter()
const authStore = useAuthStore()

// ============ Loading State ============
const loading = ref(true)

// ============ Metric Cards (reactive) ============
const modelCount = ref(0)
const activeDeployments = ref(0)
const dailyInferences = ref(0)
const patientRecords = ref(0)
const researchProjects = ref(0)
const pendingApprovals = ref(0)

// ============ Welcome Section ============
const userName = computed(() => authStore.userInfo?.realName || '医生')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '晚上好'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const currentDate = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  return `${year}年${month}月${day}日`
})

// ============ Recent Alerts ============
const recentAlerts = ref<any[]>([])

// ============ Recent Activity ============
const recentActivities = ref<any[]>([])

// ============ Data Sources ============
const dataSources = ref<any[]>([])

// ============ Chart: Model Status Horizontal Bar ============
const modelStatusOption = ref(buildChartOption({}))

function buildChartOption(metricsData: Record<string, any>) {
  const distribution = metricsData.modelStatusDistribution || {}
  const categories = ['DRAFT', 'REGISTERED', 'PUBLISHED', 'DEPRECATED']
  const colorMap: Record<string, string> = {
    DRAFT: '#d9d9d9',
    REGISTERED: '#1677ff',
    PUBLISHED: '#52c41a',
    DEPRECATED: '#faad14',
  }
  const chartData = categories.map((cat) => ({
    value: distribution[cat] || 0,
    itemStyle: { color: colorMap[cat] },
  }))

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '10%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', boundaryGap: [0, 0.1] },
    yAxis: {
      type: 'category',
      data: categories,
      axisLabel: { fontSize: 13 },
    },
    series: [
      {
        type: 'bar',
        barWidth: 28,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}个',
          fontSize: 13,
          color: 'rgba(0,0,0,0.65)',
        },
        data: chartData,
      },
    ],
  }
}

// ============ Helper: format relative time ============
function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const diffDay = Math.floor(diffHour / 24)
  return `${diffDay}天前`
}

// ============ Helper: map audit module to activity category ============
function mapAuditToCategory(module: string): { category: string; tagColor: string; dotColor: string } {
  const map: Record<string, { category: string; tagColor: string; dotColor: string }> = {
    MODEL: { category: '模型', tagColor: 'geekblue', dotColor: '#2f54eb' },
    APPROVAL: { category: '审批', tagColor: 'purple', dotColor: '#722ed1' },
    EVALUATION: { category: '评估', tagColor: 'blue', dotColor: '#1677ff' },
    DEPLOYMENT: { category: '部署', tagColor: 'green', dotColor: '#52c41a' },
    ALERT: { category: '告警', tagColor: 'red', dotColor: '#ff4d4f' },
    ETL: { category: 'ETL', tagColor: 'cyan', dotColor: '#13c2c2' },
    DATA: { category: '数据', tagColor: 'orange', dotColor: '#fa8c16' },
  }
  return map[module] || { category: module || '其他', tagColor: 'default', dotColor: '#8c8c8c' }
}

// ============ Fetch All Dashboard Data ============
async function fetchDashboardData() {
  loading.value = true
  try {
    const [
      modelsRes,
      deploymentsRes,
      metricsRes,
      patientsRes,
      projectsRes,
      approvalsRes,
      alertsRes,
      auditRes,
      dsRes,
    ] = await Promise.all([
      getModels({ page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getDeployments({ page: 1, page_size: 1, status: 'RUNNING' }).catch(() => ({ data: { data: { total: 0 } } })),
      getMetricsOverview().catch(() => ({ data: { data: {} } })),
      getPatients({ page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getProjects({ page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getApprovals({ status: 'PENDING', page: 1, page_size: 1 }).catch(() => ({ data: { data: { total: 0 } } })),
      getAlerts({ page: 1, page_size: 4 }).catch(() => ({ data: { data: { items: [] } } })),
      getAuditLogs({ page: 1, page_size: 8 }).catch(() => ({ data: { data: { items: [] } } })),
      getDataSources({ page: 1, page_size: 10 }).catch(() => ({ data: { data: { items: [] } } })),
    ])

    // Metric cards
    modelCount.value = modelsRes.data.data.total || 0
    const depData = deploymentsRes.data.data
    activeDeployments.value = Array.isArray(depData) ? depData.length : (depData.total || 0)
    patientRecords.value = patientsRes.data.data.total || 0
    researchProjects.value = projectsRes.data.data.total || 0
    pendingApprovals.value = approvalsRes.data.data.total || 0

    // Daily inferences from metrics
    const metricsData = metricsRes.data.data
    dailyInferences.value = metricsData.todayInference || 0

    // Model status chart
    modelStatusOption.value = buildChartOption(metricsData)

    // Recent alerts
    const alertItems = alertsRes.data.data.items || []
    recentAlerts.value = alertItems.map((a: any) => ({
      severity: a.severity || 'WARNING',
      message: a.message || a.title || '',
      time: formatRelativeTime(a.triggeredAt || a.createdAt || a.created_at || ''),
    }))

    // Recent activities from audit logs
    const auditItems = auditRes.data.data.items || []
    recentActivities.value = auditItems.map((log: any) => {
      const { category, tagColor, dotColor } = mapAuditToCategory(log.module)
      return {
        text: log.description || log.operation || `${log.module} ${log.action || ''}`,
        category,
        tagColor,
        dotColor,
        time: formatRelativeTime(log.createdAt || log.created_at || ''),
      }
    })

    // Data sources
    const dsItems = dsRes.data.data.items || []
    dataSources.value = dsItems.map((ds: any) => ({
      name: ds.name || ds.source_name || '',
      description: ds.description || ds.source_type || '',
      connected: ds.status === 'CONNECTED' || ds.status === 'ACTIVE' || ds.connected === true,
    }))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
/* Welcome Banner */
.welcome-banner {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border-radius: 12px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}
.welcome-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.welcome-greeting {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: #fff;
}
.welcome-date {
  font-size: 14px;
  margin: 0;
  opacity: 0.85;
}
.welcome-tasks {
  font-size: 14px;
  margin: 0;
  margin-top: 4px;
  opacity: 0.9;
}
.task-count {
  font-size: 18px;
  font-weight: 600;
}
.welcome-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
.welcome-actions .ant-btn-background-ghost {
  color: #fff !important;
  border-color: rgba(255, 255, 255, 0.6) !important;
}
.welcome-actions .ant-btn-background-ghost:hover {
  border-color: #fff !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

/* View-all links */
.view-all-link {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
}
.view-all-link:hover {
  color: #4096ff;
}

/* Alert List */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.alert-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}
.alert-item:last-child {
  border-bottom: none;
}
.alert-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.alert-tag {
  flex-shrink: 0;
  font-weight: 500;
}
.alert-message {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alert-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  flex-shrink: 0;
  margin-left: 16px;
}

/* Activity Feed */
.activity-list {
  display: flex;
  flex-direction: column;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.activity-item:last-child {
  border-bottom: none;
}
.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.activity-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.75);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.activity-tag {
  flex-shrink: 0;
  border-radius: 4px;
}
.activity-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  flex-shrink: 0;
  margin-left: 8px;
}

/* Data Source Status */
.datasource-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.datasource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}
.datasource-item:last-child {
  border-bottom: none;
}
.datasource-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.datasource-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.datasource-desc {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
