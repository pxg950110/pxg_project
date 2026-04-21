# 专病管理功能设计

## 概述

在数据管理模块下新增"专病管理"功能，以疾病为中心聚合患者数据，形成专病队列。支持管理员创建专病分类、配置多域纳入规则、自动匹配患者，并提供统计看板和导出功能。集成模板库和 AI 双引擎智能生成过滤条件。

## 核心需求

1. 管理员创建专病分类（如"2型糖尿病"），配置纳入/排除规则
2. 系统根据规则自动匹配患者，医生可手动调整
3. 专病维度统计看板（患者数、性别/年龄分布等）
4. 支持导出专病患者数据
5. 预期管理 10-20 种常见专病
6. 纳入规则支持多临床域（诊断、手术、检验、用药、影像、病历），可扩展
7. 规则支持分组逻辑：组内条件 AND/OR，组间 AND/OR
8. 智能生成条件：模板库精确匹配 + AI 推荐未命中疾病

## 数据模型

### 专病库表 `cdr.c_disease_cohort`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | |
| name | VARCHAR(128) NOT NULL | 专病名称 |
| description | TEXT | 描述 |
| inclusion_rules | JSONB NOT NULL | 纳入规则（分组结构） |
| patient_count | INT DEFAULT 0 | 当前匹配患者数 |
| auto_sync | BOOLEAN DEFAULT TRUE | 是否自动更新匹配 |
| status | VARCHAR(16) DEFAULT 'ACTIVE' | ACTIVE / INACTIVE |
| last_sync_at | TIMESTAMP | 最后同步时间 |
| org_id | BIGINT DEFAULT 0 | 组织 ID |
| created_by | VARCHAR(64) DEFAULT 'system' | |
| created_at | TIMESTAMP DEFAULT NOW() | |
| updated_by | VARCHAR(64) | |
| updated_at | TIMESTAMP | |
| is_deleted | BOOLEAN DEFAULT FALSE | |

### 专病-患者关联表 `cdr.c_disease_cohort_patient`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | |
| cohort_id | BIGINT NOT NULL | 专病库 ID |
| patient_id | BIGINT NOT NULL | 患者 ID |
| match_source | VARCHAR(16) NOT NULL | AUTO / MANUAL |
| matched_at | TIMESTAMP DEFAULT NOW() | 匹配时间 |
| CONSTRAINT uk_cohort_patient UNIQUE (cohort_id, patient_id) | | |

### 疾病模板表 `system.s_disease_template`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | |
| disease_name | VARCHAR(128) NOT NULL | 疾病名称 |
| icd_codes | TEXT[] | 主诊断 ICD 编码列表 |
| inclusion_template | JSONB NOT NULL | 预置纳入规则模板 |
| description | TEXT | 模板说明 |
| is_builtin | BOOLEAN DEFAULT FALSE | 系统内置/自定义 |
| org_id | BIGINT DEFAULT 0 | |
| created_by | VARCHAR(64) DEFAULT 'system' | |
| created_at | TIMESTAMP DEFAULT NOW() | |
| updated_by | VARCHAR(64) | |
| updated_at | TIMESTAMP | |
| is_deleted | BOOLEAN DEFAULT FALSE | |

### inclusion_rules JSON 结构

```json
{
  "groupLogic": "AND",
  "groups": [
    {
      "domain": "DIAGNOSIS",
      "logic": "OR",
      "conditions": [
        { "field": "diagnosis_code", "operator": "LIKE", "value": "E11%" },
        { "field": "diagnosis_code", "operator": "LIKE", "value": "E10%" },
        { "field": "diagnosis_name", "operator": "CONTAINS", "value": "糖尿病" }
      ]
    },
    {
      "domain": "SURGERY",
      "logic": "OR",
      "conditions": [
        { "field": "operation_code", "operator": "LIKE", "value": "36.0%" },
        { "field": "operation_code", "operator": "LIKE", "value": "36.1%" }
      ]
    }
  ]
}
```

- `groupLogic`：组间连接逻辑（AND/OR）
- `groups[].domain`：临床域，对应 CDR 表
- `groups[].logic`：组内条件连接逻辑
- `groups[].conditions[]`：具体条件（field/operator/value）

支持的 domain 及对应 CDR 表：

| domain | 表 | 可过滤字段 |
|--------|-----|-----------|
| DIAGNOSIS | c_diagnosis | diagnosis_code, diagnosis_name, diagnosis_type |
| SURGERY | c_operation | operation_code, operation_name |
| LAB | c_lab_test | test_code, test_name |
| MEDICATION | c_medication | med_code, med_name |
| IMAGING | c_imaging_exam | exam_type, body_part, modality |
| PATHOLOGY | c_pathology | diagnosis_desc, specimen_type, grade |
| NOTE | c_clinical_note | note_type, content |

后期扩展只需新增 domain 映射。

## 后端 API

### 专病库 CRUD

基础路径：`/api/v1/cdr/disease-cohorts`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 专病库列表（分页，支持 keyword/status 筛选） |
| POST | `/` | 创建专病库 |
| PUT | `/{id}` | 更新专病库 |
| DELETE | `/{id}` | 删除专病库（软删除） |

### 匹配与同步

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/{id}/sync` | 手动触发匹配（重新计算患者列表） |
| GET | `/{id}/match-preview` | 预览匹配结果（不写入，返回预计患者数） |

### 患者管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{id}/patients` | 专病患者列表（分页） |
| POST | `/{id}/patients/{patientId}` | 手动添加患者 |
| DELETE | `/{id}/patients/{patientId}` | 移除患者 |

