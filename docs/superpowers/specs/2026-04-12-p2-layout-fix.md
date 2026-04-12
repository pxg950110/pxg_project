# P2: 布局路由修复

## 目标
修复 BasicLayout（侧边栏+header+用户菜单）不显示的问题。

## 根因
`asyncRoutes.ts` 中外层 `path: '/'` 与 `constantRoutes.ts` 中的根路由冲突。`router.addRoute()` 添加新的 `/` 路由时覆盖了带 `BasicLayout` component 的原路由，导致动态页面渲染在 layout 之外。

## 修复方案

修改 `maidc-portal/src/router/guards.ts` 的路由注册逻辑：
- 将动态路由的 children 逐个 `addRoute` 到已有的 `'/'` 路由下
- 不再添加新的 `path: '/'` 路由

```typescript
// 修复前
routes.forEach(route => router.addRoute(route))

// 修复后
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

## 涉及文件
- `maidc-portal/src/router/guards.ts` — 修改路由注册逻辑

## 验证
- 登录后所有页面显示左侧侧边栏（220px）
- 顶部header显示用户名/头像/退出按钮
- 侧边栏菜单可正常导航
- 所有模块页面正常渲染在 layout 内容区
