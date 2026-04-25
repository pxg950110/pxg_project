# 医疗主数据系统 — 实施总览

> Spec: `docs/superpowers/specs/2026-04-25-medical-masterdata-design.md`

## 依赖图

```
01-ddl-core (基础)
  ├── 02-hierarchy (依赖01)
  ├── 03-clinical-rules (依赖01)
  ├── 04-local-mapping (依赖01)
  └── 05-import (依赖01)
06-frontend (依赖01-04全部完成)
```

## 子计划清单

| # | 计划 | 产出 | 文件 |
|---|------|------|------|
| 01 | DDL + 核心CRUD | schema + CodeSystem/Concept 全栈API | 01-ddl-core.md |
| 02 | 层级与映射 | Relationship/Ancestor/Synonym + 跨编码映射 | 02-hierarchy.md |
| 03 | 临床规则 | ReferenceRange + DrugInteraction | 03-clinical-rules.md |
| 04 | 本地编码映射 | Institution + LocalConcept + 翻译API | 04-local-mapping.md |
| 05 | 导入管道 | 异步导入 + 进度查询 | 05-import.md |
| 06 | 前端页面 | 5个管理页面 + 路由菜单 | 06-frontend.md |

## 执行顺序

1. 01 → 02、03、04 可并行 → 05 → 06
2. 每个计划完成后 commit，更新本文档状态

## 技术约束

- 实体包: `com.maidc.data.entity`（扁平结构）
- Service包: `com.maidc.data.service`
- Controller包: `com.maidc.data.controller`
- Schema: `masterdata`（新建）
- 不用Flyway，用 `docker/init-db/17-masterdata.sql` + `ddl-auto: update`
- Entity 继承 `BaseEntity`（含 id/createdBy/createdAt/updatedBy/updatedAt/isDeleted/orgId）
- Controller 返回 `R<T>`
- 权限: `hasPermission('masterdata:read')` / `hasPermission('masterdata:create')`
