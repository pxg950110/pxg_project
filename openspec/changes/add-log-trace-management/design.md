## Context

系统为 Spring Boot 3.2.5 微服务群（gateway:8080 / auth:8081 / data:8082 / model:8083 / task:8084 / label:8085 / audit:8086 / msg:8087 + Python FastAPI/Celery aiworker:8090），RabbitMQ 3.12 做异步总线，Nacos 做注册与共享配置，PostgreSQL 15 存储，前端 maidc-portal 经网关访问。

traceId 相关的现状资产与断点（调研结论）：

**已有资产**
- 网关有 `TraceFilter`（GlobalFilter）雏形、`MaidcMessage.traceId` 消息体字段、`BaseMessageProducer`（发送前从 MDC 补 traceId）/`BaseMessageConsumer`（消费时 MDC.put 还原）闭环——MQ 段链路是通的。
- `common-log` 的 `OperationLogAspect` 已从 MDC 读 traceId 并写入审计事件；`R.ok()/fail()` 已从 MDC 读 traceId 放入响应体。
- Nacos 共享配置 `maidc-shared.yaml` 的日志 pattern 已含 `%X{traceId}/%X{userId}/%X{orgId}` 占位符。
- `audit.a_audit_log` 已有 `trace_id VARCHAR(64) NOT NULL` 列 + 索引；audit 服务消费/查询/导出链路完整；前端审计页面已存在。
- `monitoring/elk/logstash.conf` 已预留 trace_id 字段解析（TCP 5044 json_lines，无生产者对接）。

**断点**
1. 网关 `TraceFilter` 有缺陷：`exchange.getRequest().mutate().header(...).build()` 的结果未传给 `chain.filter()`，`X-Trace-Id` 实际未下发；网关自身从不 `MDC.put`。
2. 8 个 MVC 服务没有任何「X-Trace-Id → MDC」Filter，HTTP 路径日志占位符恒空、`R.traceId` 返回 null、`OperationLogAspect` 取到 null。
3. 跨服务 HTTP 调用（`maidc-model/AiWorkerClient` 裸 RestTemplate 调 aiworker）不透传 trace 头。
4. Python aiworker 无 trace 概念：无中间件、无 logging 配置（`print()`）、Celery 任务伪造 `"eval-{id}"`，不继承来源消息 traceId。
5. 审计覆盖缺口：data/task/label/msg 四个服务的 `@OperLog` 事件发布后无 listener 消费（审计静默丢失）；`a_data_access_log`/`a_system_event` 无 trace_id 列；审计查询 DTO 与前端页面均不支持 traceId。
6. 部分 MQ 消费者（如 `PersonalTaskConsumer`、`AlertNotifyConsumer`、`EvaluationResultConsumer`）未继承 `BaseMessageConsumer`，trace 还原情况未保障。

## Goals / Non-Goals

**Goals:**
- 一次请求（HTTP 入口 → 下游服务 → 跨服务 HTTP/MQ → Python Celery 异步任务）产生的所有日志行可用同一 traceId 关联，形成可检索的链路。
- traceId 在响应头、响应体 `R.traceId`、审计库、前端页面全链路可见，可作为排障入口（用户报障时提供 traceId 即可定位全链路日志与审计记录）。
- 业务服务（含未来新增服务）零代码接入：注入、透传、审计转发全部由 common 自动装配完成。
- 审计事件覆盖 8 个 Java 服务全部 `@OperLog` 注解方法，不再有静默丢失。

**Non-Goals:**
- 不做完整分布式追踪（span 树、耗时瀑布图），不引入 micrometer-tracing / Sleuth / Zipkin / Jaeger。
- 不建设日志采集平台：ELK 对接仅提供配置模板（P1，不默认启用）。
- 不改变 MQ 消息契约（traceId 继续走 `MaidcMessage` 消息体字段，不迁移到 RabbitMQ Headers）。
- 不处理前端页面级操作序列串联（同一页面的多次请求仍是独立 trace）。
- 不为 `@Async` 线程池、定时任务等非 MQ/HTTP 入口补 trace 传播（后续按需扩展）。

## Decisions

### D1. 自研轻量 TraceContext（MDC + Filter + 拦截器），不引入 tracing 框架

- **选择**：在 `common-log` 内新增约 5 个类（Filter、RestTemplate 拦截器、审计转发器、自动装配、常量工具），复用 SLF4J MDC 与现有 pattern。
- **理由**：现有资产已覆盖 80%（见 Context），缺的只是若干断点；目标场景是「审计关联 + 按 traceId 查日志」，单 traceId 足够。引入 micrometer-tracing + Zipkin 需要新部署追踪后端、全服务加依赖、W3C traceparent 改造，对本目标过度。
- **演进预留**：header 沿用 `X-Trace-Id`；未来升级 W3C 时在 Filter 中兼容解析 `traceparent` 即可，API 契约不变。

