# 06: 前端页面

> **前置:** 01-04 后端全部完成
> **产出:** 5个管理页面 + API层 + 路由菜单

---

### Task 1: API 层

**Files:**
- Create: `maidc-portal/src/api/masterdata.ts`

- [ ] **Step 1: 写 API 模块**

```typescript
import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ========== CodeSystem ==========
export function getCodeSystems() {
  return request.get<ApiResponse<any[]>>('/masterdata/code-systems')
}

export function getCodeSystem(id: number) {
  return request.get<ApiResponse<any>>(`/masterdata/code-systems/${id}`)
}

export function getCodeSystemStats(id: number) {
  return request.get<ApiResponse<any>>(`/masterdata/code-systems/${id}/stats`)
}

export function createCodeSystem(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/code-systems', data)
}

export function updateCodeSystem(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/masterdata/code-systems/${id}`, data)
}

// ========== Concept ==========
export function getConcepts(params: { codeSystemId?: number; domain?: string; keyword?: string; page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/masterdata/concepts', { params })
}

export function getConcept(id: number) {
  return request.get<ApiResponse<any>>(`/masterdata/concepts/${id}`)
}

export function searchConcepts(params: { keyword: string; codeSystemId?: number; page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/masterdata/concepts/search', { params })
}

export function getConceptChildren(id: number) {
  return request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/children`)
}

export function getConceptAncestors(id: number) {
  return request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/ancestors`)
}

export function getConceptMappings(id: number, targetSystem?: string) {
  return request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/mappings`, { params: { targetSystem } })
}

export function getConceptSynonyms(id: number) {
  return request.get<ApiResponse<any[]>>(`/masterdata/concepts/${id}/synonyms`)
}

export function createConcept(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/concepts', data)
}

export function updateConcept(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/masterdata/concepts/${id}`, data)
}

export function deleteConcept(id: number) {
  return request.delete<ApiResponse<void>>(`/masterdata/concepts/${id}`)
}

// ========== Mappings ==========
export function getMappings(params: { sourceSystem?: number; targetSystem?: number }) {
  return request.get<ApiResponse<any[]>>('/masterdata/mappings', { params })
}

export function createMapping(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/mappings', data)
}

export function batchCreateMappings(data: any[]) {
  return request.post<ApiResponse<any[]>>('/masterdata/mappings/batch', data)
}

export function deleteMapping(id: number) {
  return request.delete<ApiResponse<void>>(`/masterdata/mappings/${id}`)
}

// ========== Reference Ranges ==========
export function getReferenceRanges(params: { conceptId?: number; gender?: string }) {
  return request.get<ApiResponse<any[]>>('/masterdata/reference-ranges', { params })
}

export function evaluateReferenceRange(params: { conceptId: number; gender: string; age: number }) {
  return request.get<ApiResponse<any>>('/masterdata/reference-ranges/evaluate', { params })
}

export function createReferenceRange(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/reference-ranges', data)
}

export function updateReferenceRange(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/masterdata/reference-ranges/${id}`, data)
}

// ========== Drug Interactions ==========
export function getDrugInteractions(params: { drug1?: number; drug2?: number; severity?: string }) {
  return request.get<ApiResponse<any[]>>('/masterdata/drug-interactions', { params })
}

export function checkDrugInteraction(drug1: number, drug2: number) {
  return request.get<ApiResponse<any[]>>('/masterdata/drug-interactions/check', { params: { drug1, drug2 } })
}

export function checkDrugList(drugIds: number[]) {
  return request.post<ApiResponse<any[]>>('/masterdata/drug-interactions/check-list', drugIds)
}

export function createDrugInteraction(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/drug-interactions', data)
}

// ========== Institutions ==========
export function getInstitutions() {
  return request.get<ApiResponse<any[]>>('/masterdata/institutions')
}

export function createInstitution(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/institutions', data)
}

export function updateInstitution(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/masterdata/institutions/${id}`, data)
}

