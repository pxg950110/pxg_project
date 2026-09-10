<template>
  <PageContainer title="随访工作台" :breadcrumb="[{ title: '随访工作台' }]">
    <template #extra>
      <a-radio-group v-model:value="mineOnly" button-style="solid" @change="load">
        <a-radio-button :value="true">我负责的</a-radio-button>
        <a-radio-button :value="false">全部</a-radio-button>
      </a-radio-group>
      <a-input-search v-model:value="keyword" placeholder="搜索患者" style="width: 220px" allow-clear />
    </template>

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="8">
        <a-badge :count="groups.today.length" :offset="[8, 0]">
          <a-card :class="['stat-card', { active: group === 'today' }]" @click="group = 'today'">
            <a-statistic title="今日到期" :value="groups.today.length" :value-style="{ color: '#1677ff' }" />
          </a-card>
        </a-badge>
      </a-col>
      <a-col :span="8">
        <a-badge :count="groups.overdue.length" color="#cf1322" :offset="[8, 0]">
          <a-card :class="['stat-card', { active: group === 'overdue' }]" @click="group = 'overdue'">
            <a-statistic title="已超期" :value="groups.overdue.length" :value-style="{ color: '#cf1322' }" />
          </a-card>
        </a-badge>
      </a-col>
      <a-col :span="8">
        <a-card :class="['stat-card', { active: group === 'upcoming' }]" @click="group = 'upcoming'">
          <a-statistic title="未来 7 天" :value="groups.upcoming.length" />
        </a-card>
      </a-col>
    </a-row>

    <a-card>
      <a-table :columns="columns" :data-source="currentRows" :loading="loading" row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'patient'">
            <b>{{ record.patientName }}</b>
            <span v-if="record.gender" class="dim">（{{ record.gender }}/{{ record.age }}岁）</span>
          </template>
          <template v-if="column.key === 'requiredScales'">
            <a-tag v-for="c in record.requiredScales" :key="c" color="red" class="scale-tag">{{ shortScale(c) }}</a-tag>
            <a-tag v-for="c in record.optionalScales || []" :key="'o' + c" class="scale-tag">{{ shortScale(c) }}</a-tag>
          </template>
          <template v-if="column.key === 'dueDate'">
            <span :class="{ overdue: record.status === 'OVERDUE' }">{{ record.dueDate }}</span>
            <a-tag v-if="record.status === 'OVERDUE'" color="red" class="ml8">超期 {{ record.overdueDays }} 天</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="primary" size="small" @click="openTask(record)">开始随访</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 执行抽屉 -->
    <a-drawer v-model:open="drawerOpen" :title="`执行随访 · ${currentRow?.patientName || ''}`" width="720" destroy-on-close>
      <template v-if="currentRow">
        <a-card size="small" style="margin-bottom: 16px">
          <a-space :size="24">
            <span><b>{{ currentRow.patientName }}</b><span v-if="currentRow.gender">（{{ currentRow.gender }}/{{ currentRow.age }}岁）</span></span>
            <span class="dim">阶段：{{ currentRow.stageName }}</span>
            <a-tag v-if="currentRow.status === 'OVERDUE'" color="red">超期 {{ currentRow.overdueDays }} 天</a-tag>
            <a-tag v-else color="processing">进行中</a-tag>
          </a-space>
        </a-card>
        <a-alert type="info" show-icon style="margin-bottom: 16px"
          :message="`任务：${currentRow.stageName} · 必评 ${currentRow.requiredScales.map(shortScale).join(' + ')} · 到期 ${currentRow.dueDate}`" />

        <!-- 量表填写：必评 + 选评（选评不拦截提交） -->
        <ScaleFillPanel v-for="code in allScaleCodes" :key="code" :scale-code="code"
          :required="currentRow.requiredScales.includes(code)" :patient-id="currentRow.patientId"
          @change="onScaleChange" />

        <!-- 治疗记录（可选） -->
        <a-card size="small">
          <template #title>治疗记录（可选）</template>
          <template #extra><a-button size="small" @click="treatmentOpen = true">+ 添加治疗记录</a-button></template>
          <a-empty v-if="!addedTreatments.length" :image-style="{ height: '40px' }" description="本次随访未记录治疗" />
          <div v-for="(t, i) in addedTreatments" :key="i" class="treat-line">
            <a-tag :color="t.category === 'SURGERY' ? 'red' : t.category === 'MEDICATION' ? 'orange' : 'default'">
              {{ categoryLabel(t.category) }}
            </a-tag>
            {{ t.name }} · {{ t.occurredDate }}
            <a style="margin-left: 8px" @click="addedTreatments.splice(i, 1)">移除</a>
          </div>
        </a-card>

        <div class="drawer-footer">
          <a-space>
            <a-button v-if="hasPermission('disease:followup:manage')" danger ghost @click="skipOpen = true">跳过任务（医生）</a-button>
            <a-button @click="drawerOpen = false">取消</a-button>
            <a-button type="primary" :disabled="!allScalesComplete" :loading="submitting" @click="handleSubmit">
              提交随访（量表 {{ completeCount }}/{{ currentRow.requiredScales.length }}）
            </a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>

    <!-- 跳过原因 -->
    <a-modal v-model:open="skipOpen" title="跳过随访任务（仅医生）" ok-text="确认跳过" @ok="handleSkip">
      <a-form layout="vertical">
        <a-form-item label="跳过原因（必填）" required>
          <a-textarea v-model:value="skipReason" :maxlength="512" show-count placeholder="例如：患者住院期间由病房完成评估" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 添加治疗记录 -->
    <a-modal v-model:open="treatmentOpen" title="添加治疗记录" ok-text="添加" @ok="addTreatment">
      <a-form layout="vertical">
        <a-form-item label="类别" required>
          <a-radio-group v-model:value="treatForm.category">
            <a-radio-button value="MEDICATION">药物</a-radio-button>
            <a-radio-button value="SURGERY">手术</a-radio-button>
            <a-radio-button value="OTHER">其他</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="名称" required>
          <a-input v-model:value="treatForm.name" placeholder="如：鼻用糠酸莫米松 / ESS 手术" />
        </a-form-item>
        <a-form-item label="发生日期" required>
          <a-date-picker v-model:value="treatForm.date" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import PageContainer from '@/components/PageContainer/index.vue'
