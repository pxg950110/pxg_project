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
              <a-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
            </a-radio-group>
          </div>
        </a-col>
        <a-col :span="12">
          <div class="side-title">右侧</div>
          <div v-for="item in itemsOf('RIGHT')" :key="item.no" class="scale-item">
            <div class="item-text">{{ item.no }}. {{ item.text }}</div>
            <a-radio-group v-model:value="answers[item.no]" size="small">
              <a-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
            </a-radio-group>
          </div>
        </a-col>
      </a-row>
    </template>

    <!-- 单列量表 -->
    <template v-else>
      <div v-for="item in def.items" :key="item.no" class="scale-item">
        <div class="item-text">
          {{ item.no }}. {{ item.text }}
          <template v-if="item.binding?.mode === 'AUTO_READONLY'">
            <a-tag v-if="bound[item.no]" :color="domainColor(item.binding.domain)" class="ml8">
              <ApiOutlined /> {{ domainLabel(item.binding.domain) }}带入 · {{ bound[item.no].at }}
            </a-tag>
            <a-tag v-else-if="bindingResolved" color="default" class="ml8">
              无{{ domainLabel(item.binding.domain) }}数据{{ item.binding.windowDays ? `（${item.binding.windowDays}天内）` : '' }}
            </a-tag>
          </template>
          <a-tag v-if="item.binding?.mode === 'AUTO_EDITABLE' && bound[item.no]" color="cyan" class="ml8">
            <ApiOutlined /> {{ domainLabel(item.binding.domain) }}带入 · 可修改
          </a-tag>
          <a-tag v-if="item.binding?.mode === 'OPTIONS'" :color="domainColor(item.binding.domain)" class="ml8">
            <ApiOutlined /> 选项来自{{ domainLabel(item.binding.domain) }}记录
          </a-tag>
        </div>

        <!-- 只读快照 -->
        <div v-if="item.binding?.mode === 'AUTO_READONLY'" class="readonly-box">
          <span class="readonly-value">{{ bound[item.no]?.value ?? '—' }}</span>
          <span v-if="bound[item.no]?.unit" class="readonly-unit">{{ bound[item.no].unit }}</span>
        </div>

        <!-- 多选（含 OPTIONS 动态枚举） -->
        <a-checkbox-group v-else-if="item.type === 'CHECKBOX'" v-model:value="answers[item.no]" class="wide">
          <a-checkbox v-for="o in optionsOf(item)" :key="String(o.value)" :value="o.value">{{ o.label }}</a-checkbox>
        </a-checkbox-group>
        <a-select v-else-if="item.type === 'SELECT'" v-model:value="answers[item.no]"
          :options="optionsOf(item).map(o => ({ value: o.value, label: o.label }))"
          placeholder="请选择" allow-clear style="min-width: 220px" />
        <a-radio-group v-else-if="item.type === 'RADIO'" v-model:value="answers[item.no]" size="small">
          <a-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>

        <!-- 常规控件 -->
        <a-input-number v-else-if="item.type === 'NUMBER'" v-model:value="answers[item.no]"
          :min="0" :max="item.max ?? 10" :addon-after="bound[item.no]?.unit" style="width: 200px" />
        <a-input v-else-if="item.type === 'INPUT'" v-model:value="answers[item.no]"
          placeholder="请输入" style="max-width: 420px" />
        <a-radio-group v-else v-model:value="answers[item.no]" size="small">
          <a-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ApiOutlined } from '@ant-design/icons-vue'
import { getScaleLatest, resolveScaleBindings } from '@/api/followup'

/**
 * 通用量表填写组件：definition 驱动（后端 c_scale_definition），
 * patientId 传入时自动解析数据绑定（只读快照/预填/选项枚举）。
 */
const props = defineProps<{
  scaleCode: string
  required?: boolean
  patientId?: number
  /** 直接传入 definition（设计器预览用），跳过 API */
  definition?: any
}>()

const emit = defineEmits<{
  (e: 'change', payload: { scaleCode: string; totalScore: number; answers: Record<string, any>; complete: boolean }): void
}>()

const DOMAIN_META: Record<string, { label: string; color: string }> = {
  LAB: { label: '检验', color: 'purple' },
  VITAL: { label: '体征', color: 'green' },
  MEDICATION: { label: '用药', color: 'orange' },
  DIAGNOSIS: { label: '诊断', color: 'blue' },
  IMAGING: { label: '影像', color: 'cyan' },
}
const domainLabel = (d: string) => DOMAIN_META[d]?.label || d
const domainColor = (d: string) => DOMAIN_META[d]?.color || 'default'

const scale = ref<any>({ name: props.scaleCode, scaleCode: props.scaleCode, version: '-', definition: { items: [] } })
const bound = ref<Record<string, any>>({})
const bindingResolved = ref(false)

const def = computed(() => scale.value.definition || { items: [] })
const hasSide = computed(() => (def.value.items as any[]).some(i => i.side))
const hasBinding = computed(() => (def.value.items as any[]).some(i => i.binding))

const answers = ref<Record<string, any>>({})

const itemsOf = (side: string) => (def.value.items as any[]).filter(i => i.side === side)

/** 选项：item.options 优先，回退量表级 itemOptions；OPTIONS 绑定模式用解析到的动态选项 */
function optionsOf(item: any): { value: any; label: string }[] {
  if (item.binding?.mode === 'OPTIONS') {
    const opts = bound.value[item.no]?.options
    return (opts || []).map((label: string) => ({ value: label, label }))
  }
  const opts = (item.options && item.options.length ? item.options : def.value.itemOptions) || []
  return opts.map((o: any) => ({ value: o.value, label: o.label }))
}

async function load() {
  bindingResolved.value = false
  bound.value = {}
  answers.value = {}
  if (props.definition) {
    scale.value = { name: '（预览）' + props.definition.name || props.scaleCode, scaleCode: props.scaleCode, version: '-', definition: props.definition }
    return
  }
  try {
    const res = await getScaleLatest(props.scaleCode)
    scale.value = res.data?.data || scale.value
  } catch { /* 量表加载失败保持空壳 */ }
  if (props.patientId && hasBinding.value) {
    try {
      const res = await resolveScaleBindings(props.scaleCode, props.patientId)
      const items = res.data?.data || {}
      bound.value = items
      // AUTO_* 值型绑定项预填
      for (const [no, info] of Object.entries(items)) {
        if ((info as any).mode === 'OPTIONS') continue
        const v = (info as any).value
        if (v !== undefined && v !== null && answers.value[no] === undefined) {
          answers.value[no] = Number(v)
        }
      }
    } catch { /* 绑定解析失败按无数据渲染 */ }
    bindingResolved.value = true
  }
}

function isAnswered(item: any): boolean {
  if (item.binding?.mode === 'AUTO_READONLY') return true
  const v = answers.value[item.no]
  if (Array.isArray(v)) return v.length > 0
  return v !== undefined && v !== '' && v !== null
}

const totalScore = computed(() =>
  Object.values(answers.value).reduce((s, v) => s + (typeof v === 'number' ? v : 0), 0))
const complete = computed(() => (def.value.items as any[]).length > 0 && (def.value.items as any[]).every(isAnswered))

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

onMounted(load)
watch(() => props.scaleCode, load)

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
