<template>
  <PageContainer title="患者360视图" subtitle="患者全生命周期健康档案" :loading="loading">
    <template #extra>
      <a-button @click="router.back()">
        <template #icon><ArrowLeftOutlined /></template>
        返回列表
      </a-button>
    </template>

    <template v-if="patient">

    <!-- Compact Profile Header -->
    <div class="profile-compact">
      <div class="profile-left">
        <div class="avatar-mini">
          <UserOutlined style="font-size: 18px; color: #1677ff" />
        </div>
        <div class="profile-basic">
          <span class="name">{{ patient.name }}</span>
          <a-tag :color="patient.gender === 'M' ? 'blue' : 'pink'" size="small">
            {{ patient.gender === 'M' ? '男' : patient.gender === 'F' ? '女' : '未知' }}
          </a-tag>
          <span class="age">{{ age }}岁</span>
          <span class="divider">|</span>
          <span class="birth">{{ patient.birthDate }}</span>
          <span class="divider">|</span>
          <span class="org">组织: {{ patient.orgId }}</span>
          <template v-if="allergies.length">
            <span class="divider">|</span>
            <a-tag v-for="item in allergies.slice(0, 2)" :key="item.id" color="red" size="small">{{ item.allergen }}</a-tag>
            <a-tag v-if="allergies.length > 2" size="small">+{{ allergies.length - 2 }}</a-tag>
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

    <!-- Main Content: Tabs with Timeline in Extra -->
    <a-card :bordered="false" style="margin-top: 12px">
      <a-tabs v-model:activeKey="activeTab">
        <template #rightExtras>
          <div class="timeline-mini" v-if="timelineEvents.length">
            <span class="timeline-label">最近就诊:</span>
            <a-tooltip v-for="(event, idx) in timelineEvents.slice(0, 3)" :key="idx">
              <template #title>{{ event.date }} - {{ event.title }}</template>
              <a-tag :color="event.tagColor" class="timeline-tag">
                {{ event.type }} {{ event.date }}
              </a-tag>
            </a-tooltip>
          </div>
        </template>

        <a-tab-pane key="outpatient" tab="门诊记录">
          <a-table
            :columns="outpatientColumns"
            :data-source="outpatientData"
            size="small"
            row-key="id"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'encounterType'">
                <a-badge status="success" text="已完成" />
              </template>
            </template>
          </a-table>
          <a-empty v-if="!outpatientData.length" description="暂无门诊记录" />
        </a-tab-pane>

        <a-tab-pane key="inpatient" tab="住院记录">
          <a-table
            :columns="inpatientColumns"
            :data-source="inpatientData"
            size="small"
            row-key="id"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'dischargeTime'">
                {{ record.dischargeTime ? record.dischargeTime : '住院中' }}
              </template>
              <template v-if="column.key === 'los'">
                {{ record.dischargeTime ? calcLos(record.admissionTime, record.dischargeTime) + '天' : '-' }}
              </template>
            </template>
          </a-table>
          <a-empty v-if="!inpatientData.length" description="暂无住院记录" />
        </a-tab-pane>

        <a-tab-pane key="lab" tab="检验报告">
          <a-segmented v-model:value="labSubTab" :options="[{ value: 'regular', label: '常规检验' }, { value: 'micro', label: '微生物报告' }]" style="margin-bottom: 16px" />

          <!-- Regular Lab Reports -->
          <template v-if="labSubTab === 'regular'">
            <div v-for="report in labTests" :key="report.id" class="report-card">
              <div class="report-header" @click="toggleReport('lab', report.id)">
                <div class="report-title-row">
                  <span class="report-title">{{ report.testName }}</span>
                  <a-tag v-if="report.items?.filter((i: any) => i.abnormalFlag).length" color="red">
                    {{ report.items.filter((i: any) => i.abnormalFlag).length }}项异常
                  </a-tag>
                  <a-tag v-else color="green">正常</a-tag>
                  <a-tag>{{ report.specimenType || '-' }}</a-tag>
                </div>
                <div class="report-meta">
                  报告时间：{{ fmtDateTime(report.reportedAt) }} &nbsp;|&nbsp; 申请医生：{{ report.orderingDoctor || '-' }}
                </div>
              </div>
              <div v-show="expandedReports['lab-' + report.id]" class="report-body">
                <a-table :columns="labItemColumns" :data-source="report.items || []" size="small" row-key="id" :pagination="false">
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'abnormalFlag'">
                      <span :style="{ color: record.abnormalFlag ? '#ff4d4f' : 'inherit', fontWeight: record.abnormalFlag ? '600' : 'normal' }">
                        {{ record.resultValue }}
                      </span>
                    </template>
                    <template v-if="column.key === 'flag'">
                      <a-tag v-if="record.abnormalFlag" color="red" size="small">H</a-tag>
                    </template>
                  </template>
                </a-table>
              </div>
            </div>
            <a-empty v-if="!labTests.length" description="暂无常规检验报告" />
          </template>

          <!-- Microbiology Reports -->
          <template v-if="labSubTab === 'micro'">
            <div v-for="(report, ridx) in microReports" :key="ridx" class="report-card">
              <div class="report-header" @click="toggleReport('micro', ridx)">
                <div class="report-title-row">
                  <span class="report-title">{{ report.specTypeDesc }}</span>
                  <a-tag color="purple">{{ report.chartDate }}</a-tag>
                </div>
              </div>
              <div v-show="expandedReports['micro-' + ridx]" class="report-body">
                <div v-for="(org, oidx) in report.organisms" :key="oidx" class="micro-organism">
                  <div class="organism-header">
                    <span class="organism-name">{{ org.orgName }}</span>
                    <a-tag v-if="org.quantity" color="orange">菌量 {{ org.quantity }}</a-tag>
                  </div>
                  <a-table :columns="microColumns" :data-source="org.antibiotics" size="small" row-key="id" :pagination="false">
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'interpretation'">
                        <a-tag :color="record.interpretation === 'S' ? 'green' : record.interpretation === 'R' ? 'red' : 'orange'">
                          {{ record.interpretation === 'S' ? '敏感(S)' : record.interpretation === 'R' ? '耐药(R)' : '中介(I)' }}
                        </a-tag>
                      </template>
                    </template>
                  </a-table>
                </div>
              </div>
            </div>
            <a-empty v-if="!microReports.length" description="暂无微生物报告" />
          </template>
        </a-tab-pane>

        <a-tab-pane key="imaging" tab="影像检查">
          <a-table
            :columns="imagingColumns"
            :data-source="imagingData"
            size="small"
            row-key="id"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'modality'">
                <a-tag color="blue">{{ record.modality }}</a-tag>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!imagingData.length" description="暂无影像检查" />
        </a-tab-pane>

        <a-tab-pane key="medication" tab="用药记录">
          <a-table
            :columns="medicationColumns"
            :data-source="medicationData"
            size="small"
            row-key="id"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'ACTIVE' ? 'blue' : record.status === 'COMPLETED' ? 'green' : 'default'">
                  {{ record.status === 'ACTIVE' ? '使用中' : record.status === 'COMPLETED' ? '已完成' : record.status }}
                </a-tag>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!medicationData.length" description="暂无用药记录" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  UserOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons-vue'
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

