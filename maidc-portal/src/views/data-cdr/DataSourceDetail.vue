<template>
  <PageContainer title="数据源详情" :loading="loading">
    <template #extra>
      <a-space>
        <a-button @click="handleTestConnection">测试连接</a-button>
        <a-button @click="handleSync">立即同步</a-button>
        <a-button @click="router.back()">返回</a-button>
      </a-space>
    </template>

    <template v-if="sourceData">
      <!-- 基础信息 -->
      <a-card title="基本信息" style="margin-bottom: 16px">
        <a-descriptions :column="3" bordered size="small">
          <a-descriptions-item label="数据源名称">{{ sourceData.name }}</a-descriptions-item>
          <a-descriptions-item label="类型">
            <a-tag :color="sourceTypeColorMap[sourceData.type] || 'default'">
              {{ sourceTypeMap[sourceData.type] || sourceData.type }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="连接状态">
            <StatusBadge :status="sourceData.connection_status" type="connection" />
          </a-descriptions-item>
          <a-descriptions-item label="数据源类型">
            {{ sourceData.sourceTypeCode || sourceData.source_type_code }}
          </a-descriptions-item>
          <a-descriptions-item :span="2" label="连接参数">
            <template v-if="sourceData.connectionParams || sourceData.connection_params">
              <a-tag v-for="(val, key) in parseConnectionParams()" :key="key" style="margin: 2px">
                {{ key }}: {{ key === 'password' ? '***' : val }}
              </a-tag>
            </template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item label="同步模式">{{ syncModeMap[sourceData.sync_mode] || sourceData.sync_mode }}</a-descriptions-item>
          <a-descriptions-item label="Cron 表达式">{{ sourceData.cron_expression || '-' }}</a-descriptions-item>
          <a-descriptions-item label="最后同步">
            {{ sourceData.last_sync_time ? formatDateTime(sourceData.last_sync_time) : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDateTime(sourceData.created_at) }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 统计概览 -->
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <MetricCard title="同步总次数" :value="statistics.total_syncs || 0" suffix="次" />
        </a-col>
        <a-col :span="6">
          <MetricCard title="成功次数" :value="statistics.success_count || 0" suffix="次" />
        </a-col>
        <a-col :span="6">
          <MetricCard title="失败次数" :value="statistics.fail_count || 0" suffix="次" />
        </a-col>
        <a-col :span="6">
          <MetricCard title="同步记录总数" :value="statistics.total_records || 0" suffix="条" />
        </a-col>
      </a-row>

      <!-- Tab 区域 -->
      <a-card>
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="syncHistory" tab="同步历史">
            <a-table
              :columns="syncHistoryColumns"
              :data-source="syncHistoryData"
              :loading="syncHistoryLoading"
              :pagination="syncHistoryPagination"
              @change="handleSyncHistoryChange"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <StatusBadge :status="record.status" type="sync" />
                </template>
                <template v-if="column.key === 'startTime'">
                  {{ formatDateTime(record.start_time) }}
                </template>
                <template v-if="column.key === 'duration'">
                  {{ record.duration ? `${record.duration}s` : '-' }}
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="schemaMapping" tab="Schema 映射">
            <a-table
              :columns="schemaMappingColumns"
              :data-source="schemaMappingData"
              :loading="schemaLoading"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'arrow'">
                  <SwapOutlined style="color: #1677ff" />
                </template>
                <template v-if="column.key === 'transform'">
                  <a-tag v-if="record.transform_type" color="blue">{{ record.transform_type }}</a-tag>
                  <span v-else>-</span>
                </template>
              </template>
            </a-table>
          </a-tab-pane>

          <a-tab-pane key="statistics" tab="数据统计">
            <a-row :gutter="16" style="margin-bottom: 16px">
              <a-col :span="12">
                <a-card title="每日同步量趋势" size="small">
                  <MetricChart :option="dailySyncChartOption" height="280px" />
                </a-card>
              </a-col>
              <a-col :span="12">
                <a-card title="数据表记录数" size="small">
                  <MetricChart :option="tableVolumeChartOption" height="280px" />
                </a-card>
              </a-col>
            </a-row>
          </a-tab-pane>

          <a-tab-pane key="health" tab="健康监控">
            <HealthMonitor :source-id="sourceId" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SwapOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import HealthMonitor from '@/components/HealthMonitor/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import {
  getDataSource,
  testDataSourceConnection,
  syncDataSource,
  getDataSourceSyncHistory,
  getDataSourceSchemaMapping,
  getDataSourceStatistics,
} from '@/api/data'
import { formatDateTime } from '@/utils/date'

defineOptions({ name: 'DataSourceDetail' })

const route = useRoute()
const router = useRouter()
const sourceId = Number(route.params.id)

// ===== 常量 =====
const sourceTypeMap: Record<string, string> = {
  HIS: 'HIS', LIS: 'LIS', PACS: 'PACS', EMR: 'EMR', EXTERNAL: '外部系统',
}
const sourceTypeColorMap: Record<string, string> = {
  HIS: 'blue', LIS: 'green', PACS: 'purple', EMR: 'orange', EXTERNAL: 'cyan',
}
const syncModeMap: Record<string, string> = {
  realtime: '实时同步', batch: '批量同步', manual: '手动同步',
}

function parseConnectionParams(): Record<string, any> {
  const raw = sourceData.value?.connectionParams || sourceData.value?.connection_params
  if (!raw) return {}
  if (typeof raw === 'string') {
    try { return JSON.parse(raw) } catch { return {} }
  }
  return raw
}

// ===== 基础数据 =====
const sourceData = ref<any>(null)
const loading = ref(false)
const activeTab = ref('syncHistory')
const statistics = ref<Record<string, number>>({})

async function loadSource() {
  loading.value = true
  try {
    const res = await getDataSource(sourceId)
    sourceData.value = res.data.data
    loadStatistics()
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  try {
    const res = await getDataSourceStatistics(sourceId)
    statistics.value = res.data.data
  } catch {
    // statistics are optional
  }
}

// ===== 同步历史 =====
const syncHistoryData = ref<any[]>([])
const syncHistoryLoading = ref(false)
const syncHistoryPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
})

