<template>
  <PageContainer>
    <!-- Header Row -->
    <div class="detail-header">
      <div class="detail-header-left">
        <el-icon class="back-icon" @click="handleGoBack"><ArrowLeft /></el-icon>
        <span class="detail-title">角色详情 - 模型管理员</span>
      </div>
      <div class="detail-header-right">
        <el-button>编辑角色</el-button>
      </div>
    </div>

    <!-- Two-Panel Layout -->
    <div class="two-panel-layout">
      <!-- Left Panel: 角色信息 -->
      <div class="left-panel">
        <div class="card">
          <div class="card-header">
            <span class="card-header-title">角色信息</span>
          </div>
          <div class="card-body">
            <div class="info-row">
              <span class="info-label">角色编码</span>
              <span class="info-value">MODEL_ADMIN</span>
            </div>
            <div class="info-row">
              <span class="info-label">角色名称</span>
              <span class="info-value">模型管理员</span>
            </div>
            <div class="info-row">
              <span class="info-label">描述</span>
              <span class="info-value">模型生命周期管理</span>
            </div>
            <div class="info-row last-row">
              <span class="info-label">创建时间</span>
              <span class="info-value">2026-01-01</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel -->
      <div class="right-panel">
        <!-- Card 1: 权限矩阵 -->
        <div class="card">
          <div class="card-header">
            <span class="card-header-title">权限矩阵</span>
          </div>
          <div class="card-body">
            <table class="permission-table">
              <thead>
                <tr>
                  <th class="col-module">模块</th>
                  <th class="col-action">查看</th>
                  <th class="col-action">创建</th>
                  <th class="col-action">编辑</th>
                  <th class="col-action">删除</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in permissionMatrix" :key="row.module">
                  <td class="col-module">{{ row.module }}</td>
                  <td class="col-action">
                    <span :class="row.view ? 'perm-yes' : 'perm-no'">{{ row.view ? '✓' : '✗' }}</span>
                  </td>
                  <td class="col-action">
                    <span :class="row.create ? 'perm-yes' : 'perm-no'">{{ row.create ? '✓' : '✗' }}</span>
                  </td>
                  <td class="col-action">
                    <span :class="row.edit ? 'perm-yes' : 'perm-no'">{{ row.edit ? '✓' : '✗' }}</span>
                  </td>
                  <td class="col-action">
                    <span :class="row.delete ? 'perm-yes' : 'perm-no'">{{ row.delete ? '✓' : '✗' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Card 2: 已分配用户 -->
        <div class="card" style="margin-top: 16px">
          <div class="card-header">
            <span class="card-header-title">已分配用户 (5人)</span>
          </div>
          <div class="card-body">
            <div
              v-for="(user, index) in assignedUsers"
              :key="user.name"
              :class="['user-row', { 'last-row': index === assignedUsers.length - 1 }]"
            >
              <div class="user-avatar" :style="{ background: user.color }">{{ user.initial }}</div>
              <span class="user-name">{{ user.name }}</span>
              <span class="user-dept">{{ user.department }}</span>
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
  router.push('/system/roles')
}

const permissionMatrix = [
  { module: '模型管理', view: true, create: true, edit: true, delete: false },
  { module: '部署管理', view: true, create: true, edit: true, delete: false },
  { module: '评估管理', view: true, create: false, edit: true, delete: false },
]

const assignedUsers = [
  { name: '张医生', initial: '张', department: '呼吸内科', color: '#0ea5e9' },
  { name: '李医生', initial: '李', department: '影像科', color: '#10b981' },
  { name: '王工程师', initial: '王', department: 'AI研发部', color: '#f59e0b' },
]
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

/* Info Rows */
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  gap: 16px;
}

.info-row.last-row {
  border-bottom: none;
}

.info-label {
  font-size: 14px;
  color: #64748b;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #000;
  text-align: right;
}

/* Permission Matrix Table */
.permission-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.permission-table th,
.permission-table td {
  text-align: center;
  height: 36px;
  font-size: 14px;
}

.permission-table th {
  font-weight: 600;
  color: #64748b;
  border-bottom: 1px solid #f1f5f9;
}

.permission-table td {
  color: #0f172a;
  border-bottom: 1px solid #f1f5f9;
}

.permission-table tbody tr:last-child td {
  border-bottom: none;
}

.col-module {
  text-align: left;
  width: auto;
  padding-left: 0;
}

.col-action {
  width: 64px;
}

.perm-yes {
  color: #10b981;
  font-weight: 600;
}

.perm-no {
  color: #ef4444;
  font-weight: 600;
}

/* User Rows */
.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.user-row.last-row {
  border-bottom: none;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  color: #0f172a;
}

.user-dept {
  font-size: 12px;
  color: #94a3b8;
}
</style>
