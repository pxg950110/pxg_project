<template>
  <PageContainer title="编码映射">
    <!-- Filter bar -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-row :gutter="16" align="middle">
        <a-col :span="7">
          <span style="margin-right: 8px">源编码体系:</span>
          <a-select v-model:value="sourceSystemId" placeholder="选择源体系" allow-clear
            style="width: calc(100% - 100px)" @change="fetchMappings">
            <a-select-option v-for="cs in codeSystems" :key="cs.id" :value="cs.id">{{ cs.name }}</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="7">
          <span style="margin-right: 8px">目标编码体系:</span>
          <a-select v-model:value="targetSystemId" placeholder="选择目标体系" allow-clear
            style="width: calc(100% - 100px)" @change="fetchMappings">
            <a-select-option v-for="cs in codeSystems" :key="cs.id" :value="cs.id">{{ cs.name }}</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="10" style="text-align: right">
          <a-button type="primary" @click="handleCreateMapping" :disabled="!sourceSystemId || !targetSystemId">
            <template #icon><PlusOutlined /></template>
            新增映射
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- Mappings table -->
    <a-table :columns="columns" :data-source="mappings" :loading="loading"
      row-key="id" :pagination="pagination">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'mappingType'">
          <a-tag :color="mappingTypeColor(record.mappingType)">{{ record.mappingType }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除此映射？" @confirm="handleDelete(record)">
            <a-button type="link" danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <!-- Create mapping modal -->
    <a-modal v-model:open="modalVisible" title="新增映射" :width="600"
      @ok="handleSubmit" @cancel="handleModalCancel" destroy-on-close>
      <a-form ref="formRef" :model="formState" :rules="formRules"
        :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="源概念ID" name="sourceConceptId">
          <a-input-number v-model:value="formState.sourceConceptId" placeholder="输入源概念ID" style="width: 100%" />
        </a-form-item>
        <a-form-item label="目标概念ID" name="targetConceptId">
          <a-input-number v-model:value="formState.targetConceptId" placeholder="输入目标概念ID" style="width: 100%" />
        </a-form-item>
        <a-form-item label="映射类型" name="mappingType">
          <a-select v-model:value="formState.mappingType" placeholder="选择映射类型">
            <a-select-option value="SAME_AS">相同 (SAME_AS)</a-select-option>
            <a-select-option value="BROADER_THAN">更宽 (BROADER_THAN)</a-select-option>
            <a-select-option value="NARROWER_THAN">更窄 (NARROWER_THAN)</a-select-option>
            <a-select-option value="CLOSE_ENOUGH">近似 (CLOSE_ENOUGH)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="置信度" name="confidence">
          <a-slider v-model:value="formState.confidence" :min="0" :max="100" :step="5"
            :marks="{ 0: '0%', 50: '50%', 100: '100%' }" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import { getCodeSystems, getConceptMappings, createMapping, deleteMapping } from '@/api/masterdata'

defineOptions({ name: 'MappingManager' })

const codeSystems = ref<any[]>([])
const sourceSystemId = ref<number | undefined>(undefined)
const targetSystemId = ref<number | undefined>(undefined)

const loading = ref(false)
const mappings = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })

const columns = [
  { title: '源概念编码', dataIndex: 'sourceConceptCode', key: 'sourceConceptCode', width: 150 },
  { title: '源概念名称', dataIndex: 'sourceConceptName', key: 'sourceConceptName', width: 180, ellipsis: true },
  { title: '目标概念编码', dataIndex: 'targetConceptCode', key: 'targetConceptCode', width: 150 },
  { title: '目标概念名称', dataIndex: 'targetConceptName', key: 'targetConceptName', width: 180, ellipsis: true },
  { title: '映射类型', key: 'mappingType', width: 120 },
  { title: '置信度', dataIndex: 'confidence', key: 'confidence', width: 80, customRender: ({ text }: any) => text != null ? `${(text * 100).toFixed(0)}%` : '-' },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

function mappingTypeColor(type: string) {
  const colors: Record<string, string> = { SAME_AS: 'green', BROADER_THAN: 'blue', NARROWER_THAN: 'orange', CLOSE_ENOUGH: 'purple' }
  return colors[type] || 'default'
}

async function fetchCodeSystems() {
  try {
    const res = await getCodeSystems()
    codeSystems.value = res.data.data || []
  } catch { /* ignore */ }
}

async function fetchMappings() {
  if (!sourceSystemId.value || !targetSystemId.value) {
    mappings.value = []
    return
  }
  loading.value = true
  try {
    // Fetch concepts for source system and then their mappings
    const res = await getConceptMappings(sourceSystemId.value, {
      targetCodeSystemId: targetSystemId.value,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    mappings.value = res.data.data || []
    // Note: if the API returns a page, adjust accordingly
  } catch {
    mappings.value = []
  } finally {
    loading.value = false
  }
}

// Modal
const modalVisible = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  sourceConceptId: undefined as number | undefined,
  targetConceptId: undefined as number | undefined,
  mappingType: 'SAME_AS',
  confidence: 100,
})

const formRules: Record<string, Rule[]> = {
  sourceConceptId: [{ required: true, message: '请输入源概念ID' }],
  targetConceptId: [{ required: true, message: '请输入目标概念ID' }],
  mappingType: [{ required: true, message: '请选择映射类型' }],
}

function handleCreateMapping() {
  Object.assign(formState, { sourceConceptId: undefined, targetConceptId: undefined, mappingType: 'SAME_AS', confidence: 100 })
  modalVisible.value = true
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  await createMapping({
    sourceConceptId: formState.sourceConceptId,
    targetConceptId: formState.targetConceptId,
    mappingType: formState.mappingType,
    confidence: formState.confidence / 100,
    sourceCodeSystemId: sourceSystemId.value,
    targetCodeSystemId: targetSystemId.value,
  })
  message.success('映射创建成功')
  handleModalCancel()
  fetchMappings()
}

async function handleDelete(record: any) {
  await deleteMapping(record.id)
  message.success('删除成功')
  fetchMappings()
}

onMounted(() => fetchCodeSystems())
</script>
