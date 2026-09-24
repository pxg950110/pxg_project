## ADDED Requirements

### Requirement: 网关为每个入站请求生成或沿用 traceId 并全量下发
网关 SHALL 在全局过滤器中对每个入站请求处理 traceId：请求头 `X-Trace-Id` 存在且合法时 SHALL 沿用（规范化为 32 位小写 hex），否则 SHALL 生成新的 32 位小写 hex traceId；处理后的请求（携带 `X-Trace-Id` 头）MUST 作为实际对象传递给过滤器链向下游转发，且响应头 MUST 回写 `X-Trace-Id`。

#### Scenario: 外部请求无 traceId 头
- **WHEN** 客户端发起不带 `X-Trace-Id` 头的请求
- **THEN** 网关生成 32 位小写 hex 的 traceId，下游服务收到的请求头包含该 `X-Trace-Id`，响应头 `X-Trace-Id` 返回同一值

#### Scenario: 外部请求携带合法 traceId 头
- **WHEN** 客户端（如前端 request 拦截器）发起带 `X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000` 的请求
- **THEN** 网关沿用该值并规范化为 `550e8400e29b41d4a716446655440000`，下游与响应使用同一值

#### Scenario: 外部请求携带非法 traceId 头
- **WHEN** 请求头 `X-Trace-Id` 为空串、超长（>64 字符）或含非 hex 字符
- **THEN** 网关丢弃该值并生成新的 32 位小写 hex traceId，链路不中断

### Requirement: traceId 格式全系统统一
所有入口（网关、Servlet 过滤器、MQ 消费者、Python HTTP 中间件、Celery 任务）SHALL 复用统一的 traceId 规则：格式为不超过 64 字符的字符串，规范形态为 32 位小写 hex；生成逻辑 MUST 收敛在单一工具实现（Java `TraceIds`、Python `trace.py`），禁止各入口自定义格式（如 `eval-{id}`）。

#### Scenario: Celery 任务不再伪造 traceId
- **WHEN** aiworker 执行一个由 MQ 消息触发的评测任务
- **THEN** 任务执行期间日志中的 traceId 来自消息继承或按统一规则生成，而非 `eval-{evaluation_id}` 形态

### Requirement: 服务端入站请求还原 trace 上下文
每个 MVC 微服务 SHALL 通过公共自动装配的 Servlet 过滤器在请求进入时将 `X-Trace-Id`（缺失则按统一规则生成）写入 MDC，请求结束时 MUST 清理过滤器注入的 MDC 键且不清理其他键；该能力 MUST 对业务服务零代码接入，新增服务引入 common 依赖即自动生效。

#### Scenario: 下游服务日志携带网关下发的 traceId
- **WHEN** 携带 `X-Trace-Id` 的请求到达 maidc-data 的任一接口
- **THEN** 该请求处理期间输出的日志行 `%X{traceId}` 为该值，请求结束后 MDC 中该键被清除

#### Scenario: 绕过网关的直连调用
- **WHEN** 开发环境直接调用某服务接口且未携带 `X-Trace-Id`
- **THEN** 服务按统一规则生成 traceId，日志与审计链路仍然完整

### Requirement: 跨服务 HTTP 调用自动透传 trace 上下文
通过 Spring `RestTemplate` 发起的服务间 HTTP 调用（含 `AiWorkerClient` 调用 aiworker）SHALL 通过公共拦截器自动携带当前 MDC 中的 `X-Trace-Id`（及用户上下文头），调用方代码无需显式设置。

#### Scenario: Java 服务调用 Python aiworker
- **WHEN** maidc-model 通过 RestTemplate 调用 aiworker 的 `/api/v1/predict`
- **THEN** aiworker 收到的请求头包含与调用方日志一致的 `X-Trace-Id`

### Requirement: MQ 消息承载并在消费端还原 trace
RabbitMQ 消息 SHALL 继续以 `MaidcMessage.traceId` 字段承载 traceId（生产端从 MDC 自动补齐）；所有消费者（含未继承 `BaseMessageConsumer` 的消费者）SHALL 在消费入口通过公共工具将消息 traceId 还原到 MDC，消费结束清理，消息无 traceId 时按统一规则生成兜底值。

#### Scenario: 未继承基类的消费者还原 trace
- **WHEN** `AlertNotifyConsumer` 消费一条 traceId 为 T 的告警消息
- **THEN** 消费处理期间日志的 traceId 为 T，处理后该消息流程中后续发出的消息（若有）继承 T

#### Scenario: Python 结果消息回传 Java
- **WHEN** aiworker Celery 任务完成并以消息回发结果（traceId 继承自触发消息）
- **THEN** Java 消费端（如 `EvaluationResultConsumer`）处理该消息期间 MDC 中的 traceId 与最初触发任务的消息一致

### Requirement: Python 服务 HTTP 入口与 Celery 任务的 trace 接入
aiworker SHALL 提供 FastAPI 中间件：读取 `X-Trace-Id`（缺失则按统一规则生成）存入请求级上下文（ContextVar），并通过 logging Filter 注入每条日志；Celery 任务 SHALL 在任务开始时从消息参数继承 traceId、结束时清理。Python 侧日志行的 traceId 字段含义与 Java 侧一致。

#### Scenario: aiworker HTTP 请求日志可关联
- **WHEN** 带 `X-Trace-Id` 的请求调用 aiworker 任一 API
- **THEN** 该请求处理期间 aiworker 输出的每条日志均带相同 traceId，且该 traceId 与 Java 调用方一致

#### Scenario: Celery 异步任务链路不断
- **WHEN** maidc-model 发送 traceId 为 T 的 MQ 消息触发 aiworker 评测任务
- **THEN** Celery 任务执行日志的 traceId 为 T，任务结果消息的 traceId 亦为 T
