<template>
  <PageContainer title="随访工作台 · 页面原型" :breadcrumb="[{ title: '原型' }, { title: '随访工作台' }]">
    <!-- 筛选区 -->
    <a-card size="small" style="margin-bottom: 16px">
      <a-space :size="16">
        <span class="filter-label">专病：</span>
        <a-select v-model:value="cohortSel" style="width: 200px" :options="[{ value: 'CRS', label: '慢性鼻窦炎（J32）· 4 个在管档案' }]" />
        <span class="filter-label">我的视角：</span>
        <a-radio-group v-model:value="mineOnly" size="small">
          <a-radio-button :value="true">我负责的</a-radio-button>
          <a-radio-button :value="false">全部</a-radio-button>
        </a-radio-group>
        <a-input-search v-model:value="keyword" placeholder="搜索患者姓名 / 阶段" style="width: 220px" allow-clear />
      </a-space>
    </a-card>

    <!-- 统计卡 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6"><a-card><a-statistic title="今日到期" :value="todayDue.length" :value-style="{ color: '#1677ff' }" /></a-card></a-col>
      <a-col :span="6"><a-card><a-statistic title="已超期" :value="overdue.length" :value-style="{ color: '#cf1322' }" /></a-card></a-col>
      <a-col :span="6"><a-card><a-statistic title="未来 7 天" :value="upcoming.length" /></a-card></a-col>
      <a-col :span="6"><a-card><a-statistic title="本周完成" :value="9" :value-style="{ color: '#389e0d' }" /></a-card></a-col>
    </a-row>

    <!-- 任务分组 -->
    <a-card>
      <a-tabs v-model:activeKey="group">
        <a-tab-pane key="today">
          <template #tab>今日到期 <a-badge :count="todayDue.length" size="small" style="margin-left: 4px" /></template>
        </a-tab-pane>
        <a-tab-pane key="overdue">
          <template #tab><span style="color: #cf1322">已超期</span> <a-badge :count="overdue.length" size="small" style="margin-left: 4px" color="error" /></template>
        </a-tab-pane>
        <a-tab-pane key="upcoming">
          <template #tab>未来 7 天 <a-badge :count="upcoming.length" size="small" style="margin-left: 4px" /></template>
        </a-tab-pane>
      </a-tabs>

      <a-alert v-if="group === 'overdue'" type="error" show-icon style="margin-bottom: 12px"
        message="超期 > 7 天的任务已升级通知负责医生（每日定时扫描）" />

      <a-table :columns="columns" :data-source="currentRows" row-key="taskId" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'patient'">
            <a class="patient-link" @click="goArchive(record.followupId)">{{ record.patientName }}</a>
            <span class="dim">（{{ record.gender }}/{{ record.age }}）</span>
          </template>
          <template v-if="column.key === 'stage'">
            <a-tag>{{ record.stageName }}</a-tag>
          </template>
          <template v-if="column.key === 'requiredScales'">
            <a-tag v-for="c in record.requiredScales" :key="c" color="red" style="margin-bottom: 2px">{{ shortScale(c) }}</a-tag>
          </template>
          <template v-if="column.key === 'dueDate'">
            <template v-if="record.overdueDays > 0">
              <span style="color: #cf1322; font-weight: 600">{{ record.dueDate }}</span>
              <a-tag color="error" size="small" style="margin-left: 4px">超期 {{ record.overdueDays }} 天</a-tag>
              <a-tooltip v-if="record.overdueDays > 7" title="已升级通知负责医生">
                <WarningOutlined style="color: #faad14; margin-left: 4px" />
              </a-tooltip>
            </template>
            <span v-else>{{ record.dueDate }}</span>
          </template>
          <template v-if="column.key === 'nurse'">
            {{ record.nurseName }}
            <a-tag v-if="record.nurseName === '刘敏'" color="blue" size="small" style="margin-left: 4px">我</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="primary" size="small" @click="openTask(record)">开始随访</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- ============ 任务执行抽屉 ============ -->
    <a-drawer v-model:open="drawerOpen" width="760" :title="`执行随访 · ${currentRow?.patientName || ''}`"
      :body-style="{ paddingBottom: '80px' }">
      <template v-if="currentRow">
        <!-- 患者摘要 -->
        <a-card size="small" style="margin-bottom: 16px">
          <a-space :size="24">
            <span><b>{{ currentRow.patientName }}</b>（{{ currentRow.gender }}/{{ currentRow.age }}岁）</span>
            <span class="dim">专病：慢性鼻窦炎</span>
            <span class="dim">医生：{{ currentRow.doctorName }} · 护士：{{ currentRow.nurseName }}</span>
            <a-tag color="processing">随访中</a-tag>
          </a-space>
        </a-card>
        <a-alert type="info" show-icon style="margin-bottom: 16px"
          :message="`任务：${currentRow.stageName} · 应评 ${currentRow.requiredScales.map(shortScale).join(' + ')} · 到期 ${currentRow.dueDate}`" />

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

        <!-- 底部操作 -->
        <div class="drawer-footer">
          <a-space>
            <a-button danger ghost @click="skipOpen = true">跳过任务（医生）</a-button>
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { WarningOutlined } from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import PageContainer from '@/components/PageContainer/index.vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import { workbenchRows, scales } from './mock'
import type { WorkbenchRow } from './mock'

