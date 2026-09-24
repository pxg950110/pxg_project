<template>
  <component :is="embedded ? 'div' : PageContainer" :title="embedded ? undefined : '数据元概念管理'" class="dec-wrapper">
    <template v-if="!embedded" #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增数据元概念
      </el-button>
    </template>

    <div class="dec-container">
      <!-- Filter Bar -->
      <div class="filter-bar">
        <div class="flex flex-wrap items-center gap-3">
          <el-input
            v-model="keyword"
            placeholder="搜索代码、名称、定义"
            clearable
            :suffix-icon="Search"
            style="flex: 1; max-width: 360px"
            @keyup.enter="fetchList(1)"
            @clear="fetchList(1)"
          />
          <el-select
            v-model="conceptDomainId"
            placeholder="所属概念域"
            clearable
            filterable
            style="width: 220px"
            @change="fetchList(1)"
          >
            <el-option v-for="cd in conceptDomains" :key="cd.id" :value="cd.id" :label="`${cd.name} (${cd.code})`">
              {{ cd.name }} ({{ cd.code }})
            </el-option>
          </el-select>
          <el-select v-model="status" placeholder="审核状态" clearable style="width: 140px" @change="fetchList(1)">
            <el-option value="DRAFT" label="草稿" />
            <el-option value="APPROVED" label="已批准" />
            <el-option value="RETIRED" label="已废止" />
          </el-select>
          <div class="ml-auto flex items-center gap-2">
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="embedded" type="primary" @click="handleCreate">
              <el-icon class="mr-1"><Plus /></el-icon>
              新增数据元概念
            </el-button>
          </div>
        </div>
      </div>

      <!-- Table Panel -->
      <div class="table-panel">
        <el-table
          :data="dataList"
          v-loading="loading"
          row-key="id"
          size="default"
          style="width: 100%"
        >
          <el-table-column label="代码 (Code)" width="140" fixed="left">
            <template #default="{ row }">
              <span class="code-tag">{{ row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数据元概念名称" width="220">
            <template #default="{ row }">
              <div class="name-cell">
                <span class="name-main" @click="openDetail(row)">{{ row.name }}</span>
                <span v-if="row.nameEn" class="name-sub">{{ row.nameEn }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="对象类 (Object Class)" width="150">
            <template #default="{ row }">
              <span class="oc-tag">{{ row.objectClassName || row.objectClassCode || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="特性 (Property)" width="150">
            <template #default="{ row }">
              <span class="prop-tag">{{ row.propertyName || row.propertyCode || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="关联概念域" width="160">
            <template #default="{ row }">
              <a v-if="row.conceptDomainName" class="concept-link" @click="goToConceptDomain(row)">
                {{ row.conceptDomainName }}
              </a>
              <span v-else class="text-dim">-</span>
            </template>
          </el-table-column>
          <el-table-column label="定义说明" prop="definition" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span :class="['status-badge', row.status ? row.status.toLowerCase() : 'draft']">
                {{ statusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center">
                <el-button link type="primary" size="small" class="action-btn" @click="openDetail(row)">
                  查看
                </el-button>
                <el-button link type="primary" size="small" class="action-btn" @click="handleEdit(row)">
                  编辑
                </el-button>
                <el-popconfirm title="确认删除该数据元概念？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="mt-4 justify-end"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          :current-page="pagination.current"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 15, 20, 50, 100]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <DataElementConceptFormModal ref="formModalRef" @success="fetchList(pagination.current)" />

    <!-- Detail Drawer -->
    <DataElementConceptDetailDrawer ref="detailDrawerRef" @edit="handleEdit" />
  </component>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  dataElementConceptApi,
  conceptDomainApi,
  type DataElementConcept,
  type ConceptDomain,
} from '@/api/dataElementStandard'
import DataElementConceptFormModal from './components/DataElementConceptFormModal.vue'
import DataElementConceptDetailDrawer from './components/DataElementConceptDetailDrawer.vue'

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    filterConceptDomainId?: number
  }>(),
  {
    embedded: false,
    filterConceptDomainId: undefined,
  },
)

const emit = defineEmits<{
  (e: 'navigate-concept-domain', conceptDomainId: number): void
}>()

const formModalRef = ref()
const detailDrawerRef = ref()

const keyword = ref('')
const conceptDomainId = ref<number | undefined>(props.filterConceptDomainId)
const status = ref<string>()
const loading = ref(false)
const dataList = ref<DataElementConcept[]>([])
const conceptDomains = ref<ConceptDomain[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 15,
  total: 0,
})

const statusLabel = (st?: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    APPROVED: '已批准',
    RETIRED: '已废止',
  }
  return (st && map[st]) || st || '草稿'
}

const fetchConceptDomains = async () => {
  try {
    const res = await conceptDomainApi.list({ page: 1, page_size: 500 })
    const data = (res as any)?.data?.data || (res as any)?.data || {}
    conceptDomains.value = data.content || (Array.isArray(data) ? data : [])
  } catch (error) {
    console.error('获取概念域下拉失败')
  }
}

const fetchList = async (page = pagination.current) => {
  loading.value = true
  try {
    const res = await dataElementConceptApi.list({
      keyword: keyword.value?.trim() || undefined,
      conceptDomainId: conceptDomainId.value,
      status: status.value || undefined,
      page,
      page_size: pagination.pageSize,
    })
    const data = (res as any)?.data?.data || (res as any)?.data || {}
    dataList.value = data.content || (Array.isArray(data) ? data : [])
    pagination.total = data.totalElements ?? dataList.value.length
    pagination.current = page
  } catch (error) {
    ElMessage.error('获取数据元概念列表失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  fetchList(page)
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  fetchList(1)
}

const resetFilters = () => {
  keyword.value = ''
  conceptDomainId.value = undefined
  status.value = undefined
  fetchList(1)
}

const handleCreate = () => {
  formModalRef.value?.open()
}

const handleEdit = (record: DataElementConcept) => {
  formModalRef.value?.open(record)
}

const handleDelete = async (record: DataElementConcept) => {
  try {
    await dataElementConceptApi.delete(record.id)
    ElMessage.success('删除成功')
    fetchList(pagination.current)
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const openDetail = (record: DataElementConcept) => {
  detailDrawerRef.value?.open(record.id)
}

const goToConceptDomain = (record: DataElementConcept) => {
  if (record.conceptDomainId) {
    emit('navigate-concept-domain', record.conceptDomainId)
  }
}

watch(
  () => props.filterConceptDomainId,
  (newVal) => {
    if (newVal !== undefined) {
      conceptDomainId.value = newVal
      fetchList(1)
    }
  },
)

onMounted(() => {
  fetchConceptDomains()
  fetchList()
})

defineExpose({
  openCreate: handleCreate,
  refresh: () => fetchList(1),
  setConceptDomainId: (id: number) => {
    conceptDomainId.value = id
    fetchList(1)
  },
})
</script>

<style lang="scss" scoped>
.dec-wrapper {
  height: 100%;
}

.dec-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 240px);
}

.filter-bar {
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.table-panel {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.code-tag {
  font-family: 'JetBrains Mono', monospace, Consolas;
  font-size: 12px;
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.12);
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .name-main {
    font-weight: 500;
    color: #0f172a;
    cursor: pointer;
    &:hover {
      color: #0ea5e9;
      text-decoration: underline;
    }
  }

  .name-sub {
    font-size: 11px;
    color: #94a3b8;
  }
}

.oc-tag {
  font-size: 11px;
  background: rgba(56, 189, 248, 0.14);
  color: #38bdf8;
  border: 1px solid rgba(56, 189, 248, 0.25);
  padding: 2px 8px;
  border-radius: 4px;
}

.prop-tag {
  font-size: 11px;
  background: rgba(129, 140, 248, 0.1);
  color: #818cf8;
  border: 1px solid rgba(129, 140, 248, 0.25);
  padding: 2px 8px;
  border-radius: 4px;
}

.concept-link {
  color: #0ea5e9;
  &:hover {
    color: #38bdf8;
    text-decoration: underline;
  }
}

.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;

  &.draft {
    background: #f8fafc;
    color: #94a3b8;
  }
  &.approved {
    background: #ecfdf5;
    color: #10b981;
  }
  &.retired {
    background: #fef2f2;
    color: #ef4444;
  }
}

.text-dim {
  color: #94a3b8;
}

.action-btn {
  color: #0ea5e9;
  padding: 0 4px;
  &:hover {
    color: #38bdf8;
  }
}
</style>
