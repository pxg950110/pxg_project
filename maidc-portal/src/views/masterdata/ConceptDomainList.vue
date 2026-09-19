<template>
  <component :is="embedded ? 'div' : PageContainer" :title="embedded ? undefined : '概念域管理'" class="concept-domain-view-wrapper">
    <template v-if="!embedded" #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增概念域
      </a-button>
    </template>

    <div class="concept-domain-layout">
      <!-- Left: Type filter -->
      <div class="left-type-nav">
        <div class="type-nav-header">
          <AppstoreOutlined class="nav-icon" />
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
          <a-row :gutter="12" align="middle">
            <a-col :span="10">
              <a-input-search
                v-model:value="keyword"
                placeholder="搜索概念域代码、名称、英文名"
                enter-button="搜索"
                @search="fetchList(1)"
                allow-clear
              />
            </a-col>
            <a-col :span="5">
              <a-select v-model:value="status" placeholder="审核状态" allow-clear style="width: 100%" @change="fetchList(1)">
                <a-select-option value="DRAFT">草稿 (DRAFT)</a-select-option>
                <a-select-option value="REVIEWED">已审核 (REVIEWED)</a-select-option>
                <a-select-option value="APPROVED">已批准 (APPROVED)</a-select-option>
                <a-select-option value="RETIRED">已废止 (RETIRED)</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="9" style="text-align: right">
              <a-space>
                <a-button @click="resetFilters">重置</a-button>
                <a-button v-if="embedded" type="primary" @click="handleCreate">
                  <template #icon><PlusOutlined /></template>
                  新增概念域
                </a-button>
              </a-space>
            </a-col>
          </a-row>
        </div>

        <!-- Table -->
        <div class="table-panel">
          <a-table
            :columns="columns"
            :data-source="dataList"
            :loading="loading"
            row-key="id"
            size="middle"
            :pagination="pagination"
            @change="handleTableChange"
            :scroll="{ x: 1000, y: 'calc(100vh - 420px)' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <span class="code-tag">{{ record.code }}</span>
              </template>

              <template v-if="column.key === 'name'">
                <div class="name-cell">
                  <span class="name-main" @click="openDetail(record)">{{ record.name }}</span>
                  <span v-if="record.nameEn" class="name-sub">{{ record.nameEn }}</span>
                </div>
              </template>

              <template v-if="column.key === 'domainType'">
                <span :class="['domain-tag', record.domainType === 'ENUMERABLE' ? 'enumerable' : 'non-enumerable']">
                  {{ record.domainType === 'ENUMERABLE' ? '● 可枚举' : '○ 不可枚举' }}
                </span>
              </template>

              <template v-if="column.key === 'status'">
                <span :class="['status-badge', record.status ? record.status.toLowerCase() : 'draft']">
                  {{ statusLabel(record.status) }}
                </span>
              </template>

              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" class="action-btn" @click="openDetail(record)">
                    详情 / 值含义
                  </a-button>
                  <a-button type="link" size="small" class="action-btn" @click="emit('navigate-value-domain', record.id)">
                    关联值域
                  </a-button>
                  <a-button type="link" size="small" class="action-btn" @click="handleEdit(record)">
                    编辑
                  </a-button>
                  <a-popconfirm title="确认删除该概念域？" ok-text="确认" cancel-text="取消" @confirm="handleDelete(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
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
import { message } from 'ant-design-vue'
import { PlusOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
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
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条标准概念域`,
})

const typeMenuItems = [
  { key: 'ALL', label: '全部概念域' },
  { key: 'ENUMERABLE', label: '可枚举概念域' },
  { key: 'NON_ENUMERABLE', label: '不可枚举概念域' },
]

const columns = [
  { title: '代码 (Code)', key: 'code', dataIndex: 'code', width: 140, fixed: 'left' },
  { title: '概念域名称', key: 'name', width: 220 },
  { title: '类型', key: 'domainType', width: 130 },
  { title: '测量维度', dataIndex: 'dimension', width: 110 },
  { title: '版本', dataIndex: 'version', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
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
    message.error('获取概念域列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (key: string) => {
  selectedType.value = key
  fetchList(1)
}

const handleTableChange = (pag: any) => {
  pagination.pageSize = pag.pageSize
  fetchList(pag.current)
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
    message.success('删除成功')
    fetchList(pagination.current)
  } catch (error) {
    message.error('删除失败')
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
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.type-nav-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;

  .nav-icon {
    color: #1677ff;
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
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
  transition: all 0.2s ease;

  &:hover {
    background: #f0f7ff;
    color: #1677ff;
  }

  &.active {
    background: #e6f4ff;
    color: #1677ff;
    font-weight: 600;
    border-left: 3px solid #1677ff;
  }
}

.type-count-badge {
  font-size: 11px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.45);
  padding: 1px 6px;
  border-radius: 999px;
}

.standard-tip-card {
  margin-top: auto;
  padding: 12px;
  border-radius: 8px;
  background: #e6f4ff;
  border: 1px solid #91caff;

  .tip-title {
    font-size: 12px;
    font-weight: 600;
    color: #1677ff;
    margin-bottom: 4px;
  }

  .tip-desc {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
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
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.table-panel {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #f0f0f0;
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
    color: rgba(0, 0, 0, 0.88);
    cursor: pointer;
    &:hover {
      color: #1677ff;
      text-decoration: underline;
    }
  }

  .name-sub {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.domain-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;

  &.enumerable {
    background: #e6f4ff;
    color: #1677ff;
    border: 1px solid #91caff;
  }

  &.non-enumerable {
    background: #f6ffed;
    color: #52c41a;
    border: 1px solid #b7eb8f;
  }
}

.status-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;

  &.draft {
    background: #fafafa;
    color: rgba(0, 0, 0, 0.45);
  }
  &.reviewed {
    background: rgba(56, 189, 248, 0.12);
    color: #38bdf8;
  }
  &.approved {
    background: #f6ffed;
    color: #52c41a;
  }
  &.retired {
    background: #fff1f0;
    color: #ff4d4f;
  }
}

.action-btn {
  color: #1677ff;
  padding: 0 4px;
  &:hover {
    color: #4096ff;
  }
}
</style>
