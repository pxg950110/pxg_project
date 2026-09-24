<template>
  <el-drawer
    v-model="visible"
    title="数据元概念详情"
    size="900px"
    :destroy-on-close="true"
    @close="handleClose"
  >
    <div v-loading="loading" class="min-h-[200px]">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="代码">{{ data?.code }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ data?.name }}</el-descriptions-item>
        <el-descriptions-item label="英文名称">{{ data?.nameEn }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusColor(data?.status || '')">{{ statusLabel(data?.status || '') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="定义" :span="2">{{ data?.definition }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>对象类与特性</el-divider>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="对象类代码">{{ data?.objectClassCode }}</el-descriptions-item>
        <el-descriptions-item label="对象类名称">
          <el-tag type="primary">{{ data?.objectClassName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="对象类定义" :span="2">{{ data?.objectClassDefinition }}</el-descriptions-item>
        <el-descriptions-item label="特性代码">{{ data?.propertyCode }}</el-descriptions-item>
        <el-descriptions-item label="特性名称">
          <el-tag type="success">{{ data?.propertyName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="特性定义" :span="2">{{ data?.propertyDefinition }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>关联信息</el-divider>

      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="所属概念域">
          <el-tag
            v-if="data?.conceptDomainName"
            style="color: #8b5cf6; background: #f5f3ff; border-color: #ddd6fe"
          >{{ data?.conceptDomainName }}</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="版本">{{ data?.version }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ data?.remark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 关联的数据元 -->
      <div class="mt-6">
        <div class="font-semibold text-[15px] mb-3">关联的数据元</div>
        <el-table
          v-loading="loadingDataElements"
          :data="relatedDataElements"
          row-key="id"
          size="small"
        >
          <el-table-column prop="code" label="代码" width="180" />
          <el-table-column prop="name" label="名称" width="250" />
          <el-table-column label="值域" width="150">
            <template #default="{ row }">
              <el-tag type="info">{{ row.valueDomainName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册状态" width="100">
            <template #default="{ row }">
              <el-tag :type="registrationStatusColor(row.registrationStatus)">
                {{ registrationStatusLabel(row.registrationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="goToDataElement(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-2">
        <el-button @click="handleClose">关闭</el-button>
        <el-button type="primary" @click="handleEdit">编辑</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dataElementConceptApi, dataElementApi, type DataElementConcept, type DataElement } from '@/api/dataElementStandard'

const emit = defineEmits<{
  edit: [record: DataElementConcept]
}>()

const visible = ref(false)
const loading = ref(false)
const loadingDataElements = ref(false)
const data = ref<DataElementConcept>()
const relatedDataElements = ref<DataElement[]>([])

const statusColor = (status: string) => ({ DRAFT: 'info', APPROVED: 'success', RETIRED: 'danger' }[status] || 'info')
const statusLabel = (status: string) => ({ DRAFT: '草稿', APPROVED: '已批准', RETIRED: '已废止' }[status] || status)
const registrationStatusColor = (status: string) => ({ DRAFT: 'info', PUBLISHED: 'success', RETIRED: 'danger' }[status] || 'info')
const registrationStatusLabel = (status: string) => ({ DRAFT: '草稿', PUBLISHED: '已发布', RETIRED: '已废止' }[status] || status)

const open = async (id: number) => {
  visible.value = true
  await loadData(id)
  await loadRelatedDataElements()
}

const loadData = async (id: number) => {
  loading.value = true
  try {
    const { data: result } = await dataElementConceptApi.get(id)
    data.value = result
  } catch (error) {
    ElMessage.error('获取数据元概念详情失败')
  } finally {
    loading.value = false
  }
}

const loadRelatedDataElements = async () => {
  if (!data.value) return

  loadingDataElements.value = true
  try {
    const { data: result } = await dataElementApi.list({ dataElementConceptId: data.value.id, page_size: 100 })
    relatedDataElements.value = result.content
  } catch (error) {
    ElMessage.error('获取关联数据元失败')
  } finally {
    loadingDataElements.value = false
  }
}

const goToDataElement = (record: DataElement) => console.log('Navigate to data element:', record.id)

const handleEdit = () => {
  if (data.value) emit('edit', data.value)
}

const handleClose = () => visible.value = false

defineExpose({ open })
</script>
