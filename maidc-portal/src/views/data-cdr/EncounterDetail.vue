<template>
  <PageContainer
    title="就诊详情"
    :subtitle="encounter ? encounter.encounter_id : ''"
    :loading="loading"
    :breadcrumb="breadcrumbs"
  >
    <template #extra>
      <el-button @click="router.back()">
        <el-icon class="mr-1"><ArrowLeft /></el-icon> 返回
      </el-button>
    </template>

    <template v-if="encounter">
      <!-- Encounter Header -->
      <el-card shadow="never" class="encounter-header-card !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="就诊ID">
            {{ encounter.encounter_id }}
          </el-descriptions-item>
          <el-descriptions-item label="患者姓名">
            <router-link
              :to="`/data/cdr/patients/${patientId}`"
              class="patient-link"
            >
              {{ encounter.patient_name }}
            </router-link>
          </el-descriptions-item>
          <el-descriptions-item label="就诊类型">
            <el-tag :type="encounterTypeType">{{ encounter.encounter_type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="科室">
            {{ encounter.department }}
          </el-descriptions-item>
          <el-descriptions-item label="主治医师">
            {{ encounter.attending_doctor }}
          </el-descriptions-item>
          <el-descriptions-item label="入院时间">
            {{ formatDateTime(encounter.admission_time) }}
          </el-descriptions-item>
          <el-descriptions-item label="出院时间">
            {{ encounter.discharge_time ? formatDateTime(encounter.discharge_time) : '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusBadge :status="encounter.status" type="encounter" />
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Sub-tabs -->
      <el-card shadow="never" class="mt-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="诊断" name="diagnosis">
            <DiagnosisView :patient-id="patientId" :encounter-id="encounterId" />
          </el-tab-pane>
          <el-tab-pane label="检验结果" name="labs">
            <LabResultView :patient-id="patientId" :encounter-id="encounterId" />
          </el-tab-pane>
          <el-tab-pane label="影像检查" name="imaging">
            <ImagingView :patient-id="patientId" :encounter-id="encounterId" />
          </el-tab-pane>
          <el-tab-pane label="用药记录" name="medications">
            <MedicationView :patient-id="patientId" :encounter-id="encounterId" />
          </el-tab-pane>
          <el-tab-pane label="生命体征" name="vitals">
            <VitalSignView :patient-id="patientId" :encounter-id="encounterId" />
          </el-tab-pane>
          <el-tab-pane label="临床笔记" name="notes">
            <ClinicalNoteView :patient-id="patientId" :encounter-id="encounterId" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { getEncounterDetail } from '@/api/data'
import { formatDateTime } from '@/utils/date'
import DiagnosisView from './DiagnosisView.vue'
import LabResultView from './LabResultView.vue'
import ImagingView from './ImagingView.vue'
import MedicationView from './MedicationView.vue'
import VitalSignView from './VitalSignView.vue'
import ClinicalNoteView from './ClinicalNoteView.vue'

defineOptions({ name: 'EncounterDetail' })

const route = useRoute()
const router = useRouter()

const encounterId = computed(() => route.params.encounterId as string)
const patientId = computed(() => route.params.id as string)

const encounter = ref<any>(null)
const loading = ref(false)
const activeTab = ref('diagnosis')

const breadcrumbs = computed(() => [
  { title: '患者管理', path: '/data/cdr/patients' },
  { title: '患者详情', path: `/data/cdr/patients/${patientId.value}` },
  { title: '就诊详情' },
])

const encounterTypeType = computed<'primary' | 'success' | 'danger' | 'warning' | 'info'>(() => {
  const typeMap: Record<string, 'primary' | 'success' | 'danger' | 'warning' | 'info'> = {
    '门诊': 'primary',
    '住院': 'success',
    '急诊': 'danger',
    '体检': 'warning',
  }
  return typeMap[encounter.value?.encounter_type] || 'info'
})

async function loadEncounter() {
  loading.value = true
  try {
    const res = await getEncounterDetail(patientId.value, encounterId.value)
    encounter.value = res.data.data
  } finally {
    loading.value = false
  }
}

onMounted(loadEncounter)
</script>

<style scoped>
.encounter-header-card :deep(.el-descriptions__label) {
  background-color: #f8fafc;
  font-weight: 500;
  width: 120px;
}
.patient-link {
  color: #0ea5e9;
  font-weight: 500;
}
.patient-link:hover {
  text-decoration: underline;
}
</style>
