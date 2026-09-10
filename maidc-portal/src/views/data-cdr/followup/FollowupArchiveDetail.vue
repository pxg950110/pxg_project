<template>
  <PageContainer :title="`随访档案 · ${detail?.followup?.patientName || ''}`" :breadcrumb="[{ title: '数据管理' }, { title: '随访档案' }]">
    <template #extra>
      <a-button type="primary" ghost @click="goPatient360">患者 360 视图</a-button>
      <a-button @click="$router.back()">返回</a-button>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <!-- 头部：患者 + 档案状态 + 操作 -->
        <a-card size="small" style="margin-bottom: 16px">
          <a-space :size="24" wrap>
            <span style="font-size: 16px"><b>{{ detail.followup.patientName }}</b>（{{ detail.followup.patientGender }}/{{ detail.followup.patientAge }}岁）</span>
            <a-tag :color="STATUS_META[detail.followup.status]?.color" style="font-size: 13px; padding: 2px 10px">
              档案：{{ STATUS_META[detail.followup.status]?.label }}
            </a-tag>
            <span class="dim">建档 {{ detail.followup.enrollDate }} · 方案 v{{ detail.followup.protocolVersion }}（快照）</span>
            <span class="dim">医生：{{ detail.followup.doctorName || '-' }} · 护士：{{ detail.followup.nurseName || '-' }}</span>
          </a-space>
          <template #actions>
            <a-space wrap>
              <a-button v-if="canManage && detail.followup.status === 'ACTIVE'" size="small" @click="handleSuspend">暂停随访</a-button>
              <a-button v-if="canManage && detail.followup.status === 'SUSPENDED'" size="small" type="primary" ghost @click="handleResume">恢复随访</a-button>
              <a-button v-if="canManage && !['CLOSED', 'OUT_OF_COHORT'].includes(detail.followup.status)" size="small" danger ghost @click="closeOpen = true">结案</a-button>
              <a-button v-if="canManage && latestVersion > detail.followup.protocolVersion" size="small" @click="upgradeOpen = true">
                升级方案 → v{{ latestVersion }}
              </a-button>
              <a-tag v-else color="default">方案已是最新</a-tag>
              <a-button v-if="canManage" size="small" @click="planFreeOpen = true">计划外评估</a-button>
            </a-space>
          </template>
        </a-card>

        <a-row :gutter="16">
          <!-- 左：任务时间轴 -->
          <a-col :span="8">
            <a-card title="任务时间轴" size="small">
              <a-timeline style="margin-top: 8px">
                <a-timeline-item v-for="t in detail.tasks" :key="t.id" :color="timelineColor(t)">
                  <template #dot v-if="t.status === 'OVERDUE'">
                    <WarningOutlined style="font-size: 16px; color: #cf1322" />
                  </template>
                  <div class="tl-item">
                    <div class="tl-head">
                      <b>{{ t.stageName }}</b>
                      <a-tag :color="STATUS_META[t.status]?.color" size="small">{{ STATUS_META[t.status]?.label }}</a-tag>
                    </div>
                    <div class="dim">应完成：{{ t.dueDate }}</div>
                    <div class="dim">量表：<span v-for="c in t.requiredScales" :key="c" class="scale-chip">{{ shortScale(c) }}</span></div>
                    <div v-if="t.status === 'OVERDUE'" style="color: #cf1322">已超期 {{ t.overdueDays }} 天</div>
                    <div v-if="t.status === 'PENDING'" style="margin-top: 4px">
                      <a-button size="small" type="primary" @click="goWorkbench">去完成</a-button>
                    </div>
                  </div>
                </a-timeline-item>
              </a-timeline>
            </a-card>
          </a-col>

          <!-- 右侧 -->
          <a-col :span="16">
            <!-- 评估曲线 -->
            <a-card size="small" style="margin-bottom: 16px">
              <template #title>评估曲线</template>
              <template #extra>
                <a-radio-group v-model:value="chartScale" size="small" button-style="solid">
                  <a-radio-button v-for="c in assessedCodes" :key="c" :value="c">{{ shortScale(c) }}</a-radio-button>
                </a-radio-group>
              </template>
              <a-empty v-if="!assessedCodes.length" description="暂无评估记录" />
              <MetricChart v-else :option="chartOption" :height="260" />
            </a-card>

            <!-- 治疗记录 -->
            <a-card size="small" style="margin-bottom: 16px">
              <template #title>治疗记录（{{ detail.treatments.length }}）</template>
              <template #extra>
                <a-space>
                  <a-button size="small" @click="treatOpen = true">+ 手工添加</a-button>
                  <a-button size="small" type="primary" ghost @click="openCdrImport">从 CDR 带入</a-button>
                </a-space>
              </template>
              <a-empty v-if="!detail.treatments.length" description="暂无治疗记录" />
              <div v-for="t in detail.treatments" :key="t.id" class="treat-card">
                <div class="treat-head">
                  <a-tag :color="t.category === 'SURGERY' ? 'red' : t.category === 'MEDICATION' ? 'orange' : 'default'">
                    {{ categoryLabel(t.category) }}
                  </a-tag>
                  <b>{{ t.name }}</b>
                  <a-tag size="small">{{ t.source === 'CDR' ? 'CDR 带入（只读快照）' : '手工录入' }}</a-tag>
                  <span class="dim" style="margin-left: auto">{{ t.occurredDate }}</span>
                </div>
                <div v-if="parsedDetail(t).length" class="dim" style="margin-top: 4px">
                  <span v-for="[k, v] in parsedDetail(t)" :key="k" class="detail-chip">{{ detailLabel(k) }}：{{ v ?? '-' }}</span>
                </div>
              </div>
            </a-card>

            <!-- 方案快照 -->
            <a-card size="small" title="随访方案快照">
              <span class="dim">档案绑定方案 v{{ detail.followup.protocolVersion }}；方案升级不影响本档案，除非医生主动执行「升级方案」。</span>
            </a-card>
          </a-col>
        </a-row>
      </template>
    </a-spin>

    <!-- 结案弹窗 -->
    <a-modal v-model:open="closeOpen" title="结案" ok-text="确认结案" :confirm-loading="acting" @ok="handleClose">
      <a-form layout="vertical">
        <a-form-item label="结案原因（必填）" required>
          <a-textarea v-model:value="closeReason" :maxlength="512" show-count placeholder="如：治愈完成随访 / 患者退出" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 升级方案确认 -->
    <a-modal v-model:open="upgradeOpen" title="升级方案" ok-text="确认升级" :confirm-loading="acting" @ok="handleUpgrade">
      <a-alert type="warning" show-icon
        :message="`将切换到 v${latestVersion}：仅重新生成未来未完成任务（PENDING 且到期日 ≥ 今天）；已完成 / 已跳过任务保持不变`" />
    </a-modal>

    <!-- 计划外评估 -->
    <a-modal v-model:open="planFreeOpen" title="发起计划外评估（医生）" width="720" ok-text="提交评估" :confirm-loading="acting" @ok="handlePlanFree">
      <a-form layout="vertical">
        <a-form-item label="量表">
          <a-select v-model:value="planFreeScale" :options="scaleOptions" style="width: 240px" />
        </a-form-item>
      </a-form>
      <ScaleFillPanel v-if="planFreeScale && detail" :scale-code="planFreeScale" :patient-id="detail.followup.patientId"
        @change="onPlanFreeChange" />
    </a-modal>

    <!-- 手工添加治疗 -->
    <a-modal v-model:open="treatOpen" title="手工添加治疗记录" ok-text="添加" :confirm-loading="acting" @ok="handleAddTreatment">
      <a-form layout="vertical">
        <a-form-item label="类别">
          <a-radio-group v-model:value="treatForm.category">
            <a-radio-button value="MEDICATION">药物</a-radio-button>
            <a-radio-button value="SURGERY">手术</a-radio-button>
            <a-radio-button value="OTHER">其他</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="名称" required><a-input v-model:value="treatForm.name" /></a-form-item>
        <a-form-item label="发生日期" required><a-date-picker v-model:value="treatForm.date" style="width: 100%" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- CDR 带入 -->
    <a-modal v-model:open="cdrOpen" title="从 CDR 带入治疗记录（只读快照）" width="680" ok-text="带入所选" :confirm-loading="acting" @ok="handleCdrImport">
      <a-alert type="info" show-icon style="margin-bottom: 12px"
        message="重复判定：同名 + 同发生日期的 CDR 记录已存在时自动跳过并列出" />
      <a-empty v-if="!cdrCandidates.length" description="近 180 天无 CDR 治疗记录" />
      <a-table v-else :data-source="cdrCandidates" :columns="cdrColumns" size="small" :pagination="false"
        :row-selection="{ selectedRowKeys, onChange: (keys: any[]) => (selectedRowKeys = keys), getCheckboxProps: (r: any) => ({ disabled: r.exists }) }"
        row-key="recordId">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'category'">
            <a-tag :color="record.category === 'MEDICATION' ? 'orange' : 'default'">{{ categoryLabel(record.category) }}</a-tag>
          </template>
          <template v-if="column.key === 'exists'">
            <a-tag v-if="record.exists" color="warning">已存在，跳过</a-tag>
            <a-tag v-else color="green">可带入</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { WarningOutlined } from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import {
  getFollowupDetail, getProtocols, closeFollowup, suspendFollowup, resumeFollowup,
  upgradeFollowupProtocol, createPlanFreeAssessment, createTreatment,
  getCdrTreatmentCandidates, importCdrTreatments, getScales,
} from '@/api/followup'
import { usePermissionStore } from '@/stores/permission'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const authStore = useAuthStore()
const canManage = permissionStore.hasPermission('disease:followup:manage')

