import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getWorkspaceDashboard, completeTodo } from '@/api/workspace'
import type { WorkspaceDashboardVO } from '@/api/workspace'

export const useWorkspaceStore = defineStore('workspace', () => {
  const dashboard = ref<WorkspaceDashboardVO | null>(null)
  const loading = ref(false)

  async function fetchDashboard() {
    loading.value = true
    try {
      const res = await getWorkspaceDashboard()
      dashboard.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function completeTask(id: number) {
    await completeTodo(id)
    if (dashboard.value) {
      dashboard.value.todos = dashboard.value.todos.filter(t => t.id !== id)
    }
  }

  function $reset() {
    dashboard.value = null
    loading.value = false
  }

  return { dashboard, loading, fetchDashboard, completeTask, $reset }
})
