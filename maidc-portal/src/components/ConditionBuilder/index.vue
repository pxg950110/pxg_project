<template>
  <div class="space-y-4 w-full">
    <div
      v-for="(group, gi) in groups"
      :key="gi"
      class="rounded-xl border border-slate-200/80 bg-slate-50/60 p-4 transition-all duration-200 hover:border-sky-200"
    >
      <!-- 组头部：域选择与逻辑控制 -->
      <div class="flex items-center justify-between gap-3 mb-3 pb-2.5 border-b border-slate-100">
        <div class="flex items-center gap-2.5">
          <span class="text-xs font-semibold text-slate-700 font-mono">条件组 {{ gi + 1 }}</span>
          <el-select
            v-model="group.domain"
            size="small"
            placeholder="选择临床数据域"
            class="!w-36"
            @change="onDomainChange(group)"
          >
            <el-option
              v-for="d in domains"
              :key="d.value"
              :label="d.label"
              :value="d.value"
            />
          </el-select>
        </div>

        <div class="flex items-center gap-2">
          <el-radio-group v-model="group.logic" size="small" @change="emitUpdate">
            <el-radio-button label="AND">组内 AND</el-radio-button>
            <el-radio-button label="OR">组内 OR</el-radio-button>
          </el-radio-group>
          <el-button
            link
            type="danger"
            size="small"
            :disabled="groups.length <= 1"
            class="!text-xs"
            @click="removeGroup(gi)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 条件列表 -->
      <div class="space-y-2.5">
        <div
          v-for="(cond, ci) in group.conditions"
          :key="ci"
          class="flex items-center gap-2"
        >
          <!-- 字段选择 -->
          <el-select
            v-model="cond.field"
            size="small"
            placeholder="选择字段"
            class="!w-44"
            @change="emitUpdate"
          >
            <el-option
              v-for="f in getFields(group.domain)"
              :key="f.value"
              :label="f.label"
              :value="f.value"
            />
          </el-select>

          <!-- 操作符选择 -->
          <el-select
            v-model="cond.operator"
            size="small"
            placeholder="操作符"
            class="!w-28"
            @change="emitUpdate"
          >
            <el-option label="LIKE (包含)" value="LIKE" />
            <el-option label="IN (在多项中)" value="IN" />
            <el-option label="= (等于)" value="=" />
            <el-option label="CONTAINS (包含)" value="CONTAINS" />
          </el-select>

          <!-- 单值输入 -->
          <el-input
            v-if="cond.operator !== 'IN'"
            v-model="cond.value"
            size="small"
            placeholder="请输入匹配值 (如 ICD 代码、药名等)"
            class="flex-1"
            clearable
            @input="emitUpdate"
          />

          <!-- 多值标签输入 -->
          <el-select
            v-else
            v-model="cond.valueArr"
            multiple
            filterable
            allow-create
            default-first-option
            size="small"
            placeholder="输入值后按回车添加多项"
            class="flex-1"
            @change="emitUpdate"
          />

          <!-- 移除单条条件 -->
          <el-button
            link
            type="danger"
            size="small"
            :disabled="group.conditions.length <= 1"
            class="!text-xs !p-1 text-slate-400 hover:text-rose-500"
            @click="removeCondition(gi, ci)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 添加单条条件按钮 -->
      <div class="mt-3 pt-2">
        <el-button
          type="primary"
          link
          size="small"
          class="!text-xs"
          @click="addCondition(gi)"
        >
          <el-icon class="mr-1"><Plus /></el-icon>
          添加准则条件
        </el-button>
      </div>

      <!-- 组间布尔逻辑指示器 -->
      <div v-if="gi < groups.length - 1" class="flex items-center justify-center my-4">
        <div class="flex items-center gap-2 px-3 py-1 rounded-full bg-sky-50 border border-sky-200">
          <span class="text-xs text-sky-700 font-medium">组间逻辑关系：</span>
          <el-radio-group v-model="groupLogic" size="small" @change="emitUpdate">
            <el-radio-button label="AND">AND (同时满足)</el-radio-button>
            <el-radio-button label="OR">OR (任一满足)</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </div>

    <!-- 添加新条件组 -->
    <el-button
      type="primary"
      plain
      size="small"
      class="w-full !rounded-xl !py-2 !border-dashed"
      @click="addGroup"
    >
      <el-icon class="mr-1.5"><FolderAdd /></el-icon>
      新增多模态条件组
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Plus, Delete, FolderAdd } from '@element-plus/icons-vue'

