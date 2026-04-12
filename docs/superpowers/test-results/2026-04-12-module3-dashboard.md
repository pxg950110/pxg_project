# 测试报告 - 仪表盘模块

## 测试时间
2026-04-12

## 测试环境
- 前端: http://localhost:3000
- 后端: gateway 8080, auth 8081
- 账号: admin / Admin@123 (已登录)
- 浏览器: Chrome DevTools MCP

## 测试结果

| 编号 | 操作 | 状态 | 实际API | 实际响应 | UI结果 | 备注 |
|------|------|------|---------|----------|--------|------|
| 3.1.1 | 导航到 /dashboard/overview | PASS | GET /api/v1/users/me | 200 | 显示6个统计卡片(模型总数28/活跃部署8/今日推理12456/患者记录156000/研究项目12/待审批5) + 模型状态分布图表 + 最近告警 + 最近活动 + 数据源连接状态 | 统计数据为前端硬编码mock，未调用后端统计API |
| 3.1.2 | 时间范围切换(近7天/30天/90天) | N/A | - | - | - | 页面无时间范围切换控件，Overview.vue 中未实现该功能 |
| 3.2.1 | 导航到 /dashboard/model | PASS | GET /api/v1/users/me | 200 | 显示4个指标卡片(活跃部署45/今日推理23856/平均延迟38ms/GPU利用率76%) + 推理量趋势图 + 模型性能排行 + 部署状态饼图 | 统计数据为前端硬编码mock |
| 3.2.2 | 点击模型卡片跳转详情 | N/A | - | - | - | 模型看板无单独可点击的模型卡片，仅有图表统计展示 |
| 3.3.1 | 导航到 /dashboard/data | **FAIL** | GET /api/v1/users/me | 200 | 指标卡片和图表正常显示(患者总数152847/研究项目28/数据集64/ETL任务156)，但ETL任务表格报错白屏 | **BUG**: StatusBadge 组件传入无效 type="success"/"processing"/"error"/"warning"，应为 StatusType 枚举值如 "sync" |

## 详细 Bug 记录

### BUG-3.1: DataDashboard ETL 表格 StatusBadge 类型错误
- **文件**: `maidc-portal/src/views/dashboard/DataDashboard.vue`
- **行号**: 77
- **错误**: `TypeError: Cannot read properties of undefined (reading '已完成')`
- **堆栈**: `statusMap.ts:120 -> index.vue:16`
- **根因**: `StatusBadge` 组件接收 `type` prop 值为 "success"/"processing"/"error"/"warning"（Ant Design badge 颜色），但 `getStatusMeta()` 期望 `type` 为 `StatusType` 枚举值（如 "sync"、"eval" 等）。`statusMapRegistry["success"]` 返回 `undefined`，导致 `undefined["已完成"]` 抛出 TypeError。
- **当前 mock 数据**: `statusLabel: "已完成"`, `statusType: "success"` 
- **修复方案**: 将 `statusLabel` 改为英文状态码（如 "COMPLETED"），将 `statusType` 改为合法的 `StatusType` 值（如 "sync"）。例如：
  ```js
  { statusLabel: 'COMPLETED', statusType: 'sync' }  // 已完成
  { statusLabel: 'RUNNING', statusType: 'sync' }     // 运行中
  { statusLabel: 'FAILED', statusType: 'sync' }      // 失败
  { statusLabel: 'PENDING', statusType: 'sync' }     // 排队中
  ```

### 附加警告
- **WARN**: MetricChart 组件 height prop 期望 String 类型，但 DataDashboard.vue 传入 Number 类型 (320)。建议改为 `height="320"`。
- **WARN**: ECharts 报告 `Can't get DOM width or height`，疑似因表格渲染报错导致布局异常。

## 截图文件
- 3.1.1-dashboard-overview.png
- 3.2.1-model-dashboard.png
- 3.3.1-data-dashboard.png

## 统计
- 总测试点: 5
- PASS: 2
- FAIL: 1
- N/A (功能未实现): 2
- 通过率: 66.7% (2/3 已实现功能中通过)
- Bug 数量: 1 个 (严重 - 导致页面渲染错误)
