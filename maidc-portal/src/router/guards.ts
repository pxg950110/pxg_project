import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { getToken } from '@/utils/auth'

const WHITE_LIST = ['/login', '/403', '/404', '/500']

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    document.title = `${to.meta.title || ''} - MAIDC`
    const token = getToken()

    if (token) {
      if (to.path === '/login') {
        next({ path: '/' })
      } else {
        const authStore = useAuthStore()
        if (authStore.userInfo) {
          next()
        } else {
          try {
            await authStore.getUserInfoAction()
            const permissionStore = usePermissionStore()
            const routes = await permissionStore.generateRoutes()
            routes.forEach(route => {
              if (route.path === '/' && route.children) {
                route.children.forEach(child => {
                  router.addRoute('/', child)
                })
              } else {
                router.addRoute(route)
              }
            })
            next({ ...to, replace: true })
          } catch {
            authStore.logoutAction()
            next(`/login?redirect=${to.path}`)
          }
        }
      }
    } else {
      if (WHITE_LIST.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${to.path}`)
      }
    }
  })
}
