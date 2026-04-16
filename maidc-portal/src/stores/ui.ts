import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface TabItem {
  name: string
  title: string
  fullPath: string
}

export const useUiStore = defineStore('ui', () => {
  // Sidebar
  const sidebarCollapsed = ref(false)

  // Theme
  const primaryColor = ref(localStorage.getItem('maidc-primary-color') || '1677ff')
  const theme = ref<'light' | 'dark'>('light')

  // Tab bar
  const tabBarEnabled = ref(localStorage.getItem('maidc-tab-bar') !== 'false')
  const openTabs = ref<TabItem[]>([])
  const cachedViews = ref<string[]>([])

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setPrimaryColor(color: string) {
    primaryColor.value = color
    localStorage.setItem('maidc-primary-color', color)
  }

  function toggleTabBar() {
    tabBarEnabled.value = !tabBarEnabled.value
    localStorage.setItem('maidc-tab-bar', String(tabBarEnabled.value))
  }

  function addTab(tab: TabItem) {
    if (!openTabs.value.some(t => t.name === tab.name)) {
      openTabs.value.push(tab)
    }
  }

  function removeTab(name: string) {
    openTabs.value = openTabs.value.filter(t => t.name !== name)
  }

  function removeOtherTabs(name: string) {
    openTabs.value = openTabs.value.filter(t => t.name === name || t.name === 'Dashboard')
  }

  function removeAllTabs() {
    openTabs.value = openTabs.value.filter(t => t.name === 'Dashboard')
  }

  function addCachedView(name: string) {
    if (!cachedViews.value.includes(name)) cachedViews.value.push(name)
  }

  function removeCachedView(name: string) {
    cachedViews.value = cachedViews.value.filter(v => v !== name)
  }

  return {
    sidebarCollapsed, primaryColor, theme, tabBarEnabled, openTabs, cachedViews,
    toggleSidebar, setPrimaryColor, toggleTabBar,
    addTab, removeTab, removeOtherTabs, removeAllTabs,
    addCachedView, removeCachedView,
  }
})
