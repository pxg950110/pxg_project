<template>
  <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex flex-col h-full">
    <div class="flex items-center justify-between pb-3 mb-2 border-b border-slate-100">
      <div class="flex items-center gap-2">
        <span class="w-1 h-3.5 bg-sky-500 rounded-full" />
        <h3 class="text-sm font-semibold text-slate-900 m-0">消息通知</h3>
        <span
          v-if="unreadCount > 0"
          class="inline-flex items-center justify-center px-1.5 py-0.5 text-[10px] font-bold bg-rose-500 text-white rounded-full leading-none"
        >
          {{ unreadCount }}
        </span>
      </div>
      <el-button link type="primary" class="!text-xs" @click="handleMarkAllRead">全部已读</el-button>
    </div>

    <div v-loading="loading" class="flex-1 overflow-y-auto min-h-[220px]">
      <div v-if="notifications.length === 0" class="py-10 flex flex-col items-center justify-center text-slate-400">
        <el-icon :size="36" class="text-slate-300 mb-2"><Bell /></el-icon>
        <p class="text-xs font-medium">暂无未读系统通知</p>
      </div>

      <div v-else class="space-y-1.5">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="p-2.5 rounded-lg transition-colors cursor-pointer border flex items-start gap-3"
          :class="[
            !item.isRead
              ? 'bg-sky-50/50 border-sky-200/70 hover:bg-sky-50'
              : 'bg-white border-transparent hover:bg-slate-50'
          ]"
          @click="handleClick(item)"
        >
          <!-- 图标徽标 -->
          <div
            class="w-7 h-7 rounded-md flex items-center justify-center flex-shrink-0 mt-0.5"
            :class="typeBgClass(item.type)"
          >
            <el-icon :size="14">
              <Bell />
            </el-icon>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-2">
              <span
                class="text-xs font-semibold truncate"
                :class="!item.isRead ? 'text-slate-900' : 'text-slate-600'"
              >
                {{ item.title }}
              </span>
              <span class="text-[11px] text-slate-400 flex-shrink-0">{{ item.createdAt }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import type { NotificationItem } from '@/api/workspace'

const props = defineProps<{
  notifications: NotificationItem[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'markAllRead'): void
  (e: 'click', item: NotificationItem): void
}>()

const unreadCount = computed(() => props.notifications.filter((n) => !n.isRead).length)

function typeBgClass(type: string) {
  const map: Record<string, string> = {
    SYSTEM: 'bg-sky-100 text-sky-600',
    ALERT: 'bg-rose-100 text-rose-600',
    APPROVAL: 'bg-emerald-100 text-emerald-600',
  }
  return map[type] ?? 'bg-slate-100 text-slate-600'
}

function handleMarkAllRead() {
  emit('markAllRead')
}

function handleClick(item: NotificationItem) {
  emit('click', item)
}
</script>
