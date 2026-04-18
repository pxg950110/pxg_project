# MAIDC 模型管理模块 UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Pencil 中完成 MAIDC 模型管理模块的 UI 细化设计，包括组件库升级、10个现有页面细化、3个新页面、6个弹窗细化

> **状态: ✅ 全部完成** (2026-04-09)

**Architecture:** 使用 Pencil MCP 工具操作 pencil-new.pen 文件。每个 Task 完成一个页面的设计变更，通过截图验证设计质量。遵循现有 PageShell 框架和组件体系。

**Tech Stack:** Pencil MCP (batch_design, batch_get, get_screenshot), 现有设计令牌体系

**设计规格:** `docs/superpowers/specs/2026-04-09-model-management-ui-design.md`

**Pencil 文件:** `E:/pxg_project/pencil-new.pen`

---

## Phase 0: 组件库升级

### Task 1: 创建 StatusBadge 组件集 ✅

**Files:**
- Modify: `pencil-new.pen` → `z0CUo` (组件库 frame)

**目标:** 在组件库中创建 5 组 StatusBadge 组件，供所有页面使用

- [ ] **Step 1: 读取组件库当前状态**

使用 batch_get 读取 `z0CUo` 的子节点结构，确认现有组件位置和间距。

- [ ] **Step 2: 创建 StatusBadge/Model 组件（4个状态）**

使用 batch_design 在组件库区域创建 4 个 StatusBadge：

| 变体 | 文字 | 文字色 | 背景色 |
|------|------|--------|--------|
| DRAFT | 草稿 | #8B95A5 | #F1F3F5 |
| REGISTERED | 已注册 | #3B82F6 | #EFF6FF |
| PUBLISHED | 已发布 | #10B981 | #ECFDF5 |
| DEPRECATED | 已弃用 | #F59E0B | #FFFBEB |

每个 badge：padding [2, 8], cornerRadius $--radius-sm, fontSize 12, fontWeight 500, reusable=true

- [ ] **Step 3: 创建 StatusBadge/Version 组件（6个状态）**

| 变体 | 文字 | 文字色 | 背景色 |
|------|------|--------|--------|
| CREATED | 已创建 | #8B95A5 | #F1F3F5 |
| TRAINING | 训练中 | #3B82F6 | #EFF6FF |
| EVALUATING | 评估中 | #F59E0B | #FFFBEB |
| APPROVED | 已审批 | #10B981 | #ECFDF5 |
| DEPLOYED | 已部署 | #8B5CF6 | #F5F3FF |
| DEPRECATED | 已弃用 | #8B95A5 | #F1F3F5 |

- [ ] **Step 4: 创建 StatusBadge/Deployment 组件（5个状态）**

| 变体 | 文字 | 文字色 | 背景色 |
|------|------|--------|--------|
| CREATING | 创建中 | #3B82F6 | #EFF6FF |
| RUNNING | 运行中 | #10B981 | #ECFDF5 |
| STOPPING | 停止中 | #F59E0B | #FFFBEB |
| STOPPED | 已停止 | #8B95A5 | #F1F3F5 |
| FAILED | 失败 | #EF4444 | #FEF2F2 |

- [ ] **Step 5: 创建 StatusBadge/Eval 组件（4个状态）**

| 变体 | 文字 | 文字色 | 背景色 |
|------|------|--------|--------|
| PENDING | 待评估 | #8B95A5 | #F1F3F5 |
| RUNNING | 评估中 | #3B82F6 | #EFF6FF |
| COMPLETED | 已完成 | #10B981 | #ECFDF5 |
| FAILED | 失败 | #EF4444 | #FEF2F2 |

- [ ] **Step 6: 创建 StatusBadge/Approval 组件（3个状态）**

| 变体 | 文字 | 文字色 | 背景色 |
|------|------|--------|--------|
| PENDING | 审批中 | #F59E0B | #FFFBEB |
| APPROVED | 已通过 | #10B981 | #ECFDF5 |
| REJECTED | 已驳回 | #EF4444 | #FEF2F2 |

- [ ] **Step 7: 截图验证组件库**

使用 get_screenshot 截取 `z0CUo`，验证所有新组件的对齐、间距、配色。

---

### Task 2: 创建通用组件（EmptyState, FileUploader, MetricChart, JsonViewer, KeyValueEditor） ✅

