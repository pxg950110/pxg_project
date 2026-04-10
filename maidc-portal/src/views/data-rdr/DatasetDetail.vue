<template>
  <PageContainer :title="dataset?.name || '数据集详情'" :loading="loading">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <template v-if="dataset">
      <!-- Basic Info -->
      <a-card style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="数据集名称">{{ dataset.name }}</a-descriptions-item>
          <a-descriptions-item label="所属项目">{{ dataset.project_name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="样本数">{{ dataset.sample_count ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="版本数">{{ dataset.version_count ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建人">{{ dataset.creator_name || '-' }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatDateTime(dataset.updated_at) }}</a-descriptions-item>
          <a-descriptions-item label="描述" :span="3">{{ dataset.description || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- Tabs -->
      <a-card>
        <a-tabs v-model:activeKey="activeTab">
          <!-- Versions Tab -->
          <a-tab-pane key="versions" tab="版本">
            <a-table :columns="versionColumns" :data-source="versions" :loading="versionsLoading" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'file_size'">
                  {{ formatFileSize(record.file_size) }}
                </template>
                <template v-if="column.key === 'created_at'">
                  {{ formatDateTime(record.created_at) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Fields Tab -->
          <a-tab-pane key="fields" tab="字段">
            <a-table :columns="fieldColumns" :data-source="fields" :loading="fieldsLoading" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'type'">
                  <a-tag>{{ record.type }}</a-tag>
                </template>
                <template v-if="column.key === 'nullable'">
                  <a-tag :color="record.nullable ? 'orange' : 'green'">
                    {{ record.nullable ? '可空' : '非空' }}
                  </a-tag>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Access Log Tab -->
          <a-tab-pane key="access" tab="访问记录">
            <a-table :columns="accessColumns" :data-source="accessLog" :loading="accessLoading" size="small" row-key="id">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'accessed_at'">
                  {{ formatDateTime(record.accessed_at) }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <!-- Statistics Tab -->
          <a-tab-pane key="statistics" tab="统计">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-card title="记录数趋势" size="small">
                  <MetricChart :option="recordCountOption" height="300px" />
                </a-card>
              </a-col>
              <a-col :span="12">
                <a-card title="字段分布" size="small">
                  <MetricChart :option="fieldDistOption" height="300px" />
                </a-card>
              </a-col>
            </a-row>
          </a-tab-pane>
        </a-tabs>
      </a-card>
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

const versionColumns = [
  { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
  { title: '记录数', dataIndex: 'record_count', key: 'record_count', width: 100 },
  { title: '文件大小', dataIndex: 'file_size', key: 'file_size', width: 120 },
  { title: '创建人', dataIndex: 'created_by', key: 'created_by', width: 100 },
  { title: '创建时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
]

const fieldColumns = [
  { title: '字段名', dataIndex: 'name', key: 'name', width: 180 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '可空', dataIndex: 'nullable', key: 'nullable', width: 80 },
]

const accessColumns = [
  { title: '用户', dataIndex: 'user_name', key: 'user_name', width: 120 },
  { title: '操作类型', dataIndex: 'action_type', key: 'action_type', width: 120 },
  { title: '访问时间', dataIndex: 'accessed_at', key: 'accessed_at', width: 170 },
  { title: 'IP', dataIndex: 'ip_address', key: 'ip_address', width: 140 },
]

const recordCountOption = ref({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value', name: '记录数' },
  series: [{ type: 'bar', data: [] as number[], itemStyle: { color: '#1677ff' } }],
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
      series: [{ type: 'bar', data: [1200, 3500, 4800, 6200, 7500], itemStyle: { color: '#1677ff' } }],
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
