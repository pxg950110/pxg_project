<template>
  <component :is="embedded ? 'div' : PageContainer" :title="embedded ? undefined : '值域管理'" class="value-domain-wrapper">
    <template v-if="!embedded" #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增值域
      </el-button>
    </template>

    <div class="value-domain-container">
      <!-- Filter Bar -->
      <div class="filter-bar">
        <div class="flex flex-wrap items-center gap-3">
          <el-input
            v-model="keyword"
            placeholder="搜索值域代码、名称"
            clearable
            :suffix-icon="Search"
            style="width: 260px"
            @keyup.enter="fetchList(1)"
            @clear="fetchList(1)"
          />
          <el-select v-model="domainType" placeholder="值域类型" clearable style="width: 150px" @change="fetchList(1)">
            <el-option value="ENUMERABLE" label="可枚举值域" />
            <el-option value="NON_ENUMERABLE" label="不可枚举值域" />
          </el-select>
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
          <el-select v-model="status" placeholder="审核状态" clearable style="width: 130px" @change="fetchList(1)">
            <el-option value="DRAFT" label="草稿" />
            <el-option value="APPROVED" label="已批准" />
            <el-option value="RETIRED" label="已废止" />
          </el-select>
          <div class="ml-auto flex items-center gap-2">
            <el-button @click="resetFilters">重置</el-button>
            <el-button v-if="embedded" type="primary" @click="handleCreate">
              <el-icon class="mr-1"><Plus /></el-icon>
              新增值域
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
          <el-table-column label="值域名称" width="220">
            <template #default="{ row }">
              <div class="name-cell">
                <span class="name-main" @click="openDetail(row)">{{ row.name }}</span>
                <span v-if="row.nameEn" class="name-sub">{{ row.nameEn }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <span :class="['domain-tag', row.domainType === 'ENUMERABLE' ? 'enumerable' : 'non-enumerable']">
                {{ row.domainType === 'ENUMERABLE' ? '● 可枚举' : '○ 不可枚举' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="数据类型" width="110">
            <template #default="{ row }">
              <span class="data-type-badge">{{ dataTypeLabel(row.dataType) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="长度" width="90">
            <template #default="{ row }">
              {{ row.minLength === row.maxLength ? row.maxLength || '-' : `${row.minLength || 0}-${row.maxLength || '-'}` }}
            </template>
          </el-table-column>
          <el-table-column label="计量单位" prop="unitName" width="100" />
          <el-table-column label="表示类" prop="representationClass" width="100" />
          <el-table-column label="所属概念域" width="180">
            <template #default="{ row }">
              <a v-if="row.conceptDomainName" class="concept-link" @click="goToConceptDomain(row)">
                {{ row.conceptDomainName }}
              </a>
              <span v-else class="text-dim">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span :class="['status-badge', row.status ? row.status.toLowerCase() : 'draft']">
                {{ statusLabel(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center">
                <el-button link type="primary" size="small" class="action-btn" @click="openDetail(row)">
                  允许值 / 详情
                </el-button>
                <el-button link type="primary" size="small" class="action-btn" @click="handleEdit(row)">
                  编辑
                </el-button>
                <el-popconfirm title="确认删除该值域？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row)">
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
    <ValueDomainFormModal ref="formModalRef" @success="fetchList(pagination.current)" />

    <!-- Detail Drawer -->
    <ValueDomainDetailDrawer
      ref="detailDrawerRef"
      @edit="handleEdit"
      @navigate-concept-domain="(id) => emit('navigate-concept-domain', id)"
    />
  </component>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { valueDomainApi, conceptDomainApi, type ValueDomain, type ConceptDomain } from '@/api/dataElementStandard'
import ValueDomainFormModal from './components/ValueDomainFormModal.vue'
import ValueDomainDetailDrawer from './components/ValueDomainDetailDrawer.vue'

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
const domainType = ref<string>()
const conceptDomainId = ref<number | undefined>(props.filterConceptDomainId)
const status = ref<string>()
const loading = ref(false)
const dataList = ref<ValueDomain[]>([])
const conceptDomains = ref<ConceptDomain[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 15,
  total: 0,
})

const dataTypeLabels: Record<string, string> = {
  STRING: '字符串 (S)',
  INTEGER: '整数 (N)',
  DECIMAL: '小数 (D)',
  BOOLEAN: '布尔 (B)',
  DATE: '日期 (DT)',
  DATETIME: '日期时间',
  CODE: '代码 (C)',
  TEXT: '长文本 (T)',
}

const dataTypeLabel = (t: string) => dataTypeLabels[t] || t || '-'

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
    console.error('获取概念域下拉列表失败')
  }
}

const fetchList = async (page = pagination.current) => {
  loading.value = true
  try {
    const res = await valueDomainApi.list({
      keyword: keyword.value?.trim() || undefined,
      domainType: domainType.value as 'ENUMERABLE' | 'NON_ENUMERABLE' | undefined,
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
    ElMessage.error('获取值域列表失败')
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
  domainType.value = undefined
  conceptDomainId.value = undefined
  status.value = undefined
  fetchList(1)
}

const handleCreate = () => {
  formModalRef.value?.open()
}

const handleEdit = (record: ValueDomain) => {
  formModalRef.value?.open(record)
}

const handleDelete = async (record: ValueDomain) => {
  try {
    await valueDomainApi.delete(record.id)
    ElMessage.success('删除成功')
    fetchList(pagination.current)
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const openDetail = (record: ValueDomain) => {
  detailDrawerRef.value?.open(record.id)
}

const goToConceptDomain = (record: ValueDomain) => {
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
.value-domain-wrapper {
  height: 100%;
}

.value-domain-container {
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

.domain-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;

  &.enumerable {
    background: #f0f9ff;
    color: #0ea5e9;
    border: 1px solid #7dd3fc;
  }

  &.non-enumerable {
    background: #ecfdf5;
    color: #10b981;
    border: 1px solid #a7f3d0;
  }
}

.data-type-badge {
  font-size: 11px;
  background: rgba(129, 140, 248, 0.1);
  color: #818cf8;
  border: 1px solid rgba(129, 140, 248, 0.25);
  padding: 2px 6px;
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
