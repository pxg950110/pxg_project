<template>
  <div class="clinical-trend-chart relative w-full bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm">
    <div v-if="title || $slots.headerRight" class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <span class="w-1 h-3.5 bg-sky-500 rounded-full" />
        <h4 class="text-sm font-semibold text-slate-800">{{ title }}</h4>
        <span v-if="unit" class="text-xs text-slate-400 font-normal">({{ unit }})</span>
      </div>
      <div>
        <slot name="headerRight" />
      </div>
    </div>

    <!-- 图表容器 -->
    <div ref="chartRef" :style="{ height, width: '100%' }" />

    <!-- 正常区间/危急值标注说明条 -->
    <div v-if="referenceRange" class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
      <div class="flex items-center gap-4">
        <span class="inline-flex items-center gap-1.5">
          <span class="w-3 h-1.5 bg-emerald-100 border border-emerald-400 rounded-xs inline-block" />
          <span>参考区间：{{ referenceRange[0] }} ~ {{ referenceRange[1] }} {{ unit }}</span>
        </span>
        <span v-if="criticalRange" class="inline-flex items-center gap-1.5 text-rose-600">
          <span class="w-1.5 h-1.5 bg-rose-500 rounded-full inline-block animate-pulse" />
          <span>危急阈值：&lt;{{ criticalRange[0] }} 或 &gt;{{ criticalRange[1] }}</span>
        </span>
      </div>

      <span v-if="latestPoint" class="font-medium text-slate-700">
        最新：<span class="font-semibold text-sky-600">{{ latestPoint.value }}</span> {{ unit }} ({{ latestPoint.date }})
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import * as echarts from 'echarts'

export interface TrendPoint {
  date: string
  value: number
  note?: string
}

const props = withDefaults(
  defineProps<{
    title?: string
    data: TrendPoint[]
    referenceRange?: [number, number]
    criticalRange?: [number, number]
    unit?: string
    height?: string
    color?: string
  }>(),
  {
    title: '',
    data: () => [],
    referenceRange: undefined,
    criticalRange: undefined,
    unit: '',
    height: '240px',
    color: '#0EA5E9',
  }
)

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

const latestPoint = computed(() => {
  if (!props.data || props.data.length === 0) return null
  return props.data[props.data.length - 1]
})

const renderChart = () => {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const xData = props.data.map((item) => item.date)
  const yData = props.data.map((item) => item.value)

  // 标记区域（正常参考区间）
  const markArea: any = props.referenceRange
    ? {
        silent: true,
        itemStyle: {
          color: 'rgba(16, 185, 129, 0.08)',
        },
        data: [
          [
            { yAxis: props.referenceRange[0] },
            { yAxis: props.referenceRange[1] },
          ],
        ],
      }
    : undefined

  // 标线（上下限虚线）
  const markLineData: any[] = []
  if (props.referenceRange) {
    markLineData.push(
      {
        yAxis: props.referenceRange[0],
        lineStyle: { color: '#10B981', type: 'dashed', width: 1 },
        label: { show: true, position: 'end', formatter: '下限 {c}', fontSize: 10 },
      },
      {
        yAxis: props.referenceRange[1],
        lineStyle: { color: '#10B981', type: 'dashed', width: 1 },
        label: { show: true, position: 'end', formatter: '上限 {c}', fontSize: 10 },
      }
    )
  }

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#E2E8F0',
      borderWidth: 1,
      textStyle: {
        color: '#1E293B',
        fontSize: 12,
      },
      padding: [8, 12],
      extraCssText: 'box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08); border-radius: 8px;',
      formatter: (params: any) => {
        const p = params[0]
        const rawPoint = props.data[p.dataIndex]
        let statusBadge = '<span style="color: #10B981; font-weight: 500;">正常</span>'
        if (props.referenceRange) {
          if (p.value < props.referenceRange[0]) {
            statusBadge = '<span style="color: #F59E0B; font-weight: 500;">偏低 ↓</span>'
          } else if (p.value > props.referenceRange[1]) {
            statusBadge = '<span style="color: #F59E0B; font-weight: 500;">偏高 ↑</span>'
          }
        }
        if (props.criticalRange) {
          if (p.value < props.criticalRange[0] || p.value > props.criticalRange[1]) {
            statusBadge = '<span style="color: #EF4444; font-weight: 600;">⚠ 危急值</span>'
          }
        }

        return `
          <div style="font-size: 11px; color: #64748B; margin-bottom: 4px;">${p.name}</div>
          <div style="display: flex; align-items: center; justify-content: space-between; gap: 16px;">
            <span style="font-weight: 600; color: #0F172A;">${p.value} ${props.unit}</span>
            <span>${statusBadge}</span>
          </div>
          ${rawPoint?.note ? `<div style="font-size: 11px; color: #64748B; margin-top: 4px;">备注: ${rawPoint.note}</div>` : ''}
        `
      },
    },
    grid: {
      left: '12px',
      right: '28px',
      top: '20px',
      bottom: '12px',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: xData,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748B', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      axisLabel: { color: '#64748B', fontSize: 11 },
      scale: true,
    },
    series: [
      {
        name: props.title || '数值',
        type: 'line',
        smooth: 0.35,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: {
          color: props.color,
          borderColor: '#FFFFFF',
          borderWidth: 2,
        },
        lineStyle: {
          color: props.color,
          width: 2.5,
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14, 165, 233, 0.28)' },
            { offset: 1, color: 'rgba(14, 165, 233, 0.01)' },
          ]),
        },
        markArea,
        markLine: markLineData.length
          ? {
              symbol: 'none',
              data: markLineData,
            }
          : undefined,
        data: yData,
      },
    ],
  }

  chartInstance.setOption(option, true)
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(
  () => [props.data, props.referenceRange, props.criticalRange],
  () => {
    renderChart()
  },
  { deep: true }
)

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>
