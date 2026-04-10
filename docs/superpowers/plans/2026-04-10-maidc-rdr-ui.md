# MAIDC RDR 科研数据管理模块 UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在 Pencil 中完成 RDR 科研数据管理模块的 UI 细化设计，包括 4 个现有页面细化、0 个新页面、4 个弹窗细化

**Architecture:** 使用 Pencil MCP 工具操作 pencil-new.pen。按页面粒度分任务执行。每个 Task 完成后截图验证。

**设计规格:** `docs/superpowers/specs/2026-04-08-maidc-design.md` §4.4 (RDR数据表)

**Pencil 文件:** `E:/pxg_project/pencil-new.pen`

---

## Task 1: 细化研究项目列表页 (skaqI)

**Files:**
- Modify: `pencil-new.pen` → `skaqI`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `skaqI` readDepth 3，记录表格、卡片等节点 ID。

- [x] **Step 2: 增加筛选栏**

在标题下方添加筛选行：
- 研究类型下拉: 全部 | 临床试验 | 观察性研究 | 基因组研究 | AI研究
- 状态下拉: 全部 | 草稿 | 已批准 | 进行中 | 已暂停 | 已完成
- 关键词搜索框

- [x] **Step 3: 统一状态标签**

将项目状态替换为自定义状态标签（非 StatusBadge，研究项目有独立状态）：
- DRAFT → 草稿 (灰色 #8B95A5 / #F1F3F5)
- APPROVED → 已批准 (蓝色 #3B82F6 / #EFF6FF)
- ACTIVE → 进行中 (绿色 #10B981 / #ECFDF5)
- SUSPENDED → 已暂停 (黄色 #F59E0B / #FFFBEB)
- COMPLETED → 已完成 (紫色 #8B5CF6 / #F5F3FF)

- [x] **Step 4: 增加"创建项目"按钮**

在筛选栏右侧添加 Primary 按钮"创建项目"。

- [x] **Step 5: 增加分页器**

在表格下方添加分页器：左侧"共 X 个项目"，右侧页码导航。

- [x] **Step 6: 截图验证**

截取 `skaqI`，确认筛选栏、状态标签、分页器效果。

---

## Task 2: 细化研究项目详情页 (F6nGT)

**Files:**
- Modify: `pencil-new.pen` → `F6nGT`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `F6nGT` readDepth 3。

- [x] **Step 2: 添加项目基本信息卡片**

在页面顶部添加项目信息区：
- 项目名称 + 状态标签
- 项目编码 | 研究类型 | PI | 起止日期 | 伦理审批号
- 项目描述

- [x] **Step 3: 添加 Tab 栏**

添加 4 个 Tab：
- 研究队列 | 数据集 | 项目成员 | 项目动态

- [x] **Step 4: 创建"研究队列"Tab 内容**

- 队列列表卡片（3张）：
  - 队列1: "肺癌早筛队列" — 目标500人/当前423人 — ACTIVE(绿)
  - 队列2: "糖尿病视网膜队列" — 目标300人/当前156人 — ACTIVE(绿)
  - 队列3: "心电异常验证队列" — 目标200人/当前0人 — DRAFT(灰)
- 每张卡片显示：纳入/排除标准摘要、当前受试者数/目标数、状态、"管理受试者"按钮

- [x] **Step 5: 创建"数据集"Tab 内容**

- 数据集表格：
  - 表头: 数据集名称 | 类型 | 格式 | 记录数 | 大小 | 状态 | 操作
  - 4行数据:
    ```
    肺癌CT影像数据集v2 | IMAGING | DICOM | 1,200 | 45GB | PUBLISHED(绿) | 查看
    临床特征结构化数据 | STRUCTURED | CSV | 3,600 | 12MB | PUBLISHED(绿) | 查看
    基因变异VCF数据 | GENOMIC | VCF | 450 | 8.2GB | DRAFT(灰) | 编辑
    病理报告文本集 | TEXT | JSON | 890 | 156MB | DRAFT(灰) | 编辑
    ```

- [x] **Step 6: 创建"项目成员"Tab 内容**

成员表格：
- 表头: 姓名 | 角色 | 加入时间 | 状态 | 操作
- 5行数据:
  ```
  张三 | PI(主要研究者) | 2026-01-15 | ACTIVE | -
  李工 | CO_INVESTIGATOR(共同研究者) | 2026-01-20 | ACTIVE | 移除
  王医生 | RESEARCHER(研究员) | 2026-02-01 | ACTIVE | 移除
  赵博士 | DATA_MANAGER(数据管理员) | 2026-02-10 | ACTIVE | 移除
  陈工 | RESEARCHER(研究员) | 2026-03-01 | WITHDRAWN | -
  ```
- 右上角"邀请成员"按钮

- [x] **Step 7: 创建"项目动态"Tab 内容**

时间线列表（5条）：
- 2026-04-08 张三 发布了数据集 "肺癌CT影像数据集v2"
- 2026-04-05 李工 创建了队列 "心电异常验证队列"
- 2026-04-01 王医生 添加了 45 名受试者到 "肺癌早筛队列"
- 2026-03-15 赵博士 执行了 ETL 任务 "临床特征提取"
- 2026-03-01 张三 创建了研究项目

- [x] **Step 8: 截图验证**

截取 `F6nGT`，确认 Tab 内容效果。

---

## Task 3: 细化数据集管理页 (rX5KN)

**Files:**
- Modify: `pencil-new.pen` → `rX5KN`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `rX5KN` readDepth 3。

- [x] **Step 2: 增加筛选栏**

- 数据类型下拉: 全部 | 结构化 | 影像 | 基因组 | 文本 | 多模态
- 来源类型下拉: 全部 | CDR提取 | 上传 | 合成
- 状态下拉: 全部 | 草稿 | 已发布 | 已归档
- 关键词搜索

- [x] **Step 3: 增加排序和视图切换**

在搜索框旁添加：
- 排序下拉: "排序: 更新时间 ▼"
- 视图切换: 卡片视图 | 列表视图

- [x] **Step 4: 增加批量操作**

在筛选栏左侧添加：
- 全选复选框
- "批量下载" 按钮（蓝色轮廓）
- "批量归档" 按钮（灰色轮廓）

- [x] **Step 5: 增加分页器**

表格下方添加分页器。

- [x] **Step 6: 截图验证**

---

## Task 4: 细化数据集详情页 (aRSCp)

**Files:**
- Modify: `pencil-new.pen` → `aRSCp`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `aRSCp` readDepth 3。

- [x] **Step 2: 添加数据集基本信息区**

- 数据集名称 + 状态标签
- 数据集类型 | 数据格式 | 来源类型 | 记录数 | 文件大小 | 访问级别
- 描述

- [x] **Step 3: 添加 Tab 栏**

5个 Tab: 版本历史 | 数据预览 | 关联项目 | 访问日志 | 元数据

- [x] **Step 4: 创建"版本历史"Tab 内容**

版本列表：
- 表头: 版本号 | 变更说明 | 记录数 | 文件大小 | 校验和(SHA256) | 创建时间 | 操作
- 3行数据:
  ```
  v2.0 | 新增450条影像数据 | 1,200 | 45GB | a1b2c3... | 2026-04-08 | 下载
  v1.1 | 修正标注错误 | 750 | 28GB | d4e5f6... | 2026-03-15 | 下载
  v1.0 | 初始发布 | 750 | 28GB | g7h8i9... | 2026-02-20 | 下载
  ```

- [x] **Step 5: 创建"数据预览"Tab 内容**

- 数据表格预览（前10行）：
  - 影像数据集列: subject_code | modality | body_region | file_format | image_size | file_path
  - 结构化数据集列: subject_code | feature_code | feature_name | feature_value | value_type | unit
- 底部: "显示完整数据需要申请访问权限" 提示 + "申请访问" 按钮

- [x] **Step 6: 创建"访问日志"Tab 内容**

访问日志表格：
- 表头: 用户 | 访问类型 | 用途 | IP地址 | 时间
- 5行数据（VIEW/DOWNLOAD/API_CALL 混合）

- [x] **Step 7: 截图验证**

---

## Task 5: 细化 ETL 任务页 (fstSG)

**Files:**
- Modify: `pencil-new.pen` → `fstSG`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `fstSG` readDepth 3。

- [x] **Step 2: 增加筛选栏**

- 状态下拉: 全部 | 草稿 | 运行中 | 成功 | 失败 | 已暂停
- 调度类型下拉: 全部 | 手动 | 定时 | 实时
- 目标数据集下拉
- 关键词搜索

- [x] **Step 3: 任务表格增加列**

在现有列后增加：
- 调度类型列（标签样式：手动/定时/实时）
- 源类型列（CDR/FILE/EXTERNAL_API 标签）
- 执行进度列（进度条 + 百分比）

- [x] **Step 4: 增加任务状态统计卡片**

在筛选栏下方添加 4 个 MetricCard：
- 运行中: 3 | 成功(今日): 12 | 失败(今日): 1 | 总任务数: 45

- [x] **Step 5: 增加分页器**

- [x] **Step 6: 截图验证**

---

## Task 6: 细化数据质量页 (snuTn)

**Files:**
- Modify: `pencil-new.pen` → `snuTn`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `snuTn` readDepth 3。

- [x] **Step 2: 添加质量概览卡片行**

在标题下方添加 4 个 MetricCard：
- 规则总数: 24 | 通过率: 94.5% | 警告规则: 8 | 错误规则: 3

- [x] **Step 3: 添加质量规则表格**

表头: 规则名称 | 规则类型 | 目标表 | 目标列 | 合格阈值 | 严重度 | 启用状态 | 操作

规则类型标签配色：
- COMPLETENESS(完整性) → 蓝色
- ACCURACY(准确性) → 绿色
- CONSISTENCY(一致性) → 紫色
- TIMELINESS(时效性) → 橙色
- UNIQUENESS(唯一性) → 灰色

6行示例数据。

- [x] **Step 4: 添加最近检测结果区域**

在规则表格下方添加"最近检测结果"卡片：
- 检测时间 | 规则名 | 总数 | 通过数 | 失败数 | 通过率 | 是否达标
- 5行最近数据，达标(绿) / 未达标(红) 标记

- [x] **Step 5: 增加"新建规则"和"批量检测"按钮**

右上角操作按钮组。

- [x] **Step 6: 截图验证**

---

## Task 7: 细化创建项目弹窗 (d8ttO)

**Files:**
- Modify: `pencil-new.pen` → `d8ttO`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

在现有字段基础上补充：
- 研究类型 * (下拉): 临床试验 / 观察性研究 / 基因组研究 / AI研究
- 起止日期范围选择器
- 伦理审批号（可选）
- 资金来源（可选）

- [x] **Step 3: 截图验证**

---

## Task 8: 细化新建数据集弹窗 (r1HaY)

**Files:**
- Modify: `pencil-new.pen` → `r1HaY`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

- 关联研究项目 * (下拉选择)
- 数据集类型 * (下拉): 结构化 / 影像 / 基因组 / 文本 / 多模态
- 来源类型 * (下拉): CDR提取 / 上传 / 合成
- 数据格式 (下拉，根据类型联动): CSV / PARQUET / DICOM / VCF / JSON
- 访问级别 * (下拉): PRIVATE / PROJECT / PUBLIC
- 描述 (文本域)

- [x] **Step 3: 截图验证**

---

## Task 9: 细化创建ETL任务弹窗 (FwZp1)

**Files:**
- Modify: `pencil-new.pen` → `FwZp1`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

- 任务名称 * (输入框)
- 关联研究项目 * (下拉)
- 目标数据集 * (下拉)
- 源类型 * (下拉): CDR / 文件 / 外部API
- ETL配置 (JsonViewer 区域，预填示例映射规则 JSON)
- 调度类型 * (下拉): 手动 / 定时 / 实时
- Cron表达式 (仅定时类型显示)
- 底部: [取消] [创建任务]

- [x] **Step 3: 截图验证**

---

## Task 10: 细化创建质量规则弹窗 (a420R)

**Files:**
- Modify: `pencil-new.pen` → `a420R`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

- 规则名称 * (输入框)
- 规则类型 * (下拉): 完整性 / 准确性 / 一致性 / 时效性 / 唯一性
- 目标表 * (下拉，RDR 表名列表)
- 目标列 (下拉)
- 规则表达式 * (文本域)
- 合格阈值 * (数字输入，默认 100，单位%)
- 严重度 * (下拉): ERROR / WARNING / INFO
- 启用状态 (开关，默认开启)
- 底部: [取消] [保存规则]

- [x] **Step 3: 截图验证**

---

## 执行顺序总结

| Task | 页面/弹窗 | 类型 |
|------|----------|------|
| 1 | 研究项目列表 (skaqI) | 现有页面细化 |
| 2 | 研究项目详情 (F6nGT) | 现有页面细化 |
| 3 | 数据集管理 (rX5KN) | 现有页面细化 |
| 4 | 数据集详情 (aRSCp) | 现有页面细化 |
| 5 | ETL任务 (fstSG) | 现有页面细化 |
| 6 | 数据质量 (snuTn) | 现有页面细化 |
| 7 | 创建项目弹窗 (d8ttO) | 弹窗细化 |
| 8 | 新建数据集弹窗 (r1HaY) | 弹窗细化 |
| 9 | 创建ETL任务弹窗 (FwZp1) | 弹窗细化 |
| 10 | 创建质量规则弹窗 (a420R) | 弹窗细化 |

**总计**: 10 Tasks, ~6 页面细化 + 4 弹窗细化
