<template>
  <div class="p-6 space-y-5 max-w-[1600px] mx-auto">
    <!-- 顶部导航与操作区 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <el-button link class="!text-slate-600 hover:!text-sky-600 !p-0" @click="router.back()">
          <el-icon :size="20"><Back /></el-icon>
        </el-button>
        <div>
          <h2 class="text-xl font-bold text-slate-900 tracking-tight m-0 flex items-center gap-2.5">
            <span class="w-2 h-5 bg-sky-500 rounded-full" />
            {{ meta.name ? `量表设计器 · ${meta.name}` : '新建临床量表' }}
          </h2>
          <p class="text-xs text-slate-500 mt-1 m-0">
            可视化量表画布编排，支持 Likert 计分单选、业务数据绑定带入、双侧腔区分组与临床预后分级。
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2.5">
        <el-button size="small" @click="router.push({ name: 'ScaleManagement' })">
          返回列表
        </el-button>
        <el-button size="small" type="info" plain @click="previewOpen = true">
          <el-icon class="mr-1"><View /></el-icon>
          预览量表
        </el-button>
        <el-button size="small" type="primary" :loading="publishing" @click="handlePublish">
          <el-icon class="mr-1"><Upload /></el-icon>
          发布新版本
        </el-button>
      </div>
    </div>

    <!-- 三栏核心设计工作台 -->
    <div class="flex flex-col lg:flex-row gap-5 min-h-[720px] items-start">
      <!-- ============ 左侧：控件库 (Palette) ============ -->
      <div class="w-full lg:w-56 flex-shrink-0 bg-white rounded-2xl border border-slate-200/80 p-4 shadow-clinical-sm space-y-4">
        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
          <span class="text-xs font-bold text-slate-800">可用控件库</span>
          <span class="text-[11px] text-slate-400">点击或拖拽</span>
        </div>

        <div class="grid grid-cols-2 gap-2.5">
          <div
            v-for="c in controls"
            :key="c.type"
            draggable="true"
            class="flex flex-col items-center justify-center gap-1.5 p-3 rounded-xl border border-slate-200/80 bg-slate-50/60 hover:bg-sky-50/80 hover:border-sky-300/80 hover:text-sky-600 transition-all duration-150 cursor-grab select-none text-xs text-slate-700 shadow-sm"
            @dragstart="onPaletteDragStart($event, c.type)"
            @click="addItem(c.type)"
            :title="c.desc"
          >
            <el-icon :size="18" class="text-sky-500">
              <component :is="c.icon" />
            </el-icon>
            <span class="font-medium text-[11px]">{{ c.label }}</span>
          </div>
        </div>

        <div class="p-2.5 rounded-xl bg-slate-50 border border-slate-200/60 text-[11px] text-slate-500 leading-relaxed">
          <span class="font-semibold text-slate-700">💡 设计贴士：</span><br />
          <b>评分单选</b>为 Likert 计分题（选项带分值），计入量表总分。<br />
          选中题目后在右侧「数据绑定」可关联检验/体征/用药等 CDR 业务数据，评估时自动带入。
        </div>
      </div>

      <!-- ============ 中间：画布 (Canvas) ============ -->
      <div
        class="flex-1 min-w-0 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4 min-h-[720px]"
        @dragover.prevent
        @drop="onCanvasDrop"
      >
        <div class="flex items-center justify-between pb-3 border-b border-slate-100">
          <div class="flex items-center gap-2">
            <span class="text-sm font-bold text-slate-800">量表画布</span>
            <el-tag size="small" type="info" effect="plain" class="!rounded font-mono">
              共 {{ items.length }} 题 · 计分题 {{ scoreItemCount }}
            </el-tag>
          </div>
          <div class="text-xs font-mono font-semibold text-sky-600">
            {{ (meta.maxScore || autoMaxScore) ? `满分 ${meta.maxScore || autoMaxScore} 分` : '不计分（采集类）' }}
          </div>
        </div>

        <div v-if="!items.length" class="py-24 text-center">
          <el-empty description="从左侧拖入控件，或点击控件添加第一道题目" />
        </div>

        <div class="space-y-3">
          <div
            v-for="(item, idx) in items"
            :key="item.id"
            draggable="true"
            class="group p-4 rounded-xl border transition-all duration-200 cursor-pointer relative select-none"
            :class="selectedId === item.id ? 'border-sky-500 bg-sky-50/30 ring-2 ring-sky-100 shadow-clinical' : 'border-slate-200/80 bg-white hover:border-slate-300 shadow-clinical-sm'"
            @dragstart="onItemDragStart($event, idx)"
            @click.stop="select(item.id)"
          >
            <div class="flex items-center justify-between gap-2 flex-wrap">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-bold text-sky-600 font-mono text-xs">
                  {{ item.type === 'SECTION' || item.type === 'NOTE' ? '—' : `Q${qNo(item, idx)}` }}
                </span>
                <el-tag size="small" :type="typeMeta(item.type).tagType" effect="plain" class="!rounded font-medium">
                  {{ typeMeta(item.type).label }}
                </el-tag>
                <el-tag v-if="item.side" size="small" :type="item.side === 'LEFT' ? 'primary' : 'success'" effect="light" class="!rounded font-mono">
                  {{ item.side === 'LEFT' ? '左侧腔区' : '右侧腔区' }}
                </el-tag>
                <el-tag v-if="isScoreType(item.type)" size="small" type="danger" effect="light" class="!rounded font-mono">
                  计分题
                </el-tag>
                <el-tag v-if="item.binding" size="small" type="warning" effect="plain" class="!rounded font-medium">
                  <el-icon class="mr-0.5"><Connection /></el-icon>
                  {{ item.binding.mode === 'OPTIONS' ? '选项源' : '绑定' }} · {{ BINDING_DOMAIN_META[item.binding.domain]?.label }} · {{ bindingFieldLabel(item.binding.domain, item.binding.field) }}
                </el-tag>
                <span v-if="isScoreType(item.type)" class="text-xs text-slate-400 font-mono">
                  分值 0 ~ {{ itemMax(item) }}
                </span>
                <span v-if="item.required && !['SECTION', 'NOTE'].includes(item.type)" class="text-rose-500 font-bold">*</span>
              </div>

              <!-- 操作按钮组 -->
              <div class="flex items-center gap-1 opacity-80 group-hover:opacity-100">
                <el-button link size="small" :disabled="idx === 0" @click.stop="move(idx, -1)">
                  <el-icon><ArrowUp /></el-icon>
                </el-button>
                <el-button link size="small" :disabled="idx === items.length - 1" @click.stop="move(idx, 1)">
                  <el-icon><ArrowDown /></el-icon>
                </el-button>
                <el-button link type="primary" size="small" @click.stop="duplicate(idx)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
                <el-button link type="danger" size="small" @click.stop="remove(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <div class="mt-2.5 text-xs font-medium text-slate-800 leading-snug">
              {{ item.text || '（未设置题干内容）' }}
            </div>

            <!-- 选项预览卡片 -->
            <div v-if="hasOptions(item)" class="flex flex-wrap gap-1.5 mt-2.5">
              <el-tag
                v-for="o in item.options"
                :key="o.value"
                size="small"
                type="info"
                effect="plain"
                class="!rounded !text-[11px]"
              >
                {{ o.label }} <span v-if="isScoreType(item.type)" class="text-sky-600 font-mono font-semibold ml-0.5">({{ o.value }}分)</span>
              </el-tag>
            </div>
            <div v-else-if="item.type === 'NUMBER'" class="text-xs text-slate-400 font-mono mt-2">
              连续数值输入：0 ~ {{ item.max || 10 }}
            </div>
            <div v-else-if="item.type === 'INPUT'" class="text-xs text-slate-400 mt-2">
              文本填空输入框
            </div>
          </div>
        </div>

        <div v-if="items.length" class="text-center text-xs text-slate-400 pt-3">
          提示：长按并拖动画布中的题目卡片可快速调整前后排列顺序
        </div>
      </div>

      <!-- ============ 右侧：属性面板 (Inspector) ============ -->
      <div class="w-full lg:w-80 flex-shrink-0 bg-white rounded-2xl border border-slate-200/80 p-4 shadow-clinical-sm space-y-4">
        <el-tabs v-model="inspectorTab" class="w-full">
          <el-tab-pane label="题目属性" name="item" :disabled="!selected">
            <template v-if="selected">
              <el-form label-position="top" size="small" class="space-y-3 pt-2">
                <el-form-item label="题干文本" required>
                  <el-input
                    v-model="selected.text"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入题目题干，如：鼻塞程度或面部胀痛"
                  />
                </el-form-item>

                <div class="flex items-center justify-between py-1">
                  <span class="text-xs text-slate-600">是否必答</span>
                  <el-switch v-model="selected.required" />
                </div>

                <el-form-item v-if="isScoreType(selected.type)" label="解剖侧别（双侧分栏评估）">
                  <el-radio-group v-model="selectedSide" size="small">
                    <el-radio-button :value="null">无侧别</el-radio-button>
                    <el-radio-button value="LEFT">左侧</el-radio-button>
                    <el-radio-button value="RIGHT">右侧</el-radio-button>
                  </el-radio-group>
                </el-form-item>

                <template v-if="hasOptions(selected)">
                  <el-form-item :label="`选项列表（${isScoreType(selected.type) ? '选项含分值，计入总分' : '无分值选项'}）`">
                    <div class="space-y-2 w-full">
                      <div
                        v-for="(o, i) in selected.options"
                        :key="i"
                        class="flex items-center gap-1.5 w-full"
                      >
                        <el-input-number
                          v-if="isScoreType(selected.type)"
                          v-model="o.value"
                          :min="0"
                          size="small"
                          class="!w-20"
                        />
                        <el-input
                          v-model="o.label"
                          size="small"
                          class="flex-1"
                          :placeholder="`选项 ${i + 1}`"
                        />
                        <el-button
                          link
                          type="danger"
                          size="small"
                          :disabled="selected.options.length <= 2"
                          @click="selected.options.splice(i, 1)"
                        >
                          <el-icon><Close /></el-icon>
                        </el-button>
                      </div>
                      <el-button
                        size="small"
                        type="primary"
                        plain
                        class="w-full !rounded-lg"
                        @click="addOption(selected)"
                      >
                        <el-icon class="mr-1"><Plus /></el-icon>
                        添加选项
                      </el-button>
                    </div>
                  </el-form-item>
                </template>

                <el-form-item v-if="selected.type === 'NUMBER'" label="数值上限 (Max)">
                  <el-input-number v-model="selected.max" :min="1" size="small" class="w-full" />
                </el-form-item>

                <!-- 数据绑定：评估时从 CDR 业务数据带入 -->
                <template v-if="!['SECTION', 'NOTE'].includes(selected.type)">
                  <div class="pt-3 border-t border-slate-100">
                    <div class="flex items-center justify-between mb-3">
                      <span class="text-xs font-bold text-slate-800 flex items-center gap-1">
                        <el-icon class="text-indigo-500"><Connection /></el-icon>
                        CDR 业务数据自动带入
                      </span>
                      <el-switch :model-value="!!selected.binding" @change="toggleBinding" />
                    </div>

                    <template v-if="selected.binding">
                      <div class="space-y-3 p-3 rounded-xl bg-slate-50 border border-slate-200/80">
                        <el-form-item label="业务域" required>
                          <el-select
                            v-model="selected.binding.domain"
                            size="small"
                            class="w-full"
                            @change="onDomainChange"
                          >
                            <el-option
                              v-for="d in domainOptions"
                              :key="d.value"
                              :label="d.label"
                              :value="d.value"
                            />
                          </el-select>
                        </el-form-item>

                        <el-form-item label="绑定字段" required>
                          <el-select
                            v-model="selected.binding.field"
                            size="small"
                            filterable
                            class="w-full"
                          >
                            <el-option
                              v-for="f in fieldOptions"
                              :key="f.value"
                              :label="f.label"
                              :value="f.value"
                            />
                          </el-select>
                        </el-form-item>

                        <el-form-item label="带入模式">
                          <el-radio-group v-model="selected.binding.mode" size="small">
                            <el-radio-button value="AUTO_READONLY">只读快照</el-radio-button>
                            <el-radio-button value="AUTO_EDITABLE">带入可改</el-radio-button>
                            <el-radio-button value="OPTIONS">动态选项源</el-radio-button>
                          </el-radio-group>
                        </el-form-item>

                        <el-form-item label="有效时间窗（天，空为不限）">
                          <el-input-number
                            v-model="selected.binding.windowDays"
                            :min="1"
                            size="small"
                            placeholder="如 30 天内"
                            class="w-full"
                          />
                        </el-form-item>

                        <div v-if="selected.binding.mode === 'OPTIONS'" class="text-[11px] text-indigo-700 bg-indigo-50 p-2 rounded-lg">
                          评估时按该患者{{ BINDING_DOMAIN_META[selected.binding.domain]?.label }}记录动态枚举选项，画布中的静态选项将被替代。
                        </div>
                      </div>
                    </template>
                  </div>
                </template>
              </el-form>
            </template>
            <div v-else class="py-20 text-center text-xs text-slate-400">
              请点击画布中的题目卡片以配置其属性
            </div>
          </el-tab-pane>

          <!-- 量表全局元数据配置 -->
          <el-tab-pane label="量表设置" name="scale">
            <el-form label-position="top" size="small" class="space-y-3 pt-2">
              <el-form-item label="量表全称" required>
                <el-input v-model="meta.name" placeholder="如：SNOT-22 鼻窦炎结局测试量表" />
              </el-form-item>

              <el-form-item label="量表唯一编码" required>
                <el-input v-model="meta.scaleCode" placeholder="如：SNOT22" />
              </el-form-item>

              <el-form-item :label="`理论满分（当前计分题自动累计 = ${autoMaxScore}）`">
                <el-input-number v-model="meta.maxScore" size="small" class="w-full" />
              </el-form-item>

              <el-form-item label="MCID（最小临床重要差异值）">
                <el-input-number
                  v-model="meta.mcid"
                  :min="0"
                  size="small"
                  class="w-full"
                  placeholder="如 9 分（代表治疗具有临床意义）"
                />
              </el-form-item>

              <el-form-item label="临床预后解读区间">
                <div class="space-y-2 w-full">
                  <div
                    v-for="(r, i) in meta.interpretation"
                    :key="i"
                    class="flex items-center gap-1.5 w-full"
                  >
                    <el-input-number v-model="r.min" size="small" placeholder="min" class="!w-16" />
                    <span class="text-slate-400 text-xs">~</span>
                    <el-input-number v-model="r.max" size="small" placeholder="max" class="!w-16" />
                    <el-input v-model="r.label" size="small" class="flex-1" placeholder="如轻微/中度" />
                    <el-button link type="danger" size="small" @click="meta.interpretation.splice(i, 1)">
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                  <el-button
                    size="small"
                    type="primary"
                    plain
                    class="w-full !rounded-lg"
                    @click="meta.interpretation.push({ min: 0, max: 0, label: '' })"
                  >
                    <el-icon class="mr-1"><Plus /></el-icon>
                    添加解读区间
                  </el-button>
                </div>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 预览弹窗：复用量表填写组件渲染画布内容 -->
    <el-dialog
      v-model="previewOpen"
      title="量表预览（受试者与临床填写视角）"
      width="780px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <ScaleFillPanel scale-code="PREVIEW" :definition="previewDefinition" required />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  View,
  Upload,
  CircleCheck,
  Select,
  Document,
  Operation,
  EditPen,
  Reading,
  Connection,
  ArrowUp,
  ArrowDown,
  CopyDocument,
  Delete,
  Plus,
  Close,
} from '@element-plus/icons-vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import { getScaleLatest, publishScaleNewVersion, createScale } from '@/api/followup'

