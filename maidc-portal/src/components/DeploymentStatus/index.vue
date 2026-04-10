<template>
  <div class="deployment-status">
    <div class="status-indicator">
      <span class="status-dot" :style="{ backgroundColor: dotColor }" />
      <span class="status-icon">
        <LoadingOutlined v-if="isProcessing" spin />
        <CheckCircleFilled v-else-if="status === 'RUNNING'" style="color: #52c41a" />
        <PauseCircleFilled v-else-if="status === 'STOPPED'" style="color: #8c8c8c" />
        <CloseCircleFilled v-else-if="status === 'FAILED'" style="color: #ff4d4f" />
      </span>
      <span class="status-text">{{ statusText }}</span>
    </div>
    <a-progress
      v-if="progress !== undefined && progress >= 0"
      :percent="progress"
      :status="progressStatus"
      size="small"
      :stroke-color="progress >= 100 ? '#52c41a' : '#1677ff'"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  LoadingOutlined,
  CheckCircleFilled,
  PauseCircleFilled,
  CloseCircleFilled,
} from '@ant-design/icons-vue'

interface Props {
  status: string
  progress?: number
}

const props = defineProps<Props>()

const statusMeta: Record<string, { text: string; color: string }> = {
  CREATING: { text: '创建中', color: '#1677ff' },
  STARTING: { text: '启动中', color: '#faad14' },
  RUNNING: { text: '运行中', color: '#52c41a' },
  STOPPING: { text: '停止中', color: '#faad14' },
  STOPPED: { text: '已停止', color: '#8c8c8c' },
  FAILED: { text: '失败', color: '#ff4d4f' },
}

const dotColor = computed(() => statusMeta[props.status]?.color ?? '#d9d9d9')
const statusText = computed(() => statusMeta[props.status]?.text ?? props.status)
const isProcessing = computed(() => ['CREATING', 'STARTING', 'STOPPING'].includes(props.status))

const progressStatus = computed<'success' | 'active' | 'exception' | 'normal'>(() => {
  if (props.status === 'FAILED') return 'exception'
  if (props.progress !== undefined && props.progress >= 100) return 'success'
  return 'active'
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
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}
</style>
