<template>
  <PageContainer :title="cohort.name || '专病详情'" :breadcrumb="breadcrumb">
    <template #extra>
      <a-button type="primary" ghost @click="goKnowledgeBase">
        <template #icon><BookOutlined /></template> 专病知识库
      </a-button>
      <a-button @click="handleSync" :loading="syncing">手动同步</a-button>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <a-tabs v-model:activeKey="activeTab">
      <!-- ============ Tab1 概览 ============ -->
      <a-tab-pane key="overview" tab="概览">
        <a-spin :spinning="loading">
          <a-card title="基本信息" style="margin-bottom: 16px">
            <a-descriptions :column="3" bordered size="small">
              <a-descriptions-item label="专病名称">{{ cohort.name }}</a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="cohort.status === 'ACTIVE' ? 'blue' : 'default'">
                  {{ cohort.status === 'ACTIVE' ? '已启用' : '未启用' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="自动同步">
                <a-switch :checked="cohort.autoSync" @change="toggleAutoSync" />
              </a-descriptions-item>
              <a-descriptions-item label="最后同步">{{ cohort.lastSyncAt || '-' }}</a-descriptions-item>
              <a-descriptions-item label="创建时间">{{ cohort.createdAt }}</a-descriptions-item>
              <a-descriptions-item label="描述" :span="1">{{ cohort.description || '-' }}</a-descriptions-item>
            </a-descriptions>
            <div class="rules-summary" v-if="parsedRules.length">
              <div class="rules-title">纳入规则</div>
              <div v-for="(group, gi) in parsedRules" :key="gi" class="rule-line">
                <a-tag :color="domainColor(group.domain)">{{ domainLabel(group.domain) }}</a-tag>
                <span>{{ groupSummary(group) }}</span>
              </div>
            </div>
          </a-card>

          <a-row :gutter="16">
            <a-col :span="5">
              <a-card><a-statistic title="患者总数" :value="stats.totalPatients || 0" /></a-card>
            </a-col>
            <a-col :span="5">
              <a-card><a-statistic title="在管档案" :value="followupTotal" /></a-card>
            </a-col>
            <a-col :span="5">
              <a-card><a-statistic title="随访依从率" :value="outcome?.complianceRate || 0" suffix="%" :precision="1" /></a-card>
            </a-col>
            <a-col :span="5">
              <a-card><a-statistic title="SNOT-22 改善率" :value="outcome?.improvementRate || 0" suffix="%" :precision="1" /></a-card>
            </a-col>
            <a-col :span="4">
              <a-card><a-statistic title="男性占比" :value="stats.maleRatio || 0" suffix="%" :precision="1" /></a-card>
            </a-col>
          </a-row>
        </a-spin>
      </a-tab-pane>

      <!-- ============ Tab2 患者队列 ============ -->
      <a-tab-pane key="patients" tab="患者队列">
        <a-card>
          <template #extra>
            <a-button @click="handleExport">导出</a-button>
          </template>
          <a-table
            :columns="patientColumns" :data-source="patients"
            :loading="patientsLoading"
            :pagination="{ current: patientPage, pageSize: 20, total: patientTotal }"
            row-key="id" @change="onPatientTableChange" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'matchSource'">
                <a-tag :color="record.matchSource === 'AUTO' ? 'blue' : 'orange'">
                  {{ record.matchSource === 'AUTO' ? '自动' : '手动' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'followState'">
                <a-tag v-if="record.followed" color="processing">已建档</a-tag>
                <a-button v-else-if="canManage" size="small" type="primary" ghost @click="enrollOpen = true">建档</a-button>
                <span v-else class="text-gray">-</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-popconfirm title="确认移除？" @confirm="handleRemovePatient(record.patientId)" v-if="record.matchSource === 'MANUAL'">
                  <a-button type="link" danger size="small">移除</a-button>
                </a-popconfirm>
                <span v-else class="text-gray">-</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- ============ Tab3 随访管理 ============ -->
      <a-tab-pane key="followup" tab="随访管理">
        <!-- 方案卡 -->
        <a-card size="small" style="margin-bottom: 16px">
          <template #title>随访方案</template>
          <template #extra>
            <a-button v-if="canManage" size="small" type="primary" ghost @click="openProtocolModal">发布新版本</a-button>
          </template>
          <a-spin :spinning="protocolLoading">
            <template v-if="latestProtocol">
              <a-space :size="16" wrap style="margin-bottom: 8px">
                <b>{{ latestProtocol.name }}</b>
                <a-tag color="blue">v{{ latestProtocol.version }}</a-tag>
                <a-tag color="green">PUBLISHED</a-tag>
              </a-space>
              <a-steps size="small" :current="latestProtocol.stages.length - 1">
                <a-step v-for="st in latestProtocol.stages" :key="st.stageCode"
                  :title="st.name" :description="`+${st.offsetDays}天 · 必评 ${st.requiredScales.length} 项`" />
              </a-steps>
            </template>
            <a-empty v-else description="暂无随访方案，请联系管理员创建" />
          </a-spin>
        </a-card>

        <!-- 建档列表 -->
        <a-card size="small">
          <template #title>随访档案（{{ followupTotal }}）</template>
          <template #extra>
            <a-select v-model:value="followupStatusFilter" style="width: 130px" allow-clear placeholder="状态筛选" @change="loadFollowups">
              <a-select-option value="ACTIVE">随访中</a-select-option>
              <a-select-option value="SUSPENDED">已暂停</a-select-option>
              <a-select-option value="CLOSED">已结案</a-select-option>
              <a-select-option value="OUT_OF_COHORT">已脱组</a-select-option>
            </a-select>
            <a-button v-if="canManage" type="primary" size="small" style="margin-left: 8px" @click="enrollOpen = true">+ 建档</a-button>
          </template>
          <a-table :columns="followupColumns" :data-source="followups" :loading="followupsLoading"
            row-key="id" size="small"
            :pagination="{ current: followupPage, pageSize: 20, total: followupTotal }"
            @change="(p: any) => { followupPage = p.current; loadFollowups() }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'patient'">
                <a @click="$router.push({ name: 'FollowupArchiveDetail', params: { fid: record.id }, query: { cohortId } })">
                  <b>{{ record.patientName || '患者#' + record.patientId }}</b>
                </a>
                <span v-if="record.patientGender" class="text-gray">（{{ record.patientGender }}/{{ record.patientAge }}岁）</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="FOLLOWUP_STATUS[record.status]?.color">{{ FOLLOWUP_STATUS[record.status]?.label }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-button size="small" type="link" @click="$router.push({ name: 'FollowupArchiveDetail', params: { fid: record.id }, query: { cohortId } })">详情</a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- ============ Tab4 结局看板 ============ -->
      <a-tab-pane key="outcome" tab="结局看板">
        <a-spin :spinning="outcomeLoading">
          <a-row :gutter="16" style="margin-bottom: 16px">
            <a-col :span="6"><a-card><a-statistic title="随访依从率" :value="outcome?.complianceRate || 0" suffix="%" :precision="1" /></a-card></a-col>
            <a-col :span="6"><a-card><a-statistic title="SNOT-22 改善率" :value="outcome?.improvementRate || 0" suffix="%" :precision="1" /></a-card></a-col>
            <a-col :span="6"><a-card><a-statistic title="手术率" :value="outcome?.surgeryRate || 0" suffix="%" :precision="1" /></a-card></a-col>
            <a-col :span="6"><a-card><a-statistic title="再手术患者" :value="outcome?.reoperationCount || 0" suffix=" 人" /></a-card></a-col>
          </a-row>

          <a-card title="量表趋势（按随访阶段均分）" size="small" style="margin-bottom: 16px">
            <a-radio-group v-model:value="trendScale" size="small" button-style="solid" style="margin-bottom: 8px">
              <a-radio-button v-for="(label, code) in TREND_SCALES" :key="code" :value="code">{{ label }}</a-radio-button>
            </a-radio-group>
            <MetricChart :option="trendOption" :height="280" />
          </a-card>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="药物分布（治疗记录聚类）" size="small">
                <MetricChart :option="medicationOption" :height="300" />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="指标说明" size="small">
                <a-descriptions :column="1" size="small">
                  <a-descriptions-item label="依从率">应完成任务（到期 ≤ 今日）中已完成占比</a-descriptions-item>
                  <a-descriptions-item label="改善率">末次评估距基线 ≥ 3 个月且改善 ≥ MCID(9) 的患者占比</a-descriptions-item>
                  <a-descriptions-item label="手术率">有手术记录的患者占在管档案比例</a-descriptions-item>
                  <a-descriptions-item label="再手术">含 ≥ 2 条 ESS 手术记录的患者数</a-descriptions-item>
                </a-descriptions>
              </a-card>
            </a-col>
          </a-row>
        </a-spin>
      </a-tab-pane>
    </a-tabs>

    <!-- 建档弹窗（队列行内 / 随访管理按钮 共用） -->
    <EnrollModal v-model:open="enrollOpen" :cohort-id="cohortId" @created="onEnrolled" />

    <!-- 发布新版本方案 -->
    <a-modal v-model:open="protocolModalOpen" title="发布随访方案新版本" width="680" ok-text="发布" :confirm-loading="publishing" @ok="handlePublishProtocol">
      <a-alert type="warning" show-icon style="margin-bottom: 12px"
        message="发布新版本后，存量档案保持快照不变；医生可在档案详情执行「升级方案」" />
      <a-form layout="vertical">
        <a-form-item label="方案名称" required>
          <a-input v-model:value="protocolForm.name" placeholder="CRS 慢性鼻窦炎随访方案" />
        </a-form-item>
        <a-form-item label="阶段（JSON）" required>
          <a-textarea v-model:value="protocolForm.stagesJson" :rows="8"
            placeholder='[{"stageCode":"BASELINE","name":"基线","offsetDays":0,"requiredScales":["SNOT22"],"optionalScales":[]}]' />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { BookOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import EnrollModal from '@/views/data-cdr/followup/components/EnrollModal.vue'
import {
  getDiseaseCohort, syncDiseaseCohort, getDiseaseCohortPatients,
  removeDiseaseCohortPatient, getDiseaseCohortStatistics, exportDiseaseCohort, updateDiseaseCohort,
} from '@/api/data'
import { getSpaceByCohort } from '@/api/diseaseKb'
import { getLatestProtocol, getFollowups, getOutcomeStats, publishProtocol } from '@/api/followup'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const cohortId = Number(route.params.id)
const permissionStore = usePermissionStore()
const canManage = permissionStore.hasPermission('disease:followup:manage')

const activeTab = ref('overview')
const loading = ref(false)
const syncing = ref(false)
const cohort = ref<any>({})
const stats = ref<any>({})

const breadcrumb = [
  { title: '数据管理' },
  { title: '专病管理', path: '/data/cdr/disease' },
  { title: cohort.value.name || '详情' },
]

const domainLabels: Record<string, string> = {
  DIAGNOSIS: '诊断', LAB: '检验', MEDICATION: '用药',
  IMAGING: '影像', SURGERY: '手术', PATHOLOGY: '病理',
}
const domainColors: Record<string, string> = {
  DIAGNOSIS: 'blue', LAB: 'green', MEDICATION: 'orange',
  IMAGING: 'purple', SURGERY: 'red', PATHOLOGY: 'cyan',
}
const domainLabel = (d: string) => domainLabels[d] || d
const domainColor = (d: string) => domainColors[d] || 'default'

const FOLLOWUP_STATUS: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: '随访中', color: 'processing' },
  SUSPENDED: { label: '已暂停', color: 'warning' },
  CLOSED: { label: '已结案', color: 'default' },
  OUT_OF_COHORT: { label: '已脱组', color: 'error' },
}
const TREND_SCALES: Record<string, string> = {
  SNOT22: 'SNOT-22', LUND_KENNEDY: 'Lund-Kennedy', LUND_MACKAY: 'Lund-Mackay', OLFACTION: '嗅觉',
}

const parsedRules = computed(() => {
  try {
    const rules = typeof cohort.value.inclusionRules === 'string'
      ? JSON.parse(cohort.value.inclusionRules)
      : cohort.value.inclusionRules
    return rules?.groups || []
  } catch { return [] }
})

function groupSummary(group: any) {
  return (group.conditions || [])
    .map((c: any) => `${c.field} ${c.operator} ${Array.isArray(c.value) ? c.value.join(',') : c.value}`)
    .join(` ${group.logic} `)
}

const patientColumns = [
  { title: '患者ID', dataIndex: 'patientId', key: 'patientId', width: 100 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 70 },
  { title: '年龄', dataIndex: 'age', key: 'age', width: 70 },
  { title: '匹配来源', key: 'matchSource', width: 100 },
  { title: '匹配时间', dataIndex: 'matchedAt', key: 'matchedAt', width: 180 },
  { title: '建档状态', key: 'followState', width: 110 },
  { title: '操作', key: 'action', width: 80 },
]

/* Tab 切换懒加载 */
watch(activeTab, tab => {
  if (tab === 'patients' && !patients.value.length) loadPatients()
  if (tab === 'followup' && !followups.value.length) { loadProtocol(); loadFollowups() }
  if (tab === 'outcome' && !outcome.value) loadOutcome()
})

// ==================== 概览 ====================
async function loadCohort() {
  loading.value = true
  try {
    const res = await getDiseaseCohort(cohortId)
    cohort.value = res.data?.data || {}
  } catch { message.error('加载失败') }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await getDiseaseCohortStatistics(cohortId)
    stats.value = res.data?.data || {}
  } catch { /* ignore */ }
}

async function handleSync() {
  syncing.value = true
  try {
    await syncDiseaseCohort(cohortId)
    message.success('同步完成')
    loadCohort(); loadStats()
    if (activeTab.value === 'patients') loadPatients()
  } catch { message.error('同步失败') }
  finally { syncing.value = false }
}

// ==================== 患者队列 ====================
const patients = ref<any[]>([])
const patientsLoading = ref(false)
const patientPage = ref(1)
const patientTotal = ref(0)
/** 已建档患者集合（显示"已建档"状态） */
const followedIds = ref<Set<number>>(new Set())

async function loadPatients() {
  patientsLoading.value = true
  try {
    const res = await getDiseaseCohortPatients(cohortId, { page: patientPage.value, page_size: 20 })
    const data = res.data?.data || {}
    patients.value = (data.items || []).map((p: any) => ({ ...p, followed: followedIds.value.has(p.patientId) }))
    patientTotal.value = data.total || 0
  } catch { /* ignore */ }
  finally { patientsLoading.value = false }
}

function onPatientTableChange(p: any) {
  patientPage.value = p.current
  loadPatients()
}

async function handleRemovePatient(patientId: number) {
  try {
    await removeDiseaseCohortPatient(cohortId, patientId)
    message.success('已移除')
    loadPatients(); loadStats()
  } catch (e: any) { message.error(e.response?.data?.message || '移除失败') }
}

async function handleExport() {
  try {
    const res = await exportDiseaseCohort(cohortId)
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${cohort.value.name || 'export'}_patients.csv`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch { message.error('导出失败') }
}

// ==================== 随访管理 ====================
const protocolLoading = ref(false)
const latestProtocol = ref<any>(null)
const protocolModalOpen = ref(false)
const publishing = ref(false)
const protocolForm = ref({ name: '', stagesJson: '' })

const followups = ref<any[]>([])
const followupsLoading = ref(false)
const followupPage = ref(1)
const followupTotal = ref(0)
const followupStatusFilter = ref<string | undefined>(undefined)

const followupColumns = [
  { title: '患者', key: 'patient' },
  { title: '状态', key: 'status', width: 100 },
  { title: '建档日期', dataIndex: 'enrollDate', width: 110 },
  { title: '方案版本', dataIndex: 'protocolVersion', width: 90 },
  { title: '负责医生', dataIndex: 'doctorName', width: 110 },
  { title: '随访护士', dataIndex: 'nurseName', width: 110 },
  { title: '操作', key: 'action', width: 80 },
]

async function loadProtocol() {
  protocolLoading.value = true
  try {
    const res = await getLatestProtocol(cohortId)
    latestProtocol.value = res.data?.data || null
  } catch { latestProtocol.value = null }
  finally { protocolLoading.value = false }
}

async function loadFollowups() {
  followupsLoading.value = true
  try {
    const res = await getFollowups(cohortId, {
      status: followupStatusFilter.value || undefined,
      page: followupPage.value, page_size: 20,
    })
    const data = res.data?.data || {}
    followups.value = data.content || data.items || []
    followupTotal.value = data.totalElements ?? data.total ?? followups.value.length
    // 回填队列 Tab 的已建档集合（活跃档案）
    followedIds.value = new Set(followups.value
      .filter(f => f.status === 'ACTIVE').map(f => f.patientId))
  } catch { message.error('随访档案加载失败') }
  finally { followupsLoading.value = false }
}

const enrollOpen = ref(false)
function onEnrolled() {
  followupPage.value = 1
  loadFollowups()
  if (activeTab.value === 'patients') loadPatients()
}

function openProtocolModal() {
  protocolForm.value = { name: latestProtocol.value?.name || '', stagesJson: latestProtocol.value ? JSON.stringify(latestProtocol.value.stages, null, 2) : '' }
  protocolModalOpen.value = true
}

async function handlePublishProtocol() {
  if (!protocolForm.value.name.trim()) { message.warning('请填写方案名称'); return }
  publishing.value = true
  try {
    const stages = JSON.parse(protocolForm.value.stagesJson)
    await publishProtocol(cohortId, { name: protocolForm.value.name.trim(), stages })
    message.success('新版本已发布')
    protocolModalOpen.value = false
    loadProtocol()
  } catch (e: any) {
    message.error(e.response?.data?.message || (e instanceof SyntaxError ? '阶段 JSON 格式错误' : '发布失败'))
  } finally { publishing.value = false }
}

// ==================== 结局看板 ====================
const outcomeLoading = ref(false)
const outcome = ref<any>(null)
const trendScale = ref('SNOT22')

async function loadOutcome() {
  outcomeLoading.value = true
  try {
    const res = await getOutcomeStats(cohortId)
    outcome.value = res.data?.data || {}
  } catch { outcome.value = {} }
  finally { outcomeLoading.value = false }
}

const STAGE_LABELS: Record<string, string> = { BASELINE: '基线', M3: '3个月', M6: '6个月', M12: '12个月' }
const trendOption = computed(() => {
  const points = outcome.value?.scaleTrends?.[trendScale.value] || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 30, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: points.map((p: any) => STAGE_LABELS[p.stage] || p.stage) },
    yAxis: { type: 'value', name: '均分' },
    series: [{
      type: 'line', data: points.map((p: any) => p.avg), smooth: true,
      symbolSize: 9, lineStyle: { width: 3 },
      label: { show: true, formatter: (v: any) => Number(v.value).toFixed(1) },
      areaStyle: { opacity: 0.1 },
    }],
  }
})

const medicationOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
  legend: { orient: 'vertical', left: 'left', type: 'scroll' },
  series: [{
    type: 'pie', radius: ['35%', '65%'],
    data: (outcome.value?.medicationDistribution || []).map((m: any) => ({ name: m.name, value: m.count })),
    label: { formatter: '{b}: {c}' },
  }],
}))

// ==================== 既有 ====================
async function toggleAutoSync(val: boolean) {
  try {
    await updateDiseaseCohort(cohortId, { autoSync: val })
    cohort.value.autoSync = val
  } catch { message.error('更新失败') }
}

// 队列 → 专病知识库：已关联直接进入，未关联引导创建（FR-04）
async function goKnowledgeBase() {
  try {
    const res = await getSpaceByCohort(cohortId)
    const space = res.data?.data
    if (space?.id) {
      router.push({ name: 'DiseaseKnowledgeDetail', params: { id: space.id } })
      return
    }
  } catch { /* 查询失败按未关联处理 */ }
  Modal.confirm({
    title: '该专病尚未关联知识库',
    content: '是否前往专病知识库创建并关联该专病的知识空间？',
    okText: '前往创建',
    cancelText: '取消',
    onOk: () => router.push({ name: 'DiseaseKnowledgeList' }),
  })
}

onMounted(() => {
  loadCohort()
  loadStats()
  loadFollowups() // 概览"在管档案"指标 + followedIds 回填
})
</script>

<style scoped>
.rules-summary { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.rules-title { font-weight: 600; margin-bottom: 8px; }
.rule-line { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.text-gray { color: #ccc; }
</style>
