import { defineStore } from 'pinia'

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const cachedViews = ref<string[]>([])
  const theme = ref<'light' | 'dark'>('light')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function addCachedView(name: string) {
    if (!cachedViews.value.includes(name)) cachedViews.value.push(name)
  }

  function removeCachedView(name: string) {
    cachedViews.value = cachedViews.value.filter(v => v !== name)
  }

  return { sidebarCollapsed, cachedViews, theme, toggleSidebar, addCachedView, removeCachedView }
})
