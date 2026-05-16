<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">在线试卷</span>
        <div style="width:36px" />
      </div>

      <div class="hero-banner animate-fade-in-up">
        <span class="hero-banner__label">限时模拟</span>
        <h1 class="hero-banner__title">在线试卷</h1>
        <p class="hero-banner__desc">模拟真实考试环境，限时作答，查漏补缺</p>
      </div>

      <div class="exam-grid stagger" v-if="exams.length > 0">
        <div class="exam-card card-hover" v-for="e in exams" :key="e.id" @click="$router.push(`/exam/do?id=${e.id}`)">
          <div class="exam-card__icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/>
            </svg>
          </div>
          <span class="exam-card__title">{{ e.name }}</span>
          <span class="exam-card__meta">{{ e.questionCount }} 题 · {{ e.timeLimit }} 分钟</span>
        </div>
      </div>

      <div class="empty" v-else>
        <span class="empty__icon">📝</span>
        <span class="empty__title">暂无可用的试卷</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Exam { id: number; name: string; questionCount: number; timeLimit: number }
const exams = ref<Exam[]>([])
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 640px; margin: 0 auto; padding: 0 20px; }

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

.hero-banner {
  margin-bottom: 32px;
}
.hero-banner__label {
  font-size: 12px; font-weight: 600; color: var(--accent);
  letter-spacing: 3px; text-transform: uppercase; display: block; margin-bottom: 8px;
}
.hero-banner__title {
  font-family: var(--font-serif); font-size: 32px; font-weight: 600;
  letter-spacing: -1px; margin-bottom: 8px;
}
.hero-banner__desc {
  font-size: 14px; color: var(--text-light);
}

.exam-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.exam-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 22px 18px;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
}
.exam-card__icon {
  width: 44px; height: 44px;
  background: rgba(217,117,10,0.06);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 14px;
}
.exam-card__title {
  font-size: 16px; font-weight: 500; display: block; margin-bottom: 4px;
}
.exam-card__meta {
  font-size: 12px; color: var(--text-light);
}

.empty {
  text-align: center; padding-top: 100px;
}
.empty__icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty__title { font-size: 16px; color: var(--text-light); }
</style>
