<template>
  <PageContainer title="部署监控" subtitle="实时监控所有模型部署状态与推理性能">
    <!-- Time Filter + Auto-refresh -->
    <div class="filter-bar">
      <a-radio-group v-model:value="timeRange" button-style="solid">
        <a-radio-button value="1h">近1h</a-radio-button>
        <a-radio-button value="6h">近6h</a-radio-button>
        <a-radio-button value="24h">近24h</a-radio-button>
        <a-radio-button value="7d">近7d</a-radio-button>
      </a-radio-group>
      <div class="auto-refresh">
        <span class="refresh-dot"></span>
        <span>自动刷新 30s</span>
      </div>
    </div>

    <!-- 4 Metric Cards -->
    <a-row :gutter="16" class="metric-row">
      <a-col :span="6">
        <MetricCard title="部署实例" :value="45" :icon="RocketOutlined" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="总推理次数" :value="128456" :icon="ThunderboltOutlined" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="平均延迟" value="245" suffix="ms" :icon="ClockCircleOutlined" />
      </a-col>
      <a-col :span="6">
        <MetricCard title="GPU利用率" value="67" suffix="%" :icon="DashboardOutlined" />
      </a-col>
    </a-row>

    <!-- QPS Trend Chart -->
    <a-card title="QPS 趋势" class="section-card">
      <MetricChart :option="qpsChartOption" height="320px" />
    </a-card>

    <!-- Deployment Status Panel -->
    <a-card title="部署状态" class="section-card">
      <div class="deployment-list">
        <div v-for="item in deployments" :key="item.name" class="deployment-item">
          <div class="deployment-left">
            <span class="status-dot" :style="{ backgroundColor: item.color }"></span>
            <div class="deployment-info">
              <span class="deployment-name">{{ item.name }} <span class="deployment-version">{{ item.version }}</span></span>
              <span class="deployment-detail">{{ item.status }} &middot; {{ item.detail }}</span>
            </div>
          </div>
          <span class="status-badge" :style="{ color: item.color, borderColor: item.color }">{{ item.status }}</span>
        </div>
      </div>
    </a-card>

    <!-- Alert Table -->
    <a-card title="告警规则" class="section-card">
      <template #extra>
        <a-button type="primary" size="small">
          <PlusOutlined /> 新建规则
        </a-button>
      </template>
      <a-table :columns="alertColumns" :data-source="alerts" :pagination="false" row-key="rule" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColorMap[record.status]">{{ record.status }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a>编辑</a>
              <a>禁用</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import {
  RocketOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'

// Time range filter
const timeRange = ref<string>('24h')

// QPS chart option
const qpsChartOption = {
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
  },
  yAxis: { type: 'value', name: 'QPS' },
  series: [
    {
      type: 'bar',
      data: [120, 85, 340, 580, 620, 450, 380],
      itemStyle: { color: '#1677ff', borderRadius: [4, 4, 0, 0] },
    },
  ],
}

// Deployment status items
const deployments = [
  { name: '肺结节检测-生产', version: 'v2.1.0', status: 'Running', color: '#52c41a', detail: 'QPS: 56' },
  { name: '病理分类模型', version: 'v2.0.1', status: 'Stopped', color: '#ff4d4f', detail: '2小时前' },
  { name: 'NLP命名实体识别', version: 'v1.0.0', status: 'Error', color: '#faad14', detail: 'OOM异常' },
]

// Alert table columns
const alertColumns = [
  { title: '规则名称', dataIndex: 'rule', key: 'rule' },
  { title: '部署', dataIndex: 'deployment', key: 'deployment' },
  { title: '指标', dataIndex: 'metric', key: 'metric' },
  { title: '阈值', dataIndex: 'threshold', key: 'threshold' },
  { title: '当前值', dataIndex: 'current', key: 'current' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '操作', key: 'action', width: 120 },
]

// Alert mock data
const alerts = [
  { rule: '推理延迟过高', deployment: '肺结节检测-生产', metric: 'P99延迟', threshold: '500ms', current: '892ms', status: 'Firing', time: '5分钟前' },
  { rule: 'GPU内存使用率', deployment: '病理分类模型', metric: 'GPU使用率', threshold: '90%', current: '85%', status: 'Warning', time: '15分钟前' },
  { rule: '请求错误率超标', deployment: 'NLP命名实体识别', metric: '错误率', threshold: '1%', current: '0.3%', status: 'Resolved', time: '1小时前' },
  { rule: 'QPS突降告警', deployment: '肺结节检测-生产', metric: 'QPS', threshold: '>10', current: '56', status: 'Resolved', time: '3小时前' },
]

// Status badge color mapping
const statusColorMap: Record<string, string> = {
  Firing: 'red',
  Warning: 'orange',
  Resolved: 'green',
}
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
}

.refresh-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #52c41a;
  display: inline-block;
}

.metric-row {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
}

.deployment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.deployment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  transition: box-shadow 0.2s;
}

.deployment-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.deployment-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.deployment-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.deployment-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.88);
}

.deployment-version {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-weight: 400;
}

.deployment-detail {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid;
  line-height: 20px;
}
</style>
