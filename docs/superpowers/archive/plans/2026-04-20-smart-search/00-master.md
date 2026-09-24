# CDR/RDR 智能全文检索引擎 - 总计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 PostgreSQL zhparser 全文检索构建跨 13 个域的统一智能搜索引擎

**Architecture:** PostgreSQL GIN 索引 + tsvector 生成列实现毫秒级中文全文检索。后端通过 UNION ALL 原生 SQL 一次性跨域搜索，ts_rank_cd 排序。前端重写为单搜索框 + 统一结果列表。

**Tech Stack:** PostgreSQL 15 + zhparser | Spring Boot + JPA Native Query | Vue 3 + Ant Design Vue

**Spec:** `docs/superpowers/specs/2026-04-20-smart-search-design.md`

---

| Plan | 内容 | 文件 |
|------|------|------|
| 01-ddl | zhparser 安装 + tsvector 列 + GIN 索引 | `01-ddl.md` |
| 02-backend | DTO + Service + Controller | `02-backend.md` |
| 03-frontend | API + Vue 搜索页重写 | `03-frontend.md` |

## 依赖关系

```
01-ddl (数据库基础设施)
  └→ 02-backend (后端 API，依赖 tsvector 列)
       └→ 03-frontend (前端界面，依赖后端 API)
```

## 重要说明：数据库列名

Native SQL 查询必须使用数据库实际列名（snake_case）。以下列名来自 DDL 脚本 `docker/init-db/04-cdr.sql` 和 `05-rdr.sql`：

| 表 | 搜索列（用于 tsvector） |
|---|---|
| cdr.c_patient | name, phone |
| cdr.c_encounter | doctor_name, diagnosis_name, dept_name |
| cdr.c_diagnosis | icd_name, icd_code |
| cdr.c_lab_test | test_name, test_code, ordering_doctor |
| cdr.c_medication | med_name, med_code, prescriber |
| cdr.c_imaging_exam | exam_type, body_part, report_text |
| cdr.c_operation | operation_name, operation_code, surgeon |
| cdr.c_pathology | diagnosis_desc, specimen_type |
| cdr.c_vital_sign | sign_type |
| cdr.c_allergy | allergen, reaction |
| cdr.c_clinical_note | title, content, author |
| rdr.r_study_project | project_name, description |
| rdr.r_dataset | dataset_name, description |
