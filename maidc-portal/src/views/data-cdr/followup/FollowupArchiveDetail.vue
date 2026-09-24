<template>
  <div class="p-6 space-y-6 max-w-[1600px] mx-auto">
    <!-- 顶部导航与状态行动栏 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <el-button link class="!text-slate-600 hover:!text-sky-600 !p-0" @click="router.back()">
          <el-icon :size="20"><Back /></el-icon>
        </el-button>
        <div>
          <h2 class="text-xl font-bold text-slate-900 tracking-tight m-0 flex items-center gap-2.5">
            <span class="w-2 h-5 bg-sky-500 rounded-full" />
            随访档案 · {{ detail?.followup?.patientName || '加载中...' }}
          </h2>
          <p class="text-xs text-slate-500 mt-1 m-0">
            全周期随访历程轨迹、方案快照追溯、量表分值纵向评估曲线与伴随治疗记录。
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2.5 flex-wrap">
        <el-button size="small" type="primary" plain class="!rounded-lg" @click="goPatient360">
          <el-icon class="mr-1"><User /></el-icon>
          患者 360 视图
        </el-button>
        <el-button size="small" class="!rounded-lg" @click="load">
          <el-icon class="mr-1"><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="space-y-6 min-h-[400px]">
      <template v-if="detail">
        <!-- 档案核心画像与状态管理卡片 -->
        <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div class="flex items-center gap-4 flex-wrap">
              <div class="flex items-baseline gap-2">
                <span class="text-lg font-bold text-slate-900">{{ detail.followup.patientName }}</span>
                <span v-if="detail.followup.patientGender" class="text-xs text-slate-500 font-medium">
                  ({{ detail.followup.patientGender }} / {{ detail.followup.patientAge }}岁)
                </span>
              </div>

              <el-tag
                size="small"
                :type="STATUS_META[detail.followup.status]?.tagType"
                effect="light"
                class="!rounded font-medium px-2.5"
              >
                档案状态：{{ STATUS_META[detail.followup.status]?.label }}
              </el-tag>

              <span class="text-xs text-slate-400 font-mono">
                建档日期：{{ detail.followup.enrollDate }} · 方案 v{{ detail.followup.protocolVersion }}（快照不可变）
              </span>

              <span class="text-xs text-slate-500">
                责任医生：<span class="font-medium text-slate-700">{{ detail.followup.doctorName || '-' }}</span> ·
                责任护士：<span class="font-medium text-slate-700">{{ detail.followup.nurseName || '-' }}</span>
              </span>
            </div>

            <!-- 档案状态与方案操作按钮组 -->
            <div class="flex items-center gap-2 flex-wrap">
              <el-button
                v-if="canManage && detail.followup.status === 'ACTIVE'"
                size="small"
                class="!rounded-lg"
                @click="handleSuspend"
              >
                暂停随访
              </el-button>

              <el-button
                v-if="canManage && detail.followup.status === 'SUSPENDED'"
                size="small"
                type="primary"
                plain
                class="!rounded-lg"
                @click="handleResume"
              >
                恢复随访
              </el-button>

              <el-button
                v-if="canManage && !['CLOSED', 'OUT_OF_COHORT'].includes(detail.followup.status)"
                size="small"
                type="danger"
                plain
                class="!rounded-lg"
                @click="closeOpen = true"
              >
                结案
              </el-button>

              <el-button
                v-if="canManage && latestVersion > detail.followup.protocolVersion"
                size="small"
                type="warning"
                plain
                class="!rounded-lg"
                @click="upgradeOpen = true"
              >
                升级方案 → v{{ latestVersion }}
              </el-button>
              <el-tag v-else size="small" type="info" effect="plain" class="!rounded">
                方案已是最新
              </el-tag>

              <el-button
                v-if="canManage"
                size="small"
                type="primary"
                class="!rounded-lg shadow-clinical-sm"
                @click="planFreeOpen = true"
              >
                <el-icon class="mr-1"><Plus /></el-icon>
                计划外评估
              </el-button>
            </div>
          </div>
        </div>

        <!-- 两栏核心工作区：左侧任务时间轴 + 右侧评估曲线与治疗记录 -->
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          <!-- ============ 左侧：随访任务时间轴 (4 / 12) ============ -->
          <div class="lg:col-span-4 bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-slate-100">
              <span class="text-sm font-bold text-slate-800 flex items-center gap-1.5">
                <el-icon class="text-sky-500"><Clock /></el-icon>
                随访任务时空轴
              </span>
              <el-tag size="small" type="info" effect="plain" class="!rounded font-mono">
                共 {{ detail.tasks?.length || 0 }} 个阶段
              </el-tag>
            </div>

            <div v-if="!detail.tasks?.length" class="py-12 text-center text-xs text-slate-400">
              暂无规划的随访任务阶段
            </div>

            <el-timeline v-else class="!pl-1 !pt-2">
              <el-timeline-item
                v-for="t in detail.tasks"
                :key="t.id"
                :type="timelineItemType(t)"
                :hollow="t.status === 'PENDING'"
                size="normal"
              >
                <div class="p-3 rounded-xl border border-slate-200/60 bg-slate-50/50 hover:bg-white hover:border-sky-300 transition-all duration-150 space-y-1.5">
                  <div class="flex items-center justify-between gap-2">
                    <span class="font-bold text-xs text-slate-800">{{ t.stageName }}</span>
                    <el-tag
                      size="small"
                      :type="STATUS_META[t.status]?.tagType"
                      effect="light"
                      class="!rounded !text-[11px]"
                    >
                      {{ STATUS_META[t.status]?.label }}
                    </el-tag>
                  </div>

                  <div class="text-[11px] text-slate-500 font-mono">
                    应随访日：{{ t.dueDate }}
                  </div>

                  <div class="flex flex-wrap items-center gap-1 pt-0.5">
                    <span class="text-[11px] text-slate-400">量表：</span>
                    <el-tag
                      v-for="c in t.requiredScales"
                      :key="c"
                      size="small"
                      type="info"
                      effect="plain"
                      class="!rounded font-mono !text-[10px]"
                    >
                      {{ shortScale(c) }}
                    </el-tag>
                  </div>

                  <div v-if="t.status === 'OVERDUE'" class="text-xs text-rose-600 font-semibold flex items-center gap-1">
                    <el-icon><WarningFilled /></el-icon>
                    已超期 {{ t.overdueDays }} 天
                  </div>

                  <div v-if="t.status === 'PENDING'" class="pt-2">
                    <el-button
                      size="small"
                      type="primary"
                      class="w-full !rounded-lg !text-xs"
                      @click="goWorkbench"
                    >
                      前往随访工作台完成
                    </el-button>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <!-- ============ 右侧：评估曲线 + 治疗记录 (8 / 12) ============ -->
          <div class="lg:col-span-8 space-y-6">
            <!-- 评估演进曲线 -->
            <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
              <div class="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100">
                <span class="text-sm font-bold text-slate-800 flex items-center gap-1.5">
                  <el-icon class="text-sky-500"><Document /></el-icon>
                  量表评分演进轨迹
                </span>

                <div v-if="assessedCodes.length" class="flex items-center gap-1.5 flex-wrap">
                  <el-radio-group v-model="chartScale" size="small">
                    <el-radio-button v-for="c in assessedCodes" :key="c" :value="c">
                      {{ shortScale(c) }}
                    </el-radio-button>
                  </el-radio-group>
                </div>
              </div>

              <div v-if="!assessedCodes.length" class="py-14 text-center">
                <el-empty description="该患者暂无已归档的量表评估历史数据" />
              </div>
              <div v-else>
                <MetricChart :option="chartOption" :height="280" />
              </div>
            </div>

            <!-- 伴随治疗记录 -->
            <div class="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-clinical-sm space-y-4">
              <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-bold text-slate-800">
                    伴随治疗与临床干预记录
                  </span>
                  <el-tag size="small" type="info" effect="plain" class="!rounded font-mono">
                    {{ detail.treatments?.length || 0 }} 项
                  </el-tag>
                </div>

                <div class="flex items-center gap-2">
                  <el-button size="small" class="!rounded-lg" @click="treatOpen = true">
                    <el-icon class="mr-1"><Plus /></el-icon>
                    手工添加
                  </el-button>
                  <el-button size="small" type="primary" plain class="!rounded-lg" @click="openCdrImport">
                    <el-icon class="mr-1"><Connection /></el-icon>
                    从 CDR 带入快照
                  </el-button>
                </div>
              </div>

              <div v-if="!detail.treatments?.length" class="py-12 text-center">
                <el-empty description="随访期内暂无伴随治疗、手术或用药干预记录" />
              </div>

              <div v-else class="space-y-3">
                <div
                  v-for="t in detail.treatments"
                  :key="t.id"
                  class="p-3.5 rounded-xl border border-slate-200/70 bg-slate-50/60 hover:bg-white hover:border-slate-300 transition-all duration-150 space-y-2"
                >
                  <div class="flex items-center justify-between gap-2 flex-wrap">
                    <div class="flex items-center gap-2 flex-wrap">
                      <el-tag
                        size="small"
                        :type="t.category === 'SURGERY' ? 'danger' : t.category === 'MEDICATION' ? 'warning' : 'info'"
                        effect="light"
                        class="!rounded font-medium"
                      >
                        {{ categoryLabel(t.category) }}
                      </el-tag>
                      <span class="font-bold text-xs text-slate-800">{{ t.name }}</span>
                      <el-tag size="small" type="info" effect="plain" class="!rounded !text-[11px]">
                        {{ t.source === 'CDR' ? 'CDR 带入（只读快照）' : '手工录入' }}
                      </el-tag>
                    </div>
                    <span class="text-xs text-slate-400 font-mono">{{ t.occurredDate }}</span>
                  </div>

                  <div v-if="parsedDetail(t).length" class="flex flex-wrap gap-2 pt-1">
                    <span
                      v-for="[k, v] in parsedDetail(t)"
                      :key="k"
                      class="inline-flex items-center gap-1 text-[11px] bg-white px-2 py-0.5 rounded border border-slate-200 text-slate-600 font-mono"
                    >
                      <span class="text-slate-400">{{ detailLabel(k) }}:</span>
                      <span>{{ v ?? '-' }}</span>
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 方案快照说明卡片 -->
            <div class="p-4 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-500 leading-relaxed">
              <span class="font-semibold text-slate-700">📌 方案快照说明：</span><br />
              当前随访档案严格锚定建档时的方案版本（v{{ detail.followup.protocolVersion }}）。后续方案演进不影响既有档案的历史轨迹；仅在专科医生明确确认「升级方案」后，才会对未来的未完成任务进行平滑重排。
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 结案确认弹窗 -->
    <el-dialog
      v-model="closeOpen"
      title="随访档案结案确认"
      width="480px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="结案原因（必填）" required>
          <el-input
            v-model="closeReason"
            type="textarea"
            :rows="3"
            maxlength="512"
            show-word-limit
            placeholder="例如：临床治愈完成全程随访 / 患者主动退出随访计划"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="closeOpen = false">取消</el-button>
          <el-button type="danger" :loading="acting" @click="handleClose">确认结案</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 升级方案确认弹窗 -->
    <el-dialog
      v-model="upgradeOpen"
      title="随访方案升级确认"
      width="480px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <div class="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-800 leading-relaxed space-y-2">
        <div class="font-bold flex items-center gap-1.5 text-amber-900">
          <el-icon><WarningFilled /></el-icon>
          方案升级影响说明
        </div>
        <div>
          即将把本档案切换至最新方案 <b>v{{ latestVersion }}</b>：仅重新生成<b>未来未完成的任务</b>（PENDING 且到期日 ≥ 今天）；历史已完成、已跳过的任务将原样保留。
        </div>
      </div>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="upgradeOpen = false">取消</el-button>
          <el-button type="primary" :loading="acting" @click="handleUpgrade">确认平滑升级</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 计划外评估弹窗 -->
    <el-dialog
      v-model="planFreeOpen"
      title="发起计划外临床量表评估（医生授权）"
      width="780px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="选择评估量表" required>
          <el-select v-model="planFreeScale" placeholder="请选择量表" class="!w-64">
            <el-option
              v-for="opt in scaleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="planFreeScale && detail" class="mt-4">
        <ScaleFillPanel
          :scale-code="planFreeScale"
          :patient-id="detail.followup.patientId"
          @change="onPlanFreeChange"
        />
      </div>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="planFreeOpen = false">取消</el-button>
          <el-button type="primary" :loading="acting" @click="handlePlanFree">提交计划外评估</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 手工添加治疗记录弹窗 -->
    <el-dialog
      v-model="treatOpen"
      title="手工添加伴随治疗记录"
      width="480px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <el-form label-position="top" class="space-y-3">
        <el-form-item label="治疗类别" required>
          <el-radio-group v-model="treatForm.category" size="small">
            <el-radio-button value="MEDICATION">药物</el-radio-button>
            <el-radio-button value="SURGERY">手术</el-radio-button>
            <el-radio-button value="OTHER">其他</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="项目名称" required>
          <el-input v-model="treatForm.name" placeholder="如：口服地氯雷他定片 / 鼻内镜下鼻息肉切除术" />
        </el-form-item>

        <el-form-item label="发生日期" required>
          <el-date-picker
            v-model="treatForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择治疗发生日期"
            class="!w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="treatOpen = false">取消</el-button>
          <el-button type="primary" :loading="acting" @click="handleAddTreatment">确认添加</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 从 CDR 带入治疗记录弹窗 -->
    <el-dialog
      v-model="cdrOpen"
      title="从 CDR 带入治疗记录（只读快照）"
      width="720px"
      destroy-on-close
      class="!rounded-2xl"
    >
      <div class="p-3 rounded-xl bg-sky-50 border border-sky-100 text-xs text-sky-800 mb-3">
        去重机制：同项目名称且同发生日期的 CDR 记录已存在时将自动判定并置灰跳过。
      </div>

      <div v-if="!cdrCandidates.length" class="py-12 text-center text-xs text-slate-400">
        该患者近 180 天内无 CDR 治疗/用药关联记录
      </div>

      <el-table
        v-else
        :data="cdrCandidates"
        row-key="recordId"
        size="small"
        class="w-full"
        @selection-change="onCdrSelectionChange"
      >
        <el-table-column
          type="selection"
          width="48"
          :selectable="(row: any) => !row.exists"
        />
        <el-table-column label="来源类型" prop="resourceType" width="140" />
        <el-table-column label="名称" prop="name" min-width="180" />
        <el-table-column label="类别" width="90">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.category === 'MEDICATION' ? 'warning' : 'info'"
              effect="light"
              class="!rounded"
            >
              {{ categoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发生日期" prop="occurredDate" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.exists" size="small" type="warning" effect="plain" class="!rounded">
              已存在，跳过
            </el-tag>
            <el-tag v-else size="small" type="success" effect="plain" class="!rounded">
              可带入
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="flex items-center justify-end gap-2">
          <el-button @click="cdrOpen = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!selectedRows.length"
            :loading="acting"
            @click="handleCdrImport"
          >
            带入所选 ({{ selectedRows.length }})
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Back,
  User,
  Refresh,
  Plus,
  Clock,
  Document,
  Connection,
  WarningFilled,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import MetricChart from '@/components/MetricChart/index.vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import {
  getFollowupDetail,
  getProtocols,
  closeFollowup,
  suspendFollowup,
  resumeFollowup,
  upgradeFollowupProtocol,
  createPlanFreeAssessment,
  createTreatment,
  getCdrTreatmentCandidates,
  importCdrTreatments,
  getScales,
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

const STATUS_META: Record<string, { label: string; tagType: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  ACTIVE: { label: '随访中', tagType: 'primary' },
  SUSPENDED: { label: '已暂停', tagType: 'warning' },
  CLOSED: { label: '已结案', tagType: 'info' },
  OUT_OF_COHORT: { label: '已脱组', tagType: 'danger' },
  DONE: { label: '已完成', tagType: 'success' },
  SKIPPED: { label: '已跳过', tagType: 'info' },
  OVERDUE: { label: '已超期', tagType: 'danger' },
  PENDING: { label: '待办', tagType: 'primary' },
}

const scaleNames = ref<Record<string, string>>({})
const shortScale = (code: string) => scaleNames.value[code]?.split(' ')[0] || code
const scaleOptions = computed(() =>
  Object.keys(scaleNames.value).map((c) => ({ value: c, label: shortScale(c) })),
)
const categoryLabel = (c: string) => ({ MEDICATION: '药物', SURGERY: '手术', OTHER: '其他' } as any)[c] || c
const detailLabel = (k: string) =>
  ({ dosage: '剂量', route: '途径', startDate: '开始', endDate: '结束', surgeon: '术者', hospitalDays: '住院天数', side: '侧别' } as any)[k] || k

const timelineItemType = (t: any): 'primary' | 'success' | 'warning' | 'info' | 'danger' => {
  if (t.status === 'OVERDUE') return 'danger'
  if (t.status === 'DONE') return 'success'
  if (t.status === 'SKIPPED') return 'info'
  return 'primary'
}

function parsedDetail(t: any): [string, any][] {
  if (!t.detail) return []
  try {
    return Object.entries(typeof t.detail === 'string' ? JSON.parse(t.detail) : t.detail)
  } catch {
    return []
  }
}

async function load() {
  loading.value = true
  try {
    const [dRes, sRes] = await Promise.all([getFollowupDetail(fid.value), getScales()])
    detail.value = dRes.data?.data
    const map: Record<string, string> = {}
    const scaleList: any[] = sRes.data?.data || []
    scaleList.forEach((x) => {
      map[x.scaleCode] = x.name
    })
    scaleNames.value = map
  } catch {
    ElMessage.error('档案详情加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  load()
  try {
    const cohortId = route.query.cohortId
    if (cohortId) {
      const res = await getProtocols(cohortId as string)
      const versions = (res.data?.data || []).filter((p: any) => p.status === 'PUBLISHED').map((p: any) => p.version)
      latestVersion.value = versions.length ? Math.max(...versions) : 1
    }
  } catch {
    latestVersion.value = 1
  }
})

/* 评估曲线 */
const assessedCodes = computed<string[]>(() =>
  Array.from(new Set<string>((detail.value?.assessments || []).map((a: any) => a.scaleCode))),
)
const chartScale = ref('')
const chartOption = computed(() => {
  const list = (detail.value?.assessments || []).filter((a: any) => a.scaleCode === chartScale.value)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 30, top: 30, bottom: 30 },
    xAxis: {
      type: 'category',
      data: list.map((a: any) => `${a.stageName || ''}\n${(a.assessedAt || '').slice(0, 10)}`),
      axisLabel: { fontSize: 11, color: '#64748b' },
    },
    yAxis: {
      type: 'value',
      name: '得分',
      axisLabel: { color: '#64748b' },
      splitLine: { lineStyle: { stroke: '#f1f5f9' } },
    },
    series: [
      {
        type: 'line',
        data: list.map((a: any) => a.totalScore),
        smooth: true,
        symbolSize: 8,
        itemStyle: { color: '#0284c7' },
        lineStyle: { width: 2.5, color: '#0284c7' },
        label: { show: true, fontSize: 11, fontWeight: 'bold' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(2, 132, 199, 0.25)' },
              { offset: 1, color: 'rgba(2, 132, 199, 0.01)' },
            ],
          },
        },
      },
    ],
  }
})