import ScaleFillPanel from '@/views/data-cdr/followup/components/ScaleFillPanel.vue'
import { getMyWorkbench, completeTask, skipTask, getScales } from '@/api/followup'
import { usePermissionStore } from '@/stores/permission'
import { useAuthStore } from '@/stores/auth'

const permissionStore = usePermissionStore()
const authStore = useAuthStore()
const hasPermission = (code: string) => permissionStore.hasPermission(code)

const mineOnly = ref(true)
const keyword = ref('')
const group = ref<'today' | 'overdue' | 'upcoming'>('today')
const loading = ref(false)

interface TaskRow {
  id: number; followupId: number; patientId?: number
  patientName?: string; gender?: string; age?: number
  stageCode: string; stageName: string; dueDate: string
  status: 'PENDING' | 'OVERDUE'; requiredScales: string[]; optionalScales?: string[]
  overdueDays: number
}

const groups = ref<Record<'today' | 'overdue' | 'upcoming', TaskRow[]>>({ today: [], overdue: [], upcoming: [] })
const scaleNames = ref<Record<string, string>>({})
const shortScale = (code: string) => scaleNames.value[code]?.split(' ')[0] || code
const categoryLabel = (c: string) => ({ MEDICATION: '药物', SURGERY: '手术', OTHER: '其他' } as any)[c]

async function load() {
  loading.value = true
  try {
    const res = await getMyWorkbench({ userId: authStore.userInfo?.id, all: !mineOnly.value })
    const data = res.data?.data
    groups.value = {
      today: data?.today || [], overdue: data?.overdue || [], upcoming: data?.upcoming || [],
    }
    if (group.value === 'overdue' && !groups.value.overdue.length) group.value = 'today'
  } catch { message.error('工作台加载失败') }
  finally { loading.value = false }
}

const currentRows = computed(() => {
  const rows = groups.value[group.value] || []
  return keyword.value ? rows.filter(r => (r.patientName || '').includes(keyword.value)) : rows
})

