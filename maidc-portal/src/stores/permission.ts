import { defineStore } from 'pinia'
import { ref } from 'vue'
import { asyncRoutes } from '@/router/asyncRoutes'
import { useAuthStore } from './auth'
import type { RouteRecordRaw } from 'vue-router'

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([])
  const permissions = ref<string[]>([])

  function filterRoutes(routeList: RouteRecordRaw[], perms: string[]): RouteRecordRaw[] {
    return routeList.filter(route => {
      if (route.meta?.permission && !perms.includes(route.meta.permission as string)) return false
      if (route.children) route.children = filterRoutes(route.children, perms)
      return true
    }).filter(route => !route.children || route.children.length > 0)
  }

  async function generateRoutes(): Promise<RouteRecordRaw[]> {
    const authStore = useAuthStore()
    const roles = authStore.userInfo?.roles ?? []
    permissions.value = authStore.userInfo?.permissions ?? []
    // admin has all permissions
    const filtered = roles.includes('admin') ? asyncRoutes : filterRoutes(asyncRoutes, permissions.value)
    routes.value = filtered
    return filtered
  }

  function hasPermission(code: string): boolean {
    const authStore = useAuthStore()
    if (authStore.userInfo?.roles?.includes('admin')) return true
    return permissions.value.includes(code)
  }

  return { routes, permissions, generateRoutes, hasPermission }
})
