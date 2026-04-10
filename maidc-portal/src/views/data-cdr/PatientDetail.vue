<template>
  <PageContainer title="患者详情" :loading="loading">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <template v-if="patient">
      <PatientInfoCard :patient="patient" />

      <a-card style="margin-top: 16px">
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="encounters" tab="就诊记录">
            <a-table :columns="encounterColumns" :data-source="encounters" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a @click="viewEncounter(record)">查看详情</a>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="diagnosis" tab="诊断" />
          <a-tab-pane key="labs" tab="检验结果" />
          <a-tab-pane key="imaging" tab="影像" />
          <a-tab-pane key="medications" tab="用药" />
        </a-tabs>
      </a-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import PatientInfoCard from '@/components/PatientInfoCard/index.vue'
import { getPatient } from '@/api/data'

const route = useRoute()
const router = useRouter()
const patient = ref<any>(null)
const encounters = ref<any[]>([])
const loading = ref(false)
const activeTab = ref('encounters')

const encounterColumns = [
  { title: '就诊ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '就诊类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '科室', dataIndex: 'department', key: 'department', width: 100 },
  { title: '入院时间', dataIndex: 'admission_time', key: 'admission_time', width: 170 },
  { title: '出院时间', dataIndex: 'discharge_time', key: 'discharge_time', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

async function loadPatient() {
  loading.value = true
  try {
    const res = await getPatient(route.params.id as string)
    patient.value = res.data.data
    encounters.value = res.data.data.encounters || []
  } finally { loading.value = false }
}

function viewEncounter(record: any) {
  router.push(`/data/cdr/patients/${route.params.id}/encounters/${record.id}`)
}

onMounted(loadPatient)
</script>
