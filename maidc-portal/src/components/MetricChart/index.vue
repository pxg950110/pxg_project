<template>
  <div class="metric-chart-wrapper">
    <a-spin v-if="loading" class="chart-spin">
      <div class="chart-skeleton" :style="{ height: height }" />
    </a-spin>
    <v-chart
      v-show="!loading"
      :option="option"
      :autoresize="true"
      :style="{ height: height }"
      class="metric-chart"
    />
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart, ScatterChart, HeatmapChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  ToolboxComponent,
  DataZoomComponent,
  VisualMapComponent,
} from 'echarts/components'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  HeatmapChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  ToolboxComponent,
  DataZoomComponent,
  VisualMapComponent,
])

interface Props {
  option: Record<string, any>
  loading?: boolean
  height?: string
}

withDefaults(defineProps<Props>(), {
  loading: false,
  height: '320px',
})
</script>

<style scoped>
.metric-chart-wrapper {
  width: 100%;
}
.chart-spin {
  width: 100%;
}
.chart-skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 37%, #f0f0f0 63%);
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
  border-radius: 4px;
}
@keyframes skeleton-loading {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
.metric-chart {
  width: 100%;
}
</style>
