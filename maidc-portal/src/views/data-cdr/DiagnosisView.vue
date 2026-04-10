<template>
  <div class="diagnosis-view">
    <!-- Filter -->
    <div class="diagnosis-filter">
      <a-space>
        <span class="filter-label">诊断类型：</span>
        <a-radio-group v-model:value="filterType" button-style="solid" size="small" @change="handleFilter">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button value="primary">主诊断</a-radio-button>
          <a-radio-button value="secondary">次诊断</a-radio-button>
          <a-radio-button value="admission">入院诊断</a-radio-button>
          <a-radio-button value="discharge">出院诊断</a-radio-button>
        </a-radio-group>
      </a-space>
    </div>

    <!-- Table -->
    <a-table
      :columns="columns"
      :data-source="filteredData"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
      class="diagnosis-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'diagnosis_code'">
          <a-typography-text code>{{ record.diagnosis_code }}</a-typography-text>
        </template>
        <template v-if="column.key === 'type'">
          <a-tag :color="typeColorMap[record.type] || 'default'">
            {{ typeLabelMap[record.type] || record.type }}
          </a-tag>
        </template>
        <template v-if="column.key === 'diagnosis_time'">
          {{ formatDateTime(record.diagnosis_time) }}
        </template>
      </template>
    </a-table>

    <a-empty v-if="!loading && filteredData.length === 0" description="暂无诊断记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDiagnoses } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'DiagnosisView' })

interface Props {
  patientId: string
  encounterId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const diagnoses = ref<any[]>([])
const filterType = ref('')

const typeColorMap: Record<string, string> = {
  primary: 'red',
  secondary: 'blue',
  admission: 'orange',
  discharge: 'green',
}

const typeLabelMap: Record<string, string> = {
  primary: '主诊断',
  secondary: '次诊断',
  admission: '入院诊断',
  discharge: '出院诊断',
}

const columns = [
  { title: '诊断编码', dataIndex: 'diagnosis_code', key: 'diagnosis_code', width: 140 },
  { title: '诊断名称', dataIndex: 'diagnosis_name', key: 'diagnosis_name', ellipsis: true },
  { title: '诊断类型', dataIndex: 'type', key: 'type', width: 110 },
  { title: '诊断时间', dataIndex: 'diagnosis_time', key: 'diagnosis_time', width: 170 },
  { title: '诊断医生', dataIndex: 'doctor', key: 'doctor', width: 100 },
]

const filteredData = computed(() => {
  if (!filterType.value) return diagnoses.value
  return diagnoses.value.filter((d: any) => d.type === filterType.value)
})

async function loadData() {
  loading.value = true
  try {
    const res = await getDiagnoses(props.patientId, props.encounterId)
    diagnoses.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function handleFilter() {
  // Filtering is reactive via computed, no extra action needed
}

onMounted(loadData)
</script>

<style scoped>
.diagnosis-view {
  padding-top: 8px;
}
.diagnosis-filter {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
}
.filter-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  font-weight: 500;
}
.diagnosis-table {
  margin-top: 8px;
}
</style>
