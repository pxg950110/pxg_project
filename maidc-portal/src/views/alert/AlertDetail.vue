<template>
  <PageContainer title="告警详情">
    <template #extra>
      <a-button @click="router.back()">返回</a-button>
    </template>

    <div class="alert-detail-panels">
      <a-row :gutter="16">
        <!-- Left Panel: Alert Info -->
        <a-col :span="14">
          <div class="panel-card">
            <div class="alert-header">
              <span class="alert-name">推理延迟过高</span>
              <a-tag color="red">严重</a-tag>
            </div>
            <a-descriptions :column="2" bordered>
              <a-descriptions-item label="规则名称">推理延迟监控</a-descriptions-item>
              <a-descriptions-item label="触发时间">2026-04-12 10:30:00</a-descriptions-item>
              <a-descriptions-item label="当前值">850ms</a-descriptions-item>
              <a-descriptions-item label="阈值">&gt;500ms</a-descriptions-item>
              <a-descriptions-item label="关联模型/部署">肺结节检测-v2</a-descriptions-item>
              <a-descriptions-item label="通知方式">邮件 + 钉钉</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-col>

        <!-- Right Panel: Processing Timeline -->
        <a-col :span="10">
          <div class="panel-card">
            <div class="panel-title">处理时间线</div>
            <a-timeline>
              <a-timeline-item color="green">
                <div class="timeline-title">告警触发</div>
                <div class="timeline-desc">推理延迟超过阈值 (850ms &gt; 500ms)</div>
                <div class="timeline-time">2026-04-12 10:30:00</div>
              </a-timeline-item>
              <a-timeline-item color="blue">
                <div class="timeline-title">通知已发送</div>
                <div class="timeline-desc">已通知: 李医生(邮件), 张主任(钉钉)</div>
                <div class="timeline-time">2026-04-12 10:31:15</div>
              </a-timeline-item>
              <a-timeline-item color="orange">
                <div class="timeline-title">等待处理</div>
                <div class="timeline-desc">等待相关人员确认处理</div>
                <div class="timeline-time">2026-04-12 10:35:00</div>
              </a-timeline-item>
            </a-timeline>
          </div>
        </a-col>
      </a-row>

      <!-- Action Buttons -->
      <div class="action-bar">
        <a-button type="primary" @click="handleConfirm">确认处理</a-button>
        <a-button @click="handleFalseAlarm">标记误报</a-button>
        <a-button @click="router.back()">返回</a-button>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'

const router = useRouter()
const route = useRoute()

// Mock: use route params id for future API integration
const alertId = route.params.id

function handleConfirm() {
  message.success('告警已确认处理')
}

function handleFalseAlarm() {
  message.info('已标记为误报')
}
</script>

<style scoped>
.alert-detail-panels {
  margin-top: 16px;
}
.panel-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  height: 100%;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 20px;
}
.alert-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.alert-name {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}
.timeline-title {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.88);
}
.timeline-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin-top: 4px;
}
.timeline-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
  margin-top: 4px;
}
.action-bar {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
