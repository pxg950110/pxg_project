<template>
  <a-layout class="basic-layout">
    <a-layout-sider
      v-model:collapsed="uiStore.sidebarCollapsed"
      :trigger="null"
      collapsible
      :width="220"
      :collapsed-width="64"
      class="layout-sider"
    >
      <div class="sider-logo">
        <img src="@/assets/logo.svg" alt="MAIDC" class="logo-img" />
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
          <BreadcrumbNav />
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
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import { useUiStore } from '@/stores/ui'
import SidebarMenu from './SidebarMenu.vue'
import BreadcrumbNav from './BreadcrumbNav.vue'
import HeaderActions from './HeaderActions.vue'

const uiStore = useUiStore()
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}
.layout-sider {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
}
.sider-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}
.logo-img {
  width: 28px;
  height: 28px;
}
.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #1677ff;
  white-space: nowrap;
}
.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  line-height: 56px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
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
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.layout-content {
  margin: 16px;
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 56px - 32px);
  overflow-y: auto;
}
</style>
