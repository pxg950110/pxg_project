<template>
  <PageContainer title="患者360视图" subtitle="患者全生命周期健康档案">
    <template #extra>
      <a-button @click="router.back()">
        <template #icon><ArrowLeftOutlined /></template>
        返回列表
      </a-button>
    </template>

    <!-- Profile Header Card -->
    <a-card :bordered="false" class="profile-card">
      <div class="profile-layout">
        <div class="profile-avatar">
          <div class="avatar-circle">
            <UserOutlined style="font-size: 36px; color: #1677ff" />
          </div>
        </div>
        <div class="profile-info">
          <div class="profile-name-row">
            <h2 class="profile-name">{{ patient.name }}</h2>
            <a-tag :color="patient.gender === '男' ? 'blue' : 'pink'" class="gender-tag">
              {{ patient.gender }}
            </a-tag>
            <span class="profile-age">{{ patient.age }}岁</span>
          </div>
          <a-descriptions :column="3" size="small" :colon="true" class="profile-desc">
            <a-descriptions-item label="身份证号">{{ patient.idCard }}</a-descriptions-item>
            <a-descriptions-item label="血型">{{ patient.bloodType }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ patient.phone }}</a-descriptions-item>
            <a-descriptions-item label="家庭住址" :span="3">{{ patient.address }}</a-descriptions-item>
          </a-descriptions>
          <div class="allergy-row">
            <span class="allergy-label">过敏史：</span>
            <a-tag v-for="(item, idx) in patient.allergies" :key="idx" color="red">{{ item }}</a-tag>
          </div>
        </div>
      </div>
    </a-card>

    <!-- 4 Metric Stat Cards -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="6">
        <MetricCard
          title="就诊次数"
          :value="metrics.encounters"
          suffix="次"
          :trend="{ value: 8, type: 'up' }"
        />
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="诊断数"
          :value="metrics.diagnoses"
          suffix="个"
          :trend="{ value: 5, type: 'up' }"
        />
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="用药记录"
          :value="metrics.medications"
          suffix="条"
          :trend="{ value: 12, type: 'up' }"
        />
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="检验报告"
          :value="metrics.labReports"
          suffix="份"
          :trend="{ value: 6, type: 'up' }"
        />
      </a-col>
    </a-row>

    <!-- Medical Timeline -->
    <a-card :bordered="false" title="就医时间线" style="margin-top: 16px">
      <template #extra>
        <span class="timeline-hint">最近5条记录</span>
      </template>
      <a-timeline class="medical-timeline">
        <a-timeline-item
          v-for="(event, idx) in timelineEvents"
          :key="idx"
          :color="event.color"
        >
          <div class="timeline-event">
            <div class="timeline-date">{{ event.date }}</div>
            <div class="timeline-body">
              <a-tag :color="event.tagColor" class="timeline-type-tag">{{ event.type }}</a-tag>
              <span class="timeline-title">{{ event.title }}</span>
              <span class="timeline-dept">{{ event.department }}</span>
            </div>
            <div v-if="event.detail" class="timeline-detail">{{ event.detail }}</div>
          </div>
        </a-timeline-item>
      </a-timeline>
    </a-card>

    <!-- 5 Tabs -->
    <a-card :bordered="false" style="margin-top: 16px">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="outpatient" tab="门诊记录">
          <a-table
            :columns="outpatientColumns"
            :data-source="outpatientData"
            size="small"
            row-key="id"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-badge
                  :status="record.status === '已完成' ? 'success' : 'processing'"
                  :text="record.status"
                />
              </template>
            </template>
          </a-table>
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
              <template v-if="column.key === 'status'">
                <a-badge
                  :status="record.status === '已出院' ? 'success' : record.status === '住院中' ? 'processing' : 'default'"
                  :text="record.status"
                />
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="lab" tab="检验报告">
          <a-table
            :columns="labColumns"
            :data-source="labData"
            size="small"
            row-key="id"
            :pagination="{ pageSize: 5 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'abnormal'">
                <a-tag :color="record.abnormal ? 'red' : 'green'">
                  {{ record.abnormal ? '异常' : '正常' }}
                </a-tag>
              </template>
            </template>
          </a-table>
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
                <a-tag :color="record.status === '使用中' ? 'blue' : 'default'">
                  {{ record.status }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  UserOutlined,
  ArrowLeftOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'

const router = useRouter()
const activeTab = ref('outpatient')

// ============ Patient Profile ============
const patient = {
  name: '张三',
  gender: '男',
  age: 65,
  idCard: '310***********1234',
  bloodType: 'A型',
  phone: '138****5678',
  address: '上海市浦东新区张江高科技园区碧波路690号',
  allergies: ['青霉素', '磺胺类'],
}

// ============ Metric Stats ============
const metrics = {
  encounters: 28,
  diagnoses: 15,
  medications: 42,
  labReports: 36,
}

// ============ Timeline Events ============
const timelineEvents = [
  {
    date: '2025-03-15',
    type: '门诊',
    title: '心内科门诊随访，血压控制良好',
    department: '心内科',
    detail: '血压 128/82mmHg，心率 72次/分，继续当前方案治疗',
    color: 'blue',
    tagColor: 'blue',
  },
  {
    date: '2025-01-20',
    type: '住院',
    title: '急性心肌梗死，行PCI术',
    department: '心内科',
    detail: '冠脉造影示前降支近段狭窄90%，植入支架1枚，术后恢复良好',
    color: 'red',
    tagColor: 'red',
  },
  {
    date: '2024-09-08',
    type: '检验',
    title: '血常规+肝肾功能检查',
    department: '检验科',
    detail: '空腹血糖 7.2mmol/L（偏高），糖化血红蛋白 6.8%，血脂正常',
    color: 'green',
    tagColor: 'green',
  },
  {
    date: '2024-06-12',
    type: '影像',
    title: '胸部CT平扫',
    department: '影像科',
    detail: '双肺纹理增多，右下肺陈旧灶，心影增大，主动脉结钙化',
    color: 'orange',
    tagColor: 'orange',
  },
  {
    date: '2024-02-03',
    type: '门诊',
    title: '内分泌科门诊，2型糖尿病复诊',
    department: '内分泌科',
    detail: '调整二甲双胍剂量至500mg tid，加强饮食控制',
    color: 'blue',
    tagColor: 'blue',
  },
]

// ============ Tab: Outpatient ============
const outpatientColumns = [
  { title: '就诊日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '科室', dataIndex: 'department', key: 'department', width: 100 },
  { title: '医生', dataIndex: 'doctor', key: 'doctor', width: 90 },
  { title: '诊断', dataIndex: 'diagnosis', key: 'diagnosis' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
]

const outpatientData = [
  { id: 'OP2025031501', date: '2025-03-15', department: '心内科', doctor: '王建国', diagnosis: '冠心病、高血压3级、2型糖尿病', status: '已完成' },
  { id: 'OP2025020801', date: '2025-02-08', department: '内分泌科', doctor: '李明华', diagnosis: '2型糖尿病、糖尿病肾病II期', status: '已完成' },
  { id: 'OP2024112001', date: '2024-11-20', department: '心内科', doctor: '王建国', diagnosis: '冠心病PCI术后、高血压', status: '已完成' },
  { id: 'OP2024090801', date: '2024-09-08', department: '内分泌科', doctor: '李明华', diagnosis: '2型糖尿病复诊', status: '已完成' },
  { id: 'OP2024061201', date: '2024-06-12', department: '呼吸内科', doctor: '赵志远', diagnosis: '慢性支气管炎', status: '已完成' },
  { id: 'OP2024020301', date: '2024-02-03', department: '内分泌科', doctor: '李明华', diagnosis: '2型糖尿病、血脂异常', status: '已完成' },
  { id: 'OP2023111501', date: '2023-11-15', department: '心内科', doctor: '王建国', diagnosis: '高血压3级、窦性心动过速', status: '已完成' },
]

// ============ Tab: Inpatient ============
const inpatientColumns = [
  { title: '住院号', dataIndex: 'admissionId', key: 'admissionId', width: 120 },
  { title: '入院日期', dataIndex: 'admissionDate', key: 'admissionDate', width: 120 },
  { title: '出院日期', dataIndex: 'dischargeDate', key: 'dischargeDate', width: 120 },
  { title: '科室', dataIndex: 'department', key: 'department', width: 100 },
  { title: '入院诊断', dataIndex: 'diagnosis', key: 'diagnosis' },
  { title: '住院天数', dataIndex: 'days', key: 'days', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
]

const inpatientData = [
  { id: 'IP001', admissionId: 'A2025012001', admissionDate: '2025-01-20', dischargeDate: '2025-01-28', department: '心内科', diagnosis: '急性ST段抬高型心肌梗死', days: 8, status: '已出院' },
  { id: 'IP002', admissionId: 'A2024030501', admissionDate: '2024-03-05', dischargeDate: '2024-03-12', department: '内分泌科', diagnosis: '2型糖尿病酮症酸中毒', days: 7, status: '已出院' },
  { id: 'IP003', admissionId: 'A2023061501', admissionDate: '2023-06-15', dischargeDate: '2023-06-22', department: '呼吸内科', diagnosis: '慢性阻塞性肺疾病急性加重', days: 7, status: '已出院' },
]

// ============ Tab: Lab Reports ============
const labColumns = [
  { title: '报告日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '检验项目', dataIndex: 'testName', key: 'testName', width: 160 },
  { title: '结果', dataIndex: 'result', key: 'result', width: 120 },
  { title: '参考范围', dataIndex: 'referenceRange', key: 'referenceRange', width: 120 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '异常', dataIndex: 'abnormal', key: 'abnormal', width: 70 },
]

const labData = [
  { id: 'LAB001', date: '2025-03-15', testName: '空腹血糖(FBG)', result: '6.8', referenceRange: '3.9-6.1', unit: 'mmol/L', abnormal: true },
  { id: 'LAB002', date: '2025-03-15', testName: '糖化血红蛋白(HbA1c)', result: '6.8', referenceRange: '4.0-6.0', unit: '%', abnormal: true },
  { id: 'LAB003', date: '2025-03-15', testName: '总胆固醇(TC)', result: '4.2', referenceRange: '2.8-5.2', unit: 'mmol/L', abnormal: false },
  { id: 'LAB004', date: '2025-03-15', testName: '低密度脂蛋白(LDL)', result: '2.6', referenceRange: '0-3.4', unit: 'mmol/L', abnormal: false },
  { id: 'LAB005', date: '2025-03-15', testName: '肌酐(Cr)', result: '98', referenceRange: '44-133', unit: 'umol/L', abnormal: false },
  { id: 'LAB006', date: '2025-01-22', testName: '肌钙蛋白I(cTnI)', result: '0.03', referenceRange: '0-0.04', unit: 'ng/mL', abnormal: false },
  { id: 'LAB007', date: '2025-01-22', testName: 'BNP', result: '186', referenceRange: '0-100', unit: 'pg/mL', abnormal: true },
  { id: 'LAB008', date: '2024-09-08', testName: '血红蛋白(Hb)', result: '132', referenceRange: '120-160', unit: 'g/L', abnormal: false },
  { id: 'LAB009', date: '2024-09-08', testName: '白细胞(WBC)', result: '7.2', referenceRange: '3.5-9.5', unit: '10^9/L', abnormal: false },
  { id: 'LAB010', date: '2024-09-08', testName: '血小板(PLT)', result: '198', referenceRange: '125-350', unit: '10^9/L', abnormal: false },
]

// ============ Tab: Imaging ============
const imagingColumns = [
  { title: '检查日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '检查类型', dataIndex: 'modality', key: 'modality', width: 100 },
  { title: '检查部位', dataIndex: 'bodyPart', key: 'bodyPart', width: 100 },
  { title: '检查描述', dataIndex: 'description', key: 'description' },
  { title: '报告医生', dataIndex: 'reportDoctor', key: 'reportDoctor', width: 90 },
]

const imagingData = [
  { id: 'IMG001', date: '2025-01-21', modality: 'CT', bodyPart: '冠状动脉', description: '冠脉CTA示前降支近段支架通畅，余冠脉未见明显狭窄', reportDoctor: '陈晓峰' },
  { id: 'IMG002', date: '2024-06-12', modality: 'CT', bodyPart: '胸部', description: '双肺纹理增多，右下肺陈旧灶，心影增大，主动脉结钙化', reportDoctor: '陈晓峰' },
  { id: 'IMG003', date: '2024-02-03', modality: 'X线', bodyPart: '胸部', description: '心影增大，心胸比0.55，两肺未见明显实变影', reportDoctor: '周丽萍' },
  { id: 'IMG004', date: '2023-10-18', modality: '超声', bodyPart: '心脏', description: '左室壁运动减弱，LVEF 52%，左房增大，二尖瓣轻度反流', reportDoctor: '孙婉清' },
  { id: 'IMG005', date: '2023-06-16', modality: 'CT', bodyPart: '胸部', description: '慢支肺气肿改变，右下肺轻度感染，建议抗炎后复查', reportDoctor: '陈晓峰' },
]

// ============ Tab: Medication ============
const medicationColumns = [
  { title: '药品名称', dataIndex: 'drugName', key: 'drugName', width: 160 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '用法用量', dataIndex: 'dosage', key: 'dosage', width: 140 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '开药医生', dataIndex: 'doctor', key: 'doctor', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
]

const medicationData = [
  { id: 'MED001', drugName: '阿司匹林肠溶片', specification: '100mg', dosage: '100mg qd 口服', startDate: '2025-01-20', endDate: '-', doctor: '王建国', status: '使用中' },
  { id: 'MED002', drugName: '氯吡格雷片', specification: '75mg', dosage: '75mg qd 口服', startDate: '2025-01-20', endDate: '-', doctor: '王建国', status: '使用中' },
  { id: 'MED003', drugName: '阿托伐他汀钙片', specification: '20mg', dosage: '20mg qn 口服', startDate: '2025-01-20', endDate: '-', doctor: '王建国', status: '使用中' },
  { id: 'MED004', drugName: '盐酸二甲双胍片', specification: '500mg', dosage: '500mg tid 口服', startDate: '2024-02-03', endDate: '-', doctor: '李明华', status: '使用中' },
  { id: 'MED005', drugName: '苯磺酸氨氯地平片', specification: '5mg', dosage: '5mg qd 口服', startDate: '2023-11-15', endDate: '-', doctor: '王建国', status: '使用中' },
  { id: 'MED006', drugName: '美托洛尔缓释片', specification: '47.5mg', dosage: '47.5mg qd 口服', startDate: '2025-01-20', endDate: '-', doctor: '王建国', status: '使用中' },
  { id: 'MED007', drugName: '硝酸甘油片', specification: '0.5mg', dosage: '0.5mg 舌下含服 必要时', startDate: '2025-01-20', endDate: '2025-03-20', doctor: '王建国', status: '已停用' },
  { id: 'MED008', drugName: '低分子肝素钠注射液', specification: '4000IU', dosage: '4000IU q12h 皮下注射', startDate: '2025-01-20', endDate: '2025-01-26', doctor: '王建国', status: '已停用' },
]
</script>

<style scoped>
/* Profile Card */
.profile-card {
  border-radius: 8px;
}
.profile-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.profile-avatar {
  flex-shrink: 0;
}
.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(22, 119, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}
.profile-info {
  flex: 1;
  min-width: 0;
}
.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.profile-name {
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}
.gender-tag {
  font-size: 12px;
  border-radius: 4px;
}
.profile-age {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.55);
}
.profile-desc {
  margin-bottom: 8px;
}
.allergy-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}
.allergy-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  flex-shrink: 0;
}

/* Timeline */
.timeline-hint {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.35);
}
.medical-timeline {
  padding-top: 4px;
}
.timeline-event {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.timeline-date {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 500;
}
.timeline-body {
  display: flex;
  align-items: center;
  gap: 8px;
}
.timeline-type-tag {
  flex-shrink: 0;
  border-radius: 4px;
  font-size: 12px;
}
.timeline-title {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}
.timeline-dept {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  margin-left: auto;
  flex-shrink: 0;
}
.timeline-detail {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1.6;
  padding-left: 2px;
}
</style>
