# Frontend Design Optimization - Technical Design

## Context

MAIDC（医疗AI数据中心）前端基于 Vue 3 + TypeScript + Ant Design Vue 构建，当前处于功能稳定期。设计稿已完成 135 个设计单元（Pencil .pen），但前端实现存在以下技术债：

### 当前状态
- **样式架构**：全局样式 `global.css` 仅 26 行（reset + 滚动条），组件样式分散在各组件 scoped style 中
- **主题系统**：仅通过 `uiStore.primaryColor` 动态修改 Ant Design token，无完整 Design Token 体系
- **布局系统**：固定宽度布局（侧边栏 220px/64px，Header 60px），无响应式适配
- **组件库**：44 个自定义组件（MetricCard、StatusBadge 等），样式硬编码（如颜色 `#1677ff`）
- **性能状态**：无骨架屏、无图片懒加载、无 CSS 代码分割

### 技术约束
- Vue 3 + TypeScript + Vite（已锁定，不可更改）
- Ant Design Vue 4.x（已锁定，不可降级）
- Pencil 设计稿作为视觉规范（需对齐但允许微调）
- 医疗系统合规要求（需支持 WCAG 2.0 AA）

### 利益相关者
- 医生用户：高强度使用，需清晰视觉层次和快速响应
- 研究人员：多屏工作场景，需响应式适配
- 开发团队：需维护样式一致性，减少重复代码

## Goals / Non-Goals

### Goals
1. **统一主题系统**：建立 CSS 变量 + Ant Design Token 双层体系，支持亮色/暗色主题切换
2. **响应式布局**：实现 1440px/1920px/2560px 三档适配，侧边栏和内容区自适应
3. **组件样式标准化**：20+ 核心组件迁移到 Design Token，提升视觉一致性
4. **性能优化**：骨架屏、图片懒加载、CSS 代码分割，首屏渲染提速 40%
5. **无障碍支持**：ARIA 标签、键盘导航、焦点管理，满足 WCAG 2.0 AA 标准

### Non-Goals
1. **不重构组件逻辑**：仅优化样式和交互，不修改组件 Props/Events/API
2. **不新增功能特性**：不添加新的业务功能（如新页面、新表单）
3. **不更换 UI 框架**：不迁移到 Element Plus/Naive UI 等
4. **不实现 SSR/SSG**：保持 CSR 架构，不引入服务端渲染
5. **不做移动端适配**：仅支持 PC 端（1440px+），不响应移动端尺寸

## Decisions

### Decision 1: CSS 变量 + Ant Design Token 双层体系

**选择方案**：CSS 变量作为基础层 + Ant Design ConfigProvider 作为组件层

**理由**：
- Ant Design Vue 4.x 的 ConfigProvider 仅支持 `token` 属性修改组件样式，无法覆盖自定义组件
- CSS 变量可被自定义组件和 Ant Design 组件同时引用（通过 `var(--color-primary)` 映射到 `token.colorPrimary`）
- 双层体系确保主题切换时全局一致（修改 CSS 变量 → 自动同步到 Ant Design Token）

**替代方案**：
1. **纯 CSS 变量**：放弃 Ant Design Token → 组件样式需全部自定义，维护成本高
2. **纯 Ant Design Token**：自定义组件无法使用 → 需要为每个组件包装 `ConfigProvider`，不符合当前架构
3. **CSS-in-JS (emotion/styled-components)**：增加运行时开销，与 Vite 构建流程冲突

**实现示例**：
```css
/* design-tokens.css */
:root {
  /* 色彩 */
  --color-primary: #1677ff;
  --color-success: #52c41a;
  --color-warning: #faad14;
  --color-error: #ff4d4f;

  /* 字体 */
  --font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  --font-size-base: 14px;
  --font-size-lg: 16px;
  --font-size-sm: 12px;

  /* 间距 */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* 阴影 */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);

  /* 圆角 */
  --radius-sm: 2px;
  --radius-md: 4px;
  --radius-lg: 8px;
}

/* 暗色主题 */
[data-theme='dark'] {
  --color-primary: #3c89ff;
  --color-text: rgba(255, 255, 255, 0.85);
  --color-bg: #141414;
}
```

