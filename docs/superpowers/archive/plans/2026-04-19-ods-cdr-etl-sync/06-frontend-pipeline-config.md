# Phase 6: 管道配置页 (EtlPipelineConfig.vue) -- 核心

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 实现管道配置核心页面，包含步骤编排、字段映射表、转换规则配置、自动映射等交互。

**Architecture:** 页面分为头部（管道信息）+ 步骤卡片流 + 步骤配置面板（源表/目标表选择 + 字段映射表 + 转换规则）+ 高级配置。使用 Ant Design Vue 表单组件 + 自定义字段映射交互。

**Tech Stack:** Vue 3 / TypeScript / Ant Design Vue

---

## File Structure

```
maidc-portal/src/views/data-etl/
  EtlPipelineConfig.vue               (修改) 替换占位内容
```

---

### Task 6.1: 实现 EtlPipelineConfig.vue

**Files:**
- Modify: `maidc-portal/src/views/data-etl/EtlPipelineConfig.vue`

这是最复杂的页面，分三部分编写：头部区域 + 步骤编排 + 字段映射面板。

- [ ] **Step 1: 编写完整页面**

```vue
<template>
  <PageContainer :title="isEdit ? '编辑管道' : '新建管道'" @back="handleBack">
    <template #extra>
      <a-space>
        <a-button @click="handleValidate">
          <CheckCircleOutlined /> 校验
        </a-button>
        <a-button type="primary" @click="handleSave" :loading="saving">
          保存
        </a-button>
      </a-space>
    </template>

    <!-- 头部：管道基础信息 -->
    <a-card title="基本信息" :bordered="false" style="margin-bottom: 16px">
      <a-form layout="inline">
        <a-form-item label="管道名称">
          <a-input v-model:value="pipeline.pipelineName" style="width: 200px" />
        </a-form-item>
        <a-form-item label="数据源">
          <a-select v-model:value="pipeline.sourceId" style="width: 200px" disabled>
            <a-select-option v-for="ds in dataSources" :key="ds.id" :value="ds.id">
              {{ ds.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="同步模式">
          <a-radio-group v-model:value="pipeline.syncMode" size="small">
            <a-radio-button value="MANUAL">手动</a-radio-button>
            <a-radio-button value="INCREMENTAL">增量</a-radio-button>
            <a-radio-button value="FULL">全量</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 步骤编排区 -->
    <a-card title="步骤编排" :bordered="false" style="margin-bottom: 16px">
      <template #extra>
        <a-button type="dashed" size="small" @click="handleAddStep">
          <PlusOutlined /> 添加步骤
        </a-button>
      </template>

      <a-empty v-if="steps.length === 0" description="暂无步骤，请点击添加" />

      <div v-else style="display: flex; gap: 12px; overflow-x: auto; padding: 8px 0">
        <a-card
          v-for="(step, idx) in steps"
          :key="step.id || idx"
          size="small"
          :style="{
            width: 200,
            cursor: 'pointer',
            border: activeStepIndex === idx ? '2px solid #1890ff' : '1px solid #d9d9d9',
          }"
          @click="activeStepIndex = idx"
        >
          <template #title>
            <span style="font-size: 13px">{{ step.stepName || `步骤 ${idx + 1}` }}</span>
          </template>
          <template #extra>
            <a-button type="text" danger size="small" @click.stop="handleRemoveStep(idx)">
              <DeleteOutlined />
            </a-button>
          </template>
          <div style="font-size: 12px; color: #999">
            {{ step.sourceTable || '?' }} → {{ step.targetTable || '?' }}
          </div>
          <a-tag size="small" style="margin-top: 4px">{{ step.stepType || 'ONE_TO_ONE' }}</a-tag>
        </a-card>
      </div>
    </a-card>

    <!-- 步骤配置面板 -->
    <a-card v-if="activeStep" :title="`步骤配置: ${activeStep.stepName || '新步骤'}`" :bordered="false" style="margin-bottom: 16px">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="步骤名称">
            <a-input v-model:value="activeStep.stepName" placeholder="步骤名称" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="映射类型">
            <a-select v-model:value="activeStep.stepType" style="width: 100%">
              <a-select-option value="ONE_TO_ONE">1:1</a-select-option>
              <a-select-option value="ONE_TO_MANY">1:N</a-select-option>
              <a-select-option value="MANY_TO_ONE">N:1</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="错误处理">
            <a-select v-model:value="activeStep.onError" style="width: 100%">
              <a-select-option value="ABORT">中止</a-select-option>
              <a-select-option value="SKIP">跳过</a-select-option>
              <a-select-option value="RETRY">重试</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="源表">
            <a-input-group compact>
              <a-select v-model:value="activeStep.sourceSchema" style="width: 30%" placeholder="schema"
                        @change="loadTablesForSchema('source')">
                <a-select-option v-for="s in schemas" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
              <a-select v-model:value="activeStep.sourceTable" style="width: 70%" placeholder="选择表"
                        show-search @change="loadColumnsForTable('source')">
                <a-select-option v-for="t in sourceTables" :key="t.table_name" :value="t.table_name">
                  {{ t.table_name }}
                </a-select-option>
              </a-select>
            </a-input-group>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="目标表">
            <a-input-group compact>
              <a-select v-model:value="activeStep.targetSchema" style="width: 30%" placeholder="schema"
                        @change="loadTablesForSchema('target')">
                <a-select-option v-for="s in schemas" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
              <a-select v-model:value="activeStep.targetTable" style="width: 70%" placeholder="选择表"
                        show-search @change="loadColumnsForTable('target')">
                <a-select-option v-for="t in targetTables" :key="t.table_name" :value="t.table_name">
                  {{ t.table_name }}
                </a-select-option>
              </a-select>
            </a-input-group>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 字段映射表 -->
      <a-divider orientation="left">字段映射</a-divider>

      <div style="margin-bottom: 8px">
        <a-space>
          <a-button size="small" @click="handleAutoMap" :loading="autoMapping">
            <ThunderboltOutlined /> 自动映射
          </a-button>
          <a-button size="small" @click="handleAddMapping">
            <PlusOutlined /> 添加行
          </a-button>
          <a-popconfirm title="确定清空所有映射？" @confirm="activeStep.fieldMappings = []">
            <a-button size="small" danger>清空</a-button>
          </a-popconfirm>
        </a-space>
      </div>

      <a-table
        :columns="mappingColumns"
        :data-source="activeStep.fieldMappings"
        :pagination="false"
        size="small"
        row-key="sortOrder"
        bordered
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'sourceColumn'">
            <a-select v-model:value="record.sourceColumn" style="width: 100%" size="small"
                      show-search placeholder="源字段">
              <a-select-option v-for="c in sourceColumns" :key="c.column_name" :value="c.column_name">
                {{ c.column_name }} <span style="color:#999">({{ c.data_type }})</span>
              </a-select-option>
            </a-select>
          </template>
          <template v-if="column.key === 'transformType'">
            <a-select v-model:value="record.transformType" style="width: 100%" size="small">
              <a-select-option value="DIRECT">直接</a-select-option>
              <a-select-option value="MAP">值映射</a-select-option>
              <a-select-option value="EXPRESSION">表达式</a-select-option>
              <a-select-option value="CONSTANT">常量</a-select-option>
              <a-select-option value="DATE_FMT">日期格式</a-select-option>
              <a-select-option value="LOOKUP">关联查询</a-select-option>
            </a-select>
          </template>
          <template v-if="column.key === 'transformExpr'">
            <template v-if="record.transformType === 'MAP'">
              <a-button size="small" @click="openMapEditor(record)">编辑映射</a-button>
            </template>
            <template v-else-if="record.transformType === 'CONSTANT'">
              <a-input v-model:value="record.defaultValue" size="small" placeholder="常量值" />
            </template>
            <template v-else>
              <a-input v-model:value="record.transformExpr" size="small" placeholder="表达式" />
            </template>
          </template>
          <template v-if="column.key === 'targetColumn'">
            <a-select v-model:value="record.targetColumn" style="width: 100%" size="small"
                      show-search placeholder="目标字段">
              <a-select-option v-for="c in targetColumns" :key="c.column_name" :value="c.column_name">
                {{ c.column_name }} <span style="color:#999">({{ c.data_type }})</span>
              </a-select-option>
            </a-select>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="activeStep.fieldMappings.splice(index, 1)">
              <DeleteOutlined />
            </a-button>
          </template>
        </template>
      </a-table>

      <!-- 高级配置 -->
      <a-divider orientation="left">
        <a-typography-link @click="showAdvanced = !showAdvanced">
          高级配置 {{ showAdvanced ? '▲' : '▼' }}
        </a-typography-link>
      </a-divider>

      <template v-if="showAdvanced">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="过滤条件">
              <a-textarea v-model:value="activeStep.filterCondition" :rows="2" placeholder="WHERE 条件" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="同步模式">
              <a-radio-group v-model:value="activeStep.syncMode">
                <a-radio value="FULL">全量</a-radio>
                <a-radio value="INCREMENTAL">增量</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="前置 SQL">
              <a-textarea v-model:value="activeStep.preSql" :rows="2" placeholder="步骤前执行" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="后置 SQL">
              <a-textarea v-model:value="activeStep.postSql" :rows="2" placeholder="步骤后执行" />
            </a-form-item>
          </a-col>
        </a-row>
      </template>
    </a-card>

    <!-- 值映射编辑弹窗 -->
    <a-modal v-model:open="mapEditorVisible" title="值映射配置" @ok="mapEditorVisible = false" width="500">
      <div v-for="(item, idx) in currentMapItems" :key="idx" style="display: flex; gap: 8px; margin-bottom: 8px">
        <a-input v-model:value="item.key" placeholder="源值" style="width: 40%" />
        <span style="line-height: 32px">→</span>
        <a-input v-model:value="item.value" placeholder="目标值" style="width: 40%" />
        <a-button type="text" danger @click="currentMapItems.splice(idx, 1)">
          <DeleteOutlined />
        </a-button>
      </div>
      <a-button type="dashed" block @click="currentMapItems.push({ key: '', value: '' })">
        <PlusOutlined /> 添加映射
      </a-button>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, DeleteOutlined, CheckCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getEtlPipeline, createEtlPipeline, updateEtlPipeline,
  validateEtlPipeline,
  getEtlSteps, createEtlStep, updateEtlStep, deleteEtlStep,
  batchUpdateFieldMappings, autoMapFields,
} from '@/api/etl'
import { getEtlSchemas, getEtlTables, getEtlColumns } from '@/api/etl'
import { getDataSources } from '@/api/data'

defineOptions({ name: 'EtlPipelineConfig' })

const router = useRouter()
const route = useRoute()
const pipelineId = computed(() => route.params.id ? Number(route.params.id) : null)
const isEdit = computed(() => pipelineId.value !== null)
const saving = ref(false)

// ===== 管道基础信息 =====
const pipeline = reactive({
  pipelineName: '',
  sourceId: undefined as number | undefined,
  engineType: 'EMBULK',
  syncMode: 'MANUAL',
  cronExpression: '',
  description: '',
})

// ===== 数据源列表 =====
const dataSources = ref<any[]>([])
async function loadDataSources() {
  try {
    const res = await getDataSources({ page: 1, page_size: 200 })
    dataSources.value = res.data?.data?.items || []
  } catch { /* ignore */ }
}

// ===== 步骤列表 =====
const steps = ref<any[]>([
  // 本地维护的步骤数据
])
const activeStepIndex = ref(0)
const activeStep = computed(() => steps.value[activeStepIndex.value] || null)

function handleAddStep() {
  steps.value.push({
    stepName: `步骤 ${steps.value.length + 1}`,
    stepOrder: steps.value.length + 1,
    stepType: 'ONE_TO_ONE',
    sourceSchema: 'ods',
    sourceTable: '',
    targetSchema: 'cdr',
    targetTable: '',
    onError: 'ABORT',
    syncMode: 'INCREMENTAL',
    filterCondition: '',
    preSql: '',
    postSql: '',
    fieldMappings: [] as any[],
  })
  activeStepIndex.value = steps.value.length - 1
}

function handleRemoveStep(idx: number) {
  steps.value.splice(idx, 1)
  if (activeStepIndex.value >= steps.value.length) {
    activeStepIndex.value = Math.max(0, steps.value.length - 1)
  }
}

// ===== 元数据 =====
const schemas = ref<string[]>([])
const sourceTables = ref<any[]>([])
const targetTables = ref<any[]>([])
const sourceColumns = ref<any[]>([])
const targetColumns = ref([])
const showAdvanced = ref(false)

async function loadSchemas() {
  try {
    const res = await getEtlSchemas()
    schemas.value = res.data?.data || []
  } catch { /* ignore */ }
}

async function loadTablesForSchema(type: 'source' | 'target') {
  const schema = type === 'source' ? activeStep.value?.sourceSchema : activeStep.value?.targetSchema
  if (!schema) return
  try {
    const res = await getEtlTables(schema)
    if (type === 'source') sourceTables.value = res.data?.data || []
    else targetTables.value = res.data?.data || []
  } catch { /* ignore */ }
}

async function loadColumnsForTable(type: 'source' | 'target') {
  const schema = type === 'source' ? activeStep.value?.sourceSchema : activeStep.value?.targetSchema
  const table = type === 'source' ? activeStep.value?.sourceTable : activeStep.value?.targetTable
  if (!schema || !table) return
  try {
    const res = await getEtlColumns(schema, table)
    if (type === 'source') sourceColumns.value = res.data?.data || []
    else targetColumns.value = res.data?.data || []
  } catch { /* ignore */ }
}

// 当切换步骤时重新加载列信息
watch(activeStepIndex, () => {
  if (activeStep.value) {
    if (activeStep.value.sourceSchema && activeStep.value.sourceTable) {
      loadColumnsForTable('source')
    }
    if (activeStep.value.targetSchema && activeStep.value.targetTable) {
      loadColumnsForTable('target')
    }
  }
})

// ===== 字段映射 =====
const mappingColumns = [
  { title: '源字段', key: 'sourceColumn', width: 180 },
  { title: '转换', key: 'transformType', width: 120 },
  { title: '转换规则', key: 'transformExpr', width: 180 },
  { title: '目标字段', key: 'targetColumn', width: 180 },
  { title: '', key: 'action', width: 50 },
]

const autoMapping = ref(false)

function handleAddMapping() {
  if (!activeStep.value) return
  activeStep.value.fieldMappings.push({
    sourceColumn: '',
    targetColumn: '',
    transformType: 'DIRECT',
    transformExpr: '',
    defaultValue: '',
    isRequired: false,
    sortOrder: activeStep.value.fieldMappings.length,
  })
}

async function handleAutoMap() {
  if (!activeStep.value?.id) {
    message.warning('请先保存步骤后再使用自动映射')
    return
  }
  autoMapping.value = true
  try {
    const res = await autoMapFields(activeStep.value.id)
    activeStep.value.fieldMappings = res.data?.data || []
    message.success('自动映射完成')
  } finally {
    autoMapping.value = false
  }
}

// ===== 值映射编辑 =====
const mapEditorVisible = ref(false)
const currentMapItems = ref<{ key: string; value: string }[]>([])
let currentMapRecord: any = null

function openMapEditor(record: any) {
  currentMapRecord = record
  try {
    const parsed = record.transformExpr ? JSON.parse(record.transformExpr) : {}
    currentMapItems.value = Object.entries(parsed).map(([key, value]) => ({
      key, value: String(value),
    }))
  } catch {
    currentMapItems.value = []
  }
  mapEditorVisible.value = true
}

// 关闭时保存回 record
watch(mapEditorVisible, (v) => {
  if (!v && currentMapRecord) {
    const mapObj: Record<string, string> = {}
    currentMapItems.value.forEach(item => {
      if (item.key) mapObj[item.key] = item.value
    })
    currentMapRecord.transformExpr = JSON.stringify(mapObj)
  }
})

// ===== 保存 =====
async function handleSave() {
  if (!pipeline.pipelineName) {
    message.error('请输入管道名称')
    return
  }
  saving.value = true
  try {
    let pid = pipelineId.value
    if (!pid) {
      // 新建管道
      const res = await createEtlPipeline({ ...pipeline })
      pid = res.data?.data?.id
    } else {
      await updateEtlPipeline(pid, { ...pipeline })
    }

    // 保存步骤和映射
    for (const step of steps.value) {
      if (step.id) {
        await updateEtlStep(pid!, step.id, step)
      } else {
        const res = await createEtlStep(pid!, step)
        step.id = res.data?.data?.id
      }
      // 保存字段映射
      if (step.id && step.fieldMappings?.length > 0) {
        await batchUpdateFieldMappings(step.id, step.fieldMappings)
      }
    }

    message.success('保存成功')
    if (!pipelineId.value) {
      router.replace({ name: 'EtlPipelineConfig', params: { id: pid } })
    }
  } finally {
    saving.value = false
  }
}

async function handleValidate() {
  if (!pipelineId.value) {
    message.warning('请先保存管道')
    return
  }
  try {
    const res = await validateEtlPipeline(pipelineId.value)
    const errors = res.data?.data || []
    if (errors.length === 0) {
      message.success('校验通过')
    } else {
      message.warning(`发现问题: ${errors.join('; ')}`)
    }
  } catch { /* handled */ }
}

function handleBack() {
  router.push({ name: 'EtlPipelineList' })
}

// ===== 加载已有数据 =====
async function loadPipelineData() {
  if (!pipelineId.value) return
  try {
    const res = await getEtlPipeline(pipelineId.value)
    const data = res.data?.data
    if (data) {
      Object.assign(pipeline, {
        pipelineName: data.pipelineName,
        sourceId: data.sourceId,
        engineType: data.engineType,
        syncMode: data.syncMode,
        cronExpression: data.cronExpression,
        description: data.description,
      })
      steps.value = (data.steps || []).map((s: any) => ({
        ...s,
        fieldMappings: s.fieldMappings || [],
      }))
    }
  } catch { /* handled */ }
}

onMounted(() => {
  loadDataSources()
  loadSchemas()
  loadPipelineData()
})
</script>
```

- [ ] **Step 2: 验证前端编译**

Run: `cd maidc-portal && npm run build 2>&1 | tail -5`

Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add maidc-portal/src/views/data-etl/EtlPipelineConfig.vue
git commit -m "feat(etl): implement EtlPipelineConfig page with step orchestration and field mapping"
```

---

## Phase 6 完成标准

- [x] 管道配置页完整实现
- [x] 头部区域：管道名称、数据源、同步模式
- [x] 步骤编排区：横向卡片流，支持添加/删除/选择
- [x] 步骤配置面板：源表/目标表下拉选择（从元数据 API 加载）
- [x] 字段映射表：源字段、转换规则、目标字段、操作
- [x] 转换规则：DIRECT/MAP/EXPRESSION/CONSTANT/DATE_FMT/LOOKUP
- [x] 值映射编辑弹窗（MAP 类型）
- [x] 自动映射按钮
- [x] 高级配置（可折叠）：过滤条件、前置 SQL、后置 SQL
- [x] 保存逻辑：管道 → 步骤 → 字段映射
- [x] 前端编译通过
