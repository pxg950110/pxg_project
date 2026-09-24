<template>
  <div class="overview-container space-y-4 max-w-[1600px] mx-auto" v-loading="loading">
    <!-- 顶部欢迎横幅 -->
    <div class="relative overflow-hidden rounded-xl bg-gradient-to-r from-sky-600 via-sky-700 to-slate-900 p-6 text-white shadow-clinical flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="absolute inset-0 opacity-10 pointer-events-none bg-[radial-gradient(#fff_1px,transparent_1px)] [background-size:16px_16px]" />

      <div class="relative z-10 space-y-1.5">
        <h2 class="text-xl font-bold tracking-tight text-white m-0">
          {{ greeting }}，{{ userName }}
        </h2>
        <p class="text-xs text-sky-100/80 m-0">
          今天是 {{ currentDate }} · 医疗 AI 数据中心各核心服务平稳运行中
        </p>
        <p class="text-xs text-sky-100/90 m-0 pt-0.5">
          您当前有 <span class="font-bold text-amber-300 font-mono text-sm">{{ pendingApprovals }}</span> 项待处理审批与业务任务
        </p>
      </div>

      <div class="relative z-10 flex items-center gap-2.5 flex-wrap">
        <el-button
          type="primary"
          class="!bg-white/15 hover:!bg-white/25 !text-white !border-white/30 backdrop-blur-sm"
          @click="$router.push('/model/list')"
        >
          <el-icon class="mr-1"><Plus /></el-icon>
          注册模型
        </el-button>
        <el-button
          type="primary"
          class="!bg-white/15 hover:!bg-white/25 !text-white !border-white/30 backdrop-blur-sm"
          @click="$router.push('/model/evaluations')"
        >
          <el-icon class="mr-1"><TrendCharts /></el-icon>
          新建评估
        </el-button>
        <el-button
          type="primary"
          class="!bg-white/15 hover:!bg-white/25 !text-white !border-white/30 backdrop-blur-sm"
          @click="$router.push('/model/deployments')"
        >
          <el-icon class="mr-1"><Tickets /></el-icon>
          提交审批
        </el-button>
      </div>
    </div>

    <!-- 6 列/2行 核心指标卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical transition-all">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">模型总数</span>
          <div class="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
            <el-icon :size="16"><Cpu /></el-icon>
          </div>
        </div>
        <div class="mt-2 flex items-baseline gap-1">
          <span class="text-2xl font-bold font-mono text-slate-900">{{ modelCount }}</span>
          <span class="text-xs text-slate-400">个</span>
        </div>
        <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
          <span>周环比</span>
          <span class="text-emerald-600 font-semibold flex items-center">↑ 12%</span>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical transition-all">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">活跃部署</span>
          <div class="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <el-icon :size="16"><CircleCheck /></el-icon>
          </div>
        </div>
        <div class="mt-2 flex items-baseline gap-1">
          <span class="text-2xl font-bold font-mono text-slate-900">{{ activeDeployments }}</span>
          <span class="text-xs text-slate-400">个</span>
        </div>
        <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
          <span>服务状态</span>
          <span class="text-emerald-600 font-medium">健康在线</span>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical transition-all">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">今日推理调用</span>
          <div class="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
            <el-icon :size="16"><TrendCharts /></el-icon>
          </div>
        </div>
        <div class="mt-2 flex items-baseline gap-1">
          <span class="text-2xl font-bold font-mono text-slate-900">{{ dailyInferences }}</span>
          <span class="text-xs text-slate-400">次</span>
        </div>
        <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
          <span>推理吞吐</span>
          <span class="text-sky-600 font-semibold flex items-center">↑ 8% 今日峰值</span>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical transition-all">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">在管患者记录</span>
          <div class="w-8 h-8 rounded-lg bg-violet-50 text-violet-600 flex items-center justify-center">
            <el-icon :size="16"><User /></el-icon>
          </div>
        </div>
        <div class="mt-2 flex items-baseline gap-1">
          <span class="text-2xl font-bold font-mono text-slate-900">{{ patientRecords }}</span>
          <span class="text-xs text-slate-400">人</span>
        </div>
        <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
          <span>数据接入</span>
          <span class="text-emerald-600 font-semibold">CDR 实时入库</span>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical transition-all">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">临床科研项目</span>
          <div class="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <el-icon :size="16"><Document /></el-icon>
          </div>
        </div>
        <div class="mt-2 flex items-baseline gap-1">
          <span class="text-2xl font-bold font-mono text-slate-900">{{ researchProjects }}</span>
          <span class="text-xs text-slate-400">项</span>
        </div>
        <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
          <span>专病队列</span>
          <span class="text-slate-600 font-medium">多中心协同</span>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical transition-all">
        <div class="flex items-center justify-between">
          <span class="text-xs font-medium text-slate-500">待审批事务</span>
          <div class="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
            <el-icon :size="16"><WarningFilled /></el-icon>
          </div>
        </div>
        <div class="mt-2 flex items-baseline gap-1">
          <span class="text-2xl font-bold font-mono text-slate-900">{{ pendingApprovals }}</span>
          <span class="text-xs text-slate-400">项</span>
        </div>
        <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
          <span>优先级</span>
          <span class="text-amber-600 font-semibold">伦理与上线复审</span>
        </div>
      </div>
    </div>

    <!-- 中部图表与告警区：左侧模型状态分布，右侧最近告警 -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-4">
      <div class="lg:col-span-6 bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-col">
        <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
          <div class="flex items-center gap-2">
            <span class="w-1 h-3.5 bg-sky-500 rounded-full" />
            <h3 class="text-sm font-semibold text-slate-900 m-0">模型状态生命周期分布</h3>
          </div>
          <el-button link type="primary" class="!text-xs" @click="$router.push('/model/list')">查看列表</el-button>
        </div>
        <div ref="modelChartRef" class="w-full h-72" />
      </div>

      <div class="lg:col-span-6 bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-col">
        <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
          <div class="flex items-center gap-2">
            <span class="w-1 h-3.5 bg-rose-500 rounded-full" />
            <h3 class="text-sm font-semibold text-slate-900 m-0">实时告警与危急监控</h3>
          </div>
          <el-button link type="primary" class="!text-xs" @click="$router.push('/alert/active')">查看全部</el-button>
        </div>
        <div class="flex-1 space-y-2.5 overflow-y-auto">
          <div v-if="recentAlerts.length === 0" class="py-12 text-center text-slate-400 text-xs">
            暂无活跃系统告警
          </div>
          <div
            v-for="(alert, index) in recentAlerts"
            :key="index"
            class="p-2.5 rounded-lg border border-slate-100 hover:bg-slate-50 transition-colors flex items-center justify-between gap-3"
          >
            <div class="flex items-center gap-2.5 min-w-0">
              <span
                class="px-2 py-0.5 rounded text-[11px] font-bold flex-shrink-0"
                :class="alert.severity === 'CRITICAL' ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-700'"
              >
                {{ alert.severity }}
              </span>
              <span class="text-xs text-slate-700 truncate font-medium">{{ alert.message }}</span>
            </div>
            <span class="text-[11px] text-slate-400 flex-shrink-0">{{ alert.time }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：左侧操作审计日志，右侧数据源健康状态 -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-4">
      <div class="lg:col-span-8 bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm">
        <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
          <div class="flex items-center gap-2">
            <span class="w-1 h-3.5 bg-indigo-500 rounded-full" />
            <h3 class="text-sm font-semibold text-slate-900 m-0">最近操作与审计轨迹</h3>
          </div>
          <el-button link type="primary" class="!text-xs" @click="$router.push('/audit/operations')">更多日志</el-button>
        </div>
        <div class="space-y-2">
          <div
            v-for="(item, index) in recentActivities"
            :key="index"
            class="py-1.5 px-2 flex items-center justify-between text-xs hover:bg-slate-50 rounded-md transition-colors"
          >
            <div class="flex items-center gap-2.5 min-w-0">
              <span class="w-1.5 h-1.5 rounded-full flex-shrink-0" :style="{ backgroundColor: item.dotColor }" />
              <span class="px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 flex-shrink-0">
                {{ item.category }}
              </span>
              <span class="text-slate-700 truncate">{{ item.text }}</span>
            </div>
            <span class="text-[11px] text-slate-400 flex-shrink-0">{{ item.time }}</span>
          </div>
        </div>
      </div>

      <div class="lg:col-span-4 bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm">
        <div class="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
          <div class="flex items-center gap-2">
            <span class="w-1 h-3.5 bg-emerald-500 rounded-full" />
            <h3 class="text-sm font-semibold text-slate-900 m-0">数据源连接状态</h3>
          </div>
          <el-button link type="primary" class="!text-xs" @click="$router.push('/etl/datasources')">管理数据源</el-button>
        </div>
        <div class="space-y-2">
          <div
            v-for="(ds, index) in dataSources"
            :key="index"
            class="p-2 rounded-lg border border-slate-100 hover:bg-slate-50 transition-colors flex items-center justify-between"
          >
            <div class="min-w-0">
              <div class="text-xs font-semibold text-slate-800 truncate">{{ ds.name }}</div>
              <div class="text-[11px] text-slate-400 truncate">{{ ds.description || '医疗数据源接口' }}</div>
            </div>
            <span
              class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium"
              :class="ds.connected ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'"
            >
              <span class="w-1.5 h-1.5 rounded-full" :class="ds.connected ? 'bg-emerald-500' : 'bg-rose-500'" />
              {{ ds.connected ? '已连接' : '断开' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Plus,
  TrendCharts,
  Tickets,
  Cpu,
  CircleCheck,
  User,
  Document,
  WarningFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getModels, getDeployments, getApprovals, getAlerts, getMetricsOverview } from '@/api/model'
import { getAuditLogs } from '@/api/audit'
import { getDataSources, getPatients, getProjects } from '@/api/data'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)

// 指标统计
const modelCount = ref(0)
const activeDeployments = ref(0)
const dailyInferences = ref(0)
const patientRecords = ref(0)
const researchProjects = ref(0)
const pendingApprovals = ref(0)

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

const recentAlerts = ref<any[]>([])
const recentActivities = ref<any[]>([])
const dataSources = ref<any[]>([])

const modelChartRef = ref<HTMLElement>()
let modelChartInstance: echarts.ECharts | null = null

function renderModelStatusChart(distribution: Record<string, number> = {}) {
  if (!modelChartRef.value) return
  if (!modelChartInstance) {
    modelChartInstance = echarts.init(modelChartRef.value)
  }

  const categories = ['DRAFT', 'REGISTERED', 'PUBLISHED', 'DEPRECATED']
  const labelMap: Record<string, string> = {
    DRAFT: '草稿',
    REGISTERED: '已注册',
    PUBLISHED: '已发布',
    DEPRECATED: '已废弃',
  }
  const colorMap: Record<string, string> = {
    DRAFT: '#94A3B8',
    REGISTERED: '#0EA5E9',
    PUBLISHED: '#10B981',
    DEPRECATED: '#F59E0B',
  }

  const chartData = categories.map((cat) => ({
    value: distribution[cat] || 0,
    itemStyle: { color: colorMap[cat] },
  }))

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const item = params[0]
        return `${labelMap[item.name] || item.name}: <b>${item.value}</b> 个`
      },
    },
    grid: { left: '3%', right: '12%', bottom: '5%', top: '5%', containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      axisLabel: { color: '#64748B', fontSize: 11 },
    },
    yAxis: {
      type: 'category',
      data: categories.map((c) => labelMap[c] || c),
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: { color: '#334155', fontSize: 12, fontWeight: 500 },
    },
    series: [
      {
        type: 'bar',
        barWidth: 20,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}个',
          fontSize: 12,
          color: '#64748B',
          fontWeight: 600,
        },
        data: chartData,
      },
    ],
  }

  modelChartInstance.setOption(option, true)
}

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

