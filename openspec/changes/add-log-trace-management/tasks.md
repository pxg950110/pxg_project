## 1. common 层 TraceContext 基础设施（common-log）

- [x] 1.1 新增 `TraceIds` 工具类：generate（32 位小写 hex）、normalize（去横线、合法性校验）、isValid，以及 traceId/userId/orgId 头名与 MDC 键常量
- [x] 1.2 新增 `TraceContextFilter`（OncePerRequestFilter，最高优先级）：X-Trace-Id/X-User-Id/X-Org-Id/X-Username → MDC，finally 仅 remove 注入的固定 key 集合
- [x] 1.3 新增 `TraceRestTemplateCustomizer`（ClientHttpRequestInterceptor）：自动向 RestTemplate 请求写入 MDC 中的 traceId 与用户上下文头
- [x] 1.4 新增 `OperationLogEventForwarder`（@EventListener + @ConditionalOnBean(RabbitTemplate) + @ConditionalOnProperty maidc.audit.forward.enabled，matchIfMissing=true）：payload 字段结构与 auth 现有 listener 逐字段对齐，发 maidc.audit/audit.operation，serviceName 取 spring.application.name
- [x] 1.5 `OperationLogAutoConfiguration` 注册上述 Bean；自动装配从 spring.factories 迁移到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（保留 spring.factories 过渡）
- [x] 1.6 common-mq 新增 `TraceMessageHelper.restore(message)/clear()`（MDC 还原 + 空值兜底生成）

## 2. 网关与 Java 服务接入

- [x] 2.1 修复 `maidc-gateway/filter/TraceFilter`：chain.filter 使用 mutate 后的 exchange、响应头回写 X-Trace-Id、MDC.put + doFinally 清理、入站头规范化/非法重生成
- [x] 2.2 移除 `maidc-auth/OperationLogEventListener` 与 `maidc-model/ModelOperationLogEventListener`，确认公共转发器接管（行为等价）
- [x] 2.3 `maidc-model/AiWorkerClient` 改用注入的 RestTemplateBuilder 构建 RestTemplate（获得透传拦截器）
- [x] 2.4 盘点并接入未继承 BaseMessageConsumer 的消费者（PersonalTaskConsumer、AlertNotifyConsumer、EvaluationResultConsumer 等）消费入口的 TraceMessageHelper.restore/clear
- [x] 2.5 `AuditLogConsumer` 增加 traceId 空白兜底（生成 + log.warn），防止过渡期消息进 DLQ

## 3. 数据库与审计查询 API（maidc-audit）

- [x] 3.1 新增 `docker/init-db/22-audit-trace.sql`（幂等）：a_data_access_log、a_system_event 加 `trace_id VARCHAR(64)` + partial index（WHERE trace_id IS NOT NULL）
- [x] 3.2 audit 实体（DataAccessLogEntity、SystemEventEntity）补 traceId 字段映射
- [x] 3.3 三个查询 DTO（Audit/DataAccess/SystemEvent）加 traceId 条件，Repository/查询逻辑实现等值过滤，三个分页接口返回体回显 traceId

## 4. Python aiworker 接入

- [x] 4.1 新增 `app/core/trace.py`：ContextVar + generate/normalize（格式与 Java TraceIds 一致）
- [x] 4.2 FastAPI 中间件：读取/生成 X-Trace-Id 写入 ContextVar，响应头回写 X-Trace-Id；logging.Filter 注入每条 LogRecord
- [x] 4.3 新增 `app/core/logging.py` dictConfig 统一日志（含 traceId 字段格式），替换 main.py 与各模块 `print()` 为 logging
- [x] 4.4 Celery 接入：task_prerun/task_postrun 信号继承/清理 traceId（盘点全部任务的 MaidcMessage 入参结构）；结果回发消息（evaluation.py basic_publish）携带继承的 traceId，废除 `eval-{id}` 伪造

## 5. 门户前端（maidc-portal，深色科技风）

- [x] 5.1 `src/api/audit.ts`：三个日志查询类型加 traceId 条件与返回字段
- [x] 5.2 OperationLog.vue：traceId 列（monospace 缩略 + 点击复制 + 成功提示）、查询表单 traceId 条件
- [x] 5.3 DataAccessLog.vue 与 SystemEventLog.vue：同样增加 traceId 列与查询条件
- [x] 5.4 AuditDetailDrawer：详情展示完整 traceId 并支持一键复制
- [x] 5.5 视觉核对：traceId chip/控件配色遵循 tech-style-guide（暗夜蓝黑底 + #22d3ee 点缀），与现有审计页风格一致

## 6. 配置与可选模板（P1）

- [x] 6.1 核对 `docker/nacos-config/maidc-shared.yaml` 日志 pattern 与 MDC 键一致（已有 %X{traceId}/%X{userId}/%X{orgId}，无需变更则仅确认）
- [x] 6.2 （P1 可选）提供启用 `logstash` profile 的 logback-spring.xml 模板（LogstashTcpSocketAppender → TCP 5044，字段映射对齐 monitoring/elk/logstash.conf），默认不启用

## 7. 验证与部署

- [x] 7.1 后端构建：`mvn -q -pl common/common-log,common/common-mq,maidc-gateway,maidc-auth,maidc-model,maidc-data,maidc-audit,maidc-task,maidc-label,maidc-msg -am package`（或全量 install）通过
- [x] 7.2 单元/集成验证：TraceContextFilter 注入与清理、TraceIds 规范化、转发器 payload 与 audit 消费契约一致
- [x] 7.3 全链路手工验证（docker compose 部署后）：前端登录 → 触发一个 data 服务 @OperLog 操作 → 用响应体 traceId 在审计页过滤出该操作日志 → 用同一 traceId 在网关/服务/aiworker 控制台日志命中 → 触发一次模型评测验证 Java→MQ→Celery→回传消息 traceId 一致
- [x] 7.4 部署：执行 22-audit-trace.sql → `cd docker && docker compose -f docker-compose-full.yml up -d --build`；`docker ps` 健康检查 + 通知用户按 7.3 步骤验证
