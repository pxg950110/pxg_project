# CDR 全局临床搜索引擎 - 总计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增统一临床搜索引擎，提供单一 API 跨 11 个临床数据域的高级检索

**Architecture:** 后端新增 ClinicalSearchController/Service，通过 JPA Specification 动态查询现有 Repository，结果统一为 Map 返回。前端新增独立搜索页面，高级表单 + 动态表格。

**Tech Stack:** Spring Boot + JPA Specification（后端）| Vue 3 + Ant Design Vue（前端）

**Spec:** `docs/superpowers/specs/2026-04-20-clinical-search-design.md`

---

| Plan | 内容 | 文件 |
|------|------|------|
| 01-backend | DTO + Specification + Service + Controller | `01-backend.md` |
| 02-frontend | API + Vue 搜索页 + 路由 | `02-frontend.md` |

## 依赖关系

```
01-backend (后端 API)
  └→ 02-frontend (前端界面，依赖后端 API)
```

## 文件清单

### 新建文件

| 文件 | 职责 |
|------|------|
| `dto/ClinicalSearchDomain.java` | 搜索域枚举 |
| `dto/ClinicalSearchRequest.java` | 搜索请求 DTO |
| `dto/ClinicalSearchResult.java` | 搜索结果 DTO |
| `service/spec/ClinicalSearchSpecs.java` | 11 个域的 JPA Specification 构建 |
| `service/ClinicalSearchService.java` | 搜索分发 + 查询 + 患者名解析 |
| `controller/ClinicalSearchController.java` | POST /api/v1/cdr/search |
| `maidc-portal/src/views/data-cdr/ClinicalSearch.vue` | 搜索主页面 |

### 修改文件

| 文件 | 变更 |
|------|------|
| `maidc-portal/src/api/data.ts` | 添加 clinicalSearch() |
| `maidc-portal/src/router/asyncRoutes.ts` | 添加 /data/cdr/search 路由 |
