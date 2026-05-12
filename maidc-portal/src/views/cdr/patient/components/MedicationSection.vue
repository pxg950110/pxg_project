<template>
  <div class="data-section">
    <div class="section-header" @click="toggleCollapse">
      <div class="section-title">
        <span class="section-icon">💊</span>
        用药记录
        <span class="section-badge">{{ medications.length }}</span>
      </div>
      <span :class="['collapse-arrow', { expanded: !isCollapsed }]">▼</span>
    </div>
    <div v-show="!isCollapsed" class="section-content">
      <div v-if="medications.length === 0" class="empty-state">
        暂无用药记录
      </div>
      <div v-else>
        <div
          v-for="medication in medications"
          :key="medication.id"
          class="data-item"
        >
          <div class="item-info">
            <div class="data-label">{{ medication.drugName }}</div>
            <div v-if="medication.specification" class="item-spec">
              {{ medication.specification }}
            </div>
          </div>
          <div class="item-dosage">
            <span class="dosage-value">{{ medication.dosage }}{{ medication.unit }}</span>
            <span class="dosage-freq">{{ medication.frequency }}</span>
            <span v-if="medication.usage" class="dosage-usage">{{ usageLabel(medication.usage) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface MedicationDTO {
  id: number
  drugCode: string
  drugName: string
  specification: string
  dosage: string
  unit: string
  frequency: string
  usage: string // PO/IV/IM
  startTime: string // LocalDateTime serialized as ISO string
  endTime?: string
  orderType?: string // LONG_TERM/TEMPORARY
  doctorName?: string
}

interface Props {
  medications: MedicationDTO[]
}

const props = defineProps<Props>()

const isCollapsed = ref(false)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// Map usage to Chinese label
const usageLabel = (usage: string): string => {
  const usageMap: Record<string, string> = {
    'PO': '口服',
    'IV': '静脉注射',
    'IM': '肌肉注射',
    'IH': '皮下注射',
    'TOP': '外用',
    'INH': '吸入'
  }
  return usageMap[usage] || usage
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

.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.data-label {
  color: rgba(0, 0, 0, 0.88);
  font-size: 14px;
  font-weight: 500;
}

.item-spec {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.item-dosage {
  display: flex;
  align-items: center;
  gap: 8px;
  text-align: right;
}

.dosage-value {
  color: rgba(0, 0, 0, 0.88);
  font-weight: 500;
  font-size: 14px;
}

.dosage-freq {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.dosage-usage {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  padding: 1px 6px;
  background: #f5f5f5;
  border-radius: 3px;
}
</style>
