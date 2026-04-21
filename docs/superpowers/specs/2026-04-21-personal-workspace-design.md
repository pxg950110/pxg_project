# Personal Workspace (个人工作台) Design

## Overview

Transform the existing `/dashboard/overview` into a unified personal workspace entry point. The workspace serves as the user's landing page after login, aggregating personal tasks, key metrics, notifications, and quick actions in a classic split-panel layout.

## Architecture

### Route Changes

| Route | Description | Change |
|-------|-------------|--------|
| `/dashboard` | Redirect target | Redirect to `/dashboard/workspace` (was `/dashboard/overview`) |
| `/dashboard/workspace` | Personal workspace | **New** — primary entry point |
| `/dashboard/overview` | System overview | Retained as admin/system perspective |
| `/dashboard/model` | Model dashboard | Unchanged |
| `/dashboard/data` | Data dashboard | Unchanged |

### Backend: Aggregated API

**Endpoint**: `GET /api/v1/workspace/dashboard`

**Service**: `WorkspaceController` in `maidc-task` microservice

**Response Structure**:

```json
{
  "welcome": {
    "userName": "张医生",
    "date": "2026-04-21",
    "role": "数据管理员"
  },
  "metrics": {
    "modelCount": 28,
    "activeDeployments": 8,
    "dailyInferences": 12456,
    "pendingApprovals": 5
  },
  "todos": [
    {
      "id": 1,
      "type": "APPROVAL",
      "title": "模型 v2.1 审批",
      "priority": "HIGH",
      "status": "PENDING",
      "sourceId": 1024,
      "sourceType": "APPROVAL",
      "dueDate": "2026-04-25T00:00:00",
      "createdAt": "2026-04-20T10:30:00"
    }
  ],
  "notifications": [
    {
      "id": 1,
      "type": "SYSTEM",
      "title": "模型 X 已成功部署",
      "content": "...",
      "isRead": false,
      "createdAt": "2026-04-21T09:00:00"
    }
  ],
  "quickActions": [
    { "key": "new_model", "label": "新建模型", "icon": "plus-outlined", "route": "/model/list" },
    { "key": "patient_query", "label": "患者查询", "icon": "search-outlined", "route": "/data/cdr/patients" },
    { "key": "new_evaluation", "label": "新建评估", "icon": "experiment-outlined", "route": "/model/evaluations" },
    { "key": "etl_task", "label": "ETL任务", "icon": "thunderbolt-outlined", "route": "/data/etl/pipelines" }
  ]
}
```

### Data Sources

| Module | Source | Method |
|--------|--------|--------|
| metrics | model, data services | Feign: model count/deployments/inferences + patient count |
| todos | personal_task table (local) | Direct DB query |
| notifications | maidc-msg service | Feign: `GET /api/v1/messages?is_read=false&pageSize=5` |
| quickActions | Role-based config | Hardcoded per role |

## Database: personal_task Table

Located in `maidc_task` schema:

```sql
CREATE TABLE maidc_task.personal_task (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    task_type       VARCHAR(20) NOT NULL,    -- APPROVAL, LABELING, DATA_QUERY, OTHER
    priority        VARCHAR(10) DEFAULT 'MEDIUM',  -- HIGH, MEDIUM, LOW
    status          VARCHAR(10) DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    assignee_id     BIGINT NOT NULL,
    source_id       BIGINT,
    source_type     VARCHAR(20),
    due_date        TIMESTAMP,
    org_id          BIGINT NOT NULL DEFAULT 1,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_personal_task_assignee_status ON maidc_task.personal_task(assignee_id, status);
```

**Task creation triggers**:
- Model approval submitted → `maidc-model` sends MQ message → `maidc-task` creates personal_task
- Label task assigned → `maidc-label` sends MQ message → `maidc-task` creates personal_task

**MQ Message format** (follows existing project convention):
```json
{
  "entityType": "APPROVAL",
  "entityId": 1024,
  "action": "CREATED",
  "assigneeId": 5,
  "title": "模型 v2.1 审批",
  "priority": "HIGH"
}
```

## Frontend Page Layout

```
┌─────────────────────────────────────────────────┐
│  WelcomeSection (100%)                           │
│  "欢迎回来，{userName}" | 日期 | 角色             │
├────────┬────────┬────────┬───────────────────────┤
│ MetricCards (4-column grid)                      │
│ 模型总数  活跃部署  今日推理  待审批数              │
├────────┴────────┴────────┼───────────────────────┤
│  TodoSection (60%)        │  NotifySection (40%)   │
│  个人待办任务列表           │  消息通知列表           │
├──────────────────────────┴───────────────────────┤
│  QuickActions (100%)                              │
│  [新建模型] [患者查询] [新建评估] [ETL任务] [更多]   │
└─────────────────────────────────────────────────┘
```

### Component Structure

```
maidc-portal/src/views/dashboard/workspace/
├── WorkspaceView.vue       — Main container, calls API, manages loading
├── WelcomeSection.vue      — User greeting and date
├── MetricCards.vue         — 4 metric cards (reuses existing MetricCard)
├── TodoSection.vue         — Todo list with type filter tabs
├── NotifySection.vue       — Notification list with unread indicators
└── QuickActions.vue        — Quick action button group
```

### Pinia Store

- `useWorkspaceStore` — workspace data state, fetch/refresh actions
- Reuses `useMessageStore` for notification interactions

## Interaction Details

### Todo List
- Click item → navigate to source business page (approval → `/model/approvals/:id`, labeling → `/label/workspace/:id`)
- Hover → show "Complete" button, click marks as done (`PUT /api/v1/workspace/todos/{id}/complete`)
- Filter tabs: All / Approvals / Labeling / Other (default: All)
- Empty state: "暂无待办任务" with illustration

### Notification List
- Unread messages: left blue vertical bar indicator
- Click → mark as read + navigate to related page
- "Mark all read" button → `PUT /api/v1/messages/read-all`
- Type icons: system, alert, approval

### Quick Actions
- Fixed button row, each button = a-button + icon + label
- "More" button expands secondary actions
- Actions filtered by user role (admin sees all, regular user sees common)

### Refresh
- One-time fetch on page load
- Manual "Refresh" button for user-triggered refresh
- No auto-refresh or WebSocket (YAGNI for current phase)

## Error Handling

- **Partial aggregation failure**: Return successful modules, failed modules show "数据加载失败" + retry button
- **Timeout (>3s)**: Skeleton loading state + timeout message
- **401 Unauthorized**: Redirect to login (existing interceptor handles this)

## Backend Service Classes

| Class | Responsibility |
|-------|---------------|
| `WorkspaceController` | `GET /api/v1/workspace/dashboard` aggregation endpoint |
| `WorkspaceService` | Aggregation logic: local personal_task + Feign to model/data/msg |
| `PersonalTaskRepository` | personal_task table CRUD |
| `PersonalTaskService` | Personal task business logic, MQ message consumers |

## Scope

- Frontend: 6 new Vue components, 1 new Pinia store, route changes
- Backend: 1 new controller, 1 new service, 1 new repository, 1 new database table, 2 MQ consumers
- No new microservice — all backend work within existing `maidc-task`
