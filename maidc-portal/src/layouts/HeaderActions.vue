<template>
  <div class="header-actions">
    <a-badge :count="unreadCount" :offset="[-2, 2]">
      <BellOutlined class="action-icon" @click="router.push('/message/list')" />
    </a-badge>
    <a-dropdown>
      <span class="user-info">
        <a-avatar :size="28" style="background-color: #1677ff">
          {{ avatarText }}
        </a-avatar>
        <span class="username">{{ authStore.userInfo?.real_name || authStore.userInfo?.username }}</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { BellOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { Modal } from 'ant-design-vue'

const router = useRouter()
const authStore = useAuthStore()

const unreadCount = ref(0)
const avatarText = computed(() => {
  const name = authStore.userInfo?.real_name || authStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

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

<style scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
.action-icon {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}
.action-icon:hover {
  color: #1677ff;
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
