<template>
  <span class="status-badge">
    <span class="status-dot" :style="{ background: meta.color }" />
    <span class="status-text">{{ meta.text }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getStatusMeta, type StatusType } from './statusMap'

interface Props {
  status: string
  type: StatusType
}

const props = defineProps<Props>()

const meta = computed(() => getStatusMeta(props.status, props.type))
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
}
</style>
