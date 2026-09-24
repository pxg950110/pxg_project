<template>
  <PageContainer>
    <!-- Header Row -->
    <div class="detail-header">
      <div class="detail-header-left">
        <el-icon class="back-icon" @click="handleGoBack"><ArrowLeft /></el-icon>
        <span class="detail-title">用户详情 - 张医生</span>
      </div>
      <div class="detail-header-right">
        <el-button>编辑</el-button>
        <el-button class="btn-warning-outline">重置密码</el-button>
      </div>
    </div>

    <!-- Two-Panel Layout -->
    <div class="two-panel-layout">
      <!-- Left Panel: 基本信息 -->
      <div class="left-panel">
        <div class="card">
          <div class="card-header">
            <span class="card-header-title">基本信息</span>
          </div>
          <div class="card-body">
            <!-- Avatar Section -->
            <div class="avatar-section">
              <div class="avatar-circle">张</div>
              <div class="avatar-info">
                <span class="avatar-name">张医生</span>
                <span class="avatar-email">zhangsan@hospital.cn</span>
              </div>
            </div>

            <!-- Info Rows -->
            <div class="info-row">
              <span class="info-label">用户名</span>
              <span class="info-value">zhangsan</span>
            </div>
            <div class="info-row">
              <span class="info-label">姓名</span>
              <span class="info-value">张医生</span>
            </div>
            <div class="info-row">
              <span class="info-label">邮箱</span>
              <span class="info-value">zhangsan@hospital.cn</span>
            </div>
            <div class="info-row">
              <span class="info-label">手机</span>
              <span class="info-value">138****1234</span>
            </div>
            <div class="info-row last-row">
              <span class="info-label">组织</span>
              <span class="info-value">放射科</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel -->
      <div class="right-panel">
        <!-- Card 1: 角色与权限 -->
        <div class="card">
          <div class="card-header">
            <span class="card-header-title">角色与权限</span>
          </div>
          <div class="card-body">
            <div class="role-row">
              <el-tag type="primary">AI工程师</el-tag>
              <span class="role-desc">模型开发与部署</span>
            </div>
            <div class="role-row last-row">
              <el-tag type="success">临床医生</el-tag>
              <span class="role-desc">AI辅助诊断使用者</span>
            </div>
          </div>
        </div>

        <!-- Card 2: 最近操作 -->
        <div class="card" style="margin-top: 16px">
          <div class="card-header">
            <span class="card-header-title">最近操作</span>
          </div>
          <div class="card-body">
            <div class="activity-row">
              <span class="activity-desc">部署模型 CT肺结节检测v3</span>
              <span class="activity-time">2小时前</span>
            </div>
            <div class="activity-row">
              <span class="activity-desc">提交审批申请 APR-2026-0015</span>
              <span class="activity-time">昨天 10:00</span>
            </div>
            <div class="activity-row last-row">
              <span class="activity-desc">上传版本 v3.2.1</span>
              <span class="activity-time">昨天 09:15</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import PageContainer from '@/components/PageContainer/index.vue'

const router = useRouter()

function handleGoBack() {
  router.push('/system/users')
}
</script>

<style scoped>
/* Header */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.detail-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-icon {
  font-size: 18px;
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}

.back-icon:hover {
  color: #0ea5e9;
}

.detail-title {
  font-size: 22px;
  font-weight: 600;
  color: #0f172a;
}

.detail-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-warning-outline {
  color: #f97316;
  border-color: #f97316;
}

.btn-warning-outline:hover {
  color: #c2410c;
  border-color: #c2410c;
}

/* Two-Panel Layout */
.two-panel-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.left-panel {
  width: 520px;
  flex-shrink: 0;
}

.right-panel {
  flex: 1;
  min-width: 0;
}

/* Card */
.card {
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.card-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.card-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.card-body {
  padding: 16px;
}

/* Avatar Section */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.avatar-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #0ea5e9;
  color: #fff;
  font-size: 20px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.avatar-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.avatar-email {
  font-size: 13px;
  color: #94a3b8;
}

/* Info Rows */
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.info-row.last-row {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #64748b;
}

.info-value {
  font-size: 14px;
  color: #000;
}

/* Role Rows */
.role-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
}

.role-row.last-row {
  border-bottom: none;
}

.role-desc {
  font-size: 14px;
  color: #64748b;
}

/* Activity Rows */
.activity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.activity-row.last-row {
  border-bottom: none;
}

.activity-desc {
  font-size: 14px;
  color: #0f172a;
}

.activity-time {
  font-size: 13px;
  color: #94a3b8;
  flex-shrink: 0;
}
</style>
