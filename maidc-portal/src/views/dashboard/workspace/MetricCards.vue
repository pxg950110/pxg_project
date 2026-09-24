<template>
  <div class="metric-cards-wrap relative">
    <!-- 顶部卡片排序与显示设置 -->
    <div class="flex justify-end items-center mb-3">
      <el-popover trigger="click" placement="bottom-end" :width="260" v-model:visible="settingsOpen">
        <template #reference>
          <button
            type="button"
            class="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-slate-500 hover:text-sky-600 hover:bg-slate-100 rounded-md transition-colors border-0 bg-transparent cursor-pointer"
            title="卡片显示与排序偏好"
          >
            <el-icon :size="14"><Setting /></el-icon>
            <span>卡片配置</span>
          </button>
        </template>
        <div class="p-1">
          <div class="flex justify-between items-center pb-2 mb-2 border-b border-slate-100">
            <span class="text-xs font-semibold text-slate-800">指标卡片偏好</span>
            <el-button link type="primary" class="!text-xs" @click="resetPrefs">恢复默认</el-button>
          </div>
          <div class="space-y-2 max-h-64 overflow-y-auto">
            <div
              v-for="row in settingRows"
              :key="row.key"
              class="flex items-center justify-between gap-2 p-1.5 rounded hover:bg-slate-50 text-xs text-slate-700"
            >
              <div class="flex items-center gap-2">
                <el-switch
                  size="small"
                  :model-value="row.visible"
                  @change="(v: any) => toggleHidden(row.key, !!v)"
                />
                <span class="truncate max-w-[120px]">{{ row.label }}</span>
              </div>
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  class="p-1 text-slate-400 hover:text-slate-700 disabled:opacity-30 disabled:cursor-not-allowed border-0 bg-transparent cursor-pointer"
                  :disabled="row.first"
                  @click="move(row.key, -1)"
                >
                  <el-icon :size="12"><Top /></el-icon>
                </button>
                <button
                  type="button"
                  class="p-1 text-slate-400 hover:text-slate-700 disabled:opacity-30 disabled:cursor-not-allowed border-0 bg-transparent cursor-pointer"
                  :disabled="row.last"
                  @click="move(row.key, 1)"
                >
                  <el-icon :size="12"><Bottom /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </div>
      </el-popover>
    </div>

    <!-- 4 列响应式网格指标卡 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div
        v-for="card in displayCards"
        :key="card.key"
        class="group relative bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm hover:shadow-clinical hover:border-sky-300 transition-all duration-200 cursor-pointer overflow-hidden flex flex-col justify-between"
        @click="handleClick(card)"
      >
        <!-- 顶部 2.5px 状态渐变指示条 -->
        <div
          class="absolute top-0 left-0 right-0 h-[2.5px] transition-colors"
          :class="toneBorderMap[card.tone ?? ''] || 'bg-gradient-to-r from-sky-400 to-sky-600'"
        />

        <div class="flex items-start justify-between">
          <div class="space-y-1">
            <span class="text-xs font-medium text-slate-500 block truncate">{{ card.label }}</span>
            <div class="flex items-baseline gap-1">
              <span class="text-2xl font-bold tracking-tight text-slate-900 font-mono">
                {{ card.value }}
              </span>
              <span v-if="card.suffix" class="text-xs font-medium text-slate-400">
                {{ card.suffix }}
              </span>
            </div>
          </div>

          <!-- 右侧图标胶囊背景 -->
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center transition-transform group-hover:scale-105"
            :class="toneBgMap[card.tone ?? ''] || 'bg-sky-50 text-sky-600'"
          >
            <el-icon :size="20">
              <component :is="resolveIcon(card.icon)" />
            </el-icon>
          </div>
        </div>

        <!-- 底部微指示/路由提示 -->
        <div class="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
          <span class="inline-flex items-center gap-1 group-hover:text-sky-600 transition-colors">
            查看详情
            <el-icon :size="10"><ArrowRight /></el-icon>
          </span>
          <span v-if="card.tone === 'danger'" class="text-rose-500 font-medium">需重点关注</span>
          <span v-else-if="card.tone === 'warning'" class="text-amber-500 font-medium">待处理</span>
          <span v-else class="text-emerald-500 font-medium">运行正常</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Setting,
  Top,
  Bottom,
  ArrowRight,
  User,
  Calendar,
  Cpu,
  Warning,
  Tickets,
  TrendCharts,
  CircleCheck,
} from '@element-plus/icons-vue'
import type { MetricCard, MetricsInfo } from '@/api/workspace'

const props = defineProps<{
  /** v2 角色化卡片，服务端按角色组下发 */
  cards?: MetricCard[] | null
  /** 旧响应（无 cards）时回退渲染 v1 模型四卡 */
  metrics?: MetricsInfo | null
  loading: boolean
}>()

