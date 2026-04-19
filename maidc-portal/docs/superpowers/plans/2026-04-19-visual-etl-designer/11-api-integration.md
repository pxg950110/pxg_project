# Task 11: Graph API Client + Save/Load Wiring

**Files:**
- Modify: `src/api/etl.ts`

- [ ] **Step 1: Add graph API endpoints to etl.ts**

Add these functions to the end of `src/api/etl.ts`:

```typescript
// ==================== Graph (Visual Designer) ====================
export function getEtlPipelineGraph(id: number) {
  return request.get<ApiResponse<{ nodes: any[]; edges: any[] }>>(`/cdr/etl/pipelines/${id}/graph`)
}

export function saveEtlPipelineGraph(id: number, graph: { nodes: any[]; edges: any[] }) {
  return request.put<ApiResponse<void>>(`/cdr/etl/pipelines/${id}/graph`, graph)
}

export function previewEtlYaml(id: number) {
  return request.get<ApiResponse<string>>(`/cdr/etl/pipelines/${id}/preview`)
}
```

- [ ] **Step 2: Verify no import conflicts**

The file already has `getEtlPipeline`, `updateEtlPipeline`, etc. The new functions use different names (`getEtlPipelineGraph`, `saveEtlPipelineGraph`, `previewEtlYaml`) so there are no conflicts.

- [ ] **Step 3: Commit**

```bash
git add src/api/etl.ts
git commit -m "feat(etl): add graph API client endpoints for visual designer"
```
