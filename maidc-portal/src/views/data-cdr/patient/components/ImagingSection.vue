<template>
  <div class="data-section">
    <div class="section-header" @click="toggleCollapse">
      <div class="section-title">
        <span class="section-icon">📸</span>
        影像检查
        <span class="section-badge">{{ imagingExams.length }}</span>
      </div>
      <span :class="['collapse-arrow', { expanded: !isCollapsed }]">▼</span>
    </div>
    <div v-show="!isCollapsed" class="section-content">
      <div v-if="imagingExams.length === 0" class="empty-state">
        暂无影像检查
      </div>
      <div v-else>
        <div
          v-for="exam in imagingExams"
          :key="exam.id"
          class="data-item"
        >
          <div class="item-info">
            <div class="data-label">{{ exam.examType }}</div>
            <div class="item-meta">
              <span v-if="exam.bodyPart">部位: {{ exam.bodyPart }}</span>
              <span v-if="exam.examTime">{{ formatDateTime(exam.examTime) }}</span>
              <span v-if="exam.status" class="status-tag" :class="statusClass(exam.status)">
                {{ statusLabel(exam.status) }}
              </span>
            </div>
          </div>
          <div class="item-actions">
            <el-button size="small" @click="handleViewImage(exam)">查看影像</el-button>
            <el-button type="primary" size="small" @click="handleViewReport(exam)">查看报告</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface ImagingExamDTO {
  id: number
  examNo: string
  examType: string // CT/MRI/ULTRASOUND/XRAY
  bodyPart: string
  modality?: string
  examTime: string // LocalDateTime serialized as ISO string
  reportTime?: string
  status: string
  performingDoctor?: string
  reportDoctor?: string
  findings?: string
  conclusion?: string
}

interface Props {
  imagingExams: ImagingExamDTO[]
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
    'PENDING': '待检',
    'IN_PROGRESS': '检查中',
    'COMPLETED': '已完成',
    'REPORTED': '已报告',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

// Get status class for styling
const statusClass = (status: string): string => {
  const classMap: Record<string, string> = {
    'REPORTED': 'status-success',
    'COMPLETED': 'status-success',
    'IN_PROGRESS': 'status-warning',
    'PENDING': 'status-info',
    'CANCELLED': 'status-default'
  }
  return classMap[status] || 'status-default'
}

// Handle view image
const handleViewImage = (exam: ImagingExamDTO) => {
  // TODO: Implement image viewer
  console.log('View image for:', exam.examNo)
}

// Handle view report
const handleViewReport = (exam: ImagingExamDTO) => {
  // TODO: Implement report viewer
  console.log('View report for:', exam.examNo)
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
