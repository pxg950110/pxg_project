<template>
  <div class="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 shadow-clinical-sm space-y-4">
    <!-- 量表标题与分值状态栏 -->
    <div class="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-200/60">
      <div class="flex items-center gap-2 flex-wrap">
        <span class="font-bold text-slate-800 text-sm">{{ scale.name }}</span>
        <el-tag size="small" type="info" effect="plain" class="!rounded font-mono">
          {{ scale.scaleCode }} · v{{ scale.version }}
        </el-tag>
        <el-tag v-if="required" size="small" type="danger" effect="light" class="!rounded font-medium">
          必评
        </el-tag>
        <el-tag v-else size="small" type="info" effect="plain" class="!rounded">
          选评
        </el-tag>
        <el-tag v-if="hasBinding" size="small" type="primary" effect="plain" class="!rounded">
          <el-icon class="mr-0.5"><Connection /></el-icon>
          含业务数据带入
        </el-tag>
      </div>

      <!-- 得分看板 -->
      <div v-if="def.maxScore" class="flex items-baseline gap-1.5">
        <span class="text-xs text-slate-400">当前得分</span>
        <span class="text-2xl font-bold font-mono" :class="scoreClass">{{ totalScore }}</span>
        <span class="text-xs text-slate-400 font-mono">/ {{ def.maxScore }}</span>
        <el-tag v-if="totalScore > 0" size="small" :type="interpretTagType" effect="dark" class="!rounded ml-1">
          {{ interpretation }}
        </el-tag>
      </div>
      <el-tag v-else size="small" type="info" effect="plain" class="!rounded">
        不计分（采集类）
      </el-tag>
    </div>

    <!-- 双侧量表：左右分栏（如鼻内镜左右鼻腔评分） -->
    <template v-if="hasSide">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="bg-white p-3.5 rounded-xl border border-slate-200/80 space-y-3 shadow-clinical-sm">
          <div class="text-xs font-bold text-sky-700 px-2 py-1 bg-sky-50 rounded-md inline-block">
            左侧腔区
          </div>
          <div v-for="item in itemsOf('LEFT')" :key="item.no" class="pb-2.5 border-b border-slate-100 last:border-0 space-y-1.5">
            <div class="text-xs font-medium text-slate-700">{{ item.no }}. {{ item.text }}</div>
            <el-radio-group v-model="answers[item.no]" size="small">
              <el-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :label="opt.value">
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="bg-white p-3.5 rounded-xl border border-slate-200/80 space-y-3 shadow-clinical-sm">
          <div class="text-xs font-bold text-indigo-700 px-2 py-1 bg-indigo-50 rounded-md inline-block">
            右侧腔区
          </div>
          <div v-for="item in itemsOf('RIGHT')" :key="item.no" class="pb-2.5 border-b border-slate-100 last:border-0 space-y-1.5">
            <div class="text-xs font-medium text-slate-700">{{ item.no }}. {{ item.text }}</div>
            <el-radio-group v-model="answers[item.no]" size="small">
              <el-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :label="opt.value">
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>
    </template>

    <!-- 单列量表 -->
    <template v-else>
      <div class="bg-white p-4 rounded-xl border border-slate-200/80 space-y-4 shadow-clinical-sm">
        <div
          v-for="item in def.items"
          :key="item.no"
          class="pb-3 border-b border-slate-100 last:border-0 space-y-2"
        >
          <div class="flex items-center gap-2 flex-wrap text-xs text-slate-700 font-medium">
            <span>{{ item.no }}. {{ item.text }}</span>

            <!-- 数据带入标记 -->
            <template v-if="item.binding?.mode === 'AUTO_READONLY'">
              <el-tag v-if="bound[item.no]" size="small" :type="domainTagType(item.binding.domain)" effect="light" class="!rounded">
                <el-icon class="mr-0.5"><Connection /></el-icon>
                {{ domainLabel(item.binding.domain) }}带入 · {{ bound[item.no].at }}
              </el-tag>
              <el-tag v-else-if="bindingResolved" size="small" type="info" effect="plain" class="!rounded">
                无{{ domainLabel(item.binding.domain) }}数据{{ item.binding.windowDays ? `（${item.binding.windowDays}天内）` : '' }}
              </el-tag>
            </template>

            <el-tag v-if="item.binding?.mode === 'AUTO_EDITABLE' && bound[item.no]" size="small" type="success" effect="light" class="!rounded">
              <el-icon class="mr-0.5"><Connection /></el-icon>
              {{ domainLabel(item.binding.domain) }}带入 · 可修改
            </el-tag>

            <el-tag v-if="item.binding?.mode === 'OPTIONS'" size="small" :type="domainTagType(item.binding.domain)" effect="light" class="!rounded">
              <el-icon class="mr-0.5"><Connection /></el-icon>
              选项来自{{ domainLabel(item.binding.domain) }}记录
            </el-tag>
          </div>

          <!-- 只读快照 -->
          <div v-if="item.binding?.mode === 'AUTO_READONLY'" class="inline-flex items-baseline gap-2 bg-indigo-50/70 border border-indigo-200/60 rounded-lg px-3 py-1.5">
            <span class="font-mono font-bold text-sm text-indigo-700">{{ bound[item.no]?.value ?? '—' }}</span>
            <span v-if="bound[item.no]?.unit" class="text-xs text-slate-400">{{ bound[item.no].unit }}</span>
          </div>

          <!-- 多选 -->
          <el-checkbox-group v-else-if="item.type === 'CHECKBOX'" v-model="answers[item.no]" class="flex flex-wrap gap-2">
            <el-checkbox v-for="o in optionsOf(item)" :key="String(o.value)" :label="o.value">
              {{ o.label }}
            </el-checkbox>
          </el-checkbox-group>

          <!-- 下拉选择 -->
          <el-select
            v-else-if="item.type === 'SELECT'"
            v-model="answers[item.no]"
            placeholder="请选择"
            clearable
            size="small"
            class="!w-64"
          >
            <el-option
              v-for="o in optionsOf(item)"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>

          <!-- 单选按钮组 -->
          <el-radio-group v-else-if="item.type === 'RADIO'" v-model="answers[item.no]" size="small">
            <el-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :label="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>

          <!-- 数值输入 -->
          <div v-else-if="item.type === 'NUMBER'" class="flex items-center gap-2">
            <el-input-number
              v-model="answers[item.no]"
              :min="0"
              :max="item.max ?? 100"
              size="small"
              class="!w-44"
            />
            <span v-if="bound[item.no]?.unit" class="text-xs text-slate-400">{{ bound[item.no].unit }}</span>
          </div>

          <!-- 文本输入 -->
          <el-input
            v-else-if="item.type === 'INPUT'"
            v-model="answers[item.no]"
            placeholder="请输入评估记录"
            size="small"
            class="!max-w-md"
          />

          <!-- 默认回退单选 -->
          <el-radio-group v-else v-model="answers[item.no]" size="small">
            <el-radio-button v-for="opt in optionsOf(item)" :key="opt.value" :label="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Connection } from '@element-plus/icons-vue'
