<template>
  <PageContainer :title="`随访档案 · ${f.patientName} · 页面原型`" :breadcrumb="[{ title: '原型' }, { title: '随访档案' }]">
    <template #extra>
      <a-button type="primary" ghost @click="goPatient360">患者 360 视图</a-button>
      <a-button @click="$router.push({ name: 'ProtoCrsDetail' })">返回专病详情</a-button>
    </template>

    <!-- 头部：患者 + 档案状态 + 操作 -->
    <a-card size="small" style="margin-bottom: 16px">
      <a-space :size="24" wrap>
        <span style="font-size: 16px"><b>{{ f.patientName }}</b>（{{ f.gender }}/{{ f.age }}岁 · J32）</span>
        <a-tag :color="STATUS_META[f.status]?.color" style="font-size: 13px; padding: 2px 10px">
          档案：{{ STATUS_META[f.status]?.label }}
        </a-tag>
        <span class="dim">建档 {{ f.enrollDate }} · 方案 v{{ f.protocolVersion }}（快照）</span>
        <span class="dim">医生：{{ f.doctorName }} · 护士：{{ f.nurseName }}</span>
      </a-space>
      <template #actions>
        <a-space wrap>
          <a-button v-if="f.status === 'ACTIVE'" size="small" @click="message.info('已暂停：任务冻结不可执行')">暂停随访</a-button>
          <a-button v-else-if="f.status === 'SUSPENDED'" size="small" type="primary" ghost @click="message.info('已恢复')">恢复随访</a-button>
          <a-button v-if="f.status !== 'CLOSED'" size="small" danger ghost @click="closeOpen = true">结案</a-button>
          <a-button v-if="f.protocolVersion < protocol.version" size="small" @click="upgradeOpen = true">
            升级方案 → v{{ protocol.version }}
          </a-button>
          <a-tag v-else color="default">方案已是最新</a-tag>
          <a-button size="small" @click="planFreeOpen = true">计划外评估</a-button>
        </a-space>
      </template>
    </a-card>

    <a-row :gutter="16">
      <!-- 左：任务时间轴 -->
      <a-col :span="8">
        <a-card title="任务时间轴" size="small">
          <a-timeline style="margin-top: 8px">
            <a-timeline-item v-for="t in f.tasks" :key="t.id" :color="timelineColor(t)">
              <template #dot v-if="taskDerivedStatus(t) === 'OVERDUE'">
                <WarningOutlined style="font-size: 16px; color: #cf1322" />
              </template>
              <div class="tl-item">
                <div class="tl-head">
                  <b>{{ t.stageName }}</b>
                  <a-tag :color="STATUS_META[taskDerivedStatus(t)]?.color" size="small">
                    {{ STATUS_META[taskDerivedStatus(t)]?.label }}
                  </a-tag>
                </div>
                <div class="dim">应完成：{{ t.dueDate }}</div>
                <div class="dim">量表：<span v-for="c in t.requiredScales" :key="c" class="scale-chip">{{ shortScale(c) }}</span></div>
                <div v-if="t.completedAt" class="dim">完成于 {{ t.completedAt }}（{{ t.completedByName }}）</div>
                <div v-if="t.skipReason" class="dim" style="color: #faad14">跳过原因：{{ t.skipReason }}</div>
                <div v-if="taskDerivedStatus(t) === 'OVERDUE'" style="color: #cf1322">
                  已超期 {{ overdueDays(t) }} 天
                </div>
                <div v-if="taskDerivedStatus(t) === 'PENDING'" style="margin-top: 4px">
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
          <MetricChart :option="chartOption" :height="260" />
        </a-card>

        <!-- 治疗记录 -->
        <a-card size="small" style="margin-bottom: 16px">
          <template #title>治疗记录（{{ f.treatments.length }}）</template>
          <template #extra>
            <a-space>
              <a-button size="small" @click="treatOpen = true">+ 手工添加</a-button>
              <a-button size="small" type="primary" ghost @click="cdrOpen = true">从 CDR 带入</a-button>
            </a-space>
          </template>
          <a-empty v-if="!f.treatments.length" description="暂无治疗记录" />
          <div v-for="t in f.treatments" :key="t.id" class="treat-card">
            <div class="treat-head">
              <a-tag :color="t.category === 'SURGERY' ? 'red' : t.category === 'MEDICATION' ? 'orange' : 'default'">
                {{ categoryLabel(t.category) }}
              </a-tag>
              <b>{{ t.name }}</b>
              <a-tag size="small">{{ t.source === 'CDR' ? 'CDR 带入（只读快照）' : '手工录入' }}</a-tag>
              <span class="dim" style="margin-left: auto">{{ t.occurredDate }}</span>
            </div>
            <div class="dim" style="margin-top: 4px">
              <span v-for="(v, k) in t.detail" :key="k" class="detail-chip">{{ detailLabel(k) }}：{{ v ?? '-' }}</span>
            </div>
          </div>
        </a-card>

        <!-- 方案快照 -->
        <a-card size="small" title="随访方案快照">
          <a-steps :current="1" size="small" style="margin-bottom: 12px">
            <a-step v-for="st in protocol.stages" :key="st.stageCode" :title="st.name" :description="`+${st.offsetDays}天`" />
          </a-steps>
          <span class="dim">档案绑定 v{{ f.protocolVersion }}；方案升级不影响本档案，除非医生主动执行「升级方案」。</span>
        </a-card>
      </a-col>
    </a-row>

    <!-- 结案弹窗 -->
    <a-modal v-model:open="closeOpen" title="结案" ok-text="确认结案" @ok="message.success('档案已结案（CLOSED）'); closeOpen = false">
      <a-form layout="vertical">
        <a-form-item label="结案原因（必填）" required>
          <a-textarea v-model:value="closeReason" :maxlength="512" show-count placeholder="如：治愈完成随访 / 患者退出" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 升级方案确认 -->
    <a-modal v-model:open="upgradeOpen" title="升级方案" ok-text="确认升级" @ok="message.success('已升级：仅重建未来未完成任务'); upgradeOpen = false">
      <a-alert type="warning" show-icon
        :message="`将切换到 v${protocol.version}：仅重新生成未来未完成任务（PENDING 且到期日 ≥ 今天）；已完成 / 已跳过任务保持不变`" />
    </a-modal>

    <!-- 计划外评估 -->
    <a-modal v-model:open="planFreeOpen" title="发起计划外评估（医生）" width="720" ok-text="提交评估">
      <a-form layout="vertical">
        <a-form-item label="量表">
          <a-select v-model:value="planFreeScale" :options="assessedCodes.map(c => ({ value: c, label: shortScale(c) }))" style="width: 240px" />
        </a-form-item>
      </a-form>
      <ScaleFillPanel v-if="planFreeScale" :scale-code="planFreeScale" :patient-id="f.patientId" />
    </a-modal>

    <!-- 手工添加治疗 -->
    <a-modal v-model:open="treatOpen" title="手工添加治疗记录" ok-text="添加">
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
    <a-modal v-model:open="cdrOpen" title="从 CDR 带入治疗记录（只读快照）" width="680" ok-text="带入所选">
      <a-alert type="info" show-icon style="margin-bottom: 12px"
        message="重复判定：同名 + 同发生日期的 CDR 记录已存在时自动跳过并列出" />
      <a-table :data-source="cdrTreatmentCandidates" :columns="cdrColumns" size="small" :pagination="false"
        :row-selection="{ selectedRowKeys, onChange: onCdrSelect, getCheckboxProps: (r: any) => ({ disabled: r.exists }) }" row-key="recordId">
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { WarningOutlined } from '@ant-design/icons-vue'
import dayjs, { type Dayjs } from 'dayjs'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import ScaleFillPanel from './components/ScaleFillPanel.vue'
import { followups, protocol, scales, STATUS_META, taskDerivedStatus, cdrTreatmentCandidates, TODAY } from './mock'