### D2. traceId 格式与生成规则

- 格式统一为 **32 位小写 hex**（`UUID.randomUUID()` 去横线），长度上限 64（对齐 DB 列宽）。
- **生成点**：网关是 HTTP 流量的唯一规范生成点；前端已注入的 `X-Trace-Id` **沿用**（去横线规范化，非法/超长则重新生成）。
- 所有入口（网关 GlobalFilter、Servlet Filter、MQ 消费、Celery 任务）遵守同一规则：**有则沿用并规范化，无则生成**。废除 aiworker `eval-{id}` 伪造形态。
- 提供唯一的工具类 `TraceIds`（normalize/generate/isValid），各入口复用，避免格式漂移。

### D3. 注入点分层：WebFlux 网关与 Servlet 服务各自实现，规则一致

- **8 个 MVC 服务**：`common-log` 新增 `TraceContextFilter`（`OncePerRequestFilter`，order = 最高优先级），从 `X-Trace-Id`/`X-User-Id`/`X-Org-Id`/`X-Username` 头注入 MDC，`finally` 中仅 remove 自己 put 的 key（避免污染线程池复用线程）。通过 `OperationLogAutoConfiguration` 自动装配，业务服务零改动。
- **网关（WebFlux）**：不能复用 Servlet Filter。修复 `TraceFilter`：① `chain.filter(mutatedExchange)` 使用 mutate 后的请求（修复缺陷）；② 响应头回写 `X-Trace-Id`；③ `MDC.put` + `chain.filter(...).doFinally(...)` 清理。
- **已知限制**：WebFlux 在 reactive 链路发生线程切换后 MDC 不保证可见，网关日志为尽力而为。接受该限制——网关日志量小且非关键审计路径，完整的 Reactor Context 方案复杂度不成比例。

### D4. 审计事件转发收敛到 common-log，替代各服务手写 listener

- 新增 `OperationLogEventForwarder`：`@EventListener(OperationLogEvent)` → payload 字段结构与现有 auth/model listener **完全一致**（audit 消费端 `AuditLogConsumer` 无感）→ `RabbitTemplate` 发 `maidc.audit` / `audit.operation`。
- 条件装配：`@ConditionalOnBean(RabbitTemplate)` + `@ConditionalOnProperty("maidc.audit.forward.enabled", matchIfMissing = true)`；`serviceName` 取 `spring.application.name`。
- 删除 `maidc-auth/OperationLogEventListener` 与 `maidc-model/ModelOperationLogEventListener`（行为等价收敛）；**data/task/label/msg 四服务零改动自动获得审计转发**，消除静默丢失。
- **备选否决**：让各服务继续手写 listener——现状已证明不可扩展（4 处遗漏）。

### D5. MQ traceId 保持消息体承载，补齐非标准消费者

- `MaidcMessage.traceId` + `BaseMessageProducer/Consumer` 闭环保持不变（对排障者可见性好，Python pika 读取简单，改动面最小）。
- `common-mq` 新增静态工具 `TraceMessageHelper.restore(message)/clear()`（封装 MDC put/remove + 兜底生成），供未继承 `BaseMessageConsumer` 的消费者（`PersonalTaskConsumer`、`AlertNotifyConsumer`、`EvaluationResultConsumer` 等）在消费入口调用；逐个盘点并接入。

### D6. Python aiworker 接入：ContextVar + logging.Filter，Celery 继承与回传

- **HTTP 段**：FastAPI middleware 读取 `X-Trace-Id` 存入 `ContextVar`；`logging.Filter` 将 ContextVar 值注入每条 LogRecord；统一 `dictConfig` 日志配置（替换 `print()`），格式含 `traceId`。
- **Celery 段**：`task_prerun` 信号从任务入参（`MaidcMessage` dict）取 traceId 写入 ContextVar（无则生成），`task_postrun` 清理；结果回发消息（`evaluation.result` 等）**携带继承的 traceId**，Java 消费端（`EvaluationResultConsumer`）经 `TraceMessageHelper` 还原——全链路（Java→MQ→Python→MQ→Java）traceId 不断。
- aiworker 不接 Nacos，日志配置放 `app/core/logging.py`，traceId 工具放 `app/core/trace.py`，格式与 Java 侧一致。

### D7. 跨服务 HTTP 调用透传：RestTemplate 拦截器

- `common-log` 提供 `TraceRestTemplateCustomizer`（`ClientHttpRequestInterceptor`，从 MDC 取 traceId/userId 写入请求头），Spring Boot `RestTemplateBuilder` 自动应用。
- `AiWorkerClient` 改为注入 `RestTemplateBuilder` 构建（替换裸 `new RestTemplate()`），自动获得透传能力；未来新增 HTTP 调用点同样零成本接入。
- **备选否决**：为单一调用点引入 OpenFeign——依赖与学习成本不成比例。

