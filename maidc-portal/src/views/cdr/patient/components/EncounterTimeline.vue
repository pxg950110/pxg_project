<template>
  <div class="timeline-panel">
    <div class="timeline-header">就诊时间轴</div>
    <div v-if="encounters.length === 0" class="timeline-empty">
      暂无就诊记录
    </div>
    <div v-else class="timeline-list">
      <div
        v-for="encounter in sortedEncounters"
        :key="encounter.encounterId"
        :class="['timeline-item', { active: encounter.encounterId === currentEncounterId }]"
        @click="handleSelect(encounter.encounterId)"
      >
        <div class="timeline-date">
          <span :class="['timeline-dot', isCurrent(encounter) ? 'active' : 'history']"></span>
          {{ formatDate(encounter.admitTime) }}
        </div>
        <div class="timeline-dept">{{ encounter.deptName }}</div>
        <div v-if="encounter.mainDiagnosis" class="timeline-diagnosis">
          主诊断: {{ encounter.mainDiagnosis }}
        </div>
        <span :class="['timeline-status', isCurrent(encounter) ? 'current' : 'history']">
          {{ isCurrent(encounter) ? '当前' : '历史' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface EncounterTimelineDTO {
  encounterId: number
  encounterNo: string
  encounterType: string // OUTPATIENT/INPATIENT/EMERGENCY
  deptCode: string
  deptName: string
  admitTime: string // LocalDateTime serialized as ISO string
  dischargeTime?: string
  mainDiagnosis?: string
  status: string // ACTIVE/DISCHARGED
  isCurrent?: boolean
}

interface Props {
  encounters: EncounterTimelineDTO[]
  currentEncounterId: number | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', encounterId: number): void
}>()

// Sort encounters by admitTime descending (most recent first)
const sortedEncounters = computed(() => {
  return [...props.encounters].sort((a, b) => {
    const timeA = new Date(a.admitTime).getTime()
    const timeB = new Date(b.admitTime).getTime()
    return timeB - timeA
  })
})

// Format date string for display
const formatDate = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// Check if encounter is current (status='ACTIVE' or dischargeTime null)
const isCurrent = (encounter: EncounterTimelineDTO): boolean => {
  return encounter.status === 'ACTIVE' || !encounter.dischargeTime
}

// Handle item selection
const handleSelect = (encounterId: number) => {
  emit('select', encounterId)
}
</script>

<style scoped>
.timeline-panel {
  width: 320px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow-y: auto;
  flex-shrink: 0;
}

.timeline-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
  font-weight: 500;
  font-size: 16px;
  color: rgba(0, 0, 0, 0.88);
}

.timeline-empty {
  padding: 40px 20px;
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
}

.timeline-list {
  padding: 10px;
}

.timeline-item {
  padding: 15px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 8px;
  border-left: 3px solid transparent;
}

.timeline-item:hover {
  background: #f5f7fa;
}

.timeline-item.active {
  background: #e8eaf6;
  border-left-color: #667eea;
}

.timeline-date {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 5px;
  display: flex;
  align-items: center;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
  margin-right: 8px;
  flex-shrink: 0;
}

.timeline-dot.active {
  background: #667eea;
}

.timeline-dot.history {
  border: 2px solid #bdbdbd;
  background: white;
}

.timeline-dept {
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  margin-bottom: 5px;
}

.timeline-diagnosis {
  color: #f57c00;
  font-size: 13px;
  margin-bottom: 5px;
  padding: 4px 8px;
  background: #fff3e0;
  border-radius: 4px;
  display: inline-block;
}

.timeline-status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.timeline-status.current {
  background: #667eea;
  color: white;
}

.timeline-status.history {
  background: #e0e0e0;
  color: rgba(0, 0, 0, 0.65);
}

/* Custom scrollbar styling */
.timeline-panel::-webkit-scrollbar {
  width: 8px;
}

.timeline-panel::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.timeline-panel::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.timeline-panel::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
