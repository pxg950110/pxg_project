<template>
  <PageContainer
    title="就诊详情"
    :subtitle="encounter ? encounter.encounter_id : ''"
    :loading="loading"
    :breadcrumb="breadcrumbs"
  >
    <template #extra>
      <a-button @click="router.back()">
        <LeftOutlined /> 返回
      </a-button>
    </template>

    <template v-if="encounter">
      <!-- Encounter Header -->
      <a-card :bordered="false" class="encounter-header-card">
        <a-descriptions :column="{ xs: 1, sm: 2, md: 3, lg: 4 }" bordered size="small">
          <a-descriptions-item label="就诊ID">
            {{ encounter.encounter_id }}
          </a-descriptions-item>
          <a-descriptions-item label="患者姓名">
            <router-link
              :to="`/data/cdr/patients/${patientId}`"
              class="patient-link"
            >
              {{ encounter.patient_name }}
            </router-link>
          </a-descriptions-item>
          <a-descriptions-item label="就诊类型">
            <a-tag :color="encounterTypeColor">{{ encounter.encounter_type }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="科室">
            {{ encounter.department }}
          </a-descriptions-item>
          <a-descriptions-item label="主治医师">
            {{ encounter.attending_doctor }}
          </a-descriptions-item>
          <a-descriptions-item label="入院时间">
            {{ formatDateTime(encounter.admission_time) }}
          </a-descriptions-item>
          <a-descriptions-item label="出院时间">
            {{ encounter.discharge_time ? formatDateTime(encounter.discharge_time) : '--' }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusBadge :status="encounter.status" type="encounter" />
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Sub-tabs -->
      <a-card :bordered="false" style="margin-top: 16px">
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="diagnosis" tab="诊断">
            <DiagnosisView :patient-id="patientId" :encounter-id="encounterId" />
          </a-tab-pane>
          <a-tab-pane key="labs" tab="检验结果">
            <LabResultView :patient-id="patientId" :encounter-id="encounterId" />
          </a-tab-pane>
          <a-tab-pane key="imaging" tab="影像检查">
            <ImagingView :patient-id="patientId" :encounter-id="encounterId" />
          </a-tab-pane>
          <a-tab-pane key="medications" tab="用药记录">
            <MedicationView :patient-id="patientId" :encounter-id="encounterId" />
          </a-tab-pane>
          <a-tab-pane key="vitals" tab="生命体征">
            <VitalSignView :patient-id="patientId" :encounter-id="encounterId" />
          </a-tab-pane>
          <a-tab-pane key="notes" tab="临床笔记">
            <ClinicalNoteView :patient-id="patientId" :encounter-id="encounterId" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LeftOutlined } from '@ant-design/icons-vue'
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

const encounterTypeColor = computed(() => {
  const typeMap: Record<string, string> = {
    '门诊': 'blue',
    '住院': 'green',
    '急诊': 'red',
    '体检': 'purple',
  }
  return typeMap[encounter.value?.encounter_type] || 'default'
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
.encounter-header-card {
  border-radius: 8px;
}
.encounter-header-card :deep(.ant-descriptions-item-label) {
  background-color: #fafafa;
  font-weight: 500;
  width: 120px;
}
.patient-link {
  color: #1677ff;
  font-weight: 500;
}
.patient-link:hover {
  text-decoration: underline;
}
</style>