**Files:**
- Modify: `pencil-new.pen` → `z0CUo` (组件库 frame)

- [ ] **Step 1: 创建 EmptyState 组件**

灰色图标 + "暂无数据" 标题 + 引导文案 + 操作按钮
- 可复用 frame, 200px 宽, 垂直居中布局
- 图标: inbox (lucide), 48px, #D1D5DB
- 标题: "暂无数据", 14px, #6B7280
- 描述: "点击按钮创建第一个项目", 12px, #9CA3AF
- 操作按钮: Button/Primary 引用

- [ ] **Step 2: 创建 FileUploader 组件**

虚线边框上传区域 + 文件图标 + 提示文字
- 240px 宽, 120px 高
- 虚线边框: stroke #D1D5DB, dash [6,4], thickness 2
- 图标: upload-cloud (lucide), 24px, #9CA3AF
- 主文字: "点击或拖拽上传", 13px, #6B7280
- 辅文字: "支持 .pt / .onnx / .pkl", 12px, #9CA3AF

- [ ] **Step 3: 创建 MetricChart 组件**

图表占位区域（灰色背景 + 标注文字）
- 填充父容器宽度, 高 200px
- 背景: #F9FAFB
- 边框: 1px #E5E7EB
- 居中文字: "图表区域 - {type}", 13px, #9CA3AF
- 可配置 type 参数: "折线图" / "柱状图" / "ROC曲线" / "混淆矩阵"

- [ ] **Step 4: 创建 JsonViewer 组件**

代码风格 JSON 展示区域
- 深色背景: #1E293B, 圆角 8px, padding 16px
- 等宽字体: monospace, 12px
- 示例内容展示语法高亮效果（key: 蓝色, string: 绿色, number: 橙色）

- [ ] **Step 5: 创建 KeyValueEditor 组件**

键值对编辑行
- 水平布局: [key输入框] [=] [value输入框] [删除按钮]
- 可复用 frame
- 下方 [+ 添加] 链接

- [ ] **Step 6: 截图验证新组件**

截取组件库区域，验证所有新组件的视觉效果。

---

## Phase 1: 模型管理现有页面细化

### Task 3: 细化 03-模型列表 (Syc8a) ✅

**Files:**
- Modify: `pencil-new.pen` → `Syc8a`

- [ ] **Step 1: 读取当前页面结构**

batch_get 读取 `Syc8a` readDepth 3，记录筛选栏、卡片网格的节点 ID。

- [ ] **Step 2: 筛选栏增加"基因组"分类**

在现有 5 个筛选 Tab（全部/影像/NLP/结构化/多模态）后增加第 6 个"基因组" Tab。

- [ ] **Step 3: 增加排序下拉和视图切换**

在搜索框和"注册模型"按钮之间添加：
- 排序下拉: "排序: 更新时间 ▼"，240px 宽
- 视图切换: 两个图标按钮（卡片图标高亮 + 列表图标灰色）

- [ ] **Step 4: 增加分页器**

在卡片网格下方添加分页器：
- 左侧: "共 28 个模型"
- 右侧: 页码按钮 1(蓝) 2 3 ... 3 + 每页选择 "12/页 ▼"

- [ ] **Step 5: 统一状态标签**

将 6 张卡片中的状态标签替换为 StatusBadge/Model 组件引用：
- 已发布 → PUBLISHED (绿色)
- 草稿 → DRAFT (灰色)
- 评估中 → REGISTERED (蓝色)
- 训练中 → REGISTERED (蓝色)

- [ ] **Step 6: 截图验证**

截取 `Syc8a`，确认筛选栏、排序、分页器、状态标签效果。

---

### Task 4: 细化 04-模型详情 — 补充版本列表 Tab (7xGUL) ✅

**Files:**
- Modify: `pencil-new.pen` → `7xGUL`

- [ ] **Step 1: 读取当前 Tab 结构**

batch_get 读取 `7xGUL` readDepth 3，确认 Tab 栏和内容区节点 ID。

- [ ] **Step 2: 创建版本列表 Tab 内容面板**

