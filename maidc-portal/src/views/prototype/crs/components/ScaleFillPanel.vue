<template>
  <div class="scale-fill-panel">
    <div class="scale-header">
      <div>
        <span class="scale-name">{{ scale.name }}</span>
        <a-tag class="ml8">{{ scale.scaleCode }} · v{{ scale.version }}</a-tag>
        <a-tag v-if="required" color="red">必评</a-tag>
        <a-tag v-else>选评</a-tag>
        <a-tag v-if="hasBinding" color="purple"><ApiOutlined /> 含业务数据带入</a-tag>
      </div>
      <div v-if="def.maxScore" class="score-box">
        <span class="score-label">当前得分</span>
        <span class="score-value" :class="scoreClass">{{ totalScore }}</span>
        <span class="score-max">/ {{ def.maxScore }}</span>
        <a-tag v-if="totalScore > 0" :color="interpretColor" class="ml8">{{ interpretation }}</a-tag>
      </div>
      <a-tag v-else class="dim-tag">不计分（采集类）</a-tag>
    </div>

    <!-- 双侧量表：左右分栏 -->
    <template v-if="hasSide">
      <a-row :gutter="16">
        <a-col :span="12">
          <div class="side-title">左侧</div>
          <div v-for="item in itemsOf('LEFT')" :key="item.no" class="scale-item">
            <div class="item-text">{{ item.no }}. {{ item.text }}</div>
            <a-radio-group v-model:value="answers[item.no]" size="small">
              <a-radio-button v-for="opt in item.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
            </a-radio-group>
          </div>
        </a-col>
        <a-col :span="12">
          <div class="side-title">右侧</div>
          <div v-for="item in itemsOf('RIGHT')" :key="item.no" class="scale-item">
            <div class="item-text">{{ item.no }}. {{ item.text }}</div>
            <a-radio-group v-model:value="answers[item.no]" size="small">
              <a-radio-button v-for="opt in item.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
            </a-radio-group>
          </div>
        </a-col>
      </a-row>
    </template>

    <!-- 单列量表：按控件类型 + 数据绑定渲染 -->
    <template v-else>
      <div v-for="item in def.items" :key="item.no" class="scale-item">
        <div class="item-text">
          {{ item.no }}. {{ item.text }}
          <!-- 只读带入：来源与时间 -->
          <template v-if="item.binding?.mode === 'AUTO_READONLY'">
            <a-tag v-if="boundMeta[item.no]" :color="BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].color" class="ml8">
              <ApiOutlined /> {{ BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].label }}带入 · {{ boundMeta[item.no].at }}
            </a-tag>
            <a-tag v-else color="default" class="ml8">无{{ BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].label }}数据{{ item.binding.windowDays ? `（${item.binding.windowDays}天内）` : '' }}</a-tag>
          </template>
          <!-- 可编辑带入：提示可改 -->
          <a-tag v-if="item.binding?.mode === 'AUTO_EDITABLE' && boundMeta[item.no]" color="cyan" class="ml8">
            <ApiOutlined /> {{ BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].label }}带入 · 可修改
          </a-tag>
          <!-- 选项源 -->
          <a-tag v-if="item.binding?.mode === 'OPTIONS'" :color="BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].color" class="ml8">
            <ApiOutlined /> 选项来自{{ BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].label }}记录
          </a-tag>
        </div>

        <!-- 只读快照展示 -->
        <div v-if="item.binding?.mode === 'AUTO_READONLY'" class="readonly-box">
          <span class="readonly-value">{{ boundMeta[item.no]?.value ?? '—' }}</span>
          <span v-if="boundMeta[item.no]?.unit" class="readonly-unit">{{ boundMeta[item.no].unit }}</span>
          <a-button size="small" type="text" :title="'重新取 ' + BINDING_DOMAIN_META[item.binding.domain as keyof typeof BINDING_DOMAIN_META].label + ' 最新值'" @click="initBindings">
            <ReloadOutlined />
          </a-button>
        </div>

        <!-- 选项源动态枚举（多选/下拉/单选） -->
        <a-checkbox-group v-else-if="item.type === 'CHECKBOX'" v-model:value="answers[item.no]" class="wide">
          <a-checkbox v-for="o in dynamicOptions(item)" :key="String(o.value)" :value="o.value">{{ o.label }}</a-checkbox>
        </a-checkbox-group>
        <a-select v-else-if="item.type === 'SELECT'" v-model:value="answers[item.no]" :options="dynamicOptions(item)"
          placeholder="请选择" allow-clear style="min-width: 220px" />
        <a-radio-group v-else-if="item.type === 'RADIO'" v-model:value="answers[item.no]" size="small">
          <a-radio-button v-for="opt in dynamicOptions(item)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>

        <!-- 常规控件（含可编辑带入预填） -->
        <a-input-number v-else-if="item.type === 'NUMBER'" v-model:value="answers[item.no]"
          :min="0" :max="item.max ?? 10" :addon-after="boundMeta[item.no]?.unit" style="width: 200px" />
        <a-input v-else-if="item.type === 'INPUT'" v-model:value="answers[item.no]"
          placeholder="请输入" style="max-width: 420px" />
        <a-radio-group v-else v-model:value="answers[item.no]" size="small">
          <a-radio-button v-for="opt in item.options" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ApiOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { scaleByCode, resolveBinding, BINDING_DOMAIN_META } from '../mock'
import type { BindingValue, ScaleBinding } from '../mock'

