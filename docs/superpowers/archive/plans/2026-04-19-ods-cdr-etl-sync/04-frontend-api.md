# Phase 4: 前端 API 层 + 路由配置

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 在前端添加 ETL API 函数和路由配置，为后续页面开发做准备。

**Architecture:** 遵循现有 API 层模式（`request.get/post/put/delete`），在 `data.ts` 中添加 ETL 相关函数。路由在 `asyncRoutes.ts` 中新增 ETL 相关页面。

**Tech Stack:** TypeScript / Axios / Vue Router

---

## File Structure

```
maidc-portal/src/
  api/
    etl.ts                            (新建) ETL API 函数
  router/
    asyncRoutes.ts                    (修改) 新增 ETL 路由
```

---

### Task 4.1: 创建 ETL API 文件

**Files:**
- Create: `maidc-portal/src/api/etl.ts`

- [ ] **Step 1: 创建 etl.ts**

```typescript
import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/utils/request'

// ==================== 管道管理 ====================

export function getEtlPipelines(params: {
  page?: number
  page_size?: number
  keyword?: string
  sourceId?: number
  status?: string
  engineType?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/etl/pipelines', { params })
}

export function getEtlPipeline(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/etl/pipelines/${id}`)
}

export function createEtlPipeline(data: Record<string, any>) {
  return request.post<ApiResponse<any>>('/cdr/etl/pipelines', data)
}

export function updateEtlPipeline(id: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/etl/pipelines/${id}`, data)
}

export function deleteEtlPipeline(id: number) {
  return request.delete<ApiResponse<void>>(`/cdr/etl/pipelines/${id}`)
}

export function runEtlPipeline(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/etl/pipelines/${id}/run`)
}

export function validateEtlPipeline(id: number) {
  return request.post<ApiResponse<string[]>>(`/cdr/etl/pipelines/${id}/validate`)
}

export function copyEtlPipeline(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/etl/pipelines/${id}/copy`)
}

export function updateEtlPipelineStatus(id: number, status: string) {
  return request.put<ApiResponse<any>>(`/cdr/etl/pipelines/${id}/status`, { status })
}

export function getEtlPipelineExecutions(id: number, params?: {
  page?: number
  page_size?: number
  status?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>(`/cdr/etl/pipelines/${id}/executions`, { params })
}

// ==================== 步骤管理 ====================

export function getEtlSteps(pipelineId: number) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/pipelines/${pipelineId}/steps`)
}

export function createEtlStep(pipelineId: number, data: Record<string, any>) {
  return request.post<ApiResponse<any>>(`/cdr/etl/pipelines/${pipelineId}/steps`, data)
}

export function updateEtlStep(pipelineId: number, stepId: number, data: Record<string, any>) {
  return request.put<ApiResponse<any>>(`/cdr/etl/pipelines/${pipelineId}/steps/${stepId}`, data)
}

export function deleteEtlStep(pipelineId: number, stepId: number) {
  return request.delete<ApiResponse<void>>(`/cdr/etl/pipelines/${pipelineId}/steps/${stepId}`)
}

export function reorderEtlSteps(pipelineId: number, stepIds: number[]) {
  return request.put<ApiResponse<void>>(`/cdr/etl/pipelines/${pipelineId}/steps/reorder`, { stepIds })
}

export function previewEtlStep(pipelineId: number, stepId: number) {
  return request.post<ApiResponse<any[]>>(`/cdr/etl/pipelines/${pipelineId}/steps/${stepId}/preview`)
}

// ==================== 字段映射 ====================

export function getFieldMappings(stepId: number) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/steps/${stepId}/field-mappings`)
}

export function batchUpdateFieldMappings(stepId: number, mappings: any[]) {
  return request.put<ApiResponse<any[]>>(`/cdr/etl/steps/${stepId}/field-mappings`, mappings)
}

export function autoMapFields(stepId: number) {
  return request.post<ApiResponse<any[]>>(`/cdr/etl/steps/${stepId}/field-mappings/auto-map`)
}

// ==================== 执行记录 ====================

export function getEtlExecutions(params?: {
  page?: number
  page_size?: number
  pipelineId?: number
  stepId?: number
  status?: string
  triggerType?: string
}) {
  return request.get<ApiResponse<PageResult<any>>>('/cdr/etl/executions', { params })
}

export function getEtlExecution(id: number) {
  return request.get<ApiResponse<any>>(`/cdr/etl/executions/${id}`)
}

export function getEtlExecutionLogs(id: number) {
  return request.get<ApiResponse<string>>(`/cdr/etl/executions/${id}/logs`)
}

export function cancelEtlExecution(id: number) {
  return request.post<ApiResponse<void>>(`/cdr/etl/executions/${id}/cancel`)
}

export function retryEtlExecution(id: number) {
  return request.post<ApiResponse<any>>(`/cdr/etl/executions/${id}/retry`)
}

// ==================== 元数据查询 ====================

export function getEtlSchemas() {
  return request.get<ApiResponse<string[]>>('/cdr/etl/metadata/schemas')
}

export function getEtlTables(schema: string) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/metadata/schemas/${schema}/tables`)
}

