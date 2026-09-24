<template>
  <div class="medication-view">
    <!-- Status Filter -->
    <div class="medication-filter">
      <div class="flex items-center gap-2">
        <span class="filter-label">用药状态：</span>
        <el-radio-group v-model="filterStatus" size="small" @change="handleFilterChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="ACTIVE">使用中</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
          <el-radio-button value="DISCONTINUED">已停用</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Medication Table -->
    <el-table :data="filteredMedications" v-loading="loading" row-key="id" size="small" class="medication-table">
      <el-table-column label="药品名称" width="180">
        <template #default="{ row }">
          <span class="med-name">{{ row.medication_name }}</span>
          <div v-if="row.generic_name" class="med-generic">{{ row.generic_name }}</div>
        </template>
      </el-table-column>
      <el-table-column label="剂量 / 频次" width="150">
        <template #default="{ row }">
          <span>{{ row.dosage }}</span>
          <div class="med-freq">{{ row.frequency }}</div>
        </template>
      </el-table-column>
      <el-table-column label="给药途径" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.route }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用药时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.start_date) }}
          <template v-if="row.end_date">
            <br />
            <span class="date-range-sep">至</span>
            {{ formatDate(row.end_date) }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="处方医生" prop="prescribing_doctor" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status" type="medication" />
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && filteredMedications.length === 0" description="暂无用药记录" :image-size="60" />
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
  background: #f8fafc;
  border-radius: 6px;
}
.filter-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}
.medication-table {
  margin-top: 8px;
}
.med-name {
  font-weight: 500;
  color: #0f172a;
}
.med-generic {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.med-freq {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.date-range-sep {
  color: #cbd5e1;
}
</style>
