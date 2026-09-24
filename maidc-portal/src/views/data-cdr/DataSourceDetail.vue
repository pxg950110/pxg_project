<template>
  <PageContainer title="数据源详情" :loading="loading">
    <template #extra>
      <div class="flex items-center gap-2">
        <el-button @click="handleTestConnection">测试连接</el-button>
        <el-button @click="handleSync">立即同步</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </template>

    <template v-if="sourceData">
      <!-- 基础信息 -->
      <el-card shadow="never" class="mb-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <template #header>基本信息</template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="数据源名称">{{ sourceData.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag
              :type="sourceTypeTagMap[sourceData.type] || 'info'"
              :style="sourceTypeStyleMap[sourceData.type]"
            >
              {{ sourceTypeMap[sourceData.type] || sourceData.type }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="连接状态">
            <StatusBadge :status="sourceData.connection_status" type="connection" />
          </el-descriptions-item>
          <el-descriptions-item label="数据源类型">
            {{ sourceData.sourceTypeCode || sourceData.source_type_code }}
          </el-descriptions-item>
          <el-descriptions-item :span="2" label="连接参数">
            <template v-if="sourceData.connectionParams || sourceData.connection_params">
              <el-tag
                v-for="(val, key) in parseConnectionParams()"
                :key="key"
                type="info"
                class="m-0.5"
              >
                {{ key }}: {{ key === 'password' ? '***' : val }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="同步模式">{{ syncModeMap[sourceData.sync_mode] || sourceData.sync_mode }}</el-descriptions-item>
          <el-descriptions-item label="Cron 表达式">{{ sourceData.cron_expression || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后同步">
            {{ sourceData.last_sync_time ? formatDateTime(sourceData.last_sync_time) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(sourceData.created_at) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 统计概览 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
        <MetricCard title="同步总次数" :value="statistics.total_syncs || 0" suffix="次" />
        <MetricCard title="成功次数" :value="statistics.success_count || 0" suffix="次" />
        <MetricCard title="失败次数" :value="statistics.fail_count || 0" suffix="次" />
        <MetricCard title="同步记录总数" :value="statistics.total_records || 0" suffix="条" />
      </div>

      <!-- Tab 区域 -->
      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="同步历史" name="syncHistory">
            <el-table :data="syncHistoryData" v-loading="syncHistoryLoading" row-key="id" size="small">
              <el-table-column label="开始时间" width="170">
                <template #default="{ row }">
                  {{ formatDateTime(row.start_time) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <StatusBadge :status="row.status" type="sync" />
                </template>
              </el-table-column>
              <el-table-column label="影响记录数" prop="records_affected" width="120" />
              <el-table-column label="耗时" width="100">
                <template #default="{ row }">
                  {{ row.duration ? `${row.duration}s` : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="备注" prop="message" show-overflow-tooltip />
            </el-table>
            <el-pagination
              class="mt-4 justify-end"
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="syncHistoryPagination.total"
              :current-page="syncHistoryPagination.current"
              :page-size="syncHistoryPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="handleSyncHistoryPage"
              @size-change="handleSyncHistorySize"
            />
          </el-tab-pane>

          <el-tab-pane label="Schema 映射" name="schemaMapping">
            <el-table :data="schemaMappingData" v-loading="schemaLoading" row-key="id" size="small">
              <el-table-column label="源表" prop="source_table" width="150" />
              <el-table-column label="源字段" prop="source_field" width="150" />
              <el-table-column label="" width="40">
                <template #default>
                  <el-icon color="#0ea5e9"><Switch /></el-icon>
                </template>
              </el-table-column>
              <el-table-column label="目标表" prop="target_table" width="150" />
              <el-table-column label="目标字段" prop="target_field" width="150" />
              <el-table-column label="转换类型" width="120">
                <template #default="{ row }">
                  <el-tag v-if="row.transform_type" type="primary" size="small">{{ row.transform_type }}</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="数据统计" name="statistics">
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
              <el-card shadow="never" size="small" class="!rounded-lg">
                <template #header>每日同步量趋势</template>
                <MetricChart :option="dailySyncChartOption" height="280px" />
              </el-card>
              <el-card shadow="never" size="small" class="!rounded-lg">
                <template #header>数据表记录数</template>
                <MetricChart :option="tableVolumeChartOption" height="280px" />
              </el-card>
            </div>
          </el-tab-pane>

          <el-tab-pane label="健康监控" name="health">
            <HealthMonitor :source-id="sourceId" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import { Switch } from '@element-plus/icons-vue'
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
const sourceTypeTagMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  HIS: 'primary', LIS: 'success', EMR: 'warning',
}
const sourceTypeStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
  PACS: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
  EXTERNAL: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
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
})

async function loadSyncHistory() {
  syncHistoryLoading.value = true
  try {
    const res = await getDataSourceSyncHistory(sourceId, {
      page: syncHistoryPagination.current,
      page_size: syncHistoryPagination.pageSize,
    })
    syncHistoryData.value = res.data.data.items ?? []
    syncHistoryPagination.total = res.data.data.total ?? 0
  } finally {
    syncHistoryLoading.value = false
  }
}

function handleSyncHistoryPage(page: number) {
  syncHistoryPagination.current = page
  loadSyncHistory()
}

function handleSyncHistorySize(size: number) {
  syncHistoryPagination.pageSize = size
  syncHistoryPagination.current = 1
  loadSyncHistory()
}

// ===== Schema 映射 =====
const schemaMappingData = ref<any[]>([])
const schemaLoading = ref(false)

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
  series: [{ type: 'bar', data: [], itemStyle: { color: '#0ea5e9' } }],
})

const tableVolumeChartOption = ref<Record<string, any>>({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: [] },
  yAxis: { type: 'value', name: '记录数' },
  series: [{ type: 'bar', data: [], itemStyle: { color: '#10b981' } }],
})

// ===== 操作 =====
async function handleTestConnection() {
  const loadingInstance = ElLoading.service({ fullscreen: true, text: '正在测试连接...' })
  try {
    const res = await testDataSourceConnection(sourceId)
    loadingInstance.close()
    if (res.data.data.success) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(`连接失败: ${res.data.data.message}`)
    }
  } catch {
    loadingInstance.close()
  }
}

async function handleSync() {
  try {
    await syncDataSource(sourceId)
    ElMessage.success('同步任务已启动')
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
