<template>
  <PageContainer :title="cohort.name || '专病详情'" :breadcrumb="breadcrumb">
    <template #extra>
      <div class="flex items-center gap-2">
        <el-button type="primary" plain @click="goKnowledgeBase">
          <el-icon class="mr-1"><Reading /></el-icon> 专病知识库
        </el-button>
        <el-button @click="handleSync" :loading="syncing">手动同步</el-button>
        <el-button @click="router.back()">返回</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <!-- ============ Tab1 概览 ============ -->
      <el-tab-pane label="概览" name="overview">
        <div v-loading="loading" class="min-h-[200px]">
          <el-card shadow="never" class="mb-4 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
            <template #header>基本信息</template>
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="专病名称">{{ cohort.name }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="cohort.status === 'ACTIVE' ? 'primary' : 'info'">
                  {{ cohort.status === 'ACTIVE' ? '已启用' : '未启用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="自动同步">
                <el-switch :model-value="cohort.autoSync" @change="(v: string | number | boolean) => toggleAutoSync(Boolean(v))" />
              </el-descriptions-item>
              <el-descriptions-item label="最后同步">{{ cohort.lastSyncAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ cohort.createdAt }}</el-descriptions-item>
              <el-descriptions-item label="描述">{{ cohort.description || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div class="rules-summary" v-if="parsedRules.length">
              <div class="rules-title">纳入规则</div>
              <div v-for="(group, gi) in parsedRules" :key="gi" class="rule-line">
                <el-tag
                  :type="domainTypeMap[group.domain] || 'info'"
                  size="small"
                  :style="domainStyleMap[group.domain]"
                >
                  {{ domainLabel(group.domain) }}
                </el-tag>
                <span>{{ groupSummary(group) }}</span>
              </div>
            </div>
          </el-card>

          <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ stats.totalPatients || 0 }}</div>
                <div class="stat-lbl">患者总数</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ followupTotal }}</div>
                <div class="stat-lbl">在管档案</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ Number(outcome?.complianceRate || 0).toFixed(1) }}<span class="stat-suffix">%</span></div>
                <div class="stat-lbl">随访依从率</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ Number(outcome?.improvementRate || 0).toFixed(1) }}<span class="stat-suffix">%</span></div>
                <div class="stat-lbl">SNOT-22 改善率</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ Number(stats.maleRatio || 0).toFixed(1) }}<span class="stat-suffix">%</span></div>
                <div class="stat-lbl">男性占比</div>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ Tab2 患者队列 ============ -->
      <el-tab-pane label="患者队列" name="patients">
        <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
          <template #header>
            <div class="flex justify-end">
              <el-button @click="handleExport">导出</el-button>
            </div>
          </template>
          <el-table :data="patients" v-loading="patientsLoading" row-key="id" size="small">
            <el-table-column label="患者ID" prop="patientId" width="100" />
            <el-table-column label="性别" prop="gender" width="70" />
            <el-table-column label="年龄" prop="age" width="70" />
            <el-table-column label="匹配来源" width="100">
              <template #default="{ row }">
                <el-tag :type="row.matchSource === 'AUTO' ? 'primary' : 'warning'" size="small">
                  {{ row.matchSource === 'AUTO' ? '自动' : '手动' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="匹配时间" prop="matchedAt" width="180" />
            <el-table-column label="建档状态" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.followed" type="primary" size="small">已建档</el-tag>
                <el-button v-else-if="canManage" size="small" type="primary" plain @click="enrollOpen = true">建档</el-button>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-popconfirm v-if="row.matchSource === 'MANUAL'" title="确认移除？" @confirm="handleRemovePatient(row.patientId)">
                  <template #reference>
                    <el-button link type="danger" size="small">移除</el-button>
                  </template>
                </el-popconfirm>
                <span v-else class="text-gray">-</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="mt-4 justify-end"
            background
            layout="total, prev, pager, next"
            :total="patientTotal"
            :current-page="patientPage"
            :page-size="20"
            @current-change="handlePatientPage"
          />
        </el-card>
      </el-tab-pane>

      <!-- ============ Tab3 随访管理 ============ -->
      <el-tab-pane label="随访管理" name="followup">
        <!-- 方案卡 -->
        <el-card shadow="never" size="small" class="mb-4 !rounded-lg">
          <template #header>
            <div class="flex items-center justify-between">
              <span>随访方案</span>
              <el-button v-if="canManage" size="small" type="primary" plain @click="openProtocolModal">发布新版本</el-button>
            </div>
          </template>
          <div v-loading="protocolLoading" class="min-h-[100px]">
            <template v-if="latestProtocol">
              <div class="flex flex-wrap items-center gap-4 mb-2">
                <b>{{ latestProtocol.name }}</b>
                <el-tag type="primary" size="small">v{{ latestProtocol.version }}</el-tag>
                <el-tag type="success" size="small">PUBLISHED</el-tag>
              </div>
              <el-steps :active="latestProtocol.stages.length - 1" align-center>
                <el-step
                  v-for="st in latestProtocol.stages" :key="st.stageCode"
                  :title="st.name" :description="`+${st.offsetDays}天 · 必评 ${st.requiredScales.length} 项`"
                />
              </el-steps>
            </template>
            <el-empty v-else description="暂无随访方案，请联系管理员创建" :image-size="60" />
          </div>
        </el-card>

        <!-- 建档列表 -->
        <el-card shadow="never" size="small" class="!rounded-lg">
          <template #header>
            <div class="flex items-center justify-between">
              <span>随访档案（{{ followupTotal }}）</span>
              <div class="flex items-center gap-2">
                <el-select v-model="followupStatusFilter" style="width: 130px" clearable placeholder="状态筛选" @change="loadFollowups">
                  <el-option value="ACTIVE" label="随访中" />
                  <el-option value="SUSPENDED" label="已暂停" />
                  <el-option value="CLOSED" label="已结案" />
                  <el-option value="OUT_OF_COHORT" label="已脱组" />
                </el-select>
                <el-button v-if="canManage" type="primary" size="small" @click="enrollOpen = true">+ 建档</el-button>
              </div>
            </div>
          </template>
          <el-table :data="followups" v-loading="followupsLoading" row-key="id" size="small">
            <el-table-column label="患者">
              <template #default="{ row }">
                <a class="text-sky-600 cursor-pointer" @click="$router.push({ name: 'FollowupArchiveDetail', params: { fid: row.id }, query: { cohortId } })">
                  <b>{{ row.patientName || '患者#' + row.patientId }}</b>
                </a>
                <span v-if="row.patientGender" class="text-gray">（{{ row.patientGender }}/{{ row.patientAge }}岁）</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="FOLLOWUP_STATUS[row.status]?.type || 'info'" size="small">
                  {{ FOLLOWUP_STATUS[row.status]?.label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="建档日期" prop="enrollDate" width="110" />
            <el-table-column label="方案版本" prop="protocolVersion" width="90" />
            <el-table-column label="负责医生" prop="doctorName" width="110" />
            <el-table-column label="随访护士" prop="nurseName" width="110" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="$router.push({ name: 'FollowupArchiveDetail', params: { fid: row.id }, query: { cohortId } })">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="mt-4 justify-end"
            background
            layout="total, prev, pager, next"
            :total="followupTotal"
            :current-page="followupPage"
            :page-size="20"
            @current-change="handleFollowupPage"
          />
        </el-card>
      </el-tab-pane>

      <!-- ============ Tab4 结局看板 ============ -->
      <el-tab-pane label="结局看板" name="outcome">
        <div v-loading="outcomeLoading" class="min-h-[200px]">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ Number(outcome?.complianceRate || 0).toFixed(1) }}<span class="stat-suffix">%</span></div>
                <div class="stat-lbl">随访依从率</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ Number(outcome?.improvementRate || 0).toFixed(1) }}<span class="stat-suffix">%</span></div>
                <div class="stat-lbl">SNOT-22 改善率</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ Number(outcome?.surgeryRate || 0).toFixed(1) }}<span class="stat-suffix">%</span></div>
                <div class="stat-lbl">手术率</div>
              </div>
            </el-card>
            <el-card shadow="never" class="!rounded-lg">
              <div class="stat">
                <div class="stat-val">{{ outcome?.reoperationCount || 0 }}<span class="stat-suffix"> 人</span></div>
                <div class="stat-lbl">再手术患者</div>
              </div>
            </el-card>
          </div>

          <el-card shadow="never" size="small" class="mb-4 !rounded-lg">
            <template #header>量表趋势（按随访阶段均分）</template>
            <el-radio-group v-model="trendScale" size="small" class="mb-2">
              <el-radio-button v-for="(label, code) in TREND_SCALES" :key="code" :value="code">{{ label }}</el-radio-button>
            </el-radio-group>
            <MetricChart :option="trendOption" :height="280" />
          </el-card>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <el-card shadow="never" size="small" class="!rounded-lg">
              <template #header>药物分布（治疗记录聚类）</template>
              <MetricChart :option="medicationOption" :height="300" />
            </el-card>
            <el-card shadow="never" size="small" class="!rounded-lg">
              <template #header>指标说明</template>
              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="依从率">应完成任务（到期 ≤ 今日）中已完成占比</el-descriptions-item>
                <el-descriptions-item label="改善率">末次评估距基线 ≥ 3 个月且改善 ≥ MCID(9) 的患者占比</el-descriptions-item>
                <el-descriptions-item label="手术率">有手术记录的患者占在管档案比例</el-descriptions-item>
                <el-descriptions-item label="再手术">含 ≥ 2 条 ESS 手术记录的患者数</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 建档弹窗（队列行内 / 随访管理按钮 共用） -->
    <EnrollModal v-model:open="enrollOpen" :cohort-id="cohortId" @created="onEnrolled" />

    <!-- 发布新版本方案 -->
    <el-dialog v-model="protocolModalOpen" title="发布随访方案新版本" width="680px" :destroy-on-close="true">
      <el-alert
        type="warning"
        show-icon
        :closable="false"
        title="发布新版本后，存量档案保持快照不变；医生可在档案详情执行「升级方案」"
        style="margin-bottom: 12px"
      />
      <el-form label-position="top">
        <el-form-item label="方案名称" required>
          <el-input v-model="protocolForm.name" placeholder="CRS 慢性鼻窦炎随访方案" />
        </el-form-item>
        <el-form-item label="阶段（JSON）" required>
          <el-input v-model="protocolForm.stagesJson" type="textarea" :rows="8"
            placeholder='[{"stageCode":"BASELINE","name":"基线","offsetDays":0,"requiredScales":["SNOT22"],"optionalScales":[]}]' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="protocolModalOpen = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublishProtocol">发布</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Reading } from '@element-plus/icons-vue'
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
const domainTypeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  DIAGNOSIS: 'primary', LAB: 'success', MEDICATION: 'warning', SURGERY: 'danger',
}
const domainStyleMap: Record<string, { color: string; background: string; borderColor: string }> = {
  IMAGING: { color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' },
  PATHOLOGY: { color: '#06b6d4', background: '#ecfeff', borderColor: '#a5f3fc' },
}
const domainLabel = (d: string) => domainLabels[d] || d

const FOLLOWUP_STATUS: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'danger' | 'info' }> = {
  ACTIVE: { label: '随访中', type: 'primary' },
  SUSPENDED: { label: '已暂停', type: 'warning' },
  CLOSED: { label: '已结案', type: 'info' },
  OUT_OF_COHORT: { label: '已脱组', type: 'danger' },
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
  } catch { ElMessage.error('加载失败') }
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
    ElMessage.success('同步完成')
    loadCohort(); loadStats()
    if (activeTab.value === 'patients') loadPatients()
  } catch { ElMessage.error('同步失败') }
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

