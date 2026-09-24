<template>
  <div class="login-page">
    <!-- 全局装饰 -->
    <div class="grid-overlay" aria-hidden="true"></div>
    <div class="scanline" aria-hidden="true"></div>
    <span class="particle p1" aria-hidden="true"></span>
    <span class="particle p2" aria-hidden="true"></span>
    <span class="particle p3" aria-hidden="true"></span>

    <!-- 左侧品牌区 -->
    <section class="brand-side">
      <div class="brand-inner">
        <div class="logo-row">
          <div class="logo-mark">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M2 12h4l2.5-6 4 12 3-8 1.8 4H22" />
            </svg>
          </div>
          <div class="logo-name">MAI<span>DC</span></div>
        </div>

        <div>
          <h1 class="brand-title">数据驱动的<br /><span class="hl">智慧医疗</span>中枢</h1>
          <p class="brand-sub">临床 + 科研一体化多中心医疗AI平台，让每一次诊疗数据都成为科研的养分。</p>
        </div>

        <div class="orbit-stage" aria-hidden="true">
          <div class="orbit">
            <div class="ring"></div>
            <div class="ring r2"></div>
            <div class="ring r3"></div>
            <div class="halo"></div>
            <div class="core">
              <span class="num">1.2<i>亿</i></span>
              <span class="lbl">治理后数据</span>
            </div>
          </div>
        </div>

        <!-- TODO: 平台指标当前为装饰数据，后续接入真实统计接口 -->
        <div class="metrics">
          <div class="metric">
            <div class="v">32<i>家</i></div>
            <div class="k">接入医疗机构</div>
          </div>
          <div class="metric">
            <div class="v">48<i>个</i></div>
            <div class="k">在线AI模型</div>
          </div>
          <div class="metric">
            <div class="v">99.99<i>%</i></div>
            <div class="k">平台可用性</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 右侧登录卡 -->
    <section class="form-side">
      <form class="login-card" @submit.prevent="handleLogin">
        <div>
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-sub">登录账户，进入您的工作台</p>
        </div>

        <div class="field">
          <label for="login-username">用户名</label>
          <div class="input-wrap">
            <span class="lead">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </span>
            <input
              id="login-username"
              v-model="formState.username"
              type="text"
              placeholder="请输入用户名 / 工号"
              autocomplete="username"
              @keydown.enter="passwordInput?.focus()"
            />
          </div>
        </div>

        <div class="field">
          <label for="login-password">密码</label>
          <div class="input-wrap">
            <span class="lead">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </span>
            <input
              id="login-password"
              ref="passwordInput"
              v-model="formState.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="eye-btn"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <svg v-if="!showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
            </button>
          </div>
        </div>

        <div class="row-extra">
          <label class="remember"><input v-model="formState.remember" type="checkbox" />记住我</label>
          <a class="forgot" href="javascript:void(0)">忘记密码?</a>
        </div>

        <button class="submit-btn" type="submit" :disabled="loading">
          <span v-if="loading" class="btn-spinner" aria-hidden="true"></span>
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <div class="divider">其他登录方式</div>

        <button class="sso-btn" type="button">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="14" rx="2"/><path d="M8 21h8M12 18v3"/></svg>
          医院统一认证（SSO）登录
        </button>

        <div class="sec-hints">
          <p>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            全链路加密传输，登录行为将被审计记录
          </p>
          <p>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
            连续 5 次失败将锁定账户 30 分钟
          </p>
        </div>

        <div class="foot">
          <span>© 2026 MAIDC</span><span>·</span><a href="javascript:void(0)">隐私政策</a><span>·</span><a href="javascript:void(0)">使用条款</a>
        </div>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const REMEMBER_KEY = 'maidc.login.username'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const showPassword = ref(false)
const passwordInput = ref<HTMLInputElement | null>(null)

const formState = reactive({
  username: '',
  password: '',
  remember: true,
})

onMounted(() => {
  const remembered = localStorage.getItem(REMEMBER_KEY)
  if (remembered) {
    formState.username = remembered
    formState.remember = true
  }
})