```typescript
// App.vue
const themeConfig = computed(() => ({
  token: {
    colorPrimary: getComputedStyle(document.documentElement)
      .getPropertyValue('--color-primary').trim(),
    fontSize: parseInt(getComputedStyle(document.documentElement)
      .getPropertyValue('--font-size-base').trim()),
  },
}))
```

### Decision 2: 响应式布局策略

**选择方案**：CSS 媒体查询 + JavaScript 窗口尺寸监听

**理由**：
- 媒体查询处理布局断点（侧边栏宽度、内容区 padding）
- JavaScript 监听窗口尺寸动态调整布局状态（如小屏幕自动折叠侧边栏）
- 结合 Vue 响应式状态管理，避免全局事件监听污染

**替代方案**：
1. **纯 CSS 媒体查询**：无法处理复杂状态（如侧边栏折叠/展开切换）
2. **Container Queries**：浏览器兼容性不足（Safari 16+ 支持，IE 不支持）
3. **固定断点 + 手动切换**：用户体验差，无法自适应窗口大小

**实现方案**：
```typescript
// composables/useResponsive.ts
export function useResponsive() {
  const breakpoints = {
    sm: 1440,
    md: 1920,
    lg: 2560,
  }

  const currentBreakpoint = ref<'sm' | 'md' | 'lg'>('md')

  const updateBreakpoint = () => {
    const width = window.innerWidth
    if (width < breakpoints.sm) currentBreakpoint.value = 'sm'
    else if (width < breakpoints.md) currentBreakpoint.value = 'md'
    else currentBreakpoint.value = 'lg'
  }

  onMounted(() => {
    window.addEventListener('resize', updateBreakpoint)
    updateBreakpoint()
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateBreakpoint)
  })

  return { currentBreakpoint }
}
```

```scss
// BasicLayout.vue
.layout-sider {
  width: 220px;

  @media (max-width: 1440px) {
    width: 200px;
  }

  @media (min-width: 2560px) {
    width: 260px;
  }
}
```

### Decision 3: 骨架屏实现策略

**选择方案**：Vue 3 Suspense + 自定义 Skeleton 组件

**理由**：
- Suspense 是 Vue 3 官方异步组件方案，与 Vite 配合良好
- 自定义 Skeleton 组件可复用（MetricCardSkeleton、TableSkeleton 等）
- 避免引入第三方库（如 `vue-content-loader`）增加体积

**实现示例**：
```vue
<!-- Skeleton/MetricCardSkeleton.vue -->
<template>
  <a-card :bordered="false" class="skeleton-card">
    <a-skeleton active :paragraph="{ rows: 2 }" />
  </a-card>
</template>

<!-- DataDashboard.vue -->
<template>
  <Suspense>
    <template #default>
      <MetricCard :value="metrics.totalPatients" />
    </template>
    <template #fallback>
      <MetricCardSkeleton />
    </template>
  </Suspense>
</template>
```

### Decision 4: 无障碍实现策略

**选择方案**：渐进式 ARIA 增强 + 自动化测试（axe-core）

**理由**：
- 渐进式增强不影响现有功能，逐步添加 ARIA 属性
- axe-core 自动化测试集成到 CI/CD，确保新代码符合 WCAG 2.0 AA
- 避免全面重构，优先处理高频使用页面（Dashboard、患者列表、就诊详情）

**实现示例**：
```vue
<!-- MetricCard/index.vue -->
<template>
  <a-card
    role="article"
    :aria-label="`${title}: ${displayValue}${suffix || ''}`"
    class="metric-card"
  >
    <div class="metric-title">{{ title }}</div>
    <div class="metric-value" aria-live="polite">
      {{ displayValue }}
    </div>
  </a-card>
</template>
```

```json
// package.json
{
  "scripts": {
    "test:a11y": "axe-core --output json --dir ./src"
  }
}
```

## Risks / Trade-offs

### Risk 1: Design Token 迁移导致样式不一致
**风险描述**：20+ 组件迁移到 Design Token 过程中，可能出现颜色、间距不一致的视觉问题

