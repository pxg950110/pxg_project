# MAIDC 前端全面重新设计规范（Element Plus + Tailwind CSS）

> **Medical AI Data Center (MAIDC)** — 前端全盘重构与视觉设计系统升级规范  
> 文档日期：2026-09-22  
> 适用工程：`maidc-portal/`  
> 状态：设计已确认待实施  

---

## 1. 目标与背景

随着 MAIDC 平台从早期原型向医疗 AI 全生命周期平台（数据标准、临床检索、患者 360、模型生命周期、随访管理）深入演进，现存前端体系存在以下痛点：
1. **设计语言不统一**：部分页面残留深色科技大屏风格，部分为默认 AntD 商务后台，视觉调性断层。
2. **信息密度与临床场景不匹配**：临床检验、病历、多模态时序数据需要更高的信息密度、清晰的异常标识和轻量多层阴影排版。
3. **技术栈演进需求**：国内医疗数字化系统生态中，Element Plus 具备极高的社区成熟度与业务贴合度；结合 Tailwind CSS 3 原子化样式方案，能够最大化兼顾复杂表单/表格组件与现代仪表盘排版的灵活性。

本方案旨在对 `maidc-portal` 进行**端到端全盘重构**：升级基础设计系统规范、重塑全局骨架布局与导航交互、全面重构核心高频业务场景，并将 UI 基础组件库由 Ant Design Vue 4 迁移至 **Element Plus + Tailwind CSS**。

---

## 2. 总体架构与技术选型

### 2.1 技术栈矩阵

| 维度 | 旧方案 | 新设计方案 | 升级收益 |
|------|--------|------------|----------|
| **基础框架** | Vue 3.4 + TypeScript 5 | Vue 3.4 + TypeScript 5 | 保持稳定兼容 |
| **构建工具** | Vite 5 | Vite 5 + unplugin 插件套件 | 自动化按需引入，零手动组件导入 |
| **UI 组件库** | Ant Design Vue 4.2 | **Element Plus 2.8+** | 医疗场景契合度高，生态稳定 |
| **图标库** | `@ant-design/icons-vue` | `@element-plus/icons-vue` | 原生契合 Element Plus |
| **样式体系** | SCSS + 零散全局覆盖 | **Tailwind CSS 3 + PostCSS + SCSS** | 统一 Design Tokens，原子化高效排版 |
| **图表可视化** | ECharts 5.5 + vue-echarts | ECharts 5.5 + 统一主题封装 | 适配现代医疗浅色科技风图表规范 |
| **流编排画布** | `@vue-flow/core` | `@vue-flow/core` + Tailwind 卡片 | 统一节点卡片视觉规范 |

### 2.2 自动按需引入与工程规范
在 `vite.config.ts` 中配置插件流水线：
- `unplugin-auto-import/vite`：自动导入 `vue`、`vue-router`、`pinia` 及 Element Plus 运行时反馈 API（如 `ElMessage`, `ElNotification`, `ElMessageBox`）。
- `unplugin-vue-components/vite`：自动识别并注入 `el-*` 组件声明，自动生成 `components.d.ts`。
- `tailwind.config.js` 配置：设置精准的 content 扫描路径，微调 Preflight 基础重置规则，确保与 Element Plus 表单/按钮样式和谐共存。

---

## 3. 视觉设计系统（Modern Clinical Tech Tokens）

整体风格定位为**现代医疗数据科技风（Modern Clinical Tech）**：以专业临床深蓝作为框架底色，医疗青蓝作为品牌主导，纯净浅冷灰作为页面背景，微细边框与柔和多层阴影营造精密、专业、严谨的数据质感。

### 3.1 核心颜色令牌（Color Palette）

```scss
// 医疗科技青蓝（Primary）
--el-color-primary: #0EA5E9;       // Sky-500
--el-color-primary-dark-2: #0284C7;// Sky-600
--el-color-primary-light-3: #7DD3FC;// Sky-300
--el-color-primary-light-9: #F0F9FF;// Sky-50

// 沉浸深蓝与黑夜层（Slate & Navy）
--color-slate-900: #0F172A;        // 侧边栏及深色卡片底色
--color-slate-800: #1E293B;
--color-slate-100: #F1F5F9;
--color-slate-50:  #F8FAFC;        // 全站主工作区背景

// 功能与状态警示色
--el-color-success: #10B981;       // Emerald-500 (正常/健康)
--el-color-warning: #F59E0B;       // Amber-500 (异常观察/待办)
--el-color-danger:  #EF4444;       // Red-500 (危急值/告警)
--el-color-info:    #64748B;       // Slate-500 (次要信息)
--color-clinical-purple: #8B5CF6;  // Violet-500 (队列/知识库)
```

