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
      <button class="login__btn" @click="handleLogin" :disabled="loading">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8.69 11.66c-.15-.93.06-1.86.5-2.63.61-1.06 1.7-1.72 2.86-1.61 1.16.1 2.07.77 2.56 1.71.08.16.27.21.41.1.64-.47 1.22-.72 1.94-.72 1.09 0 1.99.45 2.65 1.26.5.61.76 1.33.76 2.14 0 2.76-4.5 5.11-8.55 5.11-4.11 0-8.62-2.38-8.62-5.14 0-1.92 2.14-3.58 5.1-4.43.15-.04.26-.16.23-.31-.07-.45-.06-.86 0-1.2.03-.17-.06-.33-.22-.38-.94-.32-1.56-1.14-1.56-2.1 0-2.02 3.43-2.59 5.29-1.33.14.1.33.07.43-.06.58-.78 1.41-1.34 2.5-1.34 1.66 0 3 1.32 3 2.94 0 1.29-.85 2.41-2.05 2.81-.17.06-.28.22-.26.4.07.69.07 1.43 0 2.14-.02.15.1.3.25.28 1.76-.13 6.06.95 6.06 4.73C23.5 15.28 16.5 18 12 18c-4.66 0-11.5-2.84-11.5-6.5 0-2.05 1.97-3.63 4.57-4.55.14-.05.24-.19.21-.34-.07-.3-.07-.59 0-.85.03-.14-.06-.29-.2-.33C2.28 4.66 0 3.5 0 1.5 0 .67.7 0 1.56 0h.01c.84 0 1.53.66 1.54 1.47 0 .42-.19.78-.48 1.02-.11.09-.11.25 0 .34.63.47 1.33.47 1.93 0 .11-.09.11-.25.01-.34-.3-.24-.48-.6-.48-1.02 0-.81.69-1.47 1.54-1.47h.01c.87 0 1.56.67 1.56 1.5 0 1.1-.55 2.04-1.37 2.54-.12.08-.17.23-.12.36.1.22.2.41.31.57.09.12.25.15.37.06C7.52 3.75 8.77 3.33 10 3.33c.73 0 1.39.15 1.97.42.13.06.28.02.37-.1.43-.56.96-.98 1.62-1.14 1.09-.27 2.23.03 3.01.77.38.36.65.81.78 1.3.03.11.13.2.25.18.48-.08.92.13 1.22.48.28.33.43.74.43 1.2 0 .64-.38 1.2-.93 1.46-.13.06-.19.21-.14.35.22.68.29 1.35.2 1.98-.02.16-.19.26-.35.22-1.3-.23-2.52-.18-3.7.15-.15.04-.3-.05-.33-.2-.14-.62-.13-1.27.04-1.9.04-.15.19-.24.34-.2.61.15 1.27.2 1.93.15.17-.01.3-.15.28-.32-.05-.57-.23-1.12-.54-1.6-.06-.1-.19-.12-.28-.03-.66.66-1.53 1.07-2.51 1.07-.6 0-1.16-.14-1.68-.4-.1-.05-.23-.02-.29.07-.33.53-.82.94-1.42 1.12-.5.15-1.04.12-1.54-.07-.07-.03-.15-.01-.2.04-.41.42-.95.71-1.58.79-.16.02-.28.16-.27.32.06.53.02.96-.1 1.33-.03.08-.04.16-.04.24z"/>
        </svg>
        {{ loading ? '登录中...' : '微信一键登录' }}
      </button>
      <p class="login__error" v-if="errorMsg">{{ errorMsg }}</p>
      <p class="login__note">登录即表示同意服务条款与隐私政策</p>
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

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  loading.value = true
  errorMsg.value = ''
  try {
    // Dev mode: use a test code (WeChat OAuth in production)
    const devCode = 'dev-code-' + Date.now()
    const res = await post<LoginResponse>('/api/auth/login', {
      code: devCode,
      nickname: '开发者',
      avatarUrl: ''
    })
    if (res.code === 200 && res.data) {
      userStore.setUser({
        userId: res.data.userId,
        nickname: res.data.nickname || '用户',
        avatarUrl: res.data.avatarUrl || '',
        token: res.data.token
      })
      router.push('/')
    } else {
      errorMsg.value = (res as any).message || '登录失败'
    }
  } catch (e: unknown) {
    // Backend might not support dev login — fallback to mock
    userStore.setUser({
      userId: 1,
      nickname: '开发者',
      avatarUrl: '',
      token: 'dev-token-' + Date.now()
    })
    router.push('/')
  } finally {
    loading.value = false
  }
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