const BINDING_CATALOG = [
  {
    domain: 'LAB',
    fields: [
      { code: 'TIGE', label: '血清总IgE', unit: 'kU/L' },
      { code: 'EOS_PCT', label: '嗜酸性粒细胞比例', unit: '%' },
      { code: 'CRP', label: 'C反应蛋白', unit: 'mg/L' },
    ],
  },
  {
    domain: 'VITAL',
    fields: [
      { code: 'WEIGHT', label: '体重', unit: 'kg' },
      { code: 'HEIGHT', label: '身高', unit: 'cm' },
      { code: 'BMI', label: 'BMI', unit: 'kg/m²' },
      { code: 'BP_SYSTOLIC', label: '收缩压', unit: 'mmHg' },
    ],
  },
  {
    domain: 'MEDICATION',
    fields: [
      { code: 'ACTIVE_MEDS', label: '当前在用药物（枚举）' },
      { code: 'NASAL_STEROID', label: '在用鼻用激素（枚举）' },
    ],
  },
  {
    domain: 'DIAGNOSIS',
    fields: [
      { code: 'ACTIVE_DX', label: '现有诊断（枚举）' },
      { code: 'ALLERGY_DX', label: '过敏相关诊断（枚举）' },
    ],
  },
  {
    domain: 'IMAGING',
    fields: [
      { code: 'RECENT_CT', label: '近一次鼻窦CT报告' },
    ],
  },
]

