# 数据元标准化前端修改说明

## 概述

基于 **WS/T 303-2023《卫生健康信息数据元标准化规则》**，前端需要新增以下管理模块：

## 新增页面

### 1. 概念域管理 (`ConceptDomainList.vue`)

**路径**: `/masterdata/concept-domains`

**功能**:
- 概念域列表展示（可枚举/不可枚举分类）
- 概念域CRUD操作
- 值含义管理（可枚举概念域）
- 查看关联的值域

**关键组件**:
- `ConceptDomainFormModal.vue` - 概念域表单
- `ConceptDomainDetailDrawer.vue` - 概念域详情
- `ValueMeaningFormModal.vue` - 值含义表单

### 2. 值域管理 (`ValueDomainList.vue`)

**路径**: `/masterdata/value-domains`

**功能**:
- 值域列表展示
- 值域CRUD操作
- 允许值管理（可枚举值域）
- 查看关联的数据元

**关键组件**:
- `ValueDomainFormModal.vue` - 值域表单
- `ValueDomainDetailDrawer.vue` - 值域详情
- `PermissibleValueFormModal.vue` - 允许值表单
- `PermissibleValueBatchImportModal.vue` - 允许值批量导入

### 3. 数据元概念管理 (`DataElementConceptList.vue`)

**路径**: `/masterdata/data-element-concepts`

**功能**:
- 数据元概念列表
- 数据元概念CRUD操作
- 查看关联的数据元

### 4. 数据元管理（更新现有）

**路径**: `/masterdata/data-elements`

**更新内容**:
- 关联数据元概念
- 关联值域
- 显示完整的数据元信息（对象类、特性、概念域等）

## 文件结构

```
src/
├── api/
│   └── dataElementStandard.ts          # API接口定义
├── views/
│   └── masterdata/
│       ├── ConceptDomainList.vue       # 概念域列表
│       ├── ValueDomainList.vue         # 值域列表
│       ├── DataElementConceptList.vue  # 数据元概念列表
│       ├── DataElementList.vue         # 数据元列表（更新）
│       └── components/
│           ├── ConceptDomainFormModal.vue
│           ├── ConceptDomainDetailDrawer.vue
│           ├── ValueMeaningFormModal.vue
│           ├── ValueDomainFormModal.vue
│           ├── ValueDomainDetailDrawer.vue
│           ├── PermissibleValueFormModal.vue
│           ├── PermissibleValueBatchImportModal.vue
│           ├── DataElementConceptFormModal.vue
│           ├── DataElementConceptDetailDrawer.vue
│           └── DataElementFormModal.vue
└── router/
    └── modules/
        └── masterdata.ts               # 路由配置
```

## 路由配置

```typescript
// router/modules/masterdata.ts
export default {
  path: '/masterdata',
  name: 'MasterData',
  component: () => import('@/layouts/MainLayout.vue'),
  children: [
    {
      path: 'concept-domains',
      name: 'ConceptDomains',
      component: () => import('@/views/masterdata/ConceptDomainList.vue'),
      meta: { title: '概念域管理' },
    },
    {
      path: 'value-domains',
      name: 'ValueDomains',
      component: () => import('@/views/masterdata/ValueDomainList.vue'),
      meta: { title: '值域管理' },
    },
    {
      path: 'data-element-concepts',
      name: 'DataElementConcepts',
      component: () => import('@/views/masterdata/DataElementConceptList.vue'),
      meta: { title: '数据元概念管理' },
    },
    {
      path: 'data-elements',
      name: 'DataElements',
      component: () => import('@/views/masterdata/DataElementList.vue'),
      meta: { title: '数据元管理' },
    },
  ],
}
```

## 核心数据模型

### 概念域 (Concept Domain)

```typescript
interface ConceptDomain {
  id: number
  code: string
  name: string
  nameEn?: string
  definition: string
  domainType: 'ENUMERABLE' | 'NON_ENUMERABLE'
  descriptionRule?: string      // 不可枚举概念域描述规则
  dimension?: string            // 维度
  parentId?: number
  version: string
  status: string
}
```

### 值含义 (Value Meaning)

