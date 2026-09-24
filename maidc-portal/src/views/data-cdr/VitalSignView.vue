<template>
  <div class="vital-sign-view">
    <div v-loading="loading" class="min-h-[200px]">
      <!-- Latest Values Summary -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4 vital-summary">
        <MetricCard
          title="体温"
          :value="latestValues.temperature ?? '--'"
          suffix="°C"
          :icon="ThermometerIcon"
        />
        <MetricCard
          title="心率"
          :value="latestValues.heart_rate ?? '--'"
          suffix="bpm"
          :icon="HeartIcon"
        />
        <MetricCard
          title="血压"
          :value="latestValues.blood_pressure ?? '--'"
          suffix="mmHg"
          :icon="DashboardIcon"
        />
        <MetricCard
          title="血氧饱和度"
          :value="latestValues.spo2 ?? '--'"
          suffix="%"
          :icon="LungsIcon"
        />
      </div>

      <!-- Charts -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 vital-charts">
        <el-card shadow="never" size="small" class="chart-card !rounded-lg">
          <template #header>体温趋势</template>
          <MetricChart :option="temperatureChartOption" :height="chartHeight" />
        </el-card>
        <el-card shadow="never" size="small" class="chart-card !rounded-lg">
          <template #header>心率趋势</template>
          <MetricChart :option="heartRateChartOption" :height="chartHeight" />
        </el-card>
        <el-card shadow="never" size="small" class="chart-card !rounded-lg">
          <template #header>血压趋势</template>
          <MetricChart :option="bloodPressureChartOption" :height="chartHeight" />
        </el-card>
        <el-card shadow="never" size="small" class="chart-card !rounded-lg">
          <template #header>呼吸频率 & SpO2</template>
          <MetricChart :option="respAndSpo2ChartOption" :height="chartHeight" />
        </el-card>
      </div>

      <el-empty v-if="!loading && records.length === 0" description="暂无生命体征数据" :image-size="60" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { Odometer } from '@element-plus/icons-vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getVitalSigns } from '@/api/data'

defineOptions({ name: 'VitalSignView' })

interface Props {
  patientId: string
  encounterId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const records = ref<any[]>([])
const chartHeight = '280px'

// Custom icon components for MetricCard
const ThermometerIcon = () => h('span', { style: 'font-size: 28px' }, '🌡')
const HeartIcon = () => h('span', { style: 'font-size: 28px' }, '❤️')
const DashboardIcon = Odometer
const LungsIcon = () => h('span', { style: 'font-size: 28px' }, '🪤')

const latestValues = computed(() => {
  if (records.value.length === 0) {
    return { temperature: null, heart_rate: null, blood_pressure: null, spo2: null }
  }
  const latest = records.value[records.value.length - 1]
  return {
    temperature: latest.temperature,
    heart_rate: latest.heart_rate,
    blood_pressure: latest.blood_pressure
      ? `${latest.blood_pressure.systolic}/${latest.blood_pressure.diastolic}`
      : null,
    spo2: latest.spo2,
  }
})

const timeLabels = computed(() => records.value.map((r) => r.record_time))

// Temperature chart
const temperatureChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: '{b}<br/>体温: {c} °C',
  },
  grid: { left: 50, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: timeLabels.value,
    axisLabel: { fontSize: 11, rotate: 30 },
  },
  yAxis: {
    type: 'value',
    name: '°C',
    min: 35,
    max: 42,
    splitNumber: 7,
    axisLabel: { fontSize: 11 },
  },
  series: [{
    type: 'line',
    data: records.value.map((r) => r.temperature),
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { color: '#ef4444', width: 2 },
    itemStyle: { color: '#ef4444' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(239, 68, 68, 0.25)' },
          { offset: 1, color: 'rgba(239, 68, 68, 0.02)' },
        ],
      },
    },
    markLine: {
      silent: true,
      data: [
        { yAxis: 37.3, lineStyle: { color: '#f59e0b', type: 'dashed' }, label: { formatter: '低热 37.3' } },
        { yAxis: 38.0, lineStyle: { color: '#ef4444', type: 'dashed' }, label: { formatter: '发热 38.0' } },
      ],
    },
  }],
}))

// Heart rate chart
const heartRateChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: '{b}<br/>心率: {c} bpm',
  },
  grid: { left: 50, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: timeLabels.value,
    axisLabel: { fontSize: 11, rotate: 30 },
  },
  yAxis: {
    type: 'value',
    name: 'bpm',
    min: 40,
    max: 160,
    axisLabel: { fontSize: 11 },
  },
  series: [{
    type: 'line',
    data: records.value.map((r) => r.heart_rate),
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { color: '#0ea5e9', width: 2 },
    itemStyle: { color: '#0ea5e9' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(14, 165, 233, 0.2)' },
          { offset: 1, color: 'rgba(14, 165, 233, 0.02)' },
        ],
      },
    },
  }],
}))

// Blood pressure chart (systolic + diastolic)
const bloodPressureChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data: ['收缩压', '舒张压'],
    bottom: 0,
    textStyle: { fontSize: 12 },
  },
  grid: { left: 50, right: 20, top: 20, bottom: 40 },
  xAxis: {
    type: 'category',
    data: timeLabels.value,
    axisLabel: { fontSize: 11, rotate: 30 },
  },
  yAxis: {
    type: 'value',
    name: 'mmHg',
    min: 40,
    max: 200,
    axisLabel: { fontSize: 11 },
  },
  series: [
    {
      name: '收缩压',
      type: 'line',
      data: records.value.map((r) => r.blood_pressure?.systolic),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#ef4444', width: 2 },
      itemStyle: { color: '#ef4444' },
    },
    {
      name: '舒张压',
      type: 'line',
      data: records.value.map((r) => r.blood_pressure?.diastolic),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#0ea5e9', width: 2 },
      itemStyle: { color: '#0ea5e9' },
    },
  ],
}))

// Respiratory rate + SpO2 combined chart
const respAndSpo2ChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
  },
  legend: {
    data: ['呼吸频率', 'SpO2'],
    bottom: 0,
    textStyle: { fontSize: 12 },
  },
  grid: { left: 50, right: 50, top: 20, bottom: 40 },
  xAxis: {
    type: 'category',
    data: timeLabels.value,
    axisLabel: { fontSize: 11, rotate: 30 },
  },
  yAxis: [
    {
      type: 'value',
      name: '次/分',
      min: 8,
      max: 40,
      axisLabel: { fontSize: 11 },
    },
    {
      type: 'value',
      name: '%',
      min: 80,
      max: 100,
      axisLabel: { fontSize: 11 },
    },
  ],
  series: [
    {
      name: '呼吸频率',
      type: 'line',
      data: records.value.map((r) => r.respiratory_rate),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#10b981', width: 2 },
      itemStyle: { color: '#10b981' },
    },
    {
      name: 'SpO2',
      type: 'line',
      yAxisIndex: 1,
      data: records.value.map((r) => r.spo2),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#8b5cf6', width: 2 },
      itemStyle: { color: '#8b5cf6' },
    },
  ],
}))

async function loadData() {
  loading.value = true
  try {
    const res = await getVitalSigns(props.patientId, props.encounterId)
    records.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.vital-sign-view {
  padding-top: 8px;
}
.vital-summary :deep(.metric-card) {
  height: 100%;
}
.chart-card :deep(.el-card__header) {
  padding: 10px 16px;
  font-size: 14px;
  font-weight: 500;
}
</style>