// Table column definitions
const outpatientColumns = [
  { title: '就诊日期', dataIndex: 'admissionTime', key: 'admissionTime', width: 120, customRender: ({ text }: any) => fmtDate(text) },
  { title: '科室', dataIndex: 'department', key: 'department', width: 100 },
  { title: '医生', dataIndex: 'attendingDoctor', key: 'attendingDoctor', width: 120 },
  { title: '诊断', dataIndex: 'diagnosisSummary', key: 'diagnosisSummary' },
  { title: '状态', key: 'encounterType', width: 90 },
]

const inpatientColumns = [
  { title: '入院日期', dataIndex: 'admissionTime', key: 'admissionTime', width: 120, customRender: ({ text }: any) => fmtDate(text) },
  { title: '出院日期', key: 'dischargeTime', width: 120 },
  { title: '科室', dataIndex: 'department', key: 'department', width: 100 },
  { title: '入院诊断', dataIndex: 'diagnosisSummary', key: 'diagnosisSummary' },
  { title: '住院天数', key: 'los', width: 90 },
]

const labItemColumns = [
  { title: '项目名称', dataIndex: 'itemName', key: 'itemName', width: 160 },
  { title: '结果', key: 'abnormalFlag', width: 100 },
  { title: '单位', dataIndex: 'resultUnit', key: 'resultUnit', width: 80 },
  { title: '参考范围', dataIndex: 'referenceRange', key: 'referenceRange', width: 120 },
  { title: '', key: 'flag', width: 40 },
]