export function getEtlColumns(schema: string, table: string) {
  return request.get<ApiResponse<any[]>>(`/cdr/etl/metadata/tables/${schema}.${table}/columns`)
}
```

- [ ] **Step 2: 提交**

```bash
git add maidc-portal/src/api/etl.ts
git commit -m "feat(etl): add frontend ETL API layer"
```

---

### Task 4.2: 更新路由配置

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts`

- [ ] **Step 1: 在 data 路由组下新增 ETL 路由**

在 `asyncRoutes.ts` 的 `Data` 路由组 `children` 数组中，在 `SyncTaskList` 路由后面添加：

```typescript
          { path: 'etl/pipelines', name: 'EtlPipelineList', meta: { title: 'ETL管道管理' }, component: () => import('@/views/data-etl/EtlPipelineList.vue') },
          { path: 'etl/pipelines/:id', name: 'EtlPipelineConfig', meta: { title: '管道配置', hidden: true }, component: () => import('@/views/data-etl/EtlPipelineConfig.vue') },
          { path: 'etl/executions', name: 'EtlExecutionList', meta: { title: '执行监控' }, component: () => import('@/views/data-etl/EtlExecutionList.vue') },
```

最终路由顺序（在 `cdr/sync` 之后、`cdr/quality-rules` 之前）：

```typescript
          { path: 'cdr/sync', name: 'SyncTaskList', meta: { title: '数据同步' }, component: () => import('@/views/data-cdr/SyncTaskList.vue') },
          { path: 'etl/pipelines', name: 'EtlPipelineList', meta: { title: 'ETL管道管理' }, component: () => import('@/views/data-etl/EtlPipelineList.vue') },
          { path: 'etl/pipelines/:id', name: 'EtlPipelineConfig', meta: { title: '管道配置', hidden: true }, component: () => import('@/views/data-etl/EtlPipelineConfig.vue') },
          { path: 'etl/executions', name: 'EtlExecutionList', meta: { title: '执行监控' }, component: () => import('@/views/data-etl/EtlExecutionList.vue') },
          { path: 'cdr/quality-rules', name: 'QualityRuleList', ... },
```

- [ ] **Step 2: 创建页面组件目录占位**

```bash
mkdir -p maidc-portal/src/views/data-etl
```

创建三个占位文件确保路由不报错：

`maidc-portal/src/views/data-etl/EtlPipelineList.vue`:
```vue
<template>
  <PageContainer title="ETL管道管理">
    <p>ETL管道管理页面开发中...</p>
  </PageContainer>
</template>
<script setup lang="ts">
import PageContainer from '@/components/PageContainer/index.vue'
defineOptions({ name: 'EtlPipelineList' })
</script>
```

`maidc-portal/src/views/data-etl/EtlPipelineConfig.vue`:
```vue
<template>
  <PageContainer title="管道配置">
    <p>管道配置页面开发中...</p>
  </PageContainer>
</template>
<script setup lang="ts">
import PageContainer from '@/components/PageContainer/index.vue'
defineOptions({ name: 'EtlPipelineConfig' })
</script>
```

`maidc-portal/src/views/data-etl/EtlExecutionList.vue`:
```vue
<template>
  <PageContainer title="执行监控">
    <p>执行监控页面开发中...</p>
  </PageContainer>
</template>
<script setup lang="ts">
import PageContainer from '@/components/PageContainer/index.vue'
defineOptions({ name: 'EtlExecutionList' })
</script>
```

- [ ] **Step 3: 编译验证前端**

Run: `cd maidc-portal && npm run build 2>&1 | tail -5`

Expected: 编译成功，无错误

- [ ] **Step 4: 提交**

```bash
git add maidc-portal/src/router/asyncRoutes.ts
git add maidc-portal/src/views/data-etl/
git commit -m "feat(etl): add ETL routes and placeholder pages"
```

---

## Phase 4 完成标准

- [x] `etl.ts` API 文件包含所有 30+ API 函数
- [x] 路由配置新增 3 个 ETL 页面路由
- [x] 占位页面组件创建，前端编译通过
- [x] 菜单中显示"ETL管道管理"和"执行监控"
