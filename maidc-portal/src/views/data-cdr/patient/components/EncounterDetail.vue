<template>
  <div class="detail-panel">
    <!-- Tab Navigation Fixed Container -->
    <div class="tab-fixed-container">
      <div class="detail-tab-nav">
        <div
          v-for="tab in tabs"
          :key="tab.id"
          :class="['detail-tab-item', { active: currentTab === tab.id }]"
          @click="scrollToSection(tab.id)"
        >
          <span class="tab-icon">{{ tab.icon }}</span>
          {{ tab.label }}
          <span class="detail-tab-badge">{{ tab.count }}</span>
        </div>
      </div>
    </div>

    <!-- Content Scroll Container -->
    <div ref="contentRef" class="detail-content" @scroll="handleScroll">
      <!-- Encounter Basic Info Header -->
      <div class="encounter-header">
        <h3>📋 就诊基本信息</h3>
        <div v-if="basicInfo" class="encounter-meta">
          <div class="meta-item">
            <span class="meta-label">就诊类型</span>
            <span class="meta-value">{{ encounterTypeLabel }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">科室</span>
            <span class="meta-value">{{ basicInfo.deptName || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">主治医生</span>
            <span class="meta-value">{{ basicInfo.doctorName || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">{{ admitTimeLabel }}</span>
            <span class="meta-value">{{ formatDateTime(basicInfo.admitTime) }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">床位</span>
            <span class="meta-value">{{ basicInfo.bedNo || '-' }}床</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">病区</span>
            <span class="meta-value">{{ basicInfo.wardName || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">主诊断</span>
            <span class="meta-value">{{ basicInfo.mainDiagnosis || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">状态</span>
            <span :class="['meta-value', statusClass]">{{ statusLabel }}</span>
          </div>
        </div>
        <div v-else class="empty-state">
          暂无就诊基本信息
        </div>
      </div>

      <!-- Diagnosis Section -->
      <div ref="diagnosisRef" id="diagnosis" class="section-wrapper">
        <DiagnosisSection :diagnoses="encounterDetail?.diagnoses || []" />
      </div>

      <!-- Lab Test Section -->
      <div ref="labRef" id="lab" class="section-wrapper">
        <LabTestSection :labTests="encounterDetail?.labTests || []" />
      </div>

      <!-- Imaging Section -->
      <div ref="imagingRef" id="imaging" class="section-wrapper">
        <ImagingSection :imagingExams="encounterDetail?.imagingExams || []" />
      </div>

      <!-- Medication Section -->
      <div ref="medicationRef" id="medication" class="section-wrapper">
        <MedicationSection :medications="encounterDetail?.medications || []" />
      </div>

      <!-- Operation Section -->
      <div ref="operationRef" id="operation" class="section-wrapper">
        <OperationSection :operations="encounterDetail?.operations || []" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import DiagnosisSection from './DiagnosisSection.vue'
import LabTestSection from './LabTestSection.vue'
import ImagingSection from './ImagingSection.vue'
import MedicationSection from './MedicationSection.vue'
import OperationSection from './OperationSection.vue'

// Types based on backend DTOs
interface EncounterBasicInfoDTO {
  encounterId: number
  encounterNo: string
  encounterType: string // INPATIENT/OUTPATIENT/EMERGENCY
  deptCode: string
  deptName: string
  doctorCode: string
  doctorName: string
  admitTime: string // LocalDateTime as ISO string
  dischargeTime: string
  bedNo: string
  wardCode: string
  wardName: string
  mainDiagnosis: string
  severity: string
  status: string // IN_HOSPITAL/DISCHARGED
}

interface EncounterDetailDTO {
  basicInfo: EncounterBasicInfoDTO
  diagnoses: any[]
  labTests: any[]
  imagingExams: any[]
  medications: any[]
  operations: any[]
}

interface Props {
  encounterDetail: EncounterDetailDTO | null
}

const props = defineProps<Props>()

// Refs for sections and content
const contentRef = ref<HTMLElement | null>(null)
const diagnosisRef = ref<HTMLElement | null>(null)
const labRef = ref<HTMLElement | null>(null)
const imagingRef = ref<HTMLElement | null>(null)
const medicationRef = ref<HTMLElement | null>(null)
const operationRef = ref<HTMLElement | null>(null)

// Current active tab
const currentTab = ref('diagnosis')

// Tab configuration with icons and counts
const tabs = computed(() => [
  {
    id: 'diagnosis',
    label: '诊断信息',
    icon: '🏷️',
    count: props.encounterDetail?.diagnoses?.length || 0
  },
  {
    id: 'lab',
    label: '检验结果',
    icon: '🧪',
    count: props.encounterDetail?.labTests?.length || 0
  },
  {
    id: 'imaging',
    label: '影像检查',
    icon: '📸',
    count: props.encounterDetail?.imagingExams?.length || 0
  },
  {
    id: 'medication',
    label: '用药记录',
    icon: '💊',
    count: props.encounterDetail?.medications?.length || 0
  },
  {
    id: 'operation',
    label: '手术记录',
    icon: '🔪',
    count: props.encounterDetail?.operations?.length || 0
  }
])

// Basic info computed
const basicInfo = computed(() => props.encounterDetail?.basicInfo)

// Encounter type label
const encounterTypeLabel = computed(() => {
  const typeMap: Record<string, string> = {
    'INPATIENT': '住院',
    'OUTPATIENT': '门诊',
    'EMERGENCY': '急诊'
  }
  return typeMap[basicInfo.value?.encounterType || ''] || basicInfo.value?.encounterType || '-'
})

// Admit time label based on encounter type
const admitTimeLabel = computed(() => {
  if (basicInfo.value?.encounterType === 'INPATIENT') {
    return '入院时间'
  } else if (basicInfo.value?.encounterType === 'EMERGENCY') {
    return '就诊时间'
  }
  return '就诊时间'
})

// Status label
const statusLabel = computed(() => {
  const statusMap: Record<string, string> = {
    'IN_HOSPITAL': '在院',
    'DISCHARGED': '已出院'
  }
  return statusMap[basicInfo.value?.status || ''] || basicInfo.value?.status || '-'
})

// Status class for styling
const statusClass = computed(() => {
  if (basicInfo.value?.status === 'IN_HOSPITAL') {
    return 'status-active'
  }
  return 'status-history'
})

// Format datetime
const formatDateTime = (dateTime: string | undefined): string => {
  if (!dateTime) return '-'
  try {
    const date = new Date(dateTime)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateTime
  }
}

// Scroll to section on tab click
const scrollToSection = (sectionId: string) => {
  currentTab.value = sectionId

  const sectionRefs: Record<string, HTMLElement | null> = {
    diagnosis: diagnosisRef.value,
    lab: labRef.value,
    imaging: imagingRef.value,
    medication: medicationRef.value,
    operation: operationRef.value
  }

  const targetSection = sectionRefs[sectionId]
  if (targetSection && contentRef.value) {
    // Calculate offset for sticky tab navigation
    const tabHeight = 60 // Approximate height of sticky tab container
    const scrollTop = targetSection.offsetTop - tabHeight

    contentRef.value.scrollTo({
      top: scrollTop,
      behavior: 'smooth'
    })
  }
}

// Handle scroll to auto-switch active tab
const handleScroll = () => {
  if (!contentRef.value) return

  const scrollTop = contentRef.value.scrollTop
  const tabHeight = 60 // Approximate height of sticky tab container

  const sections = [
    { id: 'diagnosis', ref: diagnosisRef.value },
    { id: 'lab', ref: labRef.value },
    { id: 'imaging', ref: imagingRef.value },
    { id: 'medication', ref: medicationRef.value },
    { id: 'operation', ref: operationRef.value }
  ]

  // Find the section closest to the top of viewport
  let currentSection = 'diagnosis'
  let minDistance = Infinity

  for (const section of sections) {
    if (section.ref) {
      const sectionTop = section.ref.offsetTop - tabHeight
      const distance = Math.abs(scrollTop - sectionTop)

      // Check if this section is in view
      if (scrollTop >= sectionTop - 50 && distance < minDistance) {
        minDistance = distance
        currentSection = section.id
      }
    }
  }

  currentTab.value = currentSection
}

// Intersection Observer for better scroll detection
let observer: IntersectionObserver | null = null

onMounted(() => {
  // Use IntersectionObserver for more accurate section detection
  if (contentRef.value) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const sectionId = entry.target.id
            if (sectionId && ['diagnosis', 'lab', 'imaging', 'medication', 'operation'].includes(sectionId)) {
              currentTab.value = sectionId
            }
          }
        })
      },
      {
        root: contentRef.value,
        rootMargin: '-60px 0px -70% 0px',
        threshold: 0
      }
    )

    // Observe all sections
    const sectionRefs = [diagnosisRef.value, labRef.value, imagingRef.value, medicationRef.value, operationRef.value]
    sectionRefs.forEach((ref) => {
      if (ref && observer) {
        observer.observe(ref)
      }
    })
  }
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})

// Reset to first tab when encounter changes
watch(() => props.encounterDetail, () => {
  currentTab.value = 'diagnosis'
  if (contentRef.value) {
    contentRef.value.scrollTop = 0
  }
})
</script>

<style scoped>
.detail-panel {
  flex-grow: 1;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Tab Navigation Fixed Container */
.tab-fixed-container {
  position: sticky;
  top: 0;
  background: white;
  z-index: 10;
  padding: 20px 20px 0 20px;
  border-bottom: 1px solid #eee;
}

.detail-tab-nav {
  display: flex;
  gap: 10px;
  padding: 0 0 15px 0;
  flex-wrap: wrap;
}

.detail-tab-item {
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  user-select: none;
}

.detail-tab-item:hover {
  background: #e8eaf6;
}

.detail-tab-item.active {
  background: #667eea;
  color: white;
}

.tab-icon {
  font-size: 14px;
}

.detail-tab-badge {
  background: rgba(255, 255, 255, 0.3);
  padding: 1px 5px;
  border-radius: 8px;
  font-size: 11px;
}

.detail-tab-item:not(.active) .detail-tab-badge {
  background: #667eea;
  color: white;
}

/* Content Scroll Container */
.detail-content {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

/* Encounter Header */
.encounter-header {
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.encounter-header h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: rgba(0, 0, 0, 0.88);
}

.encounter-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.meta-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.meta-value {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
  font-weight: 500;
}

.meta-value.status-active {
  color: #4caf50;
}

.meta-value.status-history {
  color: rgba(0, 0, 0, 0.65);
}

.empty-state {
  text-align: center;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  padding: 20px 0;
}

/* Section Wrapper */
.section-wrapper {
  margin-bottom: 20px;
}

/* Scrollbar Styles */
.detail-content::-webkit-scrollbar {
  width: 8px;
}

.detail-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.detail-content::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.detail-content::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>