import { getScaleLatest, resolveScaleBindings } from '@/api/followup'

const props = defineProps<{
  scaleCode: string
  required?: boolean
  patientId?: number
  definition?: any
}>()

const emit = defineEmits<{
  (e: 'change', payload: { scaleCode: string; totalScore: number; answers: Record<string, any>; complete: boolean }): void
}>()

const DOMAIN_META: Record<string, { label: string; tagType: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  LAB: { label: '检验', tagType: 'primary' },
  VITAL: { label: '体征', tagType: 'success' },
  MEDICATION: { label: '用药', tagType: 'warning' },
  DIAGNOSIS: { label: '诊断', tagType: 'info' },
  IMAGING: { label: '影像', tagType: 'primary' },
}

const domainLabel = (d: string) => DOMAIN_META[d]?.label || d
const domainTagType = (d: string) => DOMAIN_META[d]?.tagType || 'info'

const scale = ref<any>({ name: props.scaleCode, scaleCode: props.scaleCode, version: '-', definition: { items: [] } })
const bound = ref<Record<string, any>>({})
const bindingResolved = ref(false)

const def = computed(() => scale.value.definition || { items: [] })
const hasSide = computed(() => (def.value.items as any[]).some((i) => i.side))
const hasBinding = computed(() => (def.value.items as any[]).some((i) => i.binding))

const answers = ref<Record<string, any>>({})

const itemsOf = (side: string) => (def.value.items as any[]).filter((i) => i.side === side)

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
    scale.value = {
      name: '（预览）' + (props.definition.name || props.scaleCode),
      scaleCode: props.scaleCode,
      version: '-',
      definition: props.definition,
    }
    return
  }
  try {
    const res = await getScaleLatest(props.scaleCode)
    scale.value = res.data?.data || scale.value
  } catch {}
  if (props.patientId && hasBinding.value) {
    try {
      const res = await resolveScaleBindings(props.scaleCode, props.patientId)
      const items = res.data?.data || {}
      bound.value = items
      for (const [no, info] of Object.entries(items)) {
        if ((info as any).mode === 'OPTIONS') continue
        const v = (info as any).value
        if (v !== undefined && v !== null && answers.value[no] === undefined) {
          answers.value[no] = Number(v)
        }
      }
    } catch {}
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
  Object.values(answers.value).reduce((s, v) => s + (typeof v === 'number' ? v : 0), 0),
)

const complete = computed(
  () => (def.value.items as any[]).length > 0 && (def.value.items as any[]).every(isAnswered),
)

const interpretation = computed(() => {
  const interps: any[] = def.value.interpretation || []
  const hit = interps.find((r) => totalScore.value >= r.min && totalScore.value <= r.max)
  return hit?.label ?? ''
})

const interpretTagType = computed<'success' | 'warning' | 'danger' | 'info'>(() => {
  const interps: any[] = def.value.interpretation || []
  const hit = interps.find((r) => totalScore.value >= r.min && totalScore.value <= r.max)
  if (hit?.label === '轻微' || hit?.label === '正常') return 'success'
  if (hit?.label === '中度' || hit?.label === '减退') return 'warning'
  return 'danger'
})

const scoreClass = computed(() => (interpretTagType.value === 'danger' ? 'text-rose-600' : 'text-sky-600'))

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
