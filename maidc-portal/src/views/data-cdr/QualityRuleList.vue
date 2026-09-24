<template>
  <PageContainer title="数据质量规则">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建规则
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="规则名称" prop="name" width="200" show-overflow-tooltip />
      <el-table-column label="规则类型" width="100">
        <template #default="{ row }">
          <el-tag
            :type="ruleTypeTagMap[row.rule_type] || 'info'"
            size="small"
            :style="ruleTypeStyleMap[row.rule_type]"
          >
            {{ ruleTypeMap[row.rule_type] || row.rule_type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标表" prop="target_table" width="140" />
      <el-table-column label="目标字段" prop="target_field" width="120" />
      <el-table-column label="阈值" width="80">
        <template #default="{ row }">
          {{ row.threshold }}%
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 'ENABLED'"
            inline-prompt
            active-text="启用"
            inactive-text="禁用"
            @change="(v: string | number | boolean) => handleToggle(row, Boolean(v))"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm title="确定删除此规则？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="mt-4 justify-end"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      :current-page="pagination.current"
      :page-size="pagination.pageSize"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 新建/编辑规则弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="isEdit ? '编辑规则' : '新建规则'"
      width="700px"
      :destroy-on-close="true"
    >
      <el-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formState.name" placeholder="请输入规则名称" />
        </el-form-item>

        <el-form-item label="规则类型" prop="rule_type">
          <el-select v-model="formState.rule_type" placeholder="请选择规则类型" style="width: 100%" @change="handleRuleTypeChange">
            <el-option v-for="item in ruleTypeOptions" :key="item.value" :value="item.value" :label="item.label">
              <div>
                <div>{{ item.label }}</div>
                <div style="font-size: 12px; color: #94a3b8">{{ item.description }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="目标表" prop="target_table">
          <el-select v-model="formState.target_table" placeholder="请选择目标表" filterable style="width: 100%">
            <el-option v-for="opt in tableOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="目标字段" prop="target_field">
          <el-select v-model="formState.target_field" placeholder="请选择目标字段" filterable style="width: 100%">
            <el-option v-for="opt in fieldOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>

        <!-- 完整性规则配置 -->
        <template v-if="formState.rule_type === 'completeness'">
          <el-form-item label="空值检查" prop="check_null">
            <el-switch v-model="formState.check_null" active-text="检查空值" />
          </el-form-item>
          <el-form-item label="空字符串检查" prop="check_empty">
            <el-switch v-model="formState.check_empty" active-text="检查空字符串" />
          </el-form-item>
        </template>

        <!-- 准确性规则配置 -->
        <template v-if="formState.rule_type === 'accuracy'">
          <el-form-item label="校验表达式" prop="expression">
            <el-input
              v-model="formState.expression"
              type="textarea"
              placeholder="如: value > 0 AND value < 200 (支持的变量: value, record)"
              :rows="2"
            />
          </el-form-item>
        </template>

        <!-- 一致性规则配置 -->
        <template v-if="formState.rule_type === 'consistency'">
          <el-form-item label="参照表" prop="reference_table">
            <el-select v-model="formState.reference_table" placeholder="选择参照表" filterable style="width: 100%">
              <el-option v-for="opt in tableOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="参照字段" prop="reference_field">
            <el-input v-model="formState.reference_field" placeholder="参照表中用于比对的字段" />
          </el-form-item>
        </template>

        <!-- 及时性规则配置 -->
        <template v-if="formState.rule_type === 'timeliness'">
          <el-form-item label="时间阈值(小时)" prop="time_threshold">
            <el-input-number v-model="formState.time_threshold" :min="1" placeholder="数据最大延迟时间" style="width: 100%" />
          </el-form-item>
        </template>

        <el-form-item label="阈值(%)" prop="threshold">
          <el-slider v-model="formState.threshold" :min="0" :max="100" :step="1" :marks="{ 0: '0%', 70: '70%', 90: '90%', 100: '100%' }" />
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-select v-model="formState.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option value="HIGH" label="高"><el-tag type="danger">高</el-tag></el-option>
            <el-option value="MEDIUM" label="中"><el-tag type="warning">中</el-tag></el-option>
            <el-option value="LOW" label="低"><el-tag type="primary">低</el-tag></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="formState.description" type="textarea" :rows="2" placeholder="规则描述（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleModalCancel">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormItemRule } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
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
import { getEtlTables, getEtlColumns } from '@/api/etl'

defineOptions({ name: 'QualityRuleList' })

// ===== 常量 =====
const ruleTypeMap: Record<string, string> = {
  completeness: '完整性',
  accuracy: '准确性',
  consistency: '一致性',
  timeliness: '及时性',
}

const ruleTypeTagMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  completeness: 'primary',
  accuracy: 'success',
  timeliness: 'warning',
}

const ruleTypeStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
  consistency: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
}

const ruleTypeOptions = [
  { value: 'completeness', label: '完整性', description: '检查数据是否存在空值、缺失等问题' },
  { value: 'accuracy', label: '准确性', description: '检查数据值是否在合理范围内' },
  { value: 'consistency', label: '一致性', description: '检查不同表/字段间的数据是否一致' },
  { value: 'timeliness', label: '及时性', description: '检查数据更新是否及时' },
]

// 表/字段选项 - 从后端获取
const tableOptions = ref<{ value: string; label: string }[]>([])
const fieldOptions = ref<{ value: string; label: string }[]>([])

async function loadTableOptions() {
  try {
    const res = await getEtlTables('cdr')
    tableOptions.value = (res.data.data || []).map((t: any) => ({ value: t.tableName || t.name, label: t.tableComment || t.tableName || t.name }))
  } catch {
    // fallback to common CDR tables
    tableOptions.value = [
      { value: 'cdr_patient', label: '患者信息表' },
      { value: 'cdr_encounter', label: '就诊记录表' },
      { value: 'cdr_diagnosis', label: '诊断信息表' },
      { value: 'cdr_lab_result', label: '检验结果表' },
      { value: 'cdr_medication', label: '用药记录表' },
      { value: 'cdr_imaging', label: '影像检查表' },
      { value: 'cdr_vital_sign', label: '生命体征表' },
    ]
  }
}

async function loadFieldOptions(tableName: string) {
  try {
    const res = await getEtlColumns('cdr', tableName)
    fieldOptions.value = (res.data.data || []).map((c: any) => ({ value: c.columnName || c.name, label: c.columnComment || c.columnName || c.name }))
  } catch {
    fieldOptions.value = []
  }
}

onMounted(() => { loadTableOptions(); fetchData() })

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
const { tableData, loading, pagination, fetchData } = useTable<any>(
  (params) => getQualityRules({
    page: params.page,
    page_size: params.pageSize,
    ...currentSearchParams,
  }),
)

function handlePageChange(page: number) {
  pagination.current = page
  fetchData({ page })
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData({ page: 1, pageSize: size })
}

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

const formRules: Record<string, FormItemRule[]> = {
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
  await formRef.value?.validate()
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
      ElMessage.success('更新成功')
    } else {
      await createQualityRule(data)
      ElMessage.success('创建成功')
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
    ElMessage.success(checked ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  await deleteQualityRule(record.id)
  ElMessage.success('删除成功')
  fetchData()
}

</script>
