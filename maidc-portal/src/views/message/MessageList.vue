<template>
  <PageContainer title="我的消息">
    <template #extra>
      <a-button @click="handleMarkAllRead" :disabled="!hasUnread">全部已读</a-button>
    </template>

    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane key="unread" tab="未读">
        <template #tab>未读 <a-badge :count="unreadCount" /></template>
      </a-tab-pane>
      <a-tab-pane key="SYSTEM" tab="系统通知" />
      <a-tab-pane key="ALERT" tab="告警通知" />
      <a-tab-pane key="APPROVAL" tab="审批通知" />
    </a-tabs>

    <a-list :data-source="tableData" :loading="loading" :pagination="pagination" @change="handleTableChange">
      <template #renderItem="{ item }">
        <a-list-item :class="{ 'unread-item': !item.is_read }" @click="handleRead(item)">
          <a-list-item-meta>
            <template #title>
              <a-space>
                <a-tag :color="typeColors[item.type]">{{ typeLabels[item.type] }}</a-tag>
                <span>{{ item.title }}</span>
              </a-space>
            </template>
            <template #description>{{ item.content }}</template>
          </a-list-item-meta>
          <template #actions>
            <span class="time-text">{{ formatDateTime(item.created_at) }}</span>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { useTable } from '@/hooks/useTable'
import { getMessages, markAsRead, markAllAsRead, getUnreadCount } from '@/api/msg'
import { formatDateTime } from '@/utils/date'

const activeTab = ref('all')
const unreadCount = ref(0)
const hasUnread = computed(() => unreadCount.value > 0)

const typeColors: Record<string, string> = { SYSTEM: 'blue', ALERT: 'red', APPROVAL: 'green' }
const typeLabels: Record<string, string> = { SYSTEM: '系统', ALERT: '告警', APPROVAL: '审批' }

const { tableData, loading, pagination, fetchData, handleTableChange } = useTable<any>(
  (params) => getMessages({ page: params.page, page_size: params.pageSize, is_read: activeTab.value === 'unread' ? false : undefined, type: ['SYSTEM', 'ALERT', 'APPROVAL'].includes(activeTab.value) ? activeTab.value : undefined })
)

function handleTabChange() { fetchData() }

async function handleRead(item: any) {
  if (!item.is_read) {
    await markAsRead(item.id)
    item.is_read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
}

async function handleMarkAllRead() {
  await markAllAsRead()
  antMessage.success('已全部标记为已读')
  unreadCount.value = 0
  fetchData()
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data.data.count
  } catch {}
}

onMounted(() => { fetchData(); loadUnreadCount() })
</script>

<style scoped>
.unread-item { background: #f6f8fa; }
.unread-item:hover { background: #e6f4ff; }
.time-text { color: rgba(0,0,0,0.45); font-size: 12px; }
</style>
