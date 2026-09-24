<template>
  <PageContainer title="脱敏规则管理">
    <template #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新建规则
      </el-button>
    </template>

    <SearchForm :fields="searchFields" @search="handleSearch" @reset="handleReset" />

    <el-table :data="tableData" v-loading="loading" row-key="id" size="default">
      <el-table-column label="规则名称" prop="name" width="180" show-overflow-tooltip />
      <el-table-column label="目标字段" prop="target_field" width="120" />
      <el-table-column label="策略" width="100">
        <template #default="{ row }">
          <el-tag
            :type="strategyTypeMap[row.strategy] || 'info'"
            size="small"
            :style="strategyStyleMap[row.strategy]"
          >
            {{ strategyMap[row.strategy] || row.strategy }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="效果预览" width="260">
        <template #default="{ row }">
          <div class="preview-cell">
            <div class="preview-before">{{ row.sample_original || '-' }}</div>
            <el-icon style="color: #cbd5e1; margin: 0 8px"><Right /></el-icon>
            <div class="preview-after">{{ row.sample_desensitized || '-' }}</div>
          </div>
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <div class="flex items-center gap-2">
            <el-button link type="primary" size="small" @click="handlePreview(row)">预览效果</el-button>
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
      :title="isEdit ? '编辑脱敏规则' : '新建脱敏规则'"
      width="640px"
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

        <el-form-item label="目标字段" prop="target_field">
          <el-select v-model="formState.target_field" placeholder="请选择要脱敏的字段" filterable style="width: 100%">
            <el-option v-for="opt in fieldOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="脱敏策略" prop="strategy">
          <el-select v-model="formState.strategy" placeholder="请选择脱敏策略" style="width: 100%" @change="handleStrategyChange">
            <el-option v-for="item in strategyOptions" :key="item.value" :value="item.value" :label="item.label">
              <div>
                <div style="font-weight: 500">{{ item.label }}</div>
                <div style="font-size: 12px; color: #94a3b8">{{ item.description }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 掩码策略参数 -->
        <template v-if="formState.strategy === 'mask'">
          <el-form-item label="掩码字符">
            <el-input v-model="formState.mask_char" placeholder="掩码替换字符，默认为 *" style="width: 80px" />
          </el-form-item>
          <el-form-item label="保留前N位">
            <el-input-number v-model="formState.keep_prefix" :min="0" :max="20" placeholder="0" style="width: 120px" />
          </el-form-item>
          <el-form-item label="保留后N位">
            <el-input-number v-model="formState.keep_suffix" :min="0" :max="20" placeholder="0" style="width: 120px" />
          </el-form-item>
        </template>

        <!-- 哈希策略参数 -->
        <template v-if="formState.strategy === 'hash'">
          <el-form-item label="哈希算法">
            <el-select v-model="formState.hash_algorithm" style="width: 160px">
              <el-option value="MD5" label="MD5" />
              <el-option value="SHA256" label="SHA-256" />
              <el-option value="SHA512" label="SHA-512" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 加密策略参数 -->
        <template v-if="formState.strategy === 'encrypt'">
          <el-form-item label="加密算法">
            <el-select v-model="formState.encrypt_algorithm" style="width: 160px">
              <el-option value="AES" label="AES" />
              <el-option value="SM4" label="SM4 (国密)" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 替换策略参数 -->
        <template v-if="formState.strategy === 'substitute'">
          <el-form-item label="替换值">
            <el-input v-model="formState.substitute_value" placeholder="替换后的固定值" />
          </el-form-item>
        </template>

        <!-- 伪匿名策略参数 -->
        <template v-if="formState.strategy === 'pseudonymize'">
          <el-form-item label="伪匿名映射">
            <el-select v-model="formState.pseudonym_pool" style="width: 200px">
              <el-option value="auto_increment" label="自增ID" />
              <el-option value="uuid" label="UUID" />
              <el-option value="random_string" label="随机字符串" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="描述" prop="description">
          <el-input v-model="formState.description" type="textarea" :rows="2" placeholder="规则描述（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleModalCancel">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 脱敏预览弹窗 -->
    <el-dialog
      v-model="previewModalVisible"
      title="脱敏效果预览"
      width="800px"
      :destroy-on-close="true"
    >
      <div v-loading="previewLoading" class="min-h-[200px]">
        <DesensitizePreview
          v-if="previewData"
          :original="previewData.original"
          :desensitized="previewData.desensitized"
        />
        <el-empty v-else description="暂无预览数据" :image-size="60" />
      </div>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormItemRule } from 'element-plus'
import { Plus, Right } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import DesensitizePreview from '@/components/DesensitizePreview/index.vue'
import { useTable } from '@/hooks/useTable'
import {
  getDesensitizeRules,
  createDesensitizeRule,
  updateDesensitizeRule,
  deleteDesensitizeRule,
  toggleDesensitizeRule,
  previewDesensitize,
} from '@/api/data'
import { getEtlColumns } from '@/api/etl'

defineOptions({ name: 'DesensitizeRule' })

// ===== 常量 =====
const strategyMap: Record<string, string> = {
  mask: '掩码',
  hash: '哈希',
  encrypt: '加密',
  substitute: '替换',
  pseudonymize: '伪匿名',
}

const strategyTypeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  mask: 'primary',
  encrypt: 'success',
  substitute: 'warning',
}

const strategyStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
  hash: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
  pseudonymize: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
}

const strategyOptions = [
  { value: 'mask', label: '掩码', description: '用指定字符替换部分内容，如: 138****1234' },
  { value: 'hash', label: '哈希', description: '使用哈希算法转换，不可逆' },
  { value: 'encrypt', label: '加密', description: '可逆加密，保留解密能力' },
  { value: 'substitute', label: '替换', description: '替换为固定值，如: [已脱敏]' },
  { value: 'pseudonymize', label: '伪匿名', description: '替换为伪标识符，保留映射关系' },
]

// 脱敏字段选项 - 从后端获取
const fieldOptions = ref<{ value: string; label: string }[]>([
  { value: 'name', label: '姓名' },
  { value: 'id_card', label: '身份证号' },
  { value: 'phone', label: '联系电话' },
  { value: 'address', label: '家庭住址' },
  { value: 'email', label: '电子邮箱' },
  { value: 'birth_date', label: '出生日期' },
  { value: 'medical_record_no', label: '病历号' },
  { value: 'insurance_no', label: '医保卡号' },
])

async function loadFieldOptions() {
  try {
    // Load sensitive fields from CDR patient table
    const res = await getEtlColumns('cdr', 'cdr_patient')
    const sensitiveFields = ['name', 'id_card', 'phone', 'address', 'email', 'birth_date', 'medical_record_no', 'insurance_no']
    const columns = res.data.data || []
    fieldOptions.value = columns
      .filter((c: any) => sensitiveFields.includes(c.columnName || c.name))
      .map((c: any) => ({ value: c.columnName || c.name, label: c.columnComment || c.columnName || c.name }))
    if (fieldOptions.value.length === 0) {
      // Fallback to defaults
      fieldOptions.value = [
        { value: 'name', label: '姓名' },
        { value: 'id_card', label: '身份证号' },
        { value: 'phone', label: '联系电话' },
        { value: 'address', label: '家庭住址' },
        { value: 'email', label: '电子邮箱' },
        { value: 'birth_date', label: '出生日期' },
        { value: 'medical_record_no', label: '病历号' },
        { value: 'insurance_no', label: '医保卡号' },
      ]
    }
  } catch { /* keep defaults */ }
}

