<template>
  <div class="deployment-status">
    <div class="status-indicator">
      <span class="status-dot" :style="{ backgroundColor: dotColor }" />
      <span class="status-icon flex items-center">
        <el-icon v-if="isProcessing" class="is-loading text-sky-500"><Loading /></el-icon>
        <el-icon v-else-if="status === 'RUNNING'" class="text-emerald-500"><CircleCheckFilled /></el-icon>
        <el-icon v-else-if="status === 'STOPPED'" class="text-slate-400"><VideoPause /></el-icon>
        <el-icon v-else-if="status === 'FAILED'" class="text-rose-500"><CircleCloseFilled /></el-icon>
      </span>
      <span class="status-text">{{ statusText }}</span>
    </div>
    <el-progress
      v-if="progress !== undefined && progress >= 0"
      :percentage="progress"
      :status="progressStatus"
      :stroke-width="6"
      :show-text="false"
      class="mt-1"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Loading,
  CircleCheckFilled,
  VideoPause,
  CircleCloseFilled,
} from '@element-plus/icons-vue'

interface Props {
  status: string
  progress?: number
}

const props = defineProps<Props>()

const statusMeta: Record<string, { text: string; color: string }> = {
  CREATING: { text: '创建中', color: '#0284c7' },
  STARTING: { text: '启动中', color: '#f59e0b' },
  RUNNING: { text: '运行中', color: '#10b981' },
  STOPPING: { text: '停止中', color: '#f59e0b' },
  STOPPED: { text: '已停止', color: '#94a3b8' },
  FAILED: { text: '失败', color: '#ef4444' },
}

const dotColor = computed(() => statusMeta[props.status]?.color ?? '#cbd5e1')
const statusText = computed(() => statusMeta[props.status]?.text ?? props.status)
const isProcessing = computed(() => ['CREATING', 'STARTING', 'STOPPING'].includes(props.status))

const progressStatus = computed<'' | 'success' | 'warning' | 'exception'>(() => {
  if (props.status === 'FAILED') return 'exception'
  if (props.progress !== undefined && props.progress >= 100) return 'success'
  return ''
})
</script>

<style scoped>
.deployment-status {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-icon {
  font-size: 16px;
}
.status-text {
  font-size: 13px;
  color: #334155;
  font-weight: 500;
}
</style>