### D8. 数据库：增量脚本加列，消费端兜底防 DLQ

- 新增 `docker/init-db/22-audit-trace.sql`：`a_data_access_log`、`a_system_event` 增加 `trace_id VARCHAR(64)`（可空，历史数据兼容）+ partial index（`WHERE trace_id IS NOT NULL`）。全新环境按编号顺序执行自动生效；存量环境手动执行一次（无 flyway，项目惯例即编号增量脚本）。
- `a_audit_log.trace_id` 保持 `NOT NULL` 不动；**过渡期防护**：`AuditLogConsumer` 在 traceId 空白时生成兜底值并 `log.warn`（避免修复上线前的直连/异常流量违约进 DLQ），源端修复后兜底自然不再触发。

### D9. 审计查询 API：三接口加 traceId 过滤，聚合端点 P1

- `AuditLogQueryDTO`、`SystemEventQueryDTO`、`DataAccessQueryDTO` 增加 `traceId`（精确等值，命中索引）；三个分页接口返回体回显 traceId；audit 实体补字段映射。
- 聚合端点 `GET /api/v1/audit/traces/{traceId}`（三表并发查询各限 100 条、按时间归并返回链路概览）列为 **P1 可选**；P0 前端用「操作日志页按 traceId 过滤」即可满足排障。

### D10. 门户端：traceId 列、查询、复制

- `OperationLog.vue` / `DataAccessLog.vue` / `SystemEventLog.vue`：表格增加 traceId 列（monospace、缩略展示、点击复制），查询表单增加 traceId 条件；`AuditDetailDrawer` 详情展示完整 traceId 可复制。样式遵循深色科技风（霓虹青 `#22d3ee` 作为 traceId chip 点缀），前端 `request.ts` 无需改动（已有 X-Trace-Id 注入）。

### D11. ELK 对接仅留模板（P1，不阻塞）

- 提供启用了 `logstash` profile 时生效的 `logback-spring.xml` 模板（`LogstashTcpSocketAppender` → TCP 5044 json-lines，字段映射对齐现有 `logstash.conf` 的 trace_id 约定），默认不启用；启用时机另行决策。

## Risks / Trade-offs

- [WebFlux 线程切换后网关 MDC 可能为空] → 接受尽力而为；网关非审计关键路径；doFinally 保证清理不串线程。
- [存量环境 init-db 不重跑，新列不会自动出现] → 部署说明明确「手动执行 22-audit-trace.sql」；脚本幂等（`IF NOT EXISTS`）。
- [过渡期（源端修复上线前）traceId 空值可能使审计消息违约进 DLQ] → `AuditLogConsumer` 兜底生成 + warn；上线顺序上先发服务端再无存量风险（见 Migration Plan）。
- [删除 auth/model 手写 listener 属行为收敛] → payload 字段结构逐字段对齐现状，audit 消费端零改动；tasks 中含回归验证项。
- [MDC 清理误伤线程池复用] → `TraceContextFilter`/`TraceMessageHelper` 只 remove 自己注入的固定 key 集合，不整体 `clear()`。
- [aiworker Celery 各任务入参形态不一致，trace 继承可能漏接] → tasks 先盘点全部任务的消息结构，统一从 `MaidcMessage` 顶层字段取，缺口任务单独标注处理。
- [前端每请求独立 traceId 无法串联同一操作序列] → 列为非目标；未来可扩展 session 级 parent-traceId，不影响本次契约。
- [trace_id 精确查询在大表上的性能] → 沿用/新增 B-tree 索引且为等值查询；partial index 控制体积。

## Migration Plan

1. **数据库**：执行 `docker/init-db/22-audit-trace.sql`（幂等）。
2. **audit 服务**：先发布（兼容旧消息与新消息，含兜底逻辑）。
3. **common 与 8 个 Java 服务**：滚动发布（互相兼容：新增 header/字段对旧版本透明）。
4. **网关**：发布修复后的 `TraceFilter`（此后 traceId 开始真正下发，全链路点亮）。
5. **aiworker**：发布 Python 侧改动。
6. **portal**：最后发布前端（新增列与查询）。
7. **回滚策略**：所有组件均为增量（新 Filter、新列可空、消息字段 Optional），按镜像回滚即可，无需数据回滚；`trace_id` 列保留无害。

## Open Questions

- ELK（logstash appender）启用时间点与目标索引保留策略 → 本变更不决策，P1 另行评估。
- 聚合链路端点 `GET /api/v1/audit/traces/{traceId}` 是否进入 P0 → 默认 P1；若排障体验反馈强烈可提前。
