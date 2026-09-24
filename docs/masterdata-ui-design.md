# 主数据管理统一界面设计

## 设计理念

将概念域、编码体系、值集三个模块整合到一个界面，采用**左侧导航树 + 右侧详情面板**的布局，提供统一的数据标准管理入口。

## 界面布局

```
┌─────────────────────────────────────────────────────────────────┐
│                        主数据管理                                │
├──────────────────┬──────────────────────────────────────────────┤
│                  │                                              │
│  数据标准体系     │              详情面板                         │
│                  │                                              │
│  ├─ 概念域       │  ┌─────────────────────────────────────────┐ │
│  │  ├─ 性别      │  │ 基本信息                                 │ │
│  │  ├─ 血型      │  │ 代码: CD_GENDER  名称: 性别              │ │
│  │  └─ 体重      │  │ 类型: 可枚举    定义: ...                │ │
│  │               │  └─────────────────────────────────────────┘ │
│  ├─ 编码体系     │                                              │
│  │  ├─ ICD-10    │  ┌─────────────────────────────────────────┐ │
│  │  ├─ LOINC     │  │ 值含义列表                               │ │
│  │  └─ SNOMED    │  │ 代码  名称  英文名称  定义  排序  操作   │ │
│  │               │  │ 1     男性  Male      ...    1     编辑  │ │
│  ├─ 值集         │  │ 2     女性  Female    ...    2     编辑  │ │
│  │  ├─ 性别代码  │  │ 9     未知  Unknown   ...    9     编辑  │ │
│  │  ├─ 血型代码  │  └─────────────────────────────────────────┘ │
│  │  └─ 国别代码  │                                              │
│  │               │  ┌─────────────────────────────────────────┐ │
│  │               │  │ 关联的值域                               │ │
│  │               │  │ 代码              名称          类型     │ │
│  │               │  │ VD_GENDER_CODE_1  性别代码-1位  可枚举   │ │
│  │               │  │ VD_GENDER_CODE_1L 性别代码-1位字母 可枚举│ │
│  │               │  └─────────────────────────────────────────┘ │
│                  │                                              │
└──────────────────┴──────────────────────────────────────────────┘
```

## 核心组件

### 1. MasterDataManagement.vue (主页面)

**功能**:
- 左侧树形导航，展示概念域、编码体系、值集三级结构
- 右侧动态加载对应的视图组件
- 支持搜索过滤
- 支持新建操作

**交互**:
- 点击树节点 → 右侧显示对应详情
- 点击"新建" → 打开对应的表单Modal

### 2. ConceptDomainView.vue (概念域视图)

**布局**: 左侧列表 + 右侧详情

**功能**:
- 概念域列表展示（可枚举/不可枚举分类）
- 概念域详情查看
- 值含义管理（可枚举概念域）
- 关联值域查看

### 3. CodeSystemView.vue (编码体系视图)

**布局**: 左侧列表 + 右侧详情

**功能**:
- 编码体系列表展示
- 编码体系详情查看
- 编码列表管理
- 编码导入导出

### 4. ValueSetView.vue (值集视图)

**布局**: 左侧列表 + 右侧详情

**功能**:
- 值集列表展示
- 值集详情查看
- 代码列表管理
- 代码导入导出

## 数据关系

```
概念域 (Concept Domain)
    │
    ├── 值含义 (Value Meaning)  [可枚举概念域]
    │
    └── 值域 (Value Domain)
           │
           └── 允许值 (Permissible Value)  [可枚举值域]
                  │
                  └── 值集 (Value Set)
                         │
                         └── 代码 (Code)

编码体系 (Code System)
    │
    └── 代码 (Code)
           │
           └── 值集 (Value Set)
```

## 颜色方案

| 类型 | 颜色 | 说明 |
|------|------|------|
| 概念域 | #1890ff (蓝色) | 核心概念层 |
| 编码体系 | #722ed1 (紫色) | 标准编码 |
| 值集 | #13c2c2 (青色) | 实际应用 |
| 可枚举 | blue | 列表型 |
| 不可枚举 | green | 描述型 |

## 文件结构

```
src/views/masterdata/
├── MasterDataManagement.vue       # 主页面
├── ConceptDomainList.vue          # 概念域独立页面
├── ValueDomainList.vue            # 值域独立页面
├── DataElementConceptList.vue     # 数据元概念独立页面
├── DataElementList.vue            # 数据元独立页面
├── CodeSystems.vue                # 编码体系独立页面
└── components/
    ├── ConceptDomainView.vue      # 概念域视图组件
    ├── CodeSystemView.vue         # 编码体系视图组件
    ├── ValueSetView.vue           # 值集视图组件
    ├── ConceptDomainFormModal.vue
    ├── ConceptDomainDetailDrawer.vue
    ├── ValueMeaningFormModal.vue
    ├── ValueDomainFormModal.vue
    ├── ValueDomainDetailDrawer.vue
    ├── PermissibleValueFormModal.vue
    ├── PermissibleValueBatchImportModal.vue
    ├── DataElementConceptFormModal.vue
    ├── DataElementConceptDetailDrawer.vue
    ├── CodeSystemFormModal.vue
    └── ValueSetFormModal.vue
```

## 路由配置

```typescript
{
  path: 'masterdata',
  name: 'SystemMasterData',
  meta: { title: '主数据管理' },
  redirect: '/system/masterdata',
  children: [
    { path: '', name: 'MasterDataManagement', meta: { title: '主数据管理' }, component: ... },
    { path: 'concept-domains', name: 'ConceptDomains', meta: { title: '概念域管理', hidden: true }, component: ... },
    { path: 'value-domains', name: 'ValueDomains', meta: { title: '值域管理', hidden: true }, component: ... },
    // ... 其他子路由
  ],
}
```

## 优势

1. **统一入口**: 所有数据标准管理集中在一个界面
2. **直观导航**: 左侧树形结构清晰展示数据层级关系
3. **高效操作**: 左右分栏，列表与详情同时可见
4. **灵活切换**: 支持独立页面和统一界面两种模式
5. **一致性**: 所有视图采用相同的布局模式，降低学习成本

## 使用场景

- **数据管理员**: 在统一界面快速浏览和管理所有数据标准
- **标准制定者**: 通过独立页面深入编辑特定模块
- **系统对接**: 通过值集管理界面配置接口数据交换标准