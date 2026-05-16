<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">题目详情</span>
        <span class="page-head__cat">{{ currentQ?.categoryName || '' }}</span>
      </div>

      <div class="detail-stage">
        <div class="panel" :style="panelStyle" v-if="currentQ" :key="currentQ.id">
          <QuestionContent :q="currentQ" />
        </div>
        <SkeletonBar v-else :height="200" />
      </div>
    </div>

    <div class="detail-actions">
      <button class="action-btn action-sec" :disabled="!hasPrev" @click="goPrev">&lt; 上一题</button>
      <button class="action-btn action-pri" :disabled="!hasNext" @click="goNext">下一题 &gt;</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/utils/request'
import SkeletonBar from '@/components/SkeletonBar.vue'
import QuestionContent from '@/components/QuestionContent.vue'

interface Question {
  id: number; categoryName: string; type: number; title: string;
  options: string; answer: string; analysis: string; difficulty: number;
}

const route = useRoute()
const cache = new Map<number, Question>()

const currentQ = ref<Question | null>(null)
const questionIds = ref<number[]>([])
const currentIndex = ref(-1)
const panelX = ref(0)
const sliding = ref(false)
const slideDir = ref<'left' | 'right'>('left')

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < questionIds.value.length - 1)

const panelStyle = computed(() => ({
  transform: `translateX(${panelX.value}%)`,
  transition: sliding.value ? 'transform 0.35s cubic-bezier(0.22, 0.1, 0.1, 1)' : 'none'
}))

function preload(id: number) {
  if (!id || cache.has(id)) return
  get<Question>(`/api/questions/${id}`).then(res => {
    if (res.data) cache.set(id, res.data)
  }).catch(() => {})
}

function preloadAdjacent() {
  const idx = currentIndex.value
  if (idx < 0) return
  preload(questionIds.value[idx - 1])
  preload(questionIds.value[idx + 1])
}

function switchTo(idx: number, dir: 'left' | 'right') {
  if (sliding.value || idx < 0 || idx >= questionIds.value.length) return
  sliding.value = true
  slideDir.value = dir
  const targetId = questionIds.value[idx]

  // Slide out: panel moves to off-screen
  panelX.value = dir === 'left' ? -100 : 100

  // At mid-point: swap content, reposition to opposite side
  setTimeout(() => {
    currentQ.value = cache.get(targetId)!
    currentIndex.value = idx
    panelX.value = dir === 'left' ? 100 : -100

    // Slide in: panel moves back to center
    requestAnimationFrame(() => {
      panelX.value = 0
    })
  }, 180)

  // After animation: clean up
  setTimeout(() => {
    sliding.value = false
    preloadAdjacent()
  }, 560)
}

function goPrev() {
  switchTo(currentIndex.value - 1, 'right')
}

function goNext() {
  switchTo(currentIndex.value + 1, 'left')
}

onMounted(async () => {
  const categoryId = route.query.categoryId as string || ''
  const initialId = Number(route.params.id) || 0

  try {
    const params: Record<string, unknown> = { size: 200 }
    if (categoryId) params.categoryId = categoryId
    const res = await get<{ records: Question[] }>('/api/questions', params)
    const data = (res.data as any)
    const records: Question[] = data.records || data || []
    questionIds.value = records.map(q => q.id)

    if (initialId) {
      // Load initial + preload adjacent
      const q = await get<Question>(`/api/questions/${initialId}`).then(r => r.data)
      if (q) {
        cache.set(initialId, q)
        currentQ.value = q
        currentIndex.value = questionIds.value.indexOf(initialId)
        preloadAdjacent()
      }
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page {
  min-height: 100vh; background: var(--bg-canvas);
  display: flex; flex-direction: column;
}
.page__inner { max-width: 700px; margin: 0 auto; padding: 0 20px; width: 100%; flex: 1; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0 20px; gap: 12px;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main); flex-shrink: 0;
  background: none; border: none; cursor: pointer;
}
.page-head__title { font-family: var(--font-serif); font-size: 18px; font-weight: 600; }
.page-head__cat { font-size: 12px; color: var(--text-light); }

.detail-stage {
  overflow: hidden;
  min-height: 200px;
}

.panel {
  padding-bottom: 20px;
  will-change: transform;
}

.detail-actions {
  display: flex; gap: 12px; padding: 16px 20px 32px;
  max-width: 700px; margin: 0 auto; width: 100%;
}
.action-btn {
  flex: 1; padding: 14px; border-radius: var(--radius-lg);
  font-size: 15px; font-weight: 500; cursor: pointer; text-align: center;
}
.action-sec {
  background: var(--bg-paper); border: 1px solid var(--border-medium);
  color: var(--text-main);
}
.action-sec:disabled { opacity: 0.3; cursor: not-allowed; }
.action-pri {
  background: var(--bg-dark); color: #fff; border: none;
  box-shadow: var(--shadow-md);
}
.action-pri:disabled { opacity: 0.3; cursor: not-allowed; }
</style>
