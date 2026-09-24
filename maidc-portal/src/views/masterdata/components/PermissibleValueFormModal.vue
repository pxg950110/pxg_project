<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑允许值' : '新增允许值'"
    width="600px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <el-form-item label="值" prop="value">
        <el-input v-model="formData.value" placeholder="允许值" />
      </el-form-item>

      <el-form-item label="值含义" prop="valueMeaningName">
        <el-input v-model="formData.valueMeaningName" placeholder="值含义名称" />
      </el-form-item>

      <el-form-item label="排序号" prop="sortOrder">
        <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%" />
      </el-form-item>

      <el-form-item label="是否有效" prop="isActive">
        <el-switch v-model="formData.isActive" />
      </el-form-item>

      <el-form-item label="生效日期" prop="effectiveDate">
        <el-date-picker v-model="formData.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>

      <el-form-item label="失效日期" prop="expiryDate">
        <el-date-picker v-model="formData.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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
import dayjs from 'dayjs'
import { valueDomainApi, type PermissibleValue } from '@/api/dataElementStandard'

const emit = defineEmits<{
  success: []
}>()

const visible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const valueDomainId = ref<number>()
const currentId = ref<number>()

const defaultFormData = {
  value: '',
  valueMeaningName: '',
  sortOrder: 0,
  isActive: true,
  effectiveDate: undefined as string | undefined,
  expiryDate: undefined as string | undefined,
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  value: [{ required: true, message: '请输入允许值' }],
  valueMeaningName: [{ required: true, message: '请输入值含义' }],
}

const open = (vdId: number, record?: PermissibleValue) => {
  visible.value = true
  valueDomainId.value = vdId
  isEdit.value = !!record

  if (record) {
    currentId.value = record.id
    Object.assign(formData, {
      value: record.value,
      valueMeaningName: record.valueMeaningName,
      sortOrder: record.sortOrder,
      isActive: record.isActive,
      effectiveDate: record.effectiveDate ? dayjs(record.effectiveDate).format('YYYY-MM-DD') : undefined,
      expiryDate: record.expiryDate ? dayjs(record.expiryDate).format('YYYY-MM-DD') : undefined,
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

    // el-date-picker with value-format="YYYY-MM-DD" keeps fields as 'YYYY-MM-DD' strings
    const submitData = { ...formData }

    if (isEdit.value && currentId.value) {
      await valueDomainApi.updatePermissibleValue(currentId.value, submitData)
      ElMessage.success('更新成功')
    } else {
      await valueDomainApi.createPermissibleValue(valueDomainId.value!, submitData)
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