### 3.2 布局与容器规范
- **主背景**：`bg-slate-50`（#F8FAFC）。
- **内容卡片**：`bg-white rounded-xl border border-slate-200/80 shadow-sm hover:shadow transition-all`。
- **信息密度**：表格默认采用紧凑排版，行高 44px~48px，字号 13px/14px，数字使用高可读等宽字体。

---

## 4. 全局骨架布局与导航重构（App Shell）

### 4.1 侧边栏导航（Sidebar）
- **深色沉浸式设计**：使用 `#0F172A`（Slate-900）作为侧边栏背景，与白色主内容区形成强视觉纵深。
- **胶囊高亮状态**：激活菜单项采用 `bg-sky-500 text-white rounded-lg`，二级菜单采用微凹陷微光样式。
- **折叠交互**：支持 64px（Mini 图标模式）与 240px（标准展开）平滑收缩，配合悬停 Floating Tooltip。

### 4.2 智能顶栏（Header & Command Center）
- **全局快捷临床检索**：顶栏正中提供快捷搜索框，快捷键 `Ctrl + K` 呼出，输入患者姓名/住院号/门诊号/专病名称秒级全局直达。
- **多科室与角色上下文切换**：右侧展示当前登录人科室（如：呼吸内科 / 临床科研室），支持下拉即时切换。
- **预警中心**：基于 `el-badge` 展示未处理危急值与模型生命周期预警。

### 4.3 多页签导航（TagsView）
- 胶囊式 Tab 标签设计，支持右键菜单（关闭当前、关闭其他、全部关闭、刷新页面），极大提升多病历交叉比对体验。

---

## 5. 核心高频业务场景重构规范

### 5.1 个人工作台与系统看板（Workspace & Overview）
- **动态角色指标卡**：4 列响应式 Grid 布局，白底搭配顶部 2px 科技蓝渐变线条，包含数字跳动、同比/环比及健康态趋势指示。
- **待办协同流**：基于 `el-timeline` 与 `el-table`，根据当前用户角色（临床医生、科研人员、数据工程师）自动聚合对应待办事项。

### 5.2 临床检索与患者就诊 360 视图（Clinical Search & Patient 360）
- **高级布尔/自然语言检索器**：集成结构化查询（诊断 ICD、检验指标范围、用药类型）与文本检索，卡片/表格一键切换。
- **患者就诊 360 视图**：
  - 顶部患者人口学概览面板（脱敏展示、关键危急标签）。
  - 左侧纵向就诊时间轴（门诊、急诊、住院多色标区分）。
  - 右侧检验趋势联动图表（ECharts 自动绘制参考区间阴影、异常上下箭头标注）。

### 5.3 专病管理与随访工作台（Disease Management & Followup）
- **随访执行工作台**：采用分段器（今日待访、逾期未访、已访）无缝筛选，侧边抽屉式即时录入问卷，不中断上下文。
- **量表设计器**：两栏布局，左侧题目编排与拖拽排序，右侧属性与打分逻辑配置，基于 Element Plus 题型组件标准化封装。

### 5.4 模型生命周期与 ETL 编排（Model & ETL Pipeline）
- **模型生命周期看板**：以 `el-steps` 串联“训练创建 → 离线评估 → 伦理审批 → 部署上线 → 推理监控”。
- **ETL 拓扑编排**：统一 `@vue-flow` 画布节点样式，采用微圆角卡片并带状态呼吸灯指示管道运行。

---

## 6. 通用组件库封装计划

1. **`ProTable`**：深度封装 `el-table`，集成检索表单折叠展开、自动自适应高度、列显隐配置、分页联动与数据加载骨架屏。
2. **`ProDrawer / ProModal`**：标准化弹窗结构，统一头部状态与底部操作栏固定样式。
3. **`StatusTag`**：医疗通用状态胶囊，根据业务枚举自动映射 Element Plus 状态色与图标。
4. **`TrendChart`**：医疗检验专用趋势图，自动计算并填充正常值绿色/灰度安全带，标示危急点。

---

## 7. 实施分步计划与演进路线

1. **阶段 1：依赖与设计系统基础设施（Token & Base Setup）**
   - 配置 Element Plus、Tailwind CSS 3、unplugin 工具链，建立全局色彩与样式重置。
2. **阶段 2：应用骨架布局重塑（App Layout & Shell）**
   - 重构登录页、Sidebar 侧边栏、Header 顶栏及 TagsView 标签页。
3. **阶段 3：工作台与系统总览重构（Workspace & Dashboard）**
   - 重构 `WorkspaceView.vue`、`Overview.vue` 及其子组件。
4. **阶段 4：临床检索、患者 360 与随访重构（Clinical Core）**
   - 重构 `ClinicalSearch.vue`、`Patient360Overview.vue`、`FollowupWorkbench.vue`。
5. **阶段 5：模型生命周期、ETL 管道重构与 AntD 彻底移除（Model, ETL & Cleanup）**
   - 替换其余业务模块中的 AntD 组件，卸载 `ant-design-vue`，执行全量类型校验与生产构建。
