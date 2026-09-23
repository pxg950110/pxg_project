<template>
  <div class="workspace-container space-y-4 max-w-[1600px] mx-auto">
    <!-- 顶部欢迎横幅 -->
    <WelcomeSection
      :user-name="store.dashboard?.welcome?.userName ?? userName"
      :date="store.dashboard?.welcome?.date ?? ''"
      :role="store.dashboard?.welcome?.role ?? ''"
      :org-name="store.dashboard?.welcome?.orgName ?? null"
      :dept-name="store.dashboard?.welcome?.deptName ?? null"
    />

    <!-- 4列指标卡片 -->
    <MetricCards
      :cards="store.dashboard?.cards ?? null"
      :metrics="store.dashboard?.metrics ?? null"
      :loading="store.loading"
    />

    <!-- 核心工作区分栏：左侧待办（8/12），右侧通知与专病动态（4/12） -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
      <div class="lg:col-span-8 space-y-4">
        <TodoSection
          :todos="store.dashboard?.todos ?? []"
          :stats="store.dashboard?.todoStats ?? null"
          :loading="store.loading"
          @complete="handleComplete"
        />
      </div>

      <div class="lg:col-span-4 space-y-4">
        <NotifySection
          :notifications="store.dashboard?.notifications ?? []"
          :loading="store.loading"
          @mark-all-read="handleMarkAllRead"
          @click="handleNotifyClick"
        />

        <CohortDigest
          v-if="store.dashboard?.cohortDigest?.length"
          :items="store.dashboard.cohortDigest"
        />
      </div>
    </div>

    <!-- 底部快捷业务入口 -->
    <div v-if="store.dashboard?.quickActions?.length" class="pt-2">
      <QuickActions :actions="store.dashboard.quickActions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkspaceStore } from '@/stores/workspace'
import { useAuthStore } from '@/stores/auth'
import WelcomeSection from './WelcomeSection.vue'
import MetricCards from './MetricCards.vue'
import TodoSection from './TodoSection.vue'
import NotifySection from './NotifySection.vue'
import QuickActions from './QuickActions.vue'
import CohortDigest from './CohortDigest.vue'
import type { NotificationItem } from '@/api/workspace'

const store = useWorkspaceStore()
const authStore = useAuthStore()
const router = useRouter()

const userName = computed(() => authStore.userInfo?.realName ?? '')

onMounted(() => {
  store.fetchDashboard()
})

function handleComplete(id: number) {
  store.completeTask(id)
}

async function handleMarkAllRead() {
  try {
    await store.markAllNotificationsRead()
  } catch {
    // ignore
  }
}

function handleNotifyClick(item: NotificationItem) {
  if (!item.isRead) {
    store.markNotificationRead(item.id)
  }
  if (item.bizType && item.bizId) {
    const routeMap: Record<string, string> = {
      APPROVAL: `/model/approvals/${item.bizId}`,
      MODEL: `/model/list`,
      ALERT: `/alert/active`,
    }
    const route =
      item.bizType === 'COHORT'
        ? `/data/cdr/disease/${item.bizId}`
        : routeMap[item.bizType]
    if (route) router.push(route)
  }
}
</script>
