# Module 3: Dashboard Rerun Results

**Test Date:** 2026-04-12
**Environment:** http://localhost:3000

| Test ID | Description | Status | Details |
|---------|-------------|--------|---------|
| T3.1 | /dashboard/overview page loads | PASS | Page loads with title "系统总览 - MAIDC". Shows greeting "下午好，系统管理员", date "今天是2026年4月12日", 3 pending tasks, and quick action buttons (注册模型, 新建评估, 提交审批). |
| T3.2 | Stats cards display correct numbers | PASS | All 6 stat cards display correctly: 模型总数 28个 (+12%), 活跃部署 8个 (+3%), 今日推理次数 12,456次 (+8%), 患者记录 156,000条 (+5%), 研究项目 12个, 待审批 5条 (-2%). Each card shows metric name, value, unit, trend arrow, and percentage. |
| T3.3 | "最近活动" section shows activity list | PASS | Displays 8 recent activity items with timestamps: 胸部CT诊断模型 v2.3 已通过审批 (审批, 10分钟前), 推理延迟P99超过阈值 (告警, 15分钟前), 心血管风险评估模型评估完成 AUC=0.94 (评估, 32分钟前), 糖尿病视网膜病变检测模型已部署上线 (部署, 1小时前), 影像数据集 v3.0 ETL 导入完成 (ETL, 2小时前), and more. Each item has a category tag and time. |
| T3.4 | "数据源连接状态" section | PASS | Shows 4 data source connections: HIS 系统 (已连接), LIS 检验系统 (已连接), PACS 影像系统 (已连接), EMR 电子病历 (断开). Each shows system name and description. |
| T3.5 | Click "查看全部" on alerts | PASS | Clicking "查看全部" under "最近告警" navigates to /alert/active. Alert center loads with title "告警中心", 4 stat cards (活跃告警 12个, 今日已处理 34个, 平均响应 8分钟, 告警规则 12条), tabs (活跃告警/历史告警), and alert table with columns: 告警名称, 级别, 关联资源, 指标, 当前值/阈值, 触发时间, 状态, 操作. |
| T3.6 | Navigate to /dashboard/data | PASS | Data dashboard loads with title "数据看板 - MAIDC". Shows subtitle "临床数据与研究数据监控", stat cards, charts sections, and ETL task table. |
| T3.7 | ETL task table displays | PASS | Table shows 5 ETL tasks with columns: 任务名, 类型, 状态, 数据量, 完成时间. Status badges display correctly: "已完成" (green), "运行中" (blue/active), "失败" (red). Tasks include: 影像数据增量同步 (增量同步, 已完成, 128.5 GB), 检验报告全量导入 (全量导入, 运行中, 45.2 GB), 电子病历数据清洗 (数据清洗, 已完成, 86.7 GB), 患者基本信息脱敏 (数据脱敏, 已完成, 12.3 GB), 用药记录增量同步 (增量同步, 失败, 0 GB). Total 8 tasks with pagination. |
| T3.8 | Data source stats cards | PASS | 4 stat cards display: 患者总数 152,847人 (+234%), 研究项目 28个 (+3%), 数据集 64个, ETL任务 156个 (+5%). Additional sections for "数据增长趋势（月度）" chart and "数据来源分布" chart. |

## Summary
- PASS: 8
- FAIL: 0
- PARTIAL: 0

## Additional Observations

### Dashboard Overview Layout
- **Stats grid:** 6 cards in responsive grid layout
- **Sections:** 模型状态分布 (with 查看全部), 最近告警 (4 alerts, 查看全部), 最近活动 (8 items, 查看全部), 数据源连接状态 (4 connections)
- **Alert types displayed:** CRITICAL (推理延迟P99超阈值), WARNING (GPU使用率 > 90%, 磁盘空间 < 20%, 模型推理服务响应时间增加)

### Dashboard Data Layout
- **Stats grid:** 4 cards with trend indicators
- **Charts:** 数据增长趋势 and 数据来源分布
- **ETL Task Table:** Well-formatted with status badges and pagination

### Alert Center (navigated from T3.5)
- **Active alerts:** 推理延迟过高 (严重, 肺结节检测-v2), GPU内存使用率告警 (警告, GPU-Node-03), 错误率异常 (警告, 心电图分析-v1), 模型服务不可用 (严重, 病理分类-v3)
- **Actions available:** 详情, 确认

## Issues Found
- None. All dashboard features work as expected.
