<template>
  <div v-if="uiStore.tabBarEnabled" class="tags-view-container h-[38px] bg-white border-b border-slate-200/80 px-4 flex items-center shadow-xs overflow-hidden select-none relative">
    <el-scrollbar class="w-full flex items-center" wrap-class="flex items-center">
      <div class="flex items-center gap-1.5 py-1">
        <div
          v-for="tab in uiStore.openTabs"
          :key="tab.fullPath"
          :class="[
            'capsule-tab group inline-flex items-center gap-1.5 px-3 py-1 text-xs rounded-full border transition-all duration-200 cursor-pointer whitespace-nowrap',
            isActive(tab)
              ? 'bg-sky-50 text-sky-700 border-sky-300 font-semibold shadow-xs ring-1 ring-sky-200/50'
              : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100 hover:text-slate-900 hover:border-slate-300'
          ]"
          @click="router.push(tab.fullPath).catch(() => {})"
          @contextmenu.prevent="openContextMenu($event, tab)"
        >
          <!-- Active dot indicator -->
          <span
            v-if="isActive(tab)"
            class="w-1.5 h-1.5 rounded-full bg-sky-500 flex-shrink-0 animate-pulse"
          ></span>

          <span class="truncate max-w-[140px]">{{ tab.title }}</span>

          <!-- Close icon (hidden on home/dashboard workspace unless there are multiple) -->
          <el-icon
            v-if="!isAffix(tab)"
            class="text-[11px] p-0.5 rounded-full text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition-colors flex-shrink-0"
            @click.stop="handleCloseTab(tab)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </el-scrollbar>

    <!-- Right-click Context Menu -->
    <transition name="el-zoom-in-top">
      <div
        v-if="contextMenu.visible"
        class="context-menu fixed z-50 bg-white rounded-lg shadow-xl border border-slate-200/90 py-1 text-xs text-slate-700 min-w-[120px]"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      >
        <button
          type="button"
          class="w-full px-3 py-1.5 text-left flex items-center gap-2 hover:bg-sky-50 hover:text-sky-600 transition-colors cursor-pointer"
          @click="refreshSelectedTab"
        >
          <el-icon><Refresh /></el-icon>
          <span>刷新当前</span>
        </button>
        <button
          v-if="contextMenu.tab && !isAffix(contextMenu.tab)"
          type="button"
          class="w-full px-3 py-1.5 text-left flex items-center gap-2 hover:bg-sky-50 hover:text-sky-600 transition-colors cursor-pointer"
          @click="closeCurrentTab"
        >
          <el-icon><Close /></el-icon>
          <span>关闭当前</span>
        </button>
        <button
          type="button"
          class="w-full px-3 py-1.5 text-left flex items-center gap-2 hover:bg-sky-50 hover:text-sky-600 transition-colors cursor-pointer"
          @click="closeOtherTabs"
        >
          <el-icon><CircleClose /></el-icon>
          <span>关闭其他</span>
        </button>
        <button
          type="button"
          class="w-full px-3 py-1.5 text-left flex items-center gap-2 hover:bg-sky-50 hover:text-sky-600 transition-colors cursor-pointer border-t border-slate-100"
          @click="closeAllTabs"
        >
          <el-icon><Remove /></el-icon>
          <span>关闭全部</span>
        </button>
      </div>
    </transition>

    <!-- Overlay backdrop to dismiss context menu -->
    <div
      v-if="contextMenu.visible"
      class="fixed inset-0 z-40"
      @click="contextMenu.visible = false"
      @contextmenu.prevent="contextMenu.visible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUiStore, type TabItem } from '@/stores/ui'
import { Close, Refresh, CircleClose, Remove } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  tab: null as TabItem | null,
})

function isActive(tab: TabItem) {
  return tab.fullPath === route.fullPath || tab.name === String(route.name)
}

function isAffix(tab: TabItem) {
  return tab.name === 'DashboardWorkspace' || (tab.name === 'Dashboard' && uiStore.openTabs.length <= 1)
}

function handleCloseTab(tab: TabItem) {
  uiStore.removeTab(tab.name)
  if (isActive(tab)) {
    // Navigate to the latest open tab
    const remaining = uiStore.openTabs
    if (remaining.length > 0) {
      const last = remaining[remaining.length - 1]
      router.push(last.fullPath).catch(() => {})
    } else {
      router.push('/dashboard/workspace').catch(() => {})
    }
  }
}

function openContextMenu(e: MouseEvent, tab: TabItem) {
  // Prevent menu from overflowing window
  const menuWidth = 130
  const menuHeight = 140
  let x = e.clientX
  let y = e.clientY

  if (x + menuWidth > window.innerWidth) {
    x = window.innerWidth - menuWidth - 10
  }
  if (y + menuHeight > window.innerHeight) {
    y = window.innerHeight - menuHeight - 10
  }

  contextMenu.x = x
  contextMenu.y = y
  contextMenu.tab = tab
  contextMenu.visible = true
}

function refreshSelectedTab() {
  const target = contextMenu.tab || { fullPath: route.fullPath }
  contextMenu.visible = false
  nextTick(() => {
    router.replace({
      path: '/redirect' + target.fullPath,
    }).catch(() => {})
  })
}

function closeCurrentTab() {
  if (contextMenu.tab) {
    handleCloseTab(contextMenu.tab)
  }
  contextMenu.visible = false
}

function closeOtherTabs() {
  if (contextMenu.tab) {
    uiStore.removeOtherTabs(contextMenu.tab.name)
    if (!isActive(contextMenu.tab)) {
      router.push(contextMenu.tab.fullPath).catch(() => {})
    }
  }
  contextMenu.visible = false
}

function closeAllTabs() {
  uiStore.removeAllTabs()
  contextMenu.visible = false
  const homeTab = uiStore.openTabs.find(t => t.name === 'DashboardWorkspace' || t.name === 'Dashboard')
  if (homeTab) {
    router.push(homeTab.fullPath).catch(() => {})
  } else {
    router.push('/dashboard/workspace').catch(() => {})
  }
}

// Ensure at least Dashboard is in tabs on initialization
watch(
  () => route.fullPath,
  () => {
    if (route.name && route.meta?.title && route.name !== 'Redirect') {
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
.tags-view-container {
  :deep(.el-scrollbar__wrap) {
    display: flex;
    align-items: center;
  }
}
</style>
