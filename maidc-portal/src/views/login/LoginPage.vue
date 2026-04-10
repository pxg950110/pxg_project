<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand-section">
          <div class="brand-logo">
            <img src="@/assets/logo.svg" alt="MAIDC" />
          </div>
          <h1 class="brand-title">MAIDC</h1>
          <p class="brand-subtitle">医疗 AI 数据中心</p>
          <div class="feature-list">
            <div class="feature-item">
              <SafetyCertificateOutlined />
              <span>模型全生命周期管理</span>
            </div>
            <div class="feature-item">
              <DatabaseOutlined />
              <span>临床数据治理与脱敏</span>
            </div>
            <div class="feature-item">
              <ExperimentOutlined />
              <span>AI 模型训练与评估</span>
            </div>
            <div class="feature-item">
              <MonitorOutlined />
              <span>实时监控与告警</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-form-wrapper">
          <h2 class="form-title">欢迎登录</h2>
          <p class="form-subtitle">请输入您的账号和密码</p>
          <a-form
            :model="formState"
            :rules="rules"
            ref="formRef"
            @finish="handleLogin"
            layout="vertical"
            size="large"
          >
            <a-form-item name="username" label="用户名">
              <a-input
                v-model:value="formState.username"
                placeholder="请输入用户名"
                :prefix="h(UserOutlined)"
                autocomplete="username"
              />
            </a-form-item>
            <a-form-item name="password" label="密码">
              <a-input-password
                v-model:value="formState.password"
                placeholder="请输入密码"
                :prefix="h(LockOutlined)"
                autocomplete="current-password"
                @pressEnter="handleLogin"
              />
            </a-form-item>
            <a-form-item>
              <div class="form-extra">
                <a-checkbox v-model:checked="formState.remember">记住我</a-checkbox>
              </div>
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                :loading="loading"
                block
                class="login-btn"
              >
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </div>
      </div>
    </div>
    <div class="login-footer">
      <span>&copy; 2026 MAIDC - 医疗 AI 数据中心平台</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { UserOutlined, LockOutlined, SafetyCertificateOutlined, DatabaseOutlined, ExperimentOutlined, MonitorOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const formState = reactive({
  username: '',
  password: '',
  remember: true,
})

const rules: Record<string, Rule[]> = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }],
}

async function handleLogin() {
  try {
    loading.value = true
    await authStore.loginAction(formState.username, formState.password)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    // error already handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}
.login-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}
.login-left {
  width: 480px;
  padding: 60px 48px;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
  color: #fff;
  border-radius: 16px 0 0 16px;
  display: flex;
  align-items: center;
}
.brand-logo img {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
}
.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px;
}
.brand-subtitle {
  font-size: 16px;
  opacity: 0.85;
  margin: 0 0 40px;
}
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.9;
}
.feature-item :deep(.anticon) {
  font-size: 18px;
}
.login-right {
  width: 420px;
  background: #fff;
  border-radius: 0 16px 16px 0;
  padding: 60px 48px;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.08);
}
.login-form-wrapper {
  width: 100%;
}
.form-title {
  font-size: 24px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 0 8px;
}
.form-subtitle {
  color: rgba(0, 0, 0, 0.45);
  margin: 0 0 32px;
  font-size: 14px;
}
.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.login-btn {
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}
.login-footer {
  text-align: center;
  padding: 16px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
