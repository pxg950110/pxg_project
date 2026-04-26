# 数据元管理功能设计

> 日期：2026-04-26
> 状态：待实现
> 参考：WS/T 303-2023 数据元标准化规则、WS/T 671-2020 国家卫生与人口信息数据字典

## 1. 概述

在系统配置 → 主数据配置下新增"数据元管理"模块，实现：

1. **数据元标准目录**：基于 WS/T 671 的 17 项元数据属性，管理通用数据元的 CRUD
2. **字段映射**：数据元与 CDR/ODS 数据库表字段的手动映射 + 自动发现
3. **CSV 导入**：支持从标准文档整理的 CSV 批量导入数据元及允许值

### 架构方案

采用方案 A — 在现有 `masterdata` schema 和 `maidc-data` 模块下扩展。数据元与已有 concept/code_system 天然关联，复用基础设施。

## 2. 数据模型

### 2.1 m_data_element — 数据元主表

```sql
CREATE TABLE masterdata.m_data_element (
    id                    BIGSERIAL    PRIMARY KEY,
    element_code          VARCHAR(64)  NOT NULL,
    name                  VARCHAR(256) NOT NULL,
    name_en               VARCHAR(256),
    definition            TEXT         NOT NULL,

    -- 数据元概念属性
    object_class_name     VARCHAR(128),
    object_class_id       VARCHAR(64),
    property_name         VARCHAR(128),
    property_id           VARCHAR(64),

    -- 表示类属性
    data_type             VARCHAR(32)  NOT NULL,      -- ST/INT/REAL/DT/CD/BL/PQ/MO等
    representation_class  VARCHAR(32),                 -- 名称/测量/数目/数量/文本/代码等
    value_domain_name     VARCHAR(128),
    value_domain_id       VARCHAR(64),
    min_length            INT,
    max_length            INT,
    format                VARCHAR(64),
    unit_of_measure       VARCHAR(32),

    -- 分类与管理
    category              VARCHAR(64),                 -- 人口学/诊断/检验/药品/...
    standard_source       VARCHAR(128),                -- WS/T 671 / 自定义
    registration_status   VARCHAR(16)  NOT NULL DEFAULT 'DRAFT', -- DRAFT/PUBLISHED/RETIRED
    version               VARCHAR(16)  NOT NULL DEFAULT '1.0',
    synonyms              TEXT[],                      -- 同义名称
    keywords              TEXT[],                      -- 关键字
    extra_attrs           JSONB,                       -- 注册机构、备注等扩展属性

    -- 通用字段
    status                VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by            VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMP,
    is_deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id                BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_data_element_code UNIQUE (element_code)
);

COMMENT ON TABLE masterdata.m_data_element IS '数据元（符合WS/T 303/671的标准化数据单元）';

CREATE INDEX idx_de_category ON masterdata.m_data_element(category) WHERE status = 'ACTIVE' AND is_deleted = false;
CREATE INDEX idx_de_status ON masterdata.m_data_element(registration_status) WHERE is_deleted = false;
CREATE INDEX idx_de_name ON masterdata.m_data_element USING gin(to_tsvector('simple', name));
CREATE INDEX idx_de_keywords ON masterdata.m_data_element USING gin(keywords);
```

### 2.2 m_data_element_value — 数据元允许值

```sql
CREATE TABLE masterdata.m_data_element_value (
    id                BIGSERIAL    PRIMARY KEY,
    data_element_id   BIGINT       NOT NULL,
    value_code        VARCHAR(64)  NOT NULL,
    value_meaning     VARCHAR(256) NOT NULL,
    sort_order        INT          NOT NULL DEFAULT 0,

    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0
);

COMMENT ON TABLE masterdata.m_data_element_value IS '数据元允许值';

CREATE INDEX idx_dev_element ON masterdata.m_data_element_value(data_element_id);
CREATE UNIQUE INDEX uk_dev_element_code ON masterdata.m_data_element_value(data_element_id, value_code) WHERE is_deleted = false;
```

### 2.3 m_data_element_mapping — 数据元字段映射

```sql
CREATE TABLE masterdata.m_data_element_mapping (
    id                BIGSERIAL    PRIMARY KEY,
    data_element_id   BIGINT       NOT NULL,
    schema_name       VARCHAR(64)  NOT NULL,
    table_name        VARCHAR(128) NOT NULL,
    column_name       VARCHAR(128) NOT NULL,
    mapping_type      VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',   -- MANUAL/AUTO_SUGGESTED
    confidence        DECIMAL(3,2),
    mapping_status    VARCHAR(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING/CONFIRMED/REJECTED
    transform_rule    TEXT,
    mapped_by         VARCHAR(64),
    mapped_at         TIMESTAMP,

    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    org_id            BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uk_de_mapping UNIQUE (data_element_id, schema_name, table_name, column_name) WHERE is_deleted = false
);

COMMENT ON TABLE masterdata.m_data_element_mapping IS '数据元与数据库字段映射';

CREATE INDEX idx_dem_element ON masterdata.m_data_element_mapping(data_element_id);
CREATE INDEX idx_dem_status ON masterdata.m_data_element_mapping(mapping_status);
CREATE INDEX idx_dem_table ON masterdata.m_data_element_mapping(schema_name, table_name);
```

### 预置分类数据

