<template>
  <div class="page">
    <div class="page__inner stagger">
      <!-- Head -->
      <div class="profile-head">
        <div class="profile-avatar" v-if="!userStore.avatarUrl">
          <span class="profile-avatar__letter">{{ (userStore.nickname || '?')[0] }}</span>
        </div>
        <img class="profile-avatar profile-avatar--img" v-else :src="userStore.avatarUrl" alt="" />
        <span class="profile-name">{{ userStore.nickname || '未登录' }}</span>
        <span class="profile-id">ID: {{ userStore.userId }}</span>
      </div>

      <!-- Stats -->
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-num">{{ stats.practiceCount }}</span>
          <span class="stat-label">刷题数</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ stats.interviewCount }}</span>
          <span class="stat-label">面试次数</span>
        </div>
        <div class="stat-card">
          <span class="stat-num">{{ stats.wrongCount }}</span>
          <span class="stat-label">错题积累</span>
        </div>
      </div>

      <!-- Menu -->
      <div class="menu-group">
        <button class="menu-item" @click="$router.push('/interview/history')">
          <span class="menu-item__label">面试历史</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
        <button class="menu-item" @click="$router.push('/exam')">
          <span class="menu-item__label">在线试卷</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
        <button class="menu-item" @click="$router.push('/wrong-book')">
          <span class="menu-item__label">错题本</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>

      <div class="menu-group">
        <button class="menu-item" @click="showAiKey = true">
          <span class="menu-item__label">AI API Key</span>
          <span class="menu-item__hint" v-if="!aiKeyConfigured">未配置</span>
          <span class="menu-item__hint menu-item__hint--ok" v-else>{{ aiProvider }}</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>

      <div class="menu-group">
        <button class="menu-item menu-item--danger" @click="handleLogout">
          <span class="menu-item__label">退出登录</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const showAiKey = ref(false)
const aiKeyConfigured = ref(false)
const aiProvider = ref('')

const stats = ref({
  practiceCount: 0,
  interviewCount: 0,
  wrongCount: 0
})

function handleLogout() {
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 500px; margin: 0 auto; padding: 0 20px; }

/* Head */
.profile-head {
  padding: 48px 0 32px;
  display: flex; flex-direction: column; align-items: center;
}
.profile-avatar {
  width: 80px; height: 80px;
  border-radius: 24px;
  background: var(--bg-surface);
  border: 1px solid var(--border-medium);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px;
  overflow: hidden;
}
.profile-avatar--img { object-fit: cover; }
.profile-avatar__letter {
  font-family: var(--font-serif);
  font-size: 32px; font-weight: 600;
  color: var(--text-main);
}
.profile-name { font-size: 20px; font-weight: 600; }
.profile-id { font-size: 12px; color: var(--text-light); margin-top: 4px; }

/* Stats */
.stats-row {
  display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px;
  margin-bottom: 36px;
}
.stat-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px 12px;
  text-align: center;
  box-shadow: var(--shadow-sm);
}
.stat-num {
  font-size: 24px; font-weight: 700; display: block; margin-bottom: 4px;
  font-family: var(--font-serif);
}
.stat-label {
  font-size: 12px; color: var(--text-light);
}

/* Menu */
.menu-group {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
}
.menu-item {
  width: 100%;
  padding: 16px;
  display: flex; align-items: center; gap: 12px;
  font-size: 15px; color: var(--text-main);
  background: none; border: none; cursor: pointer;
  transition: background 0.15s;
}
.menu-item:not(:last-child) {
  border-bottom: 1px solid var(--border-light);
}
.menu-item:hover { background: var(--bg-surface); }
.menu-item__label { flex: 1; text-align: left; }
.menu-item__hint { font-size: 13px; color: var(--text-light); }
.menu-item__hint--ok { color: var(--color-success); }
.menu-item--danger .menu-item__label { color: var(--color-danger); }

.menu-item svg:last-child { flex-shrink: 0; }
</style>
