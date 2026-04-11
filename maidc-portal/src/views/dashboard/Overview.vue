<template>
  <PageContainer title="系统总览" subtitle="MAIDC 医疗 AI 数据中心运行概览">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="welcome-info">
        <h2 class="welcome-greeting">{{ greeting }}，{{ userName }}</h2>
        <p class="welcome-date">今天是{{ currentDate }}</p>
        <p class="welcome-tasks">
          您有 <span class="task-count">3</span> 条待办事项
        </p>
      </div>
      <div class="welcome-actions">
        <a-button type="primary" ghost @click="$router.push('/model/list')">
          <template #icon><PlusOutlined /></template>
          注册模型
        </a-button>
        <a-button type="primary" ghost @click="$router.push('/model/evaluations')">
          <template #icon><LineChartOutlined /></template>
          新建评估
        </a-button>
        <a-button type="primary" ghost @click="$router.push('/model/deployments')">
          <template #icon><AuditOutlined /></template>
          提交审批
        </a-button>
      </div>
    </div>

    <!-- Metric Cards Row: 6 cards -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="4">
        <MetricCard
          title="模型总数"
          :value="28"
          suffix="个"
          :trend="{ value: 12, type: 'up' }"
        >
          <template #icon><ExperimentOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="4">
        <MetricCard
          title="活跃部署"
          :value="8"
          suffix="个"
          :trend="{ value: 3, type: 'up' }"
        >
          <template #icon><RocketOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="4">
        <MetricCard
          title="今日推理次数"
          :value="12456"
          suffix="次"
          :trend="{ value: 8, type: 'up' }"
        >
          <template #icon><ThunderboltOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="4">
        <MetricCard
          title="患者记录"
          :value="156000"
          suffix="条"
          :trend="{ value: 5, type: 'up' }"
        >
          <template #icon><TeamOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="4">
        <MetricCard
          title="研究项目"
          :value="12"
          suffix="个"
        >
          <template #icon><ProjectOutlined /></template>
        </MetricCard>
      </a-col>
      <a-col :span="4">
        <MetricCard
          title="待审批"
          :value="5"
          suffix="条"
          :trend="{ value: 2, type: 'down' }"
        >
          <template #icon><AuditOutlined /></template>
        </MetricCard>
      </a-col>
    </a-row>

    <!-- Charts Row: Model Status + Recent Alerts -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="模型状态分布" :bordered="false">
          <template #extra>
            <a class="view-all-link" @click="$router.push('/model/list')">查看全部</a>
          </template>
          <MetricChart :option="modelStatusOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="最近告警" :bordered="false">
          <template #extra>
            <a class="view-all-link" @click="$router.push('/alert/active')">查看全部</a>
          </template>
          <div class="alert-list">
            <div
              v-for="(alert, index) in recentAlerts"
              :key="index"
              class="alert-item"
            >
              <div class="alert-left">
                <a-tag
                  :color="alert.severity === 'CRITICAL' ? 'red' : 'orange'"
                  class="alert-tag"
                >
                  {{ alert.severity }}
                </a-tag>
                <span class="alert-message">{{ alert.message }}</span>
              </div>
              <span class="alert-time">{{ alert.time }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- Activity Feed + Data Source Status -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近活动" :bordered="false">
          <template #extra>
            <a class="view-all-link" @click="$router.push('/audit/logs')">查看全部</a>
          </template>
          <div class="activity-list">
            <div
              v-for="(item, index) in recentActivities"
              :key="index"
              class="activity-item"
            >
              <span class="activity-dot" :style="{ background: item.dotColor }"></span>
              <span class="activity-text">{{ item.text }}</span>
              <a-tag :color="item.tagColor" class="activity-tag">{{ item.category }}</a-tag>
              <span class="activity-time">{{ item.time }}</span>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="数据源连接状态" :bordered="false">
          <div class="datasource-list">
            <div
              v-for="(ds, index) in dataSources"
              :key="index"
              class="datasource-item"
            >
              <div class="datasource-info">
                <span class="datasource-name">{{ ds.name }}</span>
                <span class="datasource-desc">{{ ds.description }}</span>
              </div>
              <a-badge
                :status="ds.connected ? 'success' : 'error'"
                :text="ds.connected ? '已连接' : '断开'"
              />
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ExperimentOutlined,
  RocketOutlined,
  LineChartOutlined,
  PlusOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  ProjectOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'
import MetricCard from '@/components/MetricCard/index.vue'
import MetricChart from '@/components/MetricChart/index.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// ============ Welcome Section ============
const userName = computed(() => authStore.userInfo?.realName || '医生')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '晚上好'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const currentDate = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  return `${year}年${month}月${day}日`
})

