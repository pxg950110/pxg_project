# MAIDC 标注管理模块 UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在 Pencil 中完成标注管理模块的 UI 细化设计，包括 2 个现有页面细化、1 个详情页细化、1 个弹窗细化。标注模块需大幅扩展功能。

**Architecture:** 使用 Pencil MCP 工具操作 pencil-new.pen。按页面粒度分任务执行。

**设计规格:** `docs/superpowers/specs/2026-04-08-maidc-design.md` §5.2.5 (标注服务) + §4.4.10-11 (影像/文本标注表)

**Pencil 文件:** `E:/pxg_project/pencil-new.pen`

---

## Task 1: 细化标注任务列表页 (9EiHQ)

**Files:**
- Modify: `pencil-new.pen` → `9EiHQ`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `9EiHQ` readDepth 3。

- [x] **Step 2: 增加筛选栏**

- 标注类型下拉: 全部 | 影像标注 | 文本标注 | 基因组标注
- 标注格式下拉: 全部 | BBOX(矩形框) / SEGMENTATION(分割) / LANDMARK(关键点) / CLASSIFICATION(分类) / NER(命名实体) / RELATION(关系)
- 状态下拉: 全部 | 待分配 | 标注中 | 待审核 | 已完成 | 已终止
- 关键词搜索

- [x] **Step 3: 增加"AI预标注"按钮**

在"创建标注任务"按钮旁添加"AI预标注"按钮（紫色轮廓）。

- [x] **Step 4: 任务卡片增加统计信息**

每张任务卡片增加：
- 标注类型标签（影像=蓝，文本=紫，基因=绿）
- 标注格式标签
- 进度条：已标注/总数 + 百分比
- 标注人员头像组（最多显示3个头像+人数）
- 截止日期

- [x] **Step 5: 增加分页器**

- [x] **Step 6: 截图验证**

---

## Task 2: 细化标注任务详情页 (4Eqcu)

**Files:**
- Modify: `pencil-new.pen` → `4Eqcu`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `4Eqcu` readDepth 3。

- [x] **Step 2: 添加任务基本信息区**

- 任务名称 + 状态标签 + 标注类型标签
- 关联数据集 | 标注格式 | 标注人员数 | 截止日期 | 创建时间

- [x] **Step 3: 添加 Tab 栏**

5个 Tab: 标注进度 | 质量控制 | 标注人员 | 标注统计 | 操作日志

- [x] **Step 4: 创建"标注进度"Tab 内容**

- 总进度条：已完成 450/600 (75%)
- 按标注人员分组进度表格：
  - 表头: 标注员 | 已分配 | 已完成 | 进行中 | 待处理 | 完成率 | 操作
  - 5行数据
- 底部"分配标注"和"批量导出"按钮

- [x] **Step 5: 创建"质量控制"Tab 内容**

- 质量概览卡片：平均IoU | Cohen's Kappa | 一致性得分 | 驳回率
- 待审核列表：
  - 表头: 样本ID | 标注员A | 标注员B | IoU得分 | 状态 | 操作
  - 5行数据，IoU < 0.7 红色标记
- 底部"批量审核"按钮

- [x] **Step 6: 创建"标注人员"Tab 内容**

标注人员表格：
- 表头: 姓名 | 角色(标注员/审核员) | 已完成数 | 准确率 | 平均耗时 | 最后活跃 | 操作
- 6行数据
- "邀请标注员"按钮

- [x] **Step 7: 创建"标注统计"Tab 内容**

- 标注类型分布饼图占位区 (MetricChart, "标注类型分布")
- 标注员效率对比柱状图占位区 (MetricChart, "标注员效率对比")
- 每日标注量趋势折线图占位区 (MetricChart, "每日标注量趋势")

- [x] **Step 8: 创建"操作日志"Tab 内容**

时间线列表（5条操作记录）：
- 2026-04-09 张医生 完成了样本 #IMG_0345 的标注
- 2026-04-09 李医生 提交了样本 #IMG_0340 的标注
- 2026-04-08 王医生驳回了样本 #IMG_0321 的标注（IoU=0.62）
- 2026-04-08 AI预标注 完成了 50 个样本的自动标注
- 2026-04-07 管理员 创建了标注任务

- [x] **Step 9: 截图验证**

---

## Task 3: 细化创建标注任务弹窗 (BRY7a)

**Files:**
- Modify: `pencil-new.pen` → `BRY7a`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

- 任务名称 * (输入框)
- 关联数据集 * (下拉选择)
- 标注类型 * (下拉): 影像标注 / 文本标注 / 基因组标注
- 标注格式 * (下拉，根据类型联动):
  - 影像标注: BBOX / SEGMENTATION / LANDMARK / CLASSIFICATION
  - 文本标注: NER / RELATION / SENTIMENT / CLASSIFICATION
  - 基因组标注: VARIANT_CLASSIFICATION
- 标签列表 (可编辑的标签组):
  - 预设标签: nodule(结节), mass(肿块), effusion(积液), consolidation(实变)
  - [+ 添加标签] 按钮
- 分配标注员 (多选下拉)
- 分配审核员 (下拉)
- 截止日期 (日期选择器)
- AI预标注 (开关): 开启后显示 "预标注模型" 下拉
- 描述 (文本域)
- 底部: [取消] [创建任务]

- [x] **Step 3: 截图验证**

---

## Task 4: 新增 AI 预标注弹窗

**Files:**
- Create: `pencil-new.pen` → 新弹窗 frame

- [x] **Step 1: 找空位 + 创建弹窗框架**

520px 宽弹窗 + 深色遮罩背景。

- [x] **Step 2: 添加标题和表单字段**

标题: "AI 辅助预标注"
- 预标注模型 * (下拉): 肺结节检测v2.3.1 / 病理分类v3.0.0 / NLP实体识别v1.0.0
- 目标数据集 * (下拉)
- 置信度阈值 (滑块 0.0-1.0, 默认 0.7)
- 预估样本数: ~600
- 底部: [取消] [启动预标注]

- [x] **Step 3: 截图验证**

---

## Task 5: 新增标注审核弹窗

**Files:**
- Create: `pencil-new.pen` → 新弹窗 frame

- [x] **Step 1: 找空位 + 创建弹窗框架**

700px 宽弹窗。

- [x] **Step 2: 添加审核界面**

标题: "标注审核"
- 左侧: 图像/文本预览区（占位方框，标注"样本预览"）
- 右侧: 标注结果对比
  - 标注员A结果 (JsonViewer)
  - 标注员B结果 (JsonViewer)
  - IoU/一致性得分
- 底部: [驳回] (红色轮廓) [通过] (绿色 Primary)

- [x] **Step 3: 截图验证**

---

## 执行顺序总结

| Task | 页面/弹窗 | 类型 |
|------|----------|------|
| 1 | 标注任务列表 (9EiHQ) | 页面细化 |
| 2 | 标注任务详情 (4Eqcu) | 页面细化（大幅扩展） |
| 3 | 创建标注任务弹窗 (BRY7a) | 弹窗细化 |
| 4 | AI预标注弹窗 | 新增弹窗 |
| 5 | 标注审核弹窗 | 新增弹窗 |

**总计**: 5 Tasks, ~2 页面细化 + 3 弹窗
