<template>
  <div class="data-section">
    <div class="section-header" @click="toggleCollapse">
      <div class="section-title">
        <span class="section-icon">🏷️</span>
        诊断信息
        <span class="section-badge">{{ diagnoses.length }}</span>
      </div>
      <span :class="['collapse-arrow', { expanded: !isCollapsed }]">▼</span>
    </div>
    <div v-show="!isCollapsed" class="section-content">
      <div v-if="diagnoses.length === 0" class="empty-state">
        暂无诊断信息
      </div>
      <div v-else>
        <div
          v-for="diagnosis in sortedDiagnoses"
          :key="diagnosis.id"
          class="data-item"
        >
          <span class="data-label">{{ diagnosisTypeLabel(diagnosis.diagnosisType) }}</span>
          <span class="data-value">
            {{ diagnosis.icdName }}
            <span v-if="diagnosis.icdCode" class="icd-code">({{ diagnosis.icdCode }})</span>
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface DiagnosisDTO {
  id: number
  diagnosisType: string // MAIN/SECONDARY/ADMISSION/DISCHARGE
  icdCode: string
  icdName: string
  diagnosisTime: string // LocalDateTime serialized as ISO string
  doctorCode?: string
  doctorName?: string
  sortOrder?: number
}

interface Props {
  diagnoses: DiagnosisDTO[]
}

const props = defineProps<Props>()

const isCollapsed = ref(false)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// Sort diagnoses: MAIN first, then by sortOrder
const sortedDiagnoses = computed(() => {
  return [...props.diagnoses].sort((a, b) => {
    // Main diagnosis comes first
    if (a.diagnosisType === 'MAIN' && b.diagnosisType !== 'MAIN') return -1
    if (a.diagnosisType !== 'MAIN' && b.diagnosisType === 'MAIN') return 1
    // Then by sortOrder if available
    if (a.sortOrder !== undefined && b.sortOrder !== undefined) {
      return a.sortOrder - b.sortOrder
    }
    return 0
  })
})

// Map diagnosis type to Chinese label
const diagnosisTypeLabel = (type: string): string => {
  const typeMap: Record<string, string> = {
    'MAIN': '主诊断',
    'SECONDARY': '次诊断',
    'ADMISSION': '入院诊断',
    'DISCHARGE': '出院诊断'
  }
  return typeMap[type] || type
}
</script>

<style scoped>
.data-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 10px;
  transition: background 0.3s;
}

.section-header:hover {
  background: #e8eaf6;
}

.section-title {
  font-weight: 500;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: rgba(0, 0, 0, 0.88);
}

.section-icon {
  font-size: 18px;
}

.section-badge {
  background: #667eea;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.collapse-arrow {
  color: rgba(0, 0, 0, 0.45);
  transition: transform 0.3s;
  font-size: 12px;
}

.collapse-arrow.expanded {
  transform: rotate(180deg);
}

.section-content {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 6px;
  background: white;
}

.empty-state {
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  padding: 20px 0;
}

.data-item {
  padding: 12px;
  border-bottom: 1px solid #f5f5f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-item:last-child {
  border-bottom: none;
}

.data-item:hover {
  background: #fafafa;
}

.data-label {
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  flex-shrink: 0;
  min-width: 80px;
}

.data-value {
  color: rgba(0, 0, 0, 0.88);
  font-weight: 500;
  text-align: right;
}

.icd-code {
  color: rgba(0, 0, 0, 0.45);
  font-weight: normal;
  margin-left: 4px;
}
</style>
