# MAIDC 任务调度+审计+告警+消息模块 UI 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在 Pencil 中完成任务调度、审计日志、告警中心、消息中心 4 个模块的 UI 细化设计，包括 8 个页面细化、3 个弹窗细化

**Architecture:** 使用 Pencil MCP 工具操作 pencil-new.pen。按模块分 Task 执行。

**设计规格:** `docs/superpowers/specs/2026-04-08-maidc-design.md` §5.2.4 (任务调度) / §5.2.6 (审计) / §5.2.7 (消息) / §4.5.9-10 (告警)

**Pencil 文件:** `E:/pxg_project/pencil-new.pen`

---

## 模块 A: 任务调度

### Task 1: 细化任务调度页 (Ujek8)

**Files:**
- Modify: `pencil-new.pen` → `Ujek8`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `Ujek8` readDepth 3。

- [x] **Step 2: 增加筛选栏**

- 任务状态下拉: 全部 | 运行中 | 已完成 | 失败 | 已暂停
- 调度类型下拉: 全部 | Cron定时 | 手动触发 | 一次性
- 关联服务下拉: 全部 | 数据同步 / ETL / 质量检测 / 指标聚合 / 数据清理
- 关键词搜索

- [x] **Step 3: 任务表格增加列**

- Cron表达式列（等宽字体）
- 下次执行时间列
- 最近执行状态列（成功=绿/失败=红 标签）
- 执行历史列（"查看 12 次" 链接）
- 操作列增加: [执行] [暂停] [日志] [删除]

- [x] **Step 4: 增加任务状态统计卡片**

4个 MetricCard: 运行中(3) | 今日成功(15) | 今日失败(2) | 总任务数(28)

- [x] **Step 5: 增加分页器**

- [x] **Step 6: 截图验证**

---

### Task 2: 细化执行详情页 (4cH4A)

**Files:**
- Modify: `pencil-new.pen` → `4cH4A`

- [x] **Step 1: 读取当前页面结构**

- [x] **Step 2: 添加执行基本信息**

- 任务名称 + 最近执行状态标签
- Cron表达式 | 关联服务 | 调度类型 | 创建时间

- [x] **Step 3: 添加执行历史表格**

表头: 执行序号 | 开始时间 | 结束时间 | 耗时 | 读取记录 | 写入记录 | 错误数 | 状态 | 操作

10行数据，展示执行趋势。

- [x] **Step 4: 添加执行配置快照**

JsonViewer 展示当前执行的配置快照。

- [x] **Step 5: 截图验证**

---

### Task 3: 细化创建定时任务弹窗 (DaZbW)

**Files:**
- Modify: `pencil-new.pen` → `DaZbW`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

- 任务名称 * (输入框)
- 关联服务 * (下拉): 数据同步 / ETL / 质量检测 / 指标聚合 / 数据清理
- 调度类型 * (下拉): Cron定时 / 手动 / 一次性
- Cron表达式 * (输入框，仅 Cron 类型显示) + 常用表达式快捷选择: 每分钟/每小时/每天/每周
- 执行参数 (JsonViewer 区域)
- 失败重试次数 (数字输入，默认 3)
- 超时时间 (数字输入，单位秒，默认 3600)
- 描述 (文本域)
- 底部: [取消] [创建任务]

- [x] **Step 3: 截图验证**

---

## 模块 B: 审计日志

### Task 4: 细化操作审计页 (rL8XE)

**Files:**
- Modify: `pencil-new.pen` → `rL8XE`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `rL8XE` readDepth 3。

- [x] **Step 2: 增加高级筛选区**

- 操作类型下拉: 全部 | CREATE / READ / UPDATE / DELETE / LOGIN / LOGOUT
- 资源类型下拉: 全部 | MODEL / VERSION / DEPLOYMENT / EVALUATION / APPROVAL / PATIENT / DATASET / USER
- 操作人搜索
- 时间范围选择器
- 状态下拉: 全部 | 成功 / 失败
- [查询] [重置] 按钮

- [x] **Step 3: 审计表格增加列**

- 链路追踪ID列（截断显示，可复制）
- 请求方法 + URL列
- 执行耗时列
- 状态列（成功=绿/失败=红）
- 操作列: [详情]

