<template>
  <div class="pro-table-wrapper space-y-4">
    <!-- 顶部检索表单卡片 -->
    <div
      v-if="$slots.search"
      class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm transition-all"
    >
      <div class="search-form-inner">
        <slot name="search" :collapsed="isSearchCollapsed" />
      </div>

      <!-- 搜索控制按钮行（如果需要底部操作或自适应收起） -->
      <div v-if="showSearchCollapse" class="flex justify-end items-center gap-2 pt-2 border-t border-slate-100 mt-3">
        <slot name="searchAction" />
        <el-button
          link
          type="primary"
          class="!text-xs !font-medium"
          @click="isSearchCollapsed = !isSearchCollapsed"
        >
          {{ isSearchCollapsed ? '展开筛选' : '收起筛选' }}
          <el-icon class="ml-1 transition-transform" :class="{ 'rotate-180': !isSearchCollapsed }">
            <ArrowDown />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 主数据卡片 -->
    <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-col">
      <!-- 工具栏 (Toolbar) -->
      <div class="flex items-center justify-between pb-4 gap-4 flex-wrap">
        <!-- 左侧业务操作插槽 -->
        <div class="flex items-center gap-2.5 flex-wrap">
          <div v-if="title" class="text-base font-semibold text-slate-900 mr-2 flex items-center gap-2">
            <span class="w-1 h-4 bg-sky-500 rounded-full" />
            {{ title }}
          </div>
          <slot name="toolbar" />
        </div>

        <!-- 右侧通用表格工具项 -->
        <div class="flex items-center gap-2 text-slate-500">
          <slot name="toolbarRight" />

          <!-- 刷新 -->
          <el-tooltip content="刷新数据" placement="top">
            <button
              class="p-2 rounded-lg hover:bg-slate-100 text-slate-600 transition-colors cursor-pointer border-0 bg-transparent flex items-center justify-center"
              @click="handleRefresh"
            >
              <el-icon :size="16" :class="{ 'animate-spin': loading }">
                <RefreshRight />
              </el-icon>
            </button>
          </el-tooltip>

          <!-- 密度调节 -->
          <el-dropdown trigger="click" @command="handleDensityChange">
            <el-tooltip content="表格密度" placement="top">
              <button
                class="p-2 rounded-lg hover:bg-slate-100 text-slate-600 transition-colors cursor-pointer border-0 bg-transparent flex items-center justify-center"
              >
                <el-icon :size="16"><Sort /></el-icon>
              </button>
            </el-tooltip>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="small" :class="{ '!text-sky-600 font-semibold': tableSize === 'small' }">
                  紧凑
                </el-dropdown-item>
                <el-dropdown-item command="default" :class="{ '!text-sky-600 font-semibold': tableSize === 'default' }">
                  默认
                </el-dropdown-item>
                <el-dropdown-item command="large" :class="{ '!text-sky-600 font-semibold': tableSize === 'large' }">
                  宽松
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 列展示配置 -->
          <el-popover placement="bottom-end" :width="220" trigger="click">
            <template #reference>
              <button
                class="p-2 rounded-lg hover:bg-slate-100 text-slate-600 transition-colors cursor-pointer border-0 bg-transparent flex items-center justify-center"
              >
                <el-icon :size="16"><Operation /></el-icon>
              </button>
            </template>
            <div class="p-1">
              <div class="flex items-center justify-between pb-2 mb-2 border-b border-slate-100">
                <span class="text-xs font-semibold text-slate-800">列展示设置</span>
                <el-button link type="primary" class="!text-xs" @click="resetColumns">重置</el-button>
              </div>
              <div class="max-h-60 overflow-y-auto space-y-1.5 py-1">
                <div
                  v-for="col in allColumns"
                  :key="col.prop || col.label"
                  class="flex items-center gap-2 text-xs text-slate-700 hover:bg-slate-50 p-1.5 rounded cursor-pointer"
                  @click="toggleCol(col.prop)"
                >
                  <el-checkbox
                    :model-value="!hiddenColumnProps.includes(col.prop)"
                    @click.stop
                    @change="toggleCol(col.prop)"
                  />
                  <span class="truncate flex-1">{{ col.label }}</span>
                </div>
              </div>
            </div>
          </el-popover>
        </div>
      </div>

      <!-- 核心 el-table -->
      <div class="flex-1 w-full overflow-hidden">
        <el-table
          ref="tableRef"
          v-loading="loading"
          :data="data"
          :size="tableSize"
          :row-key="rowKey"
          :stripe="stripe"
          :border="border"
          :height="height"
          :max-height="maxHeight"
          class="w-full clinical-pro-table"
          header-row-class-name="!bg-slate-50 !text-slate-700 !font-semibold text-xs uppercase"
          @selection-change="handleSelectionChange"
          @sort-change="handleSortChange"
        >
          <!-- 多选列 -->
          <el-table-column
            v-if="selectable"
            type="selection"
            width="46"
            align="center"
            fixed="left"
          />

          <!-- 序号列 -->
          <el-table-column
            v-if="showIndex"
            type="index"
            label="#"
            width="52"
            align="center"
            fixed="left"
          />

          <!-- 动态配置列 -->
          <template v-for="col in visibleColumns" :key="col.prop || col.label">
            <el-table-column
              :prop="col.prop"
              :label="col.label"
              :width="col.width"
              :min-width="col.minWidth || 100"
              :fixed="col.fixed"
              :sortable="col.sortable ? 'custom' : false"
              :align="col.align || 'left'"
              :show-overflow-tooltip="col.showOverflowTooltip !== false"
            >
              <!-- 自定义表头插槽 -->
              <template v-if="$slots[`header-${col.prop}`]" #header="scope">
                <slot :name="`header-${col.prop}`" v-bind="scope" />
              </template>

              <!-- 自定义单元格内容插槽 -->
              <template #default="scope">
                <slot :name="col.prop || col.slot" v-bind="scope">
                  {{ scope.row[col.prop] ?? '-' }}
                </slot>
              </template>
            </el-table-column>
          </template>

          <!-- 空状态插槽支持 -->
          <template #empty>
            <div class="py-12 flex flex-col items-center justify-center text-slate-400">
              <el-icon :size="48" class="text-slate-300 mb-2"><DocumentDelete /></el-icon>
              <p class="text-sm font-medium">{{ emptyText || '暂无相关临床数据' }}</p>
            </div>
          </template>
        </el-table>
      </div>

      <!-- 分页区域 -->
      <div
        v-if="pagination !== false"
        class="pt-4 mt-2 border-t border-slate-100 flex items-center justify-between flex-wrap gap-3"
      >
        <div class="text-xs text-slate-500">
          共 <span class="font-semibold text-slate-700">{{ totalCount }}</span> 条记录
          <span v-if="selectedRows.length > 0" class="ml-2 text-sky-600 font-medium">
            已选择 {{ selectedRows.length }} 项
          </span>
        </div>

        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalCount"
          :page-sizes="pageSizes"
          :layout="paginationLayout"
          background
          class="!font-normal"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  ArrowDown,
  RefreshRight,
  Sort,
  Operation,
  DocumentDelete,
} from '@element-plus/icons-vue'

