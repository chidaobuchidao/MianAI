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
          <input class="modal-input" v-model="aiProvider" placeholder="Provider (deepseek / qwen)" />
          <input class="modal-input" v-model="apiKey" placeholder="API Key" type="password" />
          <input class="modal-input" v-model="apiModel" placeholder="Model (如 deepseek-v4-flash)" />
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
const aiProvider = ref('')
const apiKey = ref('')
const apiModel = ref('')

interface Stats { practiceCount: number; interviewCount: number; wrongCount: number }
const stats = ref<Stats>({ practiceCount: 0, interviewCount: 0, wrongCount: 0 })

onMounted(async () => {
  try {
    const [sRes, aRes] = await Promise.all([
      get<Stats>('/api/user/stats'),
      get<{ provider: string; model: string }>('/api/user/ai-config')
    ])
    if (sRes.data) stats.value = sRes.data
    if (aRes.data) {
      aiKeyConfigured.value = true
      aiProvider.value = aRes.data.provider || ''
      apiModel.value = aRes.data.model || ''
    }
  } catch {}
})

async function saveAiKey() {
  try {
    await put('/api/user/ai-config', {
      provider: aiProvider.value || 'deepseek',
      apiKey: apiKey.value,
      model: apiModel.value || 'deepseek-v4-flash'
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
.modal-actions { display: flex; gap: 10px; margin-top: 20px; justify-content: flex-end; }
.modal-actions .btn { padding: 10px 24px; border-radius: 100px; font-size: 14px; cursor: pointer; border: none; }
.modal-actions .btn--ghost { background: var(--bg-surface); color: var(--text-muted); }
.modal-actions .btn--dark { background: var(--bg-dark); color: #fff; }

</style>
