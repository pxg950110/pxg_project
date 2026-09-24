<template>
  <div class="diagnosis-view">
    <!-- Filter -->
    <div class="diagnosis-filter">
      <div class="flex items-center gap-2">
        <span class="filter-label">诊断类型：</span>
        <el-radio-group v-model="filterType" size="small" @change="handleFilter">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="primary">主诊断</el-radio-button>
          <el-radio-button value="secondary">次诊断</el-radio-button>
          <el-radio-button value="admission">入院诊断</el-radio-button>
          <el-radio-button value="discharge">出院诊断</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Table -->
    <el-table :data="filteredData" v-loading="loading" row-key="id" size="small" class="diagnosis-table">
      <el-table-column label="诊断编码" width="140">
        <template #default="{ row }">
          <code class="code-text">{{ row.diagnosis_code }}</code>
        </template>
      </el-table-column>
      <el-table-column label="诊断名称" prop="diagnosis_name" show-overflow-tooltip />
      <el-table-column label="诊断类型" width="110">
        <template #default="{ row }">
          <el-tag :type="typeColorMap[row.type] || 'info'" size="small">
            {{ typeLabelMap[row.type] || row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="诊断时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.diagnosis_time) }}
        </template>
      </el-table-column>
      <el-table-column label="诊断医生" prop="doctor" width="100" />
    </el-table>

    <el-empty v-if="!loading && filteredData.length === 0" description="暂无诊断记录" :image-size="60" />
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

const typeColorMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  primary: 'danger',
  secondary: 'primary',
  admission: 'warning',
  discharge: 'success',
}

const typeLabelMap: Record<string, string> = {
  primary: '主诊断',
  secondary: '次诊断',
  admission: '入院诊断',
  discharge: '出院诊断',
}

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
  background: #f8fafc;
  border-radius: 6px;
}
.filter-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}
.diagnosis-table {
  margin-top: 8px;
}
.code-text {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: #0f172a;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}
</style>
