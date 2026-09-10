<template>
  <PageContainer :title="`量表设计器 · ${meta.name || '新建量表'} · 页面原型`" :breadcrumb="[{ title: '原型' }, { title: '量表设计器' }]">
    <template #extra>
      <a-button @click="$router.push({ name: 'ProtoCrsScales' })">返回列表</a-button>
      <a-button @click="previewOpen = true">预览</a-button>
      <a-button>保存草稿</a-button>
      <a-button type="primary" @click="handlePublish">发布新版本</a-button>
    </template>

    <div class="designer">
      <!-- ============ 左：控件库 ============ -->
      <div class="palette">
        <div class="pane-title">控件库</div>
        <div class="palette-grid">
          <div v-for="c in controls" :key="c.type" class="palette-item" draggable="true"
            @dragstart="onPaletteDragStart($event, c.type)" @click="addItem(c.type)" :title="c.desc">
            <component :is="c.icon" />
            <span>{{ c.label }}</span>
          </div>
        </div>
        <div class="palette-tip">
          拖拽或点击控件添加到画布。<b>评分单选</b>为 Likert 计分题（选项带分值），计入量表总分。<br />
          选中题目后在右侧「数据绑定」可关联检验/体征/用药等 CDR 业务数据，评估时自动带入。
        </div>
      </div>

      <!-- ============ 中：画布 ============ -->
      <div class="canvas" @dragover.prevent @drop="onCanvasDrop">
        <div class="pane-title">
          量表画布（{{ items.length }} 题 · 计分题 {{ scoreItemCount }} · 满分 {{ autoMaxScore }}）
        </div>

        <div v-if="!items.length" class="canvas-empty">
          <a-empty description="从左侧拖入控件，或点击控件添加第一道题" />
        </div>

        <div v-for="(item, idx) in items" :key="item.id" class="q-card"
          :class="{ selected: selectedId === item.id }" draggable="true"
          @dragstart="onItemDragStart($event, idx)" @click.stop="select(item.id)">
          <div class="q-head">
            <span class="q-no">{{ item.type === 'SECTION' || item.type === 'NOTE' ? '—' : `Q${qNo(item, idx)}` }}</span>
            <a-tag :color="typeMeta(item.type).color" size="small">{{ typeMeta(item.type).label }}</a-tag>
            <a-tag v-if="item.side" size="small" :color="item.side === 'LEFT' ? 'cyan' : 'geekblue'">
              {{ item.side === 'LEFT' ? '左侧' : '右侧' }}
            </a-tag>
            <a-tag v-if="isScoreType(item.type)" color="red" size="small">计分</a-tag>
            <a-tag v-if="item.binding" color="purple" size="small">
              {{ item.binding.mode === 'OPTIONS' ? '选项源' : '绑定' }}·{{ BINDING_DOMAIN_META[item.binding.domain].label }}·{{ bindingFieldLabel(item.binding.domain, item.binding.field) }}
            </a-tag>
            <span class="q-score" v-if="isScoreType(item.type)">分值 0~{{ itemMax(item) }}</span>
            <span class="q-required" v-if="item.required && !['SECTION', 'NOTE'].includes(item.type)">*</span>
            <div class="q-ops">
              <a-button size="small" type="text" :disabled="idx === 0" @click.stop="move(idx, -1)">↑</a-button>
              <a-button size="small" type="text" :disabled="idx === items.length - 1" @click.stop="move(idx, 1)">↓</a-button>
              <a-button size="small" type="text" @click.stop="duplicate(idx)">复制</a-button>
              <a-button size="small" type="text" danger @click.stop="remove(idx)">删除</a-button>
            </div>
          </div>
          <div class="q-text">{{ item.text || '（未设置题干）' }}</div>
          <div v-if="hasOptions(item)" class="q-options">
            <a-tag v-for="o in item.options" :key="o.value" class="q-opt">
              {{ o.label }}<span class="q-opt-val">{{ o.value }}分</span>
            </a-tag>
          </div>
          <div v-else-if="item.type === 'NUMBER'" class="q-options dim">0 ~ {{ item.max || 10 }} 数字输入</div>
          <div v-else-if="item.type === 'INPUT'" class="q-options dim">文本填空</div>
        </div>

        <div v-if="items.length" class="canvas-foot dim">拖动画布中的题目可调整顺序</div>
      </div>

      <!-- ============ 右：属性面板 ============ -->
      <div class="inspector">
        <a-tabs v-model:activeKey="inspectorTab" size="small">
          <a-tab-pane key="item" tab="题目属性" :disabled="!selected">
            <template v-if="selected">
              <a-form layout="vertical" size="small">
                <a-form-item label="题干" required>
                  <a-textarea v-model:value="selected.text" :rows="2" placeholder="请输入题目内容" />
                </a-form-item>
                <a-form-item label="必答">
                  <a-switch v-model:checked="selected.required" size="small" />
                </a-form-item>
                <a-form-item v-if="isScoreType(selected.type)" label="侧别（双侧量表用）">
                  <a-radio-group v-model:value="selectedSide" size="small" button-style="solid">
                    <a-radio-button :value="null">无</a-radio-button>
                    <a-radio-button value="LEFT">左侧</a-radio-button>
                    <a-radio-button value="RIGHT">右侧</a-radio-button>
                  </a-radio-group>
                </a-form-item>

                <template v-if="hasOptions(selected)">
                  <a-form-item :label="`选项（${isScoreType(selected.type) ? '含分值，计入总分' : '不计分'}）`">
                    <div v-for="(o, i) in selected.options" :key="i" class="opt-row">
                      <a-input-number v-if="isScoreType(selected.type)" v-model:value="o.value" :min="0" size="small" style="width: 64px" />
                      <a-input v-model:value="o.label" size="small" style="flex: 1" :placeholder="`选项 ${i + 1}`" />
                      <a-button size="small" type="text" danger :disabled="selected.options.length <= 2"
                        @click="selected.options.splice(i, 1)">×</a-button>
                    </div>
                    <a-button size="small" type="dashed" block style="margin-top: 6px"
                      @click="addOption(selected)">+ 添加选项</a-button>
                  </a-form-item>
                </template>

                <a-form-item v-if="selected.type === 'NUMBER'" label="最大值">
                  <a-input-number v-model:value="selected.max" :min="1" size="small" style="width: 100%" />
                </a-form-item>

                <!-- 数据绑定：评估时从 CDR 业务数据带入 -->
                <template v-if="!['SECTION', 'NOTE'].includes(selected.type)">
                  <a-divider class="bind-divider">数据绑定（评估时从 CDR 业务数据带入）</a-divider>
                  <a-form-item label="启用绑定">
                    <a-switch :checked="!!selected.binding" size="small" @change="toggleBinding" />
                  </a-form-item>
                  <template v-if="selected.binding">
                    <a-form-item label="业务域" required>
                      <a-select v-model:value="selected.binding.domain" size="small" :options="domainOptions" @change="onDomainChange" />
                    </a-form-item>
                    <a-form-item label="字段" required>
                      <a-select v-model:value="selected.binding.field" size="small" :options="fieldOptions" show-search />
                    </a-form-item>
                    <a-form-item label="带入方式">
                      <a-radio-group v-model:value="selected.binding.mode" size="small" button-style="solid">
                        <a-radio-button value="AUTO_READONLY">自动带入·只读</a-radio-button>
                        <a-radio-button value="AUTO_EDITABLE">自动带入·可修改</a-radio-button>
                        <a-radio-button value="OPTIONS">选项数据源</a-radio-button>
                      </a-radio-group>
                    </a-form-item>
                    <a-form-item label="取值时间窗（天，空 = 不限）">
                      <a-input-number v-model:value="selected.binding.windowDays" :min="1" size="small" placeholder="不限" style="width: 100%" />
                    </a-form-item>
                    <div v-if="selected.binding.mode === 'OPTIONS'" class="dim bind-tip">
                      评估时按该患者{{ BINDING_DOMAIN_META[selected.binding.domain].label }}记录动态枚举选项，画布中的静态选项将被忽略
                    </div>
                  </template>
                </template>
              </a-form>
            </template>
            <div v-else class="inspector-empty dim">点击画布中的题目编辑属性</div>
          </a-tab-pane>

          <a-tab-pane key="scale" tab="量表设置">
            <a-form layout="vertical" size="small">
              <a-form-item label="量表名称" required>
                <a-input v-model:value="meta.name" placeholder="如 SNOT-22 鼻窦炎结局测试" />
              </a-form-item>
              <a-form-item label="量表编码" required>
                <a-input v-model:value="meta.scaleCode" placeholder="如 SNOT22" />
              </a-form-item>
              <a-form-item :label="`满分（计分题自动求和 = ${autoMaxScore}）`">
                <a-input-number v-model:value="meta.maxScore" size="small" style="width: 100%" />
              </a-form-item>
              <a-form-item label="MCID（最小临床重要差异）">
                <a-input-number v-model:value="meta.mcid" :min="0" size="small" style="width: 100%" placeholder="可空" />
              </a-form-item>
              <a-form-item label="解读区间">
                <div v-for="(r, i) in meta.interpretation" :key="i" class="opt-row">
                  <a-input-number v-model:value="r.min" size="small" placeholder="min" style="width: 76px" />
                  <span class="dim">~</span>
                  <a-input-number v-model:value="r.max" size="small" placeholder="max" style="width: 76px" />
                  <a-input v-model:value="r.label" size="small" style="flex: 1" placeholder="解读" />
                  <a-button size="small" type="text" danger @click="meta.interpretation.splice(i, 1)">×</a-button>
                </div>
                <a-button size="small" type="dashed" block style="margin-top: 6px"
                  @click="meta.interpretation.push({ min: 0, max: 0, label: '' })">+ 添加区间</a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>

    <!-- 预览：复用量表填写组件渲染画布内容 -->
    <a-modal v-model:open="previewOpen" title="量表预览（受试者视角）" width="760" footer="null">
      <ScaleFillPanel scale-code="PREVIEW" :preview-definition="previewDefinition" required />
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined, CheckSquareOutlined, DownCircleOutlined,
  AlignLeftOutlined, NumberOutlined, FontColorsOutlined,
  TagsOutlined, FileTextOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import { scaleByCode, BINDING_CATALOG, BINDING_DOMAIN_META, bindingFieldLabel } from './mock'
