# MAIDC 前端全面重构实施计划（Element Plus + Tailwind CSS）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 MAIDC 前端（`maidc-portal`）全面重构为基于 Element Plus 2.8+ 与 Tailwind CSS 3 的现代化医疗数据科技风系统，统一全站视觉设计规范并重塑核心工作流。

**Architecture:** 
1. 采用 Vite 5 + unplugin 自动按需导入机制解耦繁琐导入；
2. 建立统一的 Design Tokens（CSS Variables + Tailwind 配置），与 Element Plus 主题无缝对齐；
3. 封装高阶通用组件（`ProTable`, `StatusTag`, `ProDrawer`），按“基建 → 骨架导航 → 工作台看板 → 临床检索/患者360 → 随访与模型 → 彻底卸载 AntD”的阶梯进行无中断重构。

**Tech Stack:** Vue 3.4, TypeScript 5, Vite 5, Element Plus 2.8, Tailwind CSS 3, ECharts 5.5, @vue-flow/core, Pinia 2.1

---

## 模块与文件变更蓝图

```
maidc-portal/
├── package.json                         # 依赖替换：移除 antd/icons，引入 element-plus/icons, tailwindcss, unplugin
├── vite.config.ts                       # 注入 unplugin-auto-import & unplugin-vue-components
├── tailwind.config.js                   # 新增：定义医疗科技色系 (sky, slate, emerald, amber, red) 与圆角阴影
├── postcss.config.js                    # 新增：配置 tailwindcss 与 autoprefixer
├── src/
│   ├── main.ts                          # 引入全局样式体系与 Element Plus 中文语言包
│   ├── assets/styles/
│   │   ├── main.scss                    # 全局样式入口
│   │   ├── tailwind.css                 # Tailwind 指令导入与基准变量
│   │   └── element-variables.scss       # Element Plus 医疗科技主题变量
│   ├── components/common/
│   │   ├── ProTable.vue                 # 高阶数据表格（内置搜索/分页/列配置）
│   │   ├── StatusTag.vue                # 医疗状态彩色微胶囊
│   │   └── TrendChart.vue               # 检验/指标历史趋势与危急区间图表
│   ├── layout/                          # 全新骨架布局
│   │   ├── AppLayout.vue                # 布局容器
│   │   ├── components/
│   │   │   ├── Sidebar.vue              # 深色沉浸式折叠侧边栏 (el-menu)
│   │   │   ├── Header.vue               # 顶栏（含全局检索 Ctrl+K、角色科室切换、未读消息）
│   │   │   └── TagsView.vue             # 多页签导航胶囊
│   ├── views/
│   │   ├── login/LoginPage.vue          # 登录页重构
│   │   ├── dashboard/workspace/
│   │   │   ├── WorkspaceView.vue        # 个人工作台
│   │   │   ├── MetricCards.vue          # 4列指标卡
│   │   │   └── TodoSection.vue          # 待办任务流
│   │   ├── data-cdr/
│   │   │   ├── ClinicalSearch.vue       # 临床检索
│   │   │   └── patient/Patient360Overview.vue # 患者就诊360全景
│   │   └── followup/
│   │       └── FollowupWorkbench.vue    # 随访工作台
```

---

## 阶段一：依赖与原子化设计系统基建（Foundation & Tokens）

### Task 1: 依赖安装与构建工具配置

**Files:**
- Modify: `maidc-portal/package.json`
- Modify: `maidc-portal/vite.config.ts`
- Create: `maidc-portal/postcss.config.js`
- Create: `maidc-portal/tailwind.config.js`

- [x] **Step 1: 更新 package.json 依赖**

在 `maidc-portal/package.json` 中增加 `element-plus`、`@element-plus/icons-vue`、`tailwindcss`、`postcss`、`autoprefixer`、`unplugin-vue-components`、`unplugin-auto-import`。

