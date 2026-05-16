<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">刷题中</span>
        <span class="page-head__counter">{{ current + 1 }}/{{ total }}</span>
      </div>

      <!-- Progress -->
      <div class="progress-bar">
        <div class="progress-bar__fill" :style="{ width: `${((current + 1) / total) * 100}%` }" />
      </div>

      <!-- Question -->
      <div class="question-card" :key="current" v-if="question">
        <span class="question-card__type">{{ typeLabel(question.type) }}</span>
        <h3 class="question-card__title">{{ question.title }}</h3>

        <div v-if="question.type <= 2" class="options">
          <button
            class="option"
            v-for="(opt, i) in parseOptions(question.options)"
            :key="i"
            :class="{ 'option--selected': selected === i }"
            :disabled="answered"
            @click="selected = i"
          >
            <span class="option__letter">{{ letters[i] }}</span>
            <span class="option__text">{{ opt.content || opt }}</span>
          </button>
        </div>

        <div v-if="question.type === 3" class="options">
          <button class="option" :class="{ 'option--selected': selected === 0 }" :disabled="answered" @click="selected = 0">
            <span class="option__letter">&#10003;</span><span class="option__text">正确</span>
          </button>
          <button class="option" :class="{ 'option--selected': selected === 1 }" :disabled="answered" @click="selected = 1">
            <span class="option__letter">&#10007;</span><span class="option__text">错误</span>
          </button>
        </div>

        <input v-if="question.type === 4" class="fill-input" v-model="fillAnswer" :disabled="answered" placeholder="输入答案..." />

        <!-- Result -->
        <div v-if="answered" class="result" :class="lastCorrect ? 'result--correct' : 'result--wrong'">
          <div class="result__header">
            <span class="result__verdict">{{ lastCorrect ? '回答正确' : '回答错误' }}</span>
          </div>
          <p class="result__answer">正确答案：{{ question.answer }}</p>
          <p class="result__analysis" v-if="question.analysis">{{ question.analysis }}</p>
        </div>
      </div>

      <SkeletonBar v-else :height="300" :radius="20" />

      <!-- Actions -->
      <div class="actions" v-if="!answered">
        <button class="action-btn action-btn--primary" :disabled="!hasAnswer" @click="submitAnswer">提交答案</button>
      </div>
      <div class="actions" v-else>
        <button class="action-btn action-btn--secondary" v-if="current > 0" @click="prevQuestion">上一题</button>
        <button class="action-btn action-btn--primary" @click="nextQuestion">
          {{ current < total - 1 ? '下一题' : '查看结果' }}
        </button>
      </div>
    </div>

    <!-- Finish screen -->
    <div class="finish-overlay" v-if="finished">
      <div class="finish-card">
        <div class="finish-circle" :class="scorePercent >= 70 ? 'good' : 'retry'">
          <span class="finish-score">{{ correctCount }}/{{ total }}</span>
        </div>
        <span class="finish-msg">{{ scorePercent >= 80 ? '太棒了！' : scorePercent >= 60 ? '继续加油！' : '多多练习！' }}</span>
        <button class="finish-btn finish-btn--primary" @click="retry">再刷一次</button>
        <button class="finish-btn finish-btn--secondary" @click="$router.push('/practice')">返回</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get, post } from '@/utils/request'
import SkeletonBar from '@/components/SkeletonBar.vue'

interface Question {
  id: number; type: number; title: string; options: string; answer: string; analysis: string;
}
interface AnswerResult { isCorrect: boolean; correctAnswer: string; analysis: string }

const route = useRoute()
const letters = ['A', 'B', 'C', 'D']

const questions = ref<Question[]>([])
const current = ref(0)
const selected = ref<number | null>(null)
const fillAnswer = ref('')
const answered = ref(false)
const lastCorrect = ref(false)
const correctCount = ref(0)
const finished = ref(false)

const total = computed(() => questions.value.length)
const question = computed(() => questions.value[current.value] || null)
const hasAnswer = computed(() => {
  if (!question.value) return false
  if (question.value.type <= 3) return selected.value !== null
  return fillAnswer.value.trim() !== ''
})
const scorePercent = computed(() => total.value ? Math.round(correctCount.value / total.value * 100) : 0)

function parseOptions(o: string) { if (!o) return []; try { return JSON.parse(o) } catch { return [] } }
function typeLabel(t: number) { return ['', '单选题', '多选题', '判断题', '填空题'][t] || '' }

function getAnswerString(): string {
  if (!question.value) return ''
  if (question.value.type <= 2) {
    if (selected.value === null) return ''
    return letters[selected.value]
  }
  if (question.value.type === 3) return selected.value === 0 ? '正确' : '错误'
  return fillAnswer.value.trim()
}

async function submitAnswer() {
  if (!question.value || !hasAnswer.value) return
  const answerStr = getAnswerString()
  try {
    const res = await post<AnswerResult>('/api/answers', {
      questionId: question.value.id,
      userAnswer: answerStr
    })
    lastCorrect.value = res.data.isCorrect
    if (res.data.isCorrect) correctCount.value++
    answered.value = true
  } catch { /* ignore */ }
}

