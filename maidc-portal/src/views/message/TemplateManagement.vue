<template>
  <PageContainer>
    <template #default>
      <div class="template-page-header">
        <div class="template-page-header-left">
          <h2 class="template-page-title">消息模板管理</h2>
        </div>
        <div class="template-page-header-right">
          <el-button type="primary" @click="handleCreate">
            <el-icon class="mr-1"><Plus /></el-icon> 新建模板
          </el-button>
        </div>
      </div>

      <el-card shadow="never" class="!rounded-xl !border-slate-200/80 shadow-clinical-sm">
        <el-table :data="templates" row-key="id" size="default">
          <el-table-column label="模板名称" prop="name" min-width="140" />
          <el-table-column label="模板编码" prop="code" min-width="150">
            <template #default="{ row }">
              <span class="text-muted">{{ row.code }}</span>
            </template>
          </el-table-column>
          <el-table-column label="消息类型" prop="type" width="120">
            <template #default="{ row }">
              <span :style="{ color: row.typeColor || '#94a3b8' }">{{ row.type }}</span>
            </template>
          </el-table-column>
          <el-table-column label="通知渠道" prop="channel" min-width="160">
            <template #default="{ row }">
              <span class="text-muted">{{ row.channel }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span v-if="row.enabled" style="color: #10b981">已启用</span>
              <span v-else class="text-muted">已禁用</span>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" prop="updatedAt" width="140">
            <template #default="{ row }">
              <span class="text-muted">{{ row.updatedAt }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
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

const templates = ref<Template[]>([
  { id: 1, name: '告警通知模板', code: 'TPL_ALERT_001', type: '告警通知', typeColor: '#ef4444', channel: '邮件/短信/Webhook', enabled: true, updatedAt: '2026-04-08' },
  { id: 2, name: '审批通知模板', code: 'TPL_APPROVAL_001', type: '审批通知', typeColor: '#0ea5e9', channel: '邮件/站内信', enabled: true, updatedAt: '2026-04-05' },
  { id: 3, name: '任务完成通知', code: 'TPL_TASK_001', type: '任务通知', typeColor: '#10b981', channel: '邮件/站内信', enabled: true, updatedAt: '2026-04-03' },
  { id: 4, name: '系统维护通知', code: 'TPL_SYSTEM_001', type: '系统通知', typeColor: '', channel: '全渠道', enabled: true, updatedAt: '2026-03-20' },
  { id: 5, name: '数据质量告警', code: 'TPL_QUALITY_001', type: '系统通知', typeColor: '', channel: '邮件', enabled: false, updatedAt: '2026-03-15' },
])

function handleCreate() {
  ElMessage.info('新建模板')
}

function handleEdit(record: Template) {
  ElMessage.info('编辑模板')
}

function handlePreview(record: Template) {
  ElMessage.info('预览模板')
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
  color: #0f172a;
  margin: 0;
}

.template-page-header-right {
  display: flex;
  align-items: center;
}

.text-muted {
  color: #94a3b8;
}
</style>