// ========== Local Concepts ==========
export function getLocalConcepts(params: { institutionId: number; codeSystemId: number; mappingStatus?: string; page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/masterdata/local-concepts', { params })
}

export function getUnmappedLocalConcepts(params: { institutionId?: number; codeSystemId?: number; page?: number; page_size?: number }) {
  return request.get<ApiResponse<PageResult<any>>>('/masterdata/local-concepts/unmapped', { params })
}

export function translateLocalCode(params: { institutionId: number; codeSystemId: number; localCode: string }) {
  return request.get<ApiResponse<any>>('/masterdata/local-concepts/translate', { params })
}

export function getLocalConceptStats(institutionId: number, codeSystemId: number) {
  return request.get<ApiResponse<Record<string, number>>>('/masterdata/local-concepts/stats', { params: { institutionId, codeSystemId } })
}

export function createLocalConcept(data: any) {
  return request.post<ApiResponse<any>>('/masterdata/local-concepts', data)
}

export function batchCreateLocalConcepts(data: any[]) {
  return request.post<ApiResponse<any[]>>('/masterdata/local-concepts/batch', data)
}

export function updateLocalConcept(id: number, data: any) {
  return request.put<ApiResponse<any>>(`/masterdata/local-concepts/${id}`, data)
}

// ========== Import ==========
export function uploadMasterData(file: File, codeSystemId: number) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<ApiResponse<any>>(`/masterdata/import/upload?codeSystemId=${codeSystemId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getImportTaskStatus(taskId: number) {
  return request.get<ApiResponse<any>>(`/masterdata/import/tasks/${taskId}`)
}
```

- [ ] **Step 2: Commit**

```bash
git add maidc-portal/src/api/masterdata.ts
git commit -m "feat(masterdata): add frontend API module"
```

---

### Task 2: 路由与菜单配置

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

- [ ] **Step 1: 在 asyncRoutes 的 children 数组中添加**

```typescript
{
  path: 'masterdata',
  name: 'MasterData',
  meta: { title: '医疗主数据', icon: 'DatabaseOutlined', sort: 6, permission: 'masterdata' },
  redirect: '/masterdata/code-systems',
  children: [
    { path: 'code-systems', name: 'CodeSystems', meta: { title: '编码体系' }, component: () => import('@/views/masterdata/CodeSystems.vue') },
    { path: 'concepts', name: 'ConceptBrowser', meta: { title: '概念浏览' }, component: () => import('@/views/masterdata/ConceptBrowser.vue') },
    { path: 'mappings', name: 'MappingManager', meta: { title: '编码映射' }, component: () => import('@/views/masterdata/MappingManager.vue') },
    { path: 'clinical-rules', name: 'ClinicalRules', meta: { title: '临床规则' }, component: () => import('@/views/masterdata/ClinicalRules.vue') },
    { path: 'local-concepts', name: 'LocalConceptMapping', meta: { title: '本地编码映射' }, component: () => import('@/views/masterdata/LocalConceptMapping.vue') },
  ],
},
```

- [ ] **Step 2: 创建页面目录**

```bash
mkdir -p maidc-portal/src/views/masterdata
```

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/router/asyncRoutes.ts
git commit -m "feat(masterdata): add routes and menu for masterdata pages"
```

---

### Task 3: 编码体系页面 CodeSystems.vue

**Files:**
- Create: `maidc-portal/src/views/masterdata/CodeSystems.vue`

卡片布局展示各编码体系。每张卡片显示：体系名称、版本号、概念数量、状态。点击卡片跳转概念浏览页。

核心逻辑：
- `onMounted` 调用 `getCodeSystems()` 获取列表
- 遍历 `Promise.all` 调用 `getCodeSystemStats(id)` 获取各体系概念数
- 卡片使用 `a-card` + `a-row/a-col` 布局
- 支持新增编码体系（Modal 表单）

---

### Task 4: 概念浏览页面 ConceptBrowser.vue

**Files:**
- Create: `maidc-portal/src/views/masterdata/ConceptBrowser.vue`

三栏布局：左侧树形层级导航 + 右侧概念详情 + 底部概念列表表格。

核心逻辑：
- 顶部筛选栏：编码体系下拉 + 领域下拉 + 搜索框
- 底部：`a-table` 分页展示概念列表
- 点击概念行：右侧面板展示详情（`a-descriptions`），Tab 切换属性/映射/同义词
- 左侧树：点击编码体系后，加载 `getConceptChildren(rootId)` 构建 `a-tree`

---

### Task 5: 编码映射页面 MappingManager.vue

**Files:**
- Create: `maidc-portal/src/views/masterdata/MappingManager.vue`

核心逻辑：
- 双向下拉选择器（源体系 ↔ 目标体系）
- 表格展示映射关系（源编码、源名称、目标编码、目标名称、关系类型）
- 支持新增映射和批量导入

---

### Task 6: 临床规则页面 ClinicalRules.vue

**Files:**
- Create: `maidc-portal/src/views/masterdata/ClinicalRules.vue`

双 Tab 页。

参考范围 Tab：
- 筛选条件：检验项目 + 性别
- 表格展示参考范围列表
- 新增/编辑 Modal
- "匹配测试"功能区：输入概念+性别+年龄 → 调用 evaluate API

药物相互作用 Tab：
- 筛选条件：药物选择器 + 严重程度
- 表格展示相互作用列表
- "处方审核"功能区：选择一组药物 → 调用 check-list API → 展示结果

---

### Task 7: 本地编码映射页面 LocalConceptMapping.vue

**Files:**
- Create: `maidc-portal/src/views/masterdata/LocalConceptMapping.vue`

核心逻辑：
- 顶部：机构下拉 + 编码体系下拉
- 映射统计卡片行（4个 `a-statistic`：已确认/自动/待确认/未映射）
- 主表格：本地编码、本地名称、标准编码、标准名称、映射状态
- 筛选：映射状态下拉
- 操作：选择未映射行 → 弹出标准概念选择器 → 确认映射
- 批量导入：上传 CSV 文件 → 调用 import API

---

### Task 8: 所有页面 Commit

每个页面完成后单独 commit：

```bash
git add maidc-portal/src/views/masterdata/CodeSystems.vue
git commit -m "feat(masterdata): add code systems page with card layout"

git add maidc-portal/src/views/masterdata/ConceptBrowser.vue
git commit -m "feat(masterdata): add concept browser with tree and detail panel"

git add maidc-portal/src/views/masterdata/MappingManager.vue
git commit -m "feat(masterdata): add mapping manager page"

git add maidc-portal/src/views/masterdata/ClinicalRules.vue
git commit -m "feat(masterdata): add clinical rules page (reference ranges + drug interactions)"

git add maidc-portal/src/views/masterdata/LocalConceptMapping.vue
git commit -m "feat(masterdata): add local concept mapping page with stats and translate"
```