const props = defineProps<{ modelValue?: any }>()
const emit = defineEmits<{ 'update:modelValue': [val: any] }>()

const domains = [
  { value: 'DIAGNOSIS', label: '临床诊断' },
  { value: 'LAB', label: '检验化验' },
  { value: 'MEDICATION', label: '用药医嘱' },
  { value: 'IMAGING', label: '放射影像' },
  { value: 'SURGERY', label: '手术操作' },
  { value: 'PATHOLOGY', label: '病理检查' },
]

const fieldMap: Record<string, { value: string; label: string }[]> = {
  DIAGNOSIS: [
    { value: 'diagnosis_code', label: 'ICD诊断编码' },
    { value: 'diagnosis_name', label: '临床诊断名称' },
  ],
  LAB: [
    { value: 'test_code', label: '检验代码' },
    { value: 'test_name', label: '检验项目名称' },
  ],
  MEDICATION: [
    { value: 'med_name', label: '药品通用名' },
    { value: 'med_code', label: '药品编码' },
  ],
  IMAGING: [
    { value: 'exam_type', label: '检查类型' },
    { value: 'body_part', label: '检查部位' },
  ],
  SURGERY: [
    { value: 'operation_name', label: '手术名称' },
    { value: 'operation_code', label: '手术编码' },
  ],
  PATHOLOGY: [
    { value: 'diagnosis_desc', label: '病理诊断描述' },
  ],
}

const getFields = (domain: string) => fieldMap[domain] || []

interface Condition {
  field: string
  operator: string
  value: string
  valueArr: string[]
}

interface Group {
  domain: string
  logic: string
  conditions: Condition[]
}

const groupLogic = ref('AND')
const groups = ref<Group[]>([
  {
    domain: 'DIAGNOSIS',
    logic: 'OR',
    conditions: [{ field: 'diagnosis_code', operator: 'LIKE', value: '', valueArr: [] }],
  },
])

watch(
  () => props.modelValue,
  (val) => {
    if (val && val.groups && Array.isArray(val.groups)) {
      groupLogic.value = val.groupLogic || 'AND'
      groups.value = val.groups.map((g: any) => ({
        domain: g.domain || 'DIAGNOSIS',
        logic: g.logic || 'OR',
        conditions: (g.conditions || []).map((c: any) => ({
          field: c.field || (getFields(g.domain || 'DIAGNOSIS')[0]?.value ?? ''),
          operator: c.operator || 'LIKE',
          value: Array.isArray(c.value) ? '' : (c.value || ''),
          valueArr: Array.isArray(c.value) ? c.value : [],
        })),
      }))
    }
  },
  { immediate: true },
)

function emitUpdate() {
  const rules = {
    groupLogic: groupLogic.value,
    groups: groups.value.map((g) => ({
      domain: g.domain,
      logic: g.logic,
      conditions: g.conditions.map((c) => ({
        field: c.field,
        operator: c.operator,
        value: c.operator === 'IN' ? c.valueArr : c.value,
      })),
    })),
  }
  emit('update:modelValue', rules)
}

function onDomainChange(group: Group) {
  const fields = getFields(group.domain)
  if (fields.length > 0) {
    group.conditions.forEach((c) => {
      c.field = fields[0].value
    })
  }
  emitUpdate()
}

function addGroup() {
  groups.value.push({
    domain: 'DIAGNOSIS',
    logic: 'OR',
    conditions: [{ field: 'diagnosis_code', operator: 'LIKE', value: '', valueArr: [] }],
  })
  emitUpdate()
}

function removeGroup(index: number) {
  groups.value.splice(index, 1)
  emitUpdate()
}

function addCondition(gi: number) {
  const domain = groups.value[gi].domain
  const defaultField = getFields(domain)[0]?.value || ''
  groups.value[gi].conditions.push({ field: defaultField, operator: 'LIKE', value: '', valueArr: [] })
  emitUpdate()
}

function removeCondition(gi: number, ci: number) {
  groups.value[gi].conditions.splice(ci, 1)
  emitUpdate()
}
</script>