const router = useRouter()
const cohortSel = ref('CRS')
const mineOnly = ref(true)
const keyword = ref('')
const group = ref('today')

const shortScale = (code: string) => scales.find(s => s.scaleCode === code)?.name.split(' ')[0] || code
const categoryLabel = (c: string) => ({ MEDICATION: '药物', SURGERY: '手术', OTHER: '其他' } as any)[c]

const myRows = computed(() =>
  mineOnly.value ? workbenchRows.filter(r => r.nurseName === '刘敏') : workbenchRows)
const searched = computed(() =>
  keyword.value ? myRows.value.filter(r => r.patientName.includes(keyword.value) || r.stageName.includes(keyword.value)) : myRows.value)
const todayDue = computed(() => searched.value.filter(r => r.dueDate === '2026-09-09'))
const overdue = computed(() => searched.value.filter(r => r.overdueDays > 0 && r.dueDate !== '2026-09-09'))
const upcoming = computed(() => searched.value.filter(r => r.dueDate > '2026-09-09' && r.dueDate <= '2026-09-16'))
const currentRows = computed(() => ({ today: todayDue.value, overdue: overdue.value, upcoming: upcoming.value } as any)[group.value] || [])

const columns = [
  { title: '患者', key: 'patient', width: 150 },
  { title: '阶段', key: 'stage', width: 100 },
  { title: '应评量表', key: 'requiredScales' },
  { title: '到期日', key: 'dueDate', width: 220 },
  { title: '负责护士', key: 'nurse', width: 110 },
  { title: '操作', key: 'action', width: 100 },
]

/* 抽屉 */
const drawerOpen = ref(false)
const currentRow = ref<WorkbenchRow | null>(null)
const scaleResults = ref<Record<string, { totalScore: number; complete: boolean }>>({})
const submitting = ref(false)
const addedTreatments = ref<{ category: string; name: string; occurredDate: string }[]>([])
const treatmentOpen = ref(false)
const skipOpen = ref(false)
const skipReason = ref('')

const treatForm = ref<{ category: string; name: string; date: Dayjs | null }>({ category: 'MEDICATION', name: '', date: dayjs() })

/* 必评 + 选评合并渲染；完成度只按必评判定 */
const allScaleCodes = computed(() =>
  currentRow.value ? [...currentRow.value.requiredScales, ...(currentRow.value.optionalScales || [])] : [])

function openTask(row: WorkbenchRow) {
  currentRow.value = row
  scaleResults.value = {}
  addedTreatments.value = []
  drawerOpen.value = true
}
function onScaleChange(p: { scaleCode: string; totalScore: number; complete: boolean }) {
  scaleResults.value[p.scaleCode] = { totalScore: p.totalScore, complete: p.complete }
}
const completeCount = computed(() => Object.values(scaleResults.value).filter(r => r.complete).length)
const allScalesComplete = computed(() =>
  !!currentRow.value && currentRow.value.requiredScales.every(c => scaleResults.value[c]?.complete))

function handleSubmit() {
  submitting.value = true
  setTimeout(() => {
    submitting.value = false
    drawerOpen.value = false
    message.success('随访已提交：量表评估 + 治疗记录在同一事务写入')
  }, 800)
}
function handleSkip() {
  if (!skipReason.value.trim()) { message.warning('请填写跳过原因'); return }
  skipOpen.value = false
  drawerOpen.value = false
  skipReason.value = ''
  message.success('任务已跳过（SKIPPED）')
}
function addTreatment() {
  if (!treatForm.value.name || !treatForm.value.date) { message.warning('请填写名称与日期'); return }
  addedTreatments.value.push({
    category: treatForm.value.category,
    name: treatForm.value.name,
    occurredDate: treatForm.value.date.format('YYYY-MM-DD'),
  })
  treatmentOpen.value = false
  treatForm.value = { category: 'MEDICATION', name: '', date: dayjs() }
}
function goArchive(fid: number) {
  router.push({ name: 'ProtoCrsArchive', params: { fid } })
}
</script>

<style scoped>
.filter-label { color: #666; }
.dim { color: #999; }
.patient-link { color: #1677ff; }
.treat-line { padding: 6px 0; border-bottom: 1px dashed #eee; }
.drawer-footer {
  position: absolute; right: 0; bottom: 0; left: 0;
  padding: 12px 24px; background: #fff; border-top: 1px solid #f0f0f0; text-align: right;
}
</style>
