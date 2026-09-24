<template>
  <el-dialog
    v-model="visible"
    title="批量导入允许值"
    width="800px"
    :destroy-on-close="true"
    @close="handleCancel"
  >
    <el-alert
      title="导入格式说明"
      description="每行一个允许值，格式：值|值含义|排序号。例如：1|男性|1"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <el-form label-width="80px">
      <el-form-item label="导入数据">
        <el-input
          v-model="importText"
          type="textarea"
          placeholder="请输入允许值数据，每行一个"
          :rows="15"
        />
      </el-form-item>

      <el-form-item label="预览">
        <el-table :data="previewData" size="small" max-height="200">
          <el-table-column prop="value" label="值" width="100" />
          <el-table-column prop="valueMeaningName" label="值含义" width="200" />
          <el-table-column prop="sortOrder" label="排序号" width="80" />
        </el-table>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { valueDomainApi } from '@/api/dataElementStandard'

const emit = defineEmits<{
  success: []
}>()

const visible = ref(false)
const submitting = ref(false)
const valueDomainId = ref<number>()
const importText = ref('')

const previewData = computed(() => {
  if (!importText.value) return []

  const lines = importText.value.split('\n').filter(line => line.trim())
  return lines.map((line, index) => {
    const parts = line.split('|')
    return {
      key: index,
      value: parts[0]?.trim() || '',
      valueMeaningName: parts[1]?.trim() || '',
      sortOrder: parseInt(parts[2]?.trim() || '0') || index + 1,
    }
  })
})

const open = (vdId: number) => {
  visible.value = true
  valueDomainId.value = vdId
  importText.value = ''
}

const handleSubmit = async () => {
  if (!importText.value.trim()) {
    ElMessage.warning('请输入导入数据')
    return
  }

  if (previewData.value.length === 0) {
    ElMessage.warning('没有有效的导入数据')
    return
  }

  submitting.value = true
  try {
    const values = previewData.value.map(item => ({
      value: item.value,
      valueMeaningName: item.valueMeaningName,
      sortOrder: item.sortOrder,
      isActive: true,
    }))

    await valueDomainApi.importPermissibleValues(valueDomainId.value!, values)
    ElMessage.success(`成功导入 ${values.length} 条允许值`)
    visible.value = false
    emit('success')
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

defineExpose({ open })
</script>