const fid = computed(() => Number(route.params.fid))
const loading = ref(false)
const acting = ref(false)
const detail = ref<any>(null)
const latestVersion = ref(1)

const STATUS_META: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: '随访中', color: 'processing' },
  SUSPENDED: { label: '已暂停', color: 'warning' },
  CLOSED: { label: '已结案', color: 'default' },
  OUT_OF_COHORT: { label: '已脱组', color: 'error' },
  DONE: { label: '已完成', color: 'success' },
  SKIPPED: { label: '已跳过', color: 'default' },
  OVERDUE: { label: '已超期', color: 'error' },
  PENDING: { label: '待办', color: 'processing' },
}

const scaleNames = ref<Record<string, string>>({})
const shortScale = (code: string) => scaleNames.value[code]?.split(' ')[0] || code
const scaleOptions = computed(() =>
  Object.keys(scaleNames.value).map(c => ({ value: c, label: shortScale(c) })))
const categoryLabel = (c: string) => ({ MEDICATION: '药物', SURGERY: '手术', OTHER: '其他' } as any)[c]
const detailLabel = (k: string) => ({ dosage: '剂量', route: '途径', startDate: '开始', endDate: '结束', surgeon: '术者', hospitalDays: '住院天数', side: '侧别' } as any)[k] || k