export interface ProColumn {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  fixed?: boolean | 'left' | 'right'
  sortable?: boolean
  slot?: string
  hidden?: boolean
  align?: 'left' | 'center' | 'right'
  showOverflowTooltip?: boolean
}

const props = withDefaults(
  defineProps<{
    title?: string
    data?: any[]
    columns: ProColumn[]
    loading?: boolean
    rowKey?: string
    selectable?: boolean
    showIndex?: boolean
    stripe?: boolean
    border?: boolean
    height?: string | number
    maxHeight?: string | number
    emptyText?: string
    showSearchCollapse?: boolean
    pagination?: boolean | object
    total?: number
    page?: number
    pageSize?: number
    pageSizes?: number[]
  }>(),
  {
    title: '',
    data: () => [],
    loading: false,
    rowKey: 'id',
    selectable: false,
    showIndex: false,
    stripe: false,
    border: false,
    height: undefined,
    maxHeight: undefined,
    emptyText: '',
    showSearchCollapse: false,
    pagination: true,
    total: 0,
    page: 1,
    pageSize: 10,
    pageSizes: () => [10, 20, 50, 100],
  }
)

const emit = defineEmits<{
  (e: 'update:page', val: number): void
  (e: 'update:pageSize', val: number): void
  (e: 'pagination-change', params: { page: number; pageSize: number }): void
  (e: 'refresh'): void
  (e: 'selection-change', rows: any[]): void
  (e: 'sort-change', val: { column: any; prop: string; order: string }): void
}>()

// 检索区折叠控制
const isSearchCollapsed = ref(false)

// 表格尺寸密度: small | default | large
const tableSize = ref<'small' | 'default' | 'large'>('default')

// 选中行
const selectedRows = ref<any[]>([])

// 列显隐逻辑
const allColumns = computed(() => props.columns || [])
const hiddenColumnProps = ref<string[]>(
  props.columns.filter((c) => c.hidden).map((c) => c.prop)
)

const visibleColumns = computed(() =>
  allColumns.value.filter((col) => !hiddenColumnProps.value.includes(col.prop))
)

const toggleCol = (prop: string) => {
  const index = hiddenColumnProps.value.indexOf(prop)
  if (index > -1) {
    hiddenColumnProps.value.splice(index, 1)
  } else {
    // 至少保留一列
    if (visibleColumns.value.length > 1) {
      hiddenColumnProps.value.push(prop)
    }
  }
}

const resetColumns = () => {
  hiddenColumnProps.value = props.columns.filter((c) => c.hidden).map((c) => c.prop)
}

// 分页联动
const currentPage = computed({
  get: () => props.page,
  set: (val) => emit('update:page', val),
})

const pageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val),
})

const totalCount = computed(() => props.total ?? props.data?.length ?? 0)

const paginationLayout = 'prev, pager, next, sizes, jumper'

const handlePageChange = (val: number) => {
  emit('pagination-change', { page: val, pageSize: pageSize.value })
}

const handleSizeChange = (val: number) => {
  emit('pagination-change', { page: 1, pageSize: val })
}

const handleSelectionChange = (rows: any[]) => {
  selectedRows.value = rows
  emit('selection-change', rows)
}

const handleSortChange = (val: any) => {
  emit('sort-change', val)
}

const handleRefresh = () => {
  emit('refresh')
}

const handleDensityChange = (size: 'small' | 'default' | 'large') => {
  tableSize.value = size
}

const tableRef = ref()
defineExpose({
  tableRef,
  selectedRows,
  clearSelection: () => tableRef.value?.clearSelection(),
})
</script>

<style scoped>
:deep(.clinical-pro-table .el-table__header-wrapper th) {
  background-color: #f8fafc !important;
  color: #334155 !important;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.clinical-pro-table .el-table__cell) {
  padding: 10px 0;
}

:deep(.clinical-pro-table.el-table--small .el-table__cell) {
  padding: 6px 0;
}

:deep(.clinical-pro-table.el-table--large .el-table__cell) {
  padding: 14px 0;
}
</style>
