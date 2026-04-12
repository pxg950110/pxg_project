# P2: 布局路由修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 BasicLayout（侧边栏+header+用户菜单）不显示的问题，使所有动态路由页面正确渲染在 layout 内。

**Architecture:** 修改 `guards.ts` 中的路由注册逻辑，将 `asyncRoutes` 的 children 合并到已有的 BasicLayout 根路由下，避免 `path: '/'` 冲突。

**Tech Stack:** Vue Router 4, TypeScript

**Spec:** `docs/superpowers/specs/2026-04-12-p2-layout-fix.md`

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `maidc-portal/src/router/guards.ts` | 路由守卫+动态路由注册 |

---

### Task 1: 修复路由注册逻辑

**Files:**
- Modify: `maidc-portal/src/router/guards.ts:31` (routes.forEach 行)

- [ ] **Step 1: 修改 guards.ts 中的路由注册代码**

将第31行：
```typescript
routes.forEach(route => router.addRoute(route))
```

替换为：
```typescript
routes.forEach(route => {
  if (route.path === '/' && route.children) {
    route.children.forEach(child => {
      router.addRoute('/', child)
    })
  } else {
    router.addRoute(route)
  }
})
```

**原理：** `router.addRoute('/', child)` 将路由添加为 `path: '/'` 的子路由，Vue Router 会自动将其匹配到已有的 BasicLayout 根路由下。这避免了添加新的 `path: '/'` 路由覆盖 BasicLayout。

- [ ] **Step 2: 验证修改后的文件**

确认 `guards.ts` 完整内容：
```typescript
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
```

- [ ] **Step 3: 使用 chrome-devtools 验证**

1. 导航到 `http://localhost:3000/login`
2. 登录 admin / Admin@123
3. 等待跳转到 dashboard
4. 用 `take_snapshot` 检查页面结构

Expected: 页面左侧显示侧边栏（含菜单项），顶部显示 header（含用户名/头像），主内容区显示 dashboard 内容。

- [ ] **Step 4: 截图确认**

```
mcp__chrome-devtools__take_screenshot → filePath: docs/superpowers/test-results/screenshots/p2-layout-fixed.png
```

Expected: 截图显示完整的带侧边栏+header 的页面布局。

- [ ] **Step 5: 验证导航功能**

点击侧边栏菜单项（如"模型管理"、"数据管理"），确认页面切换正常且 layout 不消失。

- [ ] **Step 6: Commit**

```bash
git add maidc-portal/src/router/guards.ts
git commit -m "fix: resolve BasicLayout not rendering - merge async routes into root route"
```
