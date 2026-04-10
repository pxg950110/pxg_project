<template>
  <div class="traffic-rule-editor">
    <a-table
      :columns="columns"
      :data-source="rules"
      :pagination="false"
      size="small"
      bordered
      row-key="versionId"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'weight'">
          <div class="weight-cell">
            <a-slider
              v-model:value="record.weight"
              :min="0"
              :max="100"
              :step="1"
              style="flex: 1; margin: 0 8px 0 0"
              @change="handleWeightChange"
            />
            <a-input-number
              v-model:value="record.weight"
              :min="0"
              :max="100"
              style="width: 70px"
              @change="handleWeightChange"
            />
            <span class="weight-percent">%</span>
          </div>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-button
            type="text"
            danger
            size="small"
            :disabled="rules.length <= 1"
            @click="removeRule(index)"
          >
            <DeleteOutlined />
          </a-button>
        </template>
      </template>
    </a-table>
    <div class="traffic-footer">
      <div class="total-weight">
        权重合计：<span :class="totalWeightClass">{{ totalWeight }}%</span>
        <span v-if="totalWeight !== 100" class="weight-warning">（建议合计为100%）</span>
      </div>
      <a-button type="dashed" size="small" @click="addRule">
        <PlusOutlined />
        添加版本
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

interface TrafficRule {
  versionId: string
  weight: number
}

interface Props {
  modelValue: TrafficRule[]
}

interface Emits {
  (e: 'update:modelValue', value: TrafficRule[]): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const rules = ref<TrafficRule[]>(props.modelValue?.length ? props.modelValue.map((r) => ({ ...r })) : [])

watch(
  () => props.modelValue,
  (val) => {
    rules.value = val?.length ? val.map((r) => ({ ...r })) : []
  },
  { deep: true },
)

const columns = [
  { title: '版本ID', dataIndex: 'versionId', width: '35%' },
  { title: '流量权重', dataIndex: 'weight', width: '55%' },
  { title: '操作', dataIndex: 'action', width: '10%', align: 'center' as const },
]

const totalWeight = computed(() => rules.value.reduce((sum, r) => sum + (r.weight ?? 0), 0))

const totalWeightClass = computed(() => ({
  'weight-ok': totalWeight.value === 100,
  'weight-error': totalWeight.value !== 100,
}))

function handleWeightChange() {
  emitValue()
}

function removeRule(index: number) {
  rules.value.splice(index, 1)
  emitValue()
}

function addRule() {
  rules.value.push({ versionId: '', weight: 0 })
  emitValue()
}

function emitValue() {
  emit('update:modelValue', rules.value.map((r) => ({ ...r })))
}
</script>

<style scoped>
.traffic-rule-editor {
  width: 100%;
}
.weight-cell {
  display: flex;
  align-items: center;
}
.weight-percent {
  margin-left: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.traffic-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.total-weight {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
}
.weight-ok {
  color: #52c41a;
  font-weight: 500;
}
.weight-error {
  color: #faad14;
  font-weight: 500;
}
.weight-warning {
  color: rgba(0, 0, 0, 0.35);
  font-size: 12px;
  margin-left: 4px;
}
</style>
