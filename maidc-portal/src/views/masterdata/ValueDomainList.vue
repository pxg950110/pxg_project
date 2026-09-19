<template>
  <component :is="embedded ? 'div' : PageContainer" :title="embedded ? undefined : '值域管理'" class="value-domain-wrapper">
    <template v-if="!embedded" #extra>
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增值域
      </a-button>
    </template>

    <div class="value-domain-container">
      <!-- Filter Bar -->
      <div class="filter-bar">
        <a-row :gutter="12" align="middle">
          <a-col :span="6">
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索值域代码、名称"
              enter-button="搜索"
              @search="fetchList(1)"
              allow-clear
            />
          </a-col>
          <a-col :span="4">
            <a-select v-model:value="domainType" placeholder="值域类型" allow-clear style="width: 100%" @change="fetchList(1)">
              <a-select-option value="ENUMERABLE">可枚举值域</a-select-option>
              <a-select-option value="NON_ENUMERABLE">不可枚举值域</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="6">
            <a-select
              v-model:value="conceptDomainId"
              placeholder="所属概念域"
              allow-clear
              show-search
              :filter-option="filterOption"
              style="width: 100%"
              @change="fetchList(1)"
            >
              <a-select-option v-for="cd in conceptDomains" :key="cd.id" :value="cd.id">
                {{ cd.name }} ({{ cd.code }})
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-select v-model:value="status" placeholder="审核状态" allow-clear style="width: 100%" @change="fetchList(1)">
              <a-select-option value="DRAFT">草稿</a-select-option>
              <a-select-option value="APPROVED">已批准</a-select-option>
              <a-select-option value="RETIRED">已废止</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4" style="text-align: right">
            <a-space>
              <a-button @click="resetFilters">重置</a-button>
              <a-button v-if="embedded" type="primary" @click="handleCreate">
                <template #icon><PlusOutlined /></template>
                新增值域
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </div>

      <!-- Table Panel -->
      <div class="table-panel">
        <a-table
          :columns="columns"
          :data-source="dataList"
          :loading="loading"
          row-key="id"
          size="middle"
          :pagination="pagination"
          @change="handleTableChange"
          :scroll="{ x: 1300, y: 'calc(100vh - 420px)' }"
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

            <template v-if="column.key === 'dataType'">
              <span class="data-type-badge">{{ dataTypeLabel(record.dataType) }}</span>
            </template>

            <template v-if="column.key === 'conceptDomainName'">
              <a v-if="record.conceptDomainName" class="concept-link" @click="goToConceptDomain(record)">
                {{ record.conceptDomainName }}
              </a>
              <span v-else class="text-dim">-</span>
            </template>

            <template v-if="column.key === 'status'">
              <span :class="['status-badge', record.status ? record.status.toLowerCase() : 'draft']">
                {{ statusLabel(record.status) }}
              </span>
            </template>

            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" class="action-btn" @click="openDetail(record)">
                  允许值 / 详情
                </a-button>
                <a-button type="link" size="small" class="action-btn" @click="handleEdit(record)">
                  编辑
                </a-button>
                <a-popconfirm title="确认删除该值域？" ok-text="确认" cancel-text="取消" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
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
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
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
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 个值域`,
})

const columns = [
  { title: '代码 (Code)', key: 'code', dataIndex: 'code', width: 140, fixed: 'left' },
  { title: '值域名称', key: 'name', width: 220 },
  { title: '类型', key: 'domainType', width: 120 },
  { title: '数据类型', key: 'dataType', width: 110 },
  {
    title: '长度',
    width: 90,
    customRender: ({ record }: any) =>
      record.minLength === record.maxLength ? record.maxLength || '-' : `${record.minLength || 0}-${record.maxLength || '-'}`,
  },
  { title: '计量单位', dataIndex: 'unitName', width: 100 },
  { title: '表示类', key: 'representationClass', dataIndex: 'representationClass', width: 100 },
  { title: '所属概念域', key: 'conceptDomainName', width: 180 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' },
]

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

const filterOption = (input: string, option: any) => {
  const text = String(option.children?.map ? option.children.map((c: any) => c.children || '').join('') : option.children || '')
  return text.toLowerCase().includes(input.toLowerCase())
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
    message.error('获取值域列表失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.pageSize = pag.pageSize
  fetchList(pag.current)
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
    message.success('删除成功')
    fetchList(pagination.current)
  } catch (error) {
    message.error('删除失败')
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

.data-type-badge {
  font-size: 11px;
  background: rgba(129, 140, 248, 0.1);
  color: #818cf8;
  border: 1px solid rgba(129, 140, 248, 0.25);
  padding: 2px 6px;
  border-radius: 4px;
}

.concept-link {
  color: #1677ff;
  &:hover {
    color: #4096ff;
    text-decoration: underline;
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
  &.approved {
    background: #f6ffed;
    color: #52c41a;
  }
  &.retired {
    background: #fff1f0;
    color: #ff4d4f;
  }
}

.text-dim {
  color: rgba(0, 0, 0, 0.45);
}

.action-btn {
  color: #1677ff;
  padding: 0 4px;
  &:hover {
    color: #4096ff;
  }
}
</style>
