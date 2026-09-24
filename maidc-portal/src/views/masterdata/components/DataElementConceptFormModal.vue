<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑数据元概念' : '新增数据元概念'"
    width="800px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="代码" prop="code">
          <el-input v-model="formData.code" placeholder="数据元概念代码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="数据元概念名称" />
        </el-form-item>
      </div>

      <el-form-item label="英文名称" prop="nameEn">
        <el-input v-model="formData.nameEn" placeholder="英文名称" />
      </el-form-item>

      <el-form-item label="定义" prop="definition">
        <el-input v-model="formData.definition" type="textarea" :rows="2" placeholder="数据元概念定义" />
      </el-form-item>

      <el-divider>对象类与特性</el-divider>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="对象类代码" prop="objectClassCode">
          <el-input v-model="formData.objectClassCode" placeholder="如: OC_PATIENT" />
        </el-form-item>
        <el-form-item label="对象类名称" prop="objectClassName">
          <el-input v-model="formData.objectClassName" placeholder="如: 患者" />
        </el-form-item>
      </div>

      <el-form-item label="对象类定义" prop="objectClassDefinition">
        <el-input v-model="formData.objectClassDefinition" type="textarea" :rows="1" placeholder="对象类定义" />
      </el-form-item>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="特性代码" prop="propertyCode">
          <el-input v-model="formData.propertyCode" placeholder="如: PR_GENDER" />
        </el-form-item>
        <el-form-item label="特性名称" prop="propertyName">
          <el-input v-model="formData.propertyName" placeholder="如: 性别" />
        </el-form-item>
      </div>

      <el-form-item label="特性定义" prop="propertyDefinition">
        <el-input v-model="formData.propertyDefinition" type="textarea" :rows="1" placeholder="特性定义" />
      </el-form-item>

      <el-divider>关联信息</el-divider>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="所属概念域" prop="conceptDomainId">
          <el-select
            v-model="formData.conceptDomainId"
            placeholder="选择概念域"
            filterable
            clearable
          >
            <el-option
              v-for="cd in conceptDomains"
              :key="cd.id"
              :value="cd.id"
              :label="`${cd.name} (${cd.code})`"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="状态">
            <el-option value="DRAFT" label="草稿" />
            <el-option value="APPROVED" label="已批准" />
            <el-option value="RETIRED" label="已废止" />
          </el-select>
        </el-form-item>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="版本" prop="version">
          <el-input v-model="formData.version" placeholder="版本号" />
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
import { dataElementConceptApi, conceptDomainApi, type DataElementConcept, type ConceptDomain } from '@/api/dataElementStandard'

const emit = defineEmits<{
  success: [page?: number]
}>()

const visible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number>()
const conceptDomains = ref<ConceptDomain[]>([])

const defaultFormData = {
  code: '',
  name: '',
  nameEn: '',
  definition: '',
  objectClassCode: '',
  objectClassName: '',
  objectClassDefinition: '',
  propertyCode: '',
  propertyName: '',
  propertyDefinition: '',
  conceptDomainId: undefined as number | undefined,
  version: '1.0',
  status: 'DRAFT',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  code: [
    { required: true, message: '请输入数据元概念代码' },
    { pattern: /^DEC_[A-Z][A-Z0-9_]*$/, message: '代码应以DEC_开头' },
  ],
  name: [{ required: true, message: '请输入数据元概念名称' }],
  definition: [{ required: true, message: '请输入数据元概念定义' }],
  objectClassName: [{ required: true, message: '请输入对象类名称' }],
  propertyName: [{ required: true, message: '请输入特性名称' }],
  status: [{ required: true, message: '请选择状态' }],
}

const fetchConceptDomains = async () => {
  try {
    const { data } = await conceptDomainApi.list({ page_size: 1000 })
    conceptDomains.value = data.content
  } catch (error) {
    console.error('获取概念域列表失败')
  }
}

const open = (record?: DataElementConcept) => {
  visible.value = true
  isEdit.value = !!record

  if (record) {
    currentId.value = record.id
    Object.assign(formData, {
      code: record.code,
      name: record.name,
      nameEn: record.nameEn || '',
      definition: record.definition,
      objectClassCode: record.objectClassCode || '',
      objectClassName: record.objectClassName || '',
      objectClassDefinition: record.objectClassDefinition || '',
      propertyCode: record.propertyCode || '',
      propertyName: record.propertyName || '',
      propertyDefinition: record.propertyDefinition || '',
      conceptDomainId: record.conceptDomainId,
      version: record.version,
      status: record.status,
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
      await dataElementConceptApi.update(currentId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await dataElementConceptApi.create(formData)
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

onMounted(() => fetchConceptDomains())

defineExpose({ open })
</script>