const columns = [
  { title: '患者', key: 'patient', width: 220 },
  { title: '阶段', dataIndex: 'stageName', key: 'stage', width: 110 },
  { title: '应评量表', key: 'requiredScales' },
  { title: '到期日', key: 'dueDate', width: 220 },
  { title: '操作', key: 'action', width: 110 },
]

/* 抽屉 */
const drawerOpen = ref(false)
const currentRow = ref<TaskRow | null>(null)
const scaleResults = ref<Record<string, { complete: boolean }>>({})
const scaleAnswers = ref<Record<string, Record<string, any>>>({})
const submitting = ref(false)
const addedTreatments = ref<{ category: string; name: string; occurredDate: string }[]>([])
const treatmentOpen = ref(false)
const skipOpen = ref(false)
const skipReason = ref('')

const treatForm = ref<{ category: string; name: string; date: Dayjs | null }>({ category: 'MEDICATION', name: '', date: dayjs() })

const allScaleCodes = computed(() =>
  currentRow.value ? [...currentRow.value.requiredScales, ...(currentRow.value.optionalScales || [])] : [])

function openTask(row: TaskRow) {
  currentRow.value = row
  scaleResults.value = {}
  scaleAnswers.value = {}
  addedTreatments.value = []
  drawerOpen.value = true
}

function onScaleChange(p: { scaleCode: string; answers: Record<string, any>; totalScore: number; complete: boolean }) {
  scaleResults.value[p.scaleCode] = { complete: p.complete }
  scaleAnswers.value[p.scaleCode] = p.answers
}

const completeCount = computed(() => currentRow.value
  ? currentRow.value.requiredScales.filter(c => scaleResults.value[c]?.complete).length : 0)
const allScalesComplete = computed(() =>
  !!currentRow.value && currentRow.value.requiredScales.every(c => scaleResults.value[c]?.complete))

async function handleSubmit() {
  if (!currentRow.value) return
  submitting.value = true
  try {
    await completeTask(currentRow.value.id, {
      assessments: Object.keys(scaleAnswers.value).map(code => ({
        scaleCode: code,
        answers: scaleAnswers.value[code],
      })),
      treatments: addedTreatments.value.map(t => ({ ...t })),
    }, authStore.userInfo?.id)
    message.success('随访已提交')
    drawerOpen.value = false
    load()
  } catch (e: any) {
    message.error(e.response?.data?.message || '提交失败')
  } finally { submitting.value = false }
}

async function handleSkip() {
  if (!currentRow.value) return
  if (!skipReason.value.trim()) { message.warning('请填写跳过原因'); return }
  try {
    await skipTask(currentRow.value.id, skipReason.value.trim(), authStore.userInfo?.id)
    message.success('任务已跳过')
    skipOpen.value = false
    drawerOpen.value = false
    load()
  } catch (e: any) {
    message.error(e.response?.data?.message || '跳过失败')
  }
}

function addTreatment() {
  if (!treatForm.value.name.trim()) { message.warning('请填写治疗名称'); return }
  addedTreatments.value.push({
    category: treatForm.value.category,
    name: treatForm.value.name.trim(),
    occurredDate: treatForm.value.date ? treatForm.value.date.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
  })
  treatForm.value = { category: 'MEDICATION', name: '', date: dayjs() }
  treatmentOpen.value = false
}

async function loadScaleNames() {
  try {
    const res = await getScales()
    const map: Record<string, string> = {}
    const scaleList: any[] = res.data?.data || []
    scaleList.forEach(s => { map[s.scaleCode] = s.name })
    scaleNames.value = map
  } catch { /* 名称映射失败显示 code */ }
}

onMounted(() => { load(); loadScaleNames() })
</script>

<style scoped>
.stat-card { cursor: pointer; transition: all .15s; }
.stat-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.09); }
.stat-card.active { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22,119,255,.12); }
.scale-tag { margin-bottom: 2px; }
.ml8 { margin-left: 8px; }
.overdue { color: #cf1322; font-weight: 600; }
.dim { color: #999; font-size: 12px; }
.treat-line { padding: 6px 0; border-bottom: 1px dashed #eee; }
.drawer-footer { margin-top: 16px; text-align: right; }
</style>
