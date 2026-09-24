<template>
  <div class="h-full">
    <!-- Header -->
    <el-card shadow="never" class="!rounded-xl !border-slate-200/80 mb-3">
      <div class="flex items-center gap-4">
        <el-icon :size="24" color="#0ea5e9"><Menu /></el-icon>
        <div class="min-w-0 flex-1">
          <div class="text-base font-semibold">{{ conceptDomain?.name || '概念域列表' }}</div>
          <div class="text-xs text-slate-400">{{ conceptDomain?.code || '管理所有概念域' }}</div>
        </div>
        <el-input
          v-model="keyword"
          placeholder="搜索概念域"
          clearable
          class="!w-60"
          @keyup.enter="fetchList"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select
          v-model="domainType"
          placeholder="类型"
          clearable
          class="!w-36"
          @change="fetchList"
          @clear="domainType = undefined"
        >
          <el-option value="ENUMERABLE" label="可枚举" />
          <el-option value="NON_ENUMERABLE" label="不可枚举" />
        </el-select>
        <el-button @click="handleCreate">
          <el-icon class="mr-1"><Plus /></el-icon>
          新增
        </el-button>
      </div>
    </el-card>

    <!-- Content -->
    <div class="flex gap-3" style="height: calc(100vh - 280px)">
      <!-- Left: List -->
      <div class="w-[400px] shrink-0">
        <el-card
          shadow="never"
          class="!rounded-xl !border-slate-200/80 h-full"
          :body-style="{ height: '100%', padding: '12px', boxSizing: 'border-box' }"
        >
          <div v-loading="loading" class="h-full overflow-y-auto">
            <div
              v-for="item in listData"
              :key="item.id"
              class="flex items-start gap-3 p-3 rounded-lg cursor-pointer hover:bg-slate-100"
              :class="selectedItem?.id === item.id ? 'bg-sky-50' : ''"
              @click="selectItem(item)"
            >
              <el-avatar
                :size="32"
                class="shrink-0"
                :style="{ backgroundColor: item.domainType === 'ENUMERABLE' ? '#0ea5e9' : '#10b981' }"
              >
                {{ item.domainType === 'ENUMERABLE' ? '枚' : '描' }}
              </el-avatar>
              <div class="min-w-0">
                <div class="flex items-center">
                  <span class="font-medium text-sm">{{ item.name }}</span>
                  <el-tag size="small" type="info" class="!ml-2">
                    {{ item.domainType === 'ENUMERABLE' ? '可枚举' : '不可枚举' }}
                  </el-tag>
                </div>
                <div class="text-xs text-slate-500">{{ item.code }}</div>
                <div class="text-xs text-slate-400 mt-1">{{ item.definition }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- Right: Detail -->
      <div class="flex-1 min-w-0">
        <el-card
          shadow="never"
          class="!rounded-xl !border-slate-200/80 h-full"
          :body-style="{ height: '100%', padding: '12px', boxSizing: 'border-box', overflowY: 'auto' }"
        >
          <template v-if="selectedItem">
            <!-- Detail Header -->
            <div class="flex items-center justify-between mb-4">
              <div>
                <span class="text-[15px] font-semibold">{{ selectedItem.name }}</span>
                <el-tag class="!ml-2" :type="selectedItem.domainType === 'ENUMERABLE' ? 'primary' : 'success'">
                  {{ selectedItem.domainType === 'ENUMERABLE' ? '可枚举' : '不可枚举' }}
                </el-tag>
              </div>
              <div class="flex items-center gap-2">
                <el-button link type="primary" size="small" @click="handleEdit(selectedItem)">编辑</el-button>
                <el-popconfirm title="确认删除？" @confirm="handleDelete(selectedItem)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>

            <!-- Basic Info -->
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="代码">{{ selectedItem.code }}</el-descriptions-item>
              <el-descriptions-item label="英文名称">{{ selectedItem.nameEn }}</el-descriptions-item>
              <el-descriptions-item label="定义" :span="2">{{ selectedItem.definition }}</el-descriptions-item>
              <el-descriptions-item label="维度">{{ selectedItem.dimension }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ selectedItem.version }}</el-descriptions-item>
              <el-descriptions-item v-if="selectedItem.descriptionRule" label="描述规则" :span="2">
                {{ selectedItem.descriptionRule }}
              </el-descriptions-item>
            </el-descriptions>

            <!-- Value Meanings (for enumerable) -->
            <div v-if="selectedItem.domainType === 'ENUMERABLE'" class="mt-4">
              <div class="flex items-center justify-between mb-2">
                <span class="font-semibold">值含义列表</span>
                <el-button type="primary" size="small" @click="handleAddValueMeaning">
                  <el-icon class="mr-1"><Plus /></el-icon>
                  新增
                </el-button>
              </div>

              <el-table
                v-loading="loadingValueMeanings"
                :data="valueMeanings"
                row-key="id"
                size="small"
              >
                <el-table-column prop="code" label="代码" width="80" />
                <el-table-column prop="name" label="名称" width="120" />
                <el-table-column prop="nameEn" label="英文名称" width="120" />
                <el-table-column prop="definition" label="定义" show-overflow-tooltip />
                <el-table-column prop="sortOrder" label="排序" width="60" />
                <el-table-column label="操作" width="100">
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

            <!-- Related Value Domains -->
            <div class="mt-4">
              <div class="font-semibold mb-2">关联的值域</div>
              <el-table
                v-loading="loadingValueDomains"
                :data="relatedValueDomains"
                row-key="id"
                size="small"
              >
                <el-table-column prop="code" label="代码" width="120" />
                <el-table-column prop="name" label="名称" width="180" />
                <el-table-column label="类型" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.domainType === 'ENUMERABLE' ? 'primary' : 'success'">
                      {{ row.domainType === 'ENUMERABLE' ? '可枚举' : '不可枚举' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="dataType" label="数据类型" width="80" />
                <el-table-column prop="unitName" label="计量单位" width="80" />
              </el-table>
            </div>
          </template>

          <el-empty v-else :image-size="60" description="请从左侧选择概念域" />
        </el-card>
      </div>
    </div>

    <!-- Value Meaning Modal -->
    <ValueMeaningFormModal ref="valueMeaningFormRef" @success="loadValueMeanings" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Menu, Search } from '@element-plus/icons-vue'
import { conceptDomainApi, valueDomainApi, type ConceptDomain, type ValueMeaning, type ValueDomain } from '@/api/dataElementStandard'
import ValueMeaningFormModal from './ValueMeaningFormModal.vue'

const props = defineProps<{
  conceptDomainId?: number
}>()

const emit = defineEmits<{
  edit: [record: ConceptDomain]
}>()

const keyword = ref('')
const domainType = ref<string>()
const loading = ref(false)
const loadingValueMeanings = ref(false)
const loadingValueDomains = ref(false)
const listData = ref<ConceptDomain[]>([])
const selectedItem = ref<ConceptDomain>()
const valueMeanings = ref<ValueMeaning[]>([])
const relatedValueDomains = ref<ValueDomain[]>([])
const valueMeaningFormRef = ref()
const conceptDomain = ref<ConceptDomain>()

const fetchList = async () => {
  loading.value = true
  try {
    const { data } = await conceptDomainApi.list({
      keyword: keyword.value,
      domainType: domainType.value as 'ENUMERABLE' | 'NON_ENUMERABLE' | undefined,
      page_size: 100,
    })
    listData.value = data.content
    if (listData.value.length > 0 && !selectedItem.value) {
      selectItem(listData.value[0])
    }
  } catch (error) {
    ElMessage.error('获取概念域列表失败')
  } finally {
    loading.value = false
  }
}

const selectItem = async (item: ConceptDomain) => {
  selectedItem.value = item
  if (item.domainType === 'ENUMERABLE') {
    await loadValueMeanings()
  }
  await loadRelatedValueDomains()
}

const loadValueMeanings = async () => {
  if (!selectedItem.value) return
  loadingValueMeanings.value = true
  try {
    const { data } = await conceptDomainApi.listValueMeanings(selectedItem.value.id)
    valueMeanings.value = data
  } catch (error) {
    ElMessage.error('获取值含义失败')
  } finally {
    loadingValueMeanings.value = false
  }
}

const loadRelatedValueDomains = async () => {
  if (!selectedItem.value) return
  loadingValueDomains.value = true
  try {
    const { data } = await valueDomainApi.list({ conceptDomainId: selectedItem.value.id, page_size: 100 })
    relatedValueDomains.value = data.content
  } catch (error) {
    ElMessage.error('获取关联值域失败')
  } finally {
    loadingValueDomains.value = false
  }
}

const handleCreate = () => emit('edit', {} as ConceptDomain)
const handleEdit = (record: ConceptDomain) => emit('edit', record)

const handleDelete = async (record: ConceptDomain) => {
  try {
    await conceptDomainApi.delete(record.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleAddValueMeaning = () => {
  valueMeaningFormRef.value?.open(selectedItem.value!.id)
}

const handleEditValueMeaning = (record: ValueMeaning) => {
  valueMeaningFormRef.value?.open(selectedItem.value!.id, record)
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

watch(() => props.conceptDomainId, (id) => {
  if (id) {
    const item = listData.value.find(d => d.id === id)
    if (item) selectItem(item)
  }
})

onMounted(() => fetchList())
</script>
