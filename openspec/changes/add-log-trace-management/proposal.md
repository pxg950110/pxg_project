## Why

系统由 9 个服务（gateway/auth/data/model/task/label/audit/msg + aiworker-Python）组成，排障时无法把一次请求在各服务、MQ、异步任务中产生的日志串起来：网关 `TraceFilter` 存在缺陷（mutate 后的请求未传入 `chain.filter()`，`X-Trace-Id` 实际未下发），下游服务没有任何「请求头 → MDC」注入组件，HTTP 路径日志中 `%X{traceId}` 恒为空、`R.traceId` 返回 null；Python aiworker 完全没有 trace 概念且 Celery 任务伪造 traceId；4 个服务的 `@OperLog` 事件无人消费导致审计静默丢失；`a_data_access_log`/`a_system_event` 无 trace_id 列，前端审计页不支持按 traceId 查询。traceId 的「生成→传递→输出→落库→查询」五个环节均有断点，需要一个通用的日志链路管理方案一次性补齐。

## What Changes

- **修复并统一 traceId 生成与下发**：修复网关 `TraceFilter`（缺失的 request mutation）、沿用入站 `X-Trace-Id`、生成 32 位 hex traceId、写响应头、WebFlux MDC 注入；统一 traceId 格式（32 位 hex，废除 `eval-{id}` 等伪造形态）。
- **common 层新增通用 TraceContext 组件**（`common-log`）：`TraceContextFilter`（OncePerRequestFilter）为全部 8 个 MVC 服务自动装配「X-Trace-Id/X-User-Id/X-Org-Id → MDC」注入与请求结束清理，业务服务零代码接入；自动装配从 `spring.factories` 迁移到 `AutoConfiguration.imports`。
- **跨服务 HTTP 调用透传**：`AiWorkerClient`（RestTemplate）增加拦截器自动透传 trace 头。
- **MQ 链路补齐**：沿用 `MaidcMessage.traceId` 消息体承载方式，补齐未继承 `BaseMessageConsumer` 的消费者的 MDC 还原；aiworker Celery 任务从消息继承 traceId（contextvar）并在结果消息中回传。
- **Python aiworker 接入**：FastAPI 中间件读取 `X-Trace-Id` 注入 logging context（ContextVar + logging.Filter），统一 logging 配置替换 `print()`。
- **审计事件统一转发**：`common-log` 内置 `OperationLogEventForwarder`（`@ConditionalOnBean(RabbitTemplate)`），监听 `OperationLogEvent` 并发布到 `maidc.audit` 交换机，替代 auth/model 中手写的 listener，消除 data/task/label/msg 四个服务审计静默丢失。
- **审计库 trace 化**：`a_data_access_log`、`a_system_event` 增加 `trace_id` 列与索引；审计查询 API（operations/data-access/events）支持按 traceId 过滤并回显 traceId。
- **门户端链路检索**：审计三个日志页面增加 traceId 列（可复制）、traceId 查询条件与详情展示，遵循深色科技风。
- 可选（P1，不阻塞）：Logback Logstash JSON appender 对接既有 ELK 栈（`monitoring/elk/logstash.conf` 已预留 trace_id 解析）。

## Capabilities

### New Capabilities
- `trace-context-propagation`: traceId 的生成、格式约定、HTTP 网关→服务→跨服务→MQ→Python 全链路透传与消费端还原。
- `unified-log-mdc`: 服务端统一 MDC 注入（traceId/userId/orgId）、日志行格式、响应（响应头 + `R.traceId`）回显。
- `trace-audit`: 审计日志链路化——`@OperLog` 事件全覆盖转发、审计三表记录 trace_id、按 traceId 的查询 API。
- `trace-audit-portal`: 门户审计页面按 traceId 展示、检索与复制的交互能力。

### Modified Capabilities

（无——`openspec/specs/` 当前为空，本变更全部为新增能力。）

## Impact

- **代码**：
  - `maidc-parent/maidc-gateway`：`filter/TraceFilter.java`（修复 + MDC）。
  - `maidc-parent/common/common-log`：新增 `TraceContextFilter`、`OperationLogEventForwarder`，自动装配改造；`common-mq`：消费者 MDC 还原补齐。
  - `maidc-model`：`AiWorkerClient` 透传、`ModelOperationLogEventListener` 收敛；`maidc-auth`：listener 收敛；`maidc-data`/`maidc-task`/`maidc-label`/`maidc-msg`：零改动自动获得审计转发。
  - `maidc-audit`：实体/查询 DTO/Repository 增加 trace_id。
  - `maidc-aiworker`（Python）：FastAPI 中间件、logging 配置、Celery trace 继承、结果消息回传。
  - `maidc-portal`：`src/views/audit/*`、`src/api/audit.ts`。
- **数据库**：`docker/init-db/06-audit.sql` 修改（新环境）+ 新增增量脚本（存量环境）为 `a_data_access_log`/`a_system_event` 加 `trace_id`。
- **配置**：`docker/nacos-config/maidc-shared.yaml`（日志 pattern 已就绪，基本不动）；无新增中间件依赖。
- **兼容性**：`X-Trace-Id` 请求头语义不变（前端已有注入逻辑，无需改动即可受益）；MQ 消息体新增字段仅 `Optional` 消费，旧消息可正常消费；无 BREAKING 变更。
- **不引入** micrometer-tracing/Zipkin/Sleuth 等新框架与采集组件（作为后续演进方向，见 design.md）。
