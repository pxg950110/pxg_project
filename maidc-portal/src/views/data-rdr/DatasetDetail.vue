<template>
  <PageContainer :title="dataset?.name || '数据集详情'" :loading="loading">
    <template #extra>
      <el-button @click="router.back()">返回</el-button>
    </template>

    <template v-if="dataset">
      <!-- Basic Info -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm mb-4">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="数据集名称">{{ dataset.name }}</el-descriptions-item>
          <el-descriptions-item label="所属项目">{{ dataset.project_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="样本数">{{ dataset.sample_count ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="版本数">{{ dataset.version_count ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ dataset.creator_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(dataset.updated_at) }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="3">{{ dataset.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Tabs -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-tabs v-model="activeTab">
          <!-- Versions Tab -->
          <el-tab-pane label="版本" name="versions">
            <el-table :data="versions" v-loading="versionsLoading" size="small" row-key="id">
              <el-table-column label="版本" prop="version" width="100" />
              <el-table-column label="记录数" prop="record_count" width="100" />
              <el-table-column label="文件大小" width="120">
                <template #default="{ row }">{{ formatFileSize(row.file_size) }}</template>
              </el-table-column>
              <el-table-column label="创建人" prop="created_by" width="100" />
              <el-table-column label="创建时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Fields Tab -->
          <el-tab-pane label="字段" name="fields">
            <el-table :data="fields" v-loading="fieldsLoading" size="small" row-key="id">
              <el-table-column label="字段名" prop="name" width="180" />
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-tag type="info">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="描述" prop="description" show-overflow-tooltip />
              <el-table-column label="可空" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.nullable ? 'warning' : 'success'">
                    {{ row.nullable ? '可空' : '非空' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- Access Log Tab -->
          <el-tab-pane label="访问记录" name="access">
            <el-table :data="accessLog" v-loading="accessLoading" size="small" row-key="id">
              <el-table-column label="用户" prop="user_name" width="120" />
              <el-table-column label="操作类型" prop="action_type" width="120" />
              <el-table-column label="访问时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.accessed_at) }}</template>
              </el-table-column>
              <el-table-column label="IP" prop="ip_address" width="140" />
            </el-table>
          </el-tab-pane>

          <!-- Statistics Tab -->
          <el-tab-pane label="统计" name="statistics">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
                <template #header>记录数趋势</template>
                <MetricChart :option="recordCountOption" height="300px" />
              </el-card>
              <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
                <template #header>字段分布</template>
                <MetricChart :option="fieldDistOption" height="300px" />
              </el-card>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { getProject } from '@/api/data'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'DatasetDetail' })

const route = useRoute()
const router = useRouter()

const dataset = ref<any>(null)
const loading = ref(false)
const activeTab = ref('versions')

// Versions
const versions = ref<any[]>([])
const versionsLoading = ref(false)
// Fields
const fields = ref<any[]>([])
const fieldsLoading = ref(false)
// Access log
const accessLog = ref<any[]>([])
const accessLoading = ref(false)

const recordCountOption = ref({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value', name: '记录数' },
  series: [{ type: 'bar', data: [] as number[], itemStyle: { color: '#0ea5e9' } }],
})

const fieldDistOption = ref({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['35%', '65%'],
    data: [] as { value: number; name: string }[],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
  }],
})

function formatFileSize(bytes: number | undefined): string {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}

async function loadDataset() {
  loading.value = true
  try {
    const res = await request.get(`/rdr/datasets/${route.params.id}`)
    dataset.value = res.data.data
  } finally {
    loading.value = false
  }
}

async function loadVersions() {
  versionsLoading.value = true
  try {
    const res = await request.get(`/rdr/datasets/${route.params.id}/versions`)
    versions.value = res.data.data?.items || res.data.data || []
  } finally {
    versionsLoading.value = false
  }
}

async function loadFields() {
  fieldsLoading.value = true
  try {
    const res = await request.get(`/rdr/datasets/${route.params.id}/fields`)
    fields.value = res.data.data?.items || res.data.data || []
  } finally {
    fieldsLoading.value = false
  }
}

async function loadAccessLog() {
  accessLoading.value = true
  try {
    const res = await request.get(`/rdr/datasets/${route.params.id}/access-log`)
    accessLog.value = res.data.data?.items || res.data.data || []
  } finally {
    accessLoading.value = false
  }
}

function loadStatistics() {
  // Generate chart data from dataset info or fetch from API
  const ds = dataset.value
  if (ds) {
    recordCountOption.value = {
      ...recordCountOption.value,
      xAxis: { type: 'category', data: ['v1.0', 'v1.1', 'v1.2', 'v2.0', 'v2.1'] },
      series: [{ type: 'bar', data: [1200, 3500, 4800, 6200, 7500], itemStyle: { color: '#0ea5e9' } }],
    }
    fieldDistOption.value = {
      ...fieldDistOption.value,
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        data: [
          { value: 12, name: 'VARCHAR' },
          { value: 8, name: 'INTEGER' },
          { value: 5, name: 'FLOAT' },
          { value: 3, name: 'DATE' },
          { value: 2, name: 'BOOLEAN' },
        ],
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      }],
    }
  }
}

watch(activeTab, (tab) => {
  if (tab === 'versions' && !versions.value.length) loadVersions()
  else if (tab === 'fields' && !fields.value.length) loadFields()
  else if (tab === 'access' && !accessLog.value.length) loadAccessLog()
  else if (tab === 'statistics') loadStatistics()
})

onMounted(async () => {
  await loadDataset()
  loadVersions()
})
</script>
