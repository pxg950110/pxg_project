<template>
  <PageContainer title="临床规则">
    <a-tabs v-model:activeKey="activeTab">
      <!-- Tab 1: Reference Ranges -->
      <a-tab-pane key="refRange" tab="参考范围">
        <!-- Filter bar -->
        <a-card :bordered="false" style="margin-bottom: 16px">
          <a-row :gutter="16" align="middle">
            <a-col :span="5">
              <a-input-number v-model:value="refFilters.conceptId" placeholder="概念ID"
                style="width: 100%" @change="fetchRefRanges" />
            </a-col>
            <a-col :span="4">
              <a-select v-model:value="refFilters.gender" placeholder="性别" allow-clear
                style="width: 100%" @change="fetchRefRanges">
                <a-select-option value="MALE">男</a-select-option>
                <a-select-option value="FEMALE">女</a-select-option>
                <a-select-option value="BOTH">通用</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="6" style="text-align: right">
              <a-button type="primary" @click="handleCreateRefRange">
                <template #icon><PlusOutlined /></template>
                新增参考范围
              </a-button>
            </a-col>
          </a-row>
        </a-card>

        <a-table :columns="refRangeColumns" :data-source="refRanges" :loading="refLoading"
          row-key="id" :pagination="refPagination" @change="handleRefTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'gender'">
              <a-tag :color="record.gender === 'MALE' ? 'blue' : record.gender === 'FEMALE' ? 'pink' : 'green'">
                {{ record.gender === 'MALE' ? '男' : record.gender === 'FEMALE' ? '女' : '通用' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'ageRange'">
              {{ record.ageMin ?? 0 }} - {{ record.ageMax ?? 999 }} {{ record.ageUnit || '岁' }}
            </template>
            <template v-if="column.key === 'range'">
              {{ record.rangeLow ?? '-N/A' }} ~ {{ record.rangeHigh ?? 'N/A' }} {{ record.unit || '' }}
            </template>
            <template v-if="column.key === 'critical'">
              <span v-if="record.criticalLow != null || record.criticalHigh != null">
                {{ record.criticalLow ?? '-' }} / {{ record.criticalHigh ?? '-' }}
              </span>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>

        <!-- Evaluate section -->
        <a-card title="匹配测试" :bordered="false" style="margin-top: 16px" size="small">
          <a-row :gutter="16" align="middle">
            <a-col :span="5">
              <a-input-number v-model:value="evalParams.conceptId" placeholder="概念ID" style="width: 100%" />
            </a-col>
            <a-col :span="3">
              <a-select v-model:value="evalParams.gender" placeholder="性别" style="width: 100%">
                <a-select-option value="MALE">男</a-select-option>
                <a-select-option value="FEMALE">女</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="3">
              <a-input-number v-model:value="evalParams.age" placeholder="年龄" style="width: 100%" />
            </a-col>
            <a-col :span="3">
              <a-input-number v-model:value="evalParams.value" placeholder="检测值" style="width: 100%" />
            </a-col>
            <a-col :span="3">
              <a-button type="primary" @click="handleEvaluate" :loading="evalLoading">匹配测试</a-button>
            </a-col>
          </a-row>
          <div v-if="evalResult" style="margin-top: 12px; padding: 12px; background: #f6ffed; border-radius: 4px">
            <p><strong>匹配结果：</strong>
              <a-tag :color="evalResult.status === 'NORMAL' ? 'green' : evalResult.status === 'ABNORMAL' ? 'red' : 'orange'">
                {{ evalResult.status }}
              </a-tag>
            </p>
            <p v-if="evalResult.referenceRange">参考范围: {{ evalResult.referenceRange.rangeLow }} ~ {{ evalResult.referenceRange.rangeHigh }} {{ evalResult.referenceRange.unit }}</p>
            <p v-if="evalResult.interpretation">{{ evalResult.interpretation }}</p>
          </div>
        </a-card>
      </a-tab-pane>

      <!-- Tab 2: Drug Interactions -->
      <a-tab-pane key="drugInteraction" tab="药物相互作用">
        <!-- Filter bar -->
        <a-card :bordered="false" style="margin-bottom: 16px">
          <a-row :gutter="16" align="middle">
            <a-col :span="6">
              <a-input-number v-model:value="drugFilters.drug1" placeholder="药物1 ID" style="width: 100%" />
            </a-col>
            <a-col :span="6">
              <a-input-number v-model:value="drugFilters.drug2" placeholder="药物2 ID" style="width: 100%" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" @click="fetchDrugInteractions">查询</a-button>
            </a-col>
            <a-col :span="4" style="text-align: right">
              <a-button type="primary" @click="handleCreateDrugInteraction">
                <template #icon><PlusOutlined /></template>
                新增相互作用
              </a-button>
            </a-col>
          </a-row>
        </a-card>

        <a-table :columns="drugColumns" :data-source="drugInteractions" :loading="drugLoading"
          row-key="id" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'severity'">
              <a-tag :color="severityColor(record.severity)">{{ record.severity }}</a-tag>
            </template>
            <template v-if="column.key === 'drug1'">
              {{ record.drug1Name || record.drug1Id }}
            </template>
            <template v-if="column.key === 'drug2'">
              {{ record.drug2Name || record.drug2Id }}
            </template>
          </template>
        </a-table>

        <!-- Prescription check -->
        <a-card title="处方审核" :bordered="false" style="margin-top: 16px" size="small">
          <a-row :gutter="16">
            <a-col :span="16">
              <a-textarea v-model:value="prescriptionDrugs" placeholder="输入药物ID，多个用逗号分隔（如: 1,2,3）" :rows="3" />
            </a-col>
            <a-col :span="4">
              <a-button type="primary" @click="handleCheckDrugList" :loading="checkLoading"
                style="margin-top: 4px">审核</a-button>
            </a-col>
          </a-row>
          <a-table v-if="checkResults.length > 0" :columns="checkColumns" :data-source="checkResults"
            row-key="id" size="small" :pagination="false" style="margin-top: 12px">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'severity'">
                <a-tag :color="severityColor(record.severity)">{{ record.severity }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- Create Reference Range Modal -->
    <a-modal v-model:open="refModalVisible" title="新增参考范围" :width="640"
      @ok="handleRefSubmit" @cancel="refModalVisible = false" destroy-on-close>
      <a-form ref="refFormRef" :model="refFormState" :rules="refFormRules"
        :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="概念ID" name="conceptId">
          <a-input-number v-model:value="refFormState.conceptId" style="width: 100%" />
        </a-form-item>
        <a-form-item label="性别" name="gender">
          <a-select v-model:value="refFormState.gender">
            <a-select-option value="MALE">男</a-select-option>
            <a-select-option value="FEMALE">女</a-select-option>
            <a-select-option value="BOTH">通用</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="8">
          <a-col :span="12">
            <a-form-item label="最小年龄" name="ageMin" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
              <a-input-number v-model:value="refFormState.ageMin" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="最大年龄" name="ageMax" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
              <a-input-number v-model:value="refFormState.ageMax" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="8">
          <a-col :span="12">
            <a-form-item label="参考下限" name="rangeLow" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
              <a-input-number v-model:value="refFormState.rangeLow" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="参考上限" name="rangeHigh" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
              <a-input-number v-model:value="refFormState.rangeHigh" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="单位" name="unit">
          <a-input v-model:value="refFormState.unit" placeholder="如 mg/dL, mmol/L" />
        </a-form-item>
        <a-row :gutter="8">
          <a-col :span="12">
            <a-form-item label="危急低下限" name="criticalLow" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
              <a-input-number v-model:value="refFormState.criticalLow" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="危急高上限" name="criticalHigh" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
              <a-input-number v-model:value="refFormState.criticalHigh" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- Create Drug Interaction Modal -->
    <a-modal v-model:open="drugModalVisible" title="新增药物相互作用" :width="600"
      @ok="handleDrugSubmit" @cancel="drugModalVisible = false" destroy-on-close>
      <a-form ref="drugFormRef" :model="drugFormState" :rules="drugFormRules"
        :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="药物1 ID" name="drug1Id">
          <a-input-number v-model:value="drugFormState.drug1Id" style="width: 100%" />
        </a-form-item>
        <a-form-item label="药物2 ID" name="drug2Id">
          <a-input-number v-model:value="drugFormState.drug2Id" style="width: 100%" />
        </a-form-item>
        <a-form-item label="严重程度" name="severity">
          <a-select v-model:value="drugFormState.severity">
            <a-select-option value="MINOR">轻微 (MINOR)</a-select-option>
            <a-select-option value="MODERATE">中等 (MODERATE)</a-select-option>
            <a-select-option value="SEVERE">严重 (SEVERE)</a-select-option>
            <a-select-option value="CONTRAINDICATED">禁忌 (CONTRAINDICATED)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="drugFormState.description" :rows="3" />
        </a-form-item>
        <a-form-item label="临床建议" name="clinicalAction">
          <a-textarea v-model:value="drugFormState.clinicalAction" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getReferenceRanges, evaluateReferenceRange, createReferenceRange,
  getDrugInteractions, checkDrugInteraction, checkDrugList, createDrugInteraction,
} from '@/api/masterdata'

defineOptions({ name: 'ClinicalRules' })

const activeTab = ref('refRange')

// ====== Reference Ranges ======
const refFilters = reactive({ conceptId: undefined as number | undefined, gender: undefined as string | undefined })
const refLoading = ref(false)
const refRanges = ref<any[]>([])
const refPagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })

