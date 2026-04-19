<template>
  <PageContainer :loading="pageLoading" :breadcrumb="breadcrumb">
    <template #title>
      <span>{{ isEdit ? '编辑管道' : '新建管道' }}</span>
    </template>
    <template #extra>
      <a-space>
        <a-button v-if="isEdit" @click="handleValidate">
          <template #icon><CheckCircleOutlined /></template>
          校验
        </a-button>
        <a-button type="primary" :loading="saveLoading" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
      </a-space>
    </template>

    <!-- Section 1: Basic Info -->
    <a-card title="基本信息" :bordered="false" style="margin-bottom: 16px">
      <a-form layout="inline" :model="pipelineForm">
        <a-form-item label="管道名称">
          <a-input
            v-model:value="pipelineForm.name"
            placeholder="请输入管道名称"
            style="width: 240px"
          />
        </a-form-item>
        <a-form-item label="数据源">
          <a-select
            v-model:value="pipelineForm.sourceId"
            placeholder="请选择数据源"
            :loading="dataSourceLoading"
            :disabled="isEdit"
            style="width: 200px"
          >
            <a-select-option v-for="ds in dataSourceOptions" :key="ds.id" :value="ds.id">
              {{ ds.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="同步模式">
          <a-radio-group v-model:value="pipelineForm.syncMode">
            <a-radio value="MANUAL">手动</a-radio>
            <a-radio value="INCREMENTAL">增量</a-radio>
            <a-radio value="FULL">全量</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Section 2: Step Orchestration -->
    <a-card title="步骤编排" :bordered="false" style="margin-bottom: 16px">
      <div class="step-flow">
        <div
          v-for="(step, idx) in steps"
          :key="idx"
          class="step-card"
          :class="{ active: activeStepIndex === idx }"
          @click="selectStep(idx)"
        >
          <div class="step-card-header">
            <span class="step-card-index">{{ idx + 1 }}</span>
            <span class="step-card-name">{{ step.stepName || '未命名步骤' }}</span>
            <a-popconfirm
              title="确定删除此步骤？"
              @confirm="removeStep(idx)"
              placement="topRight"
            >
              <a-button
                type="text"
                size="small"
                danger
                class="step-card-delete"
                @click.stop
              >
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </a-popconfirm>
          </div>
          <div class="step-card-body">
            <span class="step-table-label">
              {{ step.sourceTable || '?' }} &rarr; {{ step.targetTable || '?' }}
            </span>
            <div style="margin-top: 6px">
              <a-tag
                :color="stepTypeColorMap[step.stepType] || 'default'"
                size="small"
              >
                {{ stepTypeMap[step.stepType] || step.stepType || '未设置' }}
              </a-tag>
            </div>
          </div>
        </div>

        <div class="step-card step-card-add" @click="addStep">
          <PlusOutlined style="font-size: 24px; color: #1890ff" />
          <div style="margin-top: 8px; color: rgba(0,0,0,0.45)">添加步骤</div>
        </div>
      </div>
    </a-card>

    <!-- Section 3: Step Config Panel -->
    <a-card
      v-if="activeStep"
      :title="`步骤配置 - ${activeStep.stepName || '未命名步骤'}`"
      :bordered="false"
    >
      <!-- Row 1: step basic -->
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="步骤名称" style="width: 100%">
            <a-input v-model:value="activeStep.stepName" placeholder="请输入步骤名称" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="步骤类型" style="width: 100%">
            <a-select v-model:value="activeStep.stepType" placeholder="请选择">
              <a-select-option value="ONE_TO_ONE">一对一 (1:1)</a-select-option>
              <a-select-option value="ONE_TO_MANY">一对多 (1:N)</a-select-option>
              <a-select-option value="MANY_TO_ONE">多对一 (N:1)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="错误处理" style="width: 100%">
            <a-select v-model:value="activeStep.onError" placeholder="请选择">
              <a-select-option value="ABORT">中止</a-select-option>
              <a-select-option value="SKIP">跳过</a-select-option>
              <a-select-option value="RETRY">重试</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- Row 2: source/target table selects -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-card size="small" title="源表" style="margin-bottom: 16px">
            <a-space>
              <a-select
                v-model:value="activeStep.sourceSchema"
                placeholder="选择Schema"
                style="width: 160px"
                :loading="schemaLoading"
                @change="onSourceSchemaChange"
              >
                <a-select-option v-for="s in schemaOptions" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
              <a-select
                v-model:value="activeStep.sourceTable"
                placeholder="选择表"
                style="width: 200px"
                :loading="sourceTableLoading"
                :disabled="!activeStep.sourceSchema"
                show-search
                :filter-option="filterOption"
                @change="onSourceTableChange"
              >
                <a-select-option
                  v-for="t in sourceTableOptions"
                  :key="t.tableName || t"
                  :value="t.tableName || t"
                >
                  {{ t.tableName || t }}
                </a-select-option>
              </a-select>
            </a-space>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card size="small" title="目标表" style="margin-bottom: 16px">
            <a-space>
              <a-select
                v-model:value="activeStep.targetSchema"
                placeholder="选择Schema"
                style="width: 160px"
                @change="onTargetSchemaChange"
              >
                <a-select-option v-for="s in schemaOptions" :key="s" :value="s">{{ s }}</a-select-option>
              </a-select>
              <a-select
                v-model:value="activeStep.targetTable"
                placeholder="选择表"
                style="width: 200px"
                :loading="targetTableLoading"
                :disabled="!activeStep.targetSchema"
                show-search
                :filter-option="filterOption"
                @change="onTargetTableChange"
              >
                <a-select-option
                  v-for="t in targetTableOptions"
                  :key="t.tableName || t"
                  :value="t.tableName || t"
                >
                  {{ t.tableName || t }}
                </a-select-option>
              </a-select>
            </a-space>
          </a-card>
        </a-col>
      </a-row>

      <!-- Field Mapping Table -->
      <a-card
        size="small"
        title="字段映射"
        style="margin-bottom: 16px"
      >
        <template #extra>
          <a-space>
            <a-button size="small" @click="handleAutoMap" :loading="autoMapLoading">
              <template #icon><ThunderboltOutlined /></template>
              自动映射
            </a-button>
            <a-button size="small" @click="addMappingRow">
              <template #icon><PlusOutlined /></template>
              添加行
            </a-button>
            <a-popconfirm title="确定清空所有映射？" @confirm="clearMappings">
              <a-button size="small" danger>清空</a-button>
            </a-popconfirm>
          </a-space>
        </template>

        <a-table
          :columns="mappingColumns"
          :data-source="activeStep.fieldMappings"
          :pagination="false"
          size="small"
          row-key="_rowKey"
          :scroll="{ y: 360 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'sourceColumn'">
              <a-select
                v-model:value="record.sourceColumn"
                placeholder="选择源字段"
                style="width: 100%"
                size="small"
                show-search
                :filter-option="filterOption"
              >
                <a-select-option
                  v-for="c in sourceColumnOptions"
                  :key="c.columnName || c"
                  :value="c.columnName || c"
                >
                  {{ c.columnName || c }}
                  <span v-if="c.dataType" style="color: rgba(0,0,0,0.35); margin-left: 4px; font-size: 12px">
                    {{ c.dataType }}
                  </span>
                </a-select-option>
              </a-select>
            </template>
            <template v-if="column.key === 'transformType'">
              <a-select
                v-model:value="record.transformType"
                placeholder="转换类型"
                style="width: 100%"
                size="small"
              >
                <a-select-option value="DIRECT">直接映射</a-select-option>
                <a-select-option value="MAP">值映射</a-select-option>
                <a-select-option value="EXPRESSION">表达式</a-select-option>
                <a-select-option value="CONSTANT">常量</a-select-option>
                <a-select-option value="DATE_FMT">日期格式</a-select-option>
                <a-select-option value="LOOKUP">查找</a-select-option>
              </a-select>
            </template>
            <template v-if="column.key === 'transformRule'">
              <template v-if="record.transformType === 'MAP'">
                <a-button size="small" @click="openValueMapModal(index)" style="width: 100%">
                  {{ record.transformRule ? '编辑映射' : '配置映射' }}
                </a-button>
              </template>
              <a-input
                v-else
                v-model:value="record.transformRule"
                :placeholder="transformRulePlaceholder(record.transformType)"
                size="small"
              />
            </template>
            <template v-if="column.key === 'targetColumn'">
              <a-select
                v-model:value="record.targetColumn"
                placeholder="选择目标字段"
                style="width: 100%"
                size="small"
                show-search
                :filter-option="filterOption"
              >
                <a-select-option
                  v-for="c in targetColumnOptions"
                  :key="c.columnName || c"
                  :value="c.columnName || c"
                >
                  {{ c.columnName || c }}
                  <span v-if="c.dataType" style="color: rgba(0,0,0,0.35); margin-left: 4px; font-size: 12px">
                    {{ c.dataType }}
                  </span>
                </a-select-option>
              </a-select>
            </template>
            <template v-if="column.key === 'action'">
              <a-button
                type="text"
                size="small"
                danger
                @click="removeMappingRow(index)"
              >
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- Advanced Config (collapsible) -->
      <a-collapse :bordered="false" style="margin-bottom: 16px">
        <a-collapse-panel key="advanced" header="高级配置">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="过滤条件" style="width: 100%">
                <a-input
                  v-model:value="activeStep.filterCondition"
                  placeholder="如: create_time > '2024-01-01'"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="同步模式" style="width: 100%">
                <a-select v-model:value="activeStep.syncMode" placeholder="默认跟随管道" allow-clear>
                  <a-select-option value="">跟随管道</a-select-option>
                  <a-select-option value="MANUAL">手动</a-select-option>
                  <a-select-option value="INCREMENTAL">增量</a-select-option>
                  <a-select-option value="FULL">全量</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="前置SQL" style="width: 100%">
                <a-textarea
                  v-model:value="activeStep.preSql"
                  placeholder="步骤执行前运行的SQL（可选）"
                  :rows="3"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="后置SQL" style="width: 100%">
                <a-textarea
                  v-model:value="activeStep.postSql"
                  placeholder="步骤执行后运行的SQL（可选）"
                  :rows="3"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>
    </a-card>

    <a-empty v-else description="请在上方添加或选择步骤进行配置" />

    <!-- Value Map Editor Modal -->
    <a-modal
      v-model:open="valueMapModalVisible"
      title="值映射配置"
      :width="560"
      @ok="saveValueMap"
      @cancel="valueMapModalVisible = false"
    >
      <div style="margin-bottom: 12px">
        <a-button type="dashed" size="small" @click="addValueMapEntry" style="width: 100%">
          <template #icon><PlusOutlined /></template>
          添加映射项
        </a-button>
      </div>
      <div
        v-for="(entry, eIdx) in valueMapEntries"
        :key="eIdx"
        style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px"
      >
        <a-input
          v-model:value="entry.key"
          placeholder="源值"
          style="flex: 1"
          size="small"
        />
        <span style="color: rgba(0,0,0,0.35)">&rarr;</span>
        <a-input
          v-model:value="entry.value"
          placeholder="目标值"
          style="flex: 1"
          size="small"
        />
        <a-button type="text" size="small" danger @click="removeValueMapEntry(eIdx)">
          <template #icon><DeleteOutlined /></template>
        </a-button>
      </div>
      <a-empty v-if="valueMapEntries.length === 0" description="暂无映射项" :image="null" />
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  SaveOutlined,
  CheckCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import {
  getEtlPipeline,
  createEtlPipeline,
  updateEtlPipeline,
  validateEtlPipeline,
  getEtlSteps,
  createEtlStep,
  updateEtlStep,
  deleteEtlStep,
  getFieldMappings,
  batchUpdateFieldMappings,
  autoMapFields,
  getEtlSchemas,
  getEtlTables,
  getEtlColumns,
} from '@/api/etl'
import { getDataSources } from '@/api/data'

defineOptions({ name: 'EtlPipelineConfig' })

const route = useRoute()
const router = useRouter()

// ===== Constants =====
const stepTypeMap: Record<string, string> = {
  ONE_TO_ONE: '1:1',
  ONE_TO_MANY: '1:N',
  MANY_TO_ONE: 'N:1',
}
const stepTypeColorMap: Record<string, string> = {
  ONE_TO_ONE: 'blue',
  ONE_TO_MANY: 'green',
  MANY_TO_ONE: 'orange',
}

let rowKeyCounter = 0
function newRowKey() {
  return `_row_${++rowKeyCounter}_${Date.now()}`
}

interface FieldMapping {
  _rowKey: string
  id?: number
  sourceColumn: string
  targetColumn: string
  transformType: string
  transformRule: string
}

interface Step {
  id?: number
  stepName: string
  stepType: string
  onError: string
  sourceSchema: string
  sourceTable: string
  targetSchema: string
  targetTable: string
  filterCondition: string
  syncMode: string
  preSql: string
  postSql: string
  fieldMappings: FieldMapping[]
}

function createEmptyStep(): Step {
  return {
    stepName: '',
    stepType: 'ONE_TO_ONE',
    onError: 'ABORT',
    sourceSchema: '',
    sourceTable: '',
    targetSchema: '',
    targetTable: '',
    filterCondition: '',
    syncMode: '',
    preSql: '',
    postSql: '',
    fieldMappings: [],
  }
}

// ===== State =====
const pageLoading = ref(false)
const saveLoading = ref(false)
const autoMapLoading = ref(false)

const pipelineId = computed(() => {
  const id = route.params.id
  return id && id !== 'new' ? Number(id) : null
})
const isEdit = computed(() => pipelineId.value !== null)

const breadcrumb = computed(() => [
  { title: 'ETL管道管理', path: '/data-etl/pipelines' },
  { title: isEdit.value ? '编辑管道' : '新建管道' },
])

// Pipeline basic info
const pipelineForm = reactive({
  name: '',
  sourceId: undefined as number | undefined,
  syncMode: 'MANUAL' as string,
  engineType: 'EMBULK' as string,
  description: '',
})

// Steps
const steps = reactive<Step[]>([])
const activeStepIndex = ref(-1)
const activeStep = computed(() =>
  activeStepIndex.value >= 0 && activeStepIndex.value < steps.length
    ? steps[activeStepIndex.value]
    : null,
)

// Data source options
const dataSourceOptions = ref<any[]>([])
const dataSourceLoading = ref(false)

// Schema/table/column options
const schemaOptions = ref<string[]>([])
const schemaLoading = ref(false)
const sourceTableOptions = ref<any[]>([])
const sourceTableLoading = ref(false)
const targetTableOptions = ref<any[]>([])
const targetTableLoading = ref(false)
const sourceColumnOptions = ref<any[]>([])
const targetColumnOptions = ref<any[]>([])

// Value map modal
const valueMapModalVisible = ref(false)
const valueMapEditIndex = ref(-1)
const valueMapEntries = reactive<{ key: string; value: string }[]>([])

// Field mapping table columns
const mappingColumns = [
  { title: '源字段', key: 'sourceColumn', width: 200 },
  { title: '转换', key: 'transformType', width: 130 },
  { title: '转换规则', key: 'transformRule', width: 200 },
  { title: '目标字段', key: 'targetColumn', width: 200 },
  { title: '操作', key: 'action', width: 60, align: 'center' as const },
]

// ===== Data Loading =====
async function loadDataSources() {
  dataSourceLoading.value = true
  try {
    const res = await getDataSources({ page: 1, page_size: 200 })
    dataSourceOptions.value = res.data?.data?.items || []
  } catch {
    // handled by interceptor
  } finally {
    dataSourceLoading.value = false
  }
}

async function loadSchemas() {
  schemaLoading.value = true
  try {
    const res = await getEtlSchemas()
    schemaOptions.value = res.data?.data || []
  } catch {
    // handled by interceptor
  } finally {
    schemaLoading.value = false
  }
}

async function loadTables(schema: string, target: 'source' | 'target') {
  if (!schema) return
  if (target === 'source') {
    sourceTableLoading.value = true
    sourceTableOptions.value = []
  } else {
    targetTableLoading.value = true
    targetTableOptions.value = []
  }
  try {
    const res = await getEtlTables(schema)
    const tables = res.data?.data || []
    if (target === 'source') {
      sourceTableOptions.value = tables
    } else {
      targetTableOptions.value = tables
    }
  } catch {
    // handled by interceptor
  } finally {
    sourceTableLoading.value = false
    targetTableLoading.value = false
  }
}

async function loadColumns(schema: string, table: string, target: 'source' | 'target') {
  if (!schema || !table) return
  try {
    const res = await getEtlColumns(schema, table)
    const cols = res.data?.data || []
    if (target === 'source') {
      sourceColumnOptions.value = cols
    } else {
      targetColumnOptions.value = cols
    }
  } catch {
    // handled by interceptor
  }
}

async function loadPipelineData() {
  if (!pipelineId.value) return
  pageLoading.value = true
  try {
    const res = await getEtlPipeline(pipelineId.value)
    const data = res.data?.data
    if (!data) return

    Object.assign(pipelineForm, {
      name: data.name || '',
      sourceId: data.sourceId,
      syncMode: data.syncMode || 'MANUAL',
      engineType: data.engineType || 'EMBULK',
      description: data.description || '',
    })

    // Load steps
    const stepsRes = await getEtlSteps(pipelineId.value)
    const stepsData = stepsRes.data?.data || []
    steps.length = 0
    for (const s of stepsData) {
      const step: Step = {
        id: s.id,
        stepName: s.stepName || '',
        stepType: s.stepType || 'ONE_TO_ONE',
        onError: s.onError || 'ABORT',
        sourceSchema: s.sourceSchema || '',
        sourceTable: s.sourceTable || '',
        targetSchema: s.targetSchema || '',
        targetTable: s.targetTable || '',
        filterCondition: s.filterCondition || '',
        syncMode: s.syncMode || '',
        preSql: s.preSql || '',
        postSql: s.postSql || '',
        fieldMappings: [],
      }

      // Load field mappings for this step
      if (s.id) {
        try {
          const fmRes = await getFieldMappings(s.id)
          const fmData = fmRes.data?.data || []
          step.fieldMappings = fmData.map((fm: any) => ({
            _rowKey: newRowKey(),
            id: fm.id,
            sourceColumn: fm.sourceColumn || '',
            targetColumn: fm.targetColumn || '',
            transformType: fm.transformType || 'DIRECT',
            transformRule: fm.transformRule || '',
          }))
        } catch {
          // skip failed mapping load
        }
      }

      steps.push(step)
    }

    // Auto select first step if exists
    if (steps.length > 0) {
      selectStep(0)
    }
  } catch {
    // handled by interceptor
  } finally {
    pageLoading.value = false
  }
}

// ===== Step Operations =====
function addStep() {
  steps.push(createEmptyStep())
  selectStep(steps.length - 1)
}

function removeStep(idx: number) {
  const step = steps[idx]
  if (step.id && pipelineId.value) {
    deleteEtlStep(pipelineId.value, step.id).then(() => {
      message.success('步骤已删除')
    }).catch(() => {
      // handled by interceptor
    })
  }
  steps.splice(idx, 1)
  if (activeStepIndex.value >= steps.length) {
    activeStepIndex.value = steps.length - 1
  }
  if (activeStepIndex.value < 0) {
    clearColumnOptions()
  }
}

function selectStep(idx: number) {
  activeStepIndex.value = idx
  const step = steps[idx]
  if (!step) return

  // Clear old options
  sourceColumnOptions.value = []
  targetColumnOptions.value = []

  // Load tables for selected schemas
  if (step.sourceSchema) {
    loadTables(step.sourceSchema, 'source')
  }
  if (step.targetSchema) {
    loadTables(step.targetSchema, 'target')
  }

  // Load columns if both schema and table are set
  if (step.sourceSchema && step.sourceTable) {
    loadColumns(step.sourceSchema, step.sourceTable, 'source')
  }
  if (step.targetSchema && step.targetTable) {
    loadColumns(step.targetSchema, step.targetTable, 'target')
  }
}

function clearColumnOptions() {
  sourceColumnOptions.value = []
  targetColumnOptions.value = []
  sourceTableOptions.value = []
  targetTableOptions.value = []
}

// ===== Schema/Table Change Handlers =====
function onSourceSchemaChange(schema: string) {
  if (activeStep.value) {
    activeStep.value.sourceTable = ''
    activeStep.value.fieldMappings = []
  }
  sourceColumnOptions.value = []
  sourceTableOptions.value = []
  if (schema) {
    loadTables(schema, 'source')
  }
}

function onTargetSchemaChange(schema: string) {
  if (activeStep.value) {
    activeStep.value.targetTable = ''
    activeStep.value.fieldMappings = []
  }
  targetColumnOptions.value = []
  targetTableOptions.value = []
  if (schema) {
    loadTables(schema, 'target')
  }
}

function onSourceTableChange(table: string) {
  if (activeStep.value) {
    activeStep.value.fieldMappings = []
  }
  sourceColumnOptions.value = []
  const schema = activeStep.value?.sourceSchema
  if (schema && table) {
    loadColumns(schema, table, 'source')
  }
}

function onTargetTableChange(table: string) {
  if (activeStep.value) {
    activeStep.value.fieldMappings = []
  }
  targetColumnOptions.value = []
  const schema = activeStep.value?.targetSchema
  if (schema && table) {
    loadColumns(schema, table, 'target')
  }
}

// ===== Field Mapping Operations =====
function addMappingRow() {
  if (!activeStep.value) return
  activeStep.value.fieldMappings.push({
    _rowKey: newRowKey(),
    sourceColumn: '',
    targetColumn: '',
    transformType: 'DIRECT',
    transformRule: '',
  })
}

function removeMappingRow(idx: number) {
  activeStep.value?.fieldMappings.splice(idx, 1)
}

function clearMappings() {
  if (activeStep.value) {
    activeStep.value.fieldMappings = []
  }
}

async function handleAutoMap() {
  if (!activeStep.value?.id) {
    message.warning('请先保存步骤后再使用自动映射')
    return
  }
  autoMapLoading.value = true
  try {
    const res = await autoMapFields(activeStep.value.id)
    const mapped = res.data?.data || []
    activeStep.value.fieldMappings = mapped.map((fm: any) => ({
      _rowKey: newRowKey(),
      id: fm.id,
      sourceColumn: fm.sourceColumn || '',
      targetColumn: fm.targetColumn || '',
      transformType: fm.transformType || 'DIRECT',
      transformRule: fm.transformRule || '',
    }))
    message.success(`自动映射完成，共 ${activeStep.value.fieldMappings.length} 条`)
  } catch {
    // handled by interceptor
  } finally {
    autoMapLoading.value = false
  }
}

function transformRulePlaceholder(type: string): string {
  switch (type) {
    case 'EXPRESSION': return '如: source_val * 100'
    case 'CONSTANT': return '如: 2024'
    case 'DATE_FMT': return '如: yyyy-MM-dd → yyyy/MM/dd'
    case 'LOOKUP': return '查找表名.字段'
    default: return '转换规则'
  }
}

// ===== Value Map Modal =====
function openValueMapModal(mappingIndex: number) {
  valueMapEditIndex.value = mappingIndex
  const rule = activeStep.value?.fieldMappings[mappingIndex]?.transformRule || ''
  valueMapEntries.length = 0
  if (rule) {
    try {
      const parsed = JSON.parse(rule)
      if (Array.isArray(parsed)) {
        parsed.forEach((e: any) => {
          valueMapEntries.push({ key: String(e.key ?? ''), value: String(e.value ?? '') })
        })
      } else if (typeof parsed === 'object') {
        Object.entries(parsed).forEach(([k, v]) => {
          valueMapEntries.push({ key: k, value: String(v) })
        })
      }
    } catch {
      // not JSON, ignore
    }
  }
  valueMapModalVisible.value = true
}

function addValueMapEntry() {
  valueMapEntries.push({ key: '', value: '' })
}

function removeValueMapEntry(idx: number) {
  valueMapEntries.splice(idx, 1)
}

function saveValueMap() {
  if (valueMapEditIndex.value < 0 || !activeStep.value) return
  const mapping = activeStep.value.fieldMappings[valueMapEditIndex.value]
  if (mapping) {
    // Store as JSON array of key-value pairs
    const obj: Record<string, string> = {}
    valueMapEntries.forEach((e) => {
      if (e.key) obj[e.key] = e.value
    })
    mapping.transformRule = Object.keys(obj).length > 0 ? JSON.stringify(obj) : ''
  }
  valueMapModalVisible.value = false
}

// ===== Save =====
async function handleSave() {
  // Validate basic info
  if (!pipelineForm.name) {
    message.warning('请输入管道名称')
    return
  }
  if (!pipelineForm.sourceId) {
    message.warning('请选择数据源')
    return
  }

  saveLoading.value = true
  try {
    // 1. Save or create pipeline
    const pipelineData: Record<string, any> = {
      name: pipelineForm.name,
      sourceId: pipelineForm.sourceId,
      syncMode: pipelineForm.syncMode,
      engineType: pipelineForm.engineType,
      description: pipelineForm.description,
    }

    let currentPipelineId = pipelineId.value
    if (currentPipelineId) {
      await updateEtlPipeline(currentPipelineId, pipelineData)
    } else {
      const createRes = await createEtlPipeline(pipelineData)
      currentPipelineId = createRes.data?.data?.id
      if (currentPipelineId) {
        // Navigate to edit URL so subsequent saves are updates
        router.replace({ name: 'EtlPipelineConfig', params: { id: currentPipelineId } })
      }
    }

    if (!currentPipelineId) {
      message.error('管道保存失败：未获取到ID')
      return
    }

    // 2. Save each step
    for (let i = 0; i < steps.length; i++) {
      const s = steps[i]
      const stepData: Record<string, any> = {
        stepName: s.stepName || `步骤${i + 1}`,
        stepType: s.stepType,
        onError: s.onError,
        sourceSchema: s.sourceSchema,
        sourceTable: s.sourceTable,
        targetSchema: s.targetSchema,
        targetTable: s.targetTable,
        filterCondition: s.filterCondition,
        syncMode: s.syncMode,
        preSql: s.preSql,
        postSql: s.postSql,
        stepOrder: i + 1,
      }

      let stepId = s.id
      if (stepId) {
        await updateEtlStep(currentPipelineId, stepId, stepData)
      } else {
        const stepRes = await createEtlStep(currentPipelineId, stepData)
        stepId = stepRes.data?.data?.id
        s.id = stepId
      }

      // 3. Save field mappings for this step
      if (stepId && s.fieldMappings.length > 0) {
        const mappingsData = s.fieldMappings.map((fm) => ({
          id: fm.id,
          sourceColumn: fm.sourceColumn,
          targetColumn: fm.targetColumn,
          transformType: fm.transformType,
          transformRule: fm.transformRule,
        }))
        const batchRes = await batchUpdateFieldMappings(stepId, mappingsData)
        // Update local IDs from response
        const savedMappings = batchRes.data?.data || []
        savedMappings.forEach((saved: any, idx: number) => {
          if (s.fieldMappings[idx]) {
            s.fieldMappings[idx].id = saved.id
          }
        })
      }
    }

    message.success('保存成功')
  } catch {
    // handled by interceptor
  } finally {
    saveLoading.value = false
  }
}

// ===== Validate =====
async function handleValidate() {
  if (!pipelineId.value) {
    message.warning('请先保存管道后再校验')
    return
  }
  const hide = message.loading('正在校验管道配置...', 0)
  try {
    const res = await validateEtlPipeline(pipelineId.value)
    hide()
    const errors = res.data?.data
    if (errors && errors.length > 0) {
      message.warning(`校验发现 ${errors.length} 个问题`)
      // Show details in a simple way
      const details = errors.slice(0, 5).join('\n')
      import('ant-design-vue').then(({ Modal }) => {
        Modal.info({
          title: '校验结果',
          content: details + (errors.length > 5 ? `\n...共 ${errors.length} 条` : ''),
          width: 520,
        })
      })
    } else {
      message.success('校验通过')
    }
  } catch {
    hide()
  }
}

// ===== Helpers =====
function filterOption(input: string, option: any) {
  const text = option.children?.[0]?.children || option.value || ''
  return String(text).toLowerCase().includes(input.toLowerCase())
}

// ===== Init =====
onMounted(async () => {
  await Promise.all([loadDataSources(), loadSchemas()])
  if (isEdit.value) {
    await loadPipelineData()
  }
})
</script>

<style scoped>
.step-flow {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.step-card {
  flex-shrink: 0;
  width: 200px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.step-card:hover {
  border-color: #4096ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.step-card.active {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.step-card-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-style: dashed;
  min-height: 100px;
}

.step-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.step-card-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f0f0f0;
  font-size: 12px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  flex-shrink: 0;
}

.step-card.active .step-card-index {
  background: #1890ff;
  color: #fff;
}

.step-card-name {
  flex: 1;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.step-card-delete {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.step-card:hover .step-card-delete {
  opacity: 1;
}

.step-card-body {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.step-table-label {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-card-head-title) {
  font-size: 15px;
}
</style>
