<template>
  <div class="login-page">
    <!-- Left Panel: Brand & Features -->
    <div class="login-left">
      <div class="left-content">
        <div class="logo-area">
          <div class="logo-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9.5 2a3.5 3.5 0 0 0-3.2 4.9A3 3 0 0 0 4 9.5a3 3 0 0 0 1.8 2.75A2.5 2.5 0 0 0 5 14.5a2.5 2.5 0 0 0 2.1 2.47A3 3 0 0 0 9.5 19a3 3 0 0 0 2.5-1.35A3 3 0 0 0 14.5 19a3 3 0 0 0 2.4-2.03A2.5 2.5 0 0 0 19 14.5a2.5 2.5 0 0 0-.8-2.25A3 3 0 0 0 20 9.5a3 3 0 0 0-2.3-2.6A3.5 3.5 0 0 0 14.5 2a3.5 3.5 0 0 0-2.5 1.05A3.5 3.5 0 0 0 9.5 2Z"/>
              <path d="M9.5 2v5.5"/><path d="M14.5 2v5.5"/>
              <path d="M12 7.5V22"/>
            </svg>
          </div>
          <span class="logo-text">MAIDC</span>
        </div>
        <h1 class="welcome-title">MAIDC 医疗AI数据中心</h1>
        <p class="welcome-sub">临床+科研一体化多中心医疗AI平台</p>
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <span class="check-mark">✓</span>
            </div>
            <span>统一临床数据仓库（CDR）</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <span class="check-mark">✓</span>
            </div>
            <span>全生命周期AI模型管理</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <span class="check-mark">✓</span>
            </div>
            <span>安全合规的多中心协作</span>
          </div>
        </div>
        <div class="version-tag">v1.0.0</div>
      </div>
    </div>

    <!-- Right Panel: Login Form -->
    <div class="login-right">
      <div class="login-form">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请登录您的账户</p>
        </div>

        <div class="form-fields">
          <div class="field-group">
            <label class="field-label">用户名</label>
            <input
              v-model="formState.username"
              type="text"
              class="field-input"
              placeholder="请输入用户名"
              autocomplete="username"
              @keydown.enter="passwordInput?.focus()"
            />
          </div>
          <div class="field-group">
            <label class="field-label">密码</label>
            <input
              ref="passwordInput"
              v-model="formState.password"
              type="password"
              class="field-input"
              placeholder="请输入密码"
              autocomplete="current-password"
              @keydown.enter="handleLogin"
            />
          </div>
        </div>

        <div class="form-extra">
          <label class="remember-me">
            <input type="checkbox" v-model="formState.remember" />
            <span>记住我</span>
          </label>
          <a class="forgot-link">忘记密码?</a>
        </div>

        <button class="login-btn" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <div class="divider">
          <div class="divider-line"></div>
          <span class="divider-text">其他登录方式</span>
          <div class="divider-line"></div>
        </div>

        <button class="sso-btn">医院统一认证登录</button>

        <div class="security-hints">
          <p>⚠ 您的登录行为将被审计记录</p>
          <p>⚠ 连续5次登录失败将锁定账户30分钟</p>
        </div>

        <div class="footer-links">
          <span>© 2026 MAIDC</span>
          <span>·</span>
          <a>隐私政策</a>
          <span>·</span>
          <a>使用条款</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const passwordInput = ref<HTMLInputElement | null>(null)

const formState = reactive({
  username: '',
  password: '',
  remember: true,
})

async function handleLogin() {
  if (!formState.username || !formState.password) {
    message.warning('请输入用户名和密码')
    return
  }
  try {
    loading.value = true
    await authStore.loginAction(formState.username, formState.password)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    const msg = e?.message || '登录失败，请检查用户名和密码'
    message.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&display=swap');

.login-page {
  min-height: 100vh;
  display: flex;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* Left Panel */
.login-left {
  width: 50%;
  min-width: 50%;
  background: linear-gradient(180deg, #1E293B 0%, #334155 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.left-content {
  padding: 60px;
  display: flex;
  flex-direction: column;
  gap: 28px;
  width: 100%;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.125);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
}

.welcome-title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0;
}

.welcome-sub {
  font-size: 15px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
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
  font-weight: 400;
  color: rgba(255, 255, 255, 0.8);
}

.feature-icon {
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.125);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.check-mark {
  font-size: 12px;
  color: #10B981;
}

.version-tag {
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.4);
}

/* Right Panel */
.login-right {
  flex: 1;
  background: var(--color-bg, #f8fafc);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.form-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text, #0f172a);
  margin: 0;
}

.form-subtitle {
  font-size: 14px;
  font-weight: 400;
  color: var(--color-text-secondary, #64748b);
  margin: 0;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text, #334155);
}

.field-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: var(--color-text, #0f172a);
  background: #fff;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.field-input::placeholder {
  color: #94a3b8;
}

.field-input:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.15);
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-secondary, #64748b);
  cursor: pointer;
}

.remember-me input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #1677ff;
}

.forgot-link {
  font-size: 13px;
  color: #1677ff;
  cursor: pointer;
}

.forgot-link:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  height: 44px;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}

.login-btn:hover:not(:disabled) {
  background: #4096ff;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.divider {
  display: flex;
  align-items: center;
  gap: 16px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.divider-text {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
}

.sso-btn {
  height: 40px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.sso-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.security-hints {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.security-hints p {
  font-size: 11px;
  color: #94a3b8;
  margin: 0;
}

.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding-top: 16px;
  font-size: 12px;
  color: var(--color-text-secondary, #64748b);
}

.footer-links a {
  color: #1677ff;
  cursor: pointer;
}

.footer-links a:hover {
  text-decoration: underline;
}

.footer-links span {
  color: #94a3b8;
}
</style>
