<template>
  <PageContainer title="告警规则">
    <template #extra>
      <a-button type="primary" @click="ruleModal.open()">
        <PlusOutlined /> 新建规则
      </a-button>
    </template>

    <a-table :columns="columns" :data-source="rules" :loading="loading" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'severity'">
          <StatusBadge :status="record.severity" type="alert" />
        </template>
        <template v-if="column.key === 'enabled'">
          <a-switch :checked="record.enabled" @change="(v: boolean) => handleToggle(record, v)" />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openEdit(record)">编辑</a>
            <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
              <a class="danger-link">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="ruleModal.visible" :title="editingId ? '编辑规则' : '新建规则'" @ok="handleSave" :confirm-loading="submitting" width="600px">
      <a-form layout="vertical">
        <a-form-item label="规则名称" required><a-input v-model:value="ruleForm.name" /></a-form-item>
        <a-form-item label="告警级别"><a-select v-model:value="ruleForm.severity">
          <a-select-option value="INFO">提示</a-select-option>
          <a-select-option value="WARNING">警告</a-select-option>
          <a-select-option value="CRITICAL">严重</a-select-option>
        </a-select></a-form-item>
        <a-form-item label="指标"><a-select v-model:value="ruleForm.metric_name">
          <a-select-option value="inference_latency">推理延迟</a-select-option>
          <a-select-option value="error_rate">错误率</a-select-option>
          <a-select-option value="gpu_usage">GPU利用率</a-select-option>
        </a-select></a-form-item>
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="条件"><a-select v-model:value="ruleForm.operator">
            <a-select-option value="GT">大于</a-select-option><a-select-option value="LT">小于</a-select-option>
          </a-select></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="阈值"><a-input-number v-model:value="ruleForm.threshold" style="width:100%" /></a-form-item></a-col>
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
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

const ruleModal = useModal()
const submitting = ref(false)
const loading = ref(false)
const rules = ref<any[]>([])
const editingId = ref<number | null>(null)

const columns = [
  { title: '规则名称', dataIndex: 'name', key: 'name' },
  { title: '级别', dataIndex: 'severity', key: 'severity', width: 80 },
  { title: '指标', dataIndex: 'metric_name', key: 'metric_name', width: 120 },
  { title: '条件', key: 'condition', width: 100 },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 80 },
  { title: '操作', key: 'action', width: 120 },
]

const ruleForm = reactive({ name: '', severity: 'WARNING', metric_name: '', operator: 'GT', threshold: 0 })

async function loadRules() {
  loading.value = true
  try { const res = await request.get('/alerts/rules'); rules.value = res.data.data || [] }
  finally { loading.value = false }
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(ruleForm, record)
  ruleModal.open()
}

async function handleSave() {
  submitting.value = true
  try {
    if (editingId.value) await request.put(`/alerts/rules/${editingId.value}`, ruleForm)
    else await request.post('/alerts/rules', ruleForm)
    message.success('保存成功')
    ruleModal.close()
    editingId.value = null
    loadRules()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await request.delete(`/alerts/rules/${id}`)
  message.success('规则已删除')
  loadRules()
}

async function handleToggle(record: any, enabled: boolean) {
  await request.put(`/alerts/rules/${record.id}`, { ...record, enabled })
  record.enabled = enabled
}

onMounted(loadRules)
</script>

<style scoped>
.danger-link { color: #ff4d4f; }
</style>
