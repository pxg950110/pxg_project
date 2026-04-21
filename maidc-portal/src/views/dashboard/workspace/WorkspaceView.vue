<template>
  <PageContainer title="个人工作台" subtitle="MAIDC 医疗 AI 数据中心">
    <a-spin :spinning="store.loading">
      <WelcomeSection
        :user-name="store.dashboard?.welcome?.userName ?? userName"
        :date="store.dashboard?.welcome?.date ?? ''"
      />

      <MetricCards
        :metrics="store.dashboard?.metrics ?? null"
        :loading="store.loading"
      />

      <a-row :gutter="[16, 16]" style="margin-top: 16px">
        <a-col :span="14">
          <TodoSection
            :todos="store.dashboard?.todos ?? []"
            :loading="store.loading"
            @complete="handleComplete"
          />
        </a-col>
        <a-col :span="10">
          <NotifySection
            :notifications="store.dashboard?.notifications ?? []"
            :loading="store.loading"
            @mark-all-read="handleMarkAllRead"
            @click="handleNotifyClick"
          />
        </a-col>
      </a-row>

      <div style="margin-top: 16px">
        <QuickActions :actions="store.dashboard?.quickActions ?? []" />
      </div>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useWorkspaceStore } from '@/stores/workspace'
import { useAuthStore } from '@/stores/auth'
import PageContainer from '@/components/PageContainer/index.vue'
import WelcomeSection from './WelcomeSection.vue'
import MetricCards from './MetricCards.vue'
import TodoSection from './TodoSection.vue'
import NotifySection from './NotifySection.vue'
import QuickActions from './QuickActions.vue'
import type { NotificationItem } from '@/api/workspace'

const store = useWorkspaceStore()
const authStore = useAuthStore()

const userName = computed(() => authStore.userInfo?.realName ?? '')

onMounted(() => {
  store.fetchDashboard()
})

function handleComplete(id: number) {
  store.completeTask(id)
}

function handleMarkAllRead() {
  // Wire to message API in follow-up
}

function handleNotifyClick(_item: NotificationItem) {
  // Wire to notification navigation in follow-up
}
</script>
