# Data Element Import Design

## Overview

Add Excel-based batch import for data elements (数据元), supporting multi-sheet (Sheet1=data elements, Sheet2=allowed values for CD type). Target volume: 5000+ rows per import.

## Approach

**Async task + JDBC Batch upsert** — upload triggers async processing, frontend polls task status.

## Backend

### New Files

| File | Purpose |
|------|---------|
| `DataElementImportDTO.java` | Excel row mapping DTO with validation |
| `DataElementImportService.java` | Core logic: parse Excel, validate, batch write |
| `ImportTaskEntity.java` | Import task record (progress, status, stats) |
| `ImportTaskRepository.java` | Task persistence |

### Import Task Table (`masterdata.m_import_task`)

```sql
CREATE TABLE masterdata.m_import_task (
    id              BIGSERIAL PRIMARY KEY,
    task_type       VARCHAR(32) NOT NULL DEFAULT 'DATA_ELEMENT',
    file_name       VARCHAR(256),
    total_rows      INT DEFAULT 0,
    success_count   INT DEFAULT 0,
    fail_count      INT DEFAULT 0,
    skip_count      INT DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'PROCESSING',
    error_detail    TEXT,
    created_by      VARCHAR(64) DEFAULT 'system',
    created_at      TIMESTAMP DEFAULT NOW(),
    finished_at     TIMESTAMP
);
```

### New Endpoints in `DataElementController`

```
POST   /api/v1/masterdata/data-elements/import          — upload Excel file, returns taskId
GET    /api/v1/masterdata/data-elements/import/tasks/{id} — poll task status
GET    /api/v1/masterdata/data-elements/import/template   — download empty template
```

### Import Flow

1. Controller receives `MultipartFile`, creates `ImportTask` (PROCESSING), returns taskId
2. `@Async` method:
   - POI parses Sheet1 (data elements) → row-by-row validation → collect to batch
   - Every 500 rows: JDBC batch upsert on `m_data_element` (ON CONFLICT element_code DO UPDATE)
   - Parse Sheet2 (allowed values) → group by element_code → delete existing + batch insert into `m_data_element_value`
   - Update ImportTask to COMPLETED with stats
3. On exception: update task to FAILED with error_detail

### Excel Template Format

**Sheet1: 数据元**

| 标识符 | 规范名称 | 英文名称 | 定义 | 对象类 | 对象类ID | 特性 | 特性ID | 数据类型 | 表示类 | 值域名称 | 值域ID | 最小长度 | 最大长度 | 格式 | 计量单位 | 分类 | 标准来源 | 注册状态 | 版本 |
|--------|---------|---------|------|--------|---------|------|--------|---------|--------|---------|--------|---------|---------|------|---------|------|---------|---------|------|

**Sheet2: 允许值**

| 标识符 | 值编码 | 值含义 | 排序 |
|--------|--------|--------|------|

Sheet2 links to Sheet1 via the 标识符 (element_code) column.

### Conflict Strategy

- Data elements: `ON CONFLICT (element_code) WHERE is_deleted = false DO UPDATE`
- Allowed values: delete existing values for the element, then batch insert new ones

## Frontend

### Changes to DataElementList.vue

- Add "导入" button next to "新增数据元"
- Add "下载模板" button

### New File: `DataElementImportModal.vue`

Steps:
1. **Upload** — drag-drop / click to upload .xlsx
2. **Progress** — show progress bar, poll GET /import/tasks/{id}
3. **Result** — display summary: total / success / fail / skip counts
4. **Close** — refresh list and stats

### New API Functions in `masterdata.ts`

```typescript
export const importDataElements = (file: File) => { ... }
export const getDataElementImportStatus = (taskId: number) => { ... }
export const downloadDataElementTemplate = () => { ... }
```

## Dependencies

- `org.apache.poi:poi-ooxml` (already in maidc-data pom.xml for existing import)
- `@Async` requires `@EnableAsync` on application class (check if already present)

## Error Handling

- Invalid rows: collect error messages per row, return in task error_detail (up to 100 errors)
- File too large: reject at controller if > 10MB
- Missing required fields (element_code, name, definition, data_type): skip row, increment fail_count
- Malformed Excel: set task to FAILED immediately
