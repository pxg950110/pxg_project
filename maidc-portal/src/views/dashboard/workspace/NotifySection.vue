<template>
  <a-card title="消息通知" :bordered="false" :loading="loading">
    <template #extra>
      <a-button type="link" size="small" @click="handleMarkAllRead">全部已读</a-button>
    </template>

    <div v-if="notifications.length === 0" class="empty-state">
      <a-empty description="暂无消息" />
    </div>

    <a-list v-else :data-source="notifications" size="small">
      <template #renderItem="{ item }">
        <a-list-item :class="{ 'unread-item': !item.isRead }">
          <a-list-item-meta>
            <template #title>
              <a class="notify-title" @click="handleClick(item)">{{ item.title }}</a>
            </template>
            <template #description>
              <span>{{ item.createdAt }}</span>
            </template>
            <template #avatar>
              <a-avatar :size="32" :style="{ backgroundColor: typeColor(item.type) }">
                <template #icon>
                  <NotificationOutlined />
                </template>
              </a-avatar>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>

<script setup lang="ts">
import { NotificationOutlined } from '@ant-design/icons-vue'
import type { NotificationItem } from '@/api/workspace'

const props = defineProps<{
  notifications: NotificationItem[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'markAllRead'): void
  (e: 'click', item: NotificationItem): void
}>()

function typeColor(type: string) {
  const map: Record<string, string> = { SYSTEM: '#1890ff', ALERT: '#ff4d4f', APPROVAL: '#52c41a' }
  return map[type] ?? '#999'
}

function handleMarkAllRead() {
  emit('markAllRead')
}

function handleClick(item: NotificationItem) {
  emit('click', item)
}
</script>

<style scoped lang="scss">
.empty-state { padding: 32px 0; }
.unread-item { border-left: 3px solid #1890ff; padding-left: 8px; }
.notify-title { cursor: pointer; &:hover { color: #1890ff; } }
</style>
