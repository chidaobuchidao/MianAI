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

      <!-- Quota -->
      <div class="quota-banner" v-if="quota && !quota.isAdmin && !quota.hasApiKey">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>剩余免费次数：<strong>{{ quota.quotaRemaining ?? 0 }}</strong> 次 · <a href="#" @click.prevent="showAiKey = true">配置 API Key</a> 后无限</span>
      </div>
      <div class="quota-banner quota-banner--ok" v-else-if="quota && !quota.isAdmin && quota.hasApiKey">
        <span>已配置个人 API Key，不限使用次数</span>
      </div>

      <!-- Menu -->
      <div class="menu-group">
        <button class="menu-item" @click="$router.push('/interview/history')">
          <span class="menu-item__label">面试历史</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </button>
        <button class="menu-item" @click="$router.push('/practice')">
          <span class="menu-item__label">自由刷题</span>
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
        <button class="menu-item" @click="$router.push('/admin')" v-if="quota?.isAdmin">
          <span class="menu-item__label">管理后台</span>
          <span class="menu-item__hint menu-item__hint--ok">Admin</span>
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

      <!-- AI Key Modal -->
      <div class="modal-overlay" v-if="showAiKey" @click.self="showAiKey = false">
        <div class="modal-card animate-scale-in">
          <span class="modal-title">配置 AI API Key</span>

          <div class="tab-row">
            <button class="tab" :class="{ 'tab--active': aiTab === 'deepseek' }" @click="aiTab = 'deepseek'">DeepSeek</button>
            <button class="tab" :class="{ 'tab--active': aiTab === 'qwen' }" @click="aiTab = 'qwen'">千问</button>
          </div>

          <input class="modal-input" v-model="apiKey" placeholder="API Key" type="password" />
          <p class="modal-hint">{{ aiTab === 'deepseek' ? '使用 DeepSeek API，模型 deepseek-chat' : '使用千问 API，模型 qwen-turbo' }}</p>

          <a class="modal-apikey-link" href="https://platform.deepseek.com/usage" target="_blank">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            ApiKey 获取（DeepSeek 控制台）
          </a>
          <div class="modal-actions">
            <button class="btn btn--ghost" @click="showAiKey = false">取消</button>
            <button class="btn btn--dark" @click="saveAiKey">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { get, put } from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const showAiKey = ref(false)
const aiKeyConfigured = ref(false)
const aiTab = ref<'deepseek' | 'qwen'>('deepseek')
const apiKey = ref('')
const quota = ref<{ hasApiKey: boolean; isAdmin: boolean; dailyQuota: number; quotaUsed: number; quotaRemaining: number } | null>(null)

interface Stats { practiceCount: number; interviewCount: number; wrongCount: number }
const stats = ref<Stats>({ practiceCount: 0, interviewCount: 0, wrongCount: 0 })

async function fetchProfileData() {
  try {
    const [sRes, aRes, qRes] = await Promise.all([
      get<Stats>('/api/user/stats'),
      get<{ provider: string; model: string }>('/api/user/ai-config'),
      get<{ hasApiKey: boolean; isAdmin: boolean; dailyQuota: number; quotaUsed: number; quotaRemaining: number }>('/api/user/quota')
    ])
    if (sRes.data) stats.value = sRes.data
    if (aRes.data) {
      aiKeyConfigured.value = true
      aiTab.value = (aRes.data.provider === 'qwen' ? 'qwen' : 'deepseek')
    }
    if (qRes.data) {
      quota.value = qRes.data
      userStore.setAdmin(qRes.data.isAdmin === true)
    }
  } catch {}
}

onMounted(fetchProfileData)

async function saveAiKey() {
  if (!apiKey.value.trim()) return
  try {
    const isDeepseek = aiTab.value === 'deepseek'
    await put('/api/user/ai-config', {
      provider: isDeepseek ? 'deepseek' : 'qwen',
      apiKey: apiKey.value,
      model: isDeepseek ? 'deepseek-chat' : 'qwen-turbo'
    })
    aiKeyConfigured.value = true
    showAiKey.value = false
  } catch { alert('保存失败') }
}

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
.modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(20,20,19,0.5);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.modal-card {
  background: var(--bg-paper); border-radius: var(--radius-xl);
  padding: 32px 28px; max-width: 380px; width: calc(100% - 40px);
  box-shadow: var(--shadow-xl);
}
.modal-title {
  font-family: var(--font-serif); font-size: 18px; font-weight: 600;
  display: block; margin-bottom: 20px;
}
.modal-input {
  width: 100%; padding: 12px 14px; margin-bottom: 12px;
  border: 1px solid var(--border-light); border-radius: var(--radius-md);
  font-size: 14px; outline: none; font-family: inherit;
  background: var(--bg-surface); transition: border-color 0.15s;
}
.modal-input:focus { border-color: var(--text-main); }
.modal-hint { font-size: 12px; color: var(--text-light); margin-bottom: 4px; }
.tab-row { display: flex; gap: 0; margin-bottom: 16px; border-radius: var(--radius-md); border: 1px solid var(--border-light); overflow: hidden; }
.tab { flex: 1; padding: 10px; font-size: 14px; font-weight: 500; cursor: pointer; border: none; background: var(--bg-surface); color: var(--text-muted); transition: all 0.15s; }
.tab--active { background: var(--bg-dark); color: #fff; }
.modal-actions { display: flex; gap: 10px; margin-top: 20px; justify-content: flex-end; }
.modal-actions .btn { padding: 10px 24px; border-radius: 100px; font-size: 14px; cursor: pointer; border: none; }
.modal-actions .btn--ghost { background: var(--bg-surface); color: var(--text-muted); }
.modal-actions .btn--dark { background: var(--bg-dark); color: #fff; }

.modal-apikey-link {
  display: inline-flex; align-items: center; gap: 6px;
  margin-bottom: 14px;
  font-size: 12px; color: var(--accent);
  text-decoration: none;
  transition: opacity 0.15s;
}
.modal-apikey-link:hover { opacity: 0.8; }

.quota-banner {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px;
  margin: 0 0 16px;
  background: rgba(217,117,10,0.06);
  border: 1px solid rgba(217,117,10,0.12);
  border-radius: var(--radius-md);
  font-size: 13px; color: var(--accent); line-height: 1.5;
}
.quota-banner strong { color: var(--text-main); }
.quota-banner a { color: var(--accent); font-weight: 600; }
.quota-banner--ok {
  background: rgba(34,197,94,0.05);
  border-color: rgba(34,197,94,0.12);
  color: var(--color-success);
}

</style>
