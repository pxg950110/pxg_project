# 功能实现记录：通用日志链路管理（traceId 全链路贯穿）

## 基本信息

| 项目 | 内容 |
|------|------|
| 日期 | 2026-09-14 |
| 需求 | 通用日志链路管理（跨服务 traceId 生成/传递/落库/检索） |
| 分支 | feature/disease-kb（当前工作分支） |
| 设计文档 | openspec/changes/add-log-trace-management/（proposal/design/specs×4/tasks） |

## 背景与问题

traceId 的"载体"此前已存在（网关 TraceFilter、MQ 消息体字段、日志 pattern 占位符、审计表 trace_id 列），但链路是断的：

1. 网关 `TraceFilter` 有缺陷：`mutate()` 结果未传入 `chain.filter()`，X-Trace-Id 实际未下发，且网关不写 MDC；
2. 8 个 MVC 服务无「请求头 → MDC」过滤器：HTTP 日志 `%X{traceId}` 恒空、`R.traceId` 返回 null；
3. `AiWorkerClient` 裸 RestTemplate 跨服务调用不透传 trace 头；
4. aiworker（Python）无 trace 概念，Celery 结果消息伪造 `eval-{id}`；
5. data/task/label/msg 四服务 `@OperLog` 事件无 listener，审计静默丢失；
6. `a_data_access_log`/`a_system_event` 无 trace_id 列，审计查询/前端不支持按 traceId 检索。

## 设计要点（详见 design.md）

- **自研轻量 TraceContext**（MDC + Filter + 拦截器），不引入 micrometer-tracing/Zipkin；
- **traceId 统一 32 位小写 hex**（上限 64，对齐列宽），规则「有则沿用（规范化）无则生成」，网关为唯一规范生成点；
- **组件扫描 + AutoConfiguration.imports 双机制装配**（Boot 3.x 已忽略 spring.factories 的自动装配注册，实际生效靠 `scanBasePackages` 含 `com.maidc.common` 的组件扫描）；
- **审计转发收敛到 common-log** `OperationLogEventForwarder`（ObjectProvider 解析 RabbitTemplate 规避扫描装配顺序问题；payload 与既有契约逐字段一致）；
- **MQ traceId 保持消息体承载**；`TraceMessageHelper` 供非 Base 消费者还原 MDC（空值兜底生成防 DLQ）；
- **Python 侧** ContextVar + 纯 ASGI 中间件 + logging.Filter；Celery task_prerun/postrun 信号继承/清理，结果消息回传继承的 traceId。

## 修改文件清单

### 新增（Java）
| 文件 | 说明 |
|------|------|
| common-log/trace/TraceIds.java | traceId 生成/规范化/校验唯一实现 + 头名与 MDC 键常量 |
| common-log/filter/TraceContextFilter.java | OncePerRequestFilter：X-Trace-Id/X-User-Id/X-Org-Id/X-Username → MDC，finally 定向清理 |
| common-log/web/TraceRestTemplateCustomizer.java | RestTemplateCustomizer：跨服务 HTTP 自动透传链路头 |
| common-log/audit/OperationLogEventForwarder.java | 审计事件统一转发（含 logout 用户名 JWT 兜底，行为等价移植） |
| common-log 测试 ×3 | TraceIdsTest / TraceContextFilterTest / OperationLogEventForwarderTest（payload 契约断言） |
| common-mq/trace/TraceMessageHelper.java | 消费入口 MDC 还原 + 空值兜底（值匹配清理） |
| docker/init-db/22-audit-trace.sql | 两表加 trace_id + partial index（幂等） |
| monitoring/elk/logback-spring-logstash.xml | P1 ELK 采集模板（默认不启用） |

### 修改（Java）
| 文件 | 说明 |
|------|------|
| common-log/pom.xml | +common-mq（转发器需 MaidcMessage/RabbitTemplate）、slf4j-api、test 依赖 |
| common-log/config/OperationLogAutoConfiguration.java | 注册 Filter/Customizer/Forwarder 三 Bean；+AutoConfiguration.imports |
| common-mq/consumer/BaseMessageConsumer.java | 改用 TraceMessageHelper（兜底生成） |
| maidc-gateway/filter/TraceFilter.java | 修复 mutate 丢失缺陷 + 响应头回写 + MDC 值匹配清理 + 入站头规范化 |
| maidc-model/config/AiWorkerClient.java | 改用注入 RestTemplateBuilder（自动获得透传拦截器） |
| maidc-audit | 三查询 DTO/两实体/两 VO 加 traceId，Specification+Service 9 处调用点接通；AuditLogConsumer 空值兜底；PermissionDeniedConsumer 落 trace_id |
| maidc-data/task/consumer/PersonalTaskConsumer.java | 非基类消费者接入 restore/clear |

### 删除
| 文件 | 说明 |
|------|------|
| maidc-auth/listener/OperationLogEventListener.java、mq/AuditLogProducer.java | 被公共转发器取代（行为等价） |
| maidc-model/listener/ModelOperationLogEventListener.java、mq/ModelAuditLogProducer.java | 同上 |

### Python（maidc-aiworker）
| 文件 | 说明 |
|------|------|
| app/core/trace.py（新增） | ContextVar + generate/normalize + TraceIdFilter + 纯 ASGI TraceContextMiddleware |
| app/core/logging.py（新增） | dictConfig：console 含 traceId/userId，接管 uvicorn logger |
| app/core/celery_trace.py（新增） | task_prerun/postrun：headers/kwargs/首参 dict 继承 traceId |
| app/main.py、tasks/evaluation.py、inference_batch.py、preprocessing.py | print → logging；结果消息 traceId 改为继承值，废除 eval-{id} |

