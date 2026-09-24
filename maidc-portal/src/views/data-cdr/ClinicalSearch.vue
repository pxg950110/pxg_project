<template>
  <div class="clinical-search-container max-w-[1600px] mx-auto space-y-4">
    <!-- 顶部检索输入主卡片 -->
    <div class="bg-white rounded-xl border border-slate-200/80 p-6 shadow-clinical-sm">
      <div class="max-w-3xl mx-auto space-y-4">
        <div class="text-center space-y-1 mb-2">
          <h2 class="text-xl font-bold text-slate-900 tracking-tight flex items-center justify-center gap-2">
            <span class="w-1.5 h-5 bg-sky-500 rounded-full" />
            临床多模态智能检索
          </h2>
          <p class="text-xs text-slate-500">
            支持患者基本信息、诊断记录、检验危急值、用药处方、影像报告及临床文书跨域联想搜索
          </p>
        </div>

        <!-- 搜索输入栏 -->
        <div class="flex items-center gap-2">
          <el-input
            v-model="keyword"
            placeholder="输入患者姓名、住院号、ICD 诊断、检验项目关键词..."
            size="large"
            clearable
            class="flex-1"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon class="text-slate-400"><Search /></el-icon>
            </template>
          </el-input>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="!px-6 !font-medium"
            @click="handleSearch"
          >
            检索
          </el-button>
          <el-button
            link
            type="primary"
            class="!text-xs"
            @click="showAdvanced = !showAdvanced"
          >
            {{ showAdvanced ? '收起高级筛选' : '高级多维筛选' }}
            <el-icon class="ml-1 transition-transform" :class="{ 'rotate-180': showAdvanced }">
              <ArrowDown />
            </el-icon>
          </el-button>
        </div>

        <!-- 高级筛选折叠区 -->
        <el-collapse-transition>
          <div
            v-if="showAdvanced"
            class="p-4 rounded-lg bg-slate-50 border border-slate-200/60 mt-3 space-y-3"
          >
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
              <div class="space-y-1.5">
                <label class="font-medium text-slate-700">限制检索域（可多选）</label>
                <el-select
                  v-model="selectedDomains"
                  multiple
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="默认检索全部临床域"
                  class="w-full"
                >
                  <el-option
                    v-for="item in domainOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </div>

              <div class="space-y-1.5">
                <label class="font-medium text-slate-700">就诊日期范围</label>
                <el-date-picker
                  v-model="filterForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  class="!w-full"
                />
              </div>
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <!-- 聚合统计与多域快捷过滤栏 -->
    <div
      v-if="searched && total > 0"
      class="bg-white rounded-xl border border-slate-200/80 p-3.5 shadow-clinical-sm flex items-center justify-between flex-wrap gap-3 text-xs"
    >
      <div class="flex items-center gap-2 text-slate-600">
        <span>检索命中共 <b class="text-sky-600 font-mono text-sm">{{ total }}</b> 条记录</span>
        <span class="text-slate-300">|</span>
        <span>点击分类芯片快捷单选过滤：</span>
      </div>

      <div class="flex items-center gap-1.5 flex-wrap">
        <button
          v-for="(count, domain) in aggregations"
          :key="domain"
          type="button"
          class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-all cursor-pointer"
          :class="selectedDomains.length === 1 && selectedDomains[0] === String(domain)
            ? 'bg-sky-50 text-sky-700 border-sky-300 shadow-sm'
            : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'"
          @click="toggleDomain(String(domain))"
        >
          <span>{{ domainLabelMap[String(domain)] || domain }}</span>
          <span class="px-1.5 py-0.2 rounded-full bg-white/80 font-mono text-[11px] text-slate-500">
            {{ count }}
          </span>
        </button>
      </div>
    </div>

    <!-- 检索结果卡片流 -->
    <div v-loading="loading" class="space-y-3 min-h-[300px]">
      <!-- 空状态 -->
      <div
        v-if="searched && results.length === 0"
        class="bg-white rounded-xl border border-slate-200/80 py-16 text-center space-y-2"
      >
        <el-icon :size="48" class="text-slate-300"><DocumentDelete /></el-icon>
        <p class="text-sm font-medium text-slate-500">未找到匹配的临床记录</p>
        <p class="text-xs text-slate-400">请尝试更换检索关键词或扩大检索日期与领域范围</p>
      </div>

      <!-- 结果卡片列表 -->
      <div
        v-for="item in results"
        :key="item.id || item.title"
        class="group bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical hover:border-sky-300 transition-all duration-200 flex flex-col justify-between gap-2"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="space-y-1 flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <!-- 领域彩色微胶囊 -->
              <span
                class="px-2 py-0.5 rounded text-[11px] font-semibold"
                :class="domainStyleMap[item.domain] || 'bg-slate-100 text-slate-700 border border-slate-200'"
              >
                {{ domainLabelMap[item.domain] || item.domain }}
              </span>

              <h4 class="text-sm font-bold text-slate-900 group-hover:text-sky-600 transition-colors m-0 truncate">
                {{ item.title || '-' }}
              </h4>

              <span v-if="item.subtitle" class="text-xs text-slate-400">
                · {{ item.subtitle }}
              </span>
            </div>

            <!-- 患者基本信息外链 -->
            <div v-if="item.patientId" class="text-xs text-slate-500 flex items-center gap-1.5 pt-1">
              <span>关联患者：</span>
              <button
                type="button"
                class="text-sky-600 hover:text-sky-700 hover:underline font-medium border-0 bg-transparent cursor-pointer p-0"
                @click="router.push(`/data/cdr/patients/${item.patientId}`)"
              >
                {{ item.patientName || `患者 #${item.patientId}` }}
              </button>
              <span v-if="item.encounterId" class="text-slate-400">
                （就诊流水：{{ item.encounterId }}）
              </span>
            </div>

            <!-- 命中片段高亮内容 -->
            <div
              v-if="item.headline"
              class="text-xs text-slate-600 pt-1 leading-relaxed result-headline"
              v-html="item.headline"
            />
          </div>

          <!-- 右侧直达操作 -->
          <div class="flex-shrink-0">
            <el-button
              size="small"
              class="!text-xs"
              @click="handleViewDetail(item)"
            >
              查看详情
              <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 分页栏 -->
      <div v-if="searched && total > 0" class="flex justify-end pt-3">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          size="small"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowDown, ArrowRight, DocumentDelete } from '@element-plus/icons-vue'
