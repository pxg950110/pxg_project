<template>
  <a-layout class="basic-layout">
    <a-layout-sider
      v-model:collapsed="uiStore.sidebarCollapsed"
      :trigger="null"
      collapsible
      theme="light"
      :width="220"
      :collapsed-width="64"
      class="layout-sider"
    >
      <div class="sider-logo">
        <div class="logo-icon-box">
          <img src="@/assets/logo.svg" alt="MAIDC" class="logo-img" />
        </div>
        <span v-show="!uiStore.sidebarCollapsed" class="logo-text">MAIDC</span>
      </div>
      <SidebarMenu :collapsed="uiStore.sidebarCollapsed" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="layout-header">
        <div class="header-left">
          <MenuFoldOutlined
            v-if="uiStore.sidebarCollapsed"
            class="trigger-icon"
            @click="uiStore.toggleSidebar()"
          />
          <MenuUnfoldOutlined
            v-else
            class="trigger-icon"
            @click="uiStore.toggleSidebar()"
          />
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <HeaderActions />
        </div>
      </a-layout-header>
      <a-layout-content class="layout-content">
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="uiStore.cachedViews">
            <component :is="Component" :key="route.path" />
          </keep-alive>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import { useUiStore } from '@/stores/ui'
import SidebarMenu from './SidebarMenu.vue'
import HeaderActions from './HeaderActions.vue'

const uiStore = useUiStore()
const route = useRoute()

const pageTitle = computed(() => {
  // Use the deepest matched route's meta.title
  const matched = route.matched.filter(r => r.meta?.title)
  if (matched.length > 0) {
    return matched[matched.length - 1].meta.title as string
  }
  return ''
})
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}
.layout-sider {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
  border-right: 1px solid #d9d9d9;
}
.sider-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid #d9d9d9;
}
.logo-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.logo-icon-box .logo-img {
  width: 20px;
  height: 20px;
  filter: brightness(0) invert(1);
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
}
.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  line-height: 60px;
  border-bottom: 1px solid #d9d9d9;
  z-index: 9;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.trigger-icon {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}
.trigger-icon:hover {
  color: #1677ff;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.layout-content {
  padding: 24px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
  overflow-y: auto;
}
</style>
