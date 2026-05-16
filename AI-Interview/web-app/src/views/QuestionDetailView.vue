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
        <span class="page-head__cat">{{ categoryName || '' }}</span>
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
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get } from '@/utils/request'
import SkeletonBar from '@/components/SkeletonBar.vue'
import QuestionContent from '@/components/QuestionContent.vue'

interface Question {
  id: number; categoryName: string; type: number; title: string;
  options: string; answer: string; analysis: string; difficulty: number;
}

const route = useRoute()
const router = useRouter()

const questionId = computed(() => Number(route.params.id))
const categoryId = computed(() => route.query.categoryId as string || '')
const categoryName = computed(() => route.query.categoryName as string || '')

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

async function fetchAndShow(id: number) {
  if (cache.has(id)) {
    currentQ.value = cache.get(id)!
    return
  }
  try {
    const res = await get<Question>(`/api/questions/${id}`)
    currentQ.value = res.data
    if (res.data) cache.set(id, res.data)
  } catch { currentQ.value = null }
}

function goPrev() {
  if (!hasPrev.value || animating.value) return
  const prevQ = cache.get(questionIds.value[currentIndex.value - 1])!
  slideQ.value = prevQ
  animating.value = true
  slideX.value = -100
  requestAnimationFrame(() => {
    mainX.value = 100
    slideX.value = 0
  })
  const newId = prevQ.id
  setTimeout(() => {
    currentQ.value = prevQ
    currentIndex.value--
    slideQ.value = null
    animating.value = false
    mainX.value = 0
    slideX.value = -100
    router.replace({ path: `/questions/${newId}`, query: route.query }).then(() => preloadAdjacent())
  }, 410)
}

function goNext() {
  if (!hasNext.value || animating.value) return
  const nextQ = cache.get(questionIds.value[currentIndex.value + 1])!
  slideQ.value = nextQ
  animating.value = true
  slideX.value = 100
  requestAnimationFrame(() => {
    mainX.value = -100
    slideX.value = 0
  })
  const newId = nextQ.id
  setTimeout(() => {
    currentQ.value = nextQ
    currentIndex.value++
    slideQ.value = null
    animating.value = false
    mainX.value = 0
    slideX.value = 100
    router.replace({ path: `/questions/${newId}`, query: route.query }).then(() => preloadAdjacent())
  }, 410)
}

watch(questionId, async (id) => {
  if (!id || animating.value) return
  await fetchAndShow(id)
  currentIndex.value = questionIds.value.indexOf(id)
  preloadAdjacent()
}, { immediate: true })

onMounted(async () => {
  try {
    const params: Record<string, unknown> = { size: 200 }
    if (categoryId.value) params.categoryId = categoryId.value
    const res = await get<{ records: Question[] }>('/api/questions', params)
    const data = (res.data as any)
    const records: Question[] = data.records || data || []
    questionIds.value = records.map(q => q.id)
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

/* Actions */
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
