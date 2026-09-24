# 实施计划: 登录页重新设计（医疗青蓝·品牌叙事分屏）

**日期**: 2026-09-11
**需求来源**: 用户直接描述（http://localhost:3000/login 重新设计登录页）
**目标**: 重设计 LoginPage 视觉层为医疗青蓝品牌风格，保留全部登录逻辑与可访问性

## 用户已确认

- 视觉方向: 医疗青蓝·品牌叙事分屏（左侧品牌墙 + 右侧表单）

## 修改文件

| 文件路径 | 修改说明 |
|----------|----------|
| `maidc-portal/src/views/login/LoginPage.vue` | 重写 template/style；script 登录逻辑保持不变 |

## 设计要点

1. 左侧品牌墙：深青→蓝渐变背景、SVG 心电图脉冲线（描边动画）、玻璃拟态特性卡片×3、装饰光晕
2. 右侧表单：精致表单层级、青蓝焦点环、渐变主按钮、SSO/审计提示/页脚保留
3. 修复缺陷：移除 Google Fonts CDN 引用（内网不可达），改系统字体栈
4. 响应式：≤960px 隐藏品牌墙；≤480px 紧凑间距；prefers-reduced-motion 降级
5. 逻辑不动：authStore.loginAction、redirect、remember、消息提示全部保留

## 实施步骤

- [ ] Task 1: 重写 LoginPage.vue 模板与样式
- [ ] Task 2: type-check 验证（对比存量错误基线，不引入新错误）
- [ ] Task 3: 重建 portal 容器使 localhost:3000 生效
- [ ] Task 4: 浏览器截图视觉验收
- [ ] Task 5: 保存修改记录 docs/feature/2026-09-11-login-redesign.md

## 不做

- 不改 auth store / 路由 / api 层
- 不改框架配置（package.json、tsconfig 等）