async function handleLogin() {
  if (!formState.username || !formState.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    loading.value = true
    await authStore.loginAction(formState.username, formState.password)
    if (formState.remember) {
      localStorage.setItem(REMEMBER_KEY, formState.username)
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    ElMessage.success('登录成功，正在进入工作台')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || '登录失败，请检查用户名和密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }

:root {
  --neon: #22d3ee;
}

.login-page {
  --neon: #22d3ee;
  --neon-soft: rgba(34, 211, 238, 0.14);
  --violet: #818cf8;
  --text-1: #e6edf6;
  --text-2: #94a3b8;
  --text-3: #64748b;
  min-height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background:
    radial-gradient(1100px 700px at 18% 30%, rgba(34, 211, 238, 0.07), transparent 60%),
    radial-gradient(900px 600px at 90% 85%, rgba(129, 140, 248, 0.06), transparent 60%),
    linear-gradient(160deg, #060b16 0%, #0a1220 100%);
  color: var(--text-1);
}

/* ================= 全局装饰 ================= */
.grid-overlay {
  position: absolute; inset: 0; pointer-events: none;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.045) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at 35% 45%, #000 25%, transparent 78%);
}
.scanline {
  position: absolute; left: 0; right: 0; height: 180px; pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(34, 211, 238, 0.045), transparent);
  animation: scan 9s ease-in-out infinite;
}
@keyframes scan {
  0%, 100% { top: -180px; }
  50% { top: 100%; }
}
.particle { position: absolute; border-radius: 50%; pointer-events: none; background: var(--neon); opacity: 0.35; animation: drift 14s ease-in-out infinite; }
.particle.p1 { width: 5px; height: 5px; left: 12%; top: 22%; box-shadow: 0 0 12px var(--neon); }
.particle.p2 { width: 3px; height: 3px; left: 30%; top: 68%; box-shadow: 0 0 10px var(--neon); animation-delay: -4s; }
.particle.p3 { width: 4px; height: 4px; left: 44%; top: 14%; box-shadow: 0 0 10px var(--violet); background: var(--violet); animation-delay: -8s; }
@keyframes drift {
  0%, 100% { transform: translate(0, 0); opacity: 0.15; }
  50% { transform: translate(24px, -30px); opacity: 0.5; }
}

/* ================= 左侧品牌区 ================= */
.brand-side {
  flex: 1.15;
  position: relative;
  display: flex; align-items: center; justify-content: center;
  padding: 56px 60px;
}
.brand-inner { width: 100%; max-width: 620px; display: flex; flex-direction: column; gap: 34px; }

.logo-row { display: flex; align-items: center; gap: 12px; }
.logo-mark {
  width: 44px; height: 44px; border-radius: 12px;
  background: linear-gradient(135deg, var(--neon), var(--violet));
  display: flex; align-items: center; justify-content: center;
  color: #04121e; box-shadow: 0 0 26px rgba(34, 211, 238, 0.45);
}
.logo-name { font-size: 24px; font-weight: 700; letter-spacing: 3px; }
.logo-name span { color: var(--neon); }

.brand-title { font-size: 40px; font-weight: 700; line-height: 1.28; letter-spacing: 1px; }
.brand-title .hl {
  background: linear-gradient(90deg, var(--neon), var(--violet));
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.brand-sub { font-size: 15px; color: var(--text-2); line-height: 1.8; margin-top: 14px; }

/* 数据环 */
.orbit-stage { display: flex; align-items: center; justify-content: center; padding: 8px 0; }
.orbit { position: relative; width: 300px; height: 300px; }
.orbit .ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 1px dashed rgba(34, 211, 238, 0.25);
  animation: spin 26s linear infinite;
}
.orbit .ring.r2 { inset: 34px; border-style: solid; border-color: rgba(129, 140, 248, 0.22); animation-duration: 18s; animation-direction: reverse; }
.orbit .ring.r2::before {
  content: ''; position: absolute; top: -5px; left: 50%;
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--violet); box-shadow: 0 0 14px var(--violet);
}
.orbit .ring.r3 { inset: 72px; border-color: rgba(34, 211, 238, 0.3); animation-duration: 12s; }
.orbit .ring.r3::before {
  content: ''; position: absolute; bottom: -4px; left: 50%;
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--neon); box-shadow: 0 0 14px var(--neon);
}
.orbit .halo {
  position: absolute; inset: 104px; border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(34, 211, 238, 0.5), rgba(129, 140, 248, 0.5), rgba(34, 211, 238, 0.5));
  mask: radial-gradient(circle, transparent 58%, #000 60%);
  -webkit-mask: radial-gradient(circle, transparent 58%, #000 60%);
  animation: spin 8s linear infinite;
}
.orbit .core {
  position: absolute; inset: 102px; border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, rgba(34, 211, 238, 0.22), rgba(10, 18, 32, 0.9) 70%);
  border: 1px solid rgba(34, 211, 238, 0.3);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 3px;
  box-shadow: 0 0 40px rgba(34, 211, 238, 0.18) inset, 0 0 34px rgba(34, 211, 238, 0.12);
  animation: breathe 4s ease-in-out infinite;
}
.core .num { font-size: 26px; font-weight: 700; color: var(--neon); text-shadow: 0 0 18px rgba(34, 211, 238, 0.6); font-variant-numeric: tabular-nums; }
.core .num i { font-style: normal; font-size: 14px; }
.core .lbl { font-size: 11px; color: var(--text-2); letter-spacing: 1px; white-space: nowrap; }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes breathe {
  0%, 100% { box-shadow: 0 0 40px rgba(34, 211, 238, 0.14) inset, 0 0 30px rgba(34, 211, 238, 0.1); }
  50% { box-shadow: 0 0 46px rgba(34, 211, 238, 0.24) inset, 0 0 44px rgba(34, 211, 238, 0.2); }
}

