## ADDED Requirements

### Requirement: 审计列表页展示并支持按 traceId 检索
操作日志、数据访问日志、系统事件三个审计页面 SHALL 在表格中展示 traceId（monospace、超长缩略、完整值可见），并提供 traceId 查询条件；交互样式遵循深色科技风基调（暗夜蓝黑底、霓虹青点缀）。

#### Scenario: 用户按 traceId 过滤操作日志
- **WHEN** 用户在操作日志页的 traceId 查询框输入 T 并查询
- **THEN** 表格仅显示 traceId 为 T 的记录，清空条件后恢复全量分页

#### Scenario: 页面视觉符合设计基调
- **WHEN** 打开任一审计页面查看 traceId 列与查询控件
- **THEN** 呈现深色科技风样式，与现有审计页面风格一致无突兀

### Requirement: traceId 可复制用于报障
审计页面与详情抽屉中的 traceId SHALL 支持一键复制完整值，复制成功 SHALL 有可见反馈。

#### Scenario: 从列表复制 traceId
- **WHEN** 用户点击操作日志列表某行的 traceId 复制控件
- **THEN** 完整 traceId 写入剪贴板并出现成功提示

#### Scenario: 从详情抽屉复制 traceId
- **WHEN** 用户打开某条审计记录详情抽屉
- **THEN** 抽屉中展示完整 traceId 且可一键复制