const BINDING_DOMAIN_META: Record<string, { label: string; tagType: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  LAB: { label: '检验', tagType: 'primary' },
  VITAL: { label: '体征', tagType: 'success' },
  MEDICATION: { label: '用药', tagType: 'warning' },
  DIAGNOSIS: { label: '诊断', tagType: 'info' },
  IMAGING: { label: '影像', tagType: 'primary' },
}

function bindingFieldLabel(domain: string, field: string): string {
  return (BINDING_CATALOG as any[]).find((d: any) => d.domain === domain)?.fields.find((f: any) => f.code === field)?.label || field
}

type BindingDomain = 'LAB' | 'VITAL' | 'MEDICATION' | 'DIAGNOSIS' | 'IMAGING'
interface ScaleBinding { domain: BindingDomain; field: string; mode: 'AUTO_READONLY' | 'AUTO_EDITABLE' | 'OPTIONS'; windowDays?: number | null }

const route = useRoute()
const router = useRouter()
const publishing = ref(false)

/* 控件库 */
const controls = [
  { type: 'SCORE_RADIO', label: '评分单选', icon: CircleCheck, desc: 'Likert 计分单选（如 0-5 严重程度），计入总分' },
  { type: 'RADIO', label: '单选题', icon: Select, desc: '不计分单选' },
  { type: 'CHECKBOX', label: '多选题', icon: Operation, desc: '不计分多选' },
  { type: 'SELECT', label: '下拉题', icon: Reading, desc: '不计分下拉' },
  { type: 'NUMBER', label: '数字评分', icon: EditPen, desc: '0~N 数字输入（NRS 疼痛评分等）' },
  { type: 'INPUT', label: '填空题', icon: Document, desc: '文本填空' },
  { type: 'SECTION', label: '分组标题', icon: Connection, desc: '量表分区标题' },
  { type: 'NOTE', label: '说明文字', icon: Reading, desc: '填表说明' },
] as const