在 Tab 内容区域添加"版本列表"面板（当版本列表 Tab 激活时显示）：
- 版本表格 (Table 组件引用):
  - 表头: 版本号 | 描述 | 框架版本 | 文件大小 | 训练指标 | 状态 | 创建时间 | 操作
  - 5行数据（对齐统一版本号）:
    ```
    v2.3.1 | 新增3D卷积+注意力机制 | PyTorch 2.1 | 520MB | AUC:0.983 | APPROVED(绿) | 2026-04-07 | [评估][部署]
    v2.2.0 | 优化小结节检测 | PyTorch 2.0 | 500MB | AUC:0.923 | DEPLOYED(紫) | 2026-03-15 | [查看部署]
    v2.1.0 | 多尺度特征融合 | PyTorch 2.0 | 490MB | AUC:0.908 | DEPLOYED(紫) | 2026-02-20 | [查看部署]
    v2.0.0 | 架构升级ResNet50 | PyTorch 1.13 | 480MB | AUC:0.895 | DEPRECATED(灰) | 2026-01-10 | [查看]
    v1.0.0 | 初始版本 | PyTorch 1.13 | 475MB | AUC:0.882 | DEPRECATED(灰) | 2025-11-01 | [查看]
    ```
- 版本对比区: 两个下拉选择器 (v2.3.1 ▼ / v2.2.0 ▼) + "对比"按钮
- 对比结果表格: 指标名 | v2.3.1 | v2.2.0 (行: AUC, Accuracy, Recall, Precision, F1, 参数量, 文件大小)

- [ ] **Step 3: 截图验证**

---

### Task 5: 细化 04-模型详情 — 补充评估记录 Tab (7xGUL) ✅

**Files:**
- Modify: `pencil-new.pen` → `7xGUL`

- [ ] **Step 1: 创建评估记录 Tab 内容面板**

评估卡片列表（3张）：
- 卡片1: "v2.3.1 外部验证集评估" — COMPLETED(绿) — 外部验证(紫标签) — CT肺结节数据集v2 — AUC:0.987, Acc:96.8% — 耗时 45min — [查看报告]
- 卡片2: "v2.3.1 内部回归测试" — COMPLETED(绿) — 内部评估(蓝标签) — 训练集分割(20%) — AUC:0.983 — 耗时 22min — [查看报告]
- 卡片3: "v2.2.0 性能基准测试" — COMPLETED(绿) — 内部评估(蓝标签) — CT肺结节数据集v1 — AUC:0.923 — 耗时 38min — [查看报告]

- [ ] **Step 2: 截图验证**

---

### Task 6: 细化 04-模型详情 — 补充部署管理 Tab (7xGUL) ✅

**Files:**
- Modify: `pencil-new.pen` → `7xGUL`

- [ ] **Step 1: 创建部署管理 Tab 内容面板**

- 右上角"新建部署"按钮
- 部署表格：
  - 表头: 部署名称 | 版本 | 类型 | 框架 | 状态 | QPS | 延迟 | 操作
  - 3行数据:
    ```
    肺结节检测-生产 | v2.1.0 | ONLINE | TRITON | RUNNING(绿) | 56 | 23ms | 管理
    肺结节检测-灰度 | v2.3.1 | ONLINE | TRITON | RUNNING(绿) | 5  | 25ms | 管理
    肺结节检测-测试 | v2.3.1 | ONLINE | FASTAPI | STOPPED(灰) | - | - | 管理
    ```

- [ ] **Step 2: 截图验证**

---

### Task 7: 细化 05-部署监控 (l2c3F) ✅

**Files:**
- Modify: `pencil-new.pen` → `l2c3F`

- [ ] **Step 1: 增加时间范围选择器和自动刷新**

在指标卡片行上方或图表标题旁添加：
- 时间范围 Tab: 近1h | 近6h | 近24h(高亮) | 近7d
- 自动刷新: 绿色圆点 + "自动刷新 30s"

- [ ] **Step 2: QPS图表增加多指标 Tab**

在图表标题"推理QPS趋势"旁添加 Tab 切换：
- QPS(高亮) | 延迟P50/P99 | GPU利用率 | 错误率

- [ ] **Step 3: 告警表格增加"确认"操作按钮**

在 FIRING 状态行的操作列添加"确认"按钮（蓝色文字链接）。

- [ ] **Step 4: 截图验证**

---

### Task 8: 细化 05a-部署详情 (upipB) ✅

