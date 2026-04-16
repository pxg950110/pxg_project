<template>
  <PageContainer title="消息中心" subtitle="系统通知与操作提醒">
    <template #extra>
      <button class="mark-all-btn" @click="handleMarkAllRead">
        全部已读
      </button>
    </template>

    <!-- Custom Tab Pills -->
    <div class="tab-pills">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-pill', { 'tab-pill--active': activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Message Cards -->
    <div class="message-list">
      <div
        v-for="msg in filteredMessages"
        :key="msg.id"
        :class="[
          'message-card',
          {
            'message-card--first-unread': msg.isRead === false && isFirstUnread(msg.id),
            'message-card--unread': msg.isRead === false && !isFirstUnread(msg.id),
            'message-card--read': msg.isRead === true,
          },
        ]"
      >
        <div class="message-card__top">
          <span class="message-card__dot" :style="{ backgroundColor: msg.typeColor }" />
          <span class="message-card__type" :style="{ color: msg.typeColor }">{{ msg.typeLabel }}</span>
          <span :class="['message-card__title', { 'message-card__title--bold': !msg.isRead }]">
            {{ msg.title }}
          </span>
          <span class="message-card__time">{{ msg.time }}</span>
        </div>
        <div class="message-card__bottom">
          <a class="message-card__link" @click.prevent="handleViewDetail(msg)">查看详情</a>
          <a
            v-if="!msg.isRead"
            class="message-card__link"
            @click.prevent="handleMarkRead(msg)"
          >
            标记已读
          </a>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="filteredMessages.length === 0" class="empty-state">
      <span>暂无消息</span>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message as antMessage } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getMessages, markAsRead, markAllAsRead } from '@/api/msg'
import { useTable } from '@/hooks/useTable'

interface MessageItem {
  id: number
  type: string
  typeLabel: string
  typeColor: string
  title: string
  content: string
  time: string
  isRead: boolean
}

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  SYSTEM: { label: '系统', color: '#1677ff' },
  ALERT: { label: '告警', color: '#ff4d4f' },
  APPROVAL: { label: '审批', color: '#52c41a' },
}

const router = useRouter()
const activeTab = ref('all')

const { tableData: messages, loading, pagination, fetchData } = useTable<MessageItem>(
  (params) => getMessages({ page: params.page, page_size: params.pageSize }),
)

const tabs = computed(() => [
  { key: 'all', label: '全部' },
  { key: 'unread', label: `未读 (${unreadCount.value})` },
  { key: 'read', label: '已读' },
])

const unreadCount = computed(() => messages.value.filter((m: MessageItem) => !m.isRead).length)

const filteredMessages = computed(() => {
  const list = messages.value.map((m: MessageItem) => ({
    ...m,
    typeLabel: m.typeLabel || TYPE_MAP[m.type]?.label || m.type,
    typeColor: m.typeColor || TYPE_MAP[m.type]?.color || '#1677ff',
  }))
  if (activeTab.value === 'unread') return list.filter((m: MessageItem) => !m.isRead)
  if (activeTab.value === 'read') return list.filter((m: MessageItem) => m.isRead)
  return list
})

function isFirstUnread(id: number): boolean {
  const firstUnread = messages.value.find((m: MessageItem) => !m.isRead)
  return firstUnread ? firstUnread.id === id : false
}

function handleViewDetail(msg: MessageItem) {
  router.push(`/message/detail/${msg.id}`)
}

async function handleMarkRead(msg: MessageItem) {
  await markAsRead(msg.id)
  antMessage.success('已标记为已读')
  fetchData()
}

async function handleMarkAllRead() {
  await markAllAsRead()
  antMessage.success('已全部标记为已读')
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>
/* Mark All Read Button */
.mark-all-btn {
  padding: 5px 16px;
  font-size: 14px;
  color: #1677ff;
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 22px;
}

.mark-all-btn:hover {
  background: #bae0ff;
  border-color: #1677ff;
}

/* Custom Tab Pills */
.tab-pills {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.tab-pill {
  padding: 4px 16px;
  font-size: 14px;
  line-height: 22px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
}

.tab-pill:hover {
  color: #1677ff;
  border-color: #1677ff;
}

.tab-pill--active {
  background: #1677ff;
  color: #fff;
  border-color: #1677ff;
}

.tab-pill--active:hover {
  background: #4096ff;
  border-color: #4096ff;
  color: #fff;
}

/* Message List */
.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Message Card */
.message-card {
  padding: 16px 20px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  transition: all 0.2s;
}

.message-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-card--first-unread {
  background: #e6f4ff;
  border: 1px solid #91caff;
  border-left: 3px solid #1677ff;
}

.message-card--unread {
  background: #fff;
  border: 1px solid #d9d9d9;
}

.message-card--read {
  background: #fff;
  border: 1px solid #f0f0f0;
}

/* Card Top Row */
.message-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.message-card__dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  min-width: 8px;
  border-radius: 50%;
}

.message-card__type {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.message-card__title {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-card__title--bold {
  font-weight: 600;
}

.message-card__time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  white-space: nowrap;
  margin-left: auto;
}

/* Card Bottom Row */
.message-card__bottom {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-left: 16px;
}

.message-card__link {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s;
}

.message-card__link:hover {
  color: #4096ff;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 48px 0;
  color: rgba(0, 0, 0, 0.25);
  font-size: 14px;
}
</style>