**缓解措施**：
1. 分阶段迁移：先迁移核心组件（MetricCard、StatusBadge），验证后再批量迁移
2. 视觉回归测试：使用 Percy/BackstopJS 进行截图对比测试
3. 回滚机制：Git 分支管理，每个组件迁移独立提交，问题时可快速回滚

### Risk 2: 响应式布局破坏现有功能
**风险描述**：媒体查询和 JavaScript 监听可能影响侧边栏折叠、TabBar 滚动等现有功能

**缓解措施**：
1. 功能测试：为每个响应式断点编写 E2E 测试（Playwright）
2. 手动测试清单：验证 1440px/1920px/2560px 三个分辨率下的核心流程
3. 降级方案：保留固定布局代码，通过环境变量切换响应式功能

### Risk 3: 骨架屏增加首屏加载时间
**风险描述**：Suspense 和 Skeleton 组件可能增加初始 JavaScript 体积，反而降低性能

**缓解措施**：
1. 性能监控：使用 Lighthouse 持续监控首屏加载时间（目标 FCP < 1.5s）
2. 按需加载：仅在高频页面（Dashboard、患者列表）启用骨架屏
3. 体积分析：使用 `vite-bundle-visualizer` 分析构建体积，确保增量 < 50KB

### Risk 4: 无障碍改造影响用户体验
**风险描述**：ARIA 属性和焦点管理可能影响现有交互流程（如 Tab 键导航改变焦点顺序）

**缓解措施**：
1. 用户测试：邀请医生用户参与可用性测试，验证焦点导航是否符合预期
2. 文档说明：提供无障碍使用指南（键盘快捷键说明）
3. 可配置性：允许禁用键盘导航（通过用户设置）

## Migration Plan

### Phase 1: 基础设施搭建（1 周）
1. 创建 `design-tokens.css`，定义全局 CSS 变量
2. 修改 `App.vue`，实现 CSS 变量到 Ant Design Token 的同步
3. 创建 `useResponsive` composable，实现响应式断点监听
4. 配置 axe-core 自动化测试

### Phase 2: 核心组件迁移（2 周）
1. 迁移 MetricCard、StatusBadge、PageContainer（验证 Design Token 体系）
2. 实现 MetricCardSkeleton、TableSkeleton 骨架屏组件
3. 增强 BasicLayout 响应式支持（侧边栏自适应、内容区 padding 调整）
4. 验证视觉一致性（Percy 截图对比测试）

### Phase 3: 页面级优化（2 周）
1. Dashboard 页面骨架屏 + 响应式优化
2. 患者列表、就诊详情页面无障碍改造
3. 错误页面（403/404/500）UI 优化
4. 性能监控和优化（Lighthouse CI）

### Phase 4: 全量迁移和测试（1 周）
1. 剩余 15+ 组件迁移到 Design Token
2. E2E 测试覆盖所有响应式断点
3. 无障碍合规测试（WCAG 2.0 AA）
4. 性能回归测试

### Rollback Strategy
- **代码级回滚**：每个 Phase 独立 Git 分支，问题时可快速回滚到上一个稳定版本
- **功能级降级**：通过环境变量禁用响应式、骨架屏等新功能
- **样式级降级**：保留旧版 CSS 文件，通过 `<link>` 动态切换

## Open Questions

1. **暗色主题需求优先级**：是否需要在 Phase 1 实现暗色主题，还是作为后续优化？
   - 建议：Phase 1 仅定义暗色主题变量，Phase 4 实现切换功能

2. **无障碍测试范围**：是否需要覆盖所有页面，还是仅高频页面（Dashboard、患者列表、就诊详情）？
   - 建议：Phase 3 覆盖高频页面，后续迭代逐步扩展

3. **骨架屏性能阈值**：首屏加载时间提升多少算成功？（40% 提升是否合理？）
   - 需要监控：当前 FCP 时间，确定优化空间

4. **响应式断点数量**：是否需要增加更多断点（如 1280px、3840px）？
   - 建议：Phase 1 先实现 3 档，根据用户反馈调整