```typescript
interface ValueMeaning {
  id: number
  conceptDomainId: number
  code: string
  name: string
  nameEn?: string
  definition?: string
  sortOrder: number
  isActive: boolean
}
```

### 值域 (Value Domain)

```typescript
interface ValueDomain {
  id: number
  code: string
  name: string
  definition: string
  domainType: 'ENUMERABLE' | 'NON_ENUMERABLE'
  description?: string          // 不可枚举值域描述
  dataType: string
  maxLength?: number
  minLength?: number
  format?: string
  unitOfMeasure?: string        // 计量单位代码
  unitName?: string             // 计量单位名称
  representationClass?: string  // 表示类
  conceptDomainId: number
}
```

### 允许值 (Permissible Value)

```typescript
interface PermissibleValue {
  id: number
  valueDomainId: number
  value: string
  valueMeaningId?: number
  valueMeaningName: string
  sortOrder: number
  isActive: boolean
  effectiveDate?: string
  expiryDate?: string
}
```

### 数据元概念 (Data Element Concept)

```typescript
interface DataElementConcept {
  id: number
  code: string
  name: string
  definition: string
  objectClassCode?: string
  objectClassName?: string
  propertyCode?: string
  propertyName?: string
  conceptDomainId?: number
}
```

### 数据元 (Data Element)

```typescript
interface DataElement {
  id: number
  code: string
  name: string
  definition: string
  dataType: string
  maxLength?: number
  minLength?: number
  format?: string
  representationForm?: string
  dataElementConceptId?: number
  valueDomainId?: number
  registrationStatus: string
}
```

## 关键功能

### 1. 概念域与值域的关系

- 一个概念域可对应多个值域
- 可枚举概念域需要管理值含义
- 不可枚举概念域需要定义描述规则

### 2. 值域与允许值的关系

- 可枚举值域需要管理允许值列表
- 允许值可以关联值含义
- 支持允许值的批量导入

### 3. 数据元概念与数据元的关系

- 一个数据元概念可对应多个数据元
- 数据元概念由对象类和特性组成
- 数据元概念关联概念域

### 4. 数据元的完整信息

- 数据元 = 数据元概念 + 值域
- 显示对象类、特性、概念域、值域等完整信息
- 支持查看允许值列表

## 已创建文件

| 文件 | 说明 | 状态 |
|------|------|------|
| `src/api/dataElementStandard.ts` | API接口定义 | ✅ 已创建 |
| `src/views/masterdata/ConceptDomainList.vue` | 概念域列表页 | ✅ 已创建 |
| `src/views/masterdata/ValueDomainList.vue` | 值域列表页 | ✅ 已创建 |
| `src/views/masterdata/DataElementConceptList.vue` | 数据元概念列表 | ✅ 已创建 |
| `src/views/masterdata/components/ConceptDomainFormModal.vue` | 概念域表单 | ✅ 已创建 |
| `src/views/masterdata/components/ConceptDomainDetailDrawer.vue` | 概念域详情 | ✅ 已创建 |
| `src/views/masterdata/components/ValueMeaningFormModal.vue` | 值含义表单 | ✅ 已创建 |
| `src/views/masterdata/components/ValueDomainFormModal.vue` | 值域表单 | ✅ 已创建 |
| `src/views/masterdata/components/ValueDomainDetailDrawer.vue` | 值域详情 | ✅ 已创建 |
| `src/views/masterdata/components/PermissibleValueFormModal.vue` | 允许值表单 | ✅ 已创建 |
| `src/views/masterdata/components/PermissibleValueBatchImportModal.vue` | 允许值批量导入 | ✅ 已创建 |
| `src/views/masterdata/components/DataElementConceptFormModal.vue` | 数据元概念表单 | ✅ 已创建 |
| `src/views/masterdata/components/DataElementConceptDetailDrawer.vue` | 数据元概念详情 | ✅ 已创建 |
| `src/router/asyncRoutes.ts` | 路由配置 | ✅ 已更新 |

## 下一步工作

1. 创建剩余的组件文件
2. 更新路由配置
3. 更新菜单配置
4. 测试所有功能
