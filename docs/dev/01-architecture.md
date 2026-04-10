# MAIDC 基础架构文档

> **版本**: v1.0
> **日期**: 2026-04-11
> **状态**: 已确认
> **关联**: PRD `docs/superpowers/specs/2026-04-08-maidc-design.md`

---

## 目录

1. [系统全景架构图](#1-系统全景架构图)
2. [微服务通信机制](#2-微服务通信机制)
3. [数据架构](#3-数据架构)
4. [网关设计](#4-网关设计)
5. [认证鉴权架构](#5-认证鉴权架构)
6. [可观测性](#6-可观测性)

---

## 1. 系统全景架构图

### 1.1 整体分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│   Web Portal (Vue3)  │  移动端  │  第三方系统 (HIS/PACS)     │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS
┌──────────────────────────▼──────────────────────────────────┐
│                   Spring Cloud Gateway (:8080)               │
│            (路由 / 限流 / 鉴权 / 灰度发布)                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     Nacos (:8848) 注册/配置中心               │
└──┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬─────────┘
   │      │      │      │      │      │      │      │
┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──┐┌──▼──────┐
│auth ││data ││model││task ││label││audit││msg  ││AI Worker│
│:8081││:8082││:8083││:8084││:8085││:8086││:8087││  :8090  │
│Java ││Java ││Java ││Java ││Java ││Java ││Java ││ Python  │
└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└────┬────┘
   │      │      │      │      │      │      │         │
┌──▼──────▼──────▼──────▼──────▼──────▼──────▼─────────▼─────┐
│                     基础设施层                                │
│  PostgreSQL:5432 │ Redis:6379 │ MinIO:9000 │ RabbitMQ:5672  │
│  XXL-JOB │ Prometheus:9090 │ Grafana:3000 │ ELK             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 8个微服务一览

| 服务 | 端口 | 技术栈 | 数据 Schema | Phase |
|------|------|--------|-------------|-------|
| maidc-gateway | 8080 | Spring Cloud Gateway | - | 1 |
| maidc-auth | 8081 | Spring Boot 3.x + Spring Data JPA | system | 1 |
| maidc-data | 8082 | Spring Boot 3.x + Spring Data JPA | cdr + rdr | 2 |
| **maidc-model** | **8083** | **Spring Boot 3.x + Spring Data JPA** | **model** | **1** |
| maidc-task | 8084 | Spring Boot 3.x + XXL-JOB | 独立 | 3 |
| maidc-label | 8085 | Spring Boot 3.x + Spring Data JPA | rdr | 4 |
| maidc-audit | 8086 | Spring Boot 3.x + Spring Data JPA | audit | 4 |
| maidc-msg | 8087 | Spring Boot 3.x + Spring Data JPA | 独立消息表 | 4 |
| maidc-aiworker | 8090 | FastAPI + Celery | model (只读) | 1 |

### 1.3 服务依赖关系

```
maidc-gateway
    ├── maidc-auth        （JWT鉴权）
    ├── maidc-model       ★Phase 1
    │      ├── maidc-auth      （权限校验）
    │      ├── maidc-data      （关联数据集）
    │      └── maidc-aiworker  （异步评估/推理）
    ├── maidc-data
    │      └── maidc-auth
    ├── maidc-task
    │      └── maidc-auth
    ├── maidc-label
    │      ├── maidc-auth
    │      └── maidc-data
    ├── maidc-audit
    │      └── maidc-auth
    └── maidc-msg
           └── maidc-auth
```

---

## 2. 微服务通信机制

### 2.1 通信方式总览

| 通信方式 | 场景 | 协议 | 框架 |
|----------|------|------|------|
| OpenFeign | 服务间同步调用（权限校验、数据查询） | HTTP/JSON | Spring Cloud OpenFeign |
| RabbitMQ | 异步长时任务（评估、ETL、通知） | AMQP | Spring AMQP |
| HTTP | AI Worker 同步推理 | HTTP/JSON | FastAPI |
| WebSocket | 实时消息推送（站内通知） | WS | Spring WebSocket |

### 2.2 OpenFeign 同步调用配置

各服务通过 OpenFeign 声明式调用其他服务：

```java
// maidc-model 中的 Feign 客户端示例
@FeignClient(name = "maidc-auth", path = "/api/v1")
public interface AuthClient {
    @GetMapping("/users/{id}")
    R<UserVO> getUser(@PathVariable("id") Long id);

    @GetMapping("/permissions/check")
    R<Boolean> checkPermission(@RequestParam("permissionCode") String code);
}

@FeignClient(name = "maidc-data", path = "/api/v1")
public interface DataClient {
    @GetMapping("/rdr/datasets/{id}")
    R<DatasetVO> getDataset(@PathVariable("id") Long id);
}
```

**Feign 超时与重试策略**:

```yaml
# application.yml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: BASIC
  # Sentinel 整合（熔断降级）
  sentinel:
    enabled: true
```

### 2.3 RabbitMQ Exchange/Queue 设计

#### Exchange 设计

| Exchange 名称 | 类型 | 用途 |
|---------------|------|------|
| `maidc.model` | Direct | 模型管理消息 |
| `maidc.data` | Direct | 数据管理消息 |
| `maidc.alert` | Topic | 告警通知 |
| `maidc.msg` | Topic | 消息通知 |
| `maidc.dlx` | Direct | 死信队列 |

#### Queue 设计

| Queue 名称 | 绑定 Exchange | Routing Key | 消费者 | 用途 |
|------------|---------------|-------------|--------|------|
| `model.evaluation` | maidc.model | evaluation | aiworker (Celery) | 模型评估任务 |
| `model.inference.batch` | maidc.model | inference.batch | aiworker (Celery) | 批量推理 |
| `model.evaluation.result` | maidc.model | evaluation.result | maidc-model | 评估结果回调 |
| `model.alert` | maidc.model | alert | maidc-msg | 模型告警通知 |
| `data.etl` | maidc.data | etl | maidc-data | ETL 任务执行 |
| `data.quality` | maidc.data | quality | maidc-data | 质量检测 |
| `alert.notify` | maidc.alert | alert.# | maidc-msg | 告警分发 |
| `msg.email` | maidc.msg | msg.email | maidc-msg | 邮件发送 |
| `msg.sms` | maidc.msg | msg.sms | maidc-msg | 短信发送 |
| `msg.push` | maidc.msg | msg.push | maidc-msg | 站内推送 |
| `dlq.model` | maidc.dlx | # | 人工处理 | 模型相关死信 |

#### 消息体格式

```json
{
    "traceId": "a1b2c3d4-e5f6-7890",
    "eventType": "MODEL_EVALUATION",
    "payload": {
        "evaluationId": 20,
        "modelVersionId": 10,
        "datasetId": 5
    },
    "timestamp": "2026-04-11T10:00:00Z",
    "source": "maidc-model"
}
```

### 2.4 Celery Worker 队列设计（AI Worker 侧）

| 队列名 | 用途 | 优先级 |
|--------|------|--------|
| `inference` | 实时推理任务 | 高 |
| `evaluation` | 模型评估 | 中 |
| `preprocessing` | 数据预处理 | 低 |
| `batch_inference` | 批量推理 | 低 |

### 2.5 Nacos 服务注册与配置

**注册发现**:
- 各服务启动时自动注册到 Nacos
- 心跳间隔：5s，不健康阈值：15s，剔除时间：30s
- 服务间通过服务名（如 `maidc-auth`）调用，Nacos 自动解析实例

**配置管理**:
- `maidc-{service}.yaml` — 各服务独立配置
- `maidc-shared.yaml` — 公共配置（Redis/MinIO/RabbitMQ连接信息）
- 配置变更通过 Nacos 推送，服务端监听实时生效

---

## 3. 数据架构

### 3.1 PostgreSQL 单库多 Schema

```
maidc (单库)
├── cdr      ← 临床数据仓库 (28张表) — maidc-data 读写
├── rdr      ← 研究数据仓库 (19张表) — maidc-data 读写
├── model    ← 模型管理 (10张表)      — maidc-model 读写 / aiworker 只读
├── system   ← 系统管理 (7张表)       — maidc-auth 读写
└── audit    ← 审计日志 (3张表)       — maidc-audit 读写
```

**Schema 访问权限矩阵**:

| 服务 | cdr | rdr | model | system | audit |
|------|-----|-----|-------|--------|-------|
| maidc-auth | - | - | - | RW | - |
| maidc-data | RW | RW | - | R | - |
| maidc-model | R | R | RW | - | - |
| maidc-task | - | R | R | - | - |
| maidc-label | R | RW | - | - | - |
| maidc-audit | R | - | - | R | RW |
| maidc-msg | - | - | - | R | - |
| maidc-aiworker | - | R | R | - | - |

### 3.2 Redis 缓存策略

**Key 命名规范**:

```
maidc:{service}:{business}:{identifier}

示例:
maidc:auth:token:{userId}              ← JWT Token 缓存, TTL=2h
maidc:auth:permissions:{userId}        ← 用户权限列表, TTL=30min
maidc:auth:user:{userId}               ← 用户基本信息, TTL=1h
maidc:model:model:{modelId}            ← 模型详情, TTL=10min
maidc:model:status:{deploymentId}      ← 部署状态, TTL=30s
maidc:data:patient:{patientId}         ← 患者信息, TTL=1h
maidc:data:dict:{dictType}             ← 数据字典, TTL=24h
maidc:lock:model:eval:{evaluationId}   ← 分布式锁, TTL=1h
```

**缓存策略**:

| 数据类型 | 缓存模式 | TTL | 更新策略 |
|----------|----------|-----|----------|
| JWT Token | Write-Through | 2h | 主动失效(登出) |
| 用户权限 | Cache-Aside | 30min | 权限变更时主动失效 |
| 数据字典 | Cache-Aside | 24h | 字典变更时主动失效 |
| 模型详情 | Cache-Aside | 10min | 模型更新时主动失效 |
| 部署状态 | Write-Behind | 30s | 定时刷新 |
| 分布式锁 | — | 可配 | 自动过期 |

### 3.3 MinIO Bucket 设计

| Bucket 名称 | 用途 | 文件路径规范 |
|-------------|------|-------------|
| `maidc-models` | 模型文件 | `{org_id}/{model_code}/{version_no}/model.pt` |
| `maidc-dicom` | DICOM 影像 | `{org_id}/{year}/{month}/{day}/{accession_no}.dcm` |
| `maidc-datasets` | 数据集文件 | `{org_id}/{project_id}/{dataset_id}/v{version}.parquet` |
| `maidc-docs` | 审批材料/报告 | `{org_id}/{approval_id}/{filename}` |
| `maidc-reports` | 评估报告 | `{org_id}/{eval_id}/report.pdf` |

---

## 4. 网关设计

### 4.1 Spring Cloud Gateway 路由规则

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: maidc-auth
          uri: lb://maidc-auth
          predicates:
            - Path=/api/v1/auth/**, /api/v1/users/**, /api/v1/roles/**, /api/v1/permissions/**
          filters:
            - StripPrefix=0

        - id: maidc-model
          uri: lb://maidc-model
          predicates:
            - Path=/api/v1/models/**, /api/v1/evaluations/**, /api/v1/approvals/**, /api/v1/deployments/**, /api/v1/routes/**, /api/v1/inference/**, /api/v1/monitoring/**, /api/v1/alert-rules/**, /api/v1/alerts/**
          filters:
            - StripPrefix=0

        - id: maidc-data
          uri: lb://maidc-data
          predicates:
            - Path=/api/v1/cdr/**, /api/v1/rdr/**, /api/v1/etl/**, /api/v1/quality/**, /api/v1/dict/**, /api/v1/desensitize/**
          filters:
            - StripPrefix=0

        - id: maidc-task
          uri: lb://maidc-task
          predicates:
            - Path=/api/v1/tasks/**, /api/v1/schedules/**
          filters:
            - StripPrefix=0

        - id: maidc-label
          uri: lb://maidc-label
          predicates:
            - Path=/api/v1/labels/**, /api/v1/annotations/**
          filters:
            - StripPrefix=0

        - id: maidc-audit
          uri: lb://maidc-audit
          predicates:
            - Path=/api/v1/audit/**, /api/v1/events/**
          filters:
            - StripPrefix=0

        - id: maidc-msg
          uri: lb://maidc-msg
          predicates:
            - Path=/api/v1/messages/**, /api/v1/notifications/**, /api/v1/templates/**
          filters:
            - StripPrefix=0
```

### 4.2 全局过滤器链

```
请求进入 Gateway
    │
    ▼
┌──────────────────────────────────┐
│ 1. CorsFilter                    │  ← 跨域处理
├──────────────────────────────────┤
│ 2. TraceFilter                   │  ← 生成/透传 traceId (MDC)
├──────────────────────────────────┤
│ 3. AuthFilter                    │  ← JWT 校验 (白名单路径跳过)
│    - 解析 Token → userId/roles   │
│    - 注入 Header: X-User-Id     │
│    - 注入 Header: X-Org-Id      │
├──────────────────────────────────┤
│ 4. RateLimiterFilter             │  ← Sentinel 限流
├──────────────────────────────────┤
│ 5. RequestLogFilter              │  ← 记录请求日志（方法/URL/耗时）
└──────────────────────────────────┘
    │
    ▼
路由到后端服务
```

**白名单路径（跳过鉴权）**:
```
/api/v1/auth/login
/api/v1/auth/refresh
/api/v1/auth/captcha
/actuator/**
```

### 4.3 统一响应封装

```json
{
    "code": 200,
    "message": "success",
    "data": { },
    "trace_id": "a1b2c3d4-e5f6-7890"
}
```

---

## 5. 认证鉴权架构

### 5.1 JWT Token 签发/刷新/吊销流程

```
┌────────┐     POST /auth/login      ┌───────────┐
│ 客户端  │ ──────────────────────────▶│ maidc-auth│
│        │     {username, password}   │           │
│        │◀────────────────────────── │           │
│        │     {access_token,         │  校验密码  │
│        │      refresh_token}        │  签发JWT  │
└────────┘                            └───────────┘
    │
    │ 携带 access_token 访问资源
    ▼
┌────────────────┐   校验JWT    ┌───────────┐
│ Gateway         │────────────▶│ Redis      │
│ AuthFilter      │  检查是否    │ Token黑名单 │
│                 │  在黑名单中  │            │
└────────────────┘              └───────────┘
    │ Token过期?
    │
    ▼ POST /auth/refresh
┌────────────────┐   刷新Token   ┌───────────┐
│ 客户端          │─────────────▶│ maidc-auth│
│                │◀─────────────│           │
│                │  新access_token│  验证refresh│
└────────────────┘              └───────────┘
```

**Token 规格**:

| 属性 | Access Token | Refresh Token |
|------|-------------|---------------|
| 有效期 | 2小时 | 7天 |
| 存储 | 客户端内存 | HttpOnly Cookie |
| 吊销 | Redis 黑名单 `maidc:auth:token:blacklist:{jti}` | 同左 |
| 载荷 | userId, username, roles, orgId | userId, jti |

### 5.2 RBAC 权限模型

```
用户 (s_user)
  │ N:M
  ▼
角色 (s_role) ─── 系统内置: admin/data_admin/researcher/ai_engineer/doctor/auditor
  │ N:M
  ▼
权限 (s_permission)
  ├── 菜单权限 (resource_type=MENU)
  ├── 按钮权限 (resource_type=BUTTON)
  ├── API权限  (resource_type=API)
  └── 数据权限  (resource_type=DATA)
```

**数据权限（org_id 行级隔离）**:
- 所有业务表包含 `org_id` 字段
- 用户的 `org_id` 在 JWT 中携带
- MyBatis/JPA 拦截器自动追加 `WHERE org_id = ?`

### 5.3 权限校验流程

```java
// 注解式权限控制
@PreAuthorize("hasPermission('model:create')")
@PostMapping("/models")
public R<ModelVO> createModel(@RequestBody @Valid ModelCreateDTO dto) { ... }

// 数据权限自动过滤
// 所有查询自动追加 org_id 条件，由 AuditorAware + JPA Filter 实现
```

---

## 6. 可观测性

### 6.1 TraceId 链路追踪方案

```
Gateway 生成 TraceId (UUID)
    │
    ├── HTTP Header: X-Trace-Id 透传到下游服务
    ├── MDC.put("traceId", traceId) → 日志自动包含
    ├── RabbitMQ 消息体包含 traceId
    └── 响应体返回 trace_id 字段
```

**日志格式**:
```
[2026-04-11 10:00:00.000] [traceId:a1b2c3d4] [userId:1] [orgId:1] INFO  c.m.m.controller.ModelController - 创建模型: LUNG_NODULE_DET_001
```

### 6.2 Prometheus 指标采集点规划

| 服务 | 采集指标 | 采集间隔 |
|------|----------|----------|
| Gateway | 请求总数、响应时间、错误率、QPS | 15s |
| maidc-model | 模型操作次数、评估任务数、部署数 | 30s |
| maidc-aiworker | 推理QPS、推理延迟(P50/P95/P99)、GPU利用率 | 10s |
| PostgreSQL | 连接数、慢查询、表大小、缓存命中率 | 30s |
| Redis | 内存使用、命中率、连接数 | 15s |
| RabbitMQ | 队列深度、消费速率、消息堆积 | 15s |

### 6.3 Grafana 面板规划

| 面板名称 | 内容 | 面向角色 |
|----------|------|----------|
| MAIDC 系统总览 | 全服务健康状态、QPS、错误率 | 运维 |
| 模型推理监控 | 推理QPS、延迟分布、GPU利用率、部署状态 | AI工程师 |
| 数据库监控 | 连接池、慢查询Top10、表大小趋势 | DBA |
| 基础设施监控 | CPU/内存/磁盘/网络 | 运维 |

### 6.4 ELK 日志规范

**日志等级使用**:
- `ERROR`: 异常堆栈、需要立即处理的问题
- `WARN`: 慢查询、重试、接近阈值的警告
- `INFO`: 关键业务操作（创建模型、部署、审批等）
- `DEBUG`: 详细调试信息（生产默认关闭）

**日志索引策略**:
```
maidc-log-{yyyy.MM.dd}    ← 按天滚动
保留周期: 30天热数据 → 90天温数据 → 归档冷数据
```

---

> **文档结束** - MAIDC 基础架构文档 v1.0
