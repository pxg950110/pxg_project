<template>
  <div class="roc-curve">
    <v-chart :option="chartOption" :autoresize="true" style="height: 400px; width: 100%" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

interface RocPoint {
  fpr: number
  tpr: number
}

interface Props {
  data: RocPoint[]
  auc?: number
}

const props = defineProps<Props>()

const chartOption = computed(() => {
  // Sort by fpr ascending
  const sorted = [...props.data].sort((a, b) => a.fpr - b.fpr)

  // Reference diagonal line
  const diagonal = [
    [0, 0],
    [1, 1],
  ]

  const seriesData = sorted.map((p) => [p.fpr, p.tpr])

  return {
    title: {
      text: props.auc !== undefined ? `ROC Curve (AUC = ${props.auc.toFixed(4)})` : 'ROC Curve',
      left: 'center',
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      formatter(params: any) {
        const p = Array.isArray(params) ? params[0] : params
        return `FPR: ${p.data[0].toFixed(4)}<br/>TPR: ${p.data[1].toFixed(4)}`
      },
    },
    grid: {
      left: 60,
      right: 30,
      top: 50,
      bottom: 50,
    },
    xAxis: {
      type: 'value',
      name: 'False Positive Rate',
      min: 0,
      max: 1,
      splitNumber: 10,
      axisLabel: { formatter: (v: number) => v.toFixed(1) },
    },
    yAxis: {
      type: 'value',
      name: 'True Positive Rate',
      min: 0,
      max: 1,
      splitNumber: 10,
      axisLabel: { formatter: (v: number) => v.toFixed(1) },
    },
    series: [
      {
        name: 'ROC',
        type: 'line',
        data: seriesData,
        smooth: false,
        showSymbol: false,
        lineStyle: { color: '#1677ff', width: 2 },
        areaStyle: { color: 'rgba(22, 119, 255, 0.1)' },
      },
      {
        name: 'Reference',
        type: 'line',
        data: diagonal,
        smooth: false,
        showSymbol: false,
        lineStyle: { color: '#d9d9d9', type: 'dashed', width: 1 },
      },
    ],
    legend: {
      data: ['ROC', 'Reference'],
      bottom: 5,
    },
  }
})
</script>

<style scoped>
.roc-curve {
  width: 100%;
}
</style>