const route = useRoute()
const router = useRouter()
const f = computed(() => followups.find(x => x.id === Number(route.params.fid)) || followups[0])

const message2 = message // template 里 message.info 可用
const shortScale = (code: string) => scales.find(s => s.scaleCode === code)?.name.split(' ')[0] || code
const categoryLabel = (c: string) => ({ MEDICATION: '药物', SURGERY: '手术', OTHER: '其他' } as any)[c]
const detailLabel = (k: string) => ({ dosage: '剂量', route: '途径', startDate: '开始', endDate: '结束', surgeon: '术者', hospitalDays: '住院天数', side: '侧别' } as any)[k] || k

const timelineColor = (t: any) => ({ DONE: 'green', SKIPPED: 'gray', OVERDUE: 'red', PENDING: 'blue' } as any)[taskDerivedStatus(t)]
const overdueDays = (t: any) => Math.max(0, Math.round((new Date(TODAY).getTime() - new Date(t.dueDate).getTime()) / 86400000))

/* 评估曲线 */
const assessedCodes = computed(() => Array.from(new Set(f.value.assessments.map(a => a.scaleCode))))
const chartScale = ref('SNOT22')
const chartOption = computed(() => {
  const list = f.value.assessments.filter(a => a.scaleCode === chartScale.value)
  const s = scales.find(x => x.scaleCode === chartScale.value)
  const mcid = s?.definition.mcid
  const series: any[] = [{
    type: 'line', data: list.map(a => a.totalScore), smooth: true, symbolSize: 9,
    lineStyle: { width: 3 }, label: { show: true },
    areaStyle: { opacity: 0.1 },
  }]
  const markLine = mcid != null ? [{
    type: 'line', data: list.map(() => null),
    markLine: {
      silent: true, symbol: 'none',
      data: [{ yAxis: list[0] ? list[0].totalScore - mcid : 0, label: { formatter: `改善目标（基线−MCID ${mcid}）`, position: 'insideEndTop' }, lineStyle: { color: '#389e0d', type: 'dashed' } }],
    },
  }] : []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 40, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: list.map(a => `${a.stageName}\n${a.assessedAt}`) },
    yAxis: { type: 'value', max: s?.definition.maxScore, name: '得分' },
    series: [...series, ...markLine],
  }
})

/* 弹窗们 */
const closeOpen = ref(false)
const closeReason = ref('')
const upgradeOpen = ref(false)
const planFreeOpen = ref(false)
const planFreeScale = ref('SNOT22')
const treatOpen = ref(false)
const treatForm = ref<{ category: string; name: string; date: Dayjs | null }>({ category: 'MEDICATION', name: '', date: dayjs() })
const cdrOpen = ref(false)
const selectedRowKeys = ref<number[]>([])

const cdrColumns = [
  { title: '来源类型', dataIndex: 'resourceType', width: 160 },
  { title: '名称', dataIndex: 'name' },
  { title: '类别', key: 'category', width: 80 },
  { title: '发生日期', dataIndex: 'occurredDate', width: 110 },
  { title: '状态', key: 'exists', width: 120 },
]
function onCdrSelect(keys: number[]) { selectedRowKeys.value = keys }

function goWorkbench() { router.push({ name: 'ProtoCrsWorkbench' }) }
function goPatient360() { message2.info('跳转患者 360（既有页面）') }
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