const props = defineProps<{
  scaleCode: string
  required?: boolean
  /** 评估患者上下文：绑定量表据此解析业务值（设计器预览不传） */
  patientId?: number
  /** 设计器预览：直接传入 definition，不查 mock 量表库 */
  previewDefinition?: {
    items: {
      no: string; text: string; side?: string; type?: string; max?: number; required?: boolean
      options: { value: number | string; label: string }[]
      binding?: ScaleBinding | null
    }[]
    maxScore: number
    mcid?: number | null
    interpretation?: { min: number; max: number; label: string }[]
  }
}>()

const emit = defineEmits<{
  (e: 'change', payload: { scaleCode: string; totalScore: number; answers: Record<string, any>; complete: boolean }): void
}>()

const scale = computed(() => {
  if (props.previewDefinition) {
    return {
      name: '（预览）未命名量表',
      scaleCode: props.scaleCode,
      version: '-',
      definition: props.previewDefinition as any,
    }
  }
  return scaleByCode(props.scaleCode)!
})
const def = computed(() => scale.value.definition as any)
const hasSide = computed(() => (def.value.items as any[]).some(i => i.side))
const hasBinding = computed(() => (def.value.items as any[]).some(i => i.binding))

const answers = ref<Record<string, any>>({})
/* 只读/可编辑绑定项的解析结果 { [no]: {value, at, unit} } */
const boundMeta = ref<Record<string, BindingValue>>({})

const itemsOf = (side: string) => (def.value.items as any[]).filter(i => i.side === side)

/* 绑定解析：AUTO_* 预填值 + 来源元数据；OPTIONS 枚举选项 */
function initBindings() {
  const meta: Record<string, BindingValue> = {}
  for (const item of def.value.items as any[]) {
    const b = item.binding
    if (!b) continue
    const resolved = resolveBinding(props.patientId, b as ScaleBinding)
    if (b.mode === 'OPTIONS') continue
    if (resolved && !Array.isArray(resolved)) {
      meta[item.no] = resolved
      if (answers.value[item.no] === undefined) answers.value[item.no] = resolved.value
    }
  }
  boundMeta.value = meta
}
/* OPTIONS 模式：按患者枚举该域记录为选项 */
function dynamicOptions(item: any): { value: any; label: string }[] {
  if (item.binding?.mode !== 'OPTIONS') return item.options || []
  const resolved = resolveBinding(props.patientId, item.binding as ScaleBinding)
  return Array.isArray(resolved)
    ? resolved.map(r => ({ value: String(r.value), label: String(r.value) }))
    : []  // 无业务数据时留空，执行人可看 INPUT 兜底说明
}

/* 完成度：只读带入项由系统负责不参与判定；多选需至少勾 1 项 */
function isAnswered(item: any): boolean {
  if (item.binding?.mode === 'AUTO_READONLY') return true
  const v = answers.value[item.no]
  if (Array.isArray(v)) return v.length > 0
  return v !== undefined && v !== '' && v !== null
}
const totalScore = computed(() =>
  Object.values(answers.value).reduce((s, v) => s + (typeof v === 'number' ? v : 0), 0))
const complete = computed(() => (def.value.items as any[]).every(isAnswered))

const interpretation = computed(() => {
  const interps: any[] = def.value.interpretation || []
  const hit = interps.find(r => totalScore.value >= r.min && totalScore.value <= r.max)
  return hit?.label ?? ''
})
const interpretColor = computed(() => {
  const interps: any[] = def.value.interpretation || []
  const hit = interps.find(r => totalScore.value >= r.min && totalScore.value <= r.max)
  return hit?.label === '轻微' || hit?.label === '正常' ? 'green' : hit?.label === '中度' || hit?.label === '减退' ? 'orange' : 'red'
})
const scoreClass = computed(() => (interpretColor.value === 'red' ? 'score-high' : ''))

watch(() => props.scaleCode, () => { answers.value = {}; boundMeta.value = {} })
onMounted(initBindings)
watch(() => props.patientId, initBindings)

watch([totalScore, complete], () => {
  emit('change', {
    scaleCode: props.scaleCode,
    totalScore: totalScore.value,
    answers: { ...answers.value },
    complete: complete.value,
  })
})
</script>

<style scoped>
.scale-fill-panel { border: 1px solid #f0f0f0; border-radius: 6px; padding: 16px; margin-bottom: 16px; background: #fafafa; }
.scale-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.scale-name { font-weight: 600; font-size: 15px; }
.ml8 { margin-left: 8px; }
.score-box { display: flex; align-items: baseline; gap: 4px; }
.score-label { color: #999; font-size: 12px; }
.score-value { font-size: 26px; font-weight: 700; color: #1677ff; }
.score-value.score-high { color: #cf1322; }
.score-max { color: #999; }
.dim-tag { color: #999; border-color: #e5e5e5; }
.side-title { font-weight: 600; margin-bottom: 8px; padding: 4px 8px; background: #e6f4ff; border-radius: 4px; }
.scale-item { padding: 8px 0; border-bottom: 1px dashed #eee; }
.item-text { margin-bottom: 6px; font-size: 13px; }
.readonly-box { display: inline-flex; align-items: baseline; gap: 8px; background: #f6f0ff; border: 1px solid #d3b8f5; border-radius: 6px; padding: 6px 12px; }
.readonly-value { font-size: 18px; font-weight: 700; color: #722ed1; }
.readonly-unit { color: #999; font-size: 12px; }
.wide { display: flex; flex-wrap: wrap; gap: 8px 16px; }
</style>