```json
{
  "dependencies": {
    "element-plus": "^2.8.3",
    "@element-plus/icons-vue": "^2.3.1",
    "vue": "^3.4.27",
    "vue-router": "^4.3.2",
    "pinia": "^2.1.7",
    "axios": "^1.7.2",
    "dayjs": "^1.11.11",
    "echarts": "^5.5.0",
    "vue-echarts": "^6.7.3",
    "nprogress": "^0.2.0",
    "@vue-flow/core": "^1.48.2",
    "@vue-flow/background": "^1.3.2",
    "@vue-flow/controls": "^1.1.3",
    "@vue-flow/minimap": "^1.5.4"
  },
  "devDependencies": {
    "tailwindcss": "^3.4.11",
    "postcss": "^8.4.45",
    "autoprefixer": "^10.4.20",
    "unplugin-vue-components": "^0.27.4",
    "unplugin-auto-import": "^0.18.2",
    "@vitejs/plugin-vue": "^5.0.4",
    "sass": "^1.78.0",
    "typescript": "^5.4.5",
    "vite": "^5.2.12",
    "vue-tsc": "^2.0.19"
  }
}
```

- [x] **Step 2: 创建 postcss.config.js 与 tailwind.config.js**

在 `maidc-portal/postcss.config.js` 中配置：
```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
```

在 `maidc-portal/tailwind.config.js` 中扩展医疗科技设计令牌：
```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  corePlugins: {
    preflight: false, // 禁用默认 preflight，防止破坏 Element Plus 原生组件基准样式
  },
  theme: {
    extend: {
      colors: {
        clinical: {
          primary: '#0EA5E9',
          'primary-dark': '#0284C7',
          navy: '#0F172A',
          surface: '#F8FAFC',
          card: '#FFFFFF',
          border: '#E2E8F0',
          success: '#10B981',
          warning: '#F59E0B',
          danger: '#EF4444',
          purple: '#8B5CF6'
        }
      },
      boxShadow: {
        'clinical-sm': '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
        'clinical': '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.05)',
        'clinical-lg': '0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.05)',
      }
    },
  },
  plugins: [],
}
```

- [x] **Step 3: 配置 vite.config.ts 自动按需引入**

配置 `AutoImport` 和 `Components`：
```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
})
```

- [x] **Step 4: 安装依赖并测试启动编译**

运行：`cd maidc-portal && npm install`
预期：依赖安装成功，生成 `package-lock.json`。

- [x] **Step 5: 提交基建配置变更**

```bash
git add maidc-portal/package.json maidc-portal/vite.config.ts maidc-portal/postcss.config.js maidc-portal/tailwind.config.js
git commit -m "build(portal): configure Element Plus, Tailwind CSS, and unplugin auto-import"
```

---

### Task 2: 全局样式体系与设计 Tokens 实现

**Files:**
- Create: `maidc-portal/src/assets/styles/tailwind.css`
- Create: `maidc-portal/src/assets/styles/element-variables.scss`
- Create: `maidc-portal/src/assets/styles/main.scss`
- Modify: `maidc-portal/src/main.ts`

- [x] **Step 1: 编写 tailwind.css**

在 `maidc-portal/src/assets/styles/tailwind.css` 中引入基础与工具类：
```css
@tailwind components;
@tailwind utilities;

/* 临床工作台通用工具类 */
@layer utilities {
  .text-clinical-secondary {
    color: #64748B;
  }
  .bg-clinical-canvas {
    background-color: #F8FAFC;
  }
}
```

- [x] **Step 2: 编写 element-variables.scss 医疗科技主题**

在 `maidc-portal/src/assets/styles/element-variables.scss` 中覆盖 Element Plus CSS 变量：
```scss
:root {
  --el-color-primary: #0EA5E9;
  --el-color-primary-light-3: #7DD3FC;
  --el-color-primary-light-5: #BAE6FD;
  --el-color-primary-light-7: #E0F2FE;
  --el-color-primary-light-9: #F0F9FF;
  --el-color-primary-dark-2: #0284C7;

  --el-color-success: #10B981;
  --el-color-warning: #F59E0B;
  --el-color-danger: #EF4444;
  --el-color-info: #64748B;

  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --el-border-radius-round: 20px;

  --el-font-size-base: 14px;
  --el-text-color-primary: #0F172A;
  --el-text-color-regular: #334155;
  --el-text-color-secondary: #64748B;
  --el-border-color: #E2E8F0;
  --el-border-color-light: #F1F5F9;
  --el-bg-color: #FFFFFF;
  --el-bg-color-page: #F8FAFC;
}
```

