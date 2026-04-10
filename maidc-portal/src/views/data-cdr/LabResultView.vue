<template>
  <div class="lab-result-view">
    <!-- Category Filter -->
    <div class="lab-filter">
      <a-space>
        <span class="filter-label">检验类别：</span>
        <a-radio-group v-model:value="selectedCategory" button-style="solid" size="small" @change="handleCategoryChange">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button
            v-for="cat in categories"
            :key="cat.value"
            :value="cat.value"
          >
            {{ cat.label }}
          </a-radio-button>
        </a-radio-group>
      </a-space>
    </div>

    <!-- Grouped Tables -->
    <div v-for="group in filteredGroups" :key="group.category" class="lab-group">
      <div class="lab-group-header">
        <span class="lab-group-icon">{{ group.icon }}</span>
        <span class="lab-group-title">{{ group.label }}</span>
        <a-tag color="default">{{ group.items.length }}项</a-tag>
      </div>
      <a-table
        :columns="columns"
        :data-source="group.items"
        :pagination="false"
        row-key="id"
        size="small"
        class="lab-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'result_value'">
            <span :class="{ 'abnormal-value': record.abnormal_flag && record.abnormal_flag !== 'N' }">
              {{ record.result_value }}
            </span>
          </template>
          <template v-if="column.key === 'abnormal_flag'">
            <template v-if="record.abnormal_flag && record.abnormal_flag !== 'N'">
              <a-tag :color="abnormalColorMap[record.abnormal_flag] || 'red'">
                {{ abnormalLabelMap[record.abnormal_flag] || record.abnormal_flag }}
              </a-tag>
            </template>
            <template v-else>
              <span class="normal-flag">正常</span>
            </template>
          </template>
          <template v-if="column.key === 'test_time'">
            {{ formatDateTime(record.test_time) }}
          </template>
        </template>
      </a-table>
    </div>

    <a-empty v-if="!loading && filteredGroups.length === 0" description="暂无检验结果" />
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

const abnormalColorMap: Record<string, string> = {
  H: 'red',
  HH: 'red',
  L: 'orange',
  LL: 'red',
  A: 'purple',
}

const abnormalLabelMap: Record<string, string> = {
  H: '偏高',
  HH: '极高',
  L: '偏低',
  LL: '极低',
  A: '异常',
  N: '正常',
}

const columns = [
  { title: '检验项目', dataIndex: 'test_name', key: 'test_name', width: 160, ellipsis: true },
  { title: '结果', dataIndex: 'result_value', key: 'result_value', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '参考范围', dataIndex: 'reference_range', key: 'reference_range', width: 130 },
  { title: '标志', dataIndex: 'abnormal_flag', key: 'abnormal_flag', width: 80 },
  { title: '检验时间', dataIndex: 'test_time', key: 'test_time', width: 170 },
]

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
  background: #fafafa;
  border-radius: 6px;
}
.filter-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
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
  border-bottom: 1px solid #f0f0f0;
}
.lab-group-icon {
  font-size: 18px;
}
.lab-group-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}
.abnormal-value {
  color: #ff4d4f;
  font-weight: 600;
}
.normal-flag {
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}
.lab-table :deep(.ant-table-thead > tr > th) {
  background: #fafafa;
}
</style>
