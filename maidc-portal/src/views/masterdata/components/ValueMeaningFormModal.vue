<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑值含义' : '新增值含义'"
    width="600px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="代码" prop="code">
        <el-input v-model="formData.code" placeholder="值含义代码" :disabled="isEdit" />
      </el-form-item>

      <el-form-item label="名称" prop="name">
        <el-input v-model="formData.name" placeholder="值含义名称" />
      </el-form-item>

      <el-form-item label="英文名称" prop="nameEn">
        <el-input v-model="formData.nameEn" placeholder="英文名称" />
      </el-form-item>

      <el-form-item label="定义" prop="definition">
        <el-input v-model="formData.definition" type="textarea" :rows="2" placeholder="值含义定义" />
      </el-form-item>

      <el-form-item label="排序号" prop="sortOrder">
        <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
      </el-form-item>

      <el-form-item label="是否有效" prop="isActive">
        <el-switch v-model="formData.isActive" />
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
import { conceptDomainApi, type ValueMeaning } from '@/api/dataElementStandard'

const emit = defineEmits<{
  success: []
}>()

const visible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const conceptDomainId = ref<number>()
const currentId = ref<number>()

const defaultFormData = {
  code: '',
  name: '',
  nameEn: '',
  definition: '',
  sortOrder: 0,
  isActive: true,
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  code: [{ required: true, message: '请输入值含义代码' }],
  name: [{ required: true, message: '请输入值含义名称' }],
}

const open = (cdId: number, record?: ValueMeaning) => {
  visible.value = true
  conceptDomainId.value = cdId
  isEdit.value = !!record

  if (record) {
    currentId.value = record.id
    Object.assign(formData, {
      code: record.code,
      name: record.name,
      nameEn: record.nameEn || '',
      definition: record.definition || '',
      sortOrder: record.sortOrder,
      isActive: record.isActive,
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

    if (isEdit.value && currentId.value) {
      await conceptDomainApi.updateValueMeaning(currentId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await conceptDomainApi.createValueMeaning(conceptDomainId.value!, formData)
      ElMessage.success('创建成功')
    }

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
