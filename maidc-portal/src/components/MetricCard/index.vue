<template>
  <el-card v-loading="loading" class="metric-card !rounded-xl !border-slate-200/80 shadow-clinical-sm" shadow="hover">
    <div class="metric-card-inner">
      <div class="metric-content">
        <div class="metric-title">{{ title }}</div>
        <div class="metric-value">
          <span class="value-number">{{ displayValue }}</span>
          <span v-if="suffix" class="value-suffix">{{ suffix }}</span>
          <span v-if="trend" class="metric-trend" :class="trend.type">
            <el-icon :size="12" class="mr-0.5">
              <CaretTop v-if="trend.type === 'up'" />
              <CaretBottom v-else />
            </el-icon>
            {{ Math.abs(trend.value) }}%
          </span>
        </div>
      </div>
      <div v-if="icon" class="metric-icon">
        <component :is="icon" />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { CaretTop, CaretBottom } from '@element-plus/icons-vue'

interface Trend {
  value: number
  type: 'up' | 'down'
}

interface Props {
  title: string
  value: number | string
  suffix?: string
  trend?: Trend
  loading?: boolean
  icon?: Component
}

const props = defineProps<Props>()

const displayValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})
</script>

<style scoped>
.metric-card {
  border-radius: 12px;
}
.metric-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.metric-title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
  font-weight: 500;
}
.metric-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.value-number {
  font-size: 26px;
  font-weight: 700;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #0f172a;
  line-height: 1.2;
}
.value-suffix {
  font-size: 13px;
  color: #94a3b8;
}
.metric-trend {
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
  font-weight: 600;
}
.metric-trend.up {
  color: #10b981;
}
.metric-trend.down {
  color: #ef4444;
}
.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: #f0f9ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #0ea5e9;
  flex-shrink: 0;
}
</style>
