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

      <div class="detail-stage">
        <Transition :name="slideDirection" mode="out-in">
          <div class="detail-panel" v-if="question" :key="questionId">

            <div class="question-card">
              <span class="q-type">{{ typeLabel(question.type) }}</span>
              <h3 class="q-text">{{ question.title }}</h3>

              <div v-if="question.type <= 2" class="options">
                <div
                  v-for="opt in parseOptions(question.options)"
                  :key="opt.label"
                  class="option-btn"
                  :class="{ correct: opt.label === question.answer }"
                >
                  <span class="option-letter">{{ opt.label }}</span>
                  <span class="option-text">{{ opt.content }}</span>
                  <svg v-if="opt.label === question.answer" class="option-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
              </div>

              <div v-if="question.type === 3" class="options">
                <div class="option-btn" :class="{ correct: question.answer === '正确' }">
                  <span class="option-letter">&#10003;</span>
                  <span class="option-text">正确</span>
                </div>
                <div class="option-btn" :class="{ correct: question.answer === '错误' }">
                  <span class="option-letter">&#10007;</span>
                  <span class="option-text">错误</span>
                </div>
              </div>

              <div v-if="question.type === 4" class="fill-answer">
                <span class="fill-label">参考答案：</span>
                <span class="fill-text">{{ question.answer }}</span>
              </div>
            </div>

            <div class="analysis-card" v-if="question.analysis">
              <span class="analysis-title">答案解析</span>
              <p class="analysis-text">{{ question.analysis }}</p>
            </div>

            <div class="detail-tags">
              <span class="detail-tag">{{ question.categoryName || '未分类' }}</span>
              <span class="detail-tag">{{ difficultyLabel(question.difficulty) }}</span>
            </div>

          </div>

          <div class="detail-panel" v-else :key="'skeleton-' + questionId">
            <SkeletonBar :height="200" />
          </div>
        </Transition>
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

interface Option { label: string; content: string }
interface Question {
  id: number; categoryName: string; type: number; title: string;
  options: string; answer: string; analysis: string; difficulty: number;
}

const route = useRoute()
const router = useRouter()

const questionId = computed(() => Number(route.params.id))
const categoryId = computed(() => route.query.categoryId as string || '')
const categoryName = computed(() => route.query.categoryName as string || '')

const question = ref<Question | null>(null)
const questionIds = ref<number[]>([])
const currentIndex = ref(-1)
const slideDirection = ref<'slide-left' | 'slide-right'>('slide-left')

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < questionIds.value.length - 1)

function parseOptions(o: string): Option[] {
  if (!o) return []
  try { return JSON.parse(o) } catch { return [] }
}

function typeLabel(t: number) {
  return ['', '单选题', '多选题', '判断题', '填空题'][t] || ''
}

function difficultyLabel(d: number) {
  return d === 3 ? '困难' : d === 2 ? '中等' : '简单'
}

async function fetchDetail(id: number) {
  try {
    const res = await get<Question>(`/api/questions/${id}`)
    question.value = res.data
  } catch { question.value = null }
}

async function fetchQuestionList() {
  try {
    const params: Record<string, unknown> = { size: 200 }
    if (categoryId.value) params.categoryId = categoryId.value
    const res = await get<{ records: Question[] }>('/api/questions', params)
    const data = (res.data as any)
    const records: Question[] = data.records || data || []
    questionIds.value = records.map(q => q.id)
    currentIndex.value = questionIds.value.indexOf(questionId.value)
  } catch { /* ignore */ }
}

function goPrev() {
  if (!hasPrev.value) return
  slideDirection.value = 'slide-right'
  const prevId = questionIds.value[currentIndex.value - 1]
  router.replace({ path: `/questions/${prevId}`, query: route.query })
}

function goNext() {
  if (!hasNext.value) return
  slideDirection.value = 'slide-left'
  const nextId = questionIds.value[currentIndex.value + 1]
  router.replace({ path: `/questions/${nextId}`, query: route.query })
}

watch(questionId, async (id) => {
  if (!id) return
  await fetchDetail(id)
  currentIndex.value = questionIds.value.indexOf(id)
}, { immediate: true })

onMounted(async () => {
  await fetchQuestionList()
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
  perspective: 800px;
}
.detail-panel {
  padding-bottom: 20px;
  backface-visibility: hidden;
}

/* Question card */
.question-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: 28px 24px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 12px;
}
.q-type {
  font-size: 11px; font-weight: 600; color: var(--accent);
  text-transform: uppercase; letter-spacing: 1px;
  margin-bottom: 12px; display: block;
}
.q-text {
  font-size: 17px; font-weight: 500; line-height: 1.6;
  margin-bottom: 24px;
}

/* Options */
.options { display: flex; flex-direction: column; gap: 10px; }
.option-btn {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-paper);
  font-size: 15px;
}
.option-btn.correct {
  border-color: var(--color-success);
  background: rgba(34,197,94,0.04);
}
.option-letter {
  width: 32px; height: 32px; border-radius: 8px;
  background: var(--bg-surface);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: var(--text-muted);
  flex-shrink: 0;
}
.option-btn.correct .option-letter {
  background: var(--color-success); color: #fff;
}
.option-text { flex: 1; }
.option-check { flex-shrink: 0; }

/* Fill answer */
.fill-answer {
  background: var(--bg-surface); border-radius: var(--radius-md);
  padding: 14px 16px; display: flex; gap: 8px;
}
.fill-label { font-size: 14px; color: var(--text-light); }
.fill-text { font-size: 14px; font-weight: 500; }

/* Analysis */
.analysis-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px 16px;
  margin-bottom: 12px;
}
.analysis-title {
  font-size: 14px; font-weight: 600; color: var(--text-main);
  display: block; margin-bottom: 8px;
}
.analysis-text { font-size: 14px; color: var(--text-muted); line-height: 1.7; }

/* Tags */
.detail-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.detail-tag {
  font-size: 12px; color: var(--text-muted);
  background: var(--bg-surface);
  padding: 6px 14px; border-radius: var(--radius-full);
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

/* === Spring push transition (ref: stiffness=300 damping=30) === */
.slide-left-leave-active,
.slide-right-leave-active {
  position: absolute; top: 0; left: 0; width: 100%;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.6, 1);
}

.slide-left-enter-active,
.slide-right-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* Next: enters from right with 3D page-turn */
.slide-left-enter-from {
  opacity: 0;
  transform: translateX(80px) rotateY(-8deg);
}
.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-60px) rotateY(6deg);
}

/* Prev: enters from left with 3D page-turn */
.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-80px) rotateY(8deg);
}
.slide-right-leave-to {
  opacity: 0;
  transform: translateX(60px) rotateY(-6deg);
}
</style>
