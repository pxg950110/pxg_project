# 实施计划: 前端登录页多方案设计对比稿

**需求号**: 20260911（临时，无 TFS 工作项）
**日期**: 2026-09-11
**目标**: 针对 maidc-portal 登录页产出 3 套不同风格的静态设计稿（可直接浏览器打开），供团队评审对比，择定后再落地到 `LoginPage.vue`。

---

## 背景

- 现有登录页 `maidc-portal/src/views/login/LoginPage.vue` 已实现：左右分栏、左侧深青品牌墙（心电图动效）、右侧表单 + SSO，认证链路已打通（Pinia auth store + `/auth/login`）。
- 用户决策：先出多方案对比稿，评审后再定稿实现。

## 交付物（全部为纯静态 HTML + 内联 CSS，零依赖、双击即开）

| 文件 | 说明 |
|------|------|
| `maidc-portal/design/login/index.html` | 对比导航入口：三方案卡片 + 评审要点 |
| `maidc-portal/design/login/dark-tech.html` | 方案A：深色科技风（暗夜蓝黑 + 霓虹青、数据大屏质感、玻璃拟态） |
| `maidc-portal/design/login/light-minimal.html` | 方案B：简洁浅色风（白底大留白、单栏居中卡片、SaaS 专业风） |
| `maidc-portal/design/login/current-baseline.html` | 方案C：现有风格基线（复刻当前 LoginPage.vue 视觉，作对比参照） |

## 设计共约束

- 每套均包含完整登录要素：用户名/密码（含显示密码切换）、记住我、忘记密码、登录按钮、SSO 入口、安全合规提示、页脚。
- 品牌语境：MAIDC 医疗AI数据中心，临床+科研一体化多中心平台。
- 交互态（hover/focus）与动效（CSS animation）纯 CSS 实现；`prefers-reduced-motion` 降级。
- 基本响应式（≤960px 隐藏品牌区/自适应）。
- 每页右上角悬浮方案徽标 + 「返回对比」链接，便于评审切换。

## 权限检查

- 仅**新增** `maidc-portal/design/login/` 下静态 HTML 与 `docs/plans/`、`docs/feature/` 文档。
- 不修改任何现有源码（LoginPage.vue、路由、store 均不动），不触碰禁止清单文件。✅

## 实施步骤

1. [x] 写入本计划文档
2. [ ] 方案A dark-tech.html
3. [ ] 方案B light-minimal.html
4. [ ] 方案C current-baseline.html
5. [ ] 对比导航 index.html
6. [ ] 浏览器截图逐一验证视觉质量（布局/对比度/交互态）
7. [ ] 保存修改记录到 `docs/feature/2026-09-11-login-design-concepts.md`

## 验收标准

- 三套方案风格差异明显、各自完整自洽，可直接双击打开演示。
- 无占位破图、无样式溢出/遮挡，文案符合医疗平台合规语境。
- 不影响 `maidc-portal` 构建与现有页面（design/ 目录不参与 Vite 编译）。

## 后续（评审后另起任务）

- ~~将选定方案落地到 `src/views/login/LoginPage.vue`~~ → **2026-09-11 评审结论：选定方案A（深色科技风）**，当日落地，见下。

---

## 定稿落地（2026-09-11 第二阶段）

**评审结论**: 选定方案A深色科技风；同时确立「全站科技风」设计约定（已写入根目录 `AGENTS.md`，后续所有页面设计/实现遵循）。

### 落地内容

| 任务 | 文件 | 说明 |
|------|------|------|
| 重写登录页 | `maidc-portal/src/views/login/LoginPage.vue` | 按方案A视觉重写；保留现有认证逻辑（authStore.loginAction / redirect / message）；新增记住用户名（localStorage 回填） |
| 设计约定 | `AGENTS.md` | 新增「设计风格约定」章节，全站科技风基调 + 参考实现路径 |
| 设计规范 | `maidc-portal/docs/design/tech-style-guide.md` | 色板/圆角/玻璃拟态/动效规范，供后续页面复用 |

### 实现约束

- 不改路由、store、api、全局样式（global.css 不动），组件全部 scoped
- 保留 `ant-design-vue` message 反馈；表单用原生 input（与设计稿一致）
- metrics 指标为装饰数据，标注 TODO 后续接真实平台统计

### 验证

- [x] `npm run type-check`：30 个错误均为其他模块存量问题（data-etl / model / system 等），登录页零错误
- [x] vite dev (端口3000) 截图核对：落地效果与设计稿一致（数据环/指标卡/玻璃拟态登录卡/渐变按钮）
  - 验证方法：`.env.development.local` 临时关闭 `VITE_MOCK_AUTH`（dev 守卫 mock 会自动把 /login 重定向到工作台），干净 profile 无头 Edge 截图，验证后临时文件已删除、dev server 已停止、环境恢复原状
