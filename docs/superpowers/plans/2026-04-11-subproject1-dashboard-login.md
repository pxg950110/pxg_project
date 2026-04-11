# Sub-project 1: Dashboard + Login — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the login page and 3 dashboard pages (Overview, Model, Data) with the .pen design for pixel-perfect accuracy.

**Architecture:** Vue 3 SFC components using Ant Design Vue. Dashboard pages use shared MetricCard/MetricChart components. Data is currently mocked — no backend API integration in this phase (static data).

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + ECharts (vue-echarts)

---

## Design vs Code: Gap Analysis

### Login Page (01-登录页)
| Aspect | Design | Current Code | Action |
|--------|--------|-------------|--------|
| Left panel width | 50% (720px) | 700px fixed | Change to 50% |
| Left background | Dark navy gradient | Dark navy gradient | OK |
| Welcome title | "MAIDC 医疗AI数据中心" | Same | OK |
| Features list | 3 items with checkmarks | Same | OK |
| Form title | "欢迎回来" | Same | OK |
| Form fields | Username + Password | Same | OK |
| SSO button | "医院统一认证登录" | Same | OK |
| Security hints | Bottom warnings | Same | OK |
| Footer | © 2026 MAIDC | Same | OK |

**Login page verdict:** Nearly aligned. Only change needed: left panel width from 700px to 50%.

### Dashboard Overview (02-Dashboard工作台) — MAJOR CHANGES
| Aspect | Design | Current Code | Action |
|--------|--------|-------------|--------|
| Welcome section | "早上好，张医生" + date + pending tasks + 3 action buttons | None | **ADD** |
| Metric cards | 6 cards (模型总数, 活跃部署, 今日推理次数, 患者记录, 研究项目, 待审批) | 4 cards | **CHANGE to 6** |
| Model status | Horizontal bar chart (DRAFT/REGISTERED/PUBLISHED/DEPRECATED) | Pie chart | **CHANGE** |
| Recent alerts | Separate card with CRITICAL/WARNING badges | None | **ADD** |
| Recent activity | List with category tags (告警/审批/部署/评估/ETL/模型) | Timeline without tags | **REDESIGN** |
| Data source status | Section at bottom | None | **ADD** |

### DataDashboard / ModelDashboard
These two pages are not directly shown in the "02-Dashboard工作台" design page — they appear to be additional tab views. The current mock data implementation is reasonable. Minor alignment only.

---

## File Structure

### Files to Modify
| File | Responsibility |
|------|---------------|
| `maidc-portal/src/views/login/LoginPage.vue` | Fix left panel width to 50% |
| `maidc-portal/src/views/dashboard/Overview.vue` | Major redesign: welcome section, 6 metrics, bar chart, alerts, activity feed |
| `maidc-portal/src/views/dashboard/DataDashboard.vue` | Minor: adjust layout spacing to match design style |
| `maidc-portal/src/views/dashboard/ModelDashboard.vue` | Minor: adjust layout spacing to match design style |

### Files to Create
| File | Responsibility |
|------|---------------|
| None — all changes within existing files |

---

## Task 1: Fix Login Page Layout

**Files:**
- Modify: `maidc-portal/src/views/login/LoginPage.vue`

- [ ] **Step 1: Update left panel CSS from fixed 700px to 50%**

Change the `.login-left` class:

```css
.login-left {
  width: 50%;
  min-width: 50%;
  background: linear-gradient(180deg, #1E293B 0%, #334155 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
```

Remove the `.left-content` width constraint (480px on welcome-sub) and update to `width: 100%; max-width: 560px;`.

- [ ] **Step 2: Verify visually**

