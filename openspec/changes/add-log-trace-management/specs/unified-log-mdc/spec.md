## ADDED Requirements

### Requirement: MDC 上下文注入用户与组织标识
服务的入站过滤器在注入 traceId 的同时，SHALL 将网关注入的 `X-User-Id`、`X-Org-Id` 头写入 MDC（存在时），使现有日志 pattern 的 `%X{userId}`/`%X{orgId}` 占位符生效；请求结束时一并清理。

#### Scenario: 登录用户请求日志含操作者标识
- **WHEN** 携带用户头的已认证请求到达任一服务并产生日志
- **THEN** 日志行同时包含 traceId、userId、orgId 三个字段值

#### Scenario: 匿名或缺失用户头
- **WHEN** 请求未携带 `X-User-Id`（如健康检查）
- **THEN** 服务正常处理，日志中 userId/orgId 为空而 traceId 仍有效

### Requirement: 日志行格式包含链路三要素
全部 Java 服务（经 Nacos 共享配置）与 aiworker（经本地日志配置）的日志行格式 SHALL 包含时间、traceId、userId、orgId、级别、logger 与消息，使同一 traceId 的日志可在任意服务日志中直接目视检索。

#### Scenario: 跨服务目视检索
- **WHEN** 以某 traceId 分别检索网关、maidc-data、aiworker 的控制台日志
- **THEN** 三个服务的日志行均能按该 traceId 命中且格式一致可读

### Requirement: 响应回显 traceId
服务的 HTTP 响应 SHALL 通过统一响应体 `R.traceId` 返回当前链路 traceId（取自 MDC）；该值 MUST 非空（入站过滤器保证 MDC 有值）。

#### Scenario: 前端获取 traceId 用于报障
- **WHEN** 前端任意 API 调用返回成功或失败响应
- **THEN** 响应体 `traceId` 字段为本次请求的 traceId，前端可将其展示或随报障提交