const syncHistoryColumns = [
  { title: '开始时间', key: 'startTime', width: 170 },
  { title: '状态', key: 'status', width: 100 },
  { title: '影响记录数', dataIndex: 'records_affected', width: 120 },
  { title: '耗时', key: 'duration', width: 100 },
  { title: '备注', dataIndex: 'message', ellipsis: true },
]

async function loadSyncHistory() {
  syncHistoryLoading.value = true
  try {
    const res = await getDataSourceSyncHistory(sourceId, {
      page: syncHistoryPagination.current,
      page_size: syncHistoryPagination.pageSize,
    })
    syncHistoryData.value = res.data.data.items
    syncHistoryPagination.total = res.data.data.total
  } finally {
    syncHistoryLoading.value = false
  }
}

function handleSyncHistoryChange(pag: any) {
  syncHistoryPagination.current = pag.current
  syncHistoryPagination.pageSize = pag.pageSize
  loadSyncHistory()
}

// ===== Schema 映射 =====
const schemaMappingData = ref<any[]>([])
const schemaLoading = ref(false)

const schemaMappingColumns = [
  { title: '源表', dataIndex: 'source_table', width: 150 },
  { title: '源字段', dataIndex: 'source_field', width: 150 },
  { title: '', key: 'arrow', width: 40 },
  { title: '目标表', dataIndex: 'target_table', width: 150 },
  { title: '目标字段', dataIndex: 'target_field', width: 150 },
  { title: '转换类型', key: 'transform', width: 120 },
]

async function loadSchemaMapping() {
  schemaLoading.value = true
  try {
    const res = await getDataSourceSchemaMapping(sourceId)
    schemaMappingData.value = res.data.data || []
  } finally {
    schemaLoading.value = false
  }
}

// ===== 统计图表 =====
const dailySyncChartOption = ref<Record<string, any>>({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: '记录数' },
  series: [{ type: 'bar', data: [], itemStyle: { color: '#1677ff' } }],
})

const tableVolumeChartOption = ref<Record<string, any>>({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: '记录数' },
  series: [{ type: 'bar', data: [], itemStyle: { color: '#52c41a' } }],
})

// ===== 操作 =====
async function handleTestConnection() {
  const hide = message.loading('正在测试连接...', 0)
  try {
    const res = await testDataSourceConnection(sourceId)
    hide()
    if (res.data.data.success) {
      message.success('连接成功')
    } else {
      message.error(`连接失败: ${res.data.data.message}`)
    }
  } catch {
    hide()
  }
}

async function handleSync() {
  try {
    await syncDataSource(sourceId)
    message.success('同步任务已启动')
  } catch {
    // error handled by request interceptor
  }
}

onMounted(() => {
  loadSource()
  loadSyncHistory()
  loadSchemaMapping()
})
</script>
