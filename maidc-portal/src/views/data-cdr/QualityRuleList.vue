<template>
  <PageContainer title="数据质量规则">
    <template #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新建规则
      </a-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ruleType'">
          <a-tag :color="ruleTypeColorMap[record.rule_type] || 'default'">
            {{ ruleTypeMap[record.rule_type] || record.rule_type }}
          </a-tag>
        </template>
        <template v-if="column.key === 'threshold'">
          {{ record.threshold }}%
        </template>
        <template v-if="column.key === 'status'">
          <a-switch
            :checked="record.status === 'ENABLED'"
            checked-children="启用"
            un-checked-children="禁用"
            @change="(checked: boolean) => handleToggle(record, checked)"
          />
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm title="确定删除此规则？" @confirm="handleDelete(record)">
              <a-button type="link" danger size="small">删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑规则弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑规则' : '新建规则'"
      :confirm-loading="submitLoading"
      :width="700"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
      destroy-on-close
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="规则名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入规则名称" />
        </a-form-item>

        <a-form-item label="规则类型" name="rule_type">
          <a-select v-model:value="formState.rule_type" placeholder="请选择规则类型" @change="handleRuleTypeChange">
            <a-select-option v-for="item in ruleTypeOptions" :key="item.value" :value="item.value">
              <div>
                <div>{{ item.label }}</div>
                <div style="font-size: 12px; color: rgba(0,0,0,0.45)">{{ item.description }}</div>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="目标表" name="target_table">
          <a-select
            v-model:value="formState.target_table"
            placeholder="请选择目标表"
            show-search
            :options="tableOptions"
            :filter-option="filterOption"
          />
        </a-form-item>

        <a-form-item label="目标字段" name="target_field">
          <a-select
            v-model:value="formState.target_field"
            placeholder="请选择目标字段"
            show-search
            :options="fieldOptions"
            :filter-option="filterOption"
          />
        </a-form-item>

        <!-- 完整性规则配置 -->
        <template v-if="formState.rule_type === 'completeness'">
          <a-form-item label="空值检查" name="check_null">
            <a-switch v-model:checked="formState.check_null" checked-children="检查空值" />
          </a-form-item>
          <a-form-item label="空字符串检查" name="check_empty">
            <a-switch v-model:checked="formState.check_empty" checked-children="检查空字符串" />
          </a-form-item>
        </template>

        <!-- 准确性规则配置 -->
        <template v-if="formState.rule_type === 'accuracy'">
          <a-form-item label="校验表达式" name="expression">
            <a-textarea
              v-model:value="formState.expression"
              placeholder="如: value > 0 AND value < 200 (支持的变量: value, record)"
              :rows="2"
            />
          </a-form-item>
        </template>

        <!-- 一致性规则配置 -->
        <template v-if="formState.rule_type === 'consistency'">
          <a-form-item label="参照表" name="reference_table">
            <a-select
              v-model:value="formState.reference_table"
              placeholder="选择参照表"
              show-search
              :options="tableOptions"
              :filter-option="filterOption"
            />
          </a-form-item>
          <a-form-item label="参照字段" name="reference_field">
            <a-input v-model:value="formState.reference_field" placeholder="参照表中用于比对的字段" />
          </a-form-item>
        </template>

        <!-- 及时性规则配置 -->
        <template v-if="formState.rule_type === 'timeliness'">
          <a-form-item label="时间阈值(小时)" name="time_threshold">
            <a-input-number v-model:value="formState.time_threshold" :min="1" placeholder="数据最大延迟时间" style="width: 100%" />
          </a-form-item>
        </template>

        <a-form-item label="阈值(%)" name="threshold">
          <a-slider v-model:value="formState.threshold" :min="0" :max="100" :step="1" :marks="{ 0: '0%', 70: '70%', 90: '90%', 100: '100%' }" />
        </a-form-item>

        <a-form-item label="优先级" name="priority">
          <a-select v-model:value="formState.priority" placeholder="请选择优先级">
            <a-select-option value="HIGH"><a-tag color="red">高</a-tag></a-select-option>
            <a-select-option value="MEDIUM"><a-tag color="orange">中</a-tag></a-select-option>
            <a-select-option value="LOW"><a-tag color="blue">低</a-tag></a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="规则描述（选填）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getQualityRules,
  createQualityRule,
  updateQualityRule,
  deleteQualityRule,
  toggleQualityRule,
} from '@/api/data'

defineOptions({ name: 'QualityRuleList' })

// ===== 常量 =====
const ruleTypeMap: Record<string, string> = {
  completeness: '完整性',
  accuracy: '准确性',
  consistency: '一致性',
  timeliness: '及时性',
}

const ruleTypeColorMap: Record<string, string> = {
  completeness: 'blue',
  accuracy: 'green',
  consistency: 'purple',
  timeliness: 'orange',
}

const ruleTypeOptions = [
  { value: 'completeness', label: '完整性', description: '检查数据是否存在空值、缺失等问题' },
  { value: 'accuracy', label: '准确性', description: '检查数据值是否在合理范围内' },
  { value: 'consistency', label: '一致性', description: '检查不同表/字段间的数据是否一致' },
  { value: 'timeliness', label: '及时性', description: '检查数据更新是否及时' },
]