**Files:**
- Modify: `pencil-new.pen` → `upipB`

- [ ] **Step 1: 实例表格增加列**

在现有 5 列后增加 3 列：
- 运行时长 | 错误数(24h) | 内存使用
- 示例数据: 45h12m | 2 | 12.3/16GB

- [ ] **Step 2: 增加操作按钮**

在操作栏添加:
- "推理日志" 按钮（蓝色轮廓）
- "重启" 按钮（黄色轮廓）

- [ ] **Step 3: 部署配置卡片增加资源使用进度条**

在配置卡片下方添加 3 个进度条：
- CPU: 3.2/4 核 (80%) — 蓝色进度条
- GPU: 7.2/8 GB (90%) — 橙色进度条
- 内存: 12/16 GB (75%) — 绿色进度条

- [ ] **Step 4: 截图验证**

---

### Task 9: 细化 06-版本管理 (ybnMi) ✅

**Files:**
- Modify: `pencil-new.pen` → `ybnMi`

- [ ] **Step 1: 修复版本号一致性**

将版本表格中的版本号统一为：
- v2.3.1 / v2.2.0 / v2.1.0 / v2.0.0 / v1.0.0

同步更新对应的状态标签和描述。

- [ ] **Step 2: 补充版本对比详情**

在版本对比区添加具体指标对比行表格：
- 超参数对比: learning_rate | batch_size | epochs | optimizer
- 训练指标: loss | accuracy | val_loss
- 评估指标: auc | f1 | precision | recall

- [ ] **Step 3: 操作列按状态显示不同按钮**

更新每行的操作按钮：
- APPROVED 行: [部署] (Primary) [查看] (Outline)
- DEPLOYED 行: [查看部署] (Outline)
- DEPRECATED 行: [查看] (Outline)

- [ ] **Step 4: 截图验证**

---

### Task 10: 细化 07-模型评估 (NyVaz) ✅

**Files:**
- Modify: `pencil-new.pen` → `NyVaz`

- [ ] **Step 1: 增加筛选栏**

在标题下方添加筛选行：
- 评估类型下拉: 全部 | 内部评估 | 外部验证 | 交叉验证
- 状态下拉: 全部 | 进行中 | 已完成 | 已失败
- 关键词搜索框

- [ ] **Step 2: 评估卡片增加类型标签**

在每张卡片标题行增加评估类型标签：
- 内部评估 → 蓝色标签
- 外部验证 → 紫色标签
- 交叉验证 → 橙色标签

- [ ] **Step 3: 评估详情面板增加 ROC 曲线占位**

在展开的详情面板的混淆矩阵旁边增加 MetricChart 占位区：
- 标注 "ROC曲线"
- 灰色方框 260x200px

- [ ] **Step 4: 增加"导出报告"按钮**

在已完成卡片右上角增加"导出报告"按钮（下载图标 + 文字）。

- [ ] **Step 5: 截图验证**

---

### Task 11: 细化 07a-评估详情 (gBEhi) ✅

**Files:**
- Modify: `pencil-new.pen` → `gBEhi`

- [ ] **Step 1: 增加 ROC 曲线区域**

在左侧指标卡片下方添加 MetricChart 组件：
- 尺寸: 填充宽度 x 240px
- 标注: "ROC曲线 (AUC=0.987)"
- X轴标签: 1-Specificity, Y轴标签: Sensitivity

- [ ] **Step 2: 增加数据集信息卡片**

在评估配置卡片中增加：
- 数据集名称: CT肺结节数据集v2
- 样本数量: 1,200 例
- 数据集版本: v2.1

- [ ] **Step 3: 增加"下载报告"按钮**

在右上角操作区添加"下载报告"按钮（Primary 轮廓）。

- [ ] **Step 4: 指标卡片增加趋势指示**

在每个指标卡片数值后添加趋势箭头：
- 准确率 96.5% ↑0.3% (绿色 ↑)
- 灵敏度 94.2% ↓0.1% (红色 ↓)
- 精确率 93.8% ↑0.5% (绿色 ↑)
- F1 0.94 ↑0.2% (绿色 ↑)

- [ ] **Step 5: 截图验证**

---

### Task 12: 细化 08-审批流程 (DpQ4p) — 大幅补充 ✅

