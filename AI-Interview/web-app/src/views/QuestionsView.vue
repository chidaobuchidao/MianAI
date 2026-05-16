<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">查看题库</span>
        <div style="width:36px" />
      </div>

      <div class="chip-row no-scrollbar">
        <TopicChip :active="!activeCategory" @click="selectCategory('')">全部</TopicChip>
        <TopicChip
          v-for="cat in categories"
          :key="cat.id"
          :active="activeCategory === cat.id"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </TopicChip>
      </div>

      <ScrollReveal :stagger="0.08" :y="20" :duration="0.6" v-if="questions.length > 0">
      <div class="q-list">
        <div class="q-card card-hover" v-for="(q, i) in questions" :key="q.id"
          @click="goDetail(q)">
          <span class="q-number">Q{{ i + 1 + (page - 1) * pageSize }}</span>
          <div class="q-content">
            <span class="q-title">{{ q.title }}</span>
            <span class="q-meta">{{ q.categoryName || '未分类' }} · {{ difficultyLabel(q.difficulty) }}</span>
          </div>
          <svg class="q-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </div>
      </div>
      </ScrollReveal>

      <div class="load-more" v-if="hasMore">
        <button class="load-more-btn" @click="loadMore" :disabled="loading">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/utils/request'
import TopicChip from '@/components/TopicChip.vue'
import ScrollReveal from '@/components/ScrollReveal.vue'

interface Category { id: number; name: string }
interface Question { id: number; title: string; categoryName: string; difficulty: number }

const router = useRouter()

const activeCategory = ref<string | number>('')
const categories = ref<Category[]>([])
const questions = ref<Question[]>([])
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)
const loading = ref(false)

function difficultyLabel(d: number) {
  return d === 3 ? '困难' : d === 2 ? '中等' : '简单'
}

function selectCategory(id: string | number) {
  activeCategory.value = id
  page.value = 1
  questions.value = []
  hasMore.value = true
  fetchQuestions()
}

async function fetchQuestions() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: pageSize }
    if (activeCategory.value) params.categoryId = activeCategory.value
    const res = await get<{ records: Question[]; total: number }>('/api/questions', params)
    const data = (res.data as any)
    const records = data.records || data || []
    if (page.value === 1) {
      questions.value = records
    } else {
      questions.value.push(...records)
    }
    const total = data.total || 0
    hasMore.value = questions.value.length < total
  } catch { /* ignore */ }
  loading.value = false
}

async function loadMore() {
  page.value++
  await fetchQuestions()
}

function goDetail(q: Question) {
  router.push({
    path: `/questions/${q.id}`,
    query: {
      categoryId: activeCategory.value ? String(activeCategory.value) : '',
      categoryName: q.categoryName || ''
    }
  })
}

onMounted(async () => {
  try {
    const catRes = await get<Category[]>('/api/questions/categories')
    categories.value = catRes.data || []
  } catch { /* ignore */ }
  fetchQuestions()
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
  background: none; border: none; cursor: pointer;
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
.q-chevron { flex-shrink: 0; margin-top: 2px; }
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

.load-more { text-align: center; padding: 24px 0; }
.load-more-btn {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-full); padding: 10px 32px; font-size: 14px;
  color: var(--text-muted); cursor: pointer;
}
</style>
