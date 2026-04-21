<template>
  <div class="condition-builder">
    <div v-for="(group, gi) in groups" :key="gi" class="condition-group">
      <div class="group-header">
        <a-select v-model:value="group.domain" style="width: 140px" placeholder="选择域">
          <a-select-option v-for="d in domains" :key="d.value" :value="d.value">{{ d.label }}</a-select-option>
        </a-select>
        <a-radio-group v-model:value="group.logic" size="small" button-style="solid">
          <a-radio-button value="AND">AND</a-radio-button>
          <a-radio-button value="OR">OR</a-radio-button>
        </a-radio-group>
        <a-button type="text" danger size="small" @click="removeGroup(gi)" :disabled="groups.length <= 1">
          <template #icon><DeleteOutlined /></template>
        </a-button>
      </div>
      <div v-for="(cond, ci) in group.conditions" :key="ci" class="condition-row">
        <a-select v-model:value="cond.field" style="width: 160px" placeholder="字段" @change="() => emitUpdate()">
          <a-select-option v-for="f in getFields(group.domain)" :key="f.value" :value="f.value">{{ f.label }}</a-select-option>
        </a-select>
        <a-select v-model:value="cond.operator" style="width: 120px" placeholder="操作符" @change="() => emitUpdate()">
          <a-select-option value="LIKE">LIKE</a-select-option>
          <a-select-option value="IN">IN</a-select-option>
          <a-select-option value="=">=</a-select-option>
          <a-select-option value="CONTAINS">包含</a-select-option>
        </a-select>
        <a-input
          v-if="cond.operator !== 'IN'"
          v-model:value="cond.value"
          placeholder="值"
          style="flex: 1"
          @change="() => emitUpdate()"
        />
        <a-select
          v-else
          v-model:value="cond.valueArr"
          mode="tags"
          placeholder="输入后回车"
          style="flex: 1"
          @change="onInValueChange(cond, $event)"
        />
        <a-button type="text" danger size="small" @click="removeCondition(gi, ci)" :disabled="group.conditions.length <= 1">
          <template #icon><DeleteOutlined /></template>
        </a-button>
      </div>
      <a-button type="dashed" size="small" block @click="addCondition(gi)" style="margin-top: 8px">
        <template #icon><PlusOutlined /></template> 添加条件
      </a-button>
      <div v-if="gi < groups.length - 1" class="group-logic">
        <a-radio-group v-model:value="groupLogic" size="small" button-style="solid" @change="emitUpdate">
          <a-radio-button value="AND">组间 AND</a-radio-button>
          <a-radio-button value="OR">组间 OR</a-radio-button>
        </a-radio-group>
      </div>
    </div>
    <a-button type="dashed" block @click="addGroup" style="margin-top: 12px">
      <template #icon><PlusOutlined /></template> 添加条件组
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

const props = defineProps<{ modelValue?: any }>()
const emit = defineEmits<{ 'update:modelValue': [val: any] }>()

const domains = [
  { value: 'DIAGNOSIS', label: '诊断' },
  { value: 'LAB', label: '检验' },
  { value: 'MEDICATION', label: '用药' },
  { value: 'IMAGING', label: '影像' },
  { value: 'SURGERY', label: '手术' },
  { value: 'PATHOLOGY', label: '病理' },
]

const fieldMap: Record<string, { value: string; label: string }[]> = {
  DIAGNOSIS: [
    { value: 'diagnosis_code', label: 'ICD编码' },
    { value: 'diagnosis_name', label: '诊断名称' },
  ],
  LAB: [
    { value: 'test_code', label: '检验代码' },
    { value: 'test_name', label: '检验名称' },
  ],
  MEDICATION: [
    { value: 'med_name', label: '药品名称' },
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
    { value: 'diagnosis_desc', label: '病理诊断' },
  ],
}

const getFields = (domain: string) => fieldMap[domain] || []

interface Condition { field: string; operator: string; value: string; valueArr: string[] }
interface Group { domain: string; logic: string; conditions: Condition[] }

const groupLogic = ref('AND')
const groups = ref<Group[]>([
  { domain: 'DIAGNOSIS', logic: 'OR', conditions: [{ field: 'diagnosis_code', operator: 'LIKE', value: '', valueArr: [] }] },
])

watch(() => props.modelValue, (val) => {
  if (val && val.groups) {
    groupLogic.value = val.groupLogic || 'AND'
    groups.value = val.groups.map((g: any) => ({
      ...g,
      conditions: g.conditions.map((c: any) => ({
        ...c,
        value: Array.isArray(c.value) ? '' : (c.value || ''),
        valueArr: Array.isArray(c.value) ? c.value : [],
      })),
    }))
  }
}, { immediate: true })

function emitUpdate() {
  const rules = {
    groupLogic: groupLogic.value,
    groups: groups.value.map(g => ({
      domain: g.domain,
      logic: g.logic,
      conditions: g.conditions.map(c => ({
        field: c.field,
        operator: c.operator,
        value: c.operator === 'IN' ? c.valueArr : c.value,
      })),
    })),
  }
  emit('update:modelValue', rules)
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
  groups.value[gi].conditions.push({ field: 'diagnosis_code', operator: 'LIKE', value: '', valueArr: [] })
  emitUpdate()
}

function removeCondition(gi: number, ci: number) {
  groups.value[gi].conditions.splice(ci, 1)
  emitUpdate()
}

function onInValueChange(cond: Condition, val: string[]) {
  cond.valueArr = val
  emitUpdate()
}
</script>

<style scoped>
.condition-builder { width: 100%; }
.condition-group {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  background: #fafafa;
}
.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.group-logic {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}
</style>