**Files:**
- Modify: `pencil-new.pen` → `DpQ4p`

- [ ] **Step 1: 创建"待审批" Tab 内容**

在 Tab 栏下方添加审批请求表格：
- 右上角: "待审批 5 项" 计数
- 表格列: 模型名称 | 版本 | 审批类型 | 申请人 | 提交时间 | 状态 | 操作
- 5行数据:
  ```
  肺结节检测 | v2.3.1 | 上线审批 | 张医生 | 2026-04-09 10:00 | PENDING(黄) | 审批
  病理分类 | v3.0.0 | 发布审批 | 李工 | 2026-04-08 16:30 | PENDING(黄) | 审批
  心电异常 | v1.5.1 | 临床使用审批 | 王医生 | 2026-04-08 09:15 | PENDING(黄) | 审批
  NLP实体 | v1.0.0 | 上线审批 | 赵工 | 2026-04-07 14:00 | PENDING(黄) | 审批
  基因分类 | v3.1.0 | 发布审批 | 陈博士 | 2026-04-06 11:30 | PENDING(黄) | 审批
  ```
- 审批类型标签配色: 上线审批(蓝) / 发布审批(紫) / 临床使用(橙)

- [ ] **Step 2: 截图验证**

---

### Task 13: 细化 08a-审批详情 (zBiWN) ✅

**Files:**
- Modify: `pencil-new.pen` → `zBiWN`

- [ ] **Step 1: 补全时间线 4 个步骤**

在时间线区域更新 4 个步骤节点：
- Step 1: 技术评审 — 王工 — 2026-04-07 11:00 — APPROVED(绿) — "模型性能达标，技术架构合理"
- Step 2: 临床评审 — 刘主任 — 2026-04-07 14:30 — APPROVED(绿) — "临床验证结果良好，建议上线"
- Step 3: 管理审批 — 李主任 — 进行中 — PENDING(黄) — (等待审批)
- Step 4: 完成 — — — 待定(灰色)

- [ ] **Step 2: 增加"审批材料"卡片**

在关联模型卡片下方添加"审批材料"卡片：
- 标题: "审批材料"
- 文件列表:
  - 📄 评估报告 v2.3.1 (EVALUATION 蓝标签) [下载]
  - 📄 临床验证报告 (CLINICAL 绿标签) [下载]

- [ ] **Step 3: 增加"风险评估"区域**

在审批材料下方添加：
- 风险评估: "模型为辅助诊断用途，最终决策由临床医生做出。已通过等保三级安全评估。"
- 临床验证描述: "已在3家医院完成外部验证，样本量 3,600 例，AUC > 0.97。"

- [ ] **Step 4: 截图验证**

---

### Task 14: 细化 09-流量路由 (eEgv3) ✅

**Files:**
- Modify: `pencil-new.pen` → `eEgv3`

- [ ] **Step 1: 展开详情增加变体指标对比**

在金丝雀发布展开详情中添加"各变体实时指标"表格：
- 表头: 部署名 | QPS | 延迟P99 | 错误率 | 请求占比
- 行1: 生产 v2.1.0 | 50.4 | 22ms | 0.1% | 90%
- 行2: 灰度 v2.3.1 | 5.6 | 25ms | 0.2% | 10%

- [ ] **Step 2: 增加"路由历史"区域**

在变体指标下方添加最近变更记录：
- 标题: "路由历史"
- 列表:
  - 2026-04-08 14:30 张医生 调整金丝雀比例 5% → 10%
  - 2026-04-07 10:00 张医生 创建金丝雀路由
  - 2026-04-05 16:00 李工 更新默认部署 v2.0.0 → v2.1.0

- [ ] **Step 3: 禁用状态卡片降低透明度**

将"糖尿病预测-加权路由"卡片（禁用状态）的整体不透明度设为 60%。

- [ ] **Step 4: 截图验证**

---

## Phase 2: 弹窗细化

### Task 15: 细化注册模型弹窗 (cGet2) — 改为多步向导 ✅

**Files:**
- Modify: `pencil-new.pen` → `cGet2`

- [ ] **Step 1: 改造为 3 步向导弹窗**

在弹窗顶部添加 StepIndicator：
- Step 1: 基本信息 (高亮)
- Step 2: Schema定义
- Step 3: 补充信息