- [x] **Step 4: 增加"导出审计日志"按钮**

蓝色轮廓按钮，在筛选栏右侧。

- [x] **Step 5: 增加分页器**

- [x] **Step 6: 截图验证**

---

### Task 5: 细化操作详情页 (Xa61Z)

**Files:**
- Modify: `pencil-new.pen` → `Xa61Z`

- [x] **Step 1: 读取当前页面结构**

- [x] **Step 2: 添加操作基本信息卡片**

- 链路追踪ID
- 操作人 | IP地址 | User-Agent
- 操作类型 | 资源类型 | 资源ID | 资源名称
- 请求方法 | 请求URL
- 执行耗时 | 状态

- [x] **Step 3: 添加请求/响应详情**

两个 JsonViewer 区域并排：
- 左: 请求参数 (request_params，自动脱敏)
- 右: 响应信息 (response_code + response_msg)

- [x] **Step 4: 增加错误详情区（仅失败时显示）**

红色边框卡片显示 error_message。

- [x] **Step 5: 截图验证**

---

## 模块 C: 告警中心

### Task 6: 细化告警中心页 (Skt7y)

**Files:**
- Modify: `pencil-new.pen` → `Skt7y`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `Skt7y` readDepth 3。

- [x] **Step 2: 添加告警统计卡片**

4个 MetricCard:
- 活跃告警: 3 (红色)
- 今日告警: 8
- 已确认: 5
- 告警规则: 12

- [x] **Step 3: 增加筛选栏**

- 状态Tab: 全部 | FIRING(活跃) | ACKNOWLEDGED(已确认) | RESOLVED(已恢复)
- 严重度下拉: 全部 | CRITICAL / WARNING / INFO
- 关联部署下拉
- 时间范围

- [x] **Step 4: 告警表格增加列**

- 严重度列（CRITICAL=红/WARNING=黄/INFO=蓝 标签）
- 规则名列
- 部署名列
- 触发时间列
- 当前值 | 阈值 列
- 状态列（FIRING=红脉冲 / ACKNOWLEDGED=黄 / RESOLVED=绿）
- 操作列: [确认] [查看规则]

- [x] **Step 5: 增加告警规则入口**

右上角"告警规则管理"按钮。

- [x] **Step 6: 增加分页器**

- [x] **Step 7: 截图验证**

---

### Task 7: 细化告警详情页 (nb9Tt)

**Files:**
- Modify: `pencil-new.pen` → `nb9Tt`

- [x] **Step 1: 读取当前页面结构**

- [x] **Step 2: 添加告警基本信息**

- 告警规则名 + 严重度标签 + 状态标签
- 关联部署 | 指标名称 | 触发条件 | 阈值 | 当前值
- 触发时间 | 确认人 | 确认时间 | 恢复时间

- [x] **Step 3: 添加告警历史图表**

MetricChart 占位区，标注"近7天该规则告警趋势"。

- [x] **Step 4: 添加操作记录**

时间线：
- 2026-04-09 14:30 系统自动触发告警
- 2026-04-09 14:35 张医生 确认告警
- 2026-04-09 15:10 系统自动恢复

- [x] **Step 5: 添加通知记录**

通知记录表格：
- 通知渠道 | 接收人 | 发送时间 | 状态
- 4行数据（email/webhook/sms）

- [x] **Step 6: 截图验证**

---

### Task 8: 细化创建告警规则弹窗 (KMQTC)

**Files:**
- Modify: `pencil-new.pen` → `KMQTC`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 补充表单字段**

- 规则名称 * (输入框)
- 关联部署 * (下拉)
- 监控指标 * (下拉): qps / latency_p50 / latency_p99 / gpu_util / error_rate / cpu_util / memory_util
- 条件 * (下拉): GT(大于) / GTE(大于等于) / LT(小于) / LTE(小于等于) / EQ(等于)
- 阈值 * (数字输入)
- 持续时间 (数字输入，单位秒)
- 严重度 * (下拉): CRITICAL / WARNING / INFO
- 通知渠道 (多选): 邮件 / 短信 / Webhook
- 通知用户 (多选下拉)
- Webhook地址 (仅 Webhook 渠道显示)
- 启用 (开关，默认开启)
- 底部: [取消] [保存规则]

