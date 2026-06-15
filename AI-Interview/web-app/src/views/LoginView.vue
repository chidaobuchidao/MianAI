<template>
  <div class="login">
    <div class="login__wash"></div>
    <div class="login__grid"></div>
    <ParticleBg
      :particleCount="86"
      :particleSpread="7"
      :speed="0.045"
      :alphaParticles="true"
      :particleBaseSize="52"
      :sizeRandomness="0.5"
      :moveOnHover="true"
      :hoverFactor="0.4"
      :disableRotation="false"
    />
    <div class="login__shell animate-fade-in-up">
      <section class="login__card" :class="`login__card--${tab}`">
        <div class="login__masthead">
          <span class="login__brand">Mianmian.</span>
          <p class="login__desc">AI 模拟面试平台</p>
        </div>

        <div class="login__tabs" v-if="tab !== 'reset'">
          <button class="login__tab" :class="{ 'login__tab--active': tab === 'login' }" @click="switchTab('login')">登录</button>
          <button class="login__tab" :class="{ 'login__tab--active': tab !== 'login' }" @click="switchTab('register')">注册</button>
        </div>

        <div class="login__form-head">
          <span class="login__form-title">
            {{ tab === 'login' ? '欢迎回来' : tab === 'register' ? '创建账号' : '重置密码' }}
          </span>
          <span class="login__form-subtitle">
            {{ tab === 'login' ? '使用邮箱或用户名继续访问' : tab === 'register' ? '邮箱验证后即可开始使用' : '验证邮箱后设置新密码' }}
          </span>
        </div>

        <div class="login__form-stage">
          <div v-if="tab === 'login'" key="login">
            <label class="login__field">
              <span>账号</span>
              <input
                class="login__input"
                v-model="account"
                placeholder="邮箱或用户名"
                :disabled="loading"
                autocomplete="username"
              />
            </label>
            <label class="login__field">
              <span>密码</span>
              <input
                class="login__input"
                v-model="password"
                type="password"
                placeholder="输入登录密码"
                :disabled="loading"
                autocomplete="current-password"
                @keydown.enter="handleLogin"
              />
            </label>
            <button class="login__btn" @click="handleLogin" :disabled="loading || !account || !password">
              {{ loading ? '处理中...' : '登录' }}
            </button>
            <p class="login__extra-link">
              <span @click="switchToReset">忘记密码？</span>
            </p>
          </div>

          <div v-else-if="tab === 'register'" key="register">
            <label class="login__field">
              <span>邮箱</span>
              <input
                class="login__input"
                v-model="email"
                type="email"
                placeholder="请输入邮箱地址"
                :disabled="loading"
              />
            </label>
            <label class="login__field login__field--code-group">
              <span>验证码</span>
              <div class="login__code-row">
                <input
                  class="login__input login__input--code"
                  v-model="code"
                  placeholder="邮箱验证码"
                  :disabled="loading"
                  maxlength="6"
                />
                <button
                  class="login__send-code"
                  type="button"
                  :disabled="codeCountdown > 0 || loading || !email"
                  @click="openCaptcha"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                </button>
              </div>
            </label>
            <label class="login__field">
              <span>密码</span>
              <input
                class="login__input"
                v-model="password"
                type="password"
                placeholder="设置登录密码"
                :disabled="loading"
                @keydown.enter="handleRegister"
              />
            </label>
            <label class="login__field">
              <span>昵称</span>
              <input
                class="login__input"
                v-model="nickname"
                placeholder="设置昵称（选填）"
                :disabled="loading"
              />
            </label>
            <button class="login__btn" @click="handleRegister" :disabled="loading || !email || !code || !password">
              {{ loading ? '处理中...' : '注册' }}
            </button>
            <p class="login__note">点击注册即表示同意服务条款</p>
          </div>

          <div v-else key="reset">
            <label class="login__field">
              <span>邮箱</span>
              <input
                class="login__input"
                v-model="email"
                type="email"
                placeholder="请输入注册邮箱"
                :disabled="loading"
              />
            </label>
            <label class="login__field login__field--code-group">
              <span>验证码</span>
              <div class="login__code-row">
                <input
                  class="login__input login__input--code"
                  v-model="code"
                  placeholder="邮箱验证码"
                  :disabled="loading"
                  maxlength="6"
                />
                <button
                  class="login__send-code"
                  type="button"
                  :disabled="codeCountdown > 0 || loading || !email"
                  @click="openCaptcha"
                >
                  {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
                </button>
              </div>
            </label>
            <label class="login__field">
              <span>新密码</span>
              <input
                class="login__input"
                v-model="password"
                type="password"
                placeholder="设置新登录密码"
                :disabled="loading"
                @keydown.enter="handleReset"
              />
            </label>
            <button class="login__btn" @click="handleReset" :disabled="loading || !email || !code || !password">
              {{ loading ? '处理中...' : '重置密码' }}
            </button>
            <p class="login__extra-link">
              <span @click="switchTab('login')">← 返回登录</span>
            </p>
          </div>
        </div>

        <p class="login__error" v-if="errorMsg">{{ errorMsg }}</p>
      </section>
    </div>

    <!-- Captcha modal -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCaptchaModal" class="captcha-overlay" @click.self="closeCaptcha">
          <div class="captcha-modal">
            <button class="captcha-modal__x" type="button" @click="closeCaptcha" aria-label="关闭">×</button>
            <div class="captcha-modal__head">
              <h3 class="captcha-modal__title">安全验证</h3>
              <p class="captcha-modal__desc">拖动滑块，让拼图回到缺口位置</p>
            </div>
            <SliderCaptcha v-if="showCaptchaModal" @verified="onCaptchaVerified" />
            <button class="captcha-modal__close" @click="closeCaptcha">取消</button>
          </div>
        </div>
      </Transition>
    </Teleport>

    <div class="bottom-safe" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { post } from '@/utils/request'
import ParticleBg from '@/components/ParticleBg.vue'
import SliderCaptcha from '@/components/SliderCaptcha.vue'

interface LoginResponse {
  token: string
  userId: number
  nickname: string
  avatarUrl: string
  email?: string
  needBindEmail?: boolean
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const errorMsg = ref('')
const tab = ref<'login' | 'register' | 'reset'>('login')
const account = ref('')
const email = ref('')
const password = ref('')
const code = ref('')
const nickname = ref('')
const codeCountdown = ref(0)
const showCaptchaModal = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

function switchTab(t: 'login' | 'register' | 'reset') {
  if (tab.value === t) return
  tab.value = t
  errorMsg.value = ''
  showCaptchaModal.value = false
}

function openCaptcha() {
  if (!email.value || codeCountdown.value > 0) return
  showCaptchaModal.value = true
}

function closeCaptcha() {
  showCaptchaModal.value = false
}

function onCaptchaVerified() {
  showCaptchaModal.value = false
  sendCode()
}

function switchToReset() {
  switchTab('reset')
  account.value = ''
  password.value = ''
}

// ============ Login: auto-detect email vs username ============
async function handleLogin() {
  if (!account.value || !password.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const isEmail = account.value.includes('@')
    const res = await post<LoginResponse>(
      isEmail ? '/api/auth/login/email' : '/api/auth/login/password',
      isEmail
        ? { email: account.value, password: password.value }
        : { username: account.value, password: password.value }
    )
    if (res.code === 200 && res.data) {
      loginSuccess(res.data)
    } else {
      errorMsg.value = res.message || '登录失败'
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

// ============ Register: email ============
async function handleRegister() {
  if (!email.value || !code.value || !password.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await post<LoginResponse>('/api/auth/register/email', {
      email: email.value,
      code: code.value,
      password: password.value,
      nickname: nickname.value || email.value.split('@')[0],
    })
    if (res.code === 200 && res.data) {
      loginSuccess(res.data)
    } else {
      errorMsg.value = res.message || '注册失败'
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

// ============ Reset password ============
async function handleReset() {
  if (!email.value || !code.value || !password.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await post('/api/auth/reset-password', {
      email: email.value,
      code: code.value,
      newPassword: password.value,
    })
    if (res.code === 200) {
      alert('密码重置成功，请登录')
      switchTab('login')
      email.value = ''
      password.value = ''
      code.value = ''
    } else {
      errorMsg.value = res.message || '重置失败'
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

// ============ Send verification code ============
async function sendCode() {
  if (!email.value || codeCountdown.value > 0) return
  loading.value = true
  errorMsg.value = ''
  try {
    const type = tab.value === 'reset' ? 'reset' : 'register'
    const res = await post('/api/auth/send-code', { email: email.value, type })
    if (res.code === 200) {
      codeCountdown.value = 60
      countdownTimer = setInterval(() => {
        codeCountdown.value--
        if (codeCountdown.value <= 0) {
          if (countdownTimer) clearInterval(countdownTimer)
          countdownTimer = null
        }
      }, 1000)
    } else {
      errorMsg.value = (res as any).message || '发送失败'
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '发送失败'
  } finally {
    loading.value = false
  }
}

function loginSuccess(data: LoginResponse) {
  userStore.setUser({
    userId: data.userId,
    nickname: data.nickname || '用户',
    avatarUrl: data.avatarUrl || '',
    token: data.token,
    email: data.email,
    needBindEmail: data.needBindEmail,
  })
  const payload = decodeJwtPayload(data.token)
  userStore.setAdmin(payload?.role === 1)
  router.push('/')
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  background:
    linear-gradient(135deg, rgba(20, 20, 19, 0.06), transparent 30%),
    linear-gradient(180deg, #f6f2ea 0%, #ece5d9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  position: relative;
  overflow: hidden;
  color: var(--text-main);
}
.login__wash {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, rgba(20, 20, 19, 0.88) 0%, rgba(20, 20, 19, 0.72) 34%, transparent 34.2%),
    linear-gradient(90deg, transparent 0%, rgba(217, 117, 10, 0.08) 100%);
  opacity: 0.88;
  pointer-events: none;
}
.login__grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(20,20,19,0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(20,20,19,0.045) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(90deg, transparent 0%, black 28%, black 100%);
  pointer-events: none;
}
.login__card {
  position: relative;
  z-index: 1;
  max-width: 420px;
  width: 100%;
  padding: 30px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 18px;
  background: rgba(253, 252, 251, 0.88);
  box-shadow: 0 28px 70px rgba(20, 20, 19, 0.18);
  backdrop-filter: blur(22px);
}
.login__card::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border-top: 1px solid rgba(255,255,255,0.70);
  pointer-events: none;
}
.login__card::after {
  content: "";
  position: absolute;
  left: 18px;
  bottom: 18px;
  width: 11px;
  height: 11px;
  border-radius: 999px;
  background: rgba(217, 117, 10, 0.44);
  box-shadow:
    0 0 0 5px rgba(217, 117, 10, 0.08),
    0 0 0 1px rgba(255,255,255,0.80) inset;
  pointer-events: none;
  transition: transform 0.34s var(--ease-out-expo), opacity 0.34s var(--ease-out-expo);
}
.login__card--register::after {
  transform: scale(1.16);
}
.login__card--reset::after {
  opacity: 0.26;
}
.login__masthead {
  display: grid;
  gap: 6px;
  justify-items: start;
  margin-bottom: 22px;
}
.login__eyebrow {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border: 1px solid rgba(217, 117, 10, 0.22);
  border-radius: 999px;
  background: rgba(217, 117, 10, 0.08);
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}
.login__brand {
  font-family: var(--font-serif);
  font-size: 38px;
  line-height: 1.08;
  font-weight: 600;
  letter-spacing: 0;
  color: var(--text-main);
}
.login__desc {
  color: var(--text-light);
  font-size: 14px;
  margin: 0;
}
.login__tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-bottom: 22px;
  padding: 4px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 12px;
  background: rgba(20, 20, 19, 0.04);
  overflow: hidden;
}
.login__tab {
  min-height: 38px;
  padding: 0 12px;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--text-muted);
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}
.login__tab--active {
  background: var(--bg-dark);
  color: #fff;
  box-shadow: 0 10px 18px rgba(20, 20, 19, 0.14);
}
.login__form-head {
  display: grid;
  gap: 5px;
  margin-bottom: 16px;
}
.login__form-title {
  font-size: 20px;
  font-weight: 750;
  line-height: 1.25;
}
.login__form-subtitle {
  color: var(--text-light);
  font-size: 13px;
}
.login__field {
  display: grid;
  gap: 7px;
  margin-bottom: 12px;
  text-align: left;
}
.login__field span {
  color: rgba(20, 20, 19, 0.62);
  font-size: 12px;
  font-weight: 700;
}
.login__input {
  width: 100%;
  height: 48px;
  padding: 0 15px;
  border: 1px solid rgba(20, 20, 19, 0.08);
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  font-family: inherit;
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.78);
  transition: border-color 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
  box-sizing: border-box;
}
.login__input::placeholder { color: rgba(20, 20, 19, 0.34); }
.login__input:focus {
  border-color: rgba(217, 117, 10, 0.58);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(217, 117, 10, 0.10);
}
.login__input:disabled {
  color: rgba(20, 20, 19, 0.42);
  cursor: not-allowed;
}
.login__code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 10px;
  align-items: end;
  margin-bottom: 12px;
}
.login__field--code {
  margin-bottom: 0;
}
.login__input--code {
  text-align: center;
  font-family: var(--font-mono);
  letter-spacing: 0;
}
.login__send-code {
  height: 48px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 750;
  background: #fff;
  color: var(--text-main);
  border: 1px solid rgba(20, 20, 19, 0.10);
  border-radius: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.16s ease, transform 0.16s ease, box-shadow 0.16s ease;
}
.login__send-code:hover:not(:disabled) {
  border-color: rgba(217, 117, 10, 0.46);
  box-shadow: 0 10px 20px rgba(20, 20, 19, 0.08);
}
.login__send-code:disabled {
  color: rgba(20, 20, 19, 0.34);
  cursor: not-allowed;
  background: rgba(255,255,255,0.46);
}
.login__btn {
  width: 100%;
  height: 50px;
  background:
    linear-gradient(135deg, rgba(255,255,255,0.10), transparent),
    var(--bg-dark);
  color: #fff;
  padding: 0 16px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 760;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: 0 16px 28px rgba(20, 20, 19, 0.18);
  transition: transform 0.16s ease, opacity 0.16s ease, box-shadow 0.16s ease;
  border: none;
  cursor: pointer;
}
.login__btn:hover:not(:disabled) {
  box-shadow: 0 20px 34px rgba(20, 20, 19, 0.22);
  transform: translateY(-1px);
}
.login__btn:disabled {
  opacity: 0.48;
  cursor: not-allowed;
  box-shadow: none;
}
.login__btn:active { transform: scale(0.98); }
.login__error {
  padding: 10px 12px;
  border: 1px solid rgba(239, 68, 68, 0.16);
  border-radius: 10px;
  background: rgba(239, 68, 68, 0.07);
  font-size: 13px;
  color: #b91c1c;
  margin: 14px 0 0;
  text-align: left;
}
.login__extra-link {
  font-size: 13px;
  color: var(--text-muted);
  margin: 15px 0 0;
  cursor: pointer;
  text-align: center;
}
.login__extra-link span:hover { color: var(--text-main); }
.login__note {
  font-size: 11px;
  color: var(--text-light);
  margin: 14px 0 0;
  text-align: center;
}
.bottom-safe { height: 40px; }

.captcha-overlay {
  position: fixed;
  inset: 0;
  background: rgba(20, 20, 19, 0.38);
  backdrop-filter: blur(10px) saturate(0.9);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.captcha-modal {
  position: relative;
  background: rgba(253, 252, 251, 0.96);
  border: 1px solid rgba(255,255,255,0.72);
  border-radius: 18px;
  padding: 26px 24px 18px;
  width: 100%;
  max-width: 456px;
  box-shadow: 0 30px 80px rgba(20, 20, 19, 0.28);
  animation: modal-in 0.2s ease;
}
@keyframes modal-in {
  from { opacity: 0; transform: scale(0.95) translateY(8px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}
.captcha-modal__title {
  font-size: 19px;
  font-weight: 780;
  color: #1a1a1a;
  margin: 0;
  text-align: center;
}
.captcha-modal__head {
  display: grid;
  gap: 6px;
  margin-bottom: 18px;
  text-align: center;
}
.captcha-modal__desc {
  margin: 0;
  color: rgba(20, 20, 19, 0.52);
  font-size: 13px;
}
.captcha-modal__x {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(20,20,19,0.06);
  border-radius: 999px;
  background: rgba(20,20,19,0.035);
  color: rgba(20,20,19,0.52);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}
.captcha-modal__x:hover {
  background: rgba(20,20,19,0.07);
  color: var(--text-main);
}
.captcha-modal__close {
  display: block;
  width: 100%;
  height: 42px;
  margin-top: 14px;
  padding: 0 12px;
  font-size: 14px;
  color: rgba(20,20,19,0.54);
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.captcha-modal__close:hover {
  color: #1a1a1a;
  background: rgba(20,20,19,0.04);
}

.modal-fade-enter-active { transition: opacity 0.2s ease; }
.modal-fade-leave-active { transition: opacity 0.15s ease; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }

.login__form-stage {
  position: relative;
  min-height: 0;
  perspective: 1000px;
  transform-style: preserve-3d;
}

@media (max-width: 720px) {
  .login {
    justify-content: center;
    padding: 26px 18px;
  }
  .login__wash {
    background:
      linear-gradient(180deg, rgba(20, 20, 19, 0.16), transparent 34%),
      linear-gradient(90deg, rgba(217, 117, 10, 0.08), transparent);
  }
  .login__grid {
    mask-image: linear-gradient(180deg, black 0%, transparent 100%);
  }
  .login__card {
    max-width: 390px;
    padding: 24px 20px;
    border-radius: 16px;
  }
  .login__brand {
    font-size: 34px;
  }
  .login__code-row {
    grid-template-columns: minmax(0, 1fr) 110px;
    gap: 8px;
  }
  .captcha-modal {
    padding: 24px 18px 16px;
    border-radius: 16px;
  }
}

/* Final auth layout: calm product-grade shell */
.login {
  min-height: 100vh;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(217, 117, 10, 0.10), transparent 32%),
    linear-gradient(180deg, #f6f0e7 0%, #e8dfd2 100%);
}
.login__wash {
  background:
    linear-gradient(120deg, rgba(20, 20, 19, 0.08), transparent 38%),
    linear-gradient(300deg, rgba(217, 117, 10, 0.10), transparent 42%);
  opacity: 1;
}
.login__grid {
  background-size: 36px 36px;
  opacity: 0.55;
  mask-image: linear-gradient(180deg, black 0%, rgba(0,0,0,0.38) 70%, transparent 100%);
}
.login__shell {
  position: relative;
  z-index: 1;
  width: min(920px, 100%);
  min-height: 560px;
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(360px, 1fr);
  overflow: hidden;
  border: 1px solid rgba(20, 20, 19, 0.10);
  border-radius: 22px;
  background: rgba(253, 252, 251, 0.86);
  box-shadow: 0 30px 80px rgba(20, 20, 19, 0.18);
  backdrop-filter: blur(24px);
}
.login__visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 34px;
  overflow: hidden;
  background:
    linear-gradient(145deg, rgba(20,20,19,0.96), rgba(42,34,25,0.94)),
    var(--bg-dark);
  color: #fff;
}
.login__visual::before {
  content: "";
  position: absolute;
  inset: 18px;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 18px;
  pointer-events: none;
}
.login__visual::after {
  content: "";
  position: absolute;
  width: 220px;
  height: 220px;
  right: -70px;
  bottom: -70px;
  border: 1px solid rgba(217, 117, 10, 0.34);
  border-radius: 999px;
  box-shadow: inset 0 0 60px rgba(217, 117, 10, 0.12);
}
.login__visual-top,
.login__visual-bottom,
.login__signal {
  position: relative;
  z-index: 1;
}
.login__eyebrow {
  width: fit-content;
  background: rgba(217, 117, 10, 0.14);
  color: #f0a04a;
  border-color: rgba(217, 117, 10, 0.30);
}
.login__brand {
  color: inherit;
  font-size: 46px;
}
.login__desc {
  color: rgba(255,255,255,0.62);
}
.login__signal {
  display: grid;
  gap: 12px;
  margin: 50px 0;
}
.login__signal span {
  display: block;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(217,117,10,0.86), rgba(255,255,255,0.08));
}
.login__signal span:nth-child(1) { width: 74%; }
.login__signal span:nth-child(2) { width: 48%; opacity: 0.72; }
.login__signal span:nth-child(3) { width: 62%; opacity: 0.48; }
.login__visual-bottom {
  display: grid;
  gap: 8px;
}
.login__visual-bottom span {
  color: rgba(255,255,255,0.38);
  font-size: 11px;
  font-weight: 800;
}
.login__visual-bottom strong {
  color: rgba(255,255,255,0.86);
  font-size: 18px;
}
.login__card {
  max-width: none;
  width: 100%;
  padding: 44px;
  align-self: center;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}
.login__card::before,
.login__card::after {
  display: none;
}
.login__tabs {
  position: relative;
  gap: 0;
  padding: 4px;
  margin-bottom: 28px;
  border-radius: 14px;
  background: rgba(20, 20, 19, 0.055);
}
.login__tab {
  min-height: 42px;
  border-radius: 11px;
  font-weight: 760;
}
.login__tab--active {
  background: #141413;
  color: #fff;
  box-shadow: 0 12px 22px rgba(20, 20, 19, 0.16);
}
.login__form-head {
  margin-bottom: 22px;
}
.login__form-title {
  font-size: 26px;
  font-weight: 800;
}
.login__form-subtitle {
  color: rgba(20,20,19,0.50);
}
.login__field {
  gap: 8px;
  margin-bottom: 14px;
}
.login__input {
  height: 50px;
  border-radius: 14px;
  background: rgba(255,255,255,0.76);
}
.login__btn {
  height: 52px;
  margin-top: 2px;
  border-radius: 14px;
}
.login__form-stage {
  perspective: none;
  transform-style: flat;
}
.form-shift-enter-active {
  transition: opacity 0.22s ease, transform 0.26s var(--ease-out-expo);
}
.form-shift-leave-active {
  position: absolute;
  width: 100%;
  top: 0;
  left: 0;
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.form-shift-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.form-shift-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 860px) {
  .login {
    padding: 18px;
    overflow-y: auto;
  }
  .login__shell {
    min-height: auto;
    grid-template-columns: 1fr;
    width: min(430px, 100%);
  }
  .login__visual {
    min-height: 178px;
    padding: 26px;
  }
  .login__brand {
    font-size: 36px;
  }
  .login__signal {
    display: none;
  }
  .login__visual-bottom {
    display: none;
  }
  .login__card {
    padding: 26px 22px 24px;
  }
}

@media (max-width: 430px) {
  .login {
    padding: 12px;
  }
  .login__shell {
    border-radius: 18px;
  }
  .login__visual {
    min-height: 150px;
    padding: 22px;
  }
  .login__card {
    padding: 22px 18px;
  }
  .login__form-title {
    font-size: 23px;
  }
  .login__code-row {
    grid-template-columns: minmax(0, 1fr);
  }
  .login__send-code {
    width: 100%;
  }
}

/* Polished single-card auth design */
.login {
  padding: 40px 20px;
  background:
    linear-gradient(145deg, rgba(217, 117, 10, 0.08), transparent 34%),
    linear-gradient(180deg, #f6efe4 0%, #e8ddce 100%);
}
.login__wash {
  background:
    linear-gradient(120deg, rgba(255,255,255,0.50), transparent 28%),
    radial-gradient(circle at 68% 26%, rgba(217,117,10,0.10), transparent 28%);
}
.login__grid {
  opacity: 0.45;
  background-size: 42px 42px;
  mask-image: none;
}
.login__shell {
  width: min(536px, calc(100vw - 40px));
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr;
  overflow: hidden;
  border: 1px solid rgba(20,20,19,0.08);
  border-radius: 24px;
  background: #fbfaf8;
  box-shadow: 0 28px 70px rgba(42, 34, 25, 0.20);
  backdrop-filter: none;
}
.login__visual {
  min-height: 220px;
  padding: 30px 32px;
  display: block;
  background:
    radial-gradient(circle at 90% 82%, rgba(217,117,10,0.20), transparent 34%),
    linear-gradient(145deg, #161513 0%, #211c17 100%);
  border-radius: 24px 24px 0 0;
}
.login__visual::before {
  inset: 22px;
  border-radius: 18px;
  border-color: rgba(255,255,255,0.08);
}
.login__visual::after {
  width: 210px;
  height: 210px;
  right: -34px;
  bottom: -42px;
  border-color: rgba(217, 117, 10, 0.38);
  box-shadow: inset 0 0 80px rgba(217, 117, 10, 0.10);
}
.login__visual-top {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: baseline;
  column-gap: 0;
  row-gap: 8px;
}
.login__eyebrow {
  grid-column: 1;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(217,117,10,0.16);
  color: #f4a64f;
  font-size: 12px;
  font-weight: 850;
}
.login__brand {
  grid-column: 2;
  align-self: center;
  margin-left: -2px;
  color: #fff;
  font-size: 44px;
  line-height: 1;
}
.login__desc {
  grid-column: 1 / -1;
  color: rgba(255,255,255,0.66);
  font-size: 17px;
}
.login__signal,
.login__visual-bottom {
  display: none;
}
.login__card {
  padding: 32px 28px 30px;
  background: #fbfaf8;
}
.login__tabs {
  height: 64px;
  padding: 5px;
  margin-bottom: 34px;
  border: 1px solid rgba(20,20,19,0.10);
  border-radius: 16px;
  background: #f0efed;
}
.login__tab {
  min-height: 52px;
  border-radius: 13px;
  color: rgba(20,20,19,0.68);
  font-size: 16px;
}
.login__tab--active {
  background: #11110f;
  color: #fff;
  box-shadow: 0 8px 16px rgba(20,20,19,0.16);
}
.login__form-head {
  margin-bottom: 26px;
}
.login__form-title {
  font-size: 28px;
  line-height: 1.15;
  font-weight: 850;
}
.login__form-subtitle {
  font-size: 14px;
  color: rgba(20,20,19,0.46);
}
.login__field {
  gap: 10px;
  margin-bottom: 18px;
}
.login__field span {
  color: rgba(20,20,19,0.62);
  font-size: 14px;
}
.login__input {
  height: 62px;
  padding: 0 20px;
  border-radius: 15px;
  background: #fff;
  border-color: rgba(20,20,19,0.09);
  font-size: 16px;
}
.login__input::placeholder {
  color: rgba(20,20,19,0.32);
}
.login__code-row {
  grid-template-columns: minmax(0, 1fr) 124px;
  gap: 10px;
}
.login__send-code {
  height: 62px;
  border-radius: 15px;
  background: #fff;
  font-size: 13px;
}
.login__btn {
  height: 64px;
  margin-top: 4px;
  border-radius: 16px;
  font-size: 17px;
  box-shadow: none;
}
.login__btn:disabled {
  opacity: 1;
  background: #a6a5a2;
  color: #fff;
}
.login__extra-link {
  margin-top: 22px;
  color: rgba(20,20,19,0.62);
  font-size: 14px;
}
.login__note {
  margin-top: 18px;
}
.form-shift-enter-active {
  transition: opacity 0.22s ease, transform 0.24s var(--ease-out-expo);
}
.form-shift-leave-active {
  transition: opacity 0.14s ease, transform 0.14s ease;
}

@media (max-width: 560px) {
  .login {
    padding: 26px 16px;
    justify-content: center;
  }
  .login__shell {
    width: min(100%, 420px);
    border-radius: 22px;
  }
  .login__visual {
    min-height: 188px;
    padding: 26px 24px;
  }
  .login__visual::before {
    inset: 20px;
  }
  .login__brand {
    font-size: 36px;
  }
  .login__desc {
    font-size: 15px;
  }
  .login__card {
    padding: 26px 22px 28px;
  }
  .login__tabs {
    height: 58px;
    margin-bottom: 28px;
  }
  .login__tab {
    min-height: 48px;
  }
  .login__form-title {
    font-size: 26px;
  }
  .login__input,
  .login__send-code {
    height: 56px;
  }
  .login__btn {
    height: 58px;
  }
}

@media (max-width: 390px) {
  .login {
    padding: 14px 10px;
  }
  .login__visual {
    min-height: 160px;
    padding: 22px 20px;
  }
  .login__brand {
    font-size: 32px;
  }
  .login__eyebrow {
    height: 24px;
    padding: 0 9px;
    font-size: 11px;
  }
  .login__code-row {
    grid-template-columns: 1fr;
  }
}

/* Minimal auth page */
.login {
  min-height: 100vh;
  padding: 32px 18px;
  background:
    radial-gradient(circle at 50% 8%, rgba(217, 117, 10, 0.08), transparent 30%),
    linear-gradient(180deg, #f7f7f4 0%, #e9ece7 100%);
}
.login__wash {
  background:
    linear-gradient(145deg, rgba(255,255,255,0.62), transparent 42%),
    linear-gradient(315deg, rgba(20,20,19,0.035), transparent 38%);
  opacity: 1;
}
.login__grid {
  opacity: 0.28;
  background-size: 40px 40px;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.72), transparent 88%);
}
.login__shell {
  position: relative;
  z-index: 1;
  display: block;
  width: min(460px, 100%);
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(20,20,19,0.08);
  border-radius: 18px;
  background: rgba(253,253,251,0.94);
  box-shadow: 0 22px 58px rgba(42, 38, 31, 0.12);
  backdrop-filter: blur(18px);
}
.login__visual {
  display: none;
}
.login__card {
  width: 100%;
  max-width: none;
  padding: 34px 32px 30px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}
.login__card::before,
.login__card::after {
  display: none;
}
.login__masthead {
  display: grid;
  justify-items: center;
  gap: 8px;
  margin-bottom: 26px;
  text-align: center;
}
.login__brand {
  grid-column: auto;
  margin-left: 0;
  color: #141413;
  font-family: var(--font-serif);
  font-size: 40px;
  line-height: 1;
  font-weight: 650;
  letter-spacing: 0;
}
.login__desc {
  grid-column: auto;
  margin: 0;
  color: rgba(20,20,19,0.46);
  font-size: 14px;
}
.login__tabs {
  height: 52px;
  padding: 4px;
  margin-bottom: 28px;
  border: 1px solid rgba(20,20,19,0.08);
  border-radius: 14px;
  background: rgba(20,20,19,0.045);
}
.login__tab {
  min-height: 42px;
  border-radius: 11px;
  color: rgba(20,20,19,0.66);
  font-size: 15px;
  font-weight: 760;
}
.login__tab--active {
  background: #141413;
  color: #fff;
  box-shadow: 0 8px 16px rgba(20,20,19,0.14);
}
.login__form-head {
  margin-bottom: 22px;
}
.login__form-title {
  font-size: 25px;
  line-height: 1.15;
  font-weight: 850;
}
.login__form-subtitle {
  font-size: 14px;
  color: rgba(20,20,19,0.45);
}
.login__field {
  gap: 8px;
  margin-bottom: 16px;
}
.login__field span {
  font-size: 13px;
  color: rgba(20,20,19,0.62);
}
.login__input,
.login__send-code {
  height: 54px;
  border-radius: 13px;
  background: #fff;
  border-color: rgba(20,20,19,0.09);
}
.login__input {
  padding: 0 16px;
  font-size: 15px;
}
.login__send-code {
  font-size: 13px;
  font-weight: 780;
}
.login__btn {
  height: 56px;
  margin-top: 2px;
  border-radius: 13px;
  background: #141413;
  font-size: 16px;
  box-shadow: none;
}
.login__btn:hover:not(:disabled) {
  box-shadow: 0 14px 24px rgba(20,20,19,0.16);
}
.login__btn:disabled {
  opacity: 1;
  background: #a6a5a2;
  color: #fff;
}
.login__extra-link {
  margin-top: 20px;
  color: rgba(20,20,19,0.58);
}

@media (max-width: 430px) {
  .login {
    padding: 14px 10px;
  }
  .login__shell {
    width: min(100%, 420px);
    border-radius: 16px;
  }
  .login__card {
    padding: 28px 20px 24px;
  }
  .login__brand {
    font-size: 34px;
  }
  .login__tabs {
    height: 50px;
    margin-bottom: 24px;
  }
  .login__tab {
    min-height: 40px;
  }
  .login__form-title {
    font-size: 24px;
  }
  .login__code-row {
    grid-template-columns: 1fr;
  }
  .login__send-code {
    width: 100%;
  }
}

/* Original open auth style with email registration */
.login {
  justify-content: center;
  padding: 40px 24px;
  background: #f3eee6;
}
.login__wash {
  background:
    radial-gradient(circle at 50% 24%, rgba(255,255,255,0.54), transparent 36%),
    linear-gradient(180deg, rgba(255,255,255,0.36), transparent 64%);
}
.login__grid {
  opacity: 0.20;
  background-size: 48px 48px;
  mask-image: none;
}
.login__shell {
  width: min(360px, calc(100vw - 40px));
  overflow: visible;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}
.login__card {
  padding: 0;
  background: transparent;
}
.login__masthead {
  gap: 8px;
  margin-bottom: 36px;
}
.login__masthead::after {
  content: "";
  width: 40px;
  height: 2px;
  margin-top: 28px;
  border-radius: 999px;
  background: rgba(20,20,19,0.12);
}
.login__brand {
  color: #141413;
  font-size: 36px;
  font-weight: 600;
}
.login__desc {
  color: rgba(20,20,19,0.42);
  font-size: 14px;
}
.login__tabs {
  height: 50px;
  padding: 0;
  margin-bottom: 26px;
  border: 1px solid rgba(20,20,19,0.08);
  border-radius: 13px;
  background: rgba(255,255,255,0.46);
}
.login__tab {
  min-height: 48px;
  border-radius: 12px;
  color: #141413;
  font-size: 16px;
  font-weight: 500;
}
.login__tab--active {
  background: #11110f;
  color: #fff;
  box-shadow: none;
}
.login__form-head {
  display: none;
}
.login__field {
  display: block;
  margin-bottom: 14px;
}
.login__field span {
  display: none;
}
.login__card--register .login__field,
.login__card--reset .login__field {
  display: grid;
  gap: 7px;
  margin-bottom: 13px;
}
.login__card--register .login__field span,
.login__card--reset .login__field span {
  display: block;
  color: rgba(20,20,19,0.52);
  font-size: 12px;
  font-weight: 600;
}
.login__input,
.login__send-code {
  height: 48px;
  border: 1px solid rgba(20,20,19,0.08);
  border-radius: 12px;
  background: rgba(255,255,255,0.78);
  box-shadow: none;
}
.login__input {
  padding: 0 16px;
  color: #141413;
  font-size: 16px;
}
.login__input::placeholder {
  color: rgba(20,20,19,0.42);
}
.login__input:focus {
  border-color: rgba(20,20,19,0.22);
  box-shadow: 0 0 0 4px rgba(20,20,19,0.045);
}
.login__code-row {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) 108px;
  gap: 8px;
  align-items: stretch;
  margin-bottom: 0;
  box-sizing: border-box;
}
.login__field--code-group {
  width: 100%;
}
.login__input--code {
  text-align: left;
  font-family: inherit;
}
.login__send-code {
  width: 100%;
  padding: 0 10px;
  color: #141413;
  font-size: 13px;
  font-weight: 600;
}
.login__send-code:hover:not(:disabled) {
  border-color: rgba(20,20,19,0.22);
  box-shadow: 0 8px 18px rgba(20,20,19,0.08);
}
.login__send-code:disabled {
  background: rgba(255,255,255,0.50);
  color: rgba(20,20,19,0.34);
}
.login__btn {
  height: 54px;
  margin-top: 0;
  border-radius: 16px;
  background: #11110f;
  color: #fff;
  font-size: 18px;
  font-weight: 500;
  box-shadow: none;
}
.login__btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 24px rgba(20,20,19,0.13);
}
.login__btn:disabled {
  opacity: 1;
  background: rgba(17,17,15,0.36);
}
.login__extra-link,
.login__note {
  color: rgba(20,20,19,0.52);
}
.login__extra-link {
  margin-top: 16px;
  font-size: 13px;
}
.login__note {
  margin-top: 14px;
  font-size: 12px;
}
.login__error {
  background: rgba(255,255,255,0.58);
}
.login__form-stage {
  min-height: 162px;
}
.login__card--register .login__form-stage {
  min-height: 374px;
}
.login__card--reset .login__form-stage {
  min-height: 276px;
}
.form-shift-enter-active,
.form-shift-leave-active,
.form-shift-enter-from,
.form-shift-leave-to {
  transition: none;
  transform: none;
  opacity: 1;
}

@media (max-width: 520px) {
  .login {
    padding: 30px 18px;
  }
  .login__shell {
    width: min(100%, 360px);
  }
  .login__masthead {
    margin-bottom: 34px;
  }
  .login__brand {
    font-size: 34px;
  }
  .login__desc {
    font-size: 14px;
  }
  .login__tab,
  .login__input,
  .login__btn {
    font-size: 16px;
  }
  .login__code-row {
    grid-template-columns: minmax(0, 1fr) 104px;
  }
  .login__send-code {
    font-size: 12px;
  }
}

@media (max-width: 390px) {
  .login {
    padding: 22px 14px;
  }
  .login__masthead {
    margin-bottom: 30px;
  }
  .login__brand {
    font-size: 32px;
  }
  .login__tabs {
    height: 48px;
  }
  .login__tab {
    min-height: 46px;
    font-size: 15px;
  }
  .login__input,
  .login__send-code {
    height: 48px;
  }
  .login__input {
    font-size: 15px;
  }
  .login__btn {
    height: 52px;
    border-radius: 16px;
    font-size: 16px;
  }
  .login__code-row {
    grid-template-columns: 1fr;
  }
  .login__form-stage,
  .login__card--register .login__form-stage,
  .login__card--reset .login__form-stage {
    min-height: auto;
  }
  .login__card--register .login__field,
  .login__card--reset .login__field {
    margin-bottom: 12px;
  }
}
</style>