watch(assessedCodes, (codes) => {
  if (codes.length && !chartScale.value) chartScale.value = codes[0] as string
})

/* 档案操作 */
const closeOpen = ref(false)
const closeReason = ref('')
async function handleClose() {
  if (!closeReason.value.trim()) {
    ElMessage.warning('请填写结案原因')
    return
  }
  acting.value = true
  try {
    await closeFollowup(fid.value, closeReason.value.trim())
    ElMessage.success('档案已结案')
    closeOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '结案失败')
  } finally {
    acting.value = false
  }
}

async function handleSuspend() {
  try {
    await suspendFollowup(fid.value)
    ElMessage.success('已暂停（任务冻结）')
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function handleResume() {
  try {
    await resumeFollowup(fid.value)
    ElMessage.success('已恢复随访状态')
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const upgradeOpen = ref(false)
async function handleUpgrade() {
  acting.value = true
  try {
    const res = await upgradeFollowupProtocol(fid.value)
    const d = res.data?.data
    ElMessage.success(`已升级到 v${d?.protocolVersion}：重建 ${d?.rebuiltTasks} 个未来任务`)
    upgradeOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '升级失败')
  } finally {
    acting.value = false
  }
}

/* 计划外评估 */
const planFreeOpen = ref(false)
const planFreeScale = ref('')
const planFreeAnswers = ref<Record<string, any> | null>(null)
function onPlanFreeChange(p: { answers: Record<string, any>; complete: boolean }) {
  planFreeAnswers.value = p.answers
}
async function handlePlanFree() {
  if (!planFreeScale.value) {
    ElMessage.warning('请选择量表')
    return
  }
  if (!planFreeAnswers.value) {
    ElMessage.warning('请先完成量表填写')
    return
  }
  acting.value = true
  try {
    await createPlanFreeAssessment(
      fid.value,
      { scaleCode: planFreeScale.value, answers: planFreeAnswers.value },
      authStore.userInfo?.id,
    )
    ElMessage.success('计划外评估已提交')
    planFreeOpen.value = false
    planFreeAnswers.value = null
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    acting.value = false
  }
}

/* 手工添加治疗 */
const treatOpen = ref(false)
const treatForm = ref<{ category: string; name: string; date: string }>({
  category: 'MEDICATION',
  name: '',
  date: dayjs().format('YYYY-MM-DD'),
})
async function handleAddTreatment() {
  if (!treatForm.value.name.trim()) {
    ElMessage.warning('请填写治疗名称')
    return
  }
  acting.value = true
  try {
    await createTreatment(fid.value, {
      category: treatForm.value.category,
      name: treatForm.value.name.trim(),
      occurredDate: treatForm.value.date || dayjs().format('YYYY-MM-DD'),
    })
    ElMessage.success('治疗记录已添加')
    treatOpen.value = false
    treatForm.value = { category: 'MEDICATION', name: '', date: dayjs().format('YYYY-MM-DD') }
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '添加失败')
  } finally {
    acting.value = false
  }
}

/* CDR 带入 */
const cdrOpen = ref(false)
const cdrCandidates = ref<any[]>([])
const selectedRows = ref<any[]>([])
function onCdrSelectionChange(selection: any[]) {
  selectedRows.value = selection
}

async function openCdrImport() {
  try {
    const res = await getCdrTreatmentCandidates(fid.value)
    cdrCandidates.value = res.data?.data || []
    selectedRows.value = []
    cdrOpen.value = true
  } catch {
    ElMessage.error('CDR 候选加载失败')
  }
}

async function handleCdrImport() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请勾选要带入的记录')
    return
  }
  acting.value = true
  try {
    const res = await importCdrTreatments(fid.value, selectedRows.value)
    const d = res.data?.data
    ElMessage.success(`已带入 ${d?.imported} 条${d?.skipped?.length ? `，跳过重复 ${d.skipped.length} 条` : ''}`)
    cdrOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '带入失败')
  } finally {
    acting.value = false
  }
}

function goWorkbench() {
  router.push({ name: 'FollowupWorkbench' })
}

function goPatient360() {
  if (detail.value?.followup?.patientId) {
    router.push({ name: 'PatientEncounter360', params: { patientId: detail.value.followup.patientId } })
  }
}
</script>
