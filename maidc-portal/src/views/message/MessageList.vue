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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message as antMessage } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'

interface MockMessage {
  id: number
  type: string
  typeLabel: string
  typeColor: string
  title: string
  content: string
  time: string
  isRead: boolean
}

const router = useRouter()
const activeTab = ref('all')

const tabs = computed(() => [
  { key: 'all', label: '全部' },
  { key: 'unread', label: `未读 (${unreadCount.value})` },
  { key: 'read', label: '已读' },
])

const messages = ref<MockMessage[]>([
  { id: 1, type: 'SYSTEM', typeLabel: '系统', typeColor: '#1677ff', title: '模型部署完成通知', content: '肺结节检测v3.3已成功部署至生产环境', time: '10分钟前', isRead: false },
  { id: 2, type: 'ALERT', typeLabel: '告警', typeColor: '#ff4d4f', title: 'GPU内存告警', content: 'GPU-Node-03内存使用率已超过90%阈值', time: '30分钟前', isRead: false },
  { id: 3, type: 'APPROVAL', typeLabel: '审批', typeColor: '#52c41a', title: '审批待处理', content: '张医生提交了新模型注册审批(APR-2026-0018)', time: '1小时前', isRead: false },
  { id: 4, type: 'SYSTEM', typeLabel: '系统', typeColor: '#1677ff', title: '数据同步完成', content: '患者数据增量同步完成，新增128条记录', time: '2小时前', isRead: false },
  { id: 5, type: 'ALERT', typeLabel: '告警', typeColor: '#ff4d4f', title: '数据质量异常', content: '数据集"CT肺结节数据集v2"质量评分低于阈值', time: '3小时前', isRead: false },
  { id: 6, type: 'SYSTEM', typeLabel: '系统', typeColor: '#1677ff', title: '系统维护通知', content: '系统将于本周六凌晨2:00-4:00进行维护升级', time: '昨天', isRead: true },
])

const unreadCount = computed(() => messages.value.filter((m) => !m.isRead).length)

const filteredMessages = computed(() => {
  if (activeTab.value === 'unread') return messages.value.filter((m) => !m.isRead)
  if (activeTab.value === 'read') return messages.value.filter((m) => m.isRead)
  return messages.value
})

function isFirstUnread(id: number): boolean {
  const firstUnread = messages.value.find((m) => !m.isRead)
  return firstUnread ? firstUnread.id === id : false
}

function handleViewDetail(msg: MockMessage) {
  router.push(`/message/detail/${msg.id}`)
}

function handleMarkRead(msg: MockMessage) {
  msg.isRead = true
  antMessage.success('已标记为已读')
}

function handleMarkAllRead() {
  messages.value.forEach((m) => {
    m.isRead = true
  })
  antMessage.success('已全部标记为已读')
}
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
