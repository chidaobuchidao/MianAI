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

      <div class="detail-stage" v-if="currentQ">
        <div class="panel" :style="mainStyle">
          <QuestionContent :q="currentQ" />
        </div>
        <div class="panel panel--overlay" v-if="slideQ" :style="slideStyle">
          <QuestionContent :q="slideQ" />
        </div>
      </div>

      <div class="detail-stage" v-else>
        <SkeletonBar :height="200" />
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

const currentQ = ref<Question | null>(null)
const slideQ = ref<Question | null>(null)
const questionIds = ref<number[]>([])
const currentIndex = ref(-1)
const animating = ref(false)
const mainX = ref(0)
const slideX = ref(100)
const cache = new Map<number, Question>()

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < questionIds.value.length - 1)

const mainStyle = computed(() => ({
  transform: `translateX(${mainX.value}%)`,
  transition: animating.value ? 'transform 0.4s cubic-bezier(0.22, 0.1, 0.1, 1)' : 'none'
}))

const slideStyle = computed(() => ({
  transform: `translateX(${slideX.value}%)`,
  transition: animating.value ? 'transform 0.4s cubic-bezier(0.22, 0.1, 0.1, 1)' : 'none'
}))

function preloadAdjacent() {
  const idx = currentIndex.value
  if (idx < 0) return
  for (const i of [idx - 1, idx + 1]) {
    if (i < 0 || i >= questionIds.value.length) continue
    const id = questionIds.value[i]
    if (cache.has(id)) continue
    get<Question>(`/api/questions/${id}`).then(res => {
      if (res.data) cache.set(id, res.data)
    }).catch(() => {})
  }
}

async function loadQuestion(id: number) {
  if (cache.has(id)) return cache.get(id)!
  const res = await get<Question>(`/api/questions/${id}`)
  if (res.data) cache.set(id, res.data)
  return res.data
}

function goPrev() {
  if (!hasPrev.value || animating.value) return
  const idx = currentIndex.value - 1
  const prevQ = cache.get(questionIds.value[idx])!
  slideQ.value = prevQ
  animating.value = true
  slideX.value = -100
  requestAnimationFrame(() => {
    mainX.value = 100
    slideX.value = 0
  })
  setTimeout(() => commitSlide(prevQ, idx), 410)
}

function goNext() {
  if (!hasNext.value || animating.value) return
  const idx = currentIndex.value + 1
  const nextQ = cache.get(questionIds.value[idx])!
  slideQ.value = nextQ
  animating.value = true
  slideX.value = 100
  requestAnimationFrame(() => {
    mainX.value = -100
    slideX.value = 0
  })
  setTimeout(() => commitSlide(nextQ, idx), 410)
}

function commitSlide(q: Question, idx: number) {
  currentQ.value = q
  currentIndex.value = idx
  slideQ.value = null
  animating.value = false
  mainX.value = 0
  slideX.value = idx > currentIndex.value ? 100 : -100
  preloadAdjacent()
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
      const q = await loadQuestion(initialId)
      currentQ.value = q
      currentIndex.value = questionIds.value.indexOf(initialId)
      preloadAdjacent()
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
  position: relative;
  overflow: hidden;
  min-height: 200px;
}

.panel {
  padding-bottom: 20px;
  will-change: transform;
}
.panel--overlay {
  position: absolute; top: 0; left: 0; width: 100%;
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
