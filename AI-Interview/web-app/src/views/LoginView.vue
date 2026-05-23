<template>
  <div class="login">
    <ParticleBg
      :particleCount="120"
      :particleSpread="8"
      :speed="0.06"
      :alphaParticles="true"
      :particleBaseSize="60"
      :sizeRandomness="0.6"
      :moveOnHover="true"
      :hoverFactor="0.4"
      :disableRotation="false"
    />
    <div class="login__card animate-fade-in-up">
      <span class="login__brand">Mianmian.</span>
      <p class="login__desc">AI 模拟面试平台</p>
      <div class="login__divider" />

      <!-- Tabs -->
      <div class="login__tabs">
        <button class="login__tab" :class="{ 'login__tab--active': tab === 'login' }" @click="tab = 'login'">登录</button>
        <button class="login__tab" :class="{ 'login__tab--active': tab === 'register' }" @click="tab = 'register'">注册</button>
      </div>

      <input class="login__input" v-model="username" placeholder="用户名" :disabled="loading" />
      <input class="login__input" v-model="password" type="password" placeholder="密码" :disabled="loading" @keydown.enter="handleSubmit" />
      <input class="login__input" v-if="tab === 'register'" v-model="nickname" placeholder="昵称（选填）" :disabled="loading" />

      <button class="login__btn" @click="handleSubmit" :disabled="loading || !username || !password">
        {{ loading ? '处理中...' : tab === 'register' ? '注册' : '登录' }}
      </button>

      <p class="login__error" v-if="errorMsg">{{ errorMsg }}</p>
    </div>
    <div class="bottom-safe" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { post } from '@/utils/request'
import ParticleBg from '@/components/ParticleBg.vue'

interface LoginResponse {
  token: string
  userId: number
  nickname: string
  avatarUrl: string
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
const tab = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const nickname = ref('')

async function handleSubmit() {
  if (!username.value || !password.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    if (tab.value === 'register') {
      const res = await post<LoginResponse>('/api/auth/register', {
        username: username.value,
        password: password.value,
        nickname: nickname.value || username.value
      })
      if (res.code === 200 && res.data) {
        loginSuccess(res.data)
      } else {
        errorMsg.value = (res as any).message || '注册失败'
      }
    } else {
      const res = await post<LoginResponse>('/api/auth/login/password', {
        username: username.value,
        password: password.value
      })
      if (res.code === 200 && res.data) {
        loginSuccess(res.data)
      } else {
        errorMsg.value = (res as any).message || '登录失败'
      }
    }
  } catch (e: any) {
    errorMsg.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

function loginSuccess(data: LoginResponse) {
  userStore.setUser({
    userId: data.userId,
    nickname: data.nickname || '用户',
    avatarUrl: data.avatarUrl || '',
    token: data.token
  })
  const payload = decodeJwtPayload(data.token)
  userStore.setAdmin(payload?.role === 1)
  router.push('/')
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  background: var(--bg-canvas);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  position: relative;
}
.bg-pattern {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;
  background-image:
    radial-gradient(circle at 50% 30%, rgba(217,117,10,0.05) 0%, transparent 60%),
    radial-gradient(circle at 50% 70%, rgba(20,20,19,0.04) 0%, transparent 50%);
}
.login__card {
  position: relative; z-index: 1;
  text-align: center;
  max-width: 360px;
  width: 100%;
}
.login__brand {
  font-family: var(--font-serif);
  font-size: 36px;
  font-weight: 600;
  letter-spacing: -1px;
  color: var(--text-main);
}
.login__desc {
  color: var(--text-light);
  font-size: 14px;
  margin-top: 8px;
}
.login__divider {
  width: 40px; height: 2px;
  background: var(--border-medium);
  margin: 36px auto;
}
.login__tabs {
  display: flex; gap: 0; margin-bottom: 20px;
  border-radius: var(--radius-md); border: 1px solid var(--border-light);
  overflow: hidden;
}
.login__tab {
  flex: 1; padding: 10px; font-size: 14px; font-weight: 500; cursor: pointer;
  border: none; background: var(--bg-surface); color: var(--text-muted);
  transition: all 0.15s;
}
.login__tab--active { background: var(--bg-dark); color: #fff; }
.login__input {
  width: 100%; padding: 14px 16px; margin-bottom: 12px;
  border: 1px solid var(--border-light); border-radius: var(--radius-md);
  font-size: 15px; outline: none; font-family: inherit;
  background: var(--bg-surface); transition: border-color 0.15s;
}
.login__input:focus { border-color: var(--text-main); }
.login__btn {
  width: 100%;
  background: var(--bg-dark);
  color: #fff;
  padding: 16px;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  box-shadow: var(--shadow-md);
  transition: transform 0.15s;
}
.login__btn:active { transform: scale(0.98); }
.login__error {
  font-size: 13px; color: var(--color-danger); margin-top: 16px;
}
.login__note {
  font-size: 11px;
  color: var(--text-light);
  margin-top: 20px;
}
.bottom-safe { height: 40px; }
</style>
