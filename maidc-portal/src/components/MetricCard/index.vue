<template>
  <a-card :loading="loading" class="metric-card" :bordered="false">
    <div class="metric-card-inner">
      <div class="metric-content">
        <div class="metric-title">{{ title }}</div>
        <div class="metric-value">
          <span class="value-number">{{ displayValue }}</span>
          <span v-if="suffix" class="value-suffix">{{ suffix }}</span>
          <span v-if="trend" class="metric-trend" :class="trend.type">
            <CaretUpOutlined v-if="trend.type === 'up'" />
            <CaretDownOutlined v-else />
            {{ Math.abs(trend.value) }}%
          </span>
        </div>
      </div>
      <div v-if="icon" class="metric-icon">
        <component :is="icon" />
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { CaretUpOutlined, CaretDownOutlined } from '@ant-design/icons-vue'

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
  border-radius: 8px;
}
.metric-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.metric-title {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}
.metric-value {
  font-size: 0;
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.value-number {
  font-size: 28px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  line-height: 1.2;
}
.value-suffix {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}
.metric-trend {
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: 8px;
}
.metric-trend.up {
  color: #52c41a;
}
.metric-trend.down {
  color: #ff4d4f;
}
.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: rgba(22, 119, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #1677ff;
  flex-shrink: 0;
}
</style>
