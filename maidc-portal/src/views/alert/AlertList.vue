<template>
  <PageContainer title="告警中心">
    <template #extra>
      <a-button type="primary" @click="ruleModal.open()">
        <PlusOutlined /> 新建告警规则
      </a-button>
    </template>

    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="active" tab="活跃告警" />
      <a-tab-pane key="history" tab="历史告警" />
    </a-tabs>

    <a-table :columns="alertColumns" :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'severity'">
          <StatusBadge :status="record.severity" type="alert" />
        </template>
        <template v-if="column.key === 'created_at'">
          {{ formatDateTime(record.created_at) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="viewDetail(record)">详情</a>
            <a v-if="activeTab === 'active' && !record.acknowledged" @click="handleAcknowledge(record.id)">确认</a>
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
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useTable } from '@/hooks/useTable'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'
import { formatDateTime } from '@/utils/date'

const activeTab = ref('active')
const ruleModal = useModal()
const submitting = ref(false)

const alertColumns = [
  { title: '告警名称', dataIndex: 'name', key: 'name' },
  { title: '级别', dataIndex: 'severity', key: 'severity', width: 80 },
  { title: '模型/部署', dataIndex: 'target_name', key: 'target_name' },
  { title: '指标', dataIndex: 'metric_name', key: 'metric_name', width: 120 },
  { title: '当前值', dataIndex: 'current_value', key: 'current_value', width: 100 },
  { title: '触发时间', dataIndex: 'created_at', key: 'created_at', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => request.get('/alerts/records', { params: { page: params.page, page_size: params.pageSize, status: activeTab.value === 'active' ? 'ACTIVE' : undefined } })
)

function handleTabChange() { fetchData() }

const ruleForm = reactive({ name: '', severity: 'WARNING', metric_name: '', operator: 'GT', threshold: 0 })

async function handleCreateRule() {
  submitting.value = true
  try {
    await request.post('/alerts/rules', ruleForm)
    message.success('告警规则创建成功')
    ruleModal.close()
  } finally { submitting.value = false }
}

function viewDetail(record: any) {
  message.info('查看告警详情 #' + record.id)
}

async function handleAcknowledge(id: number) {
  await request.put(`/alerts/records/${id}/acknowledge`)
  message.success('告警已确认')
  fetchData()
}

onMounted(() => fetchData())
</script>
