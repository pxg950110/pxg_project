<template>
  <PageContainer title="质量检测结果">
    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <!-- 汇总统计 -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
      <MetricCard title="检测总数" :value="summary.total" suffix="次" />
      <MetricCard title="通过" :value="summary.pass" suffix="次" />
      <MetricCard title="警告" :value="summary.warning" suffix="次" />
      <MetricCard title="不通过" :value="summary.fail" suffix="次" />
    </div>

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="规则名称" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.rule_name || row.rule?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="检测时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.check_time) }}
        </template>
      </el-table-column>
      <el-table-column label="得分" width="100" align="center">
        <template #default="{ row }">
          <el-progress
            type="circle"
            :percentage="row.score || 0"
            :width="40"
            :stroke-width="5"
            :color="getScoreColor(row.score)"
          />
        </template>
      </el-table-column>
      <el-table-column label="通过/失败" width="150">
        <template #default="{ row }">
          <span class="count-pass">{{ row.passed_count || 0 }} 通过</span>
          <el-divider direction="vertical" />
          <span class="count-fail">{{ row.failed_count || 0 }} 失败</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status" type="quality" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="mt-4 justify-end"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      :current-page="pagination.current"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      title="检测结果详情"
      size="600px"
      :destroy-on-close="true"
    >
      <template v-if="currentDetail">
        <el-descriptions border :column="2" size="small" class="mb-4">
          <el-descriptions-item label="规则名称">{{ currentDetail.rule_name || currentDetail.rule?.name }}</el-descriptions-item>
          <el-descriptions-item label="检测状态">
            <StatusBadge :status="currentDetail.status" type="quality" />
          </el-descriptions-item>
          <el-descriptions-item label="检测时间">{{ formatDateTime(currentDetail.check_time) }}</el-descriptions-item>
          <el-descriptions-item label="得分">
            <span :style="{ color: getScoreColor(currentDetail.score), fontWeight: 600 }">
              {{ currentDetail.score }}%
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="通过数">{{ currentDetail.passed_count }}</el-descriptions-item>
          <el-descriptions-item label="失败数">{{ currentDetail.failed_count }}</el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" size="small" class="mb-4 !rounded-lg">
          <template #header>得分分布</template>
          <MetricChart :option="scoreDistributionOption" height="200px" />
        </el-card>

        <el-card shadow="never" size="small" class="!rounded-lg">
          <template #header>失败明细</template>
          <el-table :data="currentDetail.fail_details || []" size="small" row-key="id">
            <el-table-column label="字段" prop="field" width="120" />
            <el-table-column label="期望值" width="120">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.expected }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="实际值" width="120">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.actual }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="记录ID" prop="record_id" width="120" />
          </el-table>
        </el-card>
      </template>
    </el-drawer>
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
  if (score >= 90) return '#10b981'
  if (score >= 70) return '#f59e0b'
  return '#ef4444'
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
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getQualityResults({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

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

const scoreDistributionOption = computed(() => {
  if (!currentDetail.value) return {}
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: currentDetail.value.passed_count || 0, name: '通过', itemStyle: { color: '#10b981' } },
        { value: currentDetail.value.failed_count || 0, name: '失败', itemStyle: { color: '#ef4444' } },
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

<style scoped>
.count-pass {
  color: #10b981;
}
.count-fail {
  color: #ef4444;
}
</style>
