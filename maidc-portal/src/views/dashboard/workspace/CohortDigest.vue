<template>
  <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm">
    <div class="flex items-center justify-between pb-3 mb-2 border-b border-slate-100">
      <div class="flex items-center gap-2">
        <span class="w-1 h-3.5 bg-violet-500 rounded-full" />
        <h3 class="text-sm font-semibold text-slate-900 m-0">专病队列动态</h3>
        <span class="inline-flex items-center justify-center px-1.5 py-0.5 text-[10px] font-bold bg-violet-100 text-violet-700 rounded-full leading-none">
          {{ items.length }}
        </span>
      </div>
      <button
        type="button"
        class="text-slate-400 hover:text-slate-600 border-0 bg-transparent cursor-pointer p-1"
        @click="collapsed = !collapsed"
      >
        <el-icon :size="14" class="transition-transform" :class="{ 'rotate-180': collapsed }">
          <ArrowUp />
        </el-icon>
      </button>
    </div>

    <div v-show="!collapsed" class="space-y-1.5">
      <div
        v-for="item in items"
        :key="item.title"
        class="py-2 px-1.5 flex items-center justify-between gap-3 hover:bg-slate-50 rounded-lg cursor-pointer transition-colors"
        @click="goCohort(item)"
      >
        <div class="flex items-center gap-2 min-w-0">
          <span
            class="text-[11px] px-1.5 py-0.5 rounded font-medium flex-shrink-0"
            :class="typeClass(item.type)"
          >
            {{ typeLabel(item.type) }}
          </span>
          <span class="text-xs text-slate-700 truncate font-medium hover:text-sky-600 transition-colors">
            {{ item.title }}
          </span>
        </div>
        <span class="text-[11px] text-slate-400 flex-shrink-0">{{ item.time }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowUp } from '@element-plus/icons-vue'
import type { CohortDigestItem } from '@/api/workspace'

const props = defineProps<{
  items: CohortDigestItem[]
}>()

const router = useRouter()
const collapsed = ref(false)

function typeLabel(type: string) {
  const map: Record<string, string> = {
    SYNC_DONE: '数据同步',
    KB_ITEM_PUBLISHED: '知识库',
    AI_SUGGEST_PENDING: 'AI 建议',
  }
  return map[type] ?? type
}

function typeClass(type: string) {
  const map: Record<string, string> = {
    SYNC_DONE: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
    KB_ITEM_PUBLISHED: 'bg-sky-50 text-sky-700 border border-sky-200',
    AI_SUGGEST_PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  }
  return map[type] ?? 'bg-slate-100 text-slate-700'
}

function goCohort(item: CohortDigestItem) {
  router.push(item.cohortId ? `/data/cdr/disease/${item.cohortId}` : '/data/cdr/disease')
}
</script>
