<template>
  <a-timeline class="approval-timeline">
    <a-timeline-item
      v-for="(record, idx) in records"
      :key="idx"
      :color="getTimelineColor(record.status)"
    >
      <div class="timeline-item">
        <div class="timeline-header">
          <span class="timeline-step">{{ record.step }}</span>
          <StatusBadge :status="record.status" type="approval" />
        </div>
        <div v-if="record.reviewer" class="timeline-reviewer">
          审批人：{{ record.reviewer }}
        </div>
        <div v-if="record.comment" class="timeline-comment">
          {{ record.comment }}
        </div>
        <div v-if="record.timestamp" class="timeline-time">
          {{ formatTime(record.timestamp) }}
        </div>
      </div>
    </a-timeline-item>
  </a-timeline>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import StatusBadge from '@/components/StatusBadge/index.vue'

interface ApprovalRecord {
  step: string
  reviewer?: string
  status: string
  comment?: string
  timestamp?: string
}

interface Props {
  records: ApprovalRecord[]
}

defineProps<Props>()

function getTimelineColor(status: string): string {
  const colorMap: Record<string, string> = {
    PENDING: '#faad14',
    APPROVED: '#52c41a',
    REJECTED: '#ff4d4f',
  }
  return colorMap[status] ?? '#d9d9d9'
}

function formatTime(timestamp: string): string {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}
</script>

<style scoped>
.approval-timeline {
  padding: 8px 0;
}
.timeline-item {
  padding-bottom: 4px;
}
.timeline-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 4px;
}
.timeline-step {
  font-weight: 500;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}
.timeline-reviewer {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 2px;
}
.timeline-comment {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  margin-bottom: 2px;
}
.timeline-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
