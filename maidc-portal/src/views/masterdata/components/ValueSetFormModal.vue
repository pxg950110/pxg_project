<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑值集' : '新增值集'"
    width="720px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="代码" prop="code">
          <el-input v-model="formData.code" placeholder="值集代码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="值集名称" />
        </el-form-item>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="OID" prop="oid">
          <el-input v-model="formData.oid" placeholder="对象标识符" />
        </el-form-item>
        <el-form-item label="来源编码体系" prop="codeSystemId">
          <el-select v-model="formData.codeSystemId" placeholder="选择编码体系" clearable filterable>
            <el-option
              v-for="cs in codeSystems"
              :key="cs.id"
              :value="cs.id"
              :label="`${cs.name} (${cs.code})`"
            />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="定义" prop="definition">
        <el-input v-model="formData.definition" type="textarea" :rows="3" placeholder="值集定义" />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="用途" prop="purpose">
          <el-select v-model="formData.purpose" placeholder="用途">
            <el-option value="CLINICAL" label="临床用途" />
            <el-option value="ADMIN" label="管理用途" />
            <el-option value="RESEARCH" label="研究用途" />
            <el-option value="ALL" label="通用" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="状态">
            <el-option value="DRAFT" label="草稿" />
            <el-option value="ACTIVE" label="生效" />
            <el-option value="RETIRED" label="废止" />
          </el-select>
        </el-form-item>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker v-model="formData.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效日期" prop="expiryDate">
          <el-date-picker v-model="formData.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </div>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注信息" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'
import dayjs from 'dayjs'

interface CodeSystem {
  id: number
  code: string
  name: string
}

interface ValueSet {
  id?: number
  code: string
  name: string
  oid?: string
  definition?: string
  codeSystemId?: number
  purpose?: string
  status: string
  effectiveDate?: string
  expiryDate?: string
  remark?: string
}

const emit = defineEmits<{
  success: []
}>()

const visible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number>()
const codeSystems = ref<CodeSystem[]>([])

const defaultFormData = {
  code: '',
  name: '',
  oid: '',
  definition: '',
  codeSystemId: undefined as number | undefined,
  purpose: 'ALL',
  status: 'DRAFT',
  effectiveDate: undefined as string | undefined,
  expiryDate: undefined as string | undefined,
  remark: '',
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  code: [
    { required: true, message: '请输入值集代码' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '代码必须以大写字母开头' },
  ],
  name: [{ required: true, message: '请输入值集名称' }],
  status: [{ required: true, message: '请选择状态' }],
}

const loadCodeSystems = async () => {
  // TODO: Load code systems from API
  codeSystems.value = []
}

const open = (record?: ValueSet) => {
  visible.value = true
  isEdit.value = !!record

  if (record) {
    currentId.value = record.id
    Object.assign(formData, {
      code: record.code,
      name: record.name,
      oid: record.oid || '',
      definition: record.definition || '',
      codeSystemId: record.codeSystemId,
      purpose: record.purpose || 'ALL',
      status: record.status,
      effectiveDate: record.effectiveDate ? dayjs(record.effectiveDate).format('YYYY-MM-DD') : undefined,
      expiryDate: record.expiryDate ? dayjs(record.expiryDate).format('YYYY-MM-DD') : undefined,
      remark: record.remark || '',
    })
  } else {
    currentId.value = undefined
    Object.assign(formData, defaultFormData)
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitting.value = true

    // TODO: Call API to create/update
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    visible.value = false
    emit('success')
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  visible.value = false
  formRef.value?.resetFields()
}

onMounted(() => {
  loadCodeSystems()
})

defineExpose({ open })
</script>
