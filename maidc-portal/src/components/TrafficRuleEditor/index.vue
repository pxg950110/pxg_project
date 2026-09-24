<template>
  <div class="traffic-rule-editor">
    <el-table
      :data="rules"
      size="small"
      border
      row-key="versionId"
      class="!rounded-lg overflow-hidden"
    >
      <el-table-column prop="versionId" label="版本ID" width="180">
        <template #default="{ row }">
          <el-input v-model="row.versionId" placeholder="版本号/ID" size="small" @input="emitValue" />
        </template>
      </el-table-column>
      <el-table-column prop="weight" label="流量权重">
        <template #default="{ row }">
          <div class="weight-cell flex items-center gap-3">
            <el-slider
              v-model="row.weight"
              :min="0"
              :max="100"
              :step="1"
              class="flex-1"
              @input="handleWeightChange"
            />
            <el-input-number
              v-model="row.weight"
              :min="0"
              :max="100"
              size="small"
              class="!w-24"
              @change="handleWeightChange"
            />
            <span class="weight-percent text-xs text-slate-400 font-mono">%</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ $index }">
          <el-button
            type="danger"
            link
            size="small"
            :disabled="rules.length <= 1"
            @click="removeRule($index)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="traffic-footer flex items-center justify-between mt-3 pt-3 border-t border-slate-100">
      <div class="total-weight text-xs">
        权重合计：<span :class="totalWeightClass" class="font-bold font-mono text-sm">{{ totalWeight }}%</span>
        <span v-if="totalWeight !== 100" class="text-amber-500 text-xs ml-1">（建议合计为100%）</span>
      </div>
      <el-button size="small" class="!border-dashed" @click="addRule">
        <el-icon class="mr-1"><Plus /></el-icon>
        添加版本
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

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

const totalWeight = computed(() => rules.value.reduce((sum, r) => sum + (r.weight ?? 0), 0))

const totalWeightClass = computed(() => ({
  'text-emerald-600': totalWeight.value === 100,
  'text-amber-500': totalWeight.value !== 100,
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
</style>
