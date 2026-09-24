<template>
  <div v-loading="loading" class="p360-overview-container max-w-[1600px] mx-auto space-y-4 p-4">
    <!-- 顶部患者画像身份卡片 (Modern Clinical Banner) -->
    <div class="relative bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 rounded-2xl p-6 text-white shadow-clinical overflow-hidden border border-slate-700/60">
      <!-- 装饰性背景科技光晕 -->
      <div class="absolute -right-16 -top-16 w-64 h-64 bg-sky-500/15 rounded-full blur-3xl pointer-events-none" />
      <div class="absolute right-40 -bottom-20 w-80 h-80 bg-teal-500/10 rounded-full blur-3xl pointer-events-none" />

      <div class="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div class="space-y-2">
          <div class="flex items-center gap-3 flex-wrap">
            <h2 class="text-2xl font-bold tracking-tight text-white m-0">
              {{ basicInfo?.name || '—' }}
            </h2>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-500/20 text-sky-300 border border-sky-400/30">
              {{ genderLabel }} · {{ basicInfo?.age ?? '—' }} 岁
            </span>
            <span class="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-mono bg-slate-800 text-slate-300 border border-slate-700">
              病案号: {{ basicInfo?.patient_no || '—' }}
            </span>
          </div>

          <p class="text-xs text-slate-400 flex items-center gap-2">
            <span>数据同步基准截至: {{ today }}</span>
            <span class="text-slate-600">|</span>
            <span class="text-emerald-400 font-medium flex items-center gap-1">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
              CDR 实时就诊时序已归集
            </span>
          </p>
        </div>

        <!-- 过敏史与家族史警示徽标区 -->
        <div class="flex flex-wrap items-center gap-2 max-w-xl">
          <!-- 过敏警示 -->
          <div
            v-for="item in allergies"
            :key="item.allergen"
            class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-medium bg-rose-950/80 text-rose-200 border border-rose-500/40 shadow-sm"
          >
            <span class="px-1.5 py-0.2 rounded bg-rose-500 text-white text-[10px] font-bold">过敏</span>
            <span>{{ item.allergen }}</span>
            <span v-if="item.reaction" class="text-rose-300 text-[11px]">({{ item.reaction }})</span>
          </div>

          <!-- 家族史 -->
          <div
            v-for="item in familyHistory"
            :key="`${item.relation}-${item.disease}`"
            class="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-medium bg-amber-950/70 text-amber-200 border border-amber-500/30"
          >
            <span class="px-1.5 py-0.2 rounded bg-amber-500/80 text-white text-[10px] font-bold">家族史</span>
            <span>{{ item.relation }} · {{ item.disease }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 4 列核心指标卡片 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex items-center justify-between">
        <div class="space-y-1">
          <span class="text-xs font-medium text-slate-500">累计就诊次数</span>
          <div class="text-2xl font-bold font-mono text-slate-900">
            {{ encounterStats?.total_encounters ?? '—' }}
            <span class="text-xs font-normal text-slate-500">次</span>
          </div>
        </div>
        <div class="w-10 h-10 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center font-bold">
          <el-icon :size="20"><Document /></el-icon>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex items-center justify-between">
        <div class="space-y-1">
          <span class="text-xs font-medium text-slate-500">活跃系统诊断</span>
          <div class="text-2xl font-bold font-mono text-slate-900">
            {{ diagnosisGroups.length }}
            <span class="text-xs font-normal text-slate-500">个系统</span>
          </div>
        </div>
        <div class="w-10 h-10 rounded-xl bg-violet-50 text-violet-600 flex items-center justify-center font-bold">
          <el-icon :size="20"><FolderChecked /></el-icon>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex items-center justify-between">
        <div class="space-y-1">
          <span class="text-xs font-medium text-slate-500">异常/危急指标</span>
          <div class="text-2xl font-bold font-mono text-rose-600">
            {{ abnormal.total }}
            <span class="text-xs font-normal text-slate-500">项</span>
          </div>
        </div>
        <div class="w-10 h-10 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
          <el-icon :size="20"><Warning /></el-icon>
        </div>
      </div>

      <div class="bg-white rounded-xl border border-slate-200/80 p-4 shadow-clinical-sm flex items-center justify-between">
        <div class="space-y-1">
          <span class="text-xs font-medium text-slate-500">数据完整度评分</span>
          <div class="text-2xl font-bold font-mono text-emerald-600">
            {{ completeness ?? '—' }}
            <span class="text-xs font-normal text-slate-500">%</span>
          </div>
        </div>
        <div class="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
          <el-icon :size="20"><CircleCheck /></el-icon>
        </div>
      </div>
    </div>

    <!-- 诊断地图 (人体热区与系统 ICD 联动) -->
    <div class="bg-white rounded-xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
      <div class="flex items-center justify-between border-b border-slate-100 pb-3">
        <div class="flex items-center gap-2">
          <span class="w-1 h-4 bg-sky-500 rounded-full" />
          <h3 class="text-sm font-semibold text-slate-900 m-0">全身系统诊断地图</h3>
          <span class="text-xs text-slate-500">依据 ICD-10 章节聚合归类至身体对应解剖系统</span>
        </div>
      </div>

      <div class="relative min-h-[480px] flex flex-col lg:flex-row items-center justify-between gap-6 py-4">
        <!-- 左侧系统卡片列表 -->
        <div class="w-full lg:w-1/3 space-y-3 z-10">
          <div
            v-for="group in leftGroups"
            :key="group.system"
            class="bg-slate-50/90 rounded-xl border border-slate-200/80 p-3.5 hover:shadow-clinical hover:border-sky-300 transition-all"
          >
            <div class="flex justify-between items-center mb-1.5">
              <span class="text-xs font-bold text-slate-800">{{ group.system }}</span>
              <span class="text-[11px] text-slate-400">{{ lastVisitOf(group) }}</span>
            </div>
            <div class="space-y-1">
              <div
                v-for="diag in group.diagnoses"
                :key="diag.code"
                class="flex items-center justify-between text-xs text-slate-700 bg-white p-1.5 rounded border border-slate-100"
              >
                <div class="flex items-center gap-1.5 truncate">
                  <span v-if="diag.isPrimary" class="px-1 py-0.2 rounded bg-sky-100 text-sky-700 text-[10px] font-bold">主</span>
                  <span class="font-medium truncate">{{ diag.name || diag.code }}</span>
                </div>
                <div class="flex items-center gap-2 text-slate-400 font-mono text-[11px] flex-shrink-0">
                  <span>{{ diag.code }}</span>
                  <span class="text-slate-500 font-sans">{{ diag.visitCount }} 次</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中部人体矢量解剖图与热区动画 -->
        <div class="relative w-full lg:w-1/3 flex justify-center py-2">
          <svg viewBox="0 0 200 460" class="w-48 h-auto select-none" aria-hidden="true">
            <g fill="#F1F5F9" stroke="#CBD5E1" stroke-width="1.5">
              <ellipse cx="100" cy="30" rx="21" ry="26" />
              <rect x="90" y="52" width="20" height="14" rx="6" />
              <path d="M60 78 Q100 64 140 78 L148 150 Q150 210 138 252 L128 258 Q100 268 72 258 L62 252 Q50 210 52 150 Z" />
              <path d="M60 78 Q40 86 36 130 L30 200 Q29 212 38 213 Q47 214 49 200 L58 140 Z" />
              <path d="M140 78 Q160 86 164 130 L170 200 Q171 212 162 213 Q153 214 151 200 L142 140 Z" />
              <path d="M76 260 Q70 330 72 380 Q73 420 70 448 L90 448 Q96 410 96 370 L100 320 L104 370 Q104 410 110 448 L130 448 Q127 420 128 380 Q130 330 124 260 Z" />
            </g>
            <!-- 肺/胃解剖抽象区 -->
            <g opacity="0.6">
              <path d="M82 100 Q92 92 98 102 L98 138 Q88 142 80 134 Z" fill="#FCA5A5" />
              <path d="M118 100 Q108 92 102 102 L102 138 Q112 142 120 134 Z" fill="#FCA5A5" />
              <path d="M100 150 Q116 148 120 162 Q122 172 112 174 L102 170 Z" fill="#FDBA74" />
              <path d="M86 190 Q100 182 116 190 Q120 204 112 216 Q100 224 88 216 Q80 204 86 190 Z" fill="#FED7AA" />
            </g>
            <!-- 脉冲热区点 -->
            <g v-for="group in hotspotGroups" :key="group.system">
              <circle
                :cx="hotX(group.hotspotX!)" :cy="hotY(group.hotspotY!)" r="10"
                fill="rgba(14, 165, 233, 0.2)" class="animate-ping"
              />
              <circle
                :cx="hotX(group.hotspotX!)" :cy="hotY(group.hotspotY!)" r="5"
                fill="#0EA5E9" stroke="#fff" stroke-width="1.5"
              />
            </g>
          </svg>
        </div>

        <!-- 右侧系统卡片列表 -->
        <div class="w-full lg:w-1/3 space-y-3 z-10">
          <div
            v-for="group in rightGroups"
            :key="group.system"
            class="bg-slate-50/90 rounded-xl border border-slate-200/80 p-3.5 hover:shadow-clinical hover:border-sky-300 transition-all"
          >
            <div class="flex justify-between items-center mb-1.5">
              <span class="text-xs font-bold text-slate-800">{{ group.system }}</span>
              <span class="text-[11px] text-slate-400">{{ lastVisitOf(group) }}</span>
            </div>
            <div class="space-y-1">
              <div
                v-for="diag in group.diagnoses"
                :key="diag.code"
                class="flex items-center justify-between text-xs text-slate-700 bg-white p-1.5 rounded border border-slate-100"
              >
                <div class="flex items-center gap-1.5 truncate">
                  <span v-if="diag.isPrimary" class="px-1 py-0.2 rounded bg-sky-100 text-sky-700 text-[10px] font-bold">主</span>
                  <span class="font-medium truncate">{{ diag.name || diag.code }}</span>
                </div>
                <div class="flex items-center gap-2 text-slate-400 font-mono text-[11px] flex-shrink-0">
                  <span>{{ diag.code }}</span>
                  <span class="text-slate-500 font-sans">{{ diag.visitCount }} 次</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 异常检验指标展示 -->
    <div class="bg-white rounded-xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
      <div class="flex items-center justify-between border-b border-slate-100 pb-3">
        <div class="flex items-center gap-2">
          <span class="w-1 h-4 bg-rose-500 rounded-full" />
          <h3 class="text-sm font-semibold text-slate-900 m-0">异常体检与生化指标</h3>
          <span class="text-xs text-slate-500">累计检出 {{ abnormal.total }} 项超标指标 · 当前展示最近 {{ abnormal.items.length }} 项</span>
        </div>
      </div>

      <div v-if="abnormal.items.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
        <div
          v-for="(item, idx) in abnormal.items"
          :key="idx"
          class="p-3 rounded-lg border border-slate-200/80 bg-slate-50/50 hover:bg-white hover:shadow-clinical-sm transition-all flex flex-col justify-between gap-2"
        >
          <div class="space-y-0.5">
            <div class="text-xs font-bold text-slate-800 truncate">{{ item.item_name }}</div>
            <div class="text-[11px] text-slate-400">
              {{ item.checkup_date }}
              <template v-if="item.reference_range"> · 参考 {{ item.reference_range }}</template>
            </div>
          </div>
          <div class="flex items-baseline justify-between pt-1 border-t border-slate-100">
            <span class="text-sm font-bold font-mono" :class="directionColor(item.direction)">
              {{ item.result_value }} <span v-if="item.unit" class="text-xs font-normal text-slate-400">{{ item.unit }}</span>
            </span>
            <span
              class="px-1.5 py-0.2 rounded text-[10px] font-bold"
              :class="item.direction === '偏高' ? 'bg-rose-100 text-rose-700' : 'bg-amber-100 text-amber-700'"
            >
              {{ item.direction }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="py-8 text-center text-xs text-slate-400">
        暂无检验异常与危急指标记录
      </div>
    </div>

    <!-- 就诊时序与病史摘要 -->
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-4 items-start">
      <!-- 左侧就诊时间轴 (8/12) -->
      <div class="lg:col-span-8 bg-white rounded-xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-1 h-4 bg-sky-500 rounded-full" />
            <h3 class="text-sm font-semibold text-slate-900 m-0">就诊纵向时间轴</h3>
            <span class="text-xs text-slate-500">归集展示最近 {{ timeline.length }} 次就诊事件</span>
          </div>
        </div>

        <div v-if="timeline.length === 0" class="py-12 text-center text-xs text-slate-400">
          暂无历史就诊记录
        </div>

        <div v-else class="space-y-4 relative pl-4 border-l-2 border-slate-100 ml-2">
          <div
            v-for="(enc, idx) in timeline"
            :key="enc.encounter_id"
            class="relative group"
          >
            <!-- 时间轴圆点 -->
            <span
              class="absolute -left-[23px] top-1.5 w-3.5 h-3.5 rounded-full border-2 border-white transition-colors"
              :class="idx === 0 ? 'bg-sky-500 shadow-sm shadow-sky-500/50' : 'bg-slate-300 group-hover:bg-sky-400'"
            />

            <!-- 就诊卡片 -->
            <div class="bg-slate-50/70 group-hover:bg-white rounded-xl border border-slate-200/70 group-hover:border-sky-300 p-3.5 shadow-clinical-sm transition-all space-y-1.5">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <span class="text-xs font-bold text-slate-900">{{ formatDate(enc.admission_time) }}</span>
                  <span
                    class="px-1.5 py-0.2 rounded text-[10px] font-semibold"
                    :class="encounterTagClass(enc.encounter_type)"
                  >
                    {{ encounterTypeLabel(enc.encounter_type) }}
                  </span>
                </div>
                <button
                  type="button"
                  class="text-xs text-sky-600 hover:text-sky-700 font-medium flex items-center gap-0.5 border-0 bg-transparent cursor-pointer"
                  @click="goEncounterDetail(enc)"
                >
                  详情分析
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>

              <div class="text-xs font-semibold text-slate-800">
                {{ diagnosisOf(enc) }}
              </div>

              <div class="text-[11px] text-slate-400">
                就诊科室: {{ enc.department || '—' }}
                <template v-if="enc.attending_doctor"> · 主诊医师: {{ enc.attending_doctor }}</template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧病史摘要与数据质量 (4/12) -->
      <div class="lg:col-span-4 bg-white rounded-xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
        <div class="flex items-center gap-2 border-b border-slate-100 pb-3">
          <span class="w-1 h-4 bg-teal-500 rounded-full" />
          <h3 class="text-sm font-semibold text-slate-900 m-0">临床病史与数据档案</h3>
        </div>

        <div class="space-y-3 text-xs">
          <!-- 过敏史 -->
          <div class="p-3 rounded-lg bg-rose-50/50 border border-rose-100 space-y-1">
            <div class="font-bold text-rose-700 flex items-center gap-1.5">
              <el-icon><WarningFilled /></el-icon>
              <span>过敏史警示</span>
            </div>
            <p class="text-slate-700 leading-relaxed m-0">
              <template v-if="allergies.length > 0">
                {{ allergies.map(a => a.allergen).join('、') }}
              </template>
              <template v-else>未见药物或食物过敏登记记录</template>
            </p>
            <span class="text-[10px] text-slate-400 block pt-1">处方下达前系统将自动进行禁忌交叉校验</span>
          </div>

          <!-- 家族病史 -->
          <div class="p-3 rounded-lg bg-amber-50/40 border border-amber-100 space-y-1">
            <div class="font-bold text-amber-700 flex items-center gap-1.5">
              <el-icon><InfoFilled /></el-icon>
              <span>家族慢性病史</span>
            </div>
            <p class="text-slate-700 leading-relaxed m-0">
              <template v-if="familyHistory.length > 0">
                {{ familyHistory.map(f => `${f.relation}·${f.disease}`).join('、') }}
              </template>
              <template v-else>暂无家族病史登记</template>
            </p>
          </div>

          <!-- 完整度度量 -->
          <div class="p-3 rounded-lg bg-slate-50 border border-slate-200/60 space-y-2">
            <div class="flex justify-between items-center text-slate-800 font-medium">
              <span>CDR 临床数据完整度</span>
              <span class="font-bold text-sky-600 font-mono">{{ completeness ?? '—' }}%</span>
            </div>
            <el-progress
              :percentage="completeness ?? 0"
              :stroke-width="6"
              :show-text="false"
              color="#0EA5E9"
            />
            <span class="text-[11px] text-slate-400 block">综合门急诊病历、诊断编码、检验指标及影像归档完整性</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Document, FolderChecked, Warning, CircleCheck, ArrowRight, WarningFilled, InfoFilled,
} from '@element-plus/icons-vue'
import {
  getPatientBasicInfo, getEncounterStats, getDiagnosisMap, getAbnormalIndicators,
  getAllergies, getFamilyHistory, getCompletenessScore, getTimeline,
} from '@/api/patient360'
import type {
  PatientBasicInfo, EncounterStats, DiagnosisMapGroup,
  AbnormalIndicator, AllergyItem, FamilyHistoryItem, TimelineItem,
} from '@/api/patient360'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const basicInfo = ref<PatientBasicInfo | null>(null)
const encounterStats = ref<EncounterStats | null>(null)
const diagnosisGroups = ref<DiagnosisMapGroup[]>([])
const abnormal = ref<{ total: number; items: AbnormalIndicator[] }>({ total: 0, items: [] })
const allergies = ref<AllergyItem[]>([])
const familyHistory = ref<FamilyHistoryItem[]>([])
const completeness = ref<number | null>(null)
const timelineRaw = ref<TimelineItem[]>([])

/** timeline 按就诊去重 */
const timeline = computed(() => {
  const seen = new Map<number, TimelineItem & { diagnoses: string[] }>()
  for (const row of timelineRaw.value) {
    const existing = seen.get(row.encounter_id)
    if (existing) {
      if (row.icd_name && !existing.diagnoses.includes(row.icd_name)) {
        existing.diagnoses.push(row.icd_name)
      }
    } else {
      seen.set(row.encounter_id, {
        ...row,
        diagnoses: row.icd_name ? [row.icd_name] : [],
      })
    }
  }
  return Array.from(seen.values()).slice(0, 8)
})

function diagnosisOf(enc: TimelineItem & { diagnoses?: string[] }): string {
  const names = enc.diagnoses ?? (enc.icd_name ? [enc.icd_name] : [])
  return names.length > 0 ? names.join('、') : '未记录诊断'
}

const patientId = computed(() => {
  const id = route.params.patientId
  return id ? Number(id) : null
})

const today = new Date().toISOString().slice(0, 10)

const genderLabel = computed(() => {
  const g = basicInfo.value?.gender
  if (g === 'M') return '男'
  if (g === 'F') return '女'
  return g || '—'
})

/** 有热区的组 */
const hotspotGroups = computed(() =>
  diagnosisGroups.value.filter(g => g.hotspotX != null && g.hotspotY != null).slice(0, 4))

const leftGroups = computed(() => hotspotGroups.value.filter((_, i) => i % 2 === 0))
const rightGroups = computed(() => hotspotGroups.value.filter((_, i) => i % 2 === 1))

function hotX(pct: number): number {
  return (pct / 100) * 200
}
function hotY(pct: number): number {
  return (pct / 100) * 460
}

function lastVisitOf(group: DiagnosisMapGroup): string {
  const dates = group.diagnoses.map(d => d.lastVisit).filter(Boolean) as string[]
  return dates.length > 0 ? `最近 ${dates.sort().slice(-1)[0].slice(0, 10)}` : ''
}

function directionColor(direction: string) {
  if (direction === '偏高') return 'text-rose-600'
  if (direction === '偏低') return 'text-amber-600'
  return 'text-rose-500'
}

function encounterTypeLabel(type: string) {
  const map: Record<string, string> = { INPATIENT: '住院', OUTPATIENT: '门诊', EMERGENCY: '急诊' }
  return map[type] ?? type
}

function encounterTagClass(type: string) {
  const map: Record<string, string> = {
    INPATIENT: 'bg-amber-100 text-amber-800 border border-amber-200',
    OUTPATIENT: 'bg-sky-100 text-sky-800 border border-sky-200',
    EMERGENCY: 'bg-rose-100 text-rose-800 border border-rose-200',
  }
  return map[type] ?? 'bg-slate-100 text-slate-700'
}

function formatDate(value: string | null): string {
  return value ? String(value).slice(0, 10) : '—'
}

function goEncounterDetail(enc: TimelineItem) {
  router.push(`/data/cdr/patients/${patientId.value}/encounters/${enc.encounter_id}`)
}

async function fetchData() {
  if (!patientId.value) return
  loading.value = true
  const id = patientId.value
  try {
    const [basic, stats, diagMap, abn, allergy, family, score, tl] = await Promise.allSettled([
      getPatientBasicInfo(id),
      getEncounterStats(id),
      getDiagnosisMap(id),
      getAbnormalIndicators(id, 8),
      getAllergies(id),
      getFamilyHistory(id),
      getCompletenessScore(id),
      getTimeline(id),
    ])
    if (basic.status === 'fulfilled') basicInfo.value = basic.value.data.data
    if (stats.status === 'fulfilled') encounterStats.value = stats.value.data.data
    if (diagMap.status === 'fulfilled') diagnosisGroups.value = diagMap.value.data.data ?? []
    if (abn.status === 'fulfilled' && abn.value.data.data) {
      abnormal.value = {
        total: Number(abn.value.data.data.total ?? 0),
        items: abn.value.data.data.items ?? [],
      }
    }
    if (allergy.status === 'fulfilled') allergies.value = allergy.value.data.data ?? []
    if (family.status === 'fulfilled') familyHistory.value = family.value.data.data ?? []
    if (score.status === 'fulfilled') completeness.value = score.value.data.data
    if (tl.status === 'fulfilled') timelineRaw.value = tl.value.data.data ?? []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
watch(patientId, fetchData)
</script>
