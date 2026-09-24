import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'
import { getToken, setToken } from '@/utils/auth'

const WHITE_LIST = ['/login', '/403', '/404', '/500']
const DEV_MODE = import.meta.env.DEV && import.meta.env.VITE_MOCK_AUTH === 'true'

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    document.title = `${to.meta.title || ''} - MAIDC`
    const token = getToken()

    if (DEV_MODE && !token) {
      setToken('dev-mock-token', 'dev-mock-refresh', 86400)
    }

    if (token || DEV_MODE) {
      if (to.path === '/login') {
        next({ path: '/' })
      } else {
        const authStore = useAuthStore()
        if (authStore.userInfo) {
          next()
        } else {
          try {
            if (DEV_MODE) {
              authStore.userInfo = {
                id: 1,
                username: 'admin',
                realName: 'Admin',
                roles: ['admin'],
                orgId: 0,
                permissions: ['*'],
              }
            } else {
              await authStore.getUserInfoAction()
            }
            const permissionStore = usePermissionStore()
            const routes = await permissionStore.generateRoutes()
            routes.forEach(route => {
              if (route.path === '/' && route.children) {
                route.children.forEach(child => {
                  router.addRoute('Root', child)
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
