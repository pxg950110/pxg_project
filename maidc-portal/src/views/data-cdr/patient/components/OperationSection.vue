<template>
  <div class="data-section">
    <div class="section-header" @click="toggleCollapse">
      <div class="section-title">
        <span class="section-icon">🔪</span>
        手术记录
        <span class="section-badge">{{ operations.length }}</span>
      </div>
      <span :class="['collapse-arrow', { expanded: !isCollapsed }]">▼</span>
    </div>
    <div v-show="!isCollapsed" class="section-content">
      <div v-if="operations.length === 0" class="empty-state">
        暂无手术记录
      </div>
      <div v-else>
        <div
          v-for="operation in operations"
          :key="operation.id"
          class="data-item"
        >
          <div class="item-info">
            <div class="data-label">{{ operation.operationName }}</div>
            <div class="item-meta">
              <span v-if="operation.startTime">{{ formatDateTime(operation.startTime) }}</span>
              <span v-if="operation.surgeonName">术者: {{ operation.surgeonName }}</span>
              <span v-if="operation.status" class="status-tag" :class="statusClass(operation.status)">
                {{ statusLabel(operation.status) }}
              </span>
            </div>
          </div>
          <div class="item-actions">
            <el-button type="primary" size="small" @click="handleViewDetail(operation)">查看详情</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface OperationDTO {
  id: number
  operationNo: string
  operationName: string
  operationCode: string
  startTime: string // LocalDateTime serialized as ISO string
  endTime?: string
  surgeonCode?: string
  surgeonName: string
  assistantDoctor?: string
  anesthesiaMethod?: string
  anesthesiaDoctor?: string
  operatingRoom?: string
  status: string
}

interface Props {
  operations: OperationDTO[]
}

const props = defineProps<Props>()

const isCollapsed = ref(false)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

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
    'SCHEDULED': '已排程',
    'IN_PROGRESS': '手术中',
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
    'SCHEDULED': 'status-info',
    'CANCELLED': 'status-default'
  }
  return classMap[status] || 'status-default'
}

// Handle view detail
const handleViewDetail = (operation: OperationDTO) => {
  // TODO: Implement detail view
  console.log('View detail for:', operation.operationNo)
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
</style>
