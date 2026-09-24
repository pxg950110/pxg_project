<template>
  <PageContainer title="患者360视图" subtitle="患者全生命周期健康档案" :loading="loading">
    <template #extra>
      <el-button @click="router.back()">
        <el-icon class="mr-1"><ArrowLeft /></el-icon>
        返回列表
      </el-button>
    </template>

    <template v-if="patient">

    <!-- Compact Profile Header -->
    <div class="profile-compact">
      <div class="profile-left">
        <div class="avatar-mini">
          <el-icon style="font-size: 18px; color: #0ea5e9"><User /></el-icon>
        </div>
        <div class="profile-basic">
          <span class="name">{{ patient.name }}</span>
          <el-tag
            size="small"
            :type="patient.gender === 'M' ? 'primary' : 'info'"
            :style="patient.gender !== 'M' ? { color: '#ec4899', background: '#fdf2f8', borderColor: '#fbcfe8' } : undefined"
          >
            {{ patient.gender === 'M' ? '男' : patient.gender === 'F' ? '女' : '未知' }}
          </el-tag>
          <span class="age">{{ age }}岁</span>
          <span class="divider">|</span>
          <span class="birth">{{ patient.birthDate }}</span>
          <span class="divider">|</span>
          <span class="org">组织: {{ patient.orgId }}</span>
          <template v-if="allergies.length">
            <span class="divider">|</span>
            <el-tag v-for="item in allergies.slice(0, 2)" :key="item.id" type="danger" size="small">{{ item.allergen }}</el-tag>
            <el-tag v-if="allergies.length > 2" type="info" size="small">+{{ allergies.length - 2 }}</el-tag>
          </template>
        </div>
      </div>
      <div class="profile-metrics-mini">
        <div class="metric"><span class="val">{{ metrics.encounterCount }}</span><span class="lbl">就诊</span></div>
        <div class="metric"><span class="val">{{ metrics.diagnosisCount }}</span><span class="lbl">诊断</span></div>
        <div class="metric"><span class="val">{{ metrics.medicationCount }}</span><span class="lbl">用药</span></div>
        <div class="metric"><span class="val">{{ metrics.labTestCount }}</span><span class="lbl">检验</span></div>
      </div>
    </div>

    <!-- Main Content: Timeline strip above Tabs -->
    <el-card shadow="never" class="mt-3 !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <div v-if="timelineEvents.length" class="flex justify-end mb-2">
        <div class="timeline-mini">
          <span class="timeline-label">最近就诊:</span>
          <el-tooltip
            v-for="(event, idx) in timelineEvents.slice(0, 3)"
            :key="idx"
            :content="`${event.date} - ${event.title}`"
          >
            <el-tag :type="timelineTagType[event.tagColor] || 'info'" size="small" class="timeline-tag">
              {{ event.type }} {{ event.date }}
            </el-tag>
          </el-tooltip>
        </div>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="门诊记录" name="outpatient">
          <el-table :data="outpatientPaged" size="small" row-key="id">
            <el-table-column label="就诊日期" width="120">
              <template #default="{ row }">{{ fmtDate(row.admissionTime) }}</template>
            </el-table-column>
            <el-table-column label="科室" prop="department" width="100" />
            <el-table-column label="医生" prop="attendingDoctor" width="120" />
            <el-table-column label="诊断" prop="diagnosisSummary" />
            <el-table-column label="状态" width="90">
              <template #default>
                <span class="status-cell">
                  <span class="status-dot" style="background: #10b981" />
                  已完成
                </span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!outpatientData.length" description="暂无门诊记录" :image-size="60" />
          <el-pagination
            v-if="outpatientData.length"
            class="mt-4 justify-end"
            background
            layout="total, prev, pager, next"
            :page-size="5"
            :total="outpatientData.length"
            :current-page="outpatientPage"
            hide-on-single-page
            @current-change="(p: number) => outpatientPage = p"
          />
        </el-tab-pane>

        <el-tab-pane label="住院记录" name="inpatient">
          <el-table :data="inpatientPaged" size="small" row-key="id">
            <el-table-column label="入院日期" width="120">
              <template #default="{ row }">{{ fmtDate(row.admissionTime) }}</template>
            </el-table-column>
            <el-table-column label="出院日期" width="120">
              <template #default="{ row }">{{ row.dischargeTime ? row.dischargeTime : '住院中' }}</template>
            </el-table-column>
            <el-table-column label="科室" prop="department" width="100" />
            <el-table-column label="入院诊断" prop="diagnosisSummary" />
            <el-table-column label="住院天数" width="90">
              <template #default="{ row }">{{ row.dischargeTime ? calcLos(row.admissionTime, row.dischargeTime) + '天' : '-' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!inpatientData.length" description="暂无住院记录" :image-size="60" />
          <el-pagination
            v-if="inpatientData.length"
            class="mt-4 justify-end"
            background
            layout="total, prev, pager, next"
            :page-size="5"
            :total="inpatientData.length"
            :current-page="inpatientPage"
            hide-on-single-page
            @current-change="(p: number) => inpatientPage = p"
          />
        </el-tab-pane>

        <el-tab-pane label="检验报告" name="lab">
          <el-segmented
            v-model="labSubTab"
            :options="[{ value: 'regular', label: '常规检验' }, { value: 'micro', label: '微生物报告' }]"
            style="margin-bottom: 16px"
          />

          <!-- Regular Lab Reports -->
          <template v-if="labSubTab === 'regular'">
            <div v-for="report in labTests" :key="report.id" class="report-card">
              <div class="report-header" @click="toggleReport('lab', report.id)">
                <div class="report-title-row">
                  <span class="report-title">{{ report.testName }}</span>
                  <el-tag v-if="report.items?.filter((i: any) => i.abnormalFlag).length" type="danger">
                    {{ report.items.filter((i: any) => i.abnormalFlag).length }}项异常
                  </el-tag>
                  <el-tag v-else type="success">正常</el-tag>
                  <el-tag type="info">{{ report.specimenType || '-' }}</el-tag>
                </div>
                <div class="report-meta">
                  报告时间：{{ fmtDateTime(report.reportedAt) }} &nbsp;|&nbsp; 申请医生：{{ report.orderingDoctor || '-' }}
                </div>
              </div>
              <div v-show="expandedReports['lab-' + report.id]" class="report-body">
                <el-table :data="report.items || []" size="small" row-key="id">
                  <el-table-column label="项目名称" prop="itemName" width="160" />
                  <el-table-column label="结果" width="100">
                    <template #default="{ row }">
                      <span :style="{ color: row.abnormalFlag ? '#ef4444' : 'inherit', fontWeight: row.abnormalFlag ? '600' : 'normal' }">
                        {{ row.resultValue }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="单位" prop="resultUnit" width="80" />
                  <el-table-column label="参考范围" prop="referenceRange" width="120" />
                  <el-table-column label="" width="40">
                    <template #default="{ row }">
                      <el-tag v-if="row.abnormalFlag" type="danger" size="small">H</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
            <el-empty v-if="!labTests.length" description="暂无常规检验报告" :image-size="60" />
          </template>

          <!-- Microbiology Reports -->
          <template v-if="labSubTab === 'micro'">
            <div v-for="(report, ridx) in microReports" :key="ridx" class="report-card">
              <div class="report-header" @click="toggleReport('micro', ridx)">
                <div class="report-title-row">
                  <span class="report-title">{{ report.specTypeDesc }}</span>
                  <el-tag type="info" :style="{ color: '#8b5cf6', background: '#f5f3ff', borderColor: '#ddd6fe' }">{{ report.chartDate }}</el-tag>
                </div>
              </div>
              <div v-show="expandedReports['micro-' + ridx]" class="report-body">
                <div v-for="(org, oidx) in report.organisms" :key="oidx" class="micro-organism">
                  <div class="organism-header">
                    <span class="organism-name">{{ org.orgName }}</span>
                    <el-tag v-if="org.quantity" type="warning">菌量 {{ org.quantity }}</el-tag>
                  </div>
                  <el-table :data="org.antibiotics" size="small" row-key="id">
                    <el-table-column label="抗生素" prop="abName" width="200" />
                    <el-table-column label="MIC值" prop="dilutionText" width="100" />
                    <el-table-column label="结果" width="120">
                      <template #default="{ row }">
                        <el-tag :type="row.interpretation === 'S' ? 'success' : row.interpretation === 'R' ? 'danger' : 'warning'">
                          {{ row.interpretation === 'S' ? '敏感(S)' : row.interpretation === 'R' ? '耐药(R)' : '中介(I)' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </div>
            <el-empty v-if="!microReports.length" description="暂无微生物报告" :image-size="60" />
          </template>
        </el-tab-pane>

        <el-tab-pane label="影像检查" name="imaging">
          <el-table :data="imagingPaged" size="small" row-key="id">
            <el-table-column label="检查日期" width="120">
              <template #default="{ row }">{{ fmtDate(row.studyDate) }}</template>
            </el-table-column>
            <el-table-column label="检查类型" width="100">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.modality }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="检查部位" prop="bodyPart" width="100" />
            <el-table-column label="检查描述" prop="reportText" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="!imagingData.length" description="暂无影像检查" :image-size="60" />
          <el-pagination
            v-if="imagingData.length"
            class="mt-4 justify-end"
            background
            layout="total, prev, pager, next"
            :page-size="5"
            :total="imagingData.length"
            :current-page="imagingPage"
            hide-on-single-page
            @current-change="(p: number) => imagingPage = p"
          />
        </el-tab-pane>

        <el-tab-pane label="用药记录" name="medication">
          <el-table :data="medicationPaged" size="small" row-key="id">
            <el-table-column label="药品名称" prop="medName" width="160" />
            <el-table-column label="用法用量" width="180">
              <template #default="{ row }">{{ row.dosage || '' }} {{ row.route || '' }} {{ row.frequency || '' }}</template>
            </el-table-column>
            <el-table-column label="开始日期" width="120">
              <template #default="{ row }">{{ fmtDate(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束日期" width="120">
              <template #default="{ row }">{{ row.endTime ? fmtDate(row.endTime) : '-' }}</template>
            </el-table-column>
            <el-table-column label="开药医生" prop="prescriber" width="110" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'primary' : row.status === 'COMPLETED' ? 'success' : 'info'">
                  {{ row.status === 'ACTIVE' ? '使用中' : row.status === 'COMPLETED' ? '已完成' : row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!medicationData.length" description="暂无用药记录" :image-size="60" />
          <el-pagination
            v-if="medicationData.length"
            class="mt-4 justify-end"
            background
            layout="total, prev, pager, next"
            :page-size="5"
            :total="medicationData.length"
            :current-page="medicationPage"
            hide-on-single-page
            @current-change="(p: number) => medicationPage = p"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, User } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getPatient360 } from '@/api/data'

const router = useRouter()
const route = useRoute()
const activeTab = ref('outpatient')
const loading = ref(false)

interface LabPanelItem { id: number; itemCode: string; itemName: string; resultValue: string; resultUnit: string; referenceRange: string; abnormalFlag: boolean }
interface LabTest { id: number; testName: string; specimenType: string; reportedAt: string; orderingDoctor: string; items: LabPanelItem[] }
interface Encounter { id: number; encounterType: string; department: string; admissionTime: string; dischargeTime: string | null; attendingDoctor: string; diagnosisSummary: string }
interface ImagingExam { id: number; modality: string; bodyPart: string; studyDate: string; reportText: string }
interface Medication { id: number; medName: string; dosage: string; route: string; frequency: string; startTime: string; endTime: string | null; prescriber: string; status: string }
interface Allergy { id: number; allergen: string; severity: string }
interface Diagnosis { id: number; diagnosisCode: string; diagnosisName: string; diagnosisType: string }
interface MicroRow { id: number; chartDate: string; specTypeDesc: string; testName: string; orgName: string; isolateNum: number; abName: string; dilutionText: string; interpretation: string; quantity: string }

const patient = ref<any>(null)
const encounters = ref<Encounter[]>([])
const diagnoses = ref<Diagnosis[]>([])
const allergies = ref<Allergy[]>([])
const labTests = ref<LabTest[]>([])
const medications = ref<Medication[]>([])
const imagingExams = ref<ImagingExam[]>([])
const microbiology = ref<MicroRow[]>([])
const metrics = ref({ encounterCount: 0, diagnosisCount: 0, medicationCount: 0, labTestCount: 0 })
const labSubTab = ref<string>('regular')
const expandedReports = ref<Record<string, boolean>>({})

const age = computed(() => {
  if (!patient.value?.birthDate) return '-'
  const birth = new Date(patient.value.birthDate)
  const today = new Date()
  let a = today.getFullYear() - birth.getFullYear()
  if (today.getMonth() < birth.getMonth() || (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())) a--
  return a
})

// Split encounters by type
const outpatientData = computed(() =>
  encounters.value.filter(e => e.encounterType === 'OUTPATIENT')
    .map(e => ({ ...e, date: fmtDateTime(e.admissionTime) }))
)
const inpatientData = computed(() =>
  encounters.value.filter(e => e.encounterType === 'INPATIENT' || e.encounterType === 'EMERGENCY')
)

// Microbiology reports grouped by specimen+date, then by organism
const microReports = computed(() => {
  const groupMap = new Map<string, { specTypeDesc: string; chartDate: string; organismsMap: Map<string, { orgName: string; quantity: string; antibiotics: any[] }> }>()
  for (const row of microbiology.value) {
    const dateStr = fmtDate(row.chartDate)
    const gKey = `${row.specTypeDesc}|${dateStr}`
    if (!groupMap.has(gKey)) {
      groupMap.set(gKey, { specTypeDesc: row.specTypeDesc, chartDate: dateStr, organismsMap: new Map() })
    }
    const report = groupMap.get(gKey)!
    if (!report.organismsMap.has(row.orgName)) {
      report.organismsMap.set(row.orgName, { orgName: row.orgName, quantity: row.quantity, antibiotics: [] })
    }
    if (row.abName) {
      report.organismsMap.get(row.orgName)!.antibiotics.push(row)
    }
  }
  return Array.from(groupMap.values()).map(r => ({
    ...r,
    organisms: Array.from(r.organismsMap.values()),
  }))
})

const imagingData = computed(() => imagingExams.value)
const medicationData = computed(() => medications.value)

// Timeline from recent encounters
const timelineEvents = computed(() => {
  const typeColor: Record<string, string> = { OUTPATIENT: 'blue', INPATIENT: 'red', EMERGENCY: 'orange' }
  const typeLabel: Record<string, string> = { OUTPATIENT: '门诊', INPATIENT: '住院', EMERGENCY: '急诊' }
  return encounters.value.slice(0, 5).map(e => ({
    date: fmtDate(e.admissionTime),
    type: typeLabel[e.encounterType] || e.encounterType,
    title: e.diagnosisSummary || '-',
    department: e.department,
    detail: e.attendingDoctor ? `${e.attendingDoctor}` : '',
    color: typeColor[e.encounterType] || 'blue',
    tagColor: typeColor[e.encounterType] || 'blue',
  }))
})
/** antd 颜色名 → el-tag type 适配 */
const timelineTagType: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
  blue: 'primary', red: 'danger', orange: 'warning',
}

function fmtDateTime(dt: string | null | undefined) {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}

function fmtDate(dt: string | null | undefined) {
  if (!dt) return '-'
  return dt.substring(0, 10)
}

function calcLos(admit: string, discharge: string) {
  const a = new Date(admit), b = new Date(discharge)
  return Math.max(1, Math.round((b.getTime() - a.getTime()) / 86400000))
}

// Client-side pagination（el-table 不会自动分页，pageSize 与原 a-table 一致为 5）
const PAGE_SIZE = 5
const outpatientPage = ref(1)
const inpatientPage = ref(1)
const imagingPage = ref(1)
const medicationPage = ref(1)
const slicePage = (arr: unknown[], page: number) => arr.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
const outpatientPaged = computed(() => slicePage(outpatientData.value, outpatientPage.value))
const inpatientPaged = computed(() => slicePage(inpatientData.value, inpatientPage.value))
const imagingPaged = computed(() => slicePage(imagingData.value, imagingPage.value))
const medicationPaged = computed(() => slicePage(medicationData.value, medicationPage.value))

function toggleReport(prefix: string, id: number | string) {
  const key = `${prefix}-${id}`
  expandedReports.value[key] = !expandedReports.value[key]
}

async function fetchPatient360() {
  const id = route.params.id as string
  if (!id) return
  loading.value = true
  try {
    const res = await getPatient360(id)
    const data = res.data?.data || res.data
    patient.value = data
    encounters.value = data.encounters || []
    diagnoses.value = data.diagnoses || []
    allergies.value = data.allergies || []
    labTests.value = data.labTests || []
    medications.value = data.medications || []
    imagingExams.value = data.imagingExams || []
    microbiology.value = data.microbiology || []
    metrics.value = {
      encounterCount: data.encounterCount || encounters.value.length,
      diagnosisCount: data.diagnosisCount || diagnoses.value.length,
      medicationCount: data.medicationCount || medications.value.length,
      labTestCount: data.labTestCount || labTests.value.length,
    }
  } catch (e) {
    console.error('Failed to load patient 360 data:', e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchPatient360)
</script>

<style scoped>
/* Compact Profile Header */
.profile-compact {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #fff 100%);
  border-radius: 8px;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
}
.profile-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar-mini {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(14, 165, 233, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.profile-basic {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.profile-basic .name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}
.profile-basic .age {
  font-size: 13px;
  color: #64748b;
}
.profile-basic .divider {
  color: #cbd5e1;
  font-size: 12px;
}
.profile-basic .birth,
.profile-basic .org {
  font-size: 12px;
  color: #94a3b8;
}

/* Compact Metrics */
.profile-metrics-mini {
  display: flex;
  gap: 24px;
}
.profile-metrics-mini .metric {
  text-align: center;
}
.profile-metrics-mini .val {
  font-size: 18px;
  font-weight: 600;
  color: #0ea5e9;
}
.profile-metrics-mini .lbl {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 4px;
}

/* Timeline Mini above Tabs */
.timeline-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}
.timeline-label {
  font-size: 12px;
  color: #94a3b8;
}
.timeline-tag {
  margin: 0;
  font-size: 11px;
}

/* Report Cards */
.report-card {
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}
.report-header {
  padding: 12px 16px;
  cursor: pointer;
  background: #f8fafc;
  transition: background 0.2s;
}
.report-header:hover {
  background: #f0f9ff;
}
.report-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.report-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}
.report-meta {
  font-size: 12px;
  color: #94a3b8;
}
.report-body {
  padding: 0 16px 12px;
}

/* Status dot cell */
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* Microbiology */
.micro-organism {
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f1f5f9;
}
.micro-organism:last-child {
  border-bottom: none;
}
.organism-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.organism-name {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}
</style>
