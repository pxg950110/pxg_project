# 04 — 前端详情 + 统计 + 导出

**Goal:** 实现专病详情页，包含基本信息、统计看板、患者列表、手动添加、CSV 导出。

**Files:**
- Create: `maidc-portal/src/views/data-cdr/DiseaseDetail.vue`

---

## Task 4.1: DiseaseDetail 页面

- [ ] **Step 1: 创建 `src/views/data-cdr/DiseaseDetail.vue`**

**模板结构：**

1. **面包屑 + 操作区**：`数据管理 > 专病管理 > {name}`，extra 区域放 [手动同步] [返回] 按钮

2. **基本信息卡片** `<a-card title="基本信息">`
   - `<a-descriptions>` 展示：专病名称、状态标签、自动同步开关(a-switch)、纳入规则展示、最后同步时间、创建时间
   - 纳入规则用只读模式渲染分组摘要（同 DiseaseCard 的摘要展示）

3. **统计指标行** `<a-row :gutter="16">`
   - 4 个 MetricCard：患者总数、男性占比、平均年龄、近30天新增

4. **患者列表区** `<a-card title="患者列表">`
   - extra：[手动添加] [导出] 按钮
   - `<a-table>` columns：患者姓名、性别、年龄、首次诊断时间、匹配来源(AUTO蓝/MANUAL橙标签)、操作(移除)
   - 分页

5. **手动添加弹窗**：简单搜索患者 + 选择添加

**Script 逻辑：**

```typescript
const route = useRoute()
const cohortId = Number(route.params.id)

async function loadCohort() { /* getDiseaseCohort */ }
async function loadStatistics() { /* getDiseaseCohortStatistics */ }
async function loadPatients() { /* getDiseaseCohortPatients */ }
async function handleSync() { /* syncDiseaseCohort */ }
async function handleRemovePatient(patientId) { /* removeDiseaseCohortPatient */ }
async function handleExport() { /* exportDiseaseCohort -> blob download */ }
async function handleAddPatient(patientId) { /* addDiseaseCohortPatient */ }
```

- [ ] **Step 2: 浏览器验证**

从列表页点"详情"跳转，验证：
- 基本信息正确展示
- 统计数字加载
- 患者列表分页
- 移除患者确认弹窗 + 调用成功
- 导出按钮下载 CSV

- [ ] **Step 3: Commit**

```bash
git add maidc-portal/src/views/data-cdr/DiseaseDetail.vue
git commit -m "feat(disease): add disease detail page with statistics and export"
```