const timelineColor = (t: any) => (STATUS_META[t.status]?.color === 'error' ? 'red' : { success: 'green', default: 'gray', processing: 'blue', warning: 'orange' } as any)[STATUS_META[t.status]?.color] || 'blue'

function parsedDetail(t: any): [string, any][] {
  if (!t.detail) return []
  try { return Object.entries(typeof t.detail === 'string' ? JSON.parse(t.detail) : t.detail) } catch { return [] }
}

async function load() {
  loading.value = true
  try {
    const [dRes, sRes] = await Promise.all([getFollowupDetail(fid.value), getScales()])
    detail.value = dRes.data?.data
    const map: Record<string, string> = {}
    const scaleList: any[] = sRes.data?.data || []
    scaleList.forEach(x => { map[x.scaleCode] = x.name })
    scaleNames.value = map
  } catch { message.error('档案详情加载失败') }
  finally { loading.value = false }
}

onMounted(async () => {
  load()
  // 最新方案版本（升级按钮可见性）
  try {
    const cohortId = route.query.cohortId
    if (cohortId) {
      const res = await getProtocols(cohortId as string)
      const versions = (res.data?.data || []).filter((p: any) => p.status === 'PUBLISHED').map((p: any) => p.version)
      latestVersion.value = versions.length ? Math.max(...versions) : 1
    }
  } catch { latestVersion.value = 1 }
})

/* 评估曲线 */
const assessedCodes = computed<string[]>(() => Array.from(new Set<string>((detail.value?.assessments || []).map((a: any) => a.scaleCode))))
const chartScale = ref('')
const chartOption = computed(() => {
  const list = (detail.value?.assessments || []).filter((a: any) => a.scaleCode === chartScale.value)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 40, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: list.map((a: any) => `${a.stageName || ''}\n${(a.assessedAt || '').slice(0, 10)}`) },
    yAxis: { type: 'value', name: '得分' },
    series: [{
      type: 'line', data: list.map((a: any) => a.totalScore), smooth: true, symbolSize: 9,
      lineStyle: { width: 3 }, label: { show: true }, areaStyle: { opacity: 0.1 },
    }],
  }
})
watch(assessedCodes, codes => { if (codes.length && !chartScale.value) chartScale.value = codes[0] as string })

/* 档案操作 */
const closeOpen = ref(false)
const closeReason = ref('')
async function handleClose() {
  if (!closeReason.value.trim()) { message.warning('请填写结案原因'); return }
  acting.value = true
  try {
    await closeFollowup(fid.value, closeReason.value.trim())
    message.success('档案已结案')
    closeOpen.value = false
    load()
  } catch (e: any) { message.error(e.response?.data?.message || '结案失败') }
  finally { acting.value = false }
}

