<template>
  <PageContainer title="系统总览" subtitle="MAIDC 医疗 AI 数据中心运行概览">
    <!-- Top Row: Metric Cards -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <MetricCard
          title="模型总数"
          :value="metrics.totalModels"
          suffix="个"
          :trend="{ value: 12, type: 'up' }"
          :loading="loading"
        >
          <template #icon><ExperimentOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="部署中"
          :value="metrics.deploying"
          suffix="个"
          :trend="{ value: 3, type: 'up' }"
          :loading="loading"
        >
          <template #icon><RocketOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="评估中"
          :value="metrics.evaluating"
          suffix="个"
          :loading="loading"
        >
          <template #icon><LineChartOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="6">
        <MetricCard
          title="告警数"
          :value="metrics.alerts"
          suffix="条"
          :trend="{ value: 5, type: 'down' }"
          :loading="loading"
        >
          <template #icon><AlertOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Second Row: Recent Activity + Quick Actions -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近活动" :bordered="false">
          <a-timeline>
            <a-timeline-item
              v-for="(item, index) in recentActivities"
              :key="index"
              :color="item.color"
            >
              <div class="timeline-item">
                <span class="timeline-title">{{ item.title }}</span>
                <span class="timeline-desc">{{ item.description }}</span>
                <span class="timeline-time">{{ item.time }}</span>
              </div>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="快捷入口" :bordered="false">
          <div class="quick-actions">
            <div
              v-for="(action, index) in quickActions"
              :key="index"
              class="quick-action-card"
              @click="action.handler"
            >
              <component :is="action.icon" class="quick-action-icon" :style="{ color: action.color }" />
              <span class="quick-action-label">{{ action.label }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Third Row: Charts -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="模型状态分布" :bordered="false">
          <MetricChart :option="modelStatusOption" :height="320" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="最近推理趋势" :bordered="false">
          <MetricChart :option="inferenceTrendOption" :height="320" />
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import {
  ExperimentOutlined,
  RocketOutlined,
  LineChartOutlined,
  AlertOutlined,
  PlusOutlined,
  ThunderboltOutlined,
  CloudUploadOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer'
import MetricCard from '@/components/MetricCard'
import MetricChart from '@/components/MetricChart'

const router = useRouter()
const loading = ref(false)

// ============ Metrics ============
const metrics = reactive({
  totalModels: 128,
  deploying: 23,
  evaluating: 15,
  alerts: 7,
})

// ============ Recent Activities ============
const recentActivities = ref([
  {
    title: '模型审批通过',
    description: '胸部CT诊断模型 v2.3 已通过审批',
    time: '10 分钟前',
    color: 'green',
  },
  {
    title: '评估任务完成',
    description: '心血管风险评估模型 AUC 达到 0.94',
    time: '32 分钟前',
    color: 'blue',
  },
  {
    title: '新模型部署',
    description: '糖尿病视网膜病变检测已上线生产环境',
    time: '1 小时前',
    color: 'green',
  },
  {
    title: '告警触发',
    description: 'GPU 集群 node-03 内存使用率超过 90%',
    time: '2 小时前',
    color: 'red',
  },
  {
    title: '数据集更新',
    description: '影像数据集 v3.0 已完成 ETL 导入',
    time: '3 小时前',
    color: 'blue',
  },
  {
    title: '模型审批通过',
    description: '病理切片分析模型 v1.0 已通过审批',
    time: '5 小时前',
    color: 'green',
  },
])

// ============ Quick Actions ============
const quickActions = [
  {
    label: '注册模型',
    icon: PlusOutlined,
    color: '#1677ff',
    handler: () => router.push('/model/list'),
  },
  {
    label: '创建评估',
    icon: LineChartOutlined,
    color: '#52c41a',
    handler: () => router.push('/model/evaluations'),
  },
  {
    label: '部署模型',
    icon: CloudUploadOutlined,
    color: '#722ed1',
    handler: () => router.push('/model/deployments'),
  },
  {
    label: '查看告警',
    icon: WarningOutlined,
    color: '#faad14',
    handler: () => router.push('/alert/active'),
  },
]

// ============ Chart Options ============
const modelStatusOption = {
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {d}%' },
      data: [
        { value: 45, name: '已部署', itemStyle: { color: '#1677ff' } },
        { value: 23, name: '部署中', itemStyle: { color: '#52c41a' } },
        { value: 15, name: '评估中', itemStyle: { color: '#faad14' } },
        { value: 30, name: '开发中', itemStyle: { color: '#d9d9d9' } },
        { value: 10, name: '已下线', itemStyle: { color: '#ff4d4f' } },
        { value: 5, name: '审批中', itemStyle: { color: '#722ed1' } },
      ],
    },
  ],
}

const inferenceTrendOption = {
  tooltip: { trigger: 'axis' },
  legend: { data: ['推理次数', '平均延迟(ms)'] },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  },
  yAxis: [
    { type: 'value', name: '推理次数' },
    { type: 'value', name: '延迟(ms)', splitLine: { show: false } },
  ],
  series: [
    {
      name: '推理次数',
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.15 },
      data: [2840, 3120, 3560, 3980, 4210, 3890, 4520],
      itemStyle: { color: '#1677ff' },
    },
    {
      name: '平均延迟(ms)',
      type: 'line',
      smooth: true,
      yAxisIndex: 1,
      data: [45, 42, 48, 38, 35, 40, 32],
      itemStyle: { color: '#52c41a' },
    },
  ],
}
</script>

<style scoped>
.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.timeline-title {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.timeline-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
}
.timeline-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}
.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.quick-action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 12px;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-action-card:hover {
  background: #e6f4ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
}
.quick-action-icon {
  font-size: 28px;
}
.quick-action-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  font-weight: 500;
}
</style>