const microColumns = [
  { title: '抗生素', dataIndex: 'abName', key: 'abName', width: 200 },
  { title: 'MIC值', dataIndex: 'dilutionText', key: 'dilutionText', width: 100 },
  { title: '结果', key: 'interpretation', width: 120 },
]

function toggleReport(prefix: string, id: number | string) {
  const key = `${prefix}-${id}`
  expandedReports.value[key] = !expandedReports.value[key]
}

const imagingColumns = [
  { title: '检查日期', dataIndex: 'studyDate', key: 'studyDate', width: 120, customRender: ({ text }: any) => fmtDate(text) },
  { title: '检查类型', dataIndex: 'modality', key: 'modality', width: 100 },
  { title: '检查部位', dataIndex: 'bodyPart', key: 'bodyPart', width: 100 },
  { title: '检查描述', dataIndex: 'reportText', key: 'reportText', ellipsis: true },
]

const medicationColumns = [
  { title: '药品名称', dataIndex: 'medName', key: 'medName', width: 160 },
  { title: '用法用量', key: 'dosage', width: 180, customRender: ({ record }: any) => `${record.dosage || ''} ${record.route || ''} ${record.frequency || ''}` },
  { title: '开始日期', dataIndex: 'startTime', key: 'startTime', width: 120, customRender: ({ text }: any) => fmtDate(text) },
  { title: '结束日期', dataIndex: 'endTime', key: 'endTime', width: 120, customRender: ({ text }: any) => text ? fmtDate(text) : '-' },
  { title: '开药医生', dataIndex: 'prescriber', key: 'prescriber', width: 110 },
  { title: '状态', key: 'status', width: 90 },
]

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
  background: linear-gradient(135deg, #f0f5ff 0%, #fff 100%);
  border-radius: 8px;
  padding: 12px 16px;
  border: 1px solid #e6e8eb;
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
  background: rgba(22, 119, 255, 0.1);
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
  color: rgba(0, 0, 0, 0.88);
}
.profile-basic .age {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}
.profile-basic .divider {
  color: #d9d9d9;
  font-size: 12px;
}
.profile-basic .birth,
.profile-basic .org {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
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
  color: #1677ff;
}
.profile-metrics-mini .lbl {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: 4px;
}

/* Timeline Mini in Tab Extra */
.timeline-mini {
  display: flex;
  align-items: center;
  gap: 8px;
}
.timeline-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
.timeline-tag {
  margin: 0;
  font-size: 11px;
}

/* Report Cards */
.report-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}
.report-header {
  padding: 12px 16px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.2s;
}
.report-header:hover {
  background: #f0f5ff;
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
  color: rgba(0, 0, 0, 0.85);
}
.report-meta {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
.report-body {
  padding: 0 16px 12px;
}

/* Microbiology */
.micro-organism {
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
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
  color: rgba(0, 0, 0, 0.8);
}
</style>