import { smartSearch } from '@/api/data'

defineOptions({ name: 'ClinicalSearch' })
const router = useRouter()

// 域风格与色系映射
const domainStyleMap: Record<string, string> = {
  PATIENT: 'bg-sky-50 text-sky-700 border border-sky-200',
  ENCOUNTER: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  DIAGNOSIS: 'bg-amber-50 text-amber-700 border border-amber-200',
  LAB: 'bg-cyan-50 text-cyan-700 border border-cyan-200',
  MEDICATION: 'bg-purple-50 text-purple-700 border border-purple-200',
  IMAGING: 'bg-blue-50 text-blue-700 border border-blue-200',
  SURGERY: 'bg-rose-50 text-rose-700 border border-rose-200',
  PATHOLOGY: 'bg-pink-50 text-pink-700 border border-pink-200',
  VITAL: 'bg-teal-50 text-teal-700 border border-teal-200',
  ALLERGY: 'bg-red-50 text-red-700 border border-red-200',
  NOTE: 'bg-yellow-50 text-yellow-800 border border-yellow-200',
  PROJECT: 'bg-indigo-50 text-indigo-700 border border-indigo-200',
  DATASET: 'bg-slate-100 text-slate-700 border border-slate-200',
}

// 域标签名称映射
const domainLabelMap: Record<string, string> = {
  PATIENT: '患者',
  ENCOUNTER: '就诊',
  DIAGNOSIS: '诊断',
  LAB: '检验',
  MEDICATION: '用药',
  IMAGING: '影像',
  SURGERY: '手术',
  PATHOLOGY: '病理',
  VITAL: '体征',
  ALLERGY: '过敏',
  NOTE: '文书',
  PROJECT: '科研项目',
  DATASET: '数据集',
}

const domainOptions = Object.entries(domainLabelMap).map(([value, label]) => ({ value, label }))

const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const total = ref(0)
const results = ref<any[]>([])
const aggregations = ref<Record<string, number>>({})

const showAdvanced = ref(false)
const selectedDomains = ref<string[]>([])
const filterForm = reactive({
  dateRange: null as any,
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
})

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    const body: { keyword: string; domains?: string[]; dateFrom?: string; dateTo?: string; page?: number; pageSize?: number } = {
      keyword: keyword.value.trim(),
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (selectedDomains.value.length > 0) {
      body.domains = selectedDomains.value
    }
    if (filterForm.dateRange?.length === 2) {
      body.dateFrom = filterForm.dateRange[0]
      body.dateTo = filterForm.dateRange[1]
    }
    const res = await smartSearch(body)
    const data = res.data?.data || {}
    results.value = data.items || []
    total.value = data.total || 0
    aggregations.value = data.aggregations || {}
    searched.value = true
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  doSearch()
}

function handlePageChange(page: number) {
  pagination.current = page
  doSearch()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  doSearch()
}

function toggleDomain(domain: string) {
  if (selectedDomains.value.length === 1 && selectedDomains.value[0] === domain) {
    selectedDomains.value = []
  } else {
    selectedDomains.value = [domain]
  }
  handleSearch()
}

function handleViewDetail(item: any) {
  if (item.patientId) {
    router.push(`/data/cdr/patients/${item.patientId}`)
  } else if (item.url) {
    router.push(item.url)
  }
}
</script>

<style scoped>
.result-headline :deep(b) {
  color: #0284c7;
  font-weight: 700;
  background-color: #f0f9ff;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
