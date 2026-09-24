# 卫生健康信息数据元标准化数据模型

## 概述

基于 **WS/T 303-2023《卫生健康信息数据元标准化规则》** 构建的数据元标准化数据模型。

## 核心概念

### 1. 三层架构模型

```
┌─────────────────────────────────────────────────────────────┐
│                     概念层 (Concept Layer)                    │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  数据元概念(DEC)  │         │   概念域(CD)     │          │
│  │  对象类 + 特性    │         │  值含义集合      │          │
│  └──────────────────┘         └──────────────────┘          │
└─────────────────────────────────────────────────────────────┘
           ↓                                 ↓
┌─────────────────────────────────────────────────────────────┐
│                     表示层 (Representation Layer)            │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │    数据元(DE)     │         │    值域(VD)      │          │
│  │  概念 + 表示      │         │  允许值集合      │          │
│  └──────────────────┘         └──────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### 2. 核心实体关系

```
数据元概念(DEC) ──────1:N──────→ 数据元(DE)
     │                              │
     │                              │
     ↓                              ↓
  概念域(CD) ──────1:N──────→ 值域(VD)
     │                              │
     │                              │
     ↓                              ↓
  值含义(VM) ──────N:1──────→ 允许值(PV)
```

## 数据表结构

### 概念层表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `concept_domain` | 概念域 - 值含义集合 | code, name, domain_type, dimension |
| `value_meaning` | 值含义 - 值的语义内容 | concept_domain_id, code, name |
| `data_element_concept` | 数据元概念 - 对象类+特性 | object_class_name, property_name, concept_domain_id |
| `object_class` | 对象类 - 事物的集合 | code, name, concept_type |
| `property` | 特性 - 对象类的特征 | code, name, concept_type |

### 表示层表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `value_domain` | 值域 - 允许值集合 | code, data_type, unit_of_measure, concept_domain_id |
| `permissible_value` | 允许值 - 值和含义的组合 | value_domain_id, value, value_meaning_name |
| `data_element` | 数据元 - 完整的数据单元 | code, definition, data_element_concept_id, value_domain_id |

### 辅助表

| 表名 | 说明 |
|------|------|
| `representation_class_dict` | 表示类字典 |
| `code_set_version` | 值集版本管理 |

## 核心概念详解

### 1. 概念域 (Conceptual Domain, CD)

**定义**: 有效的值含义的集合

**分类**:
- **可枚举概念域**: 由值含义列表规定 (如: 性别、血型)
- **不可枚举概念域**: 由描述规则规定 (如: 体重、体温)

**关键属性**:
- `dimension`: 维度 - 等价计量单位的共同特征 (如: 长度、重量、温度)

**示例**:
```sql
-- 可枚举概念域: 性别
INSERT INTO concept_domain (code, name, definition, domain_type) VALUES
('CD_GENDER', '性别', '人类生物学性别的分类', 'ENUMERABLE');

-- 不可枚举概念域: 体重
INSERT INTO concept_domain (code, name, definition, domain_type, description_rule, dimension) VALUES
('CD_WEIGHT', '体重', '身体所有器官重量的总和', 'NON_ENUMERABLE', '用非负实数表示', '重量');
```

### 2. 值域 (Value Domain, VD)

**定义**: 数据元允许值的集合

**分类**:
- **可枚举值域**: 由允许值列表规定
- **不可枚举值域**: 由描述规定

**关键属性**:
- `data_type`: 数据类型 (STRING, INTEGER, DECIMAL, DATE, etc.)
- `unit_of_measure`: 计量单位 (kg, g, mmHg, etc.)
- `representation_class`: 表示类 (CODE, AMOUNT, MEASUREMENT, etc.)

**重要关系**: 一个概念域可对应多个值域

**示例**:
```sql
-- 同一概念域(体重)对应不同值域
INSERT INTO value_domain (code, name, data_type, unit_of_measure, concept_domain_id) VALUES
('VD_WEIGHT_KG', '体重-N5,2(千克)', 'DECIMAL', 'kg', ...),
('VD_WEIGHT_G', '体重-N4(克)', 'INTEGER', 'g', ...);
```

### 3. 数据元概念 (Data Element Concept, DEC)

**定义**: 能以数据元的形式表示的概念，其表述与任何特定表示法无关

**组成**: 对象类 + 特性

**示例**:
```
数据元概念: 患者性别
  - 对象类: 患者
  - 特性: 性别
  - 概念域: 性别
