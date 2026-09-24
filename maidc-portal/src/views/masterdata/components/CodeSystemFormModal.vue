<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑编码体系' : '新增编码体系'"
    width="720px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="代码" prop="code">
          <el-input v-model="formData.code" placeholder="编码体系代码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="编码体系名称" />
        </el-form-item>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="OID" prop="oid">
          <el-input v-model="formData.oid" placeholder="对象标识符" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="formData.version" placeholder="版本号" />
        </el-form-item>
      </div>

      <el-form-item label="定义" prop="definition">
        <el-input v-model="formData.definition" type="textarea" :rows="3" placeholder="编码体系定义" />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="发布机构" prop="publisher">
          <el-input v-model="formData.publisher" placeholder="发布机构" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="状态">
            <el-option value="DRAFT" label="草稿" />
            <el-option value="ACTIVE" label="生效" />
            <el-option value="RETIRED" label="废止" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="参考链接" prop="url">
        <el-input v-model="formData.url" placeholder="参考链接" />
      </el-form-item>

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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'

interface CodeSystem {
  id?: number
  code: string
  name: string
  oid?: string
  definition?: string
  version?: string
  publisher?: string
  status: string
  url?: string
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

const defaultFormData = {
  code: '',
  name: '',
  oid: '',
  definition: '',
  version: '1.0',
  publisher: '',
  status: 'DRAFT',
  url: '',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  code: [
    { required: true, message: '请输入编码体系代码' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '代码必须以大写字母开头' },
  ],
  name: [{ required: true, message: '请输入编码体系名称' }],
  status: [{ required: true, message: '请选择状态' }],
}

const open = (record?: CodeSystem) => {
  visible.value = true
  isEdit.value = !!record

  if (record) {
    currentId.value = record.id
    Object.assign(formData, {
      code: record.code,
      name: record.name,
      oid: record.oid || '',
      definition: record.definition || '',
      version: record.version || '1.0',
      publisher: record.publisher || '',
      status: record.status,
      url: record.url || '',
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

defineExpose({ open })
</script>
