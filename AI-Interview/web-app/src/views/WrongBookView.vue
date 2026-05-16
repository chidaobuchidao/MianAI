<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">错题本</span>
        <div style="width:36px" />
      </div>

      <ScrollReveal :stagger="0.08" :y="20" :duration="0.6" v-if="questions.length > 0">
      <div class="q-list">
        <div class="q-card card-hover" v-for="(q, i) in questions" :key="i">
          <div class="q-info">
            <span class="q-title">{{ q.title }}</span>
            <span class="q-meta">{{ q.category }}</span>
          </div>
          <span class="q-tag" :class="q.wrongCount >= 3 ? 'q-tag--many' : 'q-tag--few'">错 {{ q.wrongCount }} 次</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </div>
      </div>
      </ScrollReveal>

      <div class="empty" v-else>
        <span class="empty__icon">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--text-light)" stroke-width="1.2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
        </span>
        <span class="empty__title">还没有错题</span>
        <p class="empty__desc">多练习几道题，错题会自动出现在这里</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'
import ScrollReveal from '@/components/ScrollReveal.vue'

interface WrongQ { questionId: number; title: string; category: string; wrongCount: number }
const questions = ref<WrongQ[]>([])

onMounted(async () => {
  try {
    const res = await get<WrongQ[]>('/api/wrong-questions')
    questions.value = res.data || []
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 700px; margin: 0 auto; padding: 0 20px; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0 24px;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main);
}
.page-head__title {
  font-family: var(--font-serif);
  font-size: 18px; font-weight: 600;
}

.q-list { display: flex; flex-direction: column; gap: 10px; }
.q-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
}
.q-info { flex: 1; min-width: 0; }
.q-tag {
  font-size: 10px; font-weight: 600; padding: 3px 8px; border-radius: 100px;
  flex-shrink: 0;
}
.q-tag--many { background: #FEF2F2; color: #DC2626; }
.q-tag--few { background: #FFF7ED; color: #EA580C; }
.q-title {
  font-size: 14px; font-weight: 500; display: block; margin-bottom: 2px;
}
.q-meta {
  font-size: 12px; color: var(--text-light);
}

.empty {
  text-align: center; padding-top: 160px;
}
.empty__icon { display: flex; justify-content: center; margin-bottom: 16px; }
.empty__icon svg { display: block; }
.empty__title { font-size: 16px; font-weight: 500; display: block; margin-bottom: 8px; }
.empty__desc { font-size: 13px; color: var(--text-light); }
</style>