- [ ] **Step 2: Step 1 — 基本信息**

保持现有字段：模型名称 / 模型编码+类型 / 框架+任务类型 / 模型描述
底部改为: [取消] [下一步 →]

- [ ] **Step 3: Step 2 — Schema定义**

添加两个 JsonViewer 区域：
- Input Schema 标签 + JsonViewer (预填示例 JSON)
- Output Schema 标签 + JsonViewer (预填示例 JSON)
底部: [← 上一步] [下一步 →]

- [ ] **Step 4: Step 3 — 补充信息**

- 标签输入框 (TagInput): "影像, CT, 肺结节"
- 所属项目下拉: "关联研究项目（可选）"
- 许可证输入: "MIT"
底部: [← 上一步] [确认注册]

- [ ] **Step 5: 截图验证**

---

### Task 16: 细化上传版本弹窗 (VQF32) ✅

**Files:**
- Modify: `pencil-new.pen` → `VQF32`

- [ ] **Step 1: 增加 hyper_params 区域**

在变更说明下方添加 KeyValueEditor 组件：
- 标签: "超参数 (可选)"
- 预填 4 行: learning_rate=0.001, batch_size=32, epochs=100, optimizer=Adam
- [+ 添加参数] 链接

- [ ] **Step 2: 增加训练数据集选择**

在 hyper_params 下方添加：
- 标签: "训练数据集"
- 下拉选择: "选择关联训练数据集 ▼"

- [ ] **Step 3: 截图验证**

---

### Task 17: 细化新建评估弹窗 (KsKs6) ✅

**Files:**
- Modify: `pencil-new.pen` → `KsKs6`

- [ ] **Step 1: 增加 eval_type 下拉**

在目标模型/版本行下方添加：
- 评估类型 * 下拉: 内部评估 / 外部验证 / 交叉验证

- [ ] **Step 2: 增加 metrics_config 复选框组**

替换现有"评估指标"下拉为复选框组：
- [✓] Accuracy [✓] Recall [✓] Precision [✓] F1 [✓] AUC [✓] Sensitivity [✓] Specificity

- [ ] **Step 3: 增加阈值输入**

添加两个数字输入：
- 置信度阈值: [0.5]
- IoU 阈值: [0.5] + 标注 "(仅目标检测/分割任务)"

- [ ] **Step 4: 截图验证**

---

### Task 18: 细化新建部署弹窗 (kH2Qv) ✅

**Files:**
- Modify: `pencil-new.pen` → `kH2Qv`

- [ ] **Step 1: 增加 serving_framework 下拉**

在模型版本下方添加：
- 推理框架 * 下拉: TRITON / TORCHSERVE / TFSERVING / ONNX_RUNTIME / FASTAPI

- [ ] **Step 2: 增加 env_vars 编辑器**

在资源配额下方添加 KeyValueEditor：
- 标签: "环境变量"
- 预填: MODEL_PATH=/models/lung-nodule

- [ ] **Step 3: 增加 auto_scale 开关**

在 env_vars 下方添加：
- 自动扩缩容: [开关 关闭]
- 开启后展开: 最小副本数 [1] / 最大副本数 [3]

- [ ] **Step 4: 截图验证**

---

## Phase 3: 新增页面

### Task 19: 创建推理日志页 ✅

**Files:**
- Create: `pencil-new.pen` → 新 frame

- [ ] **Step 1: 确定画布位置**

使用 find_empty_space_on_canvas 找到合适位置（建议在部署详情右侧）。

- [ ] **Step 2: 创建页面框架**

使用 PageShell 组件引用创建 1440x900 页面，侧边栏高亮"模型管理"。

- [ ] **Step 3: 添加标题栏**

- ← 推理日志 · 肺结节检测-生产
- 副标题: 部署编号: DEP-2026-0008

- [ ] **Step 4: 添加筛选栏**

时间范围 [近1h ▼] + 状态 [全部 ▼] + [查询] [重置]

- [ ] **Step 5: 添加统计卡片**

4个 MetricCard: 总请求(12,456) / 成功率(99.7%) / 平均延迟(23ms) / 超时(3)

- [ ] **Step 6: 添加日志表格**

