<template>
  <PageContainer title="脱敏规则管理">
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
        <template v-if="column.key === 'strategy'">
          <a-tag :color="strategyColorMap[record.strategy] || 'default'">
            {{ strategyMap[record.strategy] || record.strategy }}
          </a-tag>
        </template>
        <template v-if="column.key === 'preview'">
          <div class="preview-cell">
            <div class="preview-before">{{ record.sample_original || '-' }}</div>
            <ArrowRightOutlined style="color: rgba(0,0,0,0.25); margin: 0 8px" />
            <div class="preview-after">{{ record.sample_desensitized || '-' }}</div>
          </div>
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
            <a-button type="link" size="small" @click="handlePreview(record)">预览效果</a-button>
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
      :title="isEdit ? '编辑脱敏规则' : '新建脱敏规则'"
      :confirm-loading="submitLoading"
      :width="640"
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

        <a-form-item label="目标字段" name="target_field">
          <a-select
            v-model:value="formState.target_field"
            placeholder="请选择要脱敏的字段"
            show-search
            :options="fieldOptions"
            :filter-option="filterOption"
          />
        </a-form-item>

        <a-form-item label="脱敏策略" name="strategy">
          <a-select v-model:value="formState.strategy" placeholder="请选择脱敏策略" @change="handleStrategyChange">
            <a-select-option v-for="item in strategyOptions" :key="item.value" :value="item.value">
              <div>
                <div style="font-weight: 500">{{ item.label }}</div>
                <div style="font-size: 12px; color: rgba(0,0,0,0.45)">{{ item.description }}</div>
              </div>
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- 掩码策略参数 -->
        <template v-if="formState.strategy === 'mask'">
          <a-form-item label="掩码字符">
            <a-input v-model:value="formState.mask_char" placeholder="掩码替换字符，默认为 *" style="width: 80px" />
          </a-form-item>
          <a-form-item label="保留前N位">
            <a-input-number v-model:value="formState.keep_prefix" :min="0" :max="20" placeholder="0" style="width: 120px" />
          </a-form-item>
          <a-form-item label="保留后N位">
            <a-input-number v-model:value="formState.keep_suffix" :min="0" :max="20" placeholder="0" style="width: 120px" />
          </a-form-item>
        </template>

        <!-- 哈希策略参数 -->
        <template v-if="formState.strategy === 'hash'">
          <a-form-item label="哈希算法">
            <a-select v-model:value="formState.hash_algorithm" style="width: 160px">
              <a-select-option value="MD5">MD5</a-select-option>
              <a-select-option value="SHA256">SHA-256</a-select-option>
              <a-select-option value="SHA512">SHA-512</a-select-option>
            </a-select>
          </a-form-item>
        </template>

        <!-- 加密策略参数 -->
        <template v-if="formState.strategy === 'encrypt'">
          <a-form-item label="加密算法">
            <a-select v-model:value="formState.encrypt_algorithm" style="width: 160px">
              <a-select-option value="AES">AES</a-select-option>
              <a-select-option value="SM4">SM4 (国密)</a-select-option>
            </a-select>
          </a-form-item>
        </template>

        <!-- 替换策略参数 -->
        <template v-if="formState.strategy === 'substitute'">
          <a-form-item label="替换值">
            <a-input v-model:value="formState.substitute_value" placeholder="替换后的固定值" />
          </a-form-item>
        </template>

        <!-- 伪匿名策略参数 -->
        <template v-if="formState.strategy === 'pseudonymize'">
          <a-form-item label="伪匿名映射">
            <a-select v-model:value="formState.pseudonym_pool" style="width: 200px">
              <a-select-option value="auto_increment">自增ID</a-select-option>
              <a-select-option value="uuid">UUID</a-select-option>
              <a-select-option value="random_string">随机字符串</a-select-option>
            </a-select>
          </a-form-item>
        </template>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="规则描述（选填）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 脱敏预览弹窗 -->
    <a-modal
      v-model:open="previewModalVisible"
      title="脱敏效果预览"
      :footer="null"
      :width="800"
      destroy-on-close
    >
      <a-spin :spinning="previewLoading">
        <DesensitizePreview
          v-if="previewData"
          :original="previewData.original"
          :desensitized="previewData.desensitized"
        />
        <a-empty v-else description="暂无预览数据" />
      </a-spin>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ArrowRightOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
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

defineOptions({ name: 'DesensitizeRule' })

// ===== 常量 =====
const strategyMap: Record<string, string> = {
  mask: '掩码',
  hash: '哈希',
  encrypt: '加密',
  substitute: '替换',
  pseudonymize: '伪匿名',
}

const strategyColorMap: Record<string, string> = {
  mask: 'blue',
  hash: 'purple',
  encrypt: 'green',
  substitute: 'orange',
  pseudonymize: 'cyan',
}

const strategyOptions = [
  { value: 'mask', label: '掩码', description: '用指定字符替换部分内容，如: 138****1234' },
  { value: 'hash', label: '哈希', description: '使用哈希算法转换，不可逆' },
  { value: 'encrypt', label: '加密', description: '可逆加密，保留解密能力' },
  { value: 'substitute', label: '替换', description: '替换为固定值，如: [已脱敏]' },
  { value: 'pseudonymize', label: '伪匿名', description: '替换为伪标识符，保留映射关系' },
]

const fieldOptions = [
  { value: 'name', label: '姓名' },
  { value: 'id_card', label: '身份证号' },
  { value: 'phone', label: '联系电话' },
  { value: 'address', label: '家庭住址' },
  { value: 'email', label: '电子邮箱' },
  { value: 'birth_date', label: '出生日期' },
  { value: 'medical_record_no', label: '病历号' },
  { value: 'insurance_no', label: '医保卡号' },
]

function filterOption(input: string, option: any) {
  return option.label?.toLowerCase().includes(input.toLowerCase())
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
const columns = [
  { title: '规则名称', dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: '目标字段', dataIndex: 'target_field', width: 120 },
  { title: '策略', key: 'strategy', width: 100 },
  { title: '效果预览', key: 'preview', width: 260 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getDesensitizeRules({
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

const formRules: Record<string, Rule[]> = {
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
  await formRef.value?.validateFields()
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
      message.success('更新成功')
    } else {
      await createDesensitizeRule(data)
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
    await toggleDesensitizeRule(record.id, checked)
    message.success(checked ? '已启用' : '已禁用')
    fetchData()
  } catch {
    // error handled by request interceptor
  }
}

async function handleDelete(record: any) {
  await deleteDesensitizeRule(record.id)
  message.success('删除成功')
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

onMounted(() => fetchData())
</script>

<style scoped>
.preview-cell {
  display: flex;
  align-items: center;
}
.preview-before {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}
.preview-after {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #52c41a;
}
</style>