### 前端（maidc-portal）
| 文件 | 说明 |
|------|------|
| src/api/audit.ts | 三个查询/三个导出 params 加 traceId |
| src/views/audit/OperationLog.vue、DataAccessLog.vue、SystemEventLog.vue | Trace ID 列（monospace 缩略、点击复制、tooltip）+ 查询条件 + 导出透传 |
| src/components/AuditDetailDrawer/index.vue | 详情展示完整 traceId 可复制 |

## 部署顺序与验证

1. `docker init-db/22-audit-trace.sql` 已对存量库执行（幂等）；
2. `mvn package -DskipTests` 全量通过；common-log 单测通过（traceId 格式/MDC 注入清理/转发契约）；
3. `docker compose -f docker-compose-full.yml up -d --build` 全栈重建；
4. 全链路验证：登录 → 触发 @OperLog 操作 → 响应体 traceId 在审计页过滤命中 → 同一 traceId 在网关/服务/aiworker 日志命中 → 评测任务 Java→MQ→Celery→回传 traceId 一致。

## 待处理事项

- [ ] 代码提交（git-merge）与 PR
- [ ] P1：聚合链路端点 GET /api/v1/audit/traces/{traceId}；ELK 采集启用

---

# 服务合并（用户指示：不启用这么多微服务，可合并的合并）

## 合并结果

**Java 应用 8 → 2**：auth/task/label/audit/msg/data/model 七个 Servlet 服务合并为单应用 **maidc-app**（:8081），网关（WebFlux，技术栈不可直接并入）保留作统一入口。部署形态：portal → gateway → maidc-app → aiworker。

## 合并改动

| 类别 | 内容 |
|------|------|
| 新增模块 | maidc-app（唯一 boot 应用）：MaidcAppApplication + 统一 SecurityConfig（含 PasswordEncoder）+ 合并的 bootstrap/application-dev.yml（多 schema 搜索路径 URL）+ Dockerfile |
| pom | 7 个服务 pom 移除 spring-boot-maven-plugin（转纯 jar）；父 pom 增 maidc-app 模块；**app 依赖声明顺序即类路径顺序**：auth,task,label,audit,msg,data,model——使 com.maidc.task/com.maidc.label 的分叉副本以独立模块（网关路由的活跃版本）为准 |
| 删除 | 7 个 Application 启动类、7 个 SecurityConfig、AuthRabbitMqConfig、ModelAuditRabbitMqConfig（生产者已删的冗余审计声明）、data 的 DataJpaConfig（其 @EnableJpaRepositories/@EntityScan 限定会劫持全局扫描） |
| 配置 | 网关 7 条路由 uri 全部改指 lb://maidc-app；compose-full 7 个服务块替换为 maidc-app（--remove-orphans 清理旧容器）；spring.main.allow-bean-definition-overriding=true（msg/label 等模块重复声明同名 dlxExchange，定义一致） |
| 本地仓库 | 发现自定义 localRepository D:/javalib/repository 存放 4 月/9 月的旧 common 与旧 repackage jar，`mvn clean install` 全量覆盖（此前 -pl 单模块构建会嵌入旧依赖，教训：合并期一律全 reactor 构建） |

## 顺带修复的环境存量缺陷（阻塞验证，均非本次代码引入）

1. **Redis 密码漂移**：8 个服务 application-dev.yml 与 compose-infra 用旧密码 maidc_redis，compose-full 实际为 maidc123 → 统一为 maidc123（与 nacos seed 注释一致）
2. **JSONB 写入失败**：compose 的 SPRING_DATASOURCE_URL 丢失 stringtype=unspecified → 7 处 URL 补齐
3. **audit schema 漂移**：运行库 a_audit_log/a_data_access_log 为老结构（id varchar 无自增、module/ip/duration 列名）→ 按 06→18→22 脚本链重建（空表无数据损失）；operation 列过严 CHECK（只允许大写 CRUD 枚举）与 @OperLog 自由文本冲突 → 放宽（脚本+运行库）
4. **AuditLogConsumer org_id 兜底**：未认证请求（登录）orgId 为 null 撞 NOT NULL → 兜底 0（与 PermissionDeniedConsumer 一致）
5. **nacos 共享配置缺失**：运行 nacos 无 maidc-shared.yaml（日志 pattern 从未生效、redis 密码走本地旧值）→ 发布修正版 seed（acknowledge-mode manual 改 auto，manual 会导致无手动 ack 的消费者挂起）
6. **前端审计页字段契约过时**：三个页面对着废弃的老表结构字段（module/ip/duration/created_at/severity...）→ 全部对齐现行 VO 契约（serviceName/ipAddress/durationMs/SUCCESS-FAILURE/eventLevel...），系统事件页补真实详情抽屉
7. **admin 密码**：DataInitializer 每次启动重置为 Admin@123（既有 dev 行为，注意）

## 验证结果（合并部署后）

- [x] maidc-app 启动 13.4s，全部路由/消费者就绪（approval.notify×2、audit.operation、audit.permission.denied、label.notify、alert/system.notify、model.evaluation.result 各 1）
- [x] 跨模块接口抽测：auth 登录、audit 查询、masterdata、models、task/schedules、workspace/dashboard、label/tasks、messages 全部 200
- [x] 全链路 trace：登录 → 网关响应头/响应体 traceId → app 日志 [traceId:…] → 审计落库 trace_id 一致 → 审计页按 traceId 精确过滤（共 1 条）→ 详情抽屉完整展示
- [x] aiworker：请求头回写 X-Trace-Id、非法格式拒绝重生成、日志带 traceId
- [x] 门户视觉验收（judge）5/5 pass：Trace ID 列/检索框/详情展示/空态/风格一致
- [x] 容器收敛：gateway + maidc-app + aiworker + portal + 5 infra（原 10 个应用容器）