const TYPE_META: Record<string, { label: string; tagType: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  SCORE_RADIO: { label: '评分单选', tagType: 'danger' },
  RADIO: { label: '单选题', tagType: 'primary' },
  CHECKBOX: { label: '多选题', tagType: 'warning' },
  SELECT: { label: '下拉题', tagType: 'info' },
  NUMBER: { label: '数字评分', tagType: 'warning' },
  INPUT: { label: '填空题', tagType: 'info' },
  SECTION: { label: '分组标题', tagType: 'primary' },
  NOTE: { label: '说明文字', tagType: 'info' },
}

interface Opt { value: number | string; label: string }
interface DesignItem {
  id: string
  type: string
  text: string
  side: 'LEFT' | 'RIGHT' | null
  required: boolean
  options: Opt[]
  max?: number
  binding: ScaleBinding | null
}

let seq = 1
const uid = () => `q${seq++}`

/* 状态 */
const meta = reactive({
  name: '',
  scaleCode: '',
  version: 1,
  maxScore: 0 as number | null,
  mcid: undefined as number | undefined,
  interpretation: [] as { min: number; max: number; label: string }[],
})
const items = ref<DesignItem[]>([])

/* 初始化：带 scaleCode 参数则从后端载入既有量表定义 */
watch(() => route.query.scaleCode, load, { immediate: true })

