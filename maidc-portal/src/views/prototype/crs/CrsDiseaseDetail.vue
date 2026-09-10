<template>
  <PageContainer :title="`${cohort.name}（${cohort.icdCode}）· 页面原型`" :breadcrumb="[{ title: '原型' }, { title: '专病详情重构' }]">
    <template #extra>
      <a-button type="primary" ghost>
        <template #icon><BookOutlined /></template> 专病知识库
      </a-button>
      <a-button>手动同步</a-button>
      <a-button @click="$router.back()">返回</a-button>
    </template>

    <a-tabs v-model:activeKey="tab">
      <!-- ================= Tab1 概览 ================= -->
      <a-tab-pane key="overview" tab="概览">
        <a-card style="margin-bottom: 16px">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="专病名称">{{ cohort.name }}（{{ cohort.icdCode }}）</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="cohort.status === 'ACTIVE' ? 'blue' : 'default'">已启用</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="自动同步">
              <a-switch :checked="cohort.autoSync" size="small" />
            </a-descriptions-item>
            <a-descriptions-item label="最后同步">{{ cohort.lastSyncAt }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ cohort.createdAt }}</a-descriptions-item>
            <a-descriptions-item label="随访方案">CRS 随访方案 v2（PUBLISHED）</a-descriptions-item>
            <a-descriptions-item label="描述" :span="2">{{ cohort.description }}</a-descriptions-item>
          </a-descriptions>
          <div class="rules-summary">
            <div class="rules-title">纳入规则</div>
            <div v-for="(group, gi) in cohort.inclusionRules.groups" :key="gi" class="rule-line">
              <a-tag :color="domainColor(group.domain)">{{ domainLabels[group.domain] || group.domain }}</a-tag>
              <span>{{ groupSummary(group) }}</span>
            </div>
          </div>
        </a-card>

        <a-row :gutter="16">
          <a-col :span="6"><a-card><a-statistic title="患者总数" :value="cohort.stats.totalPatients" /></a-card></a-col>
          <a-col :span="6"><a-card><a-statistic title="在管随访档案" :value="4" :value-style="{ color: '#1677ff' }" /></a-card></a-col>
          <a-col :span="6"><a-card><a-statistic title="随访依从率" :value="outcomeStats.complianceRate" suffix="%" :precision="1" /></a-card></a-col>
          <a-col :span="6"><a-card><a-statistic title="SNOT-22 改善率" :value="outcomeStats.improvementRate" suffix="%" :precision="1" :value-style="{ color: '#389e0d' }" /></a-card></a-col>
        </a-row>
      </a-tab-pane>

      <!-- ================= Tab2 患者队列 ================= -->
      <a-tab-pane key="patients" tab="患者队列">
        <a-card>
          <template #extra>
            <a-button size="small">手动添加</a-button>
            <a-button size="small">导出</a-button>
          </template>
          <a-table :columns="patientColumns" :data-source="cohortPatients" row-key="patientId" size="small" :pagination="{ pageSize: 10 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'matchSource'">
                <a-tag :color="record.matchSource === 'AUTO' ? 'blue' : 'orange'">
                  {{ record.matchSource === 'AUTO' ? '自动' : '手动' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'followed'">
                <a-tag v-if="record.followed" color="green">已建档</a-tag>
                <a-button v-else type="link" size="small" @click="enrollOpen = true">建档</a-button>
              </template>
              <template v-if="column.key === 'action'">
                <a-popconfirm v-if="record.matchSource === 'MANUAL'" title="确认移除？">
                  <a-button type="link" danger size="small">移除</a-button>
                </a-popconfirm>
                <span v-else style="color: #ccc">-</span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- ================= Tab3 随访管理 ================= -->
      <a-tab-pane key="followup">
        <template #tab>随访管理 <a-badge :count="3" size="small" style="margin-left: 4px" /></template>

        <!-- 方案卡 -->
        <a-card style="margin-bottom: 16px">
          <template #title>随访方案</template>
          <template #extra>
            <a-space>
              <a-tag color="blue">v{{ protocol.version }} · PUBLISHED</a-tag>
              <a-button size="small" type="primary" ghost>发布新版本</a-button>
            </a-space>
          </template>
          <a-steps :current="4" size="small" style="margin-bottom: 16px">
            <a-step v-for="st in protocol.stages" :key="st.stageCode" :title="st.name" :description="`建档 + ${st.offsetDays} 天`" />
          </a-steps>
          <a-table :data-source="protocol.stages" :columns="stageColumns" :pagination="false" size="small" row-key="stageCode">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'requiredScales'">
                <a-tag v-for="c in record.requiredScales" :key="c" color="red">{{ scaleName(c) }}</a-tag>
              </template>
              <template v-if="column.key === 'optionalScales'">
                <a-tag v-for="c in record.optionalScales" :key="c">{{ scaleName(c) }}</a-tag>
                <span v-if="!record.optionalScales.length" style="color: #ccc">-</span>
              </template>
              <template v-if="column.key === 'note'">
                <span style="color: #999">{{ record.note || '-' }}</span>
              </template>
            </template>
          </a-table>
        </a-card>

        <!-- 建档列表 -->
        <a-card>
          <template #title>患者随访档案</template>
          <template #extra>
            <a-space>
              <a-select v-model:value="followupStatusFilter" size="small" style="width: 120px" allow-clear placeholder="状态筛选"
                :options="[{ value: 'ACTIVE', label: '随访中' }, { value: 'SUSPENDED', label: '已暂停' }, { value: 'CLOSED', label: '已结案' }]" />
              <a-button size="small" type="primary" @click="enrollOpen = true">+ 建档</a-button>
            </a-space>
          </template>
          <a-table :columns="followupColumns" :data-source="filteredFollowups" row-key="id" size="small" :pagination="{ pageSize: 10 }"
            :custom-row="(r: any) => ({ onClick: () => goArchive(r.id), style: { cursor: 'pointer' } })">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="STATUS_META[record.status]?.color">{{ STATUS_META[record.status]?.label }}</a-tag>
              </template>
              <template v-if="column.key === 'progress'">
                <a-progress :percent="progressOf(record)" size="small" style="width: 90px" />
              </template>
              <template v-if="column.key === 'currentStage'">
                {{ currentStageOf(record) }}
              </template>
              <template v-if="column.key === 'nextDue'">
                <span :style="nextTaskOf(record)?.overdue ? 'color: #cf1322' : ''">
                  {{ nextTaskOf(record)?.text || '-' }}
                </span>
              </template>
              <template v-if="column.key === 'protocolVersion'">
                <a-tag>v{{ record.protocolVersion }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <!-- ================= Tab4 结局看板 ================= -->
      <a-tab-pane key="outcome" tab="结局看板">
        <a-row :gutter="16" style="margin-bottom: 16px">
          <a-col :span="6"><a-card><a-statistic title="随访依从率" :value="outcomeStats.complianceRate" suffix="%" :precision="1" /></a-card></a-col>
          <a-col :span="6"><a-card><a-statistic title="SNOT-22 改善率（≥MCID 9）" :value="outcomeStats.improvementRate" suffix="%" :precision="1" :value-style="{ color: '#389e0d' }" /></a-card></a-col>
          <a-col :span="6"><a-card><a-statistic title="手术率" :value="outcomeStats.surgeryRate" suffix="%" :precision="1" /></a-card></a-col>
          <a-col :span="6"><a-card><a-statistic title="再手术患者数" :value="outcomeStats.reoperationCount" /></a-card></a-col>
        </a-row>

        <a-card title="量表趋势（队列均分）" style="margin-bottom: 16px">
          <template #extra>
            <a-radio-group v-model:value="trendScale" size="small" button-style="solid">
              <a-radio-button v-for="(v, k) in outcomeStats.scaleTrends" :key="k" :value="k">{{ v.name }}</a-radio-button>
            </a-radio-group>
          </template>
          <MetricChart :option="trendOption" :height="300" />
        </a-card>

        <a-card title="药物分布（MEDICATION 记录聚类）">
          <MetricChart :option="medOption" :height="260" />
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <EnrollModal v-model:open="enrollOpen" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { BookOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import EnrollModal from './components/EnrollModal.vue'
import { cohort, protocol, followups, cohortPatients, outcomeStats, scales, STATUS_META, taskDerivedStatus, TODAY } from './mock'
import type { Followup } from './mock'

const router = useRouter()
const tab = ref('overview')
const enrollOpen = ref(false)
const followupStatusFilter = ref<string | undefined>(undefined)
const trendScale = ref('SNOT22')

const domainLabels: Record<string, string> = { DIAGNOSIS: '诊断', LAB: '检验', MEDICATION: '用药' }
const domainColor = (d: string) => ({ DIAGNOSIS: 'blue', LAB: 'green', MEDICATION: 'orange' } as any)[d] || 'default'
const groupSummary = (g: any) => g.conditions.map((c: any) => `${c.field} ${c.operator} ${c.value}`).join(` ${g.logic} `)
const scaleName = (code: string) => scales.find(s => s.scaleCode === code)?.name.split(' ')[0] || code

/* Tab2 */
const patientColumns = [
  { title: '患者姓名', dataIndex: 'patientName' },
  { title: '性别', dataIndex: 'gender', width: 60 },
  { title: '年龄', dataIndex: 'age', width: 60 },
  { title: '匹配来源', key: 'matchSource', width: 90 },
  { title: '匹配时间', dataIndex: 'matchedAt', width: 160 },
  { title: '建档状态', key: 'followed', width: 100 },
  { title: '操作', key: 'action', width: 70 },
]

/* Tab3 */
const stageColumns = [
  { title: '阶段', dataIndex: 'name', width: 120 },
  { title: '偏移天数', dataIndex: 'offsetDays', width: 90 },
  { title: '必评量表', key: 'requiredScales' },
  { title: '选评量表', key: 'optionalScales' },
  { title: '备注', key: 'note' },
]

const followupColumns = [
  { title: '患者', dataIndex: 'patientName', width: 90 },
  { title: '性别/年龄', key: 'ga', width: 90, customRender: ({ record }: any) => `${record.gender}/${record.age}` },
  { title: '负责医生', dataIndex: 'doctorName', width: 90 },
  { title: '随访护士', dataIndex: 'nurseName', width: 90 },
  { title: '建档日', dataIndex: 'enrollDate', width: 110 },
  { title: '状态', key: 'status', width: 90 },
  { title: '当前阶段', key: 'currentStage', width: 130 },
  { title: '下次随访', key: 'nextDue', width: 150 },
  { title: '进度', key: 'progress', width: 120 },
  { title: '方案', key: 'protocolVersion', width: 60 },
]

const filteredFollowups = computed(() =>
  followupStatusFilter.value ? followups.filter(f => f.status === followupStatusFilter.value) : followups)

function progressOf(f: Followup) {
  const done = f.tasks.filter(t => t.status !== 'PENDING').length
  return Math.round((done / f.tasks.length) * 100)
}
function currentStageOf(f: Followup) {
  const t = f.tasks.find(t => t.status === 'PENDING')
  return t ? `${t.stageName}${t.dueDate < TODAY ? '（超期）' : ''}` : '全部完成'
}
function nextTaskOf(f: Followup) {
  const t = f.tasks.find(t => t.status === 'PENDING')
  if (!t) return null
  return { text: `${t.dueDate} · ${t.stageName}`, overdue: t.dueDate < TODAY }
}
function goArchive(fid: number) {
  router.push({ name: 'ProtoCrsArchive', params: { fid } })
}

/* Tab4 图表 */
const trendOption = computed(() => {
  const t = outcomeStats.scaleTrends[trendScale.value as keyof typeof outcomeStats.scaleTrends]
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 30, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: t.points.map(p => p[0]) },
    yAxis: { type: 'value', name: '均分' },
    series: [{
      type: 'line', data: t.points.map(p => p[1]), smooth: true,
      symbolSize: 8, lineStyle: { width: 3 },
      areaStyle: { opacity: 0.12 },
      label: { show: true },
    }],
  }
})
const medOption = {
  tooltip: { trigger: 'item', formatter: '{b}: {c} 条（{d}%）' },
  legend: { orient: 'vertical', right: 10, top: 'center' },
  series: [{
    type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
    data: outcomeStats.medicationDistribution,
    label: { show: false },
  }],
}
</script>

<style scoped>
.rules-summary { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f0f0f0; }
.rules-title { font-weight: 600; margin-bottom: 8px; }
.rule-line { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
</style>
