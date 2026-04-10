<template>
  <div class="vital-sign-view">
    <a-spin :spinning="loading">
      <!-- Latest Values Summary -->
      <a-row :gutter="16" class="vital-summary">
        <a-col :xs="12" :sm="12" :md="6">
          <MetricCard
            title="体温"
            :value="latestValues.temperature ?? '--'"
            suffix="°C"
            :icon="ThermometerIcon"
          />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <MetricCard
            title="心率"
            :value="latestValues.heart_rate ?? '--'"
            suffix="bpm"
            :icon="HeartIcon"
          />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <MetricCard
            title="血压"
            :value="latestValues.blood_pressure ?? '--'"
            suffix="mmHg"
            :icon="DashboardIcon"
          />
        </a-col>
        <a-col :xs="12" :sm="12" :md="6">
          <MetricCard
            title="血氧饱和度"
            :value="latestValues.spo2 ?? '--'"
            suffix="%"
            :icon="LungsIcon"
          />
        </a-col>
      </a-row>

      <!-- Charts -->
      <a-row :gutter="[16, 16]" class="vital-charts">
        <a-col :xs="24" :lg="12">
          <a-card title="体温趋势" :bordered="false" size="small" class="chart-card">
            <MetricChart :option="temperatureChartOption" :height="chartHeight" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="心率趋势" :bordered="false" size="small" class="chart-card">
            <MetricChart :option="heartRateChartOption" :height="chartHeight" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="血压趋势" :bordered="false" size="small" class="chart-card">
            <MetricChart :option="bloodPressureChartOption" :height="chartHeight" />
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-card title="呼吸频率 & SpO2" :bordered="false" size="small" class="chart-card">
            <MetricChart :option="respAndSpo2ChartOption" :height="chartHeight" />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>

    <a-empty v-if="!loading && records.length === 0" description="暂无生命体征数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import {
  HeartOutlined,
  DashboardOutlined,
} from '@ant-design/icons-vue'
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
const ThermometerIcon = () => h('span', { style: 'font-size: 28px' }, '\uD83C\uDF21')
const HeartIcon = HeartOutlined
const DashboardIcon = DashboardOutlined
const LungsIcon = () => h('span', { style: 'font-size: 28px' }, '\uD83E\uDEA4')

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
    lineStyle: { color: '#ff4d4f', width: 2 },
    itemStyle: { color: '#ff4d4f' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(255, 77, 79, 0.25)' },
          { offset: 1, color: 'rgba(255, 77, 79, 0.02)' },
        ],
      },
    },
    markLine: {
      silent: true,
      data: [
        { yAxis: 37.3, lineStyle: { color: '#faad14', type: 'dashed' }, label: { formatter: '低热 37.3' } },
        { yAxis: 38.0, lineStyle: { color: '#ff4d4f', type: 'dashed' }, label: { formatter: '发热 38.0' } },
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
    lineStyle: { color: '#1677ff', width: 2 },
    itemStyle: { color: '#1677ff' },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(22, 119, 255, 0.2)' },
          { offset: 1, color: 'rgba(22, 119, 255, 0.02)' },
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
      lineStyle: { color: '#ff4d4f', width: 2 },
      itemStyle: { color: '#ff4d4f' },
    },
    {
      name: '舒张压',
      type: 'line',
      data: records.value.map((r) => r.blood_pressure?.diastolic),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#1677ff', width: 2 },
      itemStyle: { color: '#1677ff' },
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
      lineStyle: { color: '#52c41a', width: 2 },
      itemStyle: { color: '#52c41a' },
    },
    {
      name: 'SpO2',
      type: 'line',
      yAxisIndex: 1,
      data: records.value.map((r) => r.spo2),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#722ed1', width: 2 },
      itemStyle: { color: '#722ed1' },
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
.vital-summary {
  margin-bottom: 16px;
}
.vital-summary :deep(.metric-card) {
  height: 100%;
}
.chart-card {
  border-radius: 8px;
}
.chart-card :deep(.ant-card-head) {
  min-height: 40px;
  padding: 0 16px;
}
.chart-card :deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 500;
  padding: 10px 0;
}
</style>
