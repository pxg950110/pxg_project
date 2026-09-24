<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑概念域' : '新增概念域'"
    width="720px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="代码" prop="code">
          <el-input v-model="formData.code" placeholder="概念域代码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="概念域名称" />
        </el-form-item>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="英文名称" prop="nameEn">
          <el-input v-model="formData.nameEn" placeholder="英文名称" />
        </el-form-item>
        <el-form-item label="类型" prop="domainType">
          <el-radio-group v-model="formData.domainType">
            <el-radio value="ENUMERABLE">可枚举</el-radio>
            <el-radio value="NON_ENUMERABLE">不可枚举</el-radio>
          </el-radio-group>
        </el-form-item>
      </div>

      <el-form-item label="定义" prop="definition">
        <el-input v-model="formData.definition" type="textarea" :rows="3" placeholder="概念域定义" />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="维度" prop="dimension">
          <el-input v-model="formData.dimension" placeholder="如: 长度、重量、温度" />
        </el-form-item>
        <el-form-item label="版本" prop="version">
          <el-input v-model="formData.version" placeholder="版本号" />
        </el-form-item>
      </div>

      <!-- 不可枚举概念域描述规则 -->
      <el-form-item
        v-if="formData.domainType === 'NON_ENUMERABLE'"
        label="描述规则"
        prop="descriptionRule"
      >
        <el-input v-model="formData.descriptionRule" type="textarea" :rows="2" placeholder="不可枚举概念域的描述规则" />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="状态">
            <el-option value="DRAFT" label="草稿" />
            <el-option value="REVIEWED" label="已审核" />
            <el-option value="APPROVED" label="已批准" />
            <el-option value="RETIRED" label="已废止" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册机构" prop="registrationAuthority">
          <el-input v-model="formData.registrationAuthority" placeholder="注册机构" />
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'
import { conceptDomainApi, type ConceptDomain } from '@/api/dataElementStandard'

const emit = defineEmits<{
  success: [page?: number]
}>()

const visible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number>()

const defaultFormData = {
  code: '',
  name: '',
  nameEn: '',
  definition: '',
  domainType: 'ENUMERABLE' as 'ENUMERABLE' | 'NON_ENUMERABLE',
  descriptionRule: '',
  dimension: '',
  version: '1.0',
  status: 'DRAFT',
  registrationAuthority: '',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  code: [
    { required: true, message: '请输入概念域代码' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '代码必须以大写字母开头，只能包含大写字母、数字和下划线' },
  ],
  name: [{ required: true, message: '请输入概念域名称' }],
  definition: [{ required: true, message: '请输入概念域定义' }],
  domainType: [{ required: true, message: '请选择概念域类型' }],
  status: [{ required: true, message: '请选择状态' }],
}

const open = (record?: ConceptDomain) => {
  visible.value = true
  isEdit.value = !!record

  if (record) {
    currentId.value = record.id
    Object.assign(formData, {
      code: record.code,
      name: record.name,
      nameEn: record.nameEn || '',
      definition: record.definition,
      domainType: record.domainType,
      descriptionRule: record.descriptionRule || '',
      dimension: record.dimension || '',
      version: record.version,
      status: record.status,
      registrationAuthority: record.registrationAuthority || '',
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

    if (isEdit.value && currentId.value) {
      await conceptDomainApi.update(currentId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await conceptDomainApi.create(formData)
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
