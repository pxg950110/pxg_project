<template>
  <div class="header-actions">
    <!-- Notification bell with popover -->
    <a-popover
      v-model:open="msgPopoverVisible"
      trigger="click"
      placement="bottomRight"
      overlay-class-name="msg-popover"
    >
      <template #content>
        <div class="msg-panel">
          <div class="msg-panel__header">
            <span class="msg-panel__title">消息通知</span>
            <a class="msg-panel__action" @click="handleMarkAllRead">全部已读</a>
          </div>

          <div class="msg-panel__tabs">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :class="['msg-tab', { 'msg-tab--active': activeTab === tab.key }]"
              @click="activeTab = tab.key"
            >{{ tab.label }}</button>
          </div>

          <div class="msg-panel__body">
            <div v-if="loading" class="msg-panel__loading">
              <a-spin size="small" />
            </div>
            <template v-else>
              <div
                v-for="msg in filteredMessages"
                :key="msg.id"
                :class="['msg-item', { 'msg-item--unread': !msg.isRead }]"
                @click="handleMsgClick(msg)"
              >
                <span class="msg-item__dot" :style="{ backgroundColor: msg.typeColor }" />
                <div class="msg-item__content">
                  <div class="msg-item__top">
                    <span class="msg-item__title">{{ msg.title }}</span>
                    <span class="msg-item__time">{{ msg.time }}</span>
                  </div>
                  <div class="msg-item__subtitle">
                    <span :style="{ color: msg.typeColor }">{{ msg.typeLabel }}</span>
                  </div>
                </div>
              </div>
              <div v-if="filteredMessages.length === 0" class="msg-panel__empty">
                暂无消息
              </div>
            </template>
          </div>

          <div class="msg-panel__footer">
            <a @click="goMessagePage">查看全部消息</a>
          </div>
        </div>
      </template>

      <a-badge :count="unreadCount" :offset="[-2, 2]">
        <BellOutlined class="action-icon" />
      </a-badge>
    </a-popover>

    <!-- Settings gear -->
    <SettingOutlined class="action-icon" @click="settingsVisible = true" />

    <!-- User dropdown -->
    <a-dropdown>
      <span class="user-info">
        <a-avatar :size="28" :style="{ backgroundColor: '#' + uiStore.primaryColor }">
          {{ avatarText }}
        </a-avatar>
        <span class="username">{{ authStore.userInfo?.realName || authStore.userInfo?.username }}</span>
      </span>
      <template #overlay>
        <a-menu>
          <a-menu-item @click="router.push('/system/users')">
            <UserOutlined /> 个人中心
          </a-menu-item>
          <a-menu-divider />
          <a-menu-item @click="handleLogout">
            <LogoutOutlined /> 退出登录
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

    <SettingsDrawer v-model:visible="settingsVisible" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  BellOutlined, UserOutlined, LogoutOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { Modal, message as antMessage } from 'ant-design-vue'
import { getMessages, markAsRead, markAllAsRead } from '@/api/msg'
import SettingsDrawer from './SettingsDrawer.vue'

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
const authStore = useAuthStore()
const uiStore = useUiStore()

const settingsVisible = ref(false)
const msgPopoverVisible = ref(false)
const messages = ref<MessageItem[]>([])
const loading = ref(false)
const activeTab = ref('all')

const unreadCount = computed(() => messages.value.filter(m => !m.isRead).length)

const tabs = computed(() => [
  { key: 'all', label: '全部' },
  { key: 'unread', label: `未读 (${unreadCount.value})` },
])

const filteredMessages = computed(() => {
  const list = messages.value.map(m => ({
    ...m,
    typeLabel: m.typeLabel || TYPE_MAP[m.type]?.label || m.type,
    typeColor: m.typeColor || TYPE_MAP[m.type]?.color || '#1677ff',
  }))
  if (activeTab.value === 'unread') return list.filter(m => !m.isRead)
  return list
})

const avatarText = computed(() => {
  const name = authStore.userInfo?.realName || authStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

async function fetchMessages() {
  loading.value = true
  try {
    const res = await getMessages({ page: 1, page_size: 20 })
    messages.value = res.data.data?.items || []
  } catch {
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function handleMsgClick(msg: MessageItem) {
  if (!msg.isRead) {
    try {
      await markAsRead(msg.id)
      const target = messages.value.find(m => m.id === msg.id)
      if (target) target.isRead = true
    } catch { /* ignore */ }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead()
    messages.value.forEach(m => { m.isRead = true })
    antMessage.success('已全部标记为已读')
  } catch { /* ignore */ }
}

function goMessagePage() {
  msgPopoverVisible.value = false
  router.push('/message/list')
}

// Fetch messages when popover opens
watch(msgPopoverVisible, (visible) => {
  if (visible) fetchMessages()
})

// Initial fetch for unread count
fetchMessages()

function handleLogout() {
  Modal.confirm({
    title: '确认退出',
    content: '确定要退出登录吗？',
    async onOk() {
      await authStore.logoutAction()
      router.push('/login')
    },
  })
}
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-icon {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  color: rgba(0, 0, 0, 0.65);

  &:hover {
    color: var(--ant-color-primary);
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}
</style>

<style lang="scss">
// Unscoped: popover is rendered in body, outside component scope
.msg-popover {
  .ant-popover-inner {
    padding: 0;
    border-radius: 8px;
    overflow: hidden;
  }
  .ant-popover-content {
    width: 380px;
  }
}
</style>

<style lang="scss" scoped>
.msg-panel {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: rgba(0, 0, 0, 0.88);
  }

  &__action {
    font-size: 13px;
    color: #1677ff;
    cursor: pointer;
    &:hover { color: #4096ff; }
  }

  &__tabs {
    display: flex;
    gap: 6px;
    padding: 8px 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  &__body {
    max-height: 360px;
    overflow-y: auto;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 32px 0;
  }

  &__empty {
    text-align: center;
    padding: 40px 0;
    color: rgba(0, 0, 0, 0.25);
    font-size: 14px;
  }

  &__footer {
    text-align: center;
    padding: 10px 16px;
    border-top: 1px solid #f0f0f0;

    a {
      font-size: 13px;
      color: #1677ff;
      cursor: pointer;
      &:hover { color: #4096ff; }
    }
  }
}

.msg-tab {
  padding: 2px 12px;
  font-size: 13px;
  line-height: 22px;
  border-radius: 12px;
  cursor: pointer;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: rgba(0, 0, 0, 0.65);
  transition: all 0.2s;

  &:hover {
    color: #1677ff;
    border-color: #1677ff;
  }

  &--active {
    background: #1677ff;
    color: #fff;
    border-color: #1677ff;
  }
}

.msg-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover { background: #fafafa; }

  &--unread { background: #f6f8ff; }
  &--unread:hover { background: #eef2ff; }

  &__dot {
    width: 8px;
    height: 8px;
    min-width: 8px;
    border-radius: 50%;
    margin-top: 6px;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__top {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__title {
    flex: 1;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.88);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .msg-item--unread &__title {
    font-weight: 600;
  }

  &__time {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
    white-space: nowrap;
    margin-left: auto;
  }

  &__subtitle {
    font-size: 12px;
    margin-top: 4px;
  }
}
</style>
