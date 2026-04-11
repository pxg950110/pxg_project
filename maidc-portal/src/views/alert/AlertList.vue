<template>
  <PageContainer title="告警中心">
    <template #extra>
      <a-button type="primary" @click="ruleModal.open()">
        <PlusOutlined /> 新建告警规则
      </a-button>
    </template>

    <!-- Metric Cards Row -->
    <a-row :gutter="[16, 16]" class="metric-row">
      <a-col :span="6">
        <MetricCard title="活跃告警" :value="12" suffix="个">
          <template #icon><AlertOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard title="今日已处理" :value="34" suffix="个">
          <template #icon><CheckCircleOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard title="平均响应" value="8分钟">
          <template #icon><ClockCircleOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard title="告警规则" :value="12" suffix="条">
          <template #icon><SettingOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Tabs -->
    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="active" tab="活跃告警" />
      <a-tab-pane key="history" tab="历史告警" />
    </a-tabs>

    <!-- Alert Table -->
    <a-table :columns="alertColumns" :data-source="filteredAlerts" row-key="id" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <span style="font-weight: 600">{{ record.name }}</span>
        </template>
        <template v-if="column.key === 'severity'">
          <a-tag :color="severityColors[record.severity]">{{ record.severity_label }}</a-tag>
        </template>
        <template v-if="column.key === 'threshold_info'">
          <span>{{ record.current_value }} / {{ record.threshold }}</span>
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'status'">
          <a-badge :status="statusBadgeMap[record.status]" :text="record.status_label" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewDetail(record)">详情</a>
            <a v-if="record.status === 'FIRING'" @click="handleAcknowledge(record)">确认</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- Create Rule Modal -->
    <a-modal v-model:open="ruleModal.visible" title="新建告警规则" @ok="handleCreateRule" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="规则名称" required>
          <a-input v-model:value="ruleForm.name" />
        </a-form-item>
        <a-form-item label="告警级别" required>
          <a-select v-model:value="ruleForm.severity">
            <a-select-option value="INFO">提示</a-select-option>
            <a-select-option value="WARNING">警告</a-select-option>
            <a-select-option value="CRITICAL">严重</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="指标" required>
          <a-select v-model:value="ruleForm.metric_name">
            <a-select-option value="inference_latency">推理延迟</a-select-option>
            <a-select-option value="error_rate">错误率</a-select-option>
            <a-select-option value="gpu_usage">GPU利用率</a-select-option>
            <a-select-option value="memory_usage">内存使用</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="条件">
              <a-select v-model:value="ruleForm.operator">
                <a-select-option value="GT">大于</a-select-option>
                <a-select-option value="LT">小于</a-select-option>
                <a-select-option value="GTE">大于等于</a-select-option>
                <a-select-option value="LTE">小于等于</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="阈值">
              <a-input-number v-model:value="ruleForm.threshold" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import {
  PlusOutlined,
  AlertOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import { useModal } from '@/hooks/useModal'
import { formatDateTime } from '@/utils/date'

// ============ State ============
const activeTab = ref('active')
const ruleModal = useModal()
const submitting = ref(false)

// ============ Color Maps ============
const severityColors: Record<string, string> = {
  CRITICAL: 'red',
  WARNING: 'orange',
  INFO: 'blue',
}

const statusBadgeMap: Record<string, string> = {
  FIRING: 'error',
  ACKNOWLEDGED: 'warning',
  RESOLVED: 'success',
}

// ============ Table Columns ============
const alertColumns = [
  { title: '告警名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '级别', dataIndex: 'severity', key: 'severity', width: 90 },
  { title: '关联资源', dataIndex: 'target_name', key: 'target_name', width: 160 },
  { title: '指标', dataIndex: 'metric_name', key: 'metric_name', width: 120 },
  { title: '当前值/阈值', key: 'threshold_info', width: 140 },
  { title: '触发时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 100 },
]

// ============ Mock Data ============
interface AlertRecord {
  id: number
  name: string
  severity: string
  severity_label: string
  target_name: string
  metric_name: string
  current_value: string
  threshold: string
  status: string
  status_label: string
  created_at: string
  acknowledged: boolean
}

const mockAlerts = ref<AlertRecord[]>([
  { id: 1, name: '推理延迟过高', severity: 'CRITICAL', severity_label: '严重', target_name: '肺结节检测-v2', metric_name: '推理延迟', current_value: '850ms', threshold: '>500ms', status: 'FIRING', status_label: '触发中', created_at: '2026-04-12 10:30:00', acknowledged: false },
  { id: 2, name: 'GPU内存使用率告警', severity: 'WARNING', severity_label: '警告', target_name: 'GPU-Node-03', metric_name: 'GPU利用率', current_value: '92%', threshold: '>85%', status: 'FIRING', status_label: '触发中', created_at: '2026-04-12 10:15:00', acknowledged: false },
  { id: 3, name: '错误率异常', severity: 'WARNING', severity_label: '警告', target_name: '心电图分析-v1', metric_name: '错误率', current_value: '5.2%', threshold: '>3%', status: 'FIRING', status_label: '触发中', created_at: '2026-04-12 09:45:00', acknowledged: false },
  { id: 4, name: '模型服务不可用', severity: 'CRITICAL', severity_label: '严重', target_name: '病理分类-v3', metric_name: '可用性', current_value: '0%', threshold: '>99%', status: 'ACKNOWLEDGED', status_label: '已确认', created_at: '2026-04-12 09:20:00', acknowledged: true },
  { id: 5, name: '数据库连接池耗尽', severity: 'CRITICAL', severity_label: '严重', target_name: 'DB-Primary', metric_name: '连接数', current_value: '198/200', threshold: '200', status: 'RESOLVED', status_label: '已恢复', created_at: '2026-04-12 08:50:00', acknowledged: true },
  { id: 6, name: 'API响应超时', severity: 'WARNING', severity_label: '警告', target_name: 'Gateway', metric_name: '响应时间', current_value: '3200ms', threshold: '>3000ms', status: 'RESOLVED', status_label: '已恢复', created_at: '2026-04-12 08:30:00', acknowledged: true },
])

// ============ Computed: Filter by Tab ============
const filteredAlerts = computed(() => {
  if (activeTab.value === 'active') {
    return mockAlerts.value.filter(a => a.status === 'FIRING' || a.status === 'ACKNOWLEDGED')
  }
  return mockAlerts.value.filter(a => a.status === 'RESOLVED')
})

// ============ Actions ============
function viewDetail(record: AlertRecord) {
  message.info('查看告警详情 #' + record.id)
}

function handleAcknowledge(record: AlertRecord) {
  record.status = 'ACKNOWLEDGED'
  record.status_label = '已确认'
  record.acknowledged = true
  message.success('告警已确认')
}

// ============ Create Rule ============
const ruleForm = reactive({ name: '', severity: 'WARNING', metric_name: '', operator: 'GT', threshold: 0 })

async function handleCreateRule() {
  submitting.value = true
  try {
    // Mock: simulate API call
    await new Promise(resolve => setTimeout(resolve, 300))
    message.success('告警规则创建成功')
    ruleModal.close()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.metric-row {
  margin-bottom: 20px;
}
</style>