- [x] **Step 3: 截图验证**

---

## 模块 D: 消息中心

### Task 9: 细化消息中心页 (gsUAE)

**Files:**
- Modify: `pencil-new.pen` → `gsUAE`

- [x] **Step 1: 读取当前页面结构**

batch_get 读取 `gsUAE` readDepth 3。

- [x] **Step 2: 增加分类 Tab**

- 全部 | 系统通知 | 审批通知 | 告警通知 | 任务通知
- 右侧: "全部标为已读" 链接

- [x] **Step 3: 消息列表改为卡片式**

每条消息卡片：
- 消息类型图标（系统=齿轮，审批=公文，告警=铃铛，任务=时钟）
- 标题（加粗=未读，普通=已读）
- 摘要文字（最多2行）
- 时间
- 未读标记（蓝色圆点）

6条示例消息：
```
[告警] 推理延迟P99超过阈值 (500ms) — 肺结节检测-生产 — 10分钟前 [蓝点]
[审批] 待审批: 肺结节检测v2.3.1上线审批 — 张医生提交 — 1小时前 [蓝点]
[任务] ETL任务"临床特征提取"执行完成 — 成功处理3,600条 — 2小时前
[系统] 系统维护通知：4月12日凌晨2:00-4:00 — 3小时前
[任务] 模型评估"v2.3.1外部验证"已完成 — AUC: 0.987 — 昨天
[审批] 心电异常v1.5.1临床使用审批已通过 — 审批人: 李主任 — 昨天
```

- [x] **Step 4: 增加分页器**

- [x] **Step 5: 截图验证**

---

### Task 10: 细化消息详情页 (pMRgm)

**Files:**
- Modify: `pencil-new.pen` → `pMRgm`

- [x] **Step 1: 读取当前页面结构**

- [x] **Step 2: 添加消息完整内容**

- 消息类型标签
- 标题
- 发送时间
- 完整内容（富文本区）
- 关联资源链接（如"查看部署详情"、"查看审批"等蓝色链接）

- [x] **Step 3: 添加消息操作区**

底部操作按钮（根据消息类型显示）：
- 告警类: [确认告警] [查看详情]
- 审批类: [去审批]
- 任务类: [查看任务]
- 系统类: [知道了]

- [x] **Step 4: 截图验证**

---

### Task 11: 细化删除确认弹窗 (TuuIC)

**Files:**
- Modify: `pencil-new.pen` → `TuuIC`

- [x] **Step 1: 读取当前弹窗结构**

- [x] **Step 2: 优化通用删除确认弹窗**

- 图标: 红色警告三角
- 标题: "确认删除"
- 内容: "确定要删除 {resource_name} 吗？此操作不可恢复。"
- 输入框: "请输入 {resource_name} 以确认删除" (红色边框)
- 底部: [取消] [确认删除] (红色 Danger 按钮)

- [x] **Step 3: 截图验证**

---

## 执行顺序总结

| Task | 模块 | 页面/弹窗 | 类型 |
|------|------|----------|------|
| 1 | 任务调度 | 任务调度页 (Ujek8) | 页面细化 |
| 2 | 任务调度 | 执行详情页 (4cH4A) | 页面细化 |
| 3 | 任务调度 | 创建定时任务弹窗 (DaZbW) | 弹窗细化 |
| 4 | 审计 | 操作审计页 (rL8XE) | 页面细化 |
| 5 | 审计 | 操作详情页 (Xa61Z) | 页面细化 |
| 6 | 告警 | 告警中心页 (Skt7y) | 页面细化 |
| 7 | 告警 | 告警详情页 (nb9Tt) | 页面细化 |
| 8 | 告警 | 创建告警规则弹窗 (KMQTC) | 弹窗细化 |
| 9 | 消息 | 消息中心页 (gsUAE) | 页面细化 |
| 10 | 消息 | 消息详情页 (pMRgm) | 页面细化 |
| 11 | 通用 | 删除确认弹窗 (TuuIC) | 弹窗细化 |

**总计**: 11 Tasks, ~8 页面细化 + 3 弹窗细化
