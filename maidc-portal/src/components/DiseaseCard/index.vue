<template>
  <div
    class="group relative flex flex-col justify-between rounded-2xl border border-slate-200/80 bg-white p-5 shadow-clinical-sm transition-all duration-200 hover:-translate-y-1 hover:shadow-clinical hover:border-sky-300/80 cursor-pointer overflow-hidden"
    @click="emit('detail')"
  >
    <!-- 顶部状态光效装饰线 -->
    <div
      class="absolute top-0 left-0 right-0 h-1 transition-opacity"
      :class="hasRules ? (data.status === 'ACTIVE' ? 'bg-gradient-to-r from-sky-500 via-emerald-400 to-indigo-500' : 'bg-slate-200') : 'bg-amber-400'"
    />

    <!-- 卡片头部：名称、图标与状态 -->
    <div>
      <div class="flex items-start justify-between gap-3 mb-3">
        <div class="flex items-center gap-3 min-w-0">
          <div
            class="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-xl transition-colors"
            :class="hasRules ? (data.status === 'ACTIVE' ? 'bg-sky-50 text-sky-600 group-hover:bg-sky-500 group-hover:text-white' : 'bg-slate-100 text-slate-400') : 'bg-amber-50 text-amber-600'"
          >
            <el-icon :size="20"><FolderChecked /></el-icon>
          </div>
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <h4 class="text-base font-semibold text-slate-900 truncate leading-snug group-hover:text-sky-600 transition-colors m-0">
                {{ data.name }}
              </h4>
            </div>
            <p class="text-xs text-slate-400 truncate mt-0.5 m-0 font-mono">
              ID: #{{ data.id }} · {{ data.createdAt ? data.createdAt.substring(0, 10) : '近期建立' }}
            </p>
          </div>
        </div>

        <div class="flex flex-col items-end gap-1">
          <el-tag
            size="small"
            :type="data.status === 'ACTIVE' ? 'success' : 'info'"
            effect="light"
            class="!rounded-md font-medium"
          >
            <span class="flex items-center gap-1.5">
              <span
                class="w-1.5 h-1.5 rounded-full"
                :class="data.status === 'ACTIVE' ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'"
              />
              {{ data.status === 'ACTIVE' ? '已启用' : '未启用' }}
            </span>
          </el-tag>

          <!-- 规则配置阶段标签 -->
          <span
            v-if="!hasRules"
            class="inline-flex items-center text-[10px] font-semibold text-amber-600 bg-amber-50 border border-amber-200/80 px-1.5 py-0.5 rounded"
          >
            待配置过滤规则
          </span>
        </div>
      </div>

      <!-- 专病简介 -->
      <p class="text-xs text-slate-500 line-clamp-2 h-8 leading-relaxed mb-4">
        {{ data.description || '暂无描述信息，包含多维临床诊断、检验与用药规则筛查队列。' }}
      </p>

      <!-- 核心指标统计条 -->
      <div class="flex items-baseline justify-between p-3 rounded-xl bg-slate-50/80 border border-slate-100/80 mb-4">
        <div>
          <div class="text-[11px] font-medium text-slate-500">入组患者总规模</div>
          <div class="flex items-baseline gap-1 mt-0.5">
            <span class="font-mono text-2xl font-bold text-slate-900 tracking-tight">
              {{ formatNumber(data.patientCount) }}
            </span>
            <span class="text-xs text-slate-400">人</span>
          </div>
        </div>
        <div class="text-right">
          <div class="text-[11px] font-medium text-slate-500">过滤规则状态</div>
          <div
            class="text-xs font-mono font-semibold mt-1"
            :class="hasRules ? 'text-sky-600' : 'text-amber-600'"
          >
            {{ hasRules ? `${parsedRules.length} 组逻辑` : '尚未设定' }}
          </div>
        </div>
      </div>

      <!-- 纳入规则多模态标签徽章展示 -->
      <div class="space-y-1.5 mb-2">
        <div class="text-[11px] font-medium text-slate-400 flex items-center justify-between">
          <span>临床纳入过滤准则</span>
          <span v-if="parsedRules.length > 2" class="text-[10px] text-slate-400">共 {{ parsedRules.length }} 组</span>
        </div>
        <div class="flex flex-col gap-1.5">
          <div
            v-for="(group, gi) in parsedRules.slice(0, 2)"
            :key="gi"
            class="flex items-center gap-2 text-xs text-slate-600 bg-white p-1.5 rounded-lg border border-slate-100"
          >
            <el-tag size="small" :type="domainTagType(group.domain)" effect="plain" class="!rounded !text-[11px] font-medium px-1.5">
              {{ domainLabel(group.domain) }}
            </el-tag>
            <span class="truncate text-[11px] text-slate-600 flex-1 font-mono">
              {{ groupSummary(group) }}
            </span>
          </div>
          <div v-if="!hasRules" class="flex items-center justify-between p-2 rounded-lg bg-amber-50/60 border border-amber-100 text-xs text-amber-700">
            <span class="flex items-center gap-1.5">
              <el-icon class="text-amber-500"><InfoFilled /></el-icon>
              专病已建立，请配置纳入规则
            </span>
            <el-button
              type="primary"
              size="small"
              link
              class="!text-xs font-semibold !text-amber-700 hover:!text-amber-900"
              @click.stop="emit('configureRules')"
            >
              立即配置 &rarr;
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 卡片底部快捷操作栏 -->
    <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between gap-2" @click.stop>
      <div class="flex items-center gap-1.5">
        <el-button link type="primary" size="small" class="!text-xs font-medium" @click="emit('detail')">
          <el-icon class="mr-0.5"><DataLine /></el-icon>
          专病画像
        </el-button>
        <el-button
          link
          type="primary"
          size="small"
          class="!text-xs font-medium"
          :class="!hasRules ? '!text-amber-600 font-semibold' : ''"
          @click="emit('configureRules')"
        >
          <el-icon class="mr-0.5"><Operation /></el-icon>
          {{ hasRules ? '过滤配置' : '配置规则' }}
        </el-button>
        <el-button v-if="hasRules" link type="primary" size="small" class="!text-xs font-medium" @click="emit('sync')">
          <el-icon class="mr-0.5"><Refresh /></el-icon>
          同步
        </el-button>
      </div>

      <div class="flex items-center gap-1">
        <el-button link type="default" size="small" class="!text-xs text-slate-500 hover:text-sky-600" @click="emit('edit')">
          <el-icon class="mr-0.5"><Edit /></el-icon>
          编辑
        </el-button>
        <el-dropdown trigger="click" @command="handleCommand">
          <el-button link size="small" class="!text-xs text-slate-400 hover:text-slate-600 !p-1">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="knowledge">
                <el-icon><Reading /></el-icon>关联知识库
              </el-dropdown-item>
              <el-dropdown-item command="followup">
                <el-icon><Calendar /></el-icon>随访方案
              </el-dropdown-item>
              <el-dropdown-item command="delete" divided class="!text-rose-600">
                <el-icon><Delete /></el-icon>删除专病库
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  FolderChecked,
  DataLine,
  Refresh,
  Edit,
  MoreFilled,
  Reading,
  Calendar,
  Delete,
  Operation,
  InfoFilled,
} from '@element-plus/icons-vue'