function mapAuditToCategory(module: string): { category: string; tagColor: string; dotColor: string } {
  const map: Record<string, { category: string; tagColor: string; dotColor: string }> = {
    MODEL: { category: '模型', tagColor: 'sky', dotColor: '#0EA5E9' },
    APPROVAL: { category: '审批', tagColor: 'violet', dotColor: '#8B5CF6' },
    EVALUATION: { category: '评估', tagColor: 'sky', dotColor: '#0284C7' },
    DEPLOYMENT: { category: '部署', tagColor: 'emerald', dotColor: '#10B981' },
    ALERT: { category: '告警', tagColor: 'rose', dotColor: '#EF4444' },
    ETL: { category: 'ETL', tagColor: 'cyan', dotColor: '#06B6D4' },
    DATA: { category: '数据', tagColor: 'amber', dotColor: '#F59E0B' },
  }
  return map[module] || { category: module || '其他', tagColor: 'slate', dotColor: '#94A3B8' }
}

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
      getAuditLogs({ page: 1, pageSize: 8 }).catch(() => ({ data: { data: { items: [] } } })),
      getDataSources({ page: 1, page_size: 10 }).catch(() => ({ data: { data: { items: [] } } })),
    ])

    modelCount.value = modelsRes.data.data.total || 0
    const depData = deploymentsRes.data.data
    activeDeployments.value = Array.isArray(depData) ? depData.length : (depData.total || 0)
    patientRecords.value = patientsRes.data.data.total || 0
    researchProjects.value = projectsRes.data.data.total || 0
    pendingApprovals.value = approvalsRes.data.data.total || 0

    const metricsData = metricsRes.data.data
    dailyInferences.value = metricsData.todayInference || 0

    renderModelStatusChart(metricsData.modelStatusDistribution || {})

    const alertItems = alertsRes.data.data.items || []
    recentAlerts.value = alertItems.map((a: any) => ({
      severity: a.severity || 'WARNING',
      message: a.message || a.title || '',
      time: formatRelativeTime(a.triggeredAt || a.createdAt || a.created_at || ''),
    }))

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

const handleResize = () => {
  modelChartInstance?.resize()
}

onMounted(() => {
  fetchDashboardData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  modelChartInstance?.dispose()
  modelChartInstance = null
})
</script>