function prevQuestion() {
  if (current.value > 0) {
    current.value--
    selected.value = null
    fillAnswer.value = ''
    answered.value = false
  }
}

function nextQuestion() {
  if (current.value < total.value - 1) {
    current.value++
    selected.value = null
    fillAnswer.value = ''
    answered.value = false
  } else {
    finished.value = true
  }
}

function retry() {
  finished.value = false
  answered.value = false
  correctCount.value = 0
  current.value = 0
  selected.value = null
  fillAnswer.value = ''
  loadQuestions()
}

async function loadQuestions() {
  const mode = route.query.mode as string || 'random'
  const count = Number(route.query.count) || 10
  const categoryId = route.query.categoryId as string || ''

  try {
    if (mode === 'topic' && categoryId) {
      const res = await get<{ records: Question[] }>('/api/questions', { categoryId, size: count })
      const data = (res.data as any)
      questions.value = data.records || data || []
    } else {
      const res = await get<Question[]>('/api/questions/random', { size: count } as Record<string, unknown>)
      questions.value = res.data || []
    }
  } catch { /* ignore */ }
}

onMounted(() => { loadQuestions() })
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 700px; margin: 0 auto; padding: 0 20px; position: relative; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main);
  background: none; border: none; cursor: pointer;
}
.page-head__title { font-size: 16px; font-weight: 600; }
.page-head__counter { font-size: 14px; color: var(--text-light); }

.progress-bar {
  height: 3px; background: var(--border-light); border-radius: 2px;
  margin-bottom: 32px; overflow: hidden;
}
.progress-bar__fill {
  height: 100%; background: var(--accent);
  border-radius: 2px; transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.question-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: 28px 24px;
  box-shadow: var(--shadow-sm);
}
.question-card__type {
  font-size: 11px; font-weight: 600; color: var(--accent);
  text-transform: uppercase; letter-spacing: 1px;
  margin-bottom: 12px; display: block;
}
.question-card__title {
  font-size: 17px; font-weight: 500; line-height: 1.6;
  margin-bottom: 28px;
}

.options { display: flex; flex-direction: column; gap: 10px; }
.option {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-paper);
  font-size: 15px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s;
}
.option:hover:not(:disabled) { border-color: var(--text-main); background: var(--bg-surface); }
.option:disabled { cursor: default; }
.option--selected {
  border-color: var(--accent);
  background: rgba(217,117,10,0.05);
}
.option__letter {
  width: 32px; height: 32px;
  background: var(--bg-surface);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: var(--text-muted);
  flex-shrink: 0;
}
.option--selected .option__letter {
  background: var(--accent); color: #fff;
}
.option__text { flex: 1; }

.fill-input {
  width: 100%; border: 1px solid var(--border-medium); border-radius: var(--radius-md);
  padding: 14px 16px; font-size: 15px; background: var(--bg-surface);
  box-sizing: border-box;
}

.result {
  margin-top: 24px; padding: 20px; border-radius: var(--radius-md);
}
.result--correct { background: rgba(34,197,94,0.06); border: 1px solid rgba(34,197,94,0.2); }
.result--wrong { background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.2); }
.result__verdict { font-size: 16px; font-weight: 600; }
.result--correct .result__verdict { color: var(--color-success); }
.result--wrong .result__verdict { color: var(--color-danger); }
.result__answer { font-size: 14px; color: var(--text-muted); margin-top: 4px; }
.result__analysis { font-size: 14px; color: var(--text-light); line-height: 1.7; margin-top: 4px; }

.actions { display: flex; gap: 12px; margin-top: 28px; }
.action-btn {
  flex: 1; padding: 14px; border-radius: var(--radius-lg);
  font-size: 15px; font-weight: 500; cursor: pointer;
}
.action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.action-btn--secondary {
  background: var(--bg-paper); border: 1px solid var(--border-medium);
  color: var(--text-main);
}
.action-btn--primary {
  background: var(--bg-dark); color: #fff; border: none;
  box-shadow: var(--shadow-md);
}

/* Finish overlay */
.finish-overlay {
  position: fixed; inset: 0; background: var(--bg-canvas);
  display: flex; align-items: center; justify-content: center;
  z-index: 100;
}
.finish-card {
  text-align: center; padding: 48px 40px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  width: calc(100% - 80px); max-width: 400px;
}
.finish-circle {
  width: 120px; height: 120px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 24px;
}
.finish-circle.good { background: rgba(34,197,94,0.08); border: 2px solid var(--color-success); }
.finish-circle.retry { background: rgba(217,117,10,0.08); border: 2px solid var(--accent); }
.finish-score { font-family: var(--font-serif); font-size: 32px; font-weight: 700; }
.finish-msg { font-size: 18px; color: var(--text-muted); display: block; margin-bottom: 32px; }
.finish-btn {
  width: 100%; height: 48px; border-radius: var(--radius-lg);
  font-size: 16px; font-weight: 500; cursor: pointer; border: none;
  display: flex; align-items: center; justify-content: center;
}
.finish-btn--primary { background: var(--bg-dark); color: #fff; margin-bottom: 12px; }
.finish-btn--secondary { background: var(--bg-surface); color: var(--text-muted); }
</style>
