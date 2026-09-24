<template>
  <PageContainer title="告警规则">
    <template #extra>
      <el-button type="primary" @click="ruleModal.open()">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建规则
      </el-button>
    </template>

    <el-table :data="rules" row-key="id" v-loading="loading">
      <el-table-column label="规则名称" prop="name" min-width="180" />
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.severity" type="alert" />
        </template>
      </el-table-column>
      <el-table-column label="指标" prop="metric_name" width="140" />
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" @change="(v: string | number | boolean) => handleToggle(row, Boolean(v))" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="ruleModal.visible" :title="editingId ? '编辑规则' : '新建规则'" width="600px">
      <el-form label-position="top">
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" />
        </el-form-item>
        <el-form-item label="告警级别">
          <el-select v-model="ruleForm.severity" style="width: 100%">
            <el-option label="提示" value="INFO" />
            <el-option label="警告" value="WARNING" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="指标">
          <el-select v-model="ruleForm.metric_name" style="width: 100%">
            <el-option label="推理延迟" value="inference_latency" />
            <el-option label="错误率" value="error_rate" />
            <el-option label="GPU利用率" value="gpu_usage" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="条件">
              <el-select v-model="ruleForm.operator" style="width: 100%">
                <el-option label="大于" value="GT" />
                <el-option label="小于" value="LT" />
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
        <el-button type="primary" :loading="submitting" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '@/components/PageContainer/index.vue'
import StatusBadge from '@/components/StatusBadge/index.vue'
import { useModal } from '@/hooks/useModal'
import request from '@/utils/request'

const ruleModal = useModal()
const submitting = ref(false)
const loading = ref(false)
const rules = ref<any[]>([])
const editingId = ref<number | null>(null)

const ruleForm = reactive({ name: '', severity: 'WARNING', metric_name: '', operator: 'GT', threshold: 0 })

async function loadRules() {
  loading.value = true
  try { const res = await request.get('/alert-rules'); rules.value = res.data.data || [] }
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
    if (editingId.value) await request.put(`/alert-rules/${editingId.value}`, ruleForm)
    else await request.post('/alert-rules', ruleForm)
    ElMessage.success('保存成功')
    ruleModal.close()
    editingId.value = null
    loadRules()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await request.delete(`/alert-rules/${id}`)
  ElMessage.success('规则已删除')
  loadRules()
}

async function handleToggle(record: any, enabled: boolean) {
  await request.put(`/alert-rules/${record.id}`, { ...record, enabled })
  record.enabled = enabled
}

onMounted(loadRules)
</script>