// ===== 搜索 =====
const searchFields = [
  { name: 'keyword', label: '关键词', type: 'input' as const, placeholder: '规则名称' },
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
  (params) => getDesensitizeRules({
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
  target_field: undefined as string | undefined,
  strategy: undefined as string | undefined,
  description: '',
  // mask params
  mask_char: '*',
  keep_prefix: 3,
  keep_suffix: 4,
  // hash params
  hash_algorithm: 'SHA256',
  // encrypt params
  encrypt_algorithm: 'AES',
  // substitute params
  substitute_value: '[已脱敏]',
  // pseudonymize params
  pseudonym_pool: 'uuid',
})

const formRules: Record<string, FormItemRule[]> = {
  name: [{ required: true, message: '请输入规则名称' }],
  target_field: [{ required: true, message: '请选择目标字段' }],
  strategy: [{ required: true, message: '请选择脱敏策略' }],
}

function handleStrategyChange() {
  // Reset strategy-specific params
  formState.mask_char = '*'
  formState.keep_prefix = 3
  formState.keep_suffix = 4
  formState.hash_algorithm = 'SHA256'
  formState.encrypt_algorithm = 'AES'
  formState.substitute_value = '[已脱敏]'
  formState.pseudonym_pool = 'uuid'
}

function handleCreate() {
  editingId.value = null
  Object.assign(formState, {
    name: '', target_field: undefined, strategy: undefined, description: '',
    mask_char: '*', keep_prefix: 3, keep_suffix: 4,
    hash_algorithm: 'SHA256', encrypt_algorithm: 'AES',
    substitute_value: '[已脱敏]', pseudonym_pool: 'uuid',
  })
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = record.id
  Object.assign(formState, {
    name: record.name,
    target_field: record.target_field,
    strategy: record.strategy,
    description: record.description || '',
    mask_char: record.params?.mask_char || '*',
    keep_prefix: record.params?.keep_prefix ?? 3,
    keep_suffix: record.params?.keep_suffix ?? 4,
    hash_algorithm: record.params?.hash_algorithm || 'SHA256',
    encrypt_algorithm: record.params?.encrypt_algorithm || 'AES',
    substitute_value: record.params?.substitute_value || '[已脱敏]',
    pseudonym_pool: record.params?.pseudonym_pool || 'uuid',
  })
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    const data: Record<string, any> = {
      name: formState.name,
      target_field: formState.target_field,
      strategy: formState.strategy,
      description: formState.description,
    }

    // Build strategy-specific params
    if (formState.strategy === 'mask') {
      data.params = {
        mask_char: formState.mask_char,
        keep_prefix: formState.keep_prefix,
        keep_suffix: formState.keep_suffix,
      }
    } else if (formState.strategy === 'hash') {
      data.params = { hash_algorithm: formState.hash_algorithm }
    } else if (formState.strategy === 'encrypt') {
      data.params = { encrypt_algorithm: formState.encrypt_algorithm }
    } else if (formState.strategy === 'substitute') {
      data.params = { substitute_value: formState.substitute_value }
    } else if (formState.strategy === 'pseudonymize') {
      data.params = { pseudonym_pool: formState.pseudonym_pool }
    }

    if (isEdit.value) {
      await updateDesensitizeRule(editingId.value!, data)
      ElMessage.success('更新成功')
    } else {
      await createDesensitizeRule(data)
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
    await toggleDesensitizeRule(record.id, checked)
    ElMessage.success(checked ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  await deleteDesensitizeRule(record.id)
  ElMessage.success('删除成功')
  fetchData()
}

// ===== 脱敏预览 =====
const previewModalVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref<{ original: string; desensitized: string } | null>(null)

async function handlePreview(record: any) {
  previewModalVisible.value = true
  previewLoading.value = true
  previewData.value = null
  try {
    const res = await previewDesensitize({
      field: record.target_field,
      strategy: record.strategy,
      params: record.params || {},
    })
    previewData.value = res.data.data
  } catch {
    previewData.value = null
  } finally {
    previewLoading.value = false
  }
}

onMounted(() => { loadFieldOptions(); fetchData() })
</script>

<style scoped>
.preview-cell {
  display: flex;
  align-items: center;
}
.preview-before {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #64748b;
}
.preview-after {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #10b981;
}
</style>
