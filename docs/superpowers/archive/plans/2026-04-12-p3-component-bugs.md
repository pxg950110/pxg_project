# P3: 弹窗+组件bug修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 useModal 类型不匹配、DataDashboard StatusBadge、标注路由缺失、模型详情按钮事件 4个bug。

**Architecture:** 逐个修复4个独立bug，每个bug修改1-2个文件。useModal 在 hook 层统一修复避免逐个改调用方。

**Tech Stack:** Vue 3 Composition API, TypeScript, Ant Design Vue

**Spec:** `docs/superpowers/specs/2026-04-12-p3-component-bugs.md`

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `maidc-portal/src/hooks/useModal.ts` | 弹窗 hook 类型修复 |
| 修改 | `maidc-portal/src/views/dashboard/DataDashboard.vue` | StatusBadge 参数修复 |
| 修改 | `maidc-portal/src/router/asyncRoutes.ts` | 标注详情路由添加 |
| 修改 | `maidc-portal/src/views/model/ModelDetail.vue` | 按钮事件添加 |

---

### Task 1: 修复 useModal 类型不匹配

**Files:**
- Modify: `maidc-portal/src/hooks/useModal.ts`

- [ ] **Step 1: 修改 useModal.ts**

当前代码（完整文件）：
```typescript
import { ref } from 'vue'

export function useModal<T = any>() {
  const visible = ref(false)
  const currentRecord = ref<T | null>(null)

  function open(record?: T) {
    currentRecord.value = record ?? null
    visible.value = true
  }

  function close() {
    visible.value = false
    currentRecord.value = null
  }

  return { visible, currentRecord, open, close }
}
```

问题：`visible` 是 `Ref<boolean>`，传给 `<a-modal :open="modal.visible">` 时 Ant Design Vue 期望 `boolean`。在 Vue 3 template 中 `ref` 会自动解包，但如果通过 `v-bind` 传递嵌套对象的属性则不会。

修复：将 `visible` 改为同时提供 `Ref` 和原始值，让调用方可以选择：

```typescript
import { ref, computed } from 'vue'

export function useModal<T = any>() {
  const visibleRef = ref(false)
  const currentRecord = ref<T | null>(null)

  const visible = computed({
    get: () => visibleRef.value,
    set: (val: boolean) => { visibleRef.value = val }
  })

  function open(record?: T) {
    currentRecord.value = record ?? null
    visibleRef.value = true
  }

  function close() {
    visibleRef.value = false
    currentRecord.value = null
  }

  return { visible, currentRecord, open, close }
}
```

`computed` 的 getter 返回 `boolean`，在 template 中直接使用 `modal.visible` 就是 `boolean` 类型，不再有 `Ref` 包装问题。

- [ ] **Step 2: 验证弹窗工作**

