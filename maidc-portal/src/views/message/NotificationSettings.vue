<template>
  <PageContainer title="通知设置" subtitle="配置您的通知偏好和接收渠道。修改后立即生效。">
    <!-- Card 1: 通知渠道 -->
    <a-card class="settings-card">
      <template #title>
        <span class="card-title">通知渠道</span>
      </template>
      <div class="channel-list">
        <div
          v-for="(item, index) in channelList"
          :key="item.key"
          class="channel-row"
          :class="{ 'channel-row--bordered': index < channelList.length - 1 }"
        >
          <div class="channel-left">
            <component :is="item.icon" class="channel-icon" />
            <span class="channel-label">{{ item.label }}</span>
          </div>
          <a-switch :checked="channels[item.key]" @change="(v: boolean) => channels[item.key] = v" />
        </div>
      </div>
    </a-card>

    <!-- Card 2: 通知类型偏好 -->
    <a-card class="settings-card" style="margin-top: 16px">
      <template #title>
        <span class="card-title">通知类型偏好</span>
      </template>
      <div class="preference-matrix">
        <div class="matrix-header">
          <div class="matrix-cell matrix-cell--type">通知类型</div>
          <div class="matrix-cell matrix-cell--check">站内</div>
          <div class="matrix-cell matrix-cell--check">邮件</div>
          <div class="matrix-cell matrix-cell--check">短信</div>
        </div>
        <div
          v-for="row in typePreferences"
          :key="row.key"
          class="matrix-row"
        >
          <div class="matrix-cell matrix-cell--type">
            <span class="row-label">{{ row.label }}</span>
          </div>
          <div class="matrix-cell matrix-cell--check">
            <a-checkbox :checked="row.inApp" @change="(e: any) => row.inApp = e.target.checked" />
          </div>
          <div class="matrix-cell matrix-cell--check">
            <a-checkbox :checked="row.email" @change="(e: any) => row.email = e.target.checked" />
          </div>
          <div class="matrix-cell matrix-cell--check">
            <a-checkbox :checked="row.sms" @change="(e: any) => row.sms = e.target.checked" />
          </div>
        </div>
      </div>
    </a-card>

    <!-- Save Button -->
    <div class="save-bar">
      <a-button type="primary" @click="handleSave">
        <template #icon><SaveOutlined /></template>
        保存设置
      </a-button>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import {
  BellOutlined,
  MailOutlined,
  MessageOutlined,
  SendOutlined,
  SaveOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

// 通知渠道开关
const channels = reactive<Record<string, boolean>>({
  inApp: true,
  email: true,
  sms: false,
  webhook: true,
})

const channelList = [
  { key: 'inApp', label: '站内通知', icon: BellOutlined },
  { key: 'email', label: '邮件通知', icon: MailOutlined },
  { key: 'sms', label: '短信通知', icon: MessageOutlined },
  { key: 'webhook', label: 'Webhook', icon: SendOutlined },
]

// 通知类型偏好矩阵
const typePreferences = reactive([
  { key: 'alert', label: '告警通知', inApp: true, email: true, sms: true },
  { key: 'approval', label: '审批通知', inApp: true, email: true, sms: false },
  { key: 'task', label: '任务通知', inApp: true, email: false, sms: false },
  { key: 'system', label: '系统通知', inApp: true, email: true, sms: true },
])

function handleSave() {
  message.success('通知设置已保存')
}
</script>

<style scoped>
.settings-card {
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

/* Channel rows */
.channel-list {
  display: flex;
  flex-direction: column;
}

.channel-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.channel-row--bordered {
  border-bottom: 1px solid #f0f0f0;
}

.channel-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-icon {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.65);
}

.channel-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}

/* Preference matrix */
.preference-matrix {
  display: flex;
  flex-direction: column;
}

.matrix-header {
  display: flex;
  align-items: center;
  height: 44px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 600;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}

.matrix-row {
  display: flex;
  align-items: center;
  height: 44px;
  border-bottom: 1px solid #f0f0f0;
}

.matrix-row:last-child {
  border-bottom: none;
}

.matrix-cell {
  padding: 0 12px;
}

.matrix-cell--type {
  flex: 1;
}

.matrix-cell--check {
  width: 80px;
  text-align: center;
}

.row-label {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}

/* Save button */
.save-bar {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
