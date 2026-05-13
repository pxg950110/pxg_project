<template>
  <div class="data-section">
    <div class="section-header" @click="toggleCollapse">
      <div class="section-title">
        <span class="section-icon">🧪</span>
        检验结果
        <span class="section-badge">{{ labTests.length }}</span>
      </div>
      <span :class="['collapse-arrow', { expanded: !isCollapsed }]">▼</span>
    </div>
    <div v-show="!isCollapsed" class="section-content">
      <div v-if="labTests.length === 0" class="empty-state">
        暂无检验结果
      </div>
      <div v-else>
        <div
          v-for="labTest in labTests"
          :key="labTest.id"
          class="data-item"
        >
          <div class="item-info">
            <div class="data-label">{{ labTest.testType }}</div>
            <div class="item-meta">
              <span v-if="labTest.sampleTime">{{ formatDateTime(labTest.sampleTime) }}</span>
              <span v-if="labTest.status" class="status-tag" :class="statusClass(labTest.status)">
                {{ statusLabel(labTest.status) }}
              </span>
            </div>
          </div>
          <div class="item-actions">
            <el-button size="small" @click="handleViewDetail(labTest)">查看详情</el-button>
            <el-button type="primary" size="small" @click="handleViewTrend(labTest)">趋势图表</el-button>
          </div>
        </div>

        <!-- Nested panels for lab items -->
        <div v-if="expandedLabId" class="lab-panels">
          <div
            v-for="panel in currentLabPanels"
            :key="panel.id"
            class="panel-item"
          >
            <div class="panel-row">
              <span class="panel-name">{{ panel.itemName }}</span>
              <span :class="['panel-result', { abnormal: isAbnormal(panel.abnormalFlag) }]">
                {{ panel.result }} {{ panel.unit }}
              </span>
            </div>
            <div v-if="panel.referenceRange" class="panel-reference">
              参考范围: {{ panel.referenceRange }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface LabPanelDTO {
  id: number
  itemName: string
  itemCode: string
  result: string
  unit: string
  referenceRange: string
  abnormalFlag: string // NORMAL/HIGH/LOW
  reportTime: string
}

interface LabTestDTO {
  id: number
  testNo: string
  testType: string
  sampleType: string
  sampleTime: string // LocalDateTime serialized as ISO string
  reportTime: string
  status: string
  orderingDoctor?: string
  panels?: LabPanelDTO[]
}

interface Props {
  labTests: LabTestDTO[]
}

const props = defineProps<Props>()

const isCollapsed = ref(false)
const expandedLabId = ref<number | null>(null)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// Get panels for currently expanded lab test
const currentLabPanels = computed(() => {
  if (!expandedLabId.value) return []
  const labTest = props.labTests.find(lt => lt.id === expandedLabId.value)
  return labTest?.panels || []
})

// Format date time for display
const formatDateTime = (dateStr: string): string => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

// Map status to label
const statusLabel = (status: string): string => {
  const statusMap: Record<string, string> = {
    'PENDING': '待检',
    'IN_PROGRESS': '检验中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

// Get status class for styling
const statusClass = (status: string): string => {
  const classMap: Record<string, string> = {
    'COMPLETED': 'status-success',
    'IN_PROGRESS': 'status-warning',
    'PENDING': 'status-info',
    'CANCELLED': 'status-default'
  }
  return classMap[status] || 'status-default'
}

// Check if result is abnormal
const isAbnormal = (flag: string): boolean => {
  return flag === 'HIGH' || flag === 'LOW'
}

// Handle view detail
const handleViewDetail = (labTest: LabTestDTO) => {
  // Toggle expanded state
  if (expandedLabId.value === labTest.id) {
    expandedLabId.value = null
  } else {
    expandedLabId.value = labTest.id
  }
}

// Handle view trend
const handleViewTrend = (labTest: LabTestDTO) => {
  // TODO: Implement trend chart view
  console.log('View trend for:', labTest.testType)
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

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.status-tag {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
}

.status-success {
  background: #e8f5e9;
  color: #4caf50;
}

.status-warning {
  background: #fff3e0;
  color: #ff9800;
}

.status-info {
  background: #e3f2fd;
  color: #2196f3;
}

.status-default {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.45);
}

.item-actions {
  display: flex;
  gap: 8px;
}

.lab-panels {
  margin-top: 10px;
  padding: 10px;
  background: #fafafa;
  border-radius: 4px;
}

.panel-item {
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.panel-item:last-child {
  border-bottom: none;
}

.panel-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-name {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.panel-result {
  color: rgba(0, 0, 0, 0.88);
  font-weight: 500;
  font-size: 13px;
}

.panel-result.abnormal {
  color: #f44336;
}

.panel-reference {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}
</style>
