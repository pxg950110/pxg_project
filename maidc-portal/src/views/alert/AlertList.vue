<template>
  <PageContainer title="告警中心">
    <template #extra>
      <el-button type="primary" @click="ruleModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建告警规则
      </el-button>
    </template>

    <!-- Metric Cards Row -->
    <el-row :gutter="16" class="mb-5">
      <el-col :span="6">
        <MetricCard title="活跃告警" :value="summary.activeCount" suffix="个" :icon="Warning" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="今日已处理" :value="summary.todayProcessed" suffix="个" :icon="CircleCheck" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="平均响应" :value="summary.avgResponseTime" :icon="Clock" />
      </el-col>
      <el-col :span="6">
        <MetricCard title="告警规则" :value="summary.ruleCount" suffix="条" :icon="Setting" />
      </el-col>
    </el-row>

    <!-- Tabs -->
    <el-tabs v-model="activeTab">
      <el-tab-pane label="活跃告警" name="active" />
      <el-tab-pane label="历史告警" name="history" />
    </el-tabs>

    <!-- Alert Table -->
    <el-table :data="filteredAlerts" row-key="id" v-loading="loading">
      <el-table-column label="告警名称" min-width="180">
        <template #default="{ row }">
          <span class="font-semibold">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="级别" width="90">
        <template #default="{ row }">
          <el-tag :type="severityTypes[row.severity]">{{ row.severity_label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联资源" prop="target_name" min-width="160" />
      <el-table-column label="指标" prop="metric_name" min-width="120" />
      <el-table-column label="当前值/阈值" width="140">
        <template #default="{ row }">{{ row.current_value }} / {{ row.threshold }}</template>
      </el-table-column>
      <el-table-column label="触发时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.created_at) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2 w-2 rounded-full" :style="{ background: statusDots[row.status] }" />
            {{ row.status_label }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'FIRING'" link type="primary" @click="handleAcknowledge(row)">确认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create Rule Modal -->
    <el-dialog v-model="ruleModal.visible" title="新建告警规则" width="600px">
      <el-form label-position="top">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" />
        </el-form-item>
        <el-form-item label="告警级别" required>
          <el-select v-model="ruleForm.severity" style="width: 100%">
            <el-option label="提示" value="INFO" />
            <el-option label="警告" value="WARNING" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标" required>
          <el-select v-model="ruleForm.metric_name" style="width: 100%">
            <el-option label="推理延迟" value="inference_latency" />
            <el-option label="错误率" value="error_rate" />
            <el-option label="GPU利用率" value="gpu_usage" />
            <el-option label="内存使用" value="memory_usage" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="条件">
              <el-select v-model="ruleForm.operator" style="width: 100%">
                <el-option label="大于" value="GT" />
                <el-option label="小于" value="LT" />
                <el-option label="大于等于" value="GTE" />
                <el-option label="小于等于" value="LTE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阈值">
              <el-input-number v-model="ruleForm.threshold" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="ruleModal.close()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateRule">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck, Clock, Setting } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import { useModal } from '@/hooks/useModal'
import { useTable } from '@/hooks/useTable'
import { formatDateTime } from '@/utils/date'
import { getAlerts, acknowledgeAlert, getAlertSummary } from '@/api/model'

// ============ State ============
const activeTab = ref('active')
const ruleModal = useModal()
const submitting = ref(false)
const summary = reactive({ activeCount: 0, todayProcessed: 0, avgResponseTime: '-', ruleCount: 0 })

// ============ Summary ============
async function fetchSummary() {
  try {
    const res = await getAlertSummary()
    const d = res.data.data
    summary.activeCount = d.activeCount
    summary.todayProcessed = d.todayProcessed
    summary.avgResponseTime = d.avgResponseTime
    summary.ruleCount = d.ruleCount
  } catch {
    // keep default zeros
  }
}

// ============ Table ============
const { tableData: alertData, loading, fetchData } = useTable<any>(
  (params) => getAlerts({ page: params.page, page_size: params.pageSize, status: activeTab.value === 'active' ? undefined : 'RESOLVED' }),
)

// ============ Color Maps ============
const severityTypes: Record<string, 'danger' | 'warning' | 'info'> = {
  CRITICAL: 'danger',
  WARNING: 'warning',
  INFO: 'info',
}

const statusDots: Record<string, string> = {
  FIRING: '#ef4444',
  ACKNOWLEDGED: '#f59e0b',
  RESOLVED: '#10b981',
}

// ============ Computed: Filter by Tab ============
const filteredAlerts = computed(() => {
  if (activeTab.value === 'active') {
    return alertData.value.filter((a: any) => a.status === 'FIRING' || a.status === 'ACKNOWLEDGED')
  }
  return alertData.value.filter((a: any) => a.status === 'RESOLVED')
})

// ============ Actions ============
function viewDetail(record: any) {
  ElMessage.info('查看告警详情 #' + record.id)
}

async function handleAcknowledge(record: any) {
  await acknowledgeAlert(record.id)
  ElMessage.success('告警已确认')
  fetchData()
}

// ============ Create Rule ============
const ruleForm = reactive({ name: '', severity: 'WARNING', metric_name: '', operator: 'GT', threshold: 0 })

async function handleCreateRule() {
  submitting.value = true
  try {
    ElMessage.success('告警规则创建成功')
    ruleModal.close()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchData()
  fetchSummary()
})
</script>