function handlePatientPage(page: number) {
  patientPage.value = page
  loadPatients()
}

async function handleRemovePatient(patientId: number) {
  try {
    await removeDiseaseCohortPatient(cohortId, patientId)
    ElMessage.success('已移除')
    loadPatients(); loadStats()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '移除失败') }
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
  } catch { ElMessage.error('导出失败') }
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
  } catch { ElMessage.error('随访档案加载失败') }
  finally { followupsLoading.value = false }
}

function handleFollowupPage(page: number) {
  followupPage.value = page
  loadFollowups()
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
  if (!protocolForm.value.name.trim()) { ElMessage.warning('请填写方案名称'); return }
  publishing.value = true
  try {
    const stages = JSON.parse(protocolForm.value.stagesJson)
    await publishProtocol(cohortId, { name: protocolForm.value.name.trim(), stages })
    ElMessage.success('新版本已发布')
    protocolModalOpen.value = false
    loadProtocol()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || (e instanceof SyntaxError ? '阶段 JSON 格式错误' : '发布失败'))
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
  } catch { ElMessage.error('更新失败') }
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
  ElMessageBox.confirm('是否前往专病知识库创建并关联该专病的知识空间？', '该专病尚未关联知识库', {
    confirmButtonText: '前往创建',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => router.push({ name: 'DiseaseKnowledgeList' })).catch(() => {})
}

onMounted(() => {
  loadCohort()
  loadStats()
  loadFollowups() // 概览"在管档案"指标 + followedIds 回填
})
</script>

<style scoped>
.rules-summary { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f1f5f9; }
.rules-title { font-weight: 600; margin-bottom: 8px; }
.rule-line { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.text-gray { color: #cbd5e1; }
.stat { text-align: left; }
.stat-val { font-size: 22px; font-weight: 600; color: #0f172a; }
.stat-suffix { font-size: 13px; font-weight: 400; color: #64748b; margin-left: 2px; }
.stat-lbl { font-size: 13px; color: #64748b; margin-top: 2px; }
</style>