### 统计与导出

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{id}/statistics` | 专病统计（性别/年龄/诊断分布） |
| GET | `/{id}/export` | 导出专病患者数据（CSV） |

### 智能推荐

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/dict/disease-templates?q={keyword}` | 搜索疾病模板 |
| POST | `/api/v1/cdr/disease-cohorts/ai-suggest` | AI 推荐纳入规则 |

AI 推荐接口：

```
Request:  { "disease_name": "慢性肾病" }
Response: {
  "groups": [ ... ],        // 同 inclusion_rules.groups 结构
  "confidence": 0.85,       // 置信度 0-1
  "source": "AI"            // 来源标识
}
```

### 匹配引擎逻辑

`DiseaseCohortService.matchPatients(cohortId)`:

1. 读取专病库的 `inclusion_rules`
2. 按 group 逐组生成 SQL：
   - 根据 domain 确定查询表（如 DIAGNOSIS → c_diagnosis）
   - 按 conditions 生成 WHERE 子句
   - 查询满足条件的 DISTINCT patient_id
3. 按 groupLogic 合并各组结果：
   - AND → 取交集
   - OR → 取并集
4. 与关联表对比，新增的患者 INSERT（match_source=AUTO），已移除的患者 DELETE
5. 更新 cohort.patient_count

## 前端设计

### 路由

```
data/disease          → DiseaseList.vue      专病库列表
data/disease/:id      → DiseaseDetail.vue    专病详情
```

### 页面结构

#### DiseaseList.vue — 卡片列表

- 页面标题"专病管理" + "新建专病库"按钮
- 搜索栏：关键词输入 + 状态筛选
- 卡片网格布局（3-4 列），每张卡片：
  - 专病名称 + 状态标签
  - 患者数量（蓝色突出）
  - 纳入条件分组摘要（每域一行，条件标签 + 逻辑标记）
  - 操作按钮：编辑 / 详情 / 同步
- 分页

#### DiseaseDetail.vue — 专病详情

- 面包屑导航
- 基本信息卡片（名称、描述、状态、自动同步开关、最后同步时间、纳入规则展示）
- 统计指标行：患者总数、男性占比、平均年龄、近30天新增
- 患者列表表格：姓名、性别、年龄、首次诊断、匹配来源（AUTO/MANUAL 标签）
- 手动添加患者按钮 + 导出按钮
- 每行可移除患者

#### 创建/编辑弹窗

- 表单：专病名称（AutoComplete，匹配模板库）、描述、状态
- 疾病名称输入时：
  - 精确匹配模板 → 自动填充条件
  - 无匹配 → 显示"AI 推荐"按钮
- AI 推荐：loading → 展示推荐条件组（带置信度），可一键采纳或逐条调整
- 分组条件构建器 `ConditionBuilder.vue`：
  - 每组：域选择器 + 组内 AND/OR 切换 + 条件行列表
  - 每条条件：字段选择 / 操作符 / 值 / 删除
  - 组间 AND/OR 连接器
  - "添加条件组"按钮
- 底部：测试匹配 | 取消 | 确定

### 组件

| 组件 | 说明 |
|------|------|
| DiseaseList.vue | 专病库卡片列表页 |
| DiseaseDetail.vue | 专病详情页 |
| ConditionBuilder.vue | 分组条件构建器（通用） |
| DiseaseCard.vue | 单张专病卡片 |

### API 集成

在 `src/api/data.ts` 中新增 diseaseCohort 相关函数。

## 预置疾病模板

初始化 SQL 插入 10-20 种常见病模板：

| 疾病 | ICD 范围 | 推荐条件组 |
|------|---------|-----------|
| 2型糖尿病 | E11* | 诊断(E11*) + 检验(HBA1C, FBG) |
| 1型糖尿病 | E10* | 诊断(E10*) + 检验(C肽, 胰岛素) |
| 冠心病 | I25* | 诊断(I25*,I20*) + 手术(PCI,CABG) |
| 高血压 | I10*,I11* | 诊断(I10*,I11*) + 用药(降压药) |
| 脑卒中 | I63*,I61*,I60* | 诊断(OR) + 影像(CT,MRI) |
| 肺癌 | C34* | 诊断(C34*) + 病理(肺) |
| 胃癌 | C16* | 诊断(C16*) + 病理(胃) |
| 结直肠癌 | C18*,C20* | 诊断(OR) + 检验(CEA) |
| 乳腺癌 | C50* | 诊断(C50*) + 病理(乳腺) |
| 慢性肾病 | N18* | 诊断(N18*) + 检验(CREA, eGFR) |
| 肝硬化 | K74* | 诊断(K74*) + 检验(肝功能) |
| 慢阻肺 | J44* | 诊断(J44*) + 检验(肺功能) |
| 房颤 | I48* | 诊断(I48*) |
| 心力衰竭 | I50* | 诊断(I50*) + 检验(BNP) |
| 肺炎 | J18* | 诊断(J18*) + 影像(胸片,CT) |
| 乙肝 | B18* | 诊断(B18*) + 检验(HBsAg, HBV-DNA) |

## 技术要点

1. 匹配引擎使用 JDBC 原生 SQL（与 SmartSearchService 类似模式），因为需要动态拼接多表查询
2. 自动同步复用现有 Schedule 模块的定时任务机制
3. AI 推荐通过 MAIDC 平台推理服务实现，prompt 注入疾病知识生成结构化 JSON
4. 关联表区分 AUTO/MANUAL 来源，同步时不删除手动添加的患者
5. 导出使用 CSV 格式，流式写入避免大结果集 OOM
6. AI 推荐不可用时返回空结果，前端提示"暂无推荐，请手动配置"
7. 自动同步默认每日凌晨 2:00 执行，可通过定时任务模块调整
