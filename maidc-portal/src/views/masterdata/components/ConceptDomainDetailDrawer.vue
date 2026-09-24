<template>
  <el-drawer
    v-model="visible"
    title="概念域详情"
    size="900px"
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
        <el-descriptions-item label="维度">{{ data?.dimension }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ data?.version }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusColor(data?.status || '')">{{ statusLabel(data?.status || '') }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册机构">{{ data?.registrationAuthority }}</el-descriptions-item>
        <el-descriptions-item v-if="data?.descriptionRule" label="描述规则" :span="2">
          {{ data?.descriptionRule }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ data?.remark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 值含义列表(可枚举概念域) -->
      <div v-if="data?.domainType === 'ENUMERABLE'" class="mt-6">
        <div class="flex items-center justify-between mb-3">
          <span class="font-semibold text-[15px]">值含义列表</span>
          <div class="flex items-center gap-2">
            <el-button type="primary" size="small" @click="handleAddValueMeaning">
              <el-icon class="mr-1"><Plus /></el-icon>
              新增值含义
            </el-button>
          </div>
        </div>

        <el-table
          v-loading="loadingValueMeanings"
          :data="valueMeanings"
          row-key="id"
          size="small"
        >
          <el-table-column prop="code" label="代码" width="100" />
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="nameEn" label="英文名称" width="150" />
          <el-table-column prop="definition" label="定义" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isActive ? 'success' : 'info'">
                {{ row.isActive ? '有效' : '无效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-button link type="primary" size="small" @click="handleEditValueMeaning(row)">编辑</el-button>
                <el-popconfirm title="确认删除？" @confirm="handleDeleteValueMeaning(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 关联的值域 -->
      <div class="mt-6">
        <div class="font-semibold text-[15px] mb-3">关联的值域</div>
        <el-table
          v-loading="loadingValueDomains"
          :data="relatedValueDomains"
          row-key="id"
          size="small"
        >
          <el-table-column prop="code" label="代码" width="150" />
          <el-table-column prop="name" label="名称" width="200" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.domainType === 'ENUMERABLE' ? 'primary' : 'success'">
                {{ row.domainType === 'ENUMERABLE' ? '可枚举' : '不可枚举' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="dataType" label="数据类型" width="100" />
          <el-table-column prop="unitName" label="计量单位" width="100" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="goToValueDomain(row)">查看</el-button>
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

  <!-- Value Meaning Modal -->
  <ValueMeaningFormModal ref="valueMeaningFormRef" @success="loadValueMeanings" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { conceptDomainApi, valueDomainApi, type ConceptDomain, type ValueMeaning, type ValueDomain } from '@/api/dataElementStandard'
import ValueMeaningFormModal from './ValueMeaningFormModal.vue'

const emit = defineEmits<{
  edit: [record: ConceptDomain]
  'navigate-value-domain': [id: number]
}>()

const visible = ref(false)
const loading = ref(false)
const loadingValueMeanings = ref(false)
const loadingValueDomains = ref(false)
const data = ref<ConceptDomain>()
const valueMeanings = ref<ValueMeaning[]>([])
const relatedValueDomains = ref<ValueDomain[]>([])
const valueMeaningFormRef = ref()

const statusColor = (status: string) => {
  const colors: Record<string, string> = {
    DRAFT: 'info',
    REVIEWED: 'primary',
    APPROVED: 'success',
    RETIRED: 'danger',
  }
  return colors[status] || 'info'
}

const statusLabel = (status: string) => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    REVIEWED: '已审核',
    APPROVED: '已批准',
    RETIRED: '已废止',
  }
  return labels[status] || status
}

const open = async (id: number) => {
  visible.value = true
  await loadData(id)
  await loadValueMeanings()
  await loadRelatedValueDomains()
}

const loadData = async (id: number) => {
  loading.value = true
  try {
    const { data: result } = await conceptDomainApi.get(id)
    data.value = result
  } catch (error) {
    ElMessage.error('获取概念域详情失败')
  } finally {
    loading.value = false
  }
}

const loadValueMeanings = async () => {
  if (!data.value || data.value.domainType !== 'ENUMERABLE') return

  loadingValueMeanings.value = true
  try {
    const { data: result } = await conceptDomainApi.listValueMeanings(data.value.id)
    valueMeanings.value = result
  } catch (error) {
    ElMessage.error('获取值含义列表失败')
  } finally {
    loadingValueMeanings.value = false
  }
}

const loadRelatedValueDomains = async () => {
  if (!data.value) return

  loadingValueDomains.value = true
  try {
    const { data: result } = await valueDomainApi.list({ conceptDomainId: data.value.id, page_size: 100 })
    relatedValueDomains.value = result.content
  } catch (error) {
    ElMessage.error('获取关联值域失败')
  } finally {
    loadingValueDomains.value = false
  }
}

const handleAddValueMeaning = () => {
  valueMeaningFormRef.value?.open(data.value!.id)
}

const handleEditValueMeaning = (record: ValueMeaning) => {
  valueMeaningFormRef.value?.open(data.value!.id, record)
}

const handleDeleteValueMeaning = async (record: ValueMeaning) => {
  try {
    await conceptDomainApi.deleteValueMeaning(record.id)
    ElMessage.success('删除成功')
    loadValueMeanings()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const goToValueDomain = (record: ValueDomain) => {
  visible.value = false
  emit('navigate-value-domain', record.id)
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
