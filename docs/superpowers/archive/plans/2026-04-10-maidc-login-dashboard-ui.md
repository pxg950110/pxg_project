# MAIDC 登录+Dashboard 模块 UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Pencil 中完成登录页和 Dashboard 工作台的 UI 细化设计

**Architecture:** 使用 Pencil MCP 工具操作 pencil-new.pen。按页面粒度分任务执行。

**设计规格:** `docs/superpowers/specs/2026-04-08-maidc-design.md`

**Pencil 文件:** `E:/pxg_project/pencil-new.pen`

---

## Task 1: 细化登录页 (nR4Uw)

**Files:**
- Modify: `pencil-new.pen` → `nR4Uw`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `nR4Uw` readDepth 3。

- [x] **Step 2: 优化左侧品牌区**

左侧占50%宽度，深色渐变背景（#1E293B → #334155）：
- MAIDC Logo（白色占位）
- 系统名称: "MAIDC 医疗AI数据中心"
- 副标题: "临床+科研一体化多中心医疗AI平台"
- 3个特性列表（白色半透明文字）：
  - "统一临床数据仓库（CDR）"
  - "全生命周期AI模型管理"
  - "安全合规的多中心协作"
- 底部版本号: "v1.0.0"

- [x] **Step 3: 优化右侧登录表单区**

右侧占50%宽度，白色背景：
- 顶部: "欢迎回来" 标题 + "请登录您的账户" 副标题
- 用户名输入框（带用户图标前缀）
- 密码输入框（带锁图标前缀 + 显示/隐藏切换）
- "记住我" 复选框 + "忘记密码?" 链接
- "登录" 按钮（Primary，全宽）
- 分割线: "其他登录方式"
- SSO按钮: "医院统一认证登录" (灰色轮廓按钮)
- 底部: "© 2026 MAIDC · 隐私政策 · 使用条款"

- [x] **Step 4: 增加登录安全提示**

在登录表单下方添加小字提示：
- "您的登录行为将被审计记录"
- "连续5次登录失败将锁定账户30分钟"

- [x] **Step 5: 截图验证**

截取完整登录页，确认左右分栏比例、品牌区视觉效果、表单区对齐。

---

## Task 2: 细化 Dashboard 工作台页 (QptC0)

**Files:**
- Modify: `pencil-new.pen` → `QptC0`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `QptC0` readDepth 3。

- [x] **Step 2: 优化欢迎区域**

在页面顶部添加个性化欢迎栏：
- "早上好，张医生" (大标题)
- "今天是2026年4月10日，您有 3 条待办事项" (副标题)
- 右侧: "快速入口" 按钮组: [注册模型] [新建评估] [提交审批]

- [x] **Step 3: 优化数据概览卡片行**

6个 MetricCard（2行3列）：

第一行:
- 模型总数: 28 (+2 本周新增)
- 活跃部署: 8 (RUNNING)
- 今日推理: 12,456 次

第二行:
- 患者记录: 156,000 条
- 研究项目: 12 个
- 待审批: 5 项 (黄色高亮)

- [x] **Step 4: 添加"模型管理"概览区**

左侧区域（60%宽度）：
- 标题: "模型状态分布" + "查看全部"链接
- 状态分布横向条形图占位 (MetricChart "模型状态分布")
- 4个状态统计: DRAFT(3) / REGISTERED(2) / PUBLISHED(18) / DEPRECATED(5)

- [x] **Step 5: 添加"最近告警"区域**

右侧区域（40%宽度）：
- 标题: "最近告警" + "查看全部"链接
- 3条最近告警（紧凑卡片）：
  - [CRITICAL] 推理延迟P99超阈值 — 10分钟前
  - [WARNING] GPU使用率 > 90% — 1小时前
  - [WARNING] 磁盘空间 < 20% — 3小时前

- [x] **Step 6: 添加"最近活动"时间线**

底部区域：
- 标题: "最近活动"
- 6条时间线：
  - 14:30 告警 肺结节检测-生产 推理延迟异常
  - 13:00 审批 张医生提交了 肺结节检测v2.3.1 上线审批
  - 11:30 部署 肺结节检测-灰度 v2.3.1 部署成功
  - 10:00 评估 v2.3.1 外部验证集评估完成 (AUC: 0.987)
  - 昨天 16:00 ETL 临床特征提取任务完成 (3,600条)
  - 昨天 14:00 模型 病理分类模型v3.0.0 注册成功

- [x] **Step 7: 添加"数据源状态"区域**

- 标题: "数据源连接状态"
- 4个数据源状态卡片（横排）：
  - HIS: 绿色圆点 + "运行中" + 最近同步 10分钟前
  - PACS: 绿色圆点 + "运行中" + 最近同步 5分钟前
  - LIS: 绿色圆点 + "运行中" + 最近同步 30分钟前
  - EMR: 黄色圆点 + "延迟" + 最近同步 2小时前

- [x] **Step 8: 截图验证**

截取完整 Dashboard，确认布局密度、信息层次、数据展示效果。

---

## 执行顺序总结

| Task | 页面 | 类型 |
|------|------|------|
| 1 | 登录页 (nR4Uw) | 页面细化 |
| 2 | Dashboard工作台 (QptC0) | 页面细化 |

**总计**: 2 Tasks, ~2 页面细化