const refRangeColumns = [
  { title: '概念ID', dataIndex: 'conceptId', key: 'conceptId', width: 90 },
  { title: '性别', key: 'gender', width: 80 },
  { title: '年龄范围', key: 'ageRange', width: 140 },
  { title: '参考范围', key: 'range', width: 180 },
  { title: '危急值', key: 'critical', width: 120 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
]

async function fetchRefRanges() {
  refLoading.value = true
  try {
    const params: any = {}
    if (refFilters.conceptId) params.conceptId = refFilters.conceptId
    if (refFilters.gender) params.gender = refFilters.gender
    const res = await getReferenceRanges(params)
    refRanges.value = res.data.data || []
  } finally {
    refLoading.value = false
  }
}

function handleRefTableChange(pag: any) {
  refPagination.current = pag.current
  refPagination.pageSize = pag.pageSize
  fetchRefRanges()
}

// Evaluate
const evalParams = reactive({ conceptId: undefined as number | undefined, gender: undefined as string | undefined, age: undefined as number | undefined, value: undefined as number | undefined })
const evalLoading = ref(false)
const evalResult = ref<any>(null)

async function handleEvaluate() {
  if (!evalParams.conceptId || !evalParams.gender || evalParams.age == null || evalParams.value == null) {
    message.warning('请填写所有测试参数')
    return
  }
  evalLoading.value = true
  try {
    const res = await evaluateReferenceRange(evalParams)
    evalResult.value = res.data.data
  } catch {
    evalResult.value = null
  } finally {
    evalLoading.value = false
  }
}

// Create reference range modal
const refModalVisible = ref(false)
const refFormRef = ref<FormInstance>()
const refFormState = reactive({
  conceptId: undefined as number | undefined,
  gender: 'BOTH',
  ageMin: undefined as number | undefined,
  ageMax: undefined as number | undefined,
  rangeLow: undefined as number | undefined,
  rangeHigh: undefined as number | undefined,
  unit: '',
  criticalLow: undefined as number | undefined,
  criticalHigh: undefined as number | undefined,
})
const refFormRules: Record<string, Rule[]> = {
  conceptId: [{ required: true, message: '请输入概念ID' }],
  gender: [{ required: true, message: '请选择性别' }],
  rangeLow: [{ required: true, message: '请输入参考下限' }],
  rangeHigh: [{ required: true, message: '请输入参考上限' }],
}

function handleCreateRefRange() {
  Object.assign(refFormState, { conceptId: undefined, gender: 'BOTH', ageMin: undefined, ageMax: undefined, rangeLow: undefined, rangeHigh: undefined, unit: '', criticalLow: undefined, criticalHigh: undefined })
  refModalVisible.value = true
}

async function handleRefSubmit() {
  await refFormRef.value?.validateFields()
  await createReferenceRange(refFormState)
  message.success('创建成功')
  refModalVisible.value = false
  fetchRefRanges()
}

// ====== Drug Interactions ======
const drugFilters = reactive({ drug1: undefined as number | undefined, drug2: undefined as number | undefined })
const drugLoading = ref(false)
const drugInteractions = ref<any[]>([])

const drugColumns = [
  { title: '药物1', key: 'drug1', width: 150 },
  { title: '药物2', key: 'drug2', width: 150 },
  { title: '严重程度', key: 'severity', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '临床建议', dataIndex: 'clinicalAction', key: 'clinicalAction', width: 200, ellipsis: true },
]

function severityColor(severity: string) {
  const colors: Record<string, string> = { MINOR: 'blue', MODERATE: 'orange', SEVERE: 'red', CONTRAINDICATED: 'purple' }
  return colors[severity] || 'default'
}

async function fetchDrugInteractions() {
  if (!drugFilters.drug1 || !drugFilters.drug2) {
    message.warning('请输入两个药物ID')
    return
  }
  drugLoading.value = true
  try {
    const res = await checkDrugInteraction(drugFilters.drug1, drugFilters.drug2)
    drugInteractions.value = res.data.data || []
  } finally {
    drugLoading.value = false
  }
}

// Prescription check
const prescriptionDrugs = ref('')
const checkLoading = ref(false)
const checkResults = ref<any[]>([])
const checkColumns = [
  { title: '药物1', dataIndex: 'drug1Name', key: 'drug1Name', width: 120 },
  { title: '药物2', dataIndex: 'drug2Name', key: 'drug2Name', width: 120 },
  { title: '严重程度', key: 'severity', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
]

async function handleCheckDrugList() {
  const ids = prescriptionDrugs.value.split(',').map(s => Number(s.trim())).filter(n => !isNaN(n))
  if (ids.length < 2) {
    message.warning('请至少输入2个药物ID')
    return
  }
  checkLoading.value = true
  try {
    const res = await checkDrugList(ids)
    checkResults.value = res.data.data || []
  } finally {
    checkLoading.value = false
  }
}

// Create drug interaction modal
const drugModalVisible = ref(false)
const drugFormRef = ref<FormInstance>()
const drugFormState = reactive({
  drug1Id: undefined as number | undefined,
  drug2Id: undefined as number | undefined,
  severity: 'MODERATE',
  description: '',
  clinicalAction: '',
})
const drugFormRules: Record<string, Rule[]> = {
  drug1Id: [{ required: true, message: '请输入药物1 ID' }],
  drug2Id: [{ required: true, message: '请输入药物2 ID' }],
  severity: [{ required: true, message: '请选择严重程度' }],
}

function handleCreateDrugInteraction() {
  Object.assign(drugFormState, { drug1Id: undefined, drug2Id: undefined, severity: 'MODERATE', description: '', clinicalAction: '' })
  drugModalVisible.value = true
}

async function handleDrugSubmit() {
  await drugFormRef.value?.validateFields()
  await createDrugInteraction(drugFormState)
  message.success('创建成功')
  drugModalVisible.value = false
}
</script>