/* 指标条 */
.metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.metric {
  padding: 16px 18px; border-radius: 14px;
  background: rgba(15, 26, 46, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.12);
  backdrop-filter: blur(8px);
  position: relative; overflow: hidden;
}
.metric::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px;
  background: linear-gradient(180deg, var(--neon), transparent);
}
.metric .v { font-size: 24px; font-weight: 700; color: var(--text-1); font-variant-numeric: tabular-nums; }
.metric .v i { font-style: normal; font-size: 13px; color: var(--neon); margin-left: 2px; }
.metric .k { font-size: 12px; color: var(--text-3); margin-top: 4px; letter-spacing: 1px; }

/* ================= 右侧登录区 ================= */
.form-side {
  width: 560px; min-width: 480px;
  display: flex; align-items: center; justify-content: center;
  padding: 48px 40px;
  border-left: 1px solid rgba(148, 163, 184, 0.08);
  background: linear-gradient(200deg, rgba(13, 22, 40, 0.5), rgba(6, 11, 22, 0.2));
}
.login-card {
  width: 400px; max-width: 100%;
  padding: 42px 38px 30px;
  border-radius: 20px;
  background: rgba(15, 26, 46, 0.72);
  border: 1px solid rgba(94, 234, 212, 0.12);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 24px 60px rgba(2, 8, 20, 0.6), 0 0 0 1px rgba(34, 211, 238, 0.04);
  display: flex; flex-direction: column; gap: 22px;
  position: relative;
}
.login-card::before {
  content: ''; position: absolute; top: 0; left: 24px; right: 24px; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(34, 211, 238, 0.5), transparent);
}

.card-title { font-size: 24px; font-weight: 700; }
.card-sub { font-size: 13px; color: var(--text-2); margin-top: 6px; }

.field { display: flex; flex-direction: column; gap: 8px; }
.field label { font-size: 13px; color: var(--text-2); }
.input-wrap { position: relative; }
.input-wrap .lead {
  position: absolute; left: 14px; top: 50%; transform: translateY(-50%);
  color: var(--text-3); display: flex; pointer-events: none; transition: color 0.2s;
}
.input-wrap:focus-within .lead { color: var(--neon); }
.field input {
  width: 100%; height: 46px;
  padding: 0 44px 0 42px;
  border-radius: 12px;
  background: rgba(8, 15, 30, 0.65);
  border: 1px solid rgba(148, 163, 184, 0.18);
  color: var(--text-1); font-size: 14px; outline: none; font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}
