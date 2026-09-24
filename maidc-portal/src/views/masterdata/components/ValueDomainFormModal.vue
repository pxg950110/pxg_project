<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑值域' : '新增值域'"
    width="800px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="代码" prop="code">
          <el-input v-model="formData.code" placeholder="值域代码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="值域名称" />
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
        <el-input v-model="formData.definition" type="textarea" :rows="2" placeholder="值域定义" />
      </el-form-item>

      <!-- 不可枚举值域描述 -->
      <el-form-item
        v-if="formData.domainType === 'NON_ENUMERABLE'"
        label="描述"
        prop="description"
      >
        <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="不可枚举值域的描述" />
      </el-form-item>

      <el-divider>数据类型与格式</el-divider>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-x-4">
        <el-form-item label="数据类型" prop="dataType">
          <el-select v-model="formData.dataType" placeholder="数据类型">
            <el-option value="STRING" label="字符串" />
            <el-option value="INTEGER" label="整数" />
            <el-option value="DECIMAL" label="小数" />
            <el-option value="BOOLEAN" label="布尔" />
            <el-option value="DATE" label="日期" />
            <el-option value="DATETIME" label="日期时间" />
            <el-option value="CODE" label="代码" />
            <el-option value="TEXT" label="文本" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小长度" prop="minLength">
          <el-input-number v-model="formData.minLength" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="最大长度" prop="maxLength">
          <el-input-number v-model="formData.maxLength" :min="1" style="width: 100%" />
        </el-form-item>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="表示格式" prop="format">
          <el-input v-model="formData.format" placeholder="如: N5,2, YYYYMMDD" />
        </el-form-item>
        <el-form-item label="表示类" prop="representationClass">
          <el-select v-model="formData.representationClass" placeholder="表示类" clearable>
            <el-option value="CODE" label="代码" />
            <el-option value="AMOUNT" label="金额" />
            <el-option value="COUNT" label="计数" />
            <el-option value="QUANTITY" label="数量" />
            <el-option value="MEASUREMENT" label="测量值" />
            <el-option value="TEXT" label="文本" />
            <el-option value="DATE" label="日期" />
            <el-option value="TIME" label="时间" />
            <el-option value="NAME" label="名称" />
            <el-option value="DESCRIPTION" label="描述" />
            <el-option value="IDENTIFIER" label="标识符" />
            <el-option value="PERCENT" label="百分比" />
            <el-option value="RATE" label="比率" />
            <el-option value="NUMBER" label="数值" />
          </el-select>
        </el-form-item>
      </div>

      <el-divider>计量单位</el-divider>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="单位代码" prop="unitOfMeasure">
          <el-input v-model="formData.unitOfMeasure" placeholder="如: kg, g, mmHg" />
        </el-form-item>
        <el-form-item label="单位名称" prop="unitName">
          <el-input v-model="formData.unitName" placeholder="如: 千克, 克, 毫米汞柱" />
        </el-form-item>
      </div>

      <el-divider>关联信息</el-divider>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
        <el-form-item label="所属概念域" prop="conceptDomainId">
          <el-select
            v-model="formData.conceptDomainId"
            placeholder="选择概念域"
            filterable
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
import { valueDomainApi, conceptDomainApi, type ValueDomain, type ConceptDomain } from '@/api/dataElementStandard'

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
  domainType: 'ENUMERABLE' as 'ENUMERABLE' | 'NON_ENUMERABLE',
  description: '',
  dataType: 'STRING',
  maxLength: undefined as number | undefined,
  minLength: undefined as number | undefined,
  format: '',
  unitOfMeasure: '',
  unitName: '',
  representationClass: '',
  conceptDomainId: undefined as number | undefined,
  version: '1.0',
  status: 'DRAFT',
  remark: '',
}

const formData = reactive({ ...defaultFormData })

const rules: Record<string, FormItemRule[]> = {
  code: [
    { required: true, message: '请输入值域代码' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '代码必须以大写字母开头' },
  ],
  name: [{ required: true, message: '请输入值域名称' }],
  definition: [{ required: true, message: '请输入值域定义' }],
  domainType: [{ required: true, message: '请选择类型' }],
  dataType: [{ required: true, message: '请选择数据类型' }],
  conceptDomainId: [{ required: true, message: '请选择所属概念域' }],
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

const open = (record?: ValueDomain) => {
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
      description: record.description || '',
      dataType: record.dataType,
      maxLength: record.maxLength,
      minLength: record.minLength,
      format: record.format || '',
      unitOfMeasure: record.unitOfMeasure || '',
      unitName: record.unitName || '',
      representationClass: record.representationClass || '',
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
      await valueDomainApi.update(currentId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await valueDomainApi.create(formData)
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

onMounted(() => {
  fetchConceptDomains()
})

defineExpose({ open })
</script>
