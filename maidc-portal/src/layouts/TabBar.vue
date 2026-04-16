<template>
  <div v-if="uiStore.tabBarEnabled && uiStore.openTabs.length" class="tab-bar">
    <div class="tab-scroll">
      <div
        v-for="tab in uiStore.openTabs"
        :key="tab.name"
        :class="['tab-item', { active: tab.name === String(route.name) }]"
        @click="router.push(tab.fullPath).catch(() => {})"
        @contextmenu.prevent="openContextMenu($event, tab)"
      >
        <span class="tab-title">{{ tab.title }}</span>
        <CloseOutlined
          v-if="tab.name !== 'Dashboard'"
          class="tab-close"
          @click.stop="closeTab(tab)"
        />
      </div>
    </div>

    <!-- Right-click context menu -->
    <div
      v-if="ctx.visible"
      class="tab-context-menu"
      :style="{ left: ctx.x + 'px', top: ctx.y + 'px' }"
    >
      <div class="ctx-item" @click="closeCurrent">关闭当前</div>
      <div class="ctx-item" @click="closeOthers">关闭其他</div>
      <div class="ctx-item" @click="closeAll">关闭全部</div>
    </div>
  </div>
  <!-- Click-away overlay to close context menu -->
  <div v-if="ctx.visible" class="ctx-overlay" @click="ctx.visible = false" @contextmenu.prevent="ctx.visible = false" />
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CloseOutlined } from '@ant-design/icons-vue'
import { useUiStore, type TabItem } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()

const ctx = reactive({
  visible: false,
  x: 0,
  y: 0,
  tab: null as TabItem | null,
})

function closeTab(tab: TabItem) {
  uiStore.removeTab(tab.name)
  // If closing the active tab, navigate to the last remaining tab
  if (tab.name === String(route.name)) {
    const last = uiStore.openTabs[uiStore.openTabs.length - 1]
    if (last) {
      router.push(last.fullPath).catch(() => {})
    }
  }
}

function openContextMenu(e: MouseEvent, tab: TabItem) {
  ctx.x = e.clientX
  ctx.y = e.clientY
  ctx.tab = tab
  ctx.visible = true
}

function closeCurrent() {
  ctx.visible = false
  if (ctx.tab && ctx.tab.name !== 'Dashboard') {
    closeTab(ctx.tab)
  }
}

function closeOthers() {
  ctx.visible = false
  const keep = ctx.tab?.name || 'Dashboard'
  uiStore.removeOtherTabs(keep)
  if (keep !== String(route.name)) {
    const tab = uiStore.openTabs.find(t => t.name === keep)
    if (tab) router.push(tab.fullPath).catch(() => {})
  }
}

function closeAll() {
  ctx.visible = false
  uiStore.removeAllTabs()
  const dash = uiStore.openTabs.find(t => t.name === 'Dashboard')
  if (dash && String(route.name) !== 'Dashboard') {
    router.push(dash.fullPath).catch(() => {})
  }
}
</script>

<style scoped>
.tab-bar {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 12px;
  position: relative;
  user-select: none;
}
.tab-scroll {
  display: flex;
  overflow-x: auto;
  gap: 4px;
  scrollbar-width: none;
}
.tab-scroll::-webkit-scrollbar {
  display: none;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 4px 4px 0 0;
  cursor: pointer;
  white-space: nowrap;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  transition: all 0.2s;
  border: 1px solid transparent;
  border-bottom: none;
  position: relative;
}
.tab-item:hover {
  color: rgba(0, 0, 0, 0.88);
  background: rgba(0, 0, 0, 0.02);
}
.tab-item.active {
  color: var(--ant-color-primary);
  background: #fff;
  border-color: #e8e8e8;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--ant-color-primary);
  border-radius: 2px 2px 0 0;
}
.tab-title {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tab-close {
  font-size: 10px;
  padding: 2px;
  border-radius: 50%;
  transition: all 0.2s;
}
.tab-close:hover {
  background: rgba(0, 0, 0, 0.12);
  color: rgba(0, 0, 0, 0.88);
}
.tab-context-menu {
  position: fixed;
  z-index: 1050;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
  min-width: 120px;
}
.ctx-item {
  padding: 6px 16px;
  cursor: pointer;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  transition: all 0.2s;
}
.ctx-item:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--ant-color-primary);
}
.ctx-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1049;
}
</style>