.field input::placeholder { color: var(--text-3); }
.field input:hover { border-color: rgba(148, 163, 184, 0.32); }
.field input:focus {
  border-color: var(--neon);
  background: rgba(8, 15, 30, 0.9);
  box-shadow: 0 0 0 3px var(--neon-soft), 0 0 18px rgba(34, 211, 238, 0.12);
}
.eye-btn {
  position: absolute; right: 6px; top: 50%; transform: translateY(-50%);
  width: 34px; height: 34px; border: none; border-radius: 9px;
  background: transparent; color: var(--text-3); cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: 0.2s;
}
.eye-btn:hover { color: var(--neon); background: rgba(34, 211, 238, 0.08); }

.row-extra { display: flex; align-items: center; justify-content: space-between; font-size: 13px; }
.remember { display: flex; align-items: center; gap: 8px; color: var(--text-2); cursor: pointer; user-select: none; }
.remember input { accent-color: var(--neon); width: 15px; height: 15px; cursor: pointer; }
.forgot { color: var(--neon); text-decoration: none; }
.forgot:hover { text-shadow: 0 0 12px rgba(34, 211, 238, 0.6); }

.submit-btn {
  height: 48px; border: none; border-radius: 12px; cursor: pointer;
  font-size: 15px; font-weight: 600; letter-spacing: 6px; font-family: inherit;
  color: #04121e;
  background: linear-gradient(135deg, #22d3ee 0%, #38bdf8 55%, #818cf8 100%);
  box-shadow: 0 6px 22px rgba(34, 211, 238, 0.35);
  transition: transform 0.15s, box-shadow 0.25s, filter 0.2s;
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); filter: brightness(1.08); box-shadow: 0 10px 30px rgba(34, 211, 238, 0.45); }
.submit-btn:active:not(:disabled) { transform: translateY(0); }
.submit-btn:disabled { opacity: 0.7; cursor: not-allowed; }

.btn-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(4, 18, 30, 0.35);
  border-top-color: #04121e;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.divider { display: flex; align-items: center; gap: 12px; color: var(--text-3); font-size: 12px; }
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: linear-gradient(90deg, transparent, rgba(148, 163, 184, 0.25)); }
.divider::after { background: linear-gradient(90deg, rgba(148, 163, 184, 0.25), transparent); }

.sso-btn {
  height: 44px; border-radius: 12px; cursor: pointer; font-family: inherit;
  background: rgba(34, 211, 238, 0.05);
  border: 1px solid rgba(34, 211, 238, 0.3);
  color: var(--neon); font-size: 13px; font-weight: 500;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: 0.2s;
}
.sso-btn:hover { background: rgba(34, 211, 238, 0.12); box-shadow: 0 0 20px rgba(34, 211, 238, 0.2); }

.sec-hints {
  display: flex; flex-direction: column; gap: 6px;
  padding: 12px 14px; border-radius: 12px;
  background: rgba(8, 15, 30, 0.5);
  border: 1px dashed rgba(148, 163, 184, 0.16);
}
.sec-hints p { display: flex; align-items: center; gap: 7px; font-size: 11.5px; color: var(--text-3); }
.sec-hints svg { flex-shrink: 0; color: rgba(34, 211, 238, 0.7); }

.foot { display: flex; align-items: center; justify-content: center; gap: 10px; font-size: 12px; color: var(--text-3); }
.foot a { color: var(--text-2); text-decoration: none; }
.foot a:hover { color: var(--neon); }

/* ================= 响应式 / 降级 ================= */
@media (max-width: 1080px) {
  .brand-side { display: none; }
  .form-side { width: 100%; min-width: 0; border-left: none; }
}
@media (max-width: 480px) {
  .login-card { padding: 32px 24px 24px; }
}
@media (prefers-reduced-motion: reduce) {
  .scanline, .particle, .ring, .halo, .core, .btn-spinner { animation: none !important; }
  .submit-btn:hover:not(:disabled) { transform: none; }
}
</style>