async function load() {
  const code = String(route.query.scaleCode || '')
  if (!code) return
  try {
    const res = await getScaleLatest(code)
    const editing = res.data?.data
    if (!editing) return
    meta.name = editing.name || ''
    meta.scaleCode = editing.scaleCode || code
    meta.version = (editing.version || 0) + 1
    const def = typeof editing.definition === 'string'
      ? JSON.parse(editing.definition || '{}')
      : (editing.definition || {})
    meta.maxScore = def.maxScore ?? 0
    meta.mcid = def.mcid ?? undefined
    meta.interpretation = (def.interpretation || []).map((r: any) => ({ ...r }))
    items.value = (def.items || []).map((i: any) => ({
      id: uid(),
      type: i.type || 'SCORE_RADIO',
      text: i.text || i.no,
      side: i.side || null,
      required: i.required !== false,
      options: (i.options || []).map((o: any) => ({ value: o.value, label: o.label })),
      max: i.max,
      binding: i.binding ? { ...i.binding } : null,
    }))
  } catch {
    ElMessage.error('量表定义加载失败')
  }
}

const isScoreType = (t: string) => t === 'SCORE_RADIO' || t === 'NUMBER'
const hasOptions = (item: DesignItem) => ['SCORE_RADIO', 'RADIO', 'CHECKBOX', 'SELECT'].includes(item.type)
const typeMeta = (t: string) => TYPE_META[t] || { label: t, tagType: 'info' }
const itemMax = (item: DesignItem) =>
  item.type === 'NUMBER' ? (item.max || 10) : Math.max(0, ...item.options.map((o) => Number(o.value)))

