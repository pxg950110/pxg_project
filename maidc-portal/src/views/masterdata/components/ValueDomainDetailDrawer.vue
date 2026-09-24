<template>
  <el-drawer
    v-model="visible"
    title="值域详情"
    size="1000px"
    :destroy-on-close="true"
    @close="handleClose"
  >
    <div v-loading="loading" class="min-h-[200px]">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="代码">{{ data?.code }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ data?.name }}</el-descriptions-item>
        <el-descriptions-item label="英文名称">{{ data?.nameEn }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="data?.domainType === 'ENUMERABLE' ? 'primary' : 'success'">
            {{ data?.domainType === 'ENUMERABLE' ? '可枚举' : '不可枚举' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="定义" :span="2">{{ data?.definition }}</el-descriptions-item>
        <el-descriptions-item v-if="data?.description" label="描述" :span="2">{{ data?.description }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>数据类型与格式</el-divider>

      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="数据类型">
          <el-tag type="info">{{ dataTypeLabel(data?.dataType || '') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="长度">
          {{ data?.minLength === data?.maxLength ? data?.maxLength : `${data?.minLength}-${data?.maxLength}` }}
        </el-descriptions-item>
        <el-descriptions-item label="表示格式">{{ data?.format }}</el-descriptions-item>
        <el-descriptions-item label="表示类">{{ representationClassLabel(data?.representationClass || '') }}</el-descriptions-item>
        <el-descriptions-item label="计量单位">{{ data?.unitName }} ({{ data?.unitOfMeasure }})</el-descriptions-item>
        <el-descriptions-item label="所属概念域">
          <a class="cursor-pointer text-sky-600 hover:text-sky-700 hover:underline" @click="goToConceptDomain">{{ data?.conceptDomainName }}</a>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="2" border size="small" class="mt-4">
        <el-descriptions-item label="版本">{{ data?.version }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusColor(data?.status || '')">{{ statusLabel(data?.status || '') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ data?.remark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 允许值列表(可枚举值域) -->
      <div v-if="data?.domainType === 'ENUMERABLE'" class="mt-6">
        <div class="flex items-center justify-between mb-3">
          <span class="font-semibold text-[15px]">允许值列表</span>
          <div class="flex items-center gap-2">
            <el-button size="small" @click="handleBatchImport">
              <el-icon class="mr-1"><Upload /></el-icon>
              批量导入
            </el-button>
            <el-button type="primary" size="small" @click="handleAddPermissibleValue">
              <el-icon class="mr-1"><Plus /></el-icon>
              新增允许值
            </el-button>
          </div>
        </div>

        <el-table
          v-loading="loadingPermissibleValues"
          :data="permissibleValues"
          row-key="id"
          size="small"
        >
          <el-table-column prop="value" label="值" width="100" />
          <el-table-column prop="valueMeaningName" label="值含义" width="200" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'">
                {{ row.isActive ? '有效' : '无效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="有效期" width="200">
            <template #default="{ row }">
              {{ row.effectiveDate || '-' }} ~ {{ row.expiryDate || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-button link type="primary" size="small" @click="handleEditPermissibleValue(row)">编辑</el-button>
                <el-popconfirm title="确认删除？" @confirm="handleDeletePermissibleValue(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 关联的数据元 -->
      <div class="mt-6">
        <div class="font-semibold text-[15px] mb-3">关联的数据元</div>
        <el-table
          v-loading="loadingDataElements"
          :data="relatedDataElements"
          row-key="id"
          size="small"
        >
          <el-table-column prop="code" label="代码" width="150" />
          <el-table-column prop="name" label="名称" width="250" />
          <el-table-column prop="dataType" label="数据类型" width="100" />
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

  <!-- Permissible Value Modal -->
  <PermissibleValueFormModal ref="permissibleValueFormRef" @success="loadPermissibleValues" />

  <!-- Batch Import Modal -->
  <PermissibleValueBatchImportModal ref="batchImportRef" @success="loadPermissibleValues" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { valueDomainApi, dataElementApi, type ValueDomain, type PermissibleValue, type DataElement } from '@/api/dataElementStandard'
import PermissibleValueFormModal from './PermissibleValueFormModal.vue'
import PermissibleValueBatchImportModal from './PermissibleValueBatchImportModal.vue'

const emit = defineEmits<{
  edit: [record: ValueDomain]
  'navigate-concept-domain': [conceptDomainId: number]
}>()

const visible = ref(false)
const loading = ref(false)
const loadingPermissibleValues = ref(false)
const loadingDataElements = ref(false)
const data = ref<ValueDomain>()
const permissibleValues = ref<PermissibleValue[]>([])
const relatedDataElements = ref<DataElement[]>([])
const permissibleValueFormRef = ref()
const batchImportRef = ref()

const dataTypeLabels: Record<string, string> = {
  STRING: '字符串', INTEGER: '整数', DECIMAL: '小数', BOOLEAN: '布尔',
  DATE: '日期', DATETIME: '日期时间', CODE: '代码', TEXT: '文本',
}

const representationClassLabels: Record<string, string> = {
  CODE: '代码', AMOUNT: '金额', COUNT: '计数', QUANTITY: '数量',
  MEASUREMENT: '测量值', TEXT: '文本', DATE: '日期', TIME: '时间',
  NAME: '名称', DESCRIPTION: '描述', IDENTIFIER: '标识符', PERCENT: '百分比',
  RATE: '比率', NUMBER: '数值',
}

const dataTypeLabel = (type: string) => dataTypeLabels[type] || type
const representationClassLabel = (cls: string) => representationClassLabels[cls] || cls || '-'

const statusColor = (status: string) => ({ DRAFT: 'info', APPROVED: 'success', RETIRED: 'danger' }[status] || 'info')
const statusLabel = (status: string) => ({ DRAFT: '草稿', APPROVED: '已批准', RETIRED: '已废止' }[status] || status)

const registrationStatusColor = (status: string) => ({ DRAFT: 'info', PUBLISHED: 'success', RETIRED: 'danger' }[status] || 'info')
const registrationStatusLabel = (status: string) => ({ DRAFT: '草稿', PUBLISHED: '已发布', RETIRED: '已废止' }[status] || status)

const open = async (id: number) => {
  visible.value = true
  await loadData(id)
  await loadPermissibleValues()
  await loadRelatedDataElements()
}

const loadData = async (id: number) => {
  loading.value = true
  try {
    const { data: result } = await valueDomainApi.get(id)
    data.value = result
  } catch (error) {
    ElMessage.error('获取值域详情失败')
  } finally {
    loading.value = false
  }
}

const loadPermissibleValues = async () => {
  if (!data.value || data.value.domainType !== 'ENUMERABLE') return

  loadingPermissibleValues.value = true
  try {
    const { data: result } = await valueDomainApi.listPermissibleValues(data.value.id)
    permissibleValues.value = result
  } catch (error) {
    ElMessage.error('获取允许值列表失败')
  } finally {
    loadingPermissibleValues.value = false
  }
}

const loadRelatedDataElements = async () => {
  if (!data.value) return

  loadingDataElements.value = true
  try {
    const { data: result } = await dataElementApi.list({ valueDomainId: data.value.id, page_size: 100 })
    relatedDataElements.value = result.content
  } catch (error) {
    ElMessage.error('获取关联数据元失败')
  } finally {
    loadingDataElements.value = false
  }
}

const handleAddPermissibleValue = () => {
  permissibleValueFormRef.value?.open(data.value!.id)
}

const handleEditPermissibleValue = (record: PermissibleValue) => {
  permissibleValueFormRef.value?.open(data.value!.id, record)
}

const handleDeletePermissibleValue = async (record: PermissibleValue) => {
  try {
    await valueDomainApi.deletePermissibleValue(record.id)
    ElMessage.success('删除成功')
    loadPermissibleValues()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleBatchImport = () => {
  batchImportRef.value?.open(data.value!.id)
}

const goToConceptDomain = () => {
  if (data.value?.conceptDomainId) {
    visible.value = false
    emit('navigate-concept-domain', data.value.conceptDomainId)
  }
}

const goToDataElement = (record: DataElement) => {
  console.log('Navigate to data element:', record.id)
}

const handleEdit = () => {
  if (data.value) {
    emit('edit', data.value)
  }
}

const handleClose = () => {
  visible.value = false
}

defineExpose({ open })
</script>
