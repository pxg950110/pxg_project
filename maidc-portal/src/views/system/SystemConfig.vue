<template>
  <PageContainer title="系统配置" subtitle="管理系统全局参数与基础配置">
    <div class="config-cards">
      <div v-for="group in configGroups" :key="group.key" class="config-card">
        <div class="config-card-header">
          <component :is="group.icon" class="card-icon" />
          <span class="card-title">{{ group.title }}</span>
        </div>
        <div class="config-card-body">
          <div v-for="item in group.items" :key="item.label" class="config-row">
            <span class="config-label">{{ item.label }}</span>
            <div class="config-value-area">
              <template v-if="item.editing">
                <a-input
                  v-model:value="item.editValue"
                  size="small"
                  class="config-input"
                  @pressEnter="saveItem(item)"
                />
              </template>
              <template v-else>
                <span class="config-value">{{ item.value }}</span>
              </template>
            </div>
            <div class="config-action">
              <template v-if="item.editing">
                <a class="action-link save-link" @click="saveItem(item)">保存</a>
                <a class="action-link cancel-link" @click="cancelEdit(item)">取消</a>
              </template>
              <template v-else>
                <a class="action-link" @click="startEdit(item)">编辑</a>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import {
  SettingOutlined,
  DatabaseOutlined,
  SafetyCertificateOutlined,
  BellOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/index.vue'

interface ConfigItem {
  label: string
  value: string
  editValue: string
  editing: boolean
}

interface ConfigGroup {
  key: string
  title: string
  icon: typeof SettingOutlined
  items: ConfigItem[]
}

function createItem(label: string, value: string): ConfigItem {
  return reactive({ label, value, editValue: value, editing: false })
}

function startEdit(item: ConfigItem) {
  item.editValue = item.value
  item.editing = true
}

function cancelEdit(item: ConfigItem) {
  item.editValue = item.value
  item.editing = false
}

function saveItem(item: ConfigItem) {
  item.value = item.editValue
  item.editing = false
  message.success('配置保存成功')
}

const configGroups = reactive<ConfigGroup[]>([
  {
    key: 'basic',
    title: '基础配置',
    icon: SettingOutlined,
    items: [
      createItem('系统名称', 'MAIDC医疗AI数据中心'),
      createItem('系统版本', 'v1.0.0'),
      createItem('默认语言', '中文'),
      createItem('会话超时', '30分钟')
    ]
  },
  {
    key: 'storage',
    title: '存储配置',
    icon: DatabaseOutlined,
    items: [
      createItem('MinIO地址', 'http://minio:9000'),
      createItem('默认Bucket', 'maidc'),
      createItem('最大上传', '2GB')
    ]
  },
  {
    key: 'security',
    title: '安全配置',
    icon: SafetyCertificateOutlined,
    items: [
      createItem('密码最小长度', '8位'),
      createItem('两步验证', '开启'),
      createItem('登录失败锁定', '5次'),
      createItem('锁定时间', '24小时')
    ]
  },
  {
    key: 'notification',
    title: '通知配置',
    icon: BellOutlined,
    items: [
      createItem('SMTP服务器', 'smtp.example.com'),
      createItem('SMS服务', '阿里云SMS'),
      createItem('Webhook', 'https://hook.example.com')
    ]
  }
])
</script>

<style scoped>
.config-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.config-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.card-icon {
  font-size: 18px;
  color: #1890ff;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.config-card-body {
  padding: 0;
}

.config-row {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f5f5f5;
}

.config-row:last-child {
  border-bottom: none;
}

.config-label {
  flex-shrink: 0;
  width: 180px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
}

.config-value-area {
  flex: 1;
  min-width: 0;
}

.config-value {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}

.config-input {
  max-width: 320px;
}

.config-action {
  flex-shrink: 0;
  margin-left: 16px;
  display: flex;
  gap: 12px;
}

.action-link {
  font-size: 14px;
  color: #1890ff;
  cursor: pointer;
  white-space: nowrap;
}

.action-link:hover {
  color: #40a9ff;
}

.cancel-link {
  color: rgba(0, 0, 0, 0.45);
}

.cancel-link:hover {
  color: rgba(0, 0, 0, 0.65);
}
</style>
