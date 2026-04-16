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
      <div class="sider-menu">
        <SidebarMenu :collapsed="uiStore.sidebarCollapsed" />
      </div>
    </a-layout-sider>
    <a-layout class="layout-main">
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
      <TabBar />
      <a-layout-content class="layout-content">
        <router-view v-slot="{ Component, route }">
          <component :is="Component" :key="route.fullPath" />
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons-vue'
import { useUiStore } from '@/stores/ui'
import SidebarMenu from './SidebarMenu.vue'
import HeaderActions from './HeaderActions.vue'
import TabBar from './TabBar.vue'

const uiStore = useUiStore()
const route = useRoute()

const pageTitle = computed(() => {
  const matched = route.matched.filter(r => r.meta?.title)
  if (matched.length > 0) {
    return matched[matched.length - 1].meta.title as string
  }
  return ''
})

// Auto-add tab on route change
watch(
  () => route.name,
  () => {
    if (route.name && route.meta?.title) {
      uiStore.addTab({
        name: String(route.name),
        title: String(route.meta.title),
        fullPath: route.fullPath,
      })
    }
  },
  { immediate: true },
)
</script>

<style lang="scss" scoped>
.basic-layout {
  height: 100vh;
  overflow: hidden;
}

.layout-sider {
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
  z-index: 10;
  border-right: 1px solid #d9d9d9;
  display: flex;
  flex-direction: column;

  // Ant Design sider override: allow height to fill
  :deep(.ant-layout-sider-children) {
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
  }
}

.sider-logo {
  height: 60px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid #d9d9d9;
}

.sider-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.logo-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--ant-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .logo-img {
    width: 20px;
    height: 20px;
    filter: brightness(0) invert(1);
  }
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.88);
  white-space: nowrap;
}

.layout-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  flex-shrink: 0;
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

  &:hover {
    color: var(--ant-color-primary);
  }
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
  flex: 1;
  overflow-y: auto;
}
</style>