const scoreItemCount = computed(() => items.value.filter((i) => isScoreType(i.type)).length)
const autoMaxScore = computed(() => items.value.filter((i) => isScoreType(i.type)).reduce((s, i) => s + itemMax(i), 0))
function qNo(item: DesignItem, idx: number) {
  const qs = items.value.filter((i) => !['SECTION', 'NOTE'].includes(i.type))
  return qs.indexOf(item) >= 0 ? qs.indexOf(item) + 1 : idx + 1
}

/* 选中与编辑 */
const selectedId = ref<string | null>(null)
const inspectorTab = ref('item')
const selected = computed(() => items.value.find((i) => i.id === selectedId.value) || null)
const selectedSide = computed({
  get: () => selected.value?.side ?? null,
  set: (v) => {
    if (selected.value) selected.value.side = (v as any) || null
  },
})
function select(id: string) {
  selectedId.value = id
  inspectorTab.value = 'item'
}

/* 增删移动复制 */
function defaultOptions(type: string): Opt[] {
  if (type === 'SCORE_RADIO') {
    return [
      { value: 0, label: '没有' },
      { value: 1, label: '轻微' },
      { value: 2, label: '轻度' },
      { value: 3, label: '中度' },
      { value: 4, label: '重度' },
      { value: 5, label: '非常严重' },
    ]
  }
  return [
    { value: 0, label: '选项一' },
    { value: 0, label: '选项二' },
  ]
}

function addItem(type: string) {
  const item: DesignItem = {
    id: uid(),
    type,
    text: '',
    side: null,
    required: type !== 'SECTION' && type !== 'NOTE',
    options: hasOptions({ type } as any) ? defaultOptions(type) : [],
    max: type === 'NUMBER' ? 10 : undefined,
    binding: null,
  }
  items.value.push(item)
  select(item.id)
}

function remove(idx: number) {
  if (selectedId.value === items.value[idx].id) selectedId.value = null
  items.value.splice(idx, 1)
}

function duplicate(idx: number) {
  const copy = JSON.parse(JSON.stringify(items.value[idx]))
  copy.id = uid()
  items.value.splice(idx + 1, 0, copy)
  select(copy.id)
}