```

### 4. 数据元 (Data Element, DE)

**定义**: 用一组属性规定其定义、标识、表示和允许值的数据单元

**组成**: 对象类 + 特性 + 表示

**重要关系**: 一个数据元概念可对应多个数据元

**示例**:
```sql
-- 同一数据元概念(患者性别)对应不同数据元
INSERT INTO data_element (code, name, value_domain_id) VALUES
('DE_PATIENT_GENDER_CODE_1', '患者性别代码-1位数字', ...),
('DE_PATIENT_GENDER_CODE_1L', '患者性别代码-1位字母', ...);
```

## 数据元属性体系

### 五类22个基本属性

#### 1. 标识类属性
- 名称 (必选)
- 标识符 (条件选)
- 版本 (条件选)
- 注册机构 (条件选)
- 同义名称 (可选)
- 相关环境 (条件选)

#### 2. 定义类属性
- 定义 (必选)

#### 3. 关系类属性
- 分类模式 (可选)
- 关键字 (可选)
- 相关数据参照 (可选)
- 关系类型 (条件选)

#### 4. 表示类属性
- 表示类别 (必选)
- 表示形式 (必选)
- 数据元值的数据类型 (必选)
- 数据元值的最大长度 (必选)
- 数据元值的最小长度 (必选)
- 表示格式 (条件选)
- 数据元允许值 (必选)

#### 5. 管理类属性
- 主管机构 (可选)
- 注册状态 (条件选)
- 提交机构 (可选)
- 备注 (可选)

#### 6. 附加类属性 (卫生健康领域扩展)
- 收集方法
- 数据来源

## 使用示例

### 示例1: 性别数据元

```
概念域: 性别
  ├─ 值含义: 1-男性, 2-女性, 9-未知

值域1: 性别代码-1位数字
  ├─ 允许值: 1-男性, 2-女性, 9-未知

值域2: 性别代码-1位字母
  ├─ 允许值: M-男性, F-女性, U-未知

数据元概念: 患者性别
  ├─ 对象类: 患者
  ├─ 特性: 性别
  └─ 概念域: 性别

数据元1: 患者性别代码-1位数字
  └─ 值域: 性别代码-1位数字

数据元2: 患者性别代码-1位字母
  └─ 值域: 性别代码-1位字母
```

### 示例2: 体重数据元

```
概念域: 体重 (不可枚举)
  └─ 描述规则: 用非负实数表示

值域1: 体重-N5,2(千克)
  ├─ 数据类型: DECIMAL
  ├─ 格式: N5,2
  └─ 计量单位: kg

值域2: 体重-N4(克)
  ├─ 数据类型: INTEGER
  └─ 计量单位: g

数据元概念1: 患者体重
  ├─ 对象类: 患者
  ├─ 特性: 体重
  └─ 概念域: 体重

数据元概念2: 新生儿体重
  ├─ 对象类: 新生儿
  ├─ 特性: 体重
  └─ 概念域: 体重

数据元1: 患者体重-N5,2(千克)
  └─ 值域: 体重-N5,2(千克)

数据元2: 新生儿体重-N5,2(千克)
  └─ 值域: 体重-N5,2(千克)

数据元3: 新生儿体重-N4(克)
  └─ 值域: 体重-N4(克)
```

## 查询示例

### 1. 查询数据元完整信息

```sql
SELECT * FROM v_data_element_full WHERE code = 'DE_PATIENT_GENDER_CODE_1';
```

### 2. 查询值域的所有允许值

```sql
SELECT * FROM v_value_domain_with_values
WHERE value_domain_code = 'VD_GENDER_CODE_1'
ORDER BY sort_order;
```

### 3. 查询同一概念域对应的所有值域

```sql
SELECT vd.code, vd.name, vd.data_type, vd.unit_of_measure
FROM concept_domain cd
JOIN value_domain vd ON cd.id = vd.concept_domain_id
WHERE cd.code = 'CD_WEIGHT';
```

### 4. 查询同一数据元概念对应的所有数据元

```sql
SELECT de.code, de.name, vd.name AS value_domain_name
FROM data_element_concept dec
JOIN data_element de ON dec.id = de.data_element_concept_id
JOIN value_domain vd ON de.value_domain_id = vd.id
WHERE dec.code = 'DEC_PATIENT_GENDER';
```

## 核心优势

1. **概念复用**: 同一概念域可被多个数据元概念共享
2. **表示灵活**: 同一概念可有多种表示方式
3. **标准统一**: 遵循国家标准 WS/T 303-2023
4. **层次清晰**: 概念层与表示层分离
5. **扩展性强**: 支持卫生健康领域附加属性

## 参考标准

- WS/T 303-2023 《卫生健康信息数据元标准化规则》
- GB/T 18391.1 《信息技术 元数据注册系统(MDR) 第1部分：框架》
- GB/T 18391.3 《信息技术 元数据注册系统(MDR) 第3部分：注册系统元模型与基本属性》
- ISO 3166-1 《国家和所属地区名称代码 第1部分：国家代码》
