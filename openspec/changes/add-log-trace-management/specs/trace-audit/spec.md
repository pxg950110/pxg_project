## ADDED Requirements

### Requirement: 操作日志事件全覆盖转发到审计服务
凡发布 `OperationLogEvent`（由 `@OperLog` 注解触发）的服务，SHALL 由 common 自动装配的事件转发器将日志事件发送到 `maidc.audit` 交换机（routing key `audit.operation`），payload 字段结构与现有审计消费契约一致；转发器 SHALL 可通过配置关闭（`maidc.audit.forward.enabled=false`），且在无 RabbitTemplate 时不装配。现有手写 listener（auth/model）MUST 移除并以公共转发器替代。

#### Scenario: 此前丢失审计的服务开始落库
- **WHEN** 用户在 maidc-data 触发任一标注 `@OperLog` 的操作
- **THEN** audit 服务消费到该操作日志消息并写入 `audit.a_audit_log`，serviceName 为 `maidc-data`

#### Scenario: 收敛后 auth 审计行为不变
- **WHEN** 用户登录（maidc-auth 的 `@OperLog` 方法）成功
- **THEN** `a_audit_log` 中该记录的模块/操作/结果字段与收敛前一致，traceId 有值

### Requirement: 审计三表记录 traceId
`audit.a_audit_log`、`audit.a_data_access_log`、`audit.a_system_event` SHALL 记录 traceId：前者沿用现有 NOT NULL 列，后两者新增可空 `trace_id VARCHAR(64)` 列并建立 partial 索引；审计消费者在消息 traceId 空白时 SHALL 生成兜底值并记录 warn 日志，不得因 traceId 缺失导致消息进入 DLQ。

#### Scenario: 越权事件可按 trace 关联
- **WHEN** 某 traceId 为 T 的请求触发权限拒绝并被审计
- **THEN** `a_system_event` 对应事件的 trace_id 为 T，可用 T 查到该事件

#### Scenario: 历史行为兼容
- **WHEN** 存量 `a_data_access_log`/`a_system_event` 行无 traceId（列为 NULL）
- **THEN** 既有查询与页面不受影响，NULL 行不被 trace 索引扫描

### Requirement: 审计查询 API 支持按 traceId 过滤并回显
审计服务的操作日志、数据访问日志、系统事件三个分页查询接口 SHALL 支持 `traceId` 精确等值过滤，且返回记录中 SHALL 包含 traceId 字段；过滤查询 MUST 命中索引。

#### Scenario: 按 traceId 检索操作日志
- **WHEN** 以参数 `traceId=T` 调用 `GET /api/v1/audit/operations`
- **THEN** 仅返回 trace_id 为 T 的记录（分页），每条记录含 traceId 字段

#### Scenario: 无权限访问被拒绝
- **WHEN** 无 `audit:read` 权限的用户调用带 traceId 过滤的审计查询
- **THEN** 请求被拒绝，行为与现有审计接口的权限控制一致