// ============ Recent Alerts ============
const recentAlerts = [
  { severity: 'CRITICAL', message: '推理延迟P99超阈值', time: '10分钟前' },
  { severity: 'WARNING', message: 'GPU使用率 > 90%', time: '1小时前' },
  { severity: 'WARNING', message: '磁盘空间 < 20%', time: '3小时前' },
  { severity: 'WARNING', message: '模型推理服务响应时间增加', time: '5小时前' },
]

// ============ Recent Activity ============
const recentActivities = [
  { text: '胸部CT诊断模型 v2.3 已通过审批', category: '审批', tagColor: 'purple', dotColor: '#722ed1', time: '10分钟前' },
  { text: '推理延迟P99超过阈值，已触发告警', category: '告警', tagColor: 'red', dotColor: '#ff4d4f', time: '15分钟前' },
  { text: '心血管风险评估模型评估完成 AUC=0.94', category: '评估', tagColor: 'blue', dotColor: '#1677ff', time: '32分钟前' },
  { text: '糖尿病视网膜病变检测模型已部署上线', category: '部署', tagColor: 'green', dotColor: '#52c41a', time: '1小时前' },
  { text: '影像数据集 v3.0 ETL 导入完成', category: 'ETL', tagColor: 'cyan', dotColor: '#13c2c2', time: '2小时前' },
  { text: '病理切片分析模型 v1.0 注册成功', category: '模型', tagColor: 'geekblue', dotColor: '#2f54eb', time: '3小时前' },
  { text: 'GPU集群 node-03 内存使用率超过90%', category: '告警', tagColor: 'red', dotColor: '#ff4d4f', time: '4小时前' },
  { text: '电子病历数据ETL任务执行完成', category: 'ETL', tagColor: 'cyan', dotColor: '#13c2c2', time: '5小时前' },
]

// ============ Data Sources ============
const dataSources = [
  { name: 'HIS 系统', description: '医院信息系统', connected: true },
  { name: 'LIS 检验系统', description: '实验室信息管理', connected: true },
  { name: 'PACS 影像系统', description: '医学影像存档与传输', connected: true },
  { name: 'EMR 电子病历', description: '电子病历系统', connected: false },
]

// ============ Chart: Model Status Horizontal Bar ============
const modelStatusOption = {
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '10%', bottom: '3%', top: '3%', containLabel: true },
  xAxis: { type: 'value', boundaryGap: [0, 0.1] },
  yAxis: {
    type: 'category',
    data: ['DRAFT', 'REGISTERED', 'PUBLISHED', 'DEPRECATED'],
    axisLabel: { fontSize: 13 },
  },
  series: [
    {
      type: 'bar',
      barWidth: 28,
      label: {
        show: true,
        position: 'right',
        formatter: '{c}个',
        fontSize: 13,
        color: 'rgba(0,0,0,0.65)',
      },
      data: [
        { value: 3, itemStyle: { color: '#d9d9d9' } },
        { value: 2, itemStyle: { color: '#1677ff' } },
        { value: 18, itemStyle: { color: '#52c41a' } },
        { value: 5, itemStyle: { color: '#faad14' } },
      ],
    },
  ],
}
</script>

<style scoped>
/* Welcome Banner */
.welcome-banner {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border-radius: 12px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
}
.welcome-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.welcome-greeting {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: #fff;
}
.welcome-date {
  font-size: 14px;
  margin: 0;
  opacity: 0.85;
}
.welcome-tasks {
  font-size: 14px;
  margin: 0;
  margin-top: 4px;
  opacity: 0.9;
}
.task-count {
  font-size: 18px;
  font-weight: 600;
}
.welcome-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
.welcome-actions .ant-btn-background-ghost {
  color: #fff !important;
  border-color: rgba(255, 255, 255, 0.6) !important;
}
.welcome-actions .ant-btn-background-ghost:hover {
  border-color: #fff !important;
  background: rgba(255, 255, 255, 0.15) !important;
}

/* View-all links */
.view-all-link {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
}
.view-all-link:hover {
  color: #4096ff;
}

/* Alert List */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.alert-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}
.alert-item:last-child {
  border-bottom: none;
}
.alert-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.alert-tag {
  flex-shrink: 0;
  font-weight: 500;
}
.alert-message {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.alert-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  flex-shrink: 0;
  margin-left: 16px;
}

/* Activity Feed */
.activity-list {
  display: flex;
  flex-direction: column;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.activity-item:last-child {
  border-bottom: none;
}
.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.activity-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.75);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.activity-tag {
  flex-shrink: 0;
  border-radius: 4px;
}
.activity-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  flex-shrink: 0;
  margin-left: 8px;
}

/* Data Source Status */
.datasource-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.datasource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}
.datasource-item:last-child {
  border-bottom: none;
}
.datasource-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.datasource-name {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.datasource-desc {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