import type { ScaleBinding, BindingDomain } from './mock'

const route = useRoute()

/* 控件库 */
const controls = [
  { type: 'SCORE_RADIO', label: '评分单选', icon: CheckCircleOutlined, desc: 'Likert 计分单选（如 0-5 严重程度），计入总分' },
  { type: 'RADIO', label: '单选题', icon: DownCircleOutlined, desc: '不计分单选' },
  { type: 'CHECKBOX', label: '多选题', icon: CheckSquareOutlined, desc: '不计分多选' },
  { type: 'SELECT', label: '下拉题', icon: AlignLeftOutlined, desc: '不计分下拉' },
  { type: 'NUMBER', label: '数字评分', icon: NumberOutlined, desc: '0~N 数字输入（NRS 疼痛评分等）' },
  { type: 'INPUT', label: '填空题', icon: FontColorsOutlined, desc: '文本填空' },
  { type: 'SECTION', label: '分组标题', icon: TagsOutlined, desc: '量表分区标题' },
  { type: 'NOTE', label: '说明文字', icon: FileTextOutlined, desc: '填表说明' },
] as const

const TYPE_META: Record<string, { label: string; color: string }> = {
  SCORE_RADIO: { label: '评分单选', color: 'red' },
  RADIO: { label: '单选题', color: 'blue' },
  CHECKBOX: { label: '多选题', color: 'purple' },
  SELECT: { label: '下拉题', color: 'cyan' },
  NUMBER: { label: '数字评分', color: 'orange' },
  INPUT: { label: '填空题', color: 'default' },
  SECTION: { label: '分组标题', color: 'geekblue' },
  NOTE: { label: '说明文字', color: 'default' },
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

/* 初始化：带 scaleCode 参数则载入既有量表 definition */
const editing = scaleByCode(String(route.query.scaleCode || ''))
const meta = reactive({
  name: editing?.name || '',
  scaleCode: editing?.scaleCode || '',
  version: (editing?.version || 0) + 1,
  maxScore: editing?.definition.maxScore ?? 0,
  mcid: editing?.definition.mcid ?? undefined,
  interpretation: (editing?.definition.interpretation || []).map((r: any) => ({ ...r })),
})

const items = ref<DesignItem[]>(
  (editing?.definition.items || [{ no: '1', text: '', options: [{ value: 0, label: '选项一' }, { value: 1, label: '选项二' }] }]).map((i: any) => ({
    id: uid(),
    type: i.type || 'SCORE_RADIO',
    text: i.text || i.no,
    side: i.side || null,
    required: i.required !== false,
    options: (i.options || []).map((o: any) => ({ value: o.value, label: o.label })),
    max: i.max,
    binding: i.binding ? { ...i.binding } : null,
  })),
)

const isScoreType = (t: string) => t === 'SCORE_RADIO' || t === 'NUMBER'
const hasOptions = (item: DesignItem) => ['SCORE_RADIO', 'RADIO', 'CHECKBOX', 'SELECT'].includes(item.type)
const typeMeta = (t: string) => TYPE_META[t] || { label: t, color: 'default' }
const itemMax = (item: DesignItem) =>
  item.type === 'NUMBER' ? (item.max || 10) : Math.max(0, ...item.options.map(o => o.value))

const scoreItemCount = computed(() => items.value.filter(i => isScoreType(i.type)).length)
const autoMaxScore = computed(() => items.value.filter(i => isScoreType(i.type)).reduce((s, i) => s + itemMax(i), 0))
function qNo(item: DesignItem, idx: number) {
  const qs = items.value.filter(i => !['SECTION', 'NOTE'].includes(i.type))
  return qs.indexOf(item) >= 0 ? qs.indexOf(item) + 1 : idx + 1
}

/* 选中与编辑 */
const selectedId = ref<string | null>(null)
const inspectorTab = ref('item')
const selected = computed(() => items.value.find(i => i.id === selectedId.value) || null)
const selectedSide = computed({
  get: () => selected.value?.side ?? null,
  set: (v) => { if (selected.value) selected.value.side = (v as any) || null },
})
function select(id: string) {
  selectedId.value = id
  inspectorTab.value = 'item'
}

/* 增删移动复制 */
function defaultOptions(type: string): Opt[] {
  if (type === 'SCORE_RADIO') {
    return [{ value: 0, label: '没有' }, { value: 1, label: '轻微' }, { value: 2, label: '轻度' },
      { value: 3, label: '中度' }, { value: 4, label: '重度' }, { value: 5, label: '非常严重' }]
  }
  return [{ value: 0, label: '选项一' }, { value: 0, label: '选项二' }]
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
  const d = BINDING_CATALOG.find(c => c.domain === selected.value?.binding?.domain)
  return (d?.fields || []).map(f => ({ value: f.code, label: f.unit ? `${f.label}（${f.unit}）` : f.label }))
})
function toggleBinding(on: any) {
  if (!selected.value) return
  selected.value.binding = on
    ? { domain: 'LAB', field: 'TIGE', mode: 'AUTO_READONLY', windowDays: null }
    : null
}
function onDomainChange(domain: BindingDomain) {
  const first = BINDING_CATALOG.find(c => c.domain === domain)?.fields[0]?.code
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
  type: 'MIXED',
  items: items.value
    .filter(i => i.type !== 'SECTION' && i.type !== 'NOTE')
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

function handlePublish() {
  if (!meta.name || !meta.scaleCode) { message.warning('请先在「量表设置」中填写名称与编码'); inspectorTab.value = 'scale'; return }
  if (!items.value.length) { message.warning('画布为空，请先添加题目'); return }
  const bad = items.value.find(i => i.type !== 'NOTE' && i.type !== 'SECTION' && !i.text?.trim())
  if (bad) { message.warning('存在未设置题干的题目'); select(bad.id); return }
  message.success(`已发布 ${meta.scaleCode} v${meta.version}（满分 ${meta.maxScore || autoMaxScore.value}${meta.mcid ? ' · MCID ' + meta.mcid : ''}）`)
}
</script>

<style scoped>
.designer { display: flex; gap: 12px; min-height: 720px; }
.pane-title { font-weight: 600; margin-bottom: 10px; font-size: 13px; }

.palette { width: 200px; flex-shrink: 0; }
.palette-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.palette-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 4px; border: 1px solid #e5e5e5; border-radius: 6px; background: #fff;
  font-size: 12px; cursor: grab; user-select: none; transition: all .15s;
}
.palette-item:hover { border-color: #1677ff; color: #1677ff; box-shadow: 0 1px 4px rgba(22,119,255,.15); }
.palette-tip { margin-top: 12px; font-size: 12px; color: #999; line-height: 1.6; }

.canvas { flex: 1; min-width: 0; background: #fafafa; border: 1px dashed #d9d9d9; border-radius: 6px; padding: 14px; }
.canvas-empty { padding: 80px 0; }
.q-card {
  background: #fff; border: 1px solid #e5e5e5; border-radius: 6px;
  padding: 10px 12px; margin-bottom: 10px; cursor: grab; transition: all .15s;
}
.q-card:hover { border-color: #91caff; }
.q-card.selected { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22,119,255,.15); }
.q-head { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.q-no { font-weight: 700; color: #1677ff; font-size: 13px; }
.q-score { color: #999; font-size: 12px; }
.q-required { color: #cf1322; font-weight: 700; }
.q-ops { margin-left: auto; display: flex; gap: 0; }
.q-text { margin: 6px 0 4px; font-size: 13px; }
.q-options { display: flex; flex-wrap: wrap; gap: 4px; }
.q-opt { font-size: 12px; }
.q-opt-val { color: #999; margin-left: 2px; }
.canvas-foot { text-align: center; font-size: 12px; padding: 6px 0; }

.inspector { width: 320px; flex-shrink: 0; background: #fff; border: 1px solid #f0f0f0; border-radius: 6px; padding: 10px; }
.inspector-empty { text-align: center; padding: 60px 0; }
.opt-row { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.dim { color: #999; font-size: 12px; }
.bind-divider { margin: 14px 0 10px; font-size: 12px; color: #722ed1; }
.bind-tip { background: #f9f0ff; border-radius: 4px; padding: 6px 8px; line-height: 1.6; }
</style>