```sql
-- 分类在数据元中为自由文本，以下为标准分类参考
-- category 对应 WS/T 363 各部分：
-- 人口学(第3部), 标识(第2部), 健康史(第4部), 危险因素(第5部),
-- 主诉症状(第6部), 体格检查(第7部), 辅助检查(第8部), 实验室检验(第9部),
-- 诊断(第10部), 评估(第11部), 计划干预(第12部), 费用(第13部),
-- 机构(第14部), 人员(第15部), 管理(第17部)
```

## 3. 后端 API

### 3.1 包结构

在 `maidc-data` 模块下新增：

```
com.maidc.data/
├── entity/
│   ├── DataElementEntity.java
│   ├── DataElementValueEntity.java
│   └── DataElementMappingEntity.java
├── repository/
│   ├── DataElementRepository.java
│   ├── DataElementValueRepository.java
│   └── DataElementMappingRepository.java
├── service/
│   └── DataElementService.java
├── controller/
│   └── DataElementController.java
├── dto/
│   ├── DataElementCreateDTO.java
│   ├── DataElementUpdateDTO.java
│   ├── DataElementQueryDTO.java
│   └── DataElementMappingDTO.java
```

遵循已有模式：Entity 继承 `BaseEntity`，`@Table(schema = "masterdata")`，`@Where(clause = "is_deleted = false")`，`@SQLDelete` 软删除。Repository 继承 `JpaRepository + JpaSpecificationExecutor`。

### 3.2 API 端点

**数据元主资源 `/api/v1/masterdata/data-elements`：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 分页列表，支持 category/status/keyword 筛选 |
| GET | `/{id}` | 详情（含允许值列表） |
| POST | `/` | 创建数据元 |
| PUT | `/{id}` | 更新数据元 |
| DELETE | `/{id}` | 软删除 |
| GET | `/categories` | 获取分类列表（去重） |
| GET | `/stats` | 统计概览 |

**允许值子资源：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{id}/values` | 获取允许值列表 |
| PUT | `/{id}/values` | 批量更新允许值 |

**映射子资源：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{id}/mappings` | 获取字段映射 |
| POST | `/{id}/mappings` | 添加手动映射 |
| PUT | `/mappings/{mappingId}` | 更新映射（确认/拒绝） |
| DELETE | `/mappings/{mappingId}` | 删除映射 |
| POST | `/auto-discover` | 自动发现：扫描数据库元数据，按相似度建议匹配 |
| GET | `/mappings/unmapped` | 查看未映射的数据元 |

### 3.3 自动发现算法

1. 查询 `information_schema.columns` 获取 cdr/ods/public schema 下的所有表字段
2. 排除已映射字段（查 m_data_element_mapping）
3. 名称匹配：column_name 去下划线后与 data_element.name/element_code 计算文本相似度
4. 类型匹配：data_type 与 SQL type 映射（ST→varchar/text, INT→integer, REAL→numeric, DT→date/timestamp, CD→varchar+有允许值）
5. 综合得分：名称相似度 × 0.6 + 类型匹配 × 0.4
6. 阈值 confidence ≥ 0.5 才输出建议
7. 结果写入 m_data_element_mapping，mapping_type=AUTO_SUGGESTED

### 3.4 CSV 导入

复用已有 `m_import_task` 机制，task_type 新增 `DATA_ELEMENT`。

CSV 模板格式：

```csv
element_code,name,name_en,definition,object_class_name,property_name,data_type,representation_class,min_length,max_length,format,unit_of_measure,category,value_domain_name,standard_source,synonyms,keywords
```

允许值 CSV：

```csv
element_code,value_code,value_meaning,sort_order
```

## 4. 前端页面

### 4.1 路由

在 `asyncRoutes.ts` 的 `system/masterdata` children 中追加：

```ts
{ path: 'data-elements', name: 'DataElementList', meta: { title: '数据元管理' },
  component: () => import('@/views/masterdata/DataElementList.vue') },
```

### 4.2 DataElementList.vue

**布局**：左侧分类树（240px）+ 右侧表格

- 左侧分类树：按 category 分组，支持"全部"节点，点击筛选列表
- 顶部操作栏：搜索框、状态筛选、新增按钮、自动发现按钮、导入按钮
- 表格列：标识符、规范名称、对象类、数据类型、表示类、分类、版本、注册状态(Tag)、映射状态(Tag)、操作
- 分页

### 4.3 DataElementDetail.vue

使用 `a-drawer`（width 720px），三个 Tab：

1. **基本信息**：标识类、定义类、概念属性、表示类属性、分类关键字的表单。name/element_code/definition/data_type 必填
2. **允许值**：可编辑表格（值代码、值含义、排序），行内增删改，仅 data_type=CD 时显示
3. **字段映射**：映射列表（schema.table.column、类型、置信度、状态、转换规则），支持确认/拒绝/删除，"添加映射"弹出数据库字段选择器

### 4.4 AutoDiscoverModal.vue

- 选择扫描 schema（cdr/ods/public）
- 调用 auto-discover API
- 展示匹配建议（左数据元 ↔ 右数据库字段）
- 逐条确认/拒绝，批量提交

### 4.5 API 模块

在 `src/api/masterdata.ts` 中追加数据元相关接口函数。

## 5. 权限

复用 masterdata 已有权限：
- `masterdata:read` — 查看数据元
- `masterdata:create` — 创建/导入
- `masterdata:update` — 编辑/映射
- `masterdata:delete` — 删除