async function handleSuspend() {
  try { await suspendFollowup(fid.value); message.success('已暂停（任务冻结）'); load() }
  catch (e: any) { message.error(e.response?.data?.message || '操作失败') }
}
async function handleResume() {
  try { await resumeFollowup(fid.value); message.success('已恢复'); load() }
  catch (e: any) { message.error(e.response?.data?.message || '操作失败') }
}

const upgradeOpen = ref(false)
async function handleUpgrade() {
  acting.value = true
  try {
    const res = await upgradeFollowupProtocol(fid.value)
    const d = res.data?.data
    message.success(`已升级到 v${d?.protocolVersion}：重建 ${d?.rebuiltTasks} 个未来任务`)
    upgradeOpen.value = false
    load()
  } catch (e: any) { message.error(e.response?.data?.message || '升级失败') }
  finally { acting.value = false }
}

/* 计划外评估 */
const planFreeOpen = ref(false)
const planFreeScale = ref('')
const planFreeAnswers = ref<Record<string, any> | null>(null)
function onPlanFreeChange(p: { answers: Record<string, any>; complete: boolean }) { planFreeAnswers.value = p.answers }
async function handlePlanFree() {
  if (!planFreeScale.value) { message.warning('请选择量表'); return }
  if (!planFreeAnswers.value) { message.warning('请先完成量表填写'); return }
  acting.value = true
  try {
    await createPlanFreeAssessment(fid.value, { scaleCode: planFreeScale.value, answers: planFreeAnswers.value }, authStore.userInfo?.id)
    message.success('计划外评估已提交')
    planFreeOpen.value = false
    planFreeAnswers.value = null
    load()
  } catch (e: any) { message.error(e.response?.data?.message || '提交失败') }
  finally { acting.value = false }
}

/* 治疗记录 */
const treatOpen = ref(false)
const treatForm = ref<{ category: string; name: string; date: Dayjs | null }>({ category: 'MEDICATION', name: '', date: dayjs() })
async function handleAddTreatment() {
  if (!treatForm.value.name.trim()) { message.warning('请填写治疗名称'); return }
  acting.value = true
  try {
    await createTreatment(fid.value, {
      category: treatForm.value.category,
      name: treatForm.value.name.trim(),
      occurredDate: treatForm.value.date ? treatForm.value.date.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
    })
    message.success('治疗记录已添加')
    treatOpen.value = false
    treatForm.value = { category: 'MEDICATION', name: '', date: dayjs() }
    load()
  } catch (e: any) { message.error(e.response?.data?.message || '添加失败') }
  finally { acting.value = false }
}

/* CDR 带入 */
const cdrOpen = ref(false)
const cdrCandidates = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const cdrColumns = [
  { title: '来源类型', dataIndex: 'resourceType', width: 160 },
  { title: '名称', dataIndex: 'name' },
  { title: '类别', key: 'category', width: 80 },
  { title: '发生日期', dataIndex: 'occurredDate', width: 110 },
  { title: '状态', key: 'exists', width: 120 },
]
async function openCdrImport() {
  try {
    const res = await getCdrTreatmentCandidates(fid.value)
    cdrCandidates.value = res.data?.data || []
    selectedRowKeys.value = cdrCandidates.value.filter(c => !c.exists).map(c => c.recordId)
    cdrOpen.value = true
  } catch { message.error('CDR 候选加载失败') }
}
async function handleCdrImport() {
  if (!selectedRowKeys.value.length) { message.warning('请勾选要带入的记录'); return }
  acting.value = true
  try {
    const items = cdrCandidates.value.filter(c => selectedRowKeys.value.includes(c.recordId))
    const res = await importCdrTreatments(fid.value, items)
    const d = res.data?.data
    message.success(`已带入 ${d?.imported} 条${d?.skipped?.length ? `，跳过重复 ${d.skipped.length} 条` : ''}`)
    cdrOpen.value = false
    load()
  } catch (e: any) { message.error(e.response?.data?.message || '带入失败') }
  finally { acting.value = false }
}

function goWorkbench() { router.push({ name: 'FollowupWorkbench' }) }
function goPatient360() {
  if (detail.value?.followup?.patientId) {
    router.push({ name: 'PatientEncounter360', params: { patientId: detail.value.followup.patientId } })
  }
}
</script>

<style scoped>
.dim { color: #999; font-size: 12px; }
.tl-item { padding-bottom: 8px; }
.tl-head { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.scale-chip { background: #f5f5f5; border-radius: 3px; padding: 0 4px; margin-right: 4px; }
.treat-card { border: 1px solid #f0f0f0; border-radius: 6px; padding: 10px 12px; margin-bottom: 8px; }
.treat-head { display: flex; align-items: center; gap: 8px; }
.detail-chip { background: #fafafa; border-radius: 3px; padding: 1px 6px; margin-right: 8px; }
</style>
