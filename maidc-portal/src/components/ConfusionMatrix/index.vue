<template>
  <div class="confusion-matrix">
    <table class="matrix-table">
      <thead>
        <tr>
          <th class="corner-cell">
            <span class="axis-label predicted">预测</span>
            <span class="axis-label actual">实际</span>
          </th>
          <th v-for="(label, ci) in displayLabels" :key="'h-' + ci" class="header-cell">
            {{ label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, ri) in matrix" :key="'r-' + ri">
          <td class="row-header">{{ displayLabels[ri] }}</td>
          <td
            v-for="(cell, ci) in row"
            :key="'c-' + ri + '-' + ci"
            class="matrix-cell"
            :style="{ backgroundColor: getCellColor(cell) }"
            :title="`实际: ${displayLabels[ri]}, 预测: ${displayLabels[ci]}, 数量: ${cell}`"
          >
            <span :style="{ color: cell > maxVal * 0.5 ? '#fff' : 'rgba(0,0,0,0.85)' }">
              {{ cell }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  matrix: number[][]
  labels?: string[]
}

const props = defineProps<Props>()

const maxVal = computed(() => {
  let max = 0
  for (const row of props.matrix) {
    for (const val of row) {
      if (val > max) max = val
    }
  }
  return max || 1
})

const displayLabels = computed(() => {
  if (props.labels && props.labels.length) return props.labels
  return props.matrix.map((_, i) => `Class ${i}`)
})

function getCellColor(value: number): string {
  const intensity = value / maxVal.value
  // Blue gradient from light to dark
  const r = Math.round(22 + (22 - 22) * intensity)
  const g = Math.round(119 - 87 * intensity)
  const b = Math.round(255 - 55 * intensity)
  const alpha = 0.1 + 0.8 * intensity
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style scoped>
.confusion-matrix {
  overflow: auto;
}
.matrix-table {
  border-collapse: collapse;
  margin: 0 auto;
}
.corner-cell {
  position: relative;
  width: 80px;
  height: 60px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
}
.axis-label {
  position: absolute;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 500;
}
.axis-label.predicted {
  top: 4px;
  right: 8px;
}
.axis-label.actual {
  bottom: 4px;
  left: 8px;
}
.header-cell {
  padding: 8px 16px;
  background: #fafafa;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #f0f0f0;
  text-align: center;
  min-width: 60px;
}
.row-header {
  padding: 8px 16px;
  background: #fafafa;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #f0f0f0;
  text-align: right;
}
.matrix-cell {
  padding: 10px 16px;
  border: 1px solid #f0f0f0;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
  min-width: 60px;
  transition: background-color 0.2s;
}
.matrix-cell:hover {
  outline: 2px solid #1677ff;
  outline-offset: -2px;
}
</style>
