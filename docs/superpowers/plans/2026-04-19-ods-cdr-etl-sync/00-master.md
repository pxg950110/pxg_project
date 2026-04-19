# ODS→CDR ETL 同步管理 - 总览

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement.

**Goal:** 构建完整的 ODS→CDR ETL 管道管理系统，支持可视化配置、字段映射、Embulk 引擎执行和执行监控。

**Architecture:** Pipeline-Step 两层模型。Pipeline 对应一个数据源→CDR 的完整链路，Pipeline 内含多个 Step（每组表映射）。可插拔引擎架构，默认 Embulk。前后端分离，后端 Spring Boot + JPA，前端 Vue 3 + Ant Design Vue。

**Tech Stack:** Java 17 / Spring Boot 3.2 / JPA / MapStruct / PostgreSQL / Embulk / Vue 3 / TypeScript / Ant Design Vue 4

---

## 阶段划分

| 阶段 | 文件 | 范围 | 依赖 |
|------|------|------|------|
| Phase 1 | [01-ddl-entity.md](01-ddl-entity.md) | DDL + Entity + Repository + DTO/VO + Mapper | 无 |
| Phase 2 | [02-service.md](02-service.md) | 6个 Service 类 | Phase 1 |
| Phase 3 | [03-controller.md](03-controller.md) | 5个 Controller + ETL 引擎接口 | Phase 2 |
| Phase 4 | [04-frontend-api.md](04-frontend-api.md) | 前端 API 层 + 路由配置 | Phase 3 |
| Phase 5 | [05-frontend-pipeline-list.md](05-frontend-pipeline-list.md) | ETL 管道列表页 | Phase 4 |
| Phase 6 | [06-frontend-pipeline-config.md](06-frontend-pipeline-config.md) | 管道配置页（核心交互） | Phase 5 |
| Phase 7 | [07-frontend-execution.md](07-frontend-execution.md) | 执行监控页 | Phase 4 |

## 关键约束

- **无外键约束**：所有表不使用 FK，业务层保证数据完整性
- **Schema**：ETL 管道相关表放 `cdr` schema，表名前缀 `r_etl_`
- **软删除**：`is_deleted` 字段，`@Where` + `@SQLDelete` 注解
- **审计字段**：继承 `BaseEntity`（id, created_by, created_at, updated_by, updated_at, is_deleted, org_id）
- **JSONB**：使用 `JsonNodeConverter` 处理 JSONB 列
- **响应包装**：统一使用 `R<T>` 包装
- **MapStruct**：Entity → VO 转换
