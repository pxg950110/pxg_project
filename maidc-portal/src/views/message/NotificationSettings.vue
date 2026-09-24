<template>
  <PageContainer title="通知设置" subtitle="配置您的通知偏好和接收渠道。修改后立即生效。">
    <!-- Card 1: 通知渠道 -->
    <el-card shadow="never" class="settings-card !rounded-xl !border-slate-200/80 shadow-clinical-sm">
      <template #header>
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
            <el-icon :size="18" class="channel-icon"><component :is="item.icon" /></el-icon>
            <span class="channel-label">{{ item.label }}</span>
          </div>
          <el-switch :model-value="channels[item.key]" @change="(v: string | number | boolean) => (channels[item.key] = Boolean(v))" />
        </div>
      </div>
    </el-card>

    <!-- Card 2: 通知类型偏好 -->
    <el-card shadow="never" class="settings-card !rounded-xl !border-slate-200/80 shadow-clinical-sm" style="margin-top: 16px">
      <template #header>
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
            <el-checkbox :model-value="row.inApp" @change="(v: string | number | boolean) => (row.inApp = Boolean(v))" />
          </div>
          <div class="matrix-cell matrix-cell--check">
            <el-checkbox :model-value="row.email" @change="(v: string | number | boolean) => (row.email = Boolean(v))" />
          </div>
          <div class="matrix-cell matrix-cell--check">
            <el-checkbox :model-value="row.sms" @change="(v: string | number | boolean) => (row.sms = Boolean(v))" />
          </div>
        </div>
      </div>
    </el-card>

    <!-- Save Button -->
    <div class="save-bar">
      <el-button type="primary" @click="handleSave">
        <el-icon class="mr-1"><DocumentChecked /></el-icon>
        保存设置
      </el-button>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Message,
  ChatDotRound,
  Promotion,
  DocumentChecked,
} from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

// 通知渠道开关
const channels = reactive<Record<string, boolean>>({
  inApp: true,
  email: true,
  sms: false,
  webhook: true,
})

const channelList = [
  { key: 'inApp', label: '站内通知', icon: Bell },
  { key: 'email', label: '邮件通知', icon: Message },
  { key: 'sms', label: '短信通知', icon: ChatDotRound },
  { key: 'webhook', label: 'Webhook', icon: Promotion },
]

// 通知类型偏好矩阵
const typePreferences = reactive([
  { key: 'alert', label: '告警通知', inApp: true, email: true, sms: true },
  { key: 'approval', label: '审批通知', inApp: true, email: true, sms: false },
  { key: 'task', label: '任务通知', inApp: true, email: false, sms: false },
  { key: 'system', label: '系统通知', inApp: true, email: true, sms: true },
])

function handleSave() {
  ElMessage.success('通知设置已保存')
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
  border-bottom: 1px solid #f1f5f9;
}

.channel-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-icon {
  color: #64748b;
}

.channel-label {
  font-size: 14px;
  color: #0f172a;
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
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  font-weight: 600;
  font-size: 14px;
  color: #0f172a;
}

.matrix-row {
  display: flex;
  align-items: center;
  height: 44px;
  border-bottom: 1px solid #f1f5f9;
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
  color: #0f172a;
}

/* Save button */
.save-bar {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