const props = defineProps<{ data: any }>()
const emit = defineEmits<{
  (e: 'edit'): void
  (e: 'detail'): void
  (e: 'sync'): void
  (e: 'delete'): void
  (e: 'knowledge'): void
  (e: 'followup'): void
  (e: 'configureRules'): void
}>()

const domainLabels: Record<string, string> = {
  DIAGNOSIS: '临床诊断',
  LAB: '检验项目',
  MEDICATION: '用药医嘱',
  IMAGING: '影像检查',
  SURGERY: '手术操作',
  PATHOLOGY: '病理组织',
}

function domainLabel(d: string) {
  return domainLabels[d] || d || '常规'
}

function domainTagType(d: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (d) {
    case 'DIAGNOSIS': return ''
    case 'LAB': return 'success'
    case 'MEDICATION': return 'warning'
    case 'IMAGING': return 'info'
    case 'SURGERY': return 'danger'
    case 'PATHOLOGY': return 'warning'
    default: return 'info'
  }
}

function formatNumber(num: number | undefined): string {
  if (!num) return '0'
  return num.toLocaleString()
}

const parsedRules = computed(() => {
  try {
    const rules = typeof props.data.inclusionRules === 'string'
      ? JSON.parse(props.data.inclusionRules)
      : props.data.inclusionRules
    return rules?.groups || []
  } catch {
    return []
  }
})

const hasRules = computed(() => {
  return parsedRules.value.length > 0 && parsedRules.value.some((g: any) => g.conditions?.length > 0)
})

function groupSummary(group: any) {
  return (group.conditions || [])
    .map((c: any) => `${c.field} ${c.operator} ${Array.isArray(c.value) ? c.value.join(',') : c.value}`)
    .join(` ${group.logic || 'AND'} `)
}

function handleCommand(cmd: string) {
  if (cmd === 'knowledge') emit('knowledge')
  else if (cmd === 'followup') emit('followup')
  else if (cmd === 'delete') emit('delete')
}
</script>