用 chrome-devtools 导航到用户列表页，点击"新建"按钮，确认弹窗正常打开。

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/hooks/useModal.ts
git commit -m "fix: useModal type mismatch - use computed instead of ref for visible"
```

---

### Task 2: 修复 DataDashboard StatusBadge

**Files:**
- Modify: `maidc-portal/src/views/dashboard/DataDashboard.vue:122-195`

- [ ] **Step 1: 修改 ETL 任务数据中的 statusLabel 和 statusType**

当前代码（Lines 122-195 中的数据）：
```typescript
const etlTasks = ref([
  {
    id: '1',
    name: '影像数据增量同步',
    type: '增量同步',
    statusLabel: '已完成',
    statusType: 'success',
    dataSize: 128.5,
    completedAt: '2026-04-11 09:32:00',
  },
```

StatusBadge 的 `type` prop 期望 `StatusType` 枚举值：`'model' | 'version' | 'deploy' | 'eval' | 'approval' | 'alert' | 'medication' | 'encounter' | 'connection' | 'sync' | 'quality'`

对于 ETL 任务应使用 `'sync'` 类型，其有效状态值为：`RUNNING | COMPLETED | FAILED | PENDING | CANCELLED`

替换整个 etlTasks 数据：
```typescript
const etlTasks = ref([
  {
    id: '1',
    name: '影像数据增量同步',
    type: '增量同步',
    statusLabel: 'COMPLETED',
    statusType: 'sync' as const,
    dataSize: 128.5,
    completedAt: '2026-04-11 09:32:00',
  },
  {
    id: '2',
    name: '检验数据全量同步',
    type: '全量同步',
    statusLabel: 'RUNNING',
    statusType: 'sync' as const,
    dataSize: 256.3,
    completedAt: '',
  },
  {
    id: '3',
    name: '病历数据增量同步',
    type: '增量同步',
    statusLabel: 'FAILED',
    statusType: 'sync' as const,
    dataSize: 64.2,
    completedAt: '2026-04-10 15:20:00',
  },
  {
    id: '4',
    name: '用药数据全量同步',
    type: '全量同步',
    statusLabel: 'PENDING',
    statusType: 'sync' as const,
    dataSize: 0,
    completedAt: '',
  },
  {
    id: '5',
    name: '手术数据增量同步',
    type: '增量同步',
    statusLabel: 'COMPLETED',
    statusType: 'sync' as const,
    dataSize: 89.7,
    completedAt: '2026-04-11 11:45:00',
  },
])
```

- [ ] **Step 2: 验证 DataDashboard 无报错**

导航到 `/dashboard/data`，确认页面正常渲染，ETL 表格状态列显示正确的状态标签（绿色"已完成"、蓝色"运行中"、红色"失败"等）。

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/views/dashboard/DataDashboard.vue
git commit -m "fix: DataDashboard StatusBadge - use correct sync StatusType enum values"
```

---

### Task 3: 添加标注详情路由

**Files:**
- Modify: `maidc-portal/src/router/asyncRoutes.ts:55-64`

- [ ] **Step 1: 在 label 路由组添加 detail 路由**

当前代码（Lines 55-64）：
```typescript
{
  path: 'label',
  name: 'Label',
  meta: { title: '标注管理', icon: 'EditOutlined', sort: 4, permission: 'label' },
  redirect: '/label/tasks',
  children: [
    { path: 'tasks', name: 'LabelTaskList', meta: { title: '标注任务' }, component: () => import('@/views/label/LabelTaskList.vue') },
    { path: 'workspace/:id', name: 'LabelWorkspace', meta: { title: '标注工作台', hidden: true }, component: () => import('@/views/label/LabelWorkspace.vue') },
  ],
},
```

在 `workspace/:id` 行后添加 detail 路由：
```typescript
{
  path: 'label',
  name: 'Label',
  meta: { title: '标注管理', icon: 'EditOutlined', sort: 4, permission: 'label' },
  redirect: '/label/tasks',
  children: [
    { path: 'tasks', name: 'LabelTaskList', meta: { title: '标注任务' }, component: () => import('@/views/label/LabelTaskList.vue') },
    { path: 'workspace/:id', name: 'LabelWorkspace', meta: { title: '标注工作台', hidden: true }, component: () => import('@/views/label/LabelWorkspace.vue') },
    { path: 'detail/:id', name: 'LabelTaskDetail', meta: { title: '标注详情', hidden: true }, component: () => import('@/views/label/LabelTaskDetail.vue') },
  ],
},
```

- [ ] **Step 2: 验证路由存在**

如果 `LabelTaskDetail.vue` 不存在，需先检查：
```bash
ls maidc-portal/src/views/label/
```

如果文件不存在，创建一个简单的详情页占位：
```typescript
// maidc-portal/src/views/label/LabelTaskDetail.vue
<template>
  <PageContainer title="标注详情">
    <a-descriptions :column="2">
      <a-descriptions-item label="任务ID">{{ route.params.id }}</a-descriptions-item>
    </a-descriptions>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import PageContainer from '@/components/PageContainer/index.vue'

const route = useRoute()
</script>
```

- [ ] **Step 3: 验证路由可访问**

导航到 `/label/detail/1`，确认页面正常显示（不再404）。

- [ ] **Step 4: Commit**

```bash
git add maidc-portal/src/router/asyncRoutes.ts maidc-portal/src/views/label/LabelTaskDetail.vue
git commit -m "fix: add missing label detail route"
```

---

### Task 4: 模型详情按钮事件

**Files:**
- Modify: `maidc-portal/src/views/model/ModelDetail.vue`

- [ ] **Step 1: 为编辑按钮添加事件**

当前（Line 20）：
```vue
<a-button type="primary">编辑</a-button>
```

改为：
```vue
<a-button type="primary" @click="showEditModal">编辑</a-button>
```

- [ ] **Step 2: 为注册版本按钮添加事件**

当前（Line 21）：
```vue
<a-button type="primary">注册新版本</a-button>
```

改为：
```vue
<a-button type="primary" @click="showVersionModal">注册新版本</a-button>
```

- [ ] **Step 3: 在 script setup 中添加事件处理函数**

在 ModelDetail.vue 的 script 区域添加：
```typescript
import { message } from 'ant-design-vue'

const editModal = useModal()
const versionModal = useModal()

function showEditModal() {
  editModal.open()
}

function showVersionModal() {
  versionModal.open()
}

function handleEditSubmit() {
  message.success('模型信息已更新')
  editModal.close()
}

function handleVersionSubmit() {
  message.success('版本注册成功')
  versionModal.close()
}
```

- [ ] **Step 4: 为下载按钮添加事件**

当前：
```vue
<a-button type="link" size="small">下载</a-button>
```

改为：
```vue
<a-button type="link" size="small" @click="handleDownload(record)">下载</a-button>
```

添加处理函数：
```typescript
function handleDownload(record: any) {
  message.info(`开始下载版本 ${record.version}`)
}
```

- [ ] **Step 5: 验证按钮可点击**

导航到模型详情页，点击"编辑"和"注册新版本"按钮，确认弹窗打开。

- [ ] **Step 6: Commit**

```bash
git add maidc-portal/src/views/model/ModelDetail.vue
git commit -m "feat: add click handlers to ModelDetail buttons"
```
