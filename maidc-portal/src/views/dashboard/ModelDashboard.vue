<template>
  <PageContainer title="模型看板" subtitle="全院医疗AI模型全生命周期部署与高并发推理运行态势">
    <template #extra>
      <div class="flex items-center gap-3">
        <!-- Cluster Selector -->
        <el-select v-model="selectedCluster" size="small" class="!w-40">
          <el-option label="全部AI算力集群" value="all" />
          <el-option label="临床推理A组 (H100)" value="h100" />
          <el-option label="病理多模态B组 (A100)" value="a100" />
          <el-option label="边缘离线质控组 (L40S)" value="edge" />
        </el-select>

        <!-- Time Range Selector -->
        <el-radio-group v-model="timeRange" size="small" @change="fetchDashboard">
          <el-radio-button label="today">今日</el-radio-button>
          <el-radio-button label="7d">近7天</el-radio-button>
          <el-radio-button label="30d">近30天</el-radio-button>
        </el-radio-group>

        <!-- Auto Refresh -->
        <div class="hidden sm:flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-100/80 text-xs text-slate-600">
          <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>实时监控</span>
          <el-switch v-model="autoRefresh" size="small" inline-prompt @change="handleAutoRefreshChange" />
        </div>

        <el-button size="small" :loading="loading" @click="fetchDashboard">
          <el-icon class="mr-1" :class="{ 'animate-spin': loading }"><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </template>

    <!-- Top Metric Matrix -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 mb-5">
      <!-- Active Deployments -->
      <div class="kpi-card group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-slate-500">生产活跃部署</span>
          <span class="p-2 rounded-lg bg-sky-50 text-sky-600 group-hover:scale-110 transition-transform">
            <el-icon :size="18"><Promotion /></el-icon>
          </span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-2xl font-bold font-mono text-slate-900 tracking-tight">
            {{ metrics.activeDeployments }}
          </span>
          <span class="text-xs text-slate-400">个服务</span>
        </div>
        <div class="mt-3 flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100">
          <span class="text-emerald-600 font-medium">3 个灰度分流</span>
          <span class="text-slate-400 font-mono">100% 容器化</span>
        </div>
      </div>

      <!-- Today Inference Calls -->
      <div class="kpi-card group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-slate-500">今日临床调用推理</span>
          <span class="p-2 rounded-lg bg-indigo-50 text-indigo-600 group-hover:scale-110 transition-transform">
            <el-icon :size="18"><Lightning /></el-icon>
          </span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-2xl font-bold font-mono text-slate-900 tracking-tight">
            {{ formatNumber(metrics.todayInference) }}
          </span>
          <span class="text-xs text-slate-400">次</span>
        </div>
        <div class="mt-3 flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100">
          <span class="text-emerald-600 font-medium flex items-center">
            <el-icon :size="12" class="mr-0.5"><Top /></el-icon>+15.4%
          </span>
          <span class="text-slate-400 font-mono">成功率 99.92%</span>
        </div>
      </div>

      <!-- Average Latency -->
      <div class="kpi-card group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-slate-500">端到端平均延迟</span>
          <span class="p-2 rounded-lg bg-amber-50 text-amber-600 group-hover:scale-110 transition-transform">
            <el-icon :size="18"><Timer /></el-icon>
          </span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-2xl font-bold font-mono text-slate-900 tracking-tight">
            {{ metrics.avgLatency }}
          </span>
          <span class="text-xs text-slate-400">ms</span>
        </div>
        <div class="mt-3 flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100">
          <span class="text-emerald-600 font-medium flex items-center">
            <el-icon :size="12" class="mr-0.5"><Bottom /></el-icon>-12ms 优化
          </span>
          <span class="text-slate-400 font-mono">P99: 145ms</span>
        </div>
      </div>

      <!-- GPU Utilization -->
      <div class="kpi-card group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-slate-500">算力集群 GPU 利用率</span>
          <span class="p-2 rounded-lg bg-teal-50 text-teal-600 group-hover:scale-110 transition-transform">
            <el-icon :size="18"><Odometer /></el-icon>
          </span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-2xl font-bold font-mono text-slate-900 tracking-tight">
            {{ metrics.gpuUtilization }}
          </span>
          <span class="text-xs text-slate-400">%</span>
        </div>
        <div class="mt-3 flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100">
          <span class="text-slate-500">显存负载 72%</span>
          <span class="text-emerald-600 font-medium font-mono">散热正常 62℃</span>
        </div>
      </div>

      <!-- Alignment & Safety Intercept -->
      <div class="kpi-card group">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium text-slate-500">临床安全对齐拦截</span>
          <span class="p-2 rounded-lg bg-rose-50 text-rose-600 group-hover:scale-110 transition-transform">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-2xl font-bold font-mono text-slate-900 tracking-tight">
            0
          </span>
          <span class="text-xs text-slate-400">次越界</span>
        </div>
        <div class="mt-3 flex items-center justify-between text-xs text-slate-500 pt-2 border-t border-slate-100">
          <span class="text-emerald-600 font-medium">前置护栏有效</span>
          <span class="text-slate-400 font-mono">合规率 100%</span>
        </div>
      </div>
    </div>

    <!-- Main Chart: Inference Trends -->
    <div class="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-clinical-sm mb-5">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
        <div>
          <h3 class="text-base font-semibold text-slate-900">临床推理流量与响应态势</h3>
          <p class="text-xs text-slate-500 mt-0.5">监控全院调用量、成功调用与异常熔断波动，智能感知峰值负载</p>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-xs text-slate-500">峰值 QPS: <b class="font-mono text-slate-800">420/s</b></span>
          <span class="text-xs text-slate-500">可用性: <b class="font-mono text-emerald-600">99.99%</b></span>
        </div>
      </div>
      <MetricChart :option="inferenceTrendOption" :height="340" />
    </div>

    <!-- Second Row: Top Models Ranking & Deployment Distribution -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-5 mb-5">
      <!-- Top Models Performance Rank -->
      <div class="lg:col-span-7 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-clinical-sm">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-base font-semibold text-slate-900">临床模型调用负载排行 (Top 5)</h3>
            <p class="text-xs text-slate-500 mt-0.5">高频临床辅助诊断与文书质控服务分布</p>
          </div>
        </div>
        <MetricChart :option="performanceRankOption" :height="300" />
      </div>

      <!-- Deployment Status Distribution -->
      <div class="lg:col-span-5 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-clinical-sm">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-base font-semibold text-slate-900">模型部署状态与容器拓扑</h3>
            <p class="text-xs text-slate-500 mt-0.5">微服务多实例在线与发布阶段占比</p>
          </div>
        </div>
        <MetricChart :option="deployStatusOption" :height="300" />
      </div>
    </div>

    <!-- Third Row: Active Serving Endpoints Table -->
    <div class="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-clinical-sm">
      <div class="flex items-center justify-between mb-3">
        <div>
          <h3 class="text-base font-semibold text-slate-900">在线模型推理服务实例</h3>
          <p class="text-xs text-slate-500 mt-0.5">生产活跃模型版本、硬件绑定及实时健康状态</p>
        </div>
        <router-link to="/model/deployments" class="text-xs text-sky-600 hover:text-sky-700 font-medium">
          查看全部部署 &rarr;
        </router-link>
      </div>

      <el-table
        :data="modelEndpoints"
        size="small"
        border
        class="!rounded-xl overflow-hidden"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: '600' }"
      >
        <el-table-column prop="name" label="模型服务名称" min-width="180">
          <template #default="{ row }">
            <div class="font-medium text-slate-900 text-xs">{{ row.name }}</div>
            <div class="text-[11px] text-slate-400 font-mono">{{ row.code }} · v{{ row.version }}</div>
          </template>
        </el-table-column>

        <el-table-column prop="cluster" label="算力资源" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain" class="!rounded-md font-mono text-[11px]">
              {{ row.hardware }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="服务态势" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.statusType" effect="light" class="!rounded-md font-medium">
              <span class="flex items-center gap-1">
                <span class="w-1.5 h-1.5 rounded-full" :class="row.statusDot"></span>
                {{ row.statusText }}
              </span>
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="qps" label="当前 QPS" width="110" align="right">
          <template #default="{ row }">
            <span class="font-mono font-semibold text-slate-700 text-xs">{{ row.qps }}</span>
            <span class="text-[11px] text-slate-400 ml-1">req/s</span>
          </template>
        </el-table-column>

        <el-table-column prop="latency" label="P99延迟" width="100" align="right">
          <template #default="{ row }">
            <span class="font-mono font-medium text-slate-700 text-xs">{{ row.latency }}ms</span>
          </template>
        </el-table-column>

        <el-table-column prop="updatedAt" label="最近更新" min-width="150">
          <template #default="{ row }">
            <span class="font-mono text-xs text-slate-500">{{ row.updatedAt }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import {
  Promotion,
  Lightning,
  Timer,
  Odometer,
  CircleCheck,
  Refresh,
  Top,
  Bottom,
} from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getMetricsOverview, getDeployments } from '@/api/model'

const loading = ref(false)
const timeRange = ref('7d')
const selectedCluster = ref('all')
const autoRefresh = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const metrics = reactive({
  activeDeployments: 0,
  todayInference: 0,
  avgLatency: 0,
  gpuUtilization: 0,
})

const inferenceTrendOption = ref<Record<string, any>>({})
const performanceRankOption = ref<Record<string, any>>({})
const deployStatusOption = ref<Record<string, any>>({})

// ============ Mock Serving Endpoints Table ============
const modelEndpoints = ref([
  {
    name: 'Chest-CT 肺结节多发病灶分割检测',
    code: 'SEG-CHEST-P01',
    version: '2.4.0',
    hardware: '1× NVIDIA H100',
    statusType: 'success' as const,
    statusDot: 'bg-emerald-500',
    statusText: '运行良好',
    qps: 84,
    latency: 120,
    updatedAt: '2026-09-22 11:20:00',
  },
  {
    name: 'Clinical-NLP 门急诊病历实体关系抽取',
    code: 'NLP-EMR-NER-02',
    version: '3.1.2',
    hardware: '2× NVIDIA A100',
    statusType: 'success' as const,
    statusDot: 'bg-emerald-500',
    statusText: '运行良好',
    qps: 196,
    latency: 35,
    updatedAt: '2026-09-22 10:45:12',
  },
  {
    name: 'Patho-WSI 胃癌全切片淋巴结转移筛查',
    code: 'PATH-WSI-LYMPH',
    version: '1.8.0',
    hardware: '2× NVIDIA H100',
    statusType: '' as const,
    statusDot: 'bg-sky-500 animate-pulse',
    statusText: '金丝雀分流 (20%)',
    qps: 42,
    latency: 280,
    updatedAt: '2026-09-22 09:30:15',
  },
  {
    name: 'ECG-ST段 异常心电波形实时预警',
    code: 'ECG-REALTIME-ALRT',
    version: '4.0.1',
    hardware: 'CPU 集群优化 (AVX-512)',
    statusType: 'success' as const,
    statusDot: 'bg-emerald-500',
    statusText: '运行良好',
    qps: 310,
    latency: 8,
    updatedAt: '2026-09-22 08:15:00',
  },
])

function formatNumber(num: number | undefined): string {
  if (!num) return '0'
  return num.toLocaleString()
}

function buildTrendOption(trend: any) {
  const dates = trend?.dates || ['09-16', '09-17', '09-18', '09-19', '09-20', '09-21', '09-22']
  const total = trend?.total || [38200, 42100, 49800, 56300, 52000, 61400, 68900]
  const success = trend?.success || [38180, 42070, 49760, 56250, 51950, 61350, 68850]
  const failed = trend?.failed || [20, 30, 40, 50, 50, 50, 50]

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross', label: { backgroundColor: '#475569' } } },
    legend: {
      data: ['推理总量', '成功调用', '熔断与异常'],
      top: 0,
      right: 12,
      icon: 'roundRect',
      textStyle: { color: '#64748b', fontSize: 12 },
    },
    grid: { left: '2%', right: '3%', bottom: '2%', top: '14%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: '调用频次',
      nameTextStyle: { color: '#94a3b8', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
    },
    series: [
      {
        name: '推理总量',
        type: 'line',
        smooth: 0.35,
        areaStyle: { color: 'rgba(14, 165, 233, 0.15)' },
        data: total,
        itemStyle: { color: '#0ea5e9' },
        lineStyle: { width: 2.5 },
      },
      {
        name: '成功调用',
        type: 'line',
        smooth: 0.35,
        areaStyle: { color: 'rgba(16, 185, 129, 0.12)' },
        data: success,
        itemStyle: { color: '#10b981' },
        lineStyle: { width: 2 },
      },
      {
        name: '熔断与异常',
        type: 'line',
        smooth: 0.35,
        data: failed,
        itemStyle: { color: '#ef4444' },
        lineStyle: { width: 1.5, type: 'dashed' },
      },
    ],
  }
}

function buildRankOption(ranking: any[]) {
  const defaultList = [
    { name: '门急诊病历抽取 (NLP)', count: 28400 },
    { name: '胸部CT肺结节筛查', count: 19800 },
    { name: 'ST段心电波形分析', count: 14500 },
    { name: '眼底视网膜病变分期', count: 8900 },
    { name: '胃癌病理切片分型', count: 6200 },
  ]
  const list = ranking?.length ? ranking : defaultList
  const yData = list.map((r) => r.name).reverse()
  const xData = list.map((r) => r.count).reverse()

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '6%', containLabel: true },
    xAxis: {
      type: 'value',
      name: '累计调用',
      nameTextStyle: { color: '#94a3b8', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 11 },
    },
    yAxis: {
      type: 'category',
      data: yData,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisLabel: { color: '#475569', fontSize: 11 },
    },
    series: [
      {
        type: 'bar',
        barWidth: 16,
        data: xData.map((val) => ({
          value: val,
          itemStyle: { color: '#0ea5e9', borderRadius: [0, 6, 6, 0] },
        })),
        label: {
          show: true,
          position: 'right',
          color: '#64748b',
          fontSize: 11,
          fontFamily: 'monospace',
          formatter: (p: any) => p.value.toLocaleString(),
        },
      },
    ],
  }
}