function move(idx: number, dir: -1 | 1) {
  const [it] = items.value.splice(idx, 1)
  items.value.splice(idx + dir, 0, it)
}

function addOption(item: DesignItem) {
  const nextVal = isScoreType(item.type) ? itemMax(item) + 1 : 0
  item.options.push({ value: nextVal, label: `选项 ${item.options.length + 1}` })
}

/* 数据绑定配置 */
const domainOptions = Object.entries(BINDING_DOMAIN_META).map(([value, m]) => ({ value, label: m.label }))
const fieldOptions = computed(() => {
  const d = (BINDING_CATALOG as any[]).find((c: any) => c.domain === selected.value?.binding?.domain)
  return (d?.fields || []).map((f: any) => ({ value: f.code, label: f.unit ? `${f.label}（${f.unit}）` : f.label }))
})

function toggleBinding(on: any) {
  if (!selected.value) return
  selected.value.binding = on
    ? { domain: 'LAB', field: 'TIGE', mode: 'AUTO_READONLY', windowDays: null }
    : null
}

function onDomainChange(domain: BindingDomain) {
  const first = (BINDING_CATALOG as any[]).find((c: any) => c.domain === domain)?.fields[0]?.code
  if (selected.value?.binding) selected.value.binding.field = first || ''
}

/* 拖拽 */
let dragType: string | null = null
let dragIdx = -1
function onPaletteDragStart(e: DragEvent, type: string) {
  dragType = type
  e.dataTransfer?.setData('text/plain', `palette:${type}`)
}

function onItemDragStart(e: DragEvent, idx: number) {
  dragType = null
  dragIdx = idx
  e.dataTransfer?.setData('text/plain', `item:${idx}`)
}

function onCanvasDrop(e: DragEvent) {
  const data = e.dataTransfer?.getData('text/plain') || ''
  if (data.startsWith('palette:')) {
    addItem(data.slice(8))
  } else if (data.startsWith('item:') && dragIdx >= 0) {
    const target = items.value.splice(dragIdx, 1)[0]
    items.value.push(target)
    dragIdx = -1
  }
}

/* 预览：由画布构造 definition，复用填写组件 */
const previewOpen = ref(false)
const previewDefinition = computed(() => ({
  name: meta.name || '预览量表',
  type: 'MIXED',
  items: items.value
    .filter((i) => i.type !== 'SECTION' && i.type !== 'NOTE')
    .map((i, idx) => ({
      no: String(idx + 1),
      text: i.text || `题目 ${idx + 1}`,
      type: i.type,
      max: i.max,
      side: i.side || undefined,
      options: i.options,
      binding: i.binding,
    })),
  maxScore: meta.maxScore || autoMaxScore.value,
  mcid: meta.mcid ?? null,
  interpretation: meta.interpretation,
}))

async function handlePublish() {
  if (!meta.name || !meta.scaleCode) {
    ElMessage.warning('请先在「量表设置」中填写名称与编码')
    inspectorTab.value = 'scale'
    return
  }
  if (!items.value.length) {
    ElMessage.warning('画布为空，请先添加题目')
    return
  }
  const bad = items.value.find((i) => i.type !== 'NOTE' && i.type !== 'SECTION' && !i.text?.trim())
  if (bad) {
    ElMessage.warning('存在未设置题干的题目')
    select(bad.id)
    return
  }
  publishing.value = true
  try {
    const definition = { ...previewDefinition.value, name: meta.name }
    const isNew = !String(route.query.scaleCode || '')
    if (isNew) {
      await createScale({ scaleCode: meta.scaleCode, name: meta.name, definition })
    } else {
      await publishScaleNewVersion(meta.scaleCode, { name: meta.name, definition })
    }
    ElMessage.success(`已发布 ${meta.scaleCode} v${meta.version}（满分 ${meta.maxScore || autoMaxScore.value}${meta.mcid ? ' · MCID ' + meta.mcid : ''}）`)
    router.push({ name: 'ScaleManagement' })
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}
</script>