const router = useRouter()

const legacyCards = (metrics?: MetricsInfo | null): MetricCard[] => [
  { key: 'model_count', label: '模型总数', value: metrics?.modelCount ?? 0, suffix: '个', icon: 'experiment', tone: 'primary' },
  { key: 'active_deployments', label: '活跃部署', value: metrics?.activeDeployments ?? 0, suffix: '个', icon: 'rocket', tone: 'success' },
  { key: 'daily_inferences', label: '今日推理', value: metrics?.dailyInferences ?? 0, suffix: '次', icon: 'thunderbolt', tone: 'primary' },
  { key: 'pending_approvals', label: '待审批', value: metrics?.pendingApprovals ?? 0, suffix: '项', icon: 'audit', tone: 'warning' },
]

const sourceCards = computed(() =>
  props.cards && props.cards.length > 0 ? props.cards : legacyCards(props.metrics)
)

// 视觉颜色映射
const toneBorderMap: Record<string, string> = {
  primary: 'bg-gradient-to-r from-sky-400 to-sky-600',
  success: 'bg-gradient-to-r from-emerald-400 to-emerald-600',
  warning: 'bg-gradient-to-r from-amber-400 to-amber-600',
  danger: 'bg-gradient-to-r from-rose-400 to-rose-600',
  purple: 'bg-gradient-to-r from-violet-400 to-violet-600',
}

const toneBgMap: Record<string, string> = {
  primary: 'bg-sky-50 text-sky-600',
  success: 'bg-emerald-50 text-emerald-600',
  warning: 'bg-amber-50 text-amber-600',
  danger: 'bg-rose-50 text-rose-600',
  purple: 'bg-violet-50 text-violet-600',
}

const iconComponentMap: Record<string, any> = {
  user: User,
  calendar: Calendar,
  experiment: Cpu,
  rocket: Cpu,
  thunderbolt: TrendCharts,
  audit: Tickets,
  alert: Warning,
  success: CircleCheck,
}

const resolveIcon = (iconName: string) => {
  return iconComponentMap[iconName] || Cpu
}

// 偏好本地持久化
const PREF_KEY = 'maidc-workspace-cards'

interface CardPrefs {
  hidden: string[]
  order: string[]
}

function loadPrefs(): CardPrefs {
  try {
    const raw = localStorage.getItem(PREF_KEY)
    if (raw) {
      const p = JSON.parse(raw)
      return {
        hidden: Array.isArray(p?.hidden) ? p.hidden : [],
        order: Array.isArray(p?.order) ? p.order : [],
      }
    }
  } catch {
    // ignore
  }
  return { hidden: [], order: [] }
}

const prefs = ref<CardPrefs>(loadPrefs())
const settingsOpen = ref(false)

function savePrefs() {
  try {
    localStorage.setItem(PREF_KEY, JSON.stringify(prefs.value))
  } catch {
    // ignore
  }
}

function currentOrder(): string[] {
  const keys = sourceCards.value.map((c) => c.key)
  const known = prefs.value.order.filter((k) => keys.includes(k))
  const rest = keys.filter((k) => !known.includes(k))
  return [...known, ...rest]
}

const displayCards = computed(() => {
  const order = currentOrder()
  const byKey = new Map(sourceCards.value.map((c) => [c.key, c]))
  return order.map((k) => byKey.get(k)!).filter((c) => c && !prefs.value.hidden.includes(c.key))
})

const settingRows = computed(() => {
  const order = currentOrder()
  const byKey = new Map(sourceCards.value.map((c) => [c.key, c]))
  return order.map((key, idx) => ({
    key,
    label: byKey.get(key)?.label ?? key,
    visible: !prefs.value.hidden.includes(key),
    first: idx === 0,
    last: idx === order.length - 1,
  }))
})

function toggleHidden(key: string, visible: boolean) {
  const hidden = new Set(prefs.value.hidden)
  if (visible) hidden.delete(key)
  else hidden.add(key)
  prefs.value = { ...prefs.value, hidden: [...hidden] }
  savePrefs()
}

function move(key: string, dir: -1 | 1) {
  const keys = currentOrder()
  const i = keys.indexOf(key)
  const j = i + dir
  if (i < 0 || j < 0 || j >= keys.length) return
  ;[keys[i], keys[j]] = [keys[j], keys[i]]
  prefs.value = { ...prefs.value, order: keys }
  savePrefs()
}

function resetPrefs() {
  prefs.value = { hidden: [], order: [] }
  localStorage.removeItem(PREF_KEY)
}

function handleClick(card: MetricCard) {
  if (card.route) router.push(card.route)
}
</script>
