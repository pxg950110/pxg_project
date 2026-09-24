# 01 项目全景与架构

## 定位

MAIDC = **Medical AI Data Center**，医疗 AI 模型全生命周期管理平台：数据接入 → 标准化 → 科研检索 → 标注 → 训练 → 部署 → 审计（README.md:1-16、docs/code-structure.md §1）。

## 技术栈总览

| 层 | 技术 |
|----|------|
| 后端 | Java 17 + Spring Boot 3.2.5 + Spring Cloud Alibaba 2023.0.1（Maven 多模块 `maidc-parent/`） |
| 前端 | Vue 3.4 + TypeScript 5 + Vite 5 + Ant Design Vue 4 + Pinia + ECharts（`maidc-portal/`） |
| AI 推理 | Python 3.11 + FastAPI + Celery（`maidc-parent/maidc-aiworker/`，独立 pyproject） |
| 存储 | PostgreSQL 15（多 Schema：system / model / cdr / rdr / audit / masterdata / ods_*）+ Redis 7 + MinIO |
| 中间件 | RabbitMQ 3.12 + Nacos 2.3 |

## 微服务拓扑（maidc-parent/，端口见 vite proxy maidc-portal/vite.config.ts:15-39）

| 服务 | 端口 | 职责 |
|------|------|------|
| maidc-gateway | （入口） | 路由 + 鉴权过滤器；AuthFilter 透传 `X-User-Id/X-Org-Id/X-User-Roles/X-Username` |
| maidc-auth | 8081 | 登录/JWT/用户/角色/权限；PermissionCacheService（Redis 权限集） |
| maidc-data | 8082 | **最大模块**：主数据/CDR/RDR/ETL/智能检索/专病/随访 + `com.maidc.task` 包（工作台/调度） |
| maidc-model | 8083 | 模型全生命周期 + 告警 |
| maidc-task | 8084 | （代码位于 maidc-data 的 com.maidc.task 包，同进程） |
| maidc-label | 8085 | 标注 |
| maidc-audit | 8086 | 操作/数据访问/系统事件审计 |
| maidc-msg | 8087 | 站内信 + WebSocket；消费 `maidc.msg` exchange 的 `system.notify` 队列转站内信 |
| maidc-aiworker | Python | `/v1/infer/{model_code}`、`/llm/summary`、`/embedding`、`/rag/chat`（app/api/llm.py:58-94、inference.py:27） |

公共库 `maidc-parent/common/`：common-core（R/PageResult/ErrorCode）、common-redis、common-minio、common-mq（MaidcMessage）、common-log（@OperLog AOP）、common-security（JWT/CurrentUser/DataScope/脱敏）、common-jpa（BaseEntity：id/createdBy/createdAt/updatedBy/updatedAt/isDeleted/orgId，BaseEntity.java:20-46）。

## 数据分层（医疗数仓三层）

```
ODS（mimic3/4 原始镜像）--ETL--> CDR（患者360/检索/质控/术语映射/随访/知识库）--抽取--> RDR（队列/数据集/特征）
```

## docker/init-db 脚本清单（按编号顺序执行）

| # | 内容 |
|---|------|
| 01-05 | schemas / system / model / cdr / rdr 基础 DDL |
| 06-11 | audit / ods(mimic) / cdr-patch / ods-reduce |
| 12-14 | cdr-etl / datasource-type / etl-edge / smart-search FTS |
| 15-16 | disease-cohort / personal-task（system.t_personal_task） |
| 17 | masterdata + **17-permission-system**（8 角色 + 55 权限码 + 角色×权限矩阵 + 脱敏豁免） |
| 18 | cdr-document-template / audit-event-type（PERMISSION_DENIED 枚举扩展） |
| 19-20 | disease-kb（5 表 + FTS/HNSW + cdr:diseasekb 权限码）/ disease-followup（6 表 + 5 量表种子 + disease:followup:* 3 权限码） |
| 21 | cohort-event（cdr.c_cohort_event，工作台 cohortDigest 数据源；**2026-09-11 新增，需手动执行**） |

## MQ 拓扑（工作台相关）

| 队列/exchange | 生产者 → 消费者 | 用途 |
|---|---|---|
| `approval.notify` / `label.notify` | model/label → maidc-task PersonalTaskConsumer（PersonalTaskConsumer.java:20-29） | 生成 personal_task 待办 |
| `maidc.msg` exchange / `system.notify` 队列 | maidc-data FollowupNotifyProducer → maidc-msg AlertNotifyConsumer | 随访提醒转站内信（FollowupNotifyProducer.java:10-11） |
| PERMISSION_DENIED 审计事件 | 各服务 → maidc-audit（MQ 发布） | 越权审计 |