// 模拟表/字段选项（实际从后端获取）
const tableOptions = [
  { value: 'cdr_patient', label: '患者信息表' },
  { value: 'cdr_encounter', label: '就诊记录表' },
  { value: 'cdr_diagnosis', label: '诊断信息表' },
  { value: 'cdr_lab_result', label: '检验结果表' },
  { value: 'cdr_medication', label: '用药记录表' },
  { value: 'cdr_imaging', label: '影像检查表' },
  { value: 'cdr_vital_sign', label: '生命体征表' },
]

const fieldOptions = [
  { value: 'id_card', label: '身份证号' },
  { value: 'name', label: '姓名' },
  { value: 'phone', label: '联系电话' },
  { value: 'gender', label: '性别' },
  { value: 'birth_date', label: '出生日期' },
  { value: 'diagnosis_code', label: '诊断编码' },
  { value: 'lab_value', label: '检验值' },
  { value: 'medication_code', label: '药品编码' },
  { value: 'created_at', label: '创建时间' },
  { value: 'updated_at', label: '更新时间' },
]

function filterOption(input: string, option: any) {
  return option.label?.toLowerCase().includes(input.toLowerCase())
}

// ===== 搜索 =====
const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '规则名称' },
  { name: 'type', label: '规则类型', type: 'select' as const, options: ruleTypeOptions.map(o => ({ value: o.value, label: o.label })) },
  { name: 'status', label: '状态', type: 'select' as const, options: [
    { label: '已启用', value: 'ENABLED' },
    { label: '已禁用', value: 'DISABLED' },
  ] },
]

let currentSearchParams: Record<string, any> = {}

function handleSearch(values: Record<string, any>) {
  currentSearchParams = values
  fetchData({ page: 1 })
}

function handleReset() {
  currentSearchParams = {}
  fetchData({ page: 1 })
}

// ===== 表格 =====
const columns = [
  { title: '规则名称', dataIndex: 'name', key: 'name', width: 200, ellipsis: true },
  { title: '规则类型', key: 'ruleType', width: 100 },
  { title: '目标表', dataIndex: 'target_table', width: 140 },
  { title: '目标字段', dataIndex: 'target_field', width: 120 },
  { title: '阈值', key: 'threshold', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getQualityRules({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

// ===== 弹窗 =====
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const formState = reactive({
  name: '',
  rule_type: undefined as string | undefined,
  target_table: undefined as string | undefined,
  target_field: undefined as string | undefined,
  expression: '',
  threshold: 90 as number,
  priority: 'MEDIUM' as string,
  description: '',
  // completeness params
  check_null: true,
  check_empty: false,
  // consistency params
  reference_table: undefined as string | undefined,
  reference_field: '',
  // timeliness params
  time_threshold: 24 as number,
})

const formRules: Record<string, Rule[]> = {
  name: [{ required: true, message: '请输入规则名称' }],
  rule_type: [{ required: true, message: '请选择规则类型' }],
  target_table: [{ required: true, message: '请选择目标表' }],
  target_field: [{ required: true, message: '请选择目标字段' }],
  threshold: [{ required: true, message: '请设置阈值' }],
  priority: [{ required: true, message: '请选择优先级' }],
}

function handleRuleTypeChange() {
  // Reset type-specific fields when type changes
  formState.expression = ''
  formState.reference_table = undefined
  formState.reference_field = ''
  formState.time_threshold = 24
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, {
    name: '', rule_type: undefined, target_table: undefined, target_field: undefined,
    expression: '', threshold: 90, priority: 'MEDIUM', description: '',
    check_null: true, check_empty: false,
    reference_table: undefined, reference_field: '',
    time_threshold: 24,
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    name: record.name,
    rule_type: record.rule_type,
    target_table: record.target_table,
    target_field: record.target_field,
    expression: record.expression || '',
    threshold: record.threshold || 90,
    priority: record.priority || 'MEDIUM',
    description: record.description || '',
    check_null: record.config?.check_null ?? true,
    check_empty: record.config?.check_empty ?? false,
    reference_table: record.config?.reference_table,
    reference_field: record.config?.reference_field || '',
    time_threshold: record.config?.time_threshold || 24,
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  submitLoading.value = true
  try {
    const data: Record<string, any> = {
      name: formState.name,
      rule_type: formState.rule_type,
      target_table: formState.target_table,
      target_field: formState.target_field,
      threshold: formState.threshold,
      priority: formState.priority,
      description: formState.description,
    }

    // Add type-specific config
    if (formState.rule_type === 'completeness') {
      data.config = { check_null: formState.check_null, check_empty: formState.check_empty }
    } else if (formState.rule_type === 'accuracy') {
      data.expression = formState.expression
    } else if (formState.rule_type === 'consistency') {
      data.config = { reference_table: formState.reference_table, reference_field: formState.reference_field }
    } else if (formState.rule_type === 'timeliness') {
      data.config = { time_threshold: formState.time_threshold }
    }

    if (isEdit.value) {
      await updateQualityRule(editingId.value!, data)
      message.success('更新成功')
    } else {
      await createQualityRule(data)
      message.success('创建成功')
    }
    handleModalCancel()
    fetchData()
  } finally {
    submitLoading.value = false
  }
}

function handleModalCancel() {
  formRef.value?.resetFields()
  modalVisible.value = false
  editingId.value = null
}

// ===== 操作 =====
async function handleToggle(record: any, checked: boolean) {
  try {
    await toggleQualityRule(record.id, checked)
    message.success(checked ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  await deleteQualityRule(record.id)
  message.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>
