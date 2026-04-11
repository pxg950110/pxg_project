<template>
  <PageContainer>
    <template #extra>
      <div class="detail-header">
        <div class="detail-header-left">
          <ArrowLeftOutlined class="back-icon" @click="goBack" />
          <span class="detail-title">消息详情</span>
        </div>
      </div>
    </template>

    <a-card class="message-card" :bordered="false">
      <!-- Card Header -->
      <div class="msg-card-header">
        <div class="msg-card-header-left">
          <a-tag :color="msgData.typeColor">{{ msgData.typeLabel }}</a-tag>
          <span class="msg-title-text">{{ msgData.title }}</span>
        </div>
        <span class="msg-time">{{ msgData.time }}</span>
      </div>

      <!-- Card Body -->
      <div class="msg-card-body">
        <div class="msg-content">{{ msgData.content }}</div>

        <a-divider />

        <div class="msg-details">
          <div class="msg-details-title">附加信息</div>
          <div class="msg-detail-row">
            <span class="msg-detail-label">发送人</span>
            <span class="msg-detail-value">{{ msgData.sender }}</span>
          </div>
          <div class="msg-detail-row">
            <span class="msg-detail-label">优先级</span>
            <span class="msg-detail-value">{{ msgData.priority }}</span>
          </div>
          <div class="msg-detail-row">
            <span class="msg-detail-label">关联资源</span>
            <span class="msg-detail-value">{{ msgData.resource }}</span>
          </div>
        </div>
      </div>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

const router = useRouter()

const goBack = () => {
  router.push('/message/list')
}

const msgData = {
  id: 1,
  type: 'SYSTEM',
  typeLabel: '系统通知',
  typeColor: 'blue',
  title: '模型部署完成通知',
  time: '2026-04-12 14:30:00',
  content:
    '您的模型"肺结节检测v3.3"已成功部署至生产环境。\n\n部署详情：\n- 部署实例：4个\n- 服务端口：8080\n- 健康检查：通过\n- 平均延迟：23ms\n\n请前往部署管理页面查看详细运行状态。',
  sender: '系统',
  priority: '普通',
  resource: '模型-肺结节检测-v3.3',
}
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.detail-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-icon {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
  transition: color 0.2s;
}

.back-icon:hover {
  color: #1677ff;
}

.detail-title {
  font-size: 22px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.message-card {
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.msg-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.msg-card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.msg-title-text {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
}

.msg-time {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.msg-card-body {
  padding: 20px;
}

.msg-content {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(0, 0, 0, 0.75);
  white-space: pre-line;
}

.msg-details {
  margin-top: 4px;
}

.msg-details-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.88);
  margin-bottom: 12px;
}

.msg-detail-row {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.msg-detail-row + .msg-detail-row {
  border-top: 1px solid #fafafa;
}

.msg-detail-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
}

.msg-detail-value {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}
</style>
