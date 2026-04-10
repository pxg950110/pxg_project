<template>
  <PageContainer title="质量检测结果">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <!-- 汇总统计 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <MetricCard title="检测总数" :value="summary.total" suffix="次" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="通过" :value="summary.pass" suffix="次" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="警告" :value="summary.warning" suffix="次" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="不通过" :value="summary.fail" suffix="次" />
      </a-col>
    </a-row>

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ruleName'">
          {{ record.rule_name || record.rule?.name || '-' }}
        </template>
        <template v-if="column.key === 'checkTime'">
          {{ formatDateTime(record.check_time) }}
        </template>
        <template v-if="column.key === 'score'">
          <a-progress
            type="circle"
            :percent="record.score || 0"
            :size="40"
            :stroke-color="getScoreColor(record.score)"
          />
        </template>
        <template v-if="column.key === 'counts'">
          <span style="color: #52c41a">{{ record.passed_count || 0 }} 通过</span>
          <a-divider type="vertical" />
          <span style="color: #ff4d4f">{{ record.failed_count || 0 }} 失败</span>
        </template>
        <template v-if="column.key === 'status'">
          <StatusBadge :status="record.status" type="quality" />
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleViewDetail(record)">详情</a-button>
        </template>
      </template>
    </a-table>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="检测结果详情"
      :width="600"
      destroy-on-close
    >
      <template v-if="currentDetail">
        <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="规则名称">{{ currentDetail.rule_name || currentDetail.rule?.name }}</a-descriptions-item>
          <a-descriptions-item label="检测状态">
            <StatusBadge :status="currentDetail.status" type="quality" />
          </a-descriptions-item>
          <a-descriptions-item label="检测时间">{{ formatDateTime(currentDetail.check_time) }}</a-descriptions-item>
          <a-descriptions-item label="得分">
            <span :style="{ color: getScoreColor(currentDetail.score), fontWeight: 600 }">
              {{ currentDetail.score }}%
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="通过数">{{ currentDetail.passed_count }}</a-descriptions-item>
          <a-descriptions-item label="失败数">{{ currentDetail.failed_count }}</a-descriptions-item>
        </a-descriptions>

        <a-card title="得分分布" size="small" style="margin-bottom: 16px">
          <MetricChart :option="scoreDistributionOption" height="200px" />
        </a-card>

        <a-card title="失败明细" size="small">
          <a-table
            :columns="failDetailColumns"
            :data-source="currentDetail.fail_details || []"
            size="small"
            row-key="id"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'expected'">
                <a-tag color="blue">{{ record.expected }}</a-tag>
              </template>
              <template v-if="column.key === 'actual'">
                <a-tag color="red">{{ record.actual }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </template>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { useTable } from '@/hooks/useTable'
import { getQualityResults, getQualityResult } from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'QualityResultList' })

// ===== 常量 =====
function getScoreColor(score: number): string {
  if (score >= 90) return '#52c41a'
  if (score >= 70) return '#faad14'
  return '#ff4d4f'
}

// ===== 搜索 =====
const searchFields = [
  { name: 'status', label: '检测结果', type: 'select' as const, options: [
    { label: '通过', value: 'PASS' },
    { label: '警告', value: 'WARNING' },
    { label: '不通过', value: 'FAIL' },
  ] },
  { name: 'timeRange', label: '检测时间', type: 'dateRange' as const },
]

let currentSearchParams: Record<string, any> = {}

function handleSearch(values: Record<string, any>) {
  currentSearchParams = values
  fetchData({ page: 1 })
}

function handleReset() {
  currentSearchParams = {}
  fetchData({ page: 1 })
}

// ===== 汇总 =====
const summary = reactive({
  total: 0,
  pass: 0,
  warning: 0,
  fail: 0,
})

// ===== 表格 =====
const columns = [
  { title: '规则名称', key: 'ruleName', width: 200, ellipsis: true },
  { title: '检测时间', key: 'checkTime', width: 170 },
  { title: '得分', key: 'score', width: 100 },
  { title: '通过/失败', key: 'counts', width: 150 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 80 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getQualityResults({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

// Update summary when table data changes
function updateSummary(data: any[]) {
  summary.total = data.length
  summary.pass = data.filter((d: any) => d.status === 'PASS').length
  summary.warning = data.filter((d: any) => d.status === 'WARNING').length
  summary.fail = data.filter((d: any) => d.status === 'FAIL').length
}

// ===== 详情抽屉 =====
const detailVisible = ref(false)
const currentDetail = ref<any>(null)

const failDetailColumns = [
  { title: '字段', dataIndex: 'field', width: 120 },
  { title: '期望值', key: 'expected', width: 120 },
  { title: '实际值', key: 'actual', width: 120 },
  { title: '记录ID', dataIndex: 'record_id', width: 120 },
]

const scoreDistributionOption = computed(() => {
  if (!currentDetail.value) return {}
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: currentDetail.value.passed_count || 0, name: '通过', itemStyle: { color: '#52c41a' } },
        { value: currentDetail.value.failed_count || 0, name: '失败', itemStyle: { color: '#ff4d4f' } },
      ],
      label: { show: true, formatter: '{b}: {c}' },
    }],
  }
})

async function handleViewDetail(record: any) {
  currentDetail.value = record
  detailVisible.value = true
  try {
    const res = await getQualityResult(record.id)
    currentDetail.value = res.data.data
  } catch {
    // keep the record data
  }
}

onMounted(() => {
  fetchData()
})
</script>