- [x] **Step 3: 聚合到 main.scss 并在 main.ts 挂载**

在 `maidc-portal/src/assets/styles/main.scss` 中引用并初始化：
```scss
@import './element-variables.scss';
@import './tailwind.css';

html, body, #app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
  -webkit-font-smoothing: antialiased;
}
```

在 `maidc-portal/src/main.ts` 中配置 Element Plus 中文语言包（`zh-cn`）与图标全局注册。

- [x] **Step 4: 验证编译**

运行：`cd maidc-portal && npm run build`（验证无 SCSS/TS 解析报错）
预期：`vite build` 成功。

- [x] **Step 5: 提交样式系统**

```bash
git add maidc-portal/src/assets/styles/ maidc-portal/src/main.ts
git commit -m "style(portal): establish clinical design tokens and Element Plus theme styling"
```

---

## 阶段二：骨架布局与导航重构（App Shell）

### Task 3: 现代临床工作台布局架构（AppLayout, Sidebar, Header, TagsView）

**Files:**
- Create: `maidc-portal/src/layout/AppLayout.vue`
- Create: `maidc-portal/src/layout/components/Sidebar.vue`
- Create: `maidc-portal/src/layout/components/Header.vue`
- Create: `maidc-portal/src/layout/components/TagsView.vue`
- Modify: `maidc-portal/src/router/constantRoutes.ts`

- [x] **Step 1: 实现 Sidebar.vue（沉浸深色 #0F172A）**

基于 `el-menu` 构建深色侧边栏，支持 64px/240px 伸缩折叠、徽标统计展示与递归菜单项渲染。

- [x] **Step 2: 实现 Header.vue（快捷检索与角色切换）**

顶栏包含：
- 折叠展开 Toggle 按钮；
- 全局快捷搜索输入框（带有 `Ctrl + K` 快捷标识）；
- 当前科室/角色微胶囊；
- 消息提醒下拉（`el-badge` + `el-dropdown`）；
- 用户头像与注销操作。

- [x] **Step 3: 实现 TagsView.vue（多页签胶囊导航）**

支持已打开病历与页面的平滑切换、右键菜单（刷新、关闭其他、关闭当前）。

- [x] **Step 4: 整合 AppLayout.vue 并绑定基础路由**

在 `maidc-portal/src/layout/AppLayout.vue` 中将 Sidebar、Header、TagsView 与 `<router-view>` 进行弹性布局组装，并在 `constantRoutes.ts` 中作为主路由载体。

- [x] **Step 5: 运行验证与提交**

运行：`cd maidc-portal && npm run build`
提交：
```bash
git add maidc-portal/src/layout/ maidc-portal/src/router/constantRoutes.ts
git commit -m "feat(portal): rebuild app layout with modern clinical sidebar and header"
```

---

## 阶段三：通用高阶组件封装（ProTable, StatusTag, TrendChart）

### Task 4: 封装 ProTable 与业务状态微胶囊组件

**Files:**
- Create: `maidc-portal/src/components/common/ProTable.vue`
- Create: `maidc-portal/src/components/common/StatusTag.vue`
- Create: `maidc-portal/src/components/common/TrendChart.vue`

- [x] **Step 1: 实现 ProTable.vue**

封装搜索表单折叠（展开/收起）、`el-table` 自适应高度、列显隐配置项、骨架加载与 `el-pagination` 事件联动。

- [x] **Step 2: 实现 StatusTag.vue**

输入 `status` 状态枚举与类型，自动输出对应医疗语义的 `el-tag`（如：正常、危急、待审、进行中、已归档）。

- [x] **Step 3: 实现 TrendChart.vue**

基于 ECharts 封装通用的数值变化折线图，内置参考值上下限虚线指示带与异常点高亮。

- [x] **Step 4: 编译检查与提交**

```bash
git add maidc-portal/src/components/common/
git commit -m "feat(portal): implement ProTable, StatusTag, and TrendChart common components"
```

---

## 阶段四：核心业务场景重构落地

### Task 5: 重构个人工作台与系统总览（Workspace & Overview）

