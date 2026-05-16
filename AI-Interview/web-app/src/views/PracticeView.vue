<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">自由刷题</span>
        <div style="width:36px" />
      </div>

      <!-- Category chips -->
      <div class="chip-row no-scrollbar">
        <TopicChip :active="!activeCategory" @click="activeCategory = ''">全部</TopicChip>
        <TopicChip
          v-for="cat in categories"
          :key="cat.id"
          :active="activeCategory === cat.id"
          @click="activeCategory = cat.id"
        >
          {{ cat.name }}
        </TopicChip>
      </div>

      <!-- Question list -->
      <ScrollReveal :stagger="0.08" :y="20" :duration="0.6" v-if="questions.length > 0">
      <div class="q-list">
        <div class="q-card card-hover" v-for="(q, i) in questions" :key="i">
          <span class="q-number">Q{{ i + 1 }}</span>
          <div class="q-content">
            <span class="q-title">{{ q.title }}</span>
            <span class="q-meta">{{ q.categoryName || '未分类' }} · {{ q.difficulty === 3 ? '困难' : q.difficulty === 2 ? '中等' : '简单' }}</span>
          </div>
        </div>
      </div>
      </ScrollReveal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { get } from '@/utils/request'
import TopicChip from '@/components/TopicChip.vue'
import ScrollReveal from '@/components/ScrollReveal.vue'

interface Category { id: number | string; name: string }
interface Question { id: number; title: string; categoryName: string; difficulty: number }

const activeCategory = ref<string | number>('')
const categories = ref<Category[]>([])
const questions = ref<Question[]>([])

onMounted(async () => {
  try {
    const [catRes, qRes] = await Promise.all([
      get<Category[]>('/api/questions/categories'),
      get<{ records: Question[] }>('/api/questions?size=50')
    ])
    categories.value = catRes.data || []
    questions.value = (qRes.data as any)?.records || qRes.data || []
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 700px; margin: 0 auto; padding: 0 20px; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0 20px;
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

.chip-row {
  display: flex; gap: 10px; overflow-x: auto;
  padding-bottom: 6px; margin-bottom: 24px;
}

.q-list { display: flex; flex-direction: column; gap: 10px; }
.q-card {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
}
.q-number {
  font-size: 13px; font-weight: 600; color: var(--accent);
  flex-shrink: 0; margin-top: 2px;
}
.q-content { flex: 1; }
.q-title {
  font-size: 15px; font-weight: 500; display: block; margin-bottom: 4px; line-height: 1.5;
}
.q-meta {
  font-size: 12px; color: var(--text-light);
}
</style>