表头: 请求ID | 患者ID | 置信度 | 延迟 | 版本 | 调用方 | 状态 | 时间
5行示例数据（混合 SUCCESS/ERROR/TIMEOUT 状态）

- [ ] **Step 7: 添加展开行区域**

在第一行下方展示展开行：
- 左: 输入摘要 (JsonViewer)
- 右: 推理结果 (JsonViewer)

- [ ] **Step 8: 添加分页器**

"共 12,456 条" + 页码导航

- [ ] **Step 9: 截图验证**

---

### Task 20: 创建 AI Worker 管理页 ✅

**Files:**
- Create: `pencil-new.pen` → 新 frame

- [ ] **Step 1: 找空位 + 创建页面框架**

- [ ] **Step 2: 添加标题栏**

AI Worker 集群管理 / 管理 GPU 推理集群的 Worker 节点和任务队列

- [ ] **Step 3: 添加资源总览卡片**

4个 MetricCard: 活跃节点(4/6) / GPU总数(8) / GPU使用率(67%) / 运行任务(12)

- [ ] **Step 4: 添加 Worker 节点表格**

表头: 节点名 | GPU型号 | GPU使用 | 内存使用 | 运行任务 | 状态 | 操作
6行数据（4 运行中 + 2 离线）

- [ ] **Step 5: 添加任务队列状态表格**

表头: 队列名 | 类型 | 待处理 | 运行中 | 优先级
4行: inference / evaluation / preprocessing / batch_inference

- [ ] **Step 6: 截图验证**

---

### Task 21: 创建路由弹窗 ✅

**Files:**
- Create: `pencil-new.pen` → 新 frame (Modal)

- [ ] **Step 1: 创建弹窗框架**

560px 宽弹窗 + 深色遮罩背景 + 居中定位

- [ ] **Step 2: 添加标题和表单字段**

标题: "新建路由规则"
字段: 路由名称 * / 目标模型 * / 路由类型 * (金丝雀/A-B/加权/镜像)

- [ ] **Step 3: 添加流量配置区域**

分割线 + "流量配置" 标题:
- 默认部署 * 下拉
- 灰度部署 * 下拉
- 金丝雀流量比例 [10] %
- 成功率阈值 [95] %
- 自动提升 [关闭] 开关

- [ ] **Step 4: 添加高级配置和底部按钮**

- 持续观察时间 [300] 秒
- 回退阈值 [90] %
- 底部: [取消] [创建路由]

- [ ] **Step 5: 截图验证**

---

## Phase 4: 版本号一致性修正

### Task 22: 全局版本号一致性修正 ✅

**Files:**
- Modify: 所有涉及肺结节检测模型的页面

- [ ] **Step 1: 搜索所有包含版本号的文本节点**

搜索所有包含 v1.0.0 ~ v3.x.x 版本号引用的节点。

- [ ] **Step 2: 逐页修正版本号**

确保以下页面使用统一版本序列：
- 03-模型列表 (Syc8a): 卡片显示 v2.1.0
- 04-模型详情 (7xGUL): 最新版本 v2.3.1
- 05-部署监控 (l2c3F): 生产 v2.1.0, 灰度 v2.3.1
- 05a-部署详情 (upipB): v2.1.0
- 06-版本管理 (ybnMi): v1.0.0 ~ v2.3.1 完整序列
- 07-模型评估 (NyVaz): 评估关联 v2.3.1
- 07a-评估详情 (gBEhi): v2.3.1
- 08-审批详情 (zBiWN): v2.3.1
- 09-流量路由 (eEgv3): 默认 v2.1.0, 金丝雀 v2.3.1

- [ ] **Step 3: 全局截图抽查**

随机抽取 3-4 个页面截图，确认版本号一致性。

---

## 执行顺序总结

| Phase | Tasks | 预计操作数 |
|-------|-------|-----------|
| Phase 0: 组件库 | Task 1-2 | ~50 batch_design ops |
| Phase 1: 现有页面细化 | Task 3-14 | ~80 batch_design ops |
| Phase 2: 弹窗细化 | Task 15-18 | ~30 batch_design ops |
| Phase 3: 新增页面 | Task 19-21 | ~60 batch_design ops |
| Phase 4: 一致性修正 | Task 22 | ~20 batch_design ops |

**总计**: 22 Tasks, ~240 batch_design operations