**Files:**
- Modify: `maidc-portal/src/views/dashboard/workspace/WorkspaceView.vue`
- Modify: `maidc-portal/src/views/dashboard/workspace/MetricCards.vue`
- Modify: `maidc-portal/src/views/dashboard/workspace/TodoSection.vue`
- Modify: `maidc-portal/src/views/dashboard/Overview.vue`

- [x] **Step 1: 重构 MetricCards.vue**

使用 Tailwind Grid（4 列排版），白底卡片配合顶部 2px 青蓝渐变条，展示“在管患者”、“待访队列”、“模型推理总数”、“危急值预警”。

- [x] **Step 2: 重构 TodoSection.vue**

采用 `el-timeline` + `el-table` 清晰列出今日待办事项，支持快速跳转与状态标记。

- [x] **Step 3: 重构 WorkspaceView.vue 与 Overview.vue**

整合工作台看板并适配 ECharts 科技蓝/翡翠绿主题。

- [x] **Step 4: 验证与提交**

```bash
git add maidc-portal/src/views/dashboard/
git commit -m "refactor(dashboard): modernize workspace and overview with Element Plus & Tailwind"
```

---

### Task 6: 重构临床检索与患者 360 视图（Clinical Search & Patient 360）

**Files:**
- Modify: `maidc-portal/src/views/data-cdr/ClinicalSearch.vue`
- Modify: `maidc-portal/src/views/data-cdr/PatientList.vue`
- Modify: `maidc-portal/src/views/data-cdr/patient/Patient360Overview.vue`

- [x] **Step 1: 重构 ClinicalSearch.vue 检索表单**

基于 `el-form` 内联排版与 `ProTable` 实现多条件组合筛选（ICD诊断、就诊科室、检验指标异常状态）。

- [x] **Step 2: 重构 Patient360Overview.vue**

实现：
1. 顶部患者画像基本信息栏（年龄、性别、血型、过敏史，带风险等级徽标）；
2. 左侧就诊事件垂直轴（门诊/急诊/住院）；
3. 右侧检验指标历史时序联动 `TrendChart` 图表。

- [x] **Step 3: 验证与提交**

```bash
git add maidc-portal/src/views/data-cdr/ClinicalSearch.vue maidc-portal/src/views/data-cdr/PatientList.vue maidc-portal/src/views/data-cdr/patient/
git commit -m "refactor(clinical): rebuild clinical search and patient 360 overview"
```

---

### Task 7: 重构专病管理与随访工作台（Disease & Followup）

**Files:**
- Modify: `maidc-portal/src/views/data-cdr/DiseaseList.vue`
- Modify: `maidc-portal/src/views/followup/FollowupWorkbench.vue`

- [x] **Step 1: 重构 FollowupWorkbench.vue**

使用 `el-segmented` 分段器进行随访状态切换，结合 `el-drawer` 抽屉式快速完成问卷录入。

- [x] **Step 2: 验证与提交**

```bash
git add maidc-portal/src/views/data-cdr/DiseaseList.vue maidc-portal/src/views/followup/FollowupWorkbench.vue
git commit -m "refactor(followup): rebuild disease management and followup workbench"
```

---

## 阶段五：清理与全量质量验收（Cleanup & Quality Gate）

### Task 8: 移除 Ant Design Vue 依赖与生产构建验收

**Files:**
- Modify: `maidc-portal/package.json`
- Test: 全站 TypeScript 类型检查与 Vite 生产打包

- [x] **Step 1: 移除 ant-design-vue 依赖**

从 `maidc-portal/package.json` 中彻底移除 `ant-design-vue` 与 `@ant-design/icons-vue`。

- [x] **Step 2: 全量执行类型校验**

运行：`cd maidc-portal && npm run type-check`
预期：零 TS 类型错误。

- [x] **Step 3: 全量执行生产构建**

运行：`cd maidc-portal && npm run build`
预期：成功生成 `dist/` 生产静态资源，无资源丢失或损坏。

- [x] **Step 4: 提交依赖卸载与最终构建验证**

```bash
git add maidc-portal/package.json maidc-portal/package-lock.json
git commit -m "chore(portal): completely remove Ant Design Vue and verify production build"
```
