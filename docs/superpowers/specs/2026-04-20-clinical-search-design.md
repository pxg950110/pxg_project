# CDR 全局临床搜索引擎 - 设计文档

> 日期: 2026-04-20
> 状态: 待实施

## 概述

在 CDR 模块新增一个统一临床搜索引擎，提供单一入口跨 11 个临床数据域的高级检索能力。采用高级搜索表单 + 表格结果的交互形式。

## 架构方案

**方案 A：统一搜索服务**（已选定）

新增 `ClinicalSearchService` 接收搜索条件，按 domain 分发到各现有 Service/Repository 查询，统一组装返回。前端只需对接一个 API。

不引入 ES 等新基础设施，复用现有 JPA 层。

## 搜索条件模型

所有条件均为可选，按需组合：

| 条件 | 字段 | 类型 | 说明 |
|------|------|------|------|
| 关键词 | `keyword` | String | 模糊匹配姓名/ID/编号 |
| 数据域 | `domain` | String (必填) | PATIENT/ENCOUNTER/DIAGNOSIS/LAB/MEDICATION/IMAGING/SURGERY/PATHOLOGY/VITAL/ALLERGY/NOTE |
| 日期起 | `dateFrom` | LocalDate | 筛选起始日期 |
| 日期止 | `dateTo` | LocalDate | 筛选结束日期 |
| 科室 | `department` | String | 就诊科室 |
| 诊断 | `diagnosis` | String | 诊断关键词 |
| 状态 | `status` | String | 就诊状态（门诊/住院/急诊） |
| 页码 | `page` | int | 默认 1 |
| 每页条数 | `pageSize` | int | 默认 20 |

### 搜索请求

```json
{
  "keyword": "张三",
  "domain": "PATIENT",
  "dateFrom": "2026-01-01",
  "dateTo": "2026-04-20",
  "department": "内科",
  "diagnosis": "糖尿病",
  "status": null,
  "page": 1,
  "pageSize": 20
}
```

### 统一响应

```json
{
  "domain": "PATIENT",
  "total": 156,
  "results": [ { "patientId": 1, "patientName": "张三", ... } ]
}
```

## 后端设计

### 新增文件

| 文件 | 职责 |
|------|------|
| `controller/ClinicalSearchController.java` | `POST /api/v1/cdr/search` 入口 |
| `service/ClinicalSearchService.java` | 按 domain 分发查询，组装结果 |
| `dto/ClinicalSearchRequest.java` | 搜索请求 DTO |
| `dto/ClinicalSearchResult.java` | 搜索结果 DTO（含 domain, total, results） |

### 数据流

```
ClinicalSearchController
  POST /api/v1/cdr/search
    → ClinicalSearchService.search(request)
         ├─ PATIENT   → PatientRepo (姓名/ID模糊)
         ├─ ENCOUNTER → EncounterRepo (科室+日期+状态)
         ├─ DIAGNOSIS → DiagnosisRepo (诊断关键词+日期)
         ├─ LAB       → LabTestRepo (项目关键词+日期)
         ├─ MEDICATION→ MedicationRepo (药品名+日期)
         ├─ IMAGING   → ImagingExamRepo (检查类型+日期)
         ├─ SURGERY   → OperationRepo (手术名+日期)
         ├─ PATHOLOGY → PathologyRepo (病理关键词+日期)
         ├─ VITAL     → VitalSignRepo (体征类型+日期)
         ├─ ALLERGY   → AllergyRepo (过敏原关键词)
         └─ NOTE      → ClinicalNoteRepo (文书内容+日期)
```

### 关键设计决策

1. **domain 必填** — 避免跨表 UNION 查询的性能问题
2. **复用现有 Repository** — 通过 JPA Specification 动态拼接条件，不修改现有方法
3. **结果统一为 Map** — 各域字段不同，用 `List<Map<String, Object>>` 返回，每条结果包含 `patientId` + `patientName` 用于跳转

## 前端设计

### 新增/修改文件

| 文件 | 操作 | 职责 |
|------|------|------|
| `views/data-cdr/ClinicalSearch.vue` | 新建 | 搜索主页面 |
| `api/data.ts` | 修改 | 添加 `clinicalSearch()` API |
| `router/asyncRoutes.ts` | 修改 | 添加 `/data/cdr/search` 路由 |

### 页面布局

```
┌─────────────────────────────────────────────┐
│  CDR 临床数据检索                             │
├─────────────────────────────────────────────┤
│  数据域: [下拉]  关键词: [输入框]              │
│  日期: [日期范围]  科室: [输入]  诊断: [输入]   │
│  状态: [下拉]            [搜索] [重置]         │
├─────────────────────────────────────────────┤
│  共找到 156 条结果                            │
├─────────────────────────────────────────────┤
│  动态表格（列根据 domain 变化）                │
│  分页                                         │
└─────────────────────────────────────────────┘
```

### 各域表格列

| 域 | 列 |
|----|----|
| PATIENT | 姓名、性别、年龄、身份证、联系电话、创建时间、操作 |
| ENCOUNTER | 患者、就诊号、科室、医生、就诊类型、状态、时间、操作 |
| DIAGNOSIS | 患者、诊断名称、ICD编码、诊断类型、就诊号、时间、操作 |
| LAB | 患者、检验项目、结果值、参考范围、异常标志、时间、操作 |
| MEDICATION | 患者、药品名称、剂量、用法、开始日期、状态、操作 |
| IMAGING | 患者、检查类型、检查部位、报告状态、时间、操作 |
| SURGERY | 患者、手术名称、手术医生、麻醉方式、时间、操作 |
| PATHOLOGY | 患者、标本类型、病理诊断、报告状态、时间、操作 |
| VITAL | 患者、体征类型、测量值、单位、异常标志、时间、操作 |
| ALLERGY | 患者、过敏原、过敏类型、严重程度、记录时间、操作 |
| NOTE | 患者、文书类型、标题、创建医生、时间、操作 |

### 交互细节

- 域切换时表格列动态变化（computed columns）
- 操作列统一有「详情」按钮，跳转到 `/data/cdr/patients/:id`
- 默认 domain = PATIENT
- 未搜索时显示引导文字，无结果时显示"未找到匹配数据"

## 范围边界

### 本次包含

- 统一搜索 API（后端）
- 高级搜索表单 + 动态表格（前端）
- 11 个临床数据域支持
- 路由集成

### 本次不包含

- 脱敏/权限控制
- 搜索历史/保存搜索
- 数据导出
- 全文搜索引擎（ES）

## 技术约束

- JPA Specification 动态查询，不修改现有 Repository 方法签名
- 不引入新依赖
- 不修改现有 Service/Controller 代码
