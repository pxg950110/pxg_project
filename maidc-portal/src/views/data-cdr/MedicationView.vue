<template>
  <div class="medication-view">
    <!-- Status Filter -->
    <div class="medication-filter">
      <a-space>
        <span class="filter-label">用药状态：</span>
        <a-radio-group v-model:value="filterStatus" button-style="solid" size="small" @change="handleFilterChange">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button value="ACTIVE">使用中</a-radio-button>
          <a-radio-button value="COMPLETED">已完成</a-radio-button>
          <a-radio-button value="DISCONTINUED">已停用</a-radio-button>
        </a-radio-group>
      </a-space>
    </div>

    <!-- Medication Table -->
    <a-table
      :columns="columns"
      :data-source="filteredMedications"
      :loading="loading"
      :pagination="false"
      row-key="id"
      size="small"
      class="medication-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'medication_name'">
          <span class="med-name">{{ record.medication_name }}</span>
          <div v-if="record.generic_name" class="med-generic">{{ record.generic_name }}</div>
        </template>
        <template v-if="column.key === 'dosage'">
          <span>{{ record.dosage }}</span>
          <div class="med-freq">{{ record.frequency }}</div>
        </template>
        <template v-if="column.key === 'route'">
          <a-tag size="small">{{ record.route }}</a-tag>
        </template>
        <template v-if="column.key === 'start_date'">
          {{ formatDate(record.start_date) }}
          <template v-if="record.end_date">
            <br />
            <span class="date-range-sep">至</span>
            {{ formatDate(record.end_date) }}
          </template>
        </template>
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="medication" />
        </template>
      </template>
    </a-table>

    <a-empty v-if="!loading && filteredMedications.length === 0" description="暂无用药记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { getMedications } from '@/api/data'
import { formatDate } from '@/utils/date'

defineOptions({ name: 'MedicationView' })

interface Props {
  patientId: string
  encounterId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const medications = ref<any[]>([])
const filterStatus = ref('')

const columns = [
  { title: '药品名称', dataIndex: 'medication_name', key: 'medication_name', width: 180 },
  { title: '剂量 / 频次', dataIndex: 'dosage', key: 'dosage', width: 150 },
  { title: '给药途径', dataIndex: 'route', key: 'route', width: 100 },
  { title: '用药时间', dataIndex: 'start_date', key: 'start_date', width: 170 },
  { title: '处方医生', dataIndex: 'prescribing_doctor', key: 'prescribing_doctor', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
]

const filteredMedications = computed(() => {
  if (!filterStatus.value) return medications.value
  return medications.value.filter((m: any) => m.status === filterStatus.value)
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMedications(props.patientId, props.encounterId)
    medications.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  // Filtering is reactive via computed
}

onMounted(loadData)
</script>

<style scoped>
.medication-view {
  padding-top: 8px;
}
.medication-filter {
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
.medication-table {
  margin-top: 8px;
}
.med-name {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.med-generic {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}
.med-freq {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 2px;
}
.date-range-sep {
  color: rgba(0, 0, 0, 0.25);
}
</style>
