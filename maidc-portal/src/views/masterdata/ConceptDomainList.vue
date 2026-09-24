<template>
  <component :is="embedded ? 'div' : PageContainer" :title="embedded ? undefined : '概念域管理'" class="concept-domain-view-wrapper">
    <template v-if="!embedded" #extra>
      <el-button type="primary" @click="handleCreate">
        <el-icon class="mr-1"><Plus /></el-icon>
        新增概念域
      </el-button>
    </template>

    <div class="concept-domain-layout">
      <!-- Left: Type filter -->
      <div class="left-type-nav">
        <div class="type-nav-header">
          <el-icon class="nav-icon"><Menu /></el-icon>
          <span>概念域类型</span>
        </div>
        <div class="type-menu-list">
          <div
            v-for="item in typeMenuItems"
            :key="item.key"
            class="type-menu-item"
            :class="{ active: selectedType === item.key }"
            @click="handleTypeChange(item.key)"
          >
            <span>{{ item.label }}</span>
            <span v-if="item.key !== 'ALL'" class="type-count-badge">
              {{ item.key === 'ENUMERABLE' ? typeCounts.enumerable : typeCounts.nonEnumerable }}
            </span>
          </div>
        </div>

        <div class="standard-tip-card">
          <div class="tip-title">WS/T 303-2023 规范</div>
          <div class="tip-desc">
            概念域是有效值含义的集合。可枚举概念域对应离散值含义表，不可枚举概念域对应连续范围或描述规则。
          </div>
        </div>
      </div>

      <!-- Right: Content -->
      <div class="right-content">
        <!-- Search Bar -->
        <div class="filter-bar">
          <div class="flex flex-wrap items-center gap-3">
            <el-input
              v-model="keyword"
              placeholder="搜索概念域代码、名称、英文名"
              clearable
              :suffix-icon="Search"
              style="flex: 1; max-width: 420px"
              @keyup.enter="fetchList(1)"
              @clear="fetchList(1)"
            />
            <el-select v-model="status" placeholder="审核状态" clearable style="width: 160px" @change="fetchList(1)">
              <el-option value="DRAFT" label="草稿 (DRAFT)" />
              <el-option value="REVIEWED" label="已审核 (REVIEWED)" />
              <el-option value="APPROVED" label="已批准 (APPROVED)" />
              <el-option value="RETIRED" label="已废止 (RETIRED)" />
            </el-select>
            <div class="ml-auto flex items-center gap-2">
              <el-button @click="resetFilters">重置</el-button>
              <el-button v-if="embedded" type="primary" @click="handleCreate">
                <el-icon class="mr-1"><Plus /></el-icon>
                新增概念域
              </el-button>
            </div>
          </div>
        </div>

        <!-- Table -->
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
            <el-table-column label="概念域名称" width="220">
              <template #default="{ row }">
                <div class="name-cell">
                  <span class="name-main" @click="openDetail(row)">{{ row.name }}</span>
                  <span v-if="row.nameEn" class="name-sub">{{ row.nameEn }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="130">
              <template #default="{ row }">
                <span :class="['domain-tag', row.domainType === 'ENUMERABLE' ? 'enumerable' : 'non-enumerable']">
                  {{ row.domainType === 'ENUMERABLE' ? '● 可枚举' : '○ 不可枚举' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="测量维度" prop="dimension" width="110" />
            <el-table-column label="版本" prop="version" width="90" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span :class="['status-badge', row.status ? row.status.toLowerCase() : 'draft']">
                  {{ statusLabel(row.status) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <div class="flex items-center">
                  <el-button link type="primary" size="small" class="action-btn" @click="openDetail(row)">
                    详情 / 值含义
                  </el-button>
                  <el-button link type="primary" size="small" class="action-btn" @click="emit('navigate-value-domain', row.id)">
                    关联值域
                  </el-button>
                  <el-button link type="primary" size="small" class="action-btn" @click="handleEdit(row)">
                    编辑
                  </el-button>
                  <el-popconfirm title="确认删除该概念域？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row)">
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
    </div>

    <!-- Create/Edit Modal -->
    <ConceptDomainFormModal ref="formModalRef" @success="fetchList(pagination.current)" />

    <!-- Detail Drawer -->
    <ConceptDomainDetailDrawer
      ref="detailDrawerRef"
      @edit="handleEdit"
      @navigate-value-domain="(id) => emit('navigate-value-domain', id)"
    />
  </component>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Menu, Search } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { conceptDomainApi, type ConceptDomain } from '@/api/dataElementStandard'
import ConceptDomainFormModal from './components/ConceptDomainFormModal.vue'
import ConceptDomainDetailDrawer from './components/ConceptDomainDetailDrawer.vue'

const props = withDefaults(
  defineProps<{
    embedded?: boolean
  }>(),
  {
    embedded: false,
  },
)

const emit = defineEmits<{
  (e: 'navigate-value-domain', conceptDomainId: number): void
  (e: 'navigate-dec', conceptDomainId: number): void
}>()

const formModalRef = ref()
const detailDrawerRef = ref()

const selectedType = ref<string>('ALL')
const keyword = ref('')
const status = ref<string>()
const loading = ref(false)
const dataList = ref<ConceptDomain[]>([])

const typeCounts = reactive({
  enumerable: 0,
  nonEnumerable: 0,
})

const pagination = reactive({
  current: 1,
  pageSize: 15,
  total: 0,
})

const typeMenuItems = [
  { key: 'ALL', label: '全部概念域' },
  { key: 'ENUMERABLE', label: '可枚举概念域' },
  { key: 'NON_ENUMERABLE', label: '不可枚举概念域' },
]

const statusLabel = (st?: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    REVIEWED: '已审核',
    APPROVED: '已批准',
    RETIRED: '已废止',
  }
  return (st && map[st]) || st || '草稿'
}

const fetchList = async (page = pagination.current) => {
  loading.value = true
  try {
    const params: any = {
      keyword: keyword.value?.trim() || undefined,
      status: status.value || undefined,
      page,
      page_size: pagination.pageSize,
    }
    if (selectedType.value !== 'ALL') {
      params.domainType = selectedType.value
    }

    const res = await conceptDomainApi.list(params)
    const data = (res as any)?.data?.data || (res as any)?.data || {}
    dataList.value = data.content || (Array.isArray(data) ? data : [])
    pagination.total = data.totalElements ?? dataList.value.length
    pagination.current = page

    // Update counts
    if (selectedType.value === 'ALL') {
      typeCounts.enumerable = dataList.value.filter(i => i.domainType === 'ENUMERABLE').length
      typeCounts.nonEnumerable = dataList.value.length - typeCounts.enumerable
    }
  } catch (error) {
    ElMessage.error('获取概念域列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (key: string) => {
  selectedType.value = key
  fetchList(1)
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
  status.value = undefined
  selectedType.value = 'ALL'
  fetchList(1)
}

const handleCreate = () => {
  formModalRef.value?.open()
}

const handleEdit = (record: ConceptDomain) => {
  formModalRef.value?.open(record)
}

const handleDelete = async (record: ConceptDomain) => {
  try {
    await conceptDomainApi.delete(record.id)
    ElMessage.success('删除成功')
    fetchList(pagination.current)
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const openDetail = (record: ConceptDomain) => {
  detailDrawerRef.value?.open(record.id)
}

onMounted(() => {
  fetchList()
})

defineExpose({
  openCreate: handleCreate,
  refresh: () => fetchList(1),
})
</script>

<style lang="scss" scoped>
.concept-domain-view-wrapper {
  height: 100%;
}

.concept-domain-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 240px);
}

.left-type-nav {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px;
  height: 100%;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.type-nav-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 12px;

  .nav-icon {
    color: #0ea5e9;
  }
}

.type-menu-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.type-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #64748b;
  font-size: 13px;
  transition: all 0.2s ease;

  &:hover {
    background: #f0f9ff;
    color: #0ea5e9;
  }

  &.active {
    background: #f0f9ff;
    color: #0ea5e9;
    font-weight: 600;
    border-left: 3px solid #0ea5e9;
  }
}

.type-count-badge {
  font-size: 11px;
  background: #f8fafc;
  color: #94a3b8;
  padding: 1px 6px;
  border-radius: 999px;
}

.standard-tip-card {
  margin-top: auto;
  padding: 12px;
  border-radius: 8px;
  background: #f0f9ff;
  border: 1px solid #7dd3fc;

  .tip-title {
    font-size: 12px;
    font-weight: 600;
    color: #0ea5e9;
    margin-bottom: 4px;
  }

  .tip-desc {
    font-size: 11px;
    color: #94a3b8;
    line-height: 1.5;
  }
}

.right-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  gap: 12px;
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

.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;

  &.draft {
    background: #f8fafc;
    color: #94a3b8;
  }
  &.reviewed {
    background: rgba(56, 189, 248, 0.12);
    color: #38bdf8;
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

.action-btn {
  color: #0ea5e9;
  padding: 0 4px;
  &:hover {
    color: #38bdf8;
  }
}
</style>