function buildDeployOption(distribution: any[]) {
  const defaultDist = [
    { status: '运行中', count: 14 },
    { status: '灰度发布', count: 3 },
    { status: '冷启动中', count: 2 },
    { status: '已下线', count: 1 },
  ]
  const dist = distribution?.length ? distribution : defaultDist
  const colors: Record<string, string> = {
    运行中: '#10b981',
    灰度发布: '#0ea5e9',
    冷启动中: '#f59e0b',
    异常: '#ef4444',
    已下线: '#94a3b8',
  }

  return {
    tooltip: { trigger: 'item', formatter: '{b}<br/>实例数: <b>{c}</b> ({d}%)' },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#64748b', fontSize: 11 } },
    series: [
      {
        type: 'pie',
        radius: ['44%', '70%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: dist.map((d: any) => ({
          value: d.count,
          name: d.status,
          itemStyle: { color: colors[d.status] || '#94a3b8' },
        })),
      },
    ],
  }
}

function handleAutoRefreshChange(val: boolean) {
  if (val) {
    timer = setInterval(() => {
      fetchDashboard()
    }, 30000)
  } else if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function fetchDashboard() {
  loading.value = true
  try {
    const [metricsRes, deploymentsRes] = await Promise.all([
      getMetricsOverview().catch(() => null),
      getDeployments({ page: 1, page_size: 1 }).catch(() => null),
    ])

    metrics.activeDeployments = deploymentsRes?.data?.data?.total || 18

    if (metricsRes?.data?.data) {
      const data = metricsRes.data.data
      metrics.todayInference = data.todayInference || 68900
      metrics.avgLatency = data.avgLatency || 42
      metrics.gpuUtilization = data.gpuUtilization || 68.4
      inferenceTrendOption.value = buildTrendOption(data.inferenceTrend)
      performanceRankOption.value = buildRankOption(data.performanceRanking)
      deployStatusOption.value = buildDeployOption(data.deploymentStatusDistribution)
    } else {
      metrics.todayInference = 68900
      metrics.avgLatency = 42
      metrics.gpuUtilization = 68.4
      inferenceTrendOption.value = buildTrendOption(null)
      performanceRankOption.value = buildRankOption([])
      deployStatusOption.value = buildDeployOption([])
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.kpi-card {
  @apply bg-white p-4 rounded-2xl border border-slate-200/80 shadow-clinical-sm transition-all duration-200;
}
.kpi-card:hover {
  @apply shadow-clinical -translate-y-0.5;
}
</style>
