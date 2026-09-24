<template>
  <div class="lab-result-view">
    <!-- Category Filter -->
    <div class="lab-filter">
      <div class="flex items-center gap-2">
        <span class="filter-label">检验类别：</span>
        <el-radio-group v-model="selectedCategory" size="small" @change="handleCategoryChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button
            v-for="cat in categories"
            :key="cat.value"
            :value="cat.value"
          >
            {{ cat.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Grouped Tables -->
    <div v-for="group in filteredGroups" :key="group.category" class="lab-group">
      <div class="lab-group-header">
        <span class="lab-group-icon">{{ group.icon }}</span>
        <span class="lab-group-title">{{ group.label }}</span>
        <el-tag type="info" size="small">{{ group.items.length }}项</el-tag>
      </div>
      <el-table :data="group.items" row-key="id" size="small" class="lab-table">
        <el-table-column label="检验项目" prop="test_name" width="160" show-overflow-tooltip />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <span :class="{ 'abnormal-value': row.abnormal_flag && row.abnormal_flag !== 'N' }">
              {{ row.result_value }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="单位" prop="unit" width="80" />
        <el-table-column label="参考范围" prop="reference_range" width="130" />
        <el-table-column label="标志" width="80">
          <template #default="{ row }">
            <template v-if="row.abnormal_flag && row.abnormal_flag !== 'N'">
              <el-tag :type="abnormalColorMap[row.abnormal_flag] || 'danger'" size="small">
                {{ abnormalLabelMap[row.abnormal_flag] || row.abnormal_flag }}
              </el-tag>
            </template>
            <template v-else>
              <span class="normal-flag">正常</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="检验时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.test_time) }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-if="!loading && filteredGroups.length === 0" description="暂无检验结果" :image-size="60" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getLabResults } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'LabResultView' })

interface Props {
  patientId: string
  encounterId: string
}

const props = defineProps<Props>()

const loading = ref(false)
const labResults = ref<any[]>([])
const selectedCategory = ref('')

const categories = [
  { label: '血液', value: 'blood', icon: '🩸' },
  { label: '尿液', value: 'urine', icon: '🧪' },
  { label: '生化', value: 'biochemistry', icon: '🔬' },
  { label: '免疫', value: 'immunology', icon: '🧫' },
  { label: '微生物', value: 'microbiology', icon: '🦠' },
]

const abnormalColorMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  H: 'danger',
  HH: 'danger',
  L: 'warning',
  LL: 'danger',
  A: 'danger',
}

const abnormalLabelMap: Record<string, string> = {
  H: '偏高',
  HH: '极高',
  L: '偏低',
  LL: '极低',
  A: '异常',
  N: '正常',
}

const categoryLabelMap: Record<string, { label: string; icon: string }> = {
  blood: { label: '血液检验', icon: '🩸' },
  urine: { label: '尿液检验', icon: '🧪' },
  biochemistry: { label: '生化检验', icon: '🔬' },
  immunology: { label: '免疫检验', icon: '🧫' },
  microbiology: { label: '微生物检验', icon: '🦠' },
}

const groupedResults = computed(() => {
  const groups: Record<string, any[]> = {}
  for (const item of labResults.value) {
    const cat = item.category || 'other'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(item)
  }
  return Object.entries(groups).map(([category, items]) => {
    const meta = categoryLabelMap[category] || { label: category, icon: '📋' }
    return { category, items, label: meta.label, icon: meta.icon }
  })
})

const filteredGroups = computed(() => {
  if (!selectedCategory.value) return groupedResults.value
  return groupedResults.value.filter((g) => g.category === selectedCategory.value)
})

async function loadData() {
  loading.value = true
  try {
    const res = await getLabResults(props.patientId, props.encounterId)
    labResults.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function handleCategoryChange() {
  // Filtering is reactive via computed
}

onMounted(loadData)
</script>

<style scoped>
.lab-result-view {
  padding-top: 8px;
}
.lab-filter {
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
.lab-group {
  margin-bottom: 20px;
}
.lab-group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}
.lab-group-icon {
  font-size: 18px;
}
.lab-group-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.abnormal-value {
  color: #ef4444;
  font-weight: 600;
}
.normal-flag {
  color: #cbd5e1;
  font-size: 13px;
}
</style>
