<template>
  <PageContainer>
    <template #default>
      <div class="template-page-header">
        <div class="template-page-header-left">
          <h2 class="template-page-title">消息模板管理</h2>
        </div>
        <div class="template-page-header-right">
          <a-button type="primary" @click="handleCreate">
            <PlusOutlined /> 新建模板
          </a-button>
        </div>
      </div>

      <a-table
        :columns="columns"
        :data-source="templates"
        row-key="id"
        size="middle"
        :pagination="false"
        :custom-header-row="() => ({ class: 'template-table-header' })"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span>{{ record.name }}</span>
          </template>
          <template v-if="column.key === 'code'">
            <span class="text-muted">{{ record.code }}</span>
          </template>
          <template v-if="column.key === 'type'">
            <span :style="{ color: record.typeColor || 'rgba(0, 0, 0, 0.45)' }">{{ record.type }}</span>
          </template>
          <template v-if="column.key === 'channel'">
            <span class="text-muted">{{ record.channel }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <span v-if="record.enabled" style="color: #52c41a">已启用</span>
            <span v-else class="text-muted">已禁用</span>
          </template>
          <template v-if="column.key === 'updatedAt'">
            <span class="text-muted">{{ record.updatedAt }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a class="action-link" @click="handleEdit(record)">编辑</a>
              <a class="action-link" @click="handlePreview(record)">预览</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

interface Template {
  id: number
  name: string
  code: string
  type: string
  typeColor: string
  channel: string
  enabled: boolean
  updatedAt: string
}

const columns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '模板编码', dataIndex: 'code', key: 'code' },
  { title: '消息类型', dataIndex: 'type', key: 'type' },
  { title: '通知渠道', dataIndex: 'channel', key: 'channel' },
  { title: '状态', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 140 },
  { title: '操作', key: 'action', width: 120, align: 'right' as const },
]

const templates = ref<Template[]>([
  { id: 1, name: '告警通知模板', code: 'TPL_ALERT_001', type: '告警通知', typeColor: '#ff4d4f', channel: '邮件/短信/Webhook', enabled: true, updatedAt: '2026-04-08' },
  { id: 2, name: '审批通知模板', code: 'TPL_APPROVAL_001', type: '审批通知', typeColor: '#1677ff', channel: '邮件/站内信', enabled: true, updatedAt: '2026-04-05' },
  { id: 3, name: '任务完成通知', code: 'TPL_TASK_001', type: '任务通知', typeColor: '#52c41a', channel: '邮件/站内信', enabled: true, updatedAt: '2026-04-03' },
  { id: 4, name: '系统维护通知', code: 'TPL_SYSTEM_001', type: '系统通知', typeColor: '', channel: '全渠道', enabled: true, updatedAt: '2026-03-20' },
  { id: 5, name: '数据质量告警', code: 'TPL_QUALITY_001', type: '系统通知', typeColor: '', channel: '邮件', enabled: false, updatedAt: '2026-03-15' },
])

function handleCreate() {
  message.info('新建模板')
}

function handleEdit(record: Template) {
  message.info('编辑模板')
}

function handlePreview(record: Template) {
  message.info('预览模板')
}
</script>

<style scoped>
.template-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.template-page-header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.template-page-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin: 0;
}

.template-page-header-right {
  display: flex;
  align-items: center;
}

.text-muted {
  color: rgba(0, 0, 0, 0.45);
}

.action-link {
  color: #1677ff;
  cursor: pointer;
  transition: color 0.2s;
}

.action-link:hover {
  color: #4096ff;
}

:deep(.template-table-header) {
  background: #f9fafb;
}

:deep(.template-table-header > th) {
  background: #f9fafb;
}
</style>
