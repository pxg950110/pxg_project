# 03 — 前端列表 + 弹窗

**Goal:** 实现专病库卡片列表页、创建/编辑弹窗、分组条件构建器组件。

**Files:**
- Create: `maidc-portal/src/views/data-cdr/DiseaseList.vue`
- Create: `maidc-portal/src/components/ConditionBuilder/index.vue`
- Create: `maidc-portal/src/components/DiseaseCard/index.vue`
- Modify: `maidc-portal/src/router/asyncRoutes.ts`
- Modify: `maidc-portal/src/api/data.ts`

---

## Task 3.1: API 函数

- [ ] **Step 1: 在 `src/api/data.ts` 末尾追加专病管理 API**

```typescript
// ===== Disease Cohort =====
export function getDiseaseCohorts(params: { page?: number; page_size?: number; keyword?: string; status?: string }) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/disease-cohorts', { params })
}
export function getDiseaseCohort(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${id}`)
}
export function createDiseaseCohort(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/disease-cohorts', data)
}
export function updateDiseaseCohort(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/disease-cohorts/${id}`, data)
}
export function deleteDiseaseCohort(id: number) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-cohorts/${id}`)
}
export function syncDiseaseCohort(id: number) {
  return request.post<ApiResponse<void>>(`/cdr/disease-cohorts/${id}/sync`)
}
export function previewDiseaseCohort(id: number) {
  return request.get<ApiResponse<{ patientCount: number }>>(`/cdr/disease-cohorts/${id}/match-preview`)
}
export function getDiseaseCohortPatients(id: number, params: { page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>(`/cdr/disease-cohorts/${id}/patients`, { params })
}
export function addDiseaseCohortPatient(id: number, patientId: number) {
  return request.post<ApiResponse<void>>(`/cdr/disease-cohorts/${id}/patients/${patientId}`)
}
export function removeDiseaseCohortPatient(id: number, patientId: number) {
  return request.delete<ApiResponse<void>>(`/cdr/disease-cohorts/${id}/patients/${patientId}`)
}
export function getDiseaseCohortStatistics(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/disease-cohorts/${id}/statistics`)
}
export function exportDiseaseCohort(id: number) {
  return request.get(`/cdr/disease-cohorts/${id}/export`, { responseType: 'blob' })
}
export function searchDiseaseTemplates(q: string) {
  return request.get<ApiResponse<any[]>>('/dict/disease-templates', { params: { q } })
}
```

## Task 3.2: 路由注册

- [ ] **Step 2: 在 `asyncRoutes.ts` 的 data.children 中追加路由**

在 `data` 路由的 children 数组中，在 `cdr/search` 之后添加：

```typescript
{ path: 'cdr/disease', name: 'DiseaseList', meta: { title: '专病管理' }, component: () => import('@/views/data-cdr/DiseaseList.vue') },
{ path: 'cdr/disease/:id', name: 'DiseaseDetail', meta: { title: '专病详情', hidden: true }, component: () => import('@/views/data-cdr/DiseaseDetail.vue') },
```

## Task 3.3: ConditionBuilder 组件

- [ ] **Step 3: 创建 `src/components/ConditionBuilder/index.vue`**

Props: `modelValue` (inclusion_rules JSON 对象)
Emits: `update:modelValue`

功能：
- 渲染 groups 数组，每个 group 一个卡片
- 每个 group：域下拉(7种) + 组内 AND/OR 切换 + 条件行列表 + "添加条件"按钮 + 删除组按钮
- 每条条件：字段 select + 操作符 select(LIKE/IN/=/CONTAINS) + 值 input + 删除按钮
- 组间显示 groupLogic 的 AND/OR 切换分隔符
- 底部"添加条件组"按钮
- 字段选项根据 domain 动态变化

## Task 3.4: DiseaseCard 组件

- [ ] **Step 4: 创建 `src/components/DiseaseCard/index.vue`**

Props: `data` (专病库对象)
Emits: `edit`, `detail`, `sync`

卡片内容：
- 顶部：名称(bold) + 状态标签(已启用/未启用)
- 中间：患者数量(蓝色大号) + "名患者"
- 中间：条件分组摘要，每域一行，域标签 + 条件 pill 标签
- 底部：操作按钮 [编辑 | 详情 | 同步]

## Task 3.5: DiseaseList 页面

- [ ] **Step 5: 创建 `src/views/data-cdr/DiseaseList.vue`**

- PageContainer 标题"专病管理" + "新建专病库"按钮
- 搜索栏：关键词 input + 状态 select
- 卡片网格 `display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px;`
- 分页
- 创建/编辑 Modal：
  - 表单：专病名称、描述、状态
  - ConditionBuilder 组件
  - 底部 footer：测试匹配按钮 + 取消 + 确定
  - 测试匹配调用 previewDiseaseCohort API 展示预计患者数

- [ ] **Step 6: 浏览器验证**

导航到 `/data/cdr/disease`，验证：
- 卡片列表渲染
- 新建弹窗打开，条件构建器可用
- 编辑弹窗预填充数据
- 同步按钮调用 API

- [ ] **Step 7: Commit**

```bash
git add maidc-portal/src/api/data.ts
git add maidc-portal/src/router/asyncRoutes.ts
git add maidc-portal/src/components/ConditionBuilder/
git add maidc-portal/src/components/DiseaseCard/
git add maidc-portal/src/views/data-cdr/DiseaseList.vue
git commit -m "feat(disease): add disease list page with card layout and condition builder"
```