Run: `cd maidc-portal && npx vite --port 3000`
Open http://localhost:3000/login and verify left panel takes exactly 50% width.

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/views/login/LoginPage.vue
git commit -m "fix: login page left panel width to 50% for design alignment"
```

---

## Task 2: Redesign Overview Dashboard — Welcome Section + 6 Metric Cards

**Files:**
- Modify: `maidc-portal/src/views/dashboard/Overview.vue`

This is the largest task. The Overview page needs a complete redesign to match the design: personalized welcome header, 6 metric cards (3x2 grid), model status bar chart, alerts section, activity feed with tags, and data source status.

- [ ] **Step 1: Rewrite Overview.vue with welcome section and 6 metric cards**

Replace the entire `<template>` and `<script setup>` content. The new template:

```vue
<template>
  <PageContainer title="" subtitle="">
    <!-- Welcome Section -->
    <a-card :bordered="false" class="welcome-card">
      <div class="welcome-content">
        <div class="welcome-left">
          <h2 class="welcome-greeting">{{ greetingText }}，{{ userName }}</h2>
          <p class="welcome-date">今天是 {{ currentDate }}</p>
          <p class="welcome-todo">您有 <a-tag color="blue">{{ pendingCount }}</a-tag> 条待办事项</p>
        </div>
        <div class="welcome-actions">
          <a-button type="primary" @click="$router.push('/model/list')">
            <template #icon><PlusOutlined /></template>
            注册模型
          </a-button>
          <a-button @click="$router.push('/model/evaluations')">
            <template #icon><LineChartOutlined /></template>
            新建评估
          </a-button>
          <a-button @click="$router.push('/model/approvals')">
            <template #icon><AuditOutlined /></template>
            提交审批
          </a-button>
        </div>
      </div>
    </a-card>

    <!-- Metric Cards Row 1 -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="4" v-for="card in metricCards" :key="card.title">
        <MetricCard
          :title="card.title"
          :value="card.value"
          :suffix="card.suffix"
          :trend="card.trend"
          :loading="loading"
        >
          <template #icon><component :is="card.icon" /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Model Status Distribution + Recent Alerts -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="模型状态分布" :bordered="false">
          <template #extra><a @click="$router.push('/model/list')">查看全部</a></template>
          <MetricChart :option="modelStatusOption" :height="280" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="最近告警" :bordered="false">
          <template #extra><a @click="$router.push('/alert/active')">查看全部</a></template>
          <div class="alert-list">
            <div v-for="alert in recentAlerts" :key="alert.id" class="alert-item">
              <a-tag :color="alert.severity === 'CRITICAL' ? 'red' : 'orange'">
                {{ alert.severity }}
              </a-tag>
              <span class="alert-message">{{ alert.message }}</span>
              <span class="alert-time">{{ alert.time }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Recent Activity + Data Source Status -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近活动" :bordered="false">
          <template #extra><a>查看全部</a></template>
          <div class="activity-list">
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
              <div class="activity-dot" :style="{ background: activity.dotColor }"></div>
              <div class="activity-body">
                <div class="activity-text">{{ activity.text }}</div>
                <div class="activity-meta">
                  <a-tag :color="activity.tagColor" size="small">{{ activity.category }}</a-tag>
                  <span class="activity-time">{{ activity.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="数据源连接状态" :bordered="false">
          <div class="datasource-list">
            <div v-for="ds in dataSources" :key="ds.name" class="datasource-item">
              <span class="ds-name">{{ ds.name }}</span>
              <a-badge :status="ds.connected ? 'success' : 'error'" :text="ds.connected ? '已连接' : '断开'" />
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>
```

The new `<script setup>`:

```typescript
import { ref, reactive, computed, type Component } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  ExperimentOutlined,
  RocketOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  ProjectOutlined,
  AuditOutlined,
  PlusOutlined,
  LineChartOutlined,
  DatabaseOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'

const authStore = useAuthStore()
const loading = ref(false)

// ============ Welcome Section ============
const userName = computed(() => authStore.userInfo?.realName ?? '用户')

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const currentDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const pendingCount = ref(3)

// ============ 6 Metric Cards ============
const metricCards = reactive([
  { title: '模型总数', value: 28, suffix: '个', icon: ExperimentOutlined as Component, trend: { value: 8, type: 'up' as const } },
  { title: '活跃部署', value: 8, suffix: '个', icon: RocketOutlined as Component, trend: { value: 3, type: 'up' as const } },
  { title: '今日推理次数', value: 12456, suffix: '次', icon: ThunderboltOutlined as Component, trend: { value: 15, type: 'up' as const } },
  { title: '患者记录', value: 156000, suffix: '条', icon: TeamOutlined as Component, trend: { value: 234, type: 'up' as const } },
  { title: '研究项目', value: 12, suffix: '个', icon: ProjectOutlined as Component, trend: { value: 2, type: 'up' as const } },
  { title: '待审批', value: 5, suffix: '条', icon: AuditOutlined as Component, trend: undefined },
])

// ============ Model Status Bar Chart ============
const modelStatusOption = {
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '8%', bottom: '3%', top: '8%', containLabel: true },
  xAxis: { type: 'value', name: '数量' },
  yAxis: {
    type: 'category',
    data: ['DEPRECATED', 'PUBLISHED', 'REGISTERED', 'DRAFT'],
  },
  series: [{
    type: 'bar',
    barWidth: '50%',
    data: [
      { value: 5, itemStyle: { color: '#faad14' } },
      { value: 18, itemStyle: { color: '#52c41a' } },
      { value: 2, itemStyle: { color: '#1677ff' } },
      { value: 3, itemStyle: { color: '#d9d9d9' } },
    ],
    itemStyle: { borderRadius: [0, 4, 4, 0] },
    label: { show: true, position: 'right', formatter: '{c}' },
  }],
}

// ============ Recent Alerts ============
const recentAlerts = ref([
  { id: 1, severity: 'CRITICAL', message: '推理延迟P99超阈值', time: '10分钟前' },
  { id: 2, severity: 'WARNING', message: 'GPU使用率 > 90%', time: '1小时前' },
  { id: 3, severity: 'WARNING', message: '磁盘空间 < 20%', time: '3小时前' },
])

// ============ Recent Activity ============
const recentActivities = ref([
  { id: 1, text: '肺结节检测-生产 推理延迟异常', time: '10分钟前', category: '告警', tagColor: 'red', dotColor: '#ff4d4f' },
  { id: 2, text: '张医生提交了 肺结节检测v2.3.1 上线审批', time: '32分钟前', category: '审批', tagColor: 'purple', dotColor: '#722ed1' },
  { id: 3, text: 'v2.3.1 外部验证集评估完成 (AUC: 0.987)', time: '1小时前', category: '评估', tagColor: 'blue', dotColor: '#1677ff' },
  { id: 4, text: '糖尿病视网膜病变检测 已上线生产环境', time: '2小时前', category: '部署', tagColor: 'green', dotColor: '#52c41a' },
  { id: 5, text: '影像数据集 v3.0 已完成 ETL 导入', time: '3小时前', category: 'ETL', tagColor: 'cyan', dotColor: '#13c2c2' },
  { id: 6, text: '心血管风险评估模型 v1.2 注册完成', time: '5小时前', category: '模型', tagColor: 'geekblue', dotColor: '#2f54eb' },
])

// ============ Data Source Status ============
const dataSources = ref([
  { name: 'HIS 系统', connected: true },
  { name: 'LIS 检验系统', connected: true },
  { name: 'PACS 影像系统', connected: true },
  { name: 'EMR 电子病历', connected: false },
])
```

- [ ] **Step 2: Add scoped CSS for the new sections**

```css
<style scoped>
.welcome-card {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border-radius: 8px;
}
.welcome-card :deep(.ant-card-body) {
  padding: 24px 32px;
}
.welcome-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.welcome-greeting {
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}
.welcome-date {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  margin: 0 0 4px;
}
.welcome-todo {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  margin: 0;
}
.welcome-actions {
  display: flex;
  gap: 12px;
}
.welcome-actions .ant-btn {
  border-radius: 6px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.alert-message {
  flex: 1;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}
.alert-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  white-space: nowrap;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.activity-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.activity-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.activity-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}
.activity-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.activity-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}

.datasource-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.datasource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.datasource-item:last-child {
  border-bottom: none;
}
.ds-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}
</style>
```

- [ ] **Step 3: Verify the page renders correctly**

Run: `cd maidc-portal && npx vite --port 3000`
Navigate to http://localhost:3000/dashboard/overview after login.
Check:
- Welcome card with blue gradient at top
- 6 metric cards in a row (may wrap to 2 rows depending on span)
- Model status horizontal bar chart
- Recent alerts with severity tags
- Activity feed with colored dots and category tags
- Data source status with connection badges

- [ ] **Step 4: Fix metric card grid to match design's 3x2 layout**

If 6 cards with `span=4` don't match the design (which shows them in a single row of 6), adjust:

```vue
<a-col :span="4" v-for="card in metricCards" :key="card.title">
```

Each card at span=4 = 6 columns in a 24-column grid. This matches a single row of 6 cards.

- [ ] **Step 5: Commit**

```bash
git add maidc-portal/src/views/dashboard/Overview.vue
git commit -m "feat: redesign dashboard overview with welcome section, 6 metrics, alerts, activity feed"
```

---

## Task 3: Adjust DataDashboard Layout

**Files:**
- Modify: `maidc-portal/src/views/dashboard/DataDashboard.vue`

The DataDashboard is mostly correct. Minor alignment tweaks to match the design's visual density.

- [ ] **Step 1: Update metric card suffixes and values to match design style**

The current metric cards show good data. No major changes needed — the design doesn't have a specific DataDashboard page separate from Overview. Keep current implementation but ensure consistent spacing.

No code changes required for this task — the DataDashboard already follows the same component patterns as the redesigned Overview.

- [ ] **Step 2: Commit (if any changes made, otherwise skip)**

```bash
git add maidc-portal/src/views/dashboard/DataDashboard.vue
git commit -m "style: minor dashboard spacing alignment"
```

---

## Task 4: Adjust ModelDashboard Layout

**Files:**
- Modify: `maidc-portal/src/views/dashboard/ModelDashboard.vue`

Same as DataDashboard — the ModelDashboard already follows consistent patterns. Verify it renders correctly with the shared MetricCard/MetricChart components after the Overview changes.

- [ ] **Step 1: Verify no regressions**

Run: `cd maidc-portal && npx vite --port 3000`
Navigate to /dashboard/model and verify all charts and metrics render.

- [ ] **Step 2: Commit (if any changes made, otherwise skip)**

```bash
git add maidc-portal/src/views/dashboard/ModelDashboard.vue
git commit -m "style: minor model dashboard alignment"
```

---

## Task 5: Final Verification + Build Test

**Files:** None

- [ ] **Step 1: Run TypeScript type check**

```bash
cd maidc-portal && npx vue-tsc --noEmit
```

Expected: No errors. Fix any type issues in modified files.

- [ ] **Step 2: Run production build**

```bash
cd maidc-portal && npm run build
```

Expected: Build succeeds with no errors.

- [ ] **Step 3: Manual visual verification**

Open http://localhost:3000 and verify:
1. Login page: left panel 50% width, all elements present
2. After login, Overview shows: welcome card (blue gradient), 6 metrics, bar chart, alerts, activity, data sources
3. Model Dashboard renders correctly
4. Data Dashboard renders correctly

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: sub-project 1 complete — login and dashboard aligned with design"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** Login page (Task 1), Dashboard Overview (Task 2), DataDashboard (Task 3), ModelDashboard (Task 4) — all covered
- [x] **Placeholder scan:** No TBD/TODO — all code is concrete
- [x] **Type consistency:** All imports and component references match existing codebase patterns (Ant Design Vue components, icon imports, store usage)
