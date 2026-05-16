# 题库与刷题功能重构 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将"自由刷题"重构为"查看题库"（分类浏览+详情查阅+上/下题切换），将"在线试卷"重构为"自由刷题"（随机组卷+按专题刷题的入口页面）

**Architecture:** 路由级重构：`/practice` → `/questions` + `/questions/:id`，`/exam` → `/practice` + `/practice/do`。后端API无需改动。严格遵循 Warm Tech 设计令牌（#F3EFE8 canvas, #FDFCFB paper, #D9750A accent, Georgia/Inter 字体）。

**Tech Stack:** Vue 3 + TypeScript + Vue Router + Pinia, uni-app (小程序), Spring Boot 3.2 (后端无需改动)

---

### Task 1: Web端 — 路由重构

**Files:**
- Modify: `AI-Interview/web-app/src/router/index.ts`

- [ ] **Step 1: Update route definitions**

Replace old `/practice` and `/exam` routes with new routes:

```typescript
// Remove old routes:
//   { path: '/exam', name: 'exam', ... }
//   { path: '/exam/do', name: 'exam-do', ... }
//   { path: '/practice', name: 'practice', ... }

// Add new routes:
{
  path: '/questions',
  name: 'questions',
  component: () => import('@/views/QuestionsView.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/questions/:id',
  name: 'question-detail',
  component: () => import('@/views/QuestionDetailView.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/practice',
  name: 'practice',
  component: () => import('@/views/PracticeView.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/practice/do',
  name: 'practice-do',
  component: () => import('@/views/PracticeDoView.vue'),
  meta: { requiresAuth: true }
},
```

- [ ] **Step 2: Verify imports**

Ensure no stale imports remain. The file should import only used component paths.

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/router/index.ts
git commit -m "feat: restructure routes — /questions for 查看题库, /practice for 自由刷题"
```

---

### Task 2: Web端 — 创建 QuestionsView (查看题库列表)

**Files:**
- Delete: `AI-Interview/web-app/src/views/PracticeView.vue`
- Create: `AI-Interview/web-app/src/views/QuestionsView.vue`

- [ ] **Step 1: Create QuestionsView.vue**

Rename from PracticeView.vue with the following changes:
- Title: "自由刷题" → "查看题库"
- Add `@click` handler on `.q-card` to navigate to `/questions/:id?categoryId=X&categoryName=X`
- Pass current category context via query params

```vue
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

      <!-- Category chips -->
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

      <!-- Question list -->
      <ScrollReveal :stagger="0.08" :y="20" :duration="0.6" v-if="questions.length > 0">
      <div class="q-list">
        <div class="q-card card-hover" v-for="(q, i) in questions" :key="q.id"
          @click="goDetail(q, i)">
          <span class="q-number">Q{{ i + 1 + (page - 1) * pageSize }}</span>
          <div class="q-content">
            <span class="q-title">{{ q.title }}</span>
            <span class="q-meta">{{ q.categoryName || '未分类' }} · {{ difficultyLabel(q.difficulty) }}</span>
          </div>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </div>
      </div>
      </ScrollReveal>

      <!-- Load more -->
      <div class="load-more" v-if="hasMore">
        <button class="load-more-btn" @click="loadMore" :disabled="loading">加载更多</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { get } from '@/utils/request'
import TopicChip from '@/components/TopicChip.vue'
import ScrollReveal from '@/components/ScrollReveal.vue'

interface Category { id: number; name: string }
interface Question { id: number; title: string; categoryName: string; difficulty: number }

const router = useRouter()
const route = useRoute()

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

function goDetail(q: Question, _index: number) {
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

.load-more { text-align: center; padding: 24px 0; }
.load-more-btn {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: var(--radius-full); padding: 10px 32px; font-size: 14px;
  color: var(--text-muted); cursor: pointer;
}
</style>
```

- [ ] **Step 2: Delete old PracticeView.vue**

```bash
rm AI-Interview/web-app/src/views/PracticeView.vue
```

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/views/QuestionsView.vue AI-Interview/web-app/src/views/PracticeView.vue
git commit -m "feat: rename PracticeView→QuestionsView with detail navigation and pagination"
```

---

### Task 3: Web端 — 创建 QuestionDetailView (题目详情)

**Files:**
- Create: `AI-Interview/web-app/src/views/QuestionDetailView.vue`

- [ ] **Step 1: Create QuestionDetailView.vue**

Detail view showing full question content with prev/next navigation. Gets question list from API filtered by category context.

```vue
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

      <div class="detail-body" v-if="question">
        <!-- Question card -->
        <div class="question-card" :key="questionId">
          <span class="q-type">{{ typeLabel(question.type) }}</span>
          <h3 class="q-text">{{ question.title }}</h3>

          <!-- Options for single/multi choice -->
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

          <!-- True/False -->
          <div v-if="question.type === 3" class="options">
            <div class="option-btn" :class="{ correct: question.answer === '正确' }">
              <span class="option-letter">✓</span>
              <span class="option-text">正确</span>
            </div>
            <div class="option-btn" :class="{ correct: question.answer === '错误' }">
              <span class="option-letter">✗</span>
              <span class="option-text">错误</span>
            </div>
          </div>

          <!-- Fill-in-blank -->
          <div v-if="question.type === 4" class="fill-answer">
            <span class="fill-label">参考答案：</span>
            <span class="fill-text">{{ question.answer }}</span>
          </div>
        </div>

        <!-- Analysis card -->
        <div class="analysis-card" v-if="question.analysis">
          <span class="analysis-title">答案解析</span>
          <p class="analysis-text">{{ question.analysis }}</p>
        </div>

        <!-- Tags -->
        <div class="detail-tags">
          <span class="detail-tag">{{ question.categoryName || '未分类' }}</span>
          <span class="detail-tag">{{ difficultyLabel(question.difficulty) }}</span>
        </div>
      </div>

      <!-- Skeleton -->
      <div class="detail-body" v-else>
        <SkeletonBar :height="200" :radius="16" />
      </div>
    </div>

    <!-- Prev / Next -->
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

async function goPrev() {
  if (!hasPrev.value) return
  const prevId = questionIds.value[currentIndex.value - 1]
  router.replace({
    path: `/questions/${prevId}`,
    query: route.query
  })
}

async function goNext() {
  if (!hasNext.value) return
  const nextId = questionIds.value[currentIndex.value + 1]
  router.replace({
    path: `/questions/${nextId}`,
    query: route.query
  })
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
}
.page-head__title { font-family: var(--font-serif); font-size: 18px; font-weight: 600; }
.page-head__cat { font-size: 12px; color: var(--text-light); }

.detail-body { padding-bottom: 20px; }

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
</style>
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/web-app/src/views/QuestionDetailView.vue
git commit -m "feat: add QuestionDetailView with full question display and prev/next navigation"
```

---

### Task 4: Web端 — 创建新 PracticeView (自由刷题入口)

**Files:**
- Delete: `AI-Interview/web-app/src/views/ExamView.vue`
- Create: `AI-Interview/web-app/src/views/PracticeView.vue`

- [ ] **Step 1: Create new PracticeView.vue**

Complete redesign as dual-entry page (随机组卷 + 按专题刷题):

```vue
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

      <div class="hero-banner">
        <span class="hero-banner__label">自由练</span>
        <h1 class="hero-banner__title">自由刷题</h1>
        <p class="hero-banner__desc">随机组卷或按专题专项突破</p>
      </div>

      <!-- Entry 1: Random -->
      <div class="entry-card">
        <div class="entry-card__icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5">
            <rect x="2" y="2" width="20" height="20" rx="4"/>
            <circle cx="8.5" cy="8.5" r="1.5" fill="#D9750A"/>
            <circle cx="15.5" cy="8.5" r="1.5" fill="#D9750A"/>
            <circle cx="8.5" cy="15.5" r="1.5" fill="#D9750A"/>
            <circle cx="15.5" cy="15.5" r="1.5" fill="#D9750A"/>
          </svg>
        </div>
        <span class="entry-card__title">随机组卷</span>
        <span class="entry-card__desc">从题库随机抽取，模拟真实考试</span>
        <div class="count-row">
          <button
            v-for="n in [5, 10, 15, 20]"
            :key="n"
            class="count-chip"
            :class="{ active: randomCount === n }"
            @click="randomCount = n"
          >{{ n }}</button>
          <button class="count-go" @click="startRandom">开始</button>
        </div>
      </div>

      <!-- Entry 2: Topic -->
      <div class="entry-card">
        <div class="entry-card__icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5">
            <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <span class="entry-card__title">按专题刷题</span>
        <span class="entry-card__desc">选择分类，集中突破薄弱环节</span>
        <div class="chip-row no-scrollbar">
          <TopicChip
            v-for="cat in categories"
            :key="cat.id"
            :active="selectedCategory === cat.id"
            @click="selectedCategory = cat.id; startTopic(cat)"
          >
            {{ cat.name }}
          </TopicChip>
        </div>
      </div>

      <!-- Recent history -->
      <div class="section-spacer">
        <span class="section-label">最近记录</span>
      </div>
      <div class="hist-list" v-if="history.length > 0">
        <div class="hist-item" v-for="h in history" :key="h.id">
          <div class="hist-info">
            <span class="hist-name">{{ h.modeLabel }} · {{ h.questionCount }}题</span>
            <span class="hist-time">正确 {{ h.correctCount }} 题</span>
          </div>
          <span class="hist-badge" :class="h.correctCount / h.questionCount >= 0.7 ? 'badge-high' : 'badge-mid'">
            {{ Math.round(h.correctCount / h.questionCount * 100) }}%
          </span>
        </div>
      </div>
      <div class="empty" v-else>
        <span class="empty__title">暂无刷题记录</span>
        <p class="empty__desc">选择上方模式开始刷题</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '@/utils/request'
import TopicChip from '@/components/TopicChip.vue'

interface Category { id: number; name: string }
interface HistoryItem {
  id: number; modeLabel: string; questionCount: number; correctCount: number;
}

const router = useRouter()
const randomCount = ref(10)
const selectedCategory = ref<number | null>(null)
const categories = ref<Category[]>([])
const history = ref<HistoryItem[]>([])

function startRandom() {
  router.push(`/practice/do?mode=random&count=${randomCount.value}`)
}

function startTopic(cat: Category) {
  router.push(`/practice/do?mode=topic&categoryId=${cat.id}&categoryName=${encodeURIComponent(cat.name)}`)
}

onMounted(async () => {
  try {
    const res = await get<Category[]>('/api/questions/categories')
    categories.value = res.data || []
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
  font-family: var(--font-serif); font-size: 18px; font-weight: 600;
}

.hero-banner { margin-bottom: 24px; }
.hero-banner__label {
  font-size: 12px; font-weight: 600; color: var(--accent);
  letter-spacing: 3px; text-transform: uppercase; display: block; margin-bottom: 8px;
}
.hero-banner__title {
  font-family: var(--font-serif); font-size: 32px; font-weight: 600;
  letter-spacing: -1px; margin-bottom: 8px;
}
.hero-banner__desc { font-size: 14px; color: var(--text-light); }

/* Entry cards */
.entry-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: 24px 20px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
}
.entry-card__icon {
  width: 44px; height: 44px; border-radius: 12px;
  background: rgba(217,117,10,0.06);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 14px;
}
.entry-card__title { font-size: 17px; font-weight: 500; display: block; margin-bottom: 4px; }
.entry-card__desc { font-size: 13px; color: var(--text-light); display: block; margin-bottom: 16px; }

/* Count selector */
.count-row { display: flex; gap: 8px; align-items: center; }
.count-chip {
  width: 40px; height: 40px; border-radius: 10px;
  border: 1px solid var(--border-light);
  background: var(--bg-paper);
  font-size: 14px; font-weight: 500; color: var(--text-main);
  cursor: pointer;
}
.count-chip.active {
  border-color: var(--accent);
  background: rgba(217,117,10,0.06);
  color: var(--accent); font-weight: 600;
}
.count-go {
  margin-left: auto; height: 40px; padding: 0 24px;
  background: var(--bg-dark); color: #fff;
  border: none; border-radius: 10px;
  font-size: 14px; font-weight: 500; cursor: pointer;
}

/* Chips */
.chip-row { display: flex; gap: 8px; flex-wrap: wrap; }

/* Section */
.section-spacer { margin: 28px 0 12px; }
.section-label {
  font-size: 11px; font-weight: 600; color: var(--text-light);
  text-transform: uppercase; letter-spacing: 1px;
}

/* History */
.hist-list { display: flex; flex-direction: column; gap: 8px; }
.hist-item {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 14px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
}
.hist-info { flex: 1; min-width: 0; }
.hist-name { font-size: 14px; font-weight: 500; display: block; }
.hist-time { font-size: 12px; color: var(--text-light); }
.hist-badge {
  font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: var(--radius-full);
}
.badge-high { background: #F0FDF4; color: #16A34A; }
.badge-mid { background: #FFFBEB; color: #D97706; }

.empty { text-align: center; padding-top: 40px; }
.empty__title { font-size: 15px; font-weight: 500; display: block; margin-bottom: 6px; }
.empty__desc { font-size: 13px; color: var(--text-light); }
</style>
```

- [ ] **Step 2: Delete old ExamView.vue**

```bash
rm AI-Interview/web-app/src/views/ExamView.vue
```

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/views/PracticeView.vue AI-Interview/web-app/src/views/ExamView.vue
git commit -m "feat: redesign PracticeView as 自由刷题 dual-entry page (random + topic)"
```

---

### Task 5: Web端 — 创建 PracticeDoView (刷题中)

**Files:**
- Rename: `AI-Interview/web-app/src/views/ExamDoView.vue` → `AI-Interview/web-app/src/views/PracticeDoView.vue`

- [ ] **Step 1: Create PracticeDoView.vue from ExamDoView.vue**

Copy ExamDoView.vue content, update title from "答题" to "刷题中", add query param handling for mode/random/topic:

```vue
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
            <span class="option__letter">✓</span><span class="option__text">正确</span>
          </button>
          <button class="option" :class="{ 'option--selected': selected === 1 }" :disabled="answered" @click="selected = 1">
            <span class="option__letter">✗</span><span class="option__text">错误</span>
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
}
.page-head__title { font-size: 16px; font-weight: 600; }
.page-head__counter { font-size: 14px; color: var(--text-light); }

.progress-bar {
  height: 3px; background: var(--border-light); border-radius: 2px;
  margin-bottom: 32px; overflow: hidden;
}
.progress-bar__fill {
  height: 100%; background: var(--accent);
  border-radius: 2px; transition: width 0.3s var(--ease-out-expo);
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
```

- [ ] **Step 2: Remove old ExamDoView.vue**

```bash
rm AI-Interview/web-app/src/views/ExamDoView.vue
```

- [ ] **Step 3: Commit**

```bash
git add AI-Interview/web-app/src/views/PracticeDoView.vue AI-Interview/web-app/src/views/ExamDoView.vue
git commit -m "feat: refactor PracticeDoView with query-driven question loading and instant scoring"
```

---

### Task 6: Web端 — HomeView 入口卡片更新

**Files:**
- Modify: `AI-Interview/web-app/src/views/HomeView.vue`

- [ ] **Step 1: Update card nav targets and text**

Three changes in HomeView.vue:

**Change 1 — Card 3 (was `/exam`, now `/practice`):**
```vue
<!-- OLD -->
<PixelCard :gap="10" :dotRadius="1.0" colors="#e0f2fe,#7dd3fc,#0ea5e9" :opacityMin="0.15" :opacityMax="0.5"
  className="func-card" @click="$router.push('/exam')">
  ...
  <span class="func-card__title">在线试卷</span>
  <span class="func-card__desc">限时模拟考试，查漏补缺</span>
</PixelCard>

<!-- NEW -->
<PixelCard :gap="10" :dotRadius="1.0" colors="#e0f2fe,#7dd3fc,#0ea5e9" :opacityMin="0.15" :opacityMax="0.5"
  className="func-card" @click="$router.push('/practice')">
  ...
  <span class="func-card__title">自由刷题</span>
  <span class="func-card__desc">随机组卷 · 按专题练习</span>
</PixelCard>
```

**Change 2 — Card 4 (was `/practice`, now `/questions`):**
```vue
<!-- OLD -->
<PixelCard :gap="10" :dotRadius="1.0" colors="#f8fafc,#f1f5f9,#cbd5e1" :opacityMin="0.12" :opacityMax="0.4"
  className="func-card" @click="$router.push('/practice')">
  ...
  <span class="func-card__title">自由刷题</span>
  <span class="func-card__desc">按分类随机练习</span>
</PixelCard>

<!-- NEW -->
<PixelCard :gap="10" :dotRadius="1.0" colors="#f8fafc,#f1f5f9,#cbd5e1" :opacityMin="0.12" :opacityMax="0.4"
  className="func-card" @click="$router.push('/questions')">
  ...
  <span class="func-card__title">查看题库</span>
  <span class="func-card__desc">分类浏览 · 逐题精学</span>
</PixelCard>
```

**Change 3 — Hot Topics / Category links (was `/practice`, now `/questions`):**
```typescript
// OLD
function goTopic(t: string) {
  router.push(`/practice?tag=${encodeURIComponent(t)}`)
}
function goCategory(c: Category) {
  router.push(`/practice?categoryId=${c.id}&categoryName=${encodeURIComponent(c.name)}`)
}

// NEW
function goTopic(t: string) {
  router.push(`/questions?tag=${encodeURIComponent(t)}`)
}
function goCategory(c: Category) {
  router.push(`/questions?categoryId=${c.id}&categoryName=${encodeURIComponent(c.name)}`)
}
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/web-app/src/views/HomeView.vue
git commit -m "feat: update HomeView cards — 自由刷题↔查看题库 route and text swap"
```

---

### Task 7: Web端 — ProfileView 菜单更新

**Files:**
- Modify: `AI-Interview/web-app/src/views/ProfileView.vue`

- [ ] **Step 1: Update menu item**

```vue
<!-- OLD -->
<button class="menu-item" @click="$router.push('/exam')">
  <span class="menu-item__label">在线试卷</span>
</button>

<!-- NEW -->
<button class="menu-item" @click="$router.push('/practice')">
  <span class="menu-item__label">自由刷题</span>
</button>
```

- [ ] **Step 2: Commit**

```bash
git add AI-Interview/web-app/src/views/ProfileView.vue
git commit -m "feat: update ProfileView menu — 在线试卷→自由刷题"
```

---

### Task 8: 小程序端 — pages.json 路由与 TabBar 更新

**Files:**
- Modify: `AI-Interview/pages.json`

- [ ] **Step 1: Update practice page path**

```json
// OLD
{
    "path": "pages/practice/practice",
    "style": {
        "navigationBarTitleText": "自由刷题"
    }
}

// NEW — rename to question-bank
{
    "path": "pages/question-bank/index",
    "style": {
        "navigationBarTitleText": "查看题库"
    }
}
```

- [ ] **Step 2: Add question-bank detail page**

```json
{
    "path": "pages/question-bank/detail",
    "style": {
        "navigationBarTitleText": "题目详情"
    }
}
```

- [ ] **Step 3: Update exam page titles**

```json
// OLD
{
    "root": "pages/exam",
    "pages": [
        {
            "path": "index",
            "style": { "navigationBarTitleText": "在线试卷" }
        },
        {
            "path": "do",
            "style": { "navigationBarTitleText": "考试中" }
        }
    ]
}

// NEW
{
    "root": "pages/practice-entry",
    "pages": [
        {
            "path": "index",
            "style": { "navigationBarTitleText": "自由刷题" }
        },
        {
            "path": "do",
            "style": { "navigationBarTitleText": "刷题中" }
        }
    ]
}
```

- [ ] **Step 4: Update tabBar**

```json
// OLD
{
    "pagePath": "pages/practice/practice",
    "text": "刷题"
}

// NEW
{
    "pagePath": "pages/question-bank/index",
    "text": "题库"
}
```

- [ ] **Step 5: Commit**

```bash
git add AI-Interview/pages.json
git commit -m "feat: update mini-program routing — 题库 + 自由刷题 rename"
```

---

### Task 9: 小程序端 — 创建 查看题库 页面

**Files:**
- Create: `AI-Interview/pages/question-bank/index.vue` (based on `pages/practice/practice.vue`)
- Create: `AI-Interview/pages/question-bank/detail.vue`
- Delete: `AI-Interview/pages/practice/practice.vue`

- [ ] **Step 1: Create question-bank/index.vue**

Adapt from current `pages/practice/practice.vue`, changing:
- Title: "自由刷题" → "查看题库"
- Start screen → remove, show category tabs + question list directly
- Question list cards: tap → navigate to detail page

```vue
<template>
  <view class="question-bank">
    <view class="page-header">
      <text class="page-title">查看题库</text>
    </view>

    <!-- Category chips -->
    <scroll-view scroll-x class="chip-scroll">
      <view class="chip-row">
        <view class="chip" :class="{ active: !activeCategory }" @click="selectCategory('')">全部</view>
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="chip"
          :class="{ active: activeCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >{{ cat.name }}</view>
      </view>
    </scroll-view>

    <!-- Question list -->
    <view class="q-list" v-if="questions.length > 0">
      <view class="q-card" v-for="(q, i) in questions" :key="q.id" @click="goDetail(q)">
        <text class="q-number">Q{{ i + 1 }}</text>
        <view class="q-content">
          <text class="q-title">{{ q.title }}</text>
          <text class="q-meta">{{ q.categoryName || '未分类' }} · {{ difficultyLabel(q.difficulty) }}</text>
        </view>
        <text class="q-arrow">›</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-title">暂无题目</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';

interface Category { id: number; name: string }
interface Question { id: number; title: string; categoryName: string; difficulty: number }

const activeCategory = ref<string | number>('');
const categories = ref<Category[]>([]);
const questions = ref<Question[]>([]);

function difficultyLabel(d: number) {
  return d === 3 ? '困难' : d === 2 ? '中等' : '简单';
}

function selectCategory(id: string | number) {
  activeCategory.value = id;
  fetchQuestions();
}

async function fetchQuestions() {
  try {
    const params: Record<string, unknown> = { size: 50 };
    if (activeCategory.value) params.categoryId = activeCategory.value;
    const res = await get<{ records: Question[] }>('/api/questions', params);
    const data = (res.data as any);
    questions.value = data.records || data || [];
  } catch {}
}

function goDetail(q: Question) {
  uni.navigateTo({ url: `/pages/question-bank/detail?id=${q.id}&categoryId=${activeCategory.value || ''}&categoryName=${encodeURIComponent(q.categoryName || '')}` });
}

onMounted(async () => {
  try {
    const r = await get<Category[]>('/api/questions/categories');
    categories.value = r.data || [];
  } catch {}
  fetchQuestions();
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.question-bank { min-height: 100vh; background: $bg-canvas; padding: 0 28rpx 40rpx; }

.page-header { padding-top: 40rpx; margin-bottom: 24rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; letter-spacing: -0.5px;
}

.chip-scroll { white-space: nowrap; margin-bottom: 28rpx; }
.chip-row { display: inline-flex; gap: 14rpx; }
.chip {
  display: inline-block; padding: 12rpx 28rpx; border-radius: $radius-full;
  font-size: 26rpx; color: $text-muted; background: $bg-surface;
  border: 1px solid $border-light;
}
.chip.active { background: rgba(217,117,10,0.06); border-color: $accent; color: $accent; font-weight: 600; }

.q-list { display: flex; flex-direction: column; gap: 14rpx; }
.q-card {
  display: flex; align-items: flex-start; gap: 16rpx;
  padding: 28rpx 24rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg;
}
.q-card:active { background: $bg-surface; }
.q-number { font-size: 26rpx; font-weight: 600; color: $accent; flex-shrink: 0; }
.q-content { flex: 1; min-width: 0; }
.q-title { font-size: 28rpx; color: $text-main; line-height: 1.5; display: block; margin-bottom: 6rpx; }
.q-meta { font-size: 22rpx; color: $text-light; }
.q-arrow { color: #CCC; font-size: 32rpx; flex-shrink: 0; }

.empty-state { text-align: center; padding-top: 200rpx; }
.empty-title { font-size: 28rpx; color: $text-light; }

@media (min-width: 1025px) { .question-bank { max-width: 700px; margin: 0 auto; } }
</style>
```

- [ ] **Step 2: Create question-bank/detail.vue**

Detail page with full question display, prev/next via category context:

```vue
<template>
  <view class="detail-page">
    <view class="detail-body" v-if="question">
      <!-- Question card -->
      <view class="q-card">
        <view class="q-meta">
          <text class="q-type">{{ typeLabel(question.type) }}</text>
        </view>
        <text class="q-title">{{ question.title }}</text>

        <!-- Options -->
        <view v-if="question.type <= 2" class="options">
          <view
            v-for="opt in parseOptions(question.options)"
            :key="opt.label"
            class="opt"
            :class="{ correct: opt.label === question.answer }"
          >
            <view class="opt-letter" :class="{ on: opt.label === question.answer }">{{ opt.label }}</view>
            <text class="opt-text">{{ opt.content }}</text>
            <text v-if="opt.label === question.answer" class="opt-check">✓</text>
          </view>
        </view>

        <view v-if="question.type === 3" class="options">
          <view class="opt" :class="{ correct: question.answer === '正确' }">
            <view class="opt-letter" :class="{ on: question.answer === '正确' }">✓</view>
            <text class="opt-text">正确</text>
          </view>
          <view class="opt" :class="{ correct: question.answer === '错误' }">
            <view class="opt-letter" :class="{ on: question.answer === '错误' }">✗</view>
            <text class="opt-text">错误</text>
          </view>
        </view>

        <view v-if="question.type === 4" class="fill-answer">
          <text class="fill-label">参考答案：</text>
          <text class="fill-text">{{ question.answer }}</text>
        </view>
      </view>

      <!-- Analysis -->
      <view class="analysis-card" v-if="question.analysis">
        <text class="analysis-title">答案解析</text>
        <text class="analysis-text">{{ question.analysis }}</text>
      </view>

      <!-- Tags -->
      <view class="tag-row">
        <text class="tag">{{ question.categoryName }}</text>
        <text class="tag">{{ difficultyLabel(question.difficulty) }}</text>
      </view>
    </view>

    <view v-else class="empty-state"><text>加载中...</text></view>

    <!-- Prev / Next -->
    <view class="bottom-actions">
      <view class="action-btn" :class="{ disabled: !hasPrev }" @click="goPrev">&lt; 上一题</view>
      <view class="action-btn primary" :class="{ disabled: !hasNext }" @click="goNext">下一题 &gt;</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get } from '@/utils/request';

interface Question { id: number; categoryName: string; type: number; title: string; options: string; answer: string; analysis: string; difficulty: number; }
interface Option { label: string; content: string }

const questionId = ref(0);
const categoryId = ref('');
const question = ref<Question | null>(null);
const questionIds = ref<number[]>([]);
const currentIndex = ref(-1);

const hasPrev = currentIndex.value > 0;
const hasNext = currentIndex.value < questionIds.value.length - 1;

onLoad((opts: any) => {
  questionId.value = Number(opts.id || 0);
  categoryId.value = opts.categoryId || '';
});

function parseOptions(o: string): Option[] { if (!o) return []; try { return JSON.parse(o) } catch { return [] } }
function typeLabel(t: number) { return ['', '单选题', '多选题', '判断题', '填空题'][t] || '' }
function difficultyLabel(d: number) { return d === 3 ? '困难' : d === 2 ? '中等' : '简单' }

async function fetchDetail() {
  try {
    const r = await get<Question>(`/api/questions/${questionId.value}`);
    question.value = r.data;
  } catch {}
}

async function fetchQuestionList() {
  try {
    const params: Record<string, unknown> = { size: 200 };
    if (categoryId.value) params.categoryId = categoryId.value;
    const r = await get<{ records: Question[] }>('/api/questions', params);
    const data = (r.data as any);
    const records: Question[] = data.records || data || [];
    questionIds.value = records.map(q => q.id);
    currentIndex.value = questionIds.value.indexOf(questionId.value);
  } catch {}
}

function goPrev() {
  if (currentIndex.value <= 0) return;
  const prevId = questionIds.value[currentIndex.value - 1];
  uni.redirectTo({ url: `/pages/question-bank/detail?id=${prevId}&categoryId=${categoryId.value}` });
}

function goNext() {
  if (currentIndex.value >= questionIds.value.length - 1) return;
  const nextId = questionIds.value[currentIndex.value + 1];
  uni.redirectTo({ url: `/pages/question-bank/detail?id=${nextId}&categoryId=${categoryId.value}` });
}

onMounted(async () => {
  await fetchQuestionList();
  await fetchDetail();
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.detail-page { min-height: 100vh; background: $bg-canvas; display: flex; flex-direction: column; }
.detail-body { flex: 1; padding: 20rpx 28rpx; }

.q-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-xl; padding: 36rpx 28rpx;
  box-shadow: $shadow-sm; margin-bottom: 16rpx;
}
.q-meta { margin-bottom: 20rpx; }
.q-type { font-size: 22rpx; font-weight: 600; color: $accent; text-transform: uppercase; letter-spacing: 1px; }
.q-title { font-size: 32rpx; color: $text-main; line-height: 1.8; margin-bottom: 32rpx; display: block; }

.options { display: flex; flex-direction: column; gap: 14rpx; }
.opt {
  display: flex; align-items: center; gap: 18rpx;
  padding: 24rpx 22rpx; border: 1px solid $border-light;
  border-radius: $radius-md; font-size: 28rpx; color: $text-main;
  background: $bg-paper;
}
.opt.correct { border-color: $color-success; background: rgba(34,197,94,0.04); }
.opt-letter {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1px solid $border-medium; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; font-weight: 600;
  color: $text-light; flex-shrink: 0; background: $bg-surface;
}
.opt-letter.on { background: $color-success; color: #fff; border-color: $color-success; }
.opt-text { flex: 1; line-height: 1.6; }
.opt-check { font-size: 28rpx; font-weight: 700; color: $color-success; flex-shrink: 0; }

.fill-answer { background: $bg-surface; border-radius: $radius-md; padding: 24rpx; display: flex; gap: 12rpx; }
.fill-label { font-size: 26rpx; color: $text-light; }
.fill-text { font-size: 26rpx; font-weight: 500; color: $text-main; }

.analysis-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx 24rpx; margin-bottom: 16rpx;
}
.analysis-title { font-size: 28rpx; font-weight: 600; color: $text-main; display: block; margin-bottom: 12rpx; }
.analysis-text { font-size: 26rpx; color: $text-muted; line-height: 1.8; }

.tag-row { display: flex; gap: 14rpx; flex-wrap: wrap; }
.tag { font-size: 24rpx; color: $text-muted; background: $bg-surface; padding: 10rpx 24rpx; border-radius: $radius-full; }

.bottom-actions { display: flex; gap: 16rpx; padding: 20rpx 28rpx 44rpx; background: $bg-paper; border-top: 1px solid $border-light; }
.action-btn {
  flex: 1; height: 88rpx; background: $bg-surface;
  color: $text-main; font-size: 28rpx; font-weight: 500;
  border-radius: $radius-xl; border: none;
  display: flex; align-items: center; justify-content: center;
}
.action-btn:active { opacity: 0.9; }
.action-btn.disabled { opacity: 0.3; }
.action-btn.primary { background: $bg-dark; color: #fff; }

.empty-state { text-align: center; padding-top: 200rpx; }

@media (min-width: 1025px) { .detail-page { max-width: 800px; margin: 0 auto; } }
</style>
```

- [ ] **Step 3: Delete old practice page**

```bash
rm AI-Interview/pages/practice/practice.vue
```

- [ ] **Step 4: Commit**

```bash
git add AI-Interview/pages/question-bank/ AI-Interview/pages/practice/
git commit -m "feat: create mini-program 查看题库 list + detail pages"
```

---

### Task 10: 小程序端 — 创建 自由刷题 页面

**Files:**
- Create: `AI-Interview/pages/practice-entry/index.vue` (based on `pages/exam/index.vue`)
- Create: `AI-Interview/pages/practice-entry/do.vue` (based on `pages/exam/do.vue`)
- Delete: `AI-Interview/pages/exam/index.vue`
- Delete: `AI-Interview/pages/exam/do.vue`

- [ ] **Step 1: Create practice-entry/index.vue**

Adapt from `pages/exam/index.vue` — dual entry design (随机组卷 + 按专题):

```vue
<template>
  <view class="practice-entry">
    <view class="page-header">
      <text class="page-title">自由刷题</text>
      <text class="page-desc">随机组卷或按专题专项突破</text>
    </view>

    <!-- Entry 1: Random -->
    <view class="entry-card">
      <view class="entry-icon">
        <text class="entry-icon-text">🎲</text>
      </view>
      <text class="entry-title">随机组卷</text>
      <text class="entry-desc">从题库随机抽取，模拟真实考试</text>
      <view class="count-row">
        <view v-for="n in [5,10,15,20]" :key="n" class="count-chip" :class="{ active: randomCount === n }" @click="randomCount = n">
          <text>{{ n }}</text>
        </view>
        <view class="count-go" @click="startRandom"><text>开始</text></view>
      </view>
    </view>

    <!-- Entry 2: Topic -->
    <view class="entry-card">
      <view class="entry-icon">
        <text class="entry-icon-text">📂</text>
      </view>
      <text class="entry-title">按专题刷题</text>
      <text class="entry-desc">选择分类，集中突破薄弱环节</text>
      <scroll-view scroll-x class="chip-scroll">
        <view class="chip-row">
          <view
            v-for="cat in categories"
            :key="cat.id"
            class="chip"
            :class="{ active: selectedCategory === cat.id }"
            @click="selectedCategory = cat.id; startTopic(cat)"
          >{{ cat.name }}</view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';

interface Category { id: number; name: string }

const randomCount = ref(10);
const selectedCategory = ref<number | null>(null);
const categories = ref<Category[]>([]);

function startRandom() {
  uni.navigateTo({ url: `/pages/practice-entry/do?mode=random&count=${randomCount.value}` });
}

function startTopic(cat: Category) {
  uni.navigateTo({ url: `/pages/practice-entry/do?mode=topic&categoryId=${cat.id}&categoryName=${encodeURIComponent(cat.name)}` });
}

onMounted(async () => {
  try {
    const r = await get<Category[]>('/api/questions/categories');
    categories.value = r.data || [];
  } catch {}
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.practice-entry { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

.page-header { margin-bottom: 36rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; display: block; margin-bottom: 10rpx; letter-spacing: -0.5px;
}
.page-desc { font-size: 26rpx; color: $text-muted; }

.entry-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-xl; padding: 36rpx 28rpx;
  box-shadow: $shadow-sm; margin-bottom: 20rpx;
}
.entry-icon {
  width: 80rpx; height: 80rpx; border-radius: 20rpx;
  background: rgba(217,117,10,0.06);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 20rpx;
}
.entry-icon-text { font-size: 36rpx; }
.entry-title { font-size: 32rpx; font-weight: 500; color: $text-main; display: block; margin-bottom: 8rpx; }
.entry-desc { font-size: 24rpx; color: $text-light; display: block; margin-bottom: 24rpx; }

.count-row { display: flex; gap: 12rpx; align-items: center; }
.count-chip {
  width: 72rpx; height: 72rpx; border-radius: 16rpx;
  border: 1px solid $border-light; background: $bg-paper;
  display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 500; color: $text-main;
}
.count-chip.active { border-color: $accent; background: rgba(217,117,10,0.06); color: $accent; font-weight: 600; }
.count-go {
  margin-left: auto; height: 72rpx; padding: 0 40rpx;
  background: $bg-dark; border-radius: 16rpx;
  display: flex; align-items: center; justify-content: center;
}
.count-go text { color: #fff; font-size: 28rpx; font-weight: 500; }

.chip-scroll { white-space: nowrap; }
.chip-row { display: inline-flex; gap: 14rpx; }
.chip {
  display: inline-block; padding: 14rpx 32rpx; border-radius: $radius-full;
  font-size: 26rpx; color: $text-muted; background: $bg-surface;
  border: 1px solid $border-light;
}
.chip.active { background: rgba(217,117,10,0.06); border-color: $accent; color: $accent; font-weight: 600; }

@media (min-width: 1025px) { .practice-entry { max-width: 700px; margin: 0 auto; } }
</style>
```

- [ ] **Step 2: Create practice-entry/do.vue**

Adapt from `pages/exam/do.vue` — remove exam timer, add random/topic loading, keep prev/next and scoring:

```vue
<template>
  <view class="practice-do">
    <!-- Progress -->
    <view class="progress-wrap">
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }" />
      </view>
      <text class="progress-text">{{ currentIndex + 1 }}/{{ questions.length }}</text>
    </view>

    <view class="quiz-card" v-if="currentQuestion">
      <view class="quiz-meta">
        <text class="quiz-index">第 {{ currentIndex + 1 }} 题</text>
        <text class="quiz-type">{{ typeLabel(currentQuestion.type) }}</text>
      </view>
      <text class="quiz-title">{{ currentQuestion.title }}</text>

      <view v-if="currentQuestion.type <= 2" class="options">
        <view v-for="(opt, i) in parseOptions(currentQuestion.options)" :key="i"
          class="option" :class="{ selected: selectedIdx === i }"
          @click="selectedIdx = i">
          <view class="opt-letter" :class="{ active: selectedIdx === i }">{{ letters[i] }}</view>
          <text class="opt-text">{{ opt.content || opt }}</text>
        </view>
      </view>

      <view v-if="currentQuestion.type === 3" class="options">
        <view class="option" :class="{ selected: selectedIdx === 0 }" @click="selectedIdx = 0">
          <view class="opt-letter" :class="{ active: selectedIdx === 0 }">✓</view>
          <text class="opt-text">正确</text>
        </view>
        <view class="option" :class="{ selected: selectedIdx === 1 }" @click="selectedIdx = 1">
          <view class="opt-letter" :class="{ active: selectedIdx === 1 }">✗</view>
          <text class="opt-text">错误</text>
        </view>
      </view>

      <input v-if="currentQuestion.type === 4" class="fill-input" v-model="fillAnswer" placeholder="输入答案..." />

      <!-- Result -->
      <view v-if="answered" class="quiz-result" :class="lastCorrect ? 'is-correct' : 'is-wrong'">
        <text class="result-icon">{{ lastCorrect ? '✓' : '✗' }}</text>
        <view class="result-body">
          <text class="result-verdict">{{ lastCorrect ? '回答正确' : '回答错误' }}</text>
          <text class="result-answer">正确答案：{{ currentQuestion.answer }}</text>
          <text class="result-analysis" v-if="currentQuestion.analysis">{{ currentQuestion.analysis }}</text>
        </view>
      </view>
    </view>

    <!-- Actions -->
    <view class="quiz-actions" v-if="answered">
      <view v-if="currentIndex > 0" class="btn-action secondary" @click="prevQuestion"><text>上一题</text></view>
      <view v-if="currentIndex < questions.length - 1" class="btn-action primary" @click="nextQuestion"><text>下一题</text></view>
      <view v-else class="btn-action finish" @click="finish"><text>查看结果</text></view>
    </view>
    <view v-else class="btn-submit" :class="{ disabled: !hasAnswer }" @click="submitAnswer"><text>提交答案</text></view>

    <!-- Finish -->
    <view class="finish-screen" v-if="finished">
      <view class="finish-circle" :class="scorePercent >= 70 ? 'good' : 'retry'">
        <text class="finish-score">{{ correctCount }}/{{ questions.length }}</text>
      </view>
      <text class="finish-msg">{{ scorePercent >= 80 ? '太棒了！' : scorePercent >= 60 ? '继续加油！' : '多多练习！' }}</text>
      <view class="btn-retry" @click="retry"><text>再刷一次</text></view>
      <view class="btn-back" @click="goBack"><text>返回</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post } from '@/utils/request';

interface Question { id: number; type: number; title: string; options: string; answer: string; analysis: string; }
interface AR { isCorrect: boolean; correctAnswer: string; analysis: string; }

const letters = ['A', 'B', 'C', 'D'];
const mode = ref('random');
const count = ref(10);
const categoryId = ref('');

const questions = ref<Question[]>([]);
const currentIndex = ref(0);
const currentQuestion = ref<Question | null>(null);
const selectedIdx = ref<number | null>(null);
const fillAnswer = ref('');
const answered = ref(false);
const lastCorrect = ref(false);
const correctCount = ref(0);
const finished = ref(false);

const hasAnswer = computed(() => {
  if (!currentQuestion.value) return false;
  if (currentQuestion.value.type <= 3) return selectedIdx.value !== null;
  return fillAnswer.value.trim() !== '';
});
const scorePercent = computed(() =>
  questions.value.length ? Math.round(correctCount.value / questions.value.length * 100) : 0
);

onLoad((opts: any) => {
  mode.value = opts.mode || 'random';
  count.value = Number(opts.count) || 10;
  categoryId.value = opts.categoryId || '';
});

function parseOptions(o: string) { if (!o) return []; try { return JSON.parse(o) } catch { return [] } }
function typeLabel(t: number) { return ['', '单选', '多选', '判断', '填空'][t] || ''; }

function getAnswerString(): string {
  if (!currentQuestion.value) return '';
  if (currentQuestion.value.type <= 2) {
    return selectedIdx.value !== null ? letters[selectedIdx.value] : '';
  }
  if (currentQuestion.value.type === 3) return selectedIdx.value === 0 ? '正确' : '错误';
  return fillAnswer.value.trim();
}

async function loadQuestions() {
  try {
    if (mode.value === 'topic' && categoryId.value) {
      const r = await get<{ records: Question[] }>('/api/questions', { categoryId: categoryId.value, size: count.value });
      const data = (r.data as any);
      questions.value = data.records || data || [];
    } else {
      const r = await get<Question[]>('/api/questions/random', { size: count.value } as Record<string, unknown>);
      questions.value = r.data || [];
    }
    if (questions.value.length > 0) currentQuestion.value = questions.value[0];
  } catch { uni.showToast({ title: '加载失败', icon: 'error' }); }
}

async function submitAnswer() {
  if (!hasAnswer.value || !currentQuestion.value) return;
  try {
    const r = await post<AR>('/api/answers', { questionId: currentQuestion.value.id, userAnswer: getAnswerString() });
    lastCorrect.value = r.data.isCorrect;
    if (r.data.isCorrect) correctCount.value++;
    answered.value = true;
  } catch { uni.showToast({ title: '提交失败', icon: 'error' }); }
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--;
    currentQuestion.value = questions.value[currentIndex.value];
    selectedIdx.value = null; fillAnswer.value = ''; answered.value = false;
  }
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++;
    currentQuestion.value = questions.value[currentIndex.value];
    selectedIdx.value = null; fillAnswer.value = ''; answered.value = false;
  }
}

function finish() { finished.value = true; }

function retry() {
  finished.value = false; selectedIdx.value = null; fillAnswer.value = '';
  answered.value = false; correctCount.value = 0; currentIndex.value = 0; questions.value = [];
  loadQuestions();
}

function goBack() { uni.navigateBack(); }

onMounted(() => { loadQuestions(); });
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.practice-do { min-height: 100vh; background: $bg-canvas; display: flex; flex-direction: column; }

.progress-wrap {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 28rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light;
}
.progress-bar { flex: 1; height: 6rpx; background: $bg-surface; border-radius: 3rpx; overflow: hidden; }
.progress-fill { height: 100%; background: $accent; border-radius: 3rpx; transition: width 0.3s; }
.progress-text { font-size: 24rpx; color: $text-light; font-weight: 500; }

.quiz-card {
  flex: 1; margin: 20rpx; padding: 36rpx 28rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; box-shadow: $shadow-sm; overflow-y: auto;
}
.quiz-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.quiz-index { font-size: 24rpx; color: $accent; font-weight: 600; }
.quiz-type { font-size: 22rpx; color: $text-light; background: $bg-surface; padding: 6rpx 16rpx; border-radius: $radius-full; }
.quiz-title { font-size: 30rpx; color: $text-main; line-height: 1.8; margin-bottom: 32rpx; display: block; }

.options { display: flex; flex-direction: column; gap: 14rpx; }
.option {
  display: flex; align-items: center; gap: 18rpx;
  padding: 24rpx 22rpx; border: 1px solid $border-light;
  border-radius: $radius-md; font-size: 28rpx; color: $text-main;
}
.option:active { background: $bg-surface; }
.option.selected { border-color: $accent; background: rgba(217,117,10,0.04); }
.opt-letter {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1px solid $border-medium; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; font-weight: 600;
  color: $text-light; flex-shrink: 0; background: $bg-surface;
}
.opt-letter.active { background: $bg-dark; color: #fff; border-color: $bg-dark; }
.opt-text { flex: 1; line-height: 1.6; }

.fill-input { width: 100%; border: 1px solid $border-medium; border-radius: $radius-md; padding: 20rpx 24rpx; font-size: 28rpx; background: $bg-surface; box-sizing: border-box; }

.quiz-result { margin-top: 28rpx; padding: 24rpx; border-radius: $radius-md; display: flex; gap: 16rpx; }
.quiz-result.is-correct { background: rgba(34,197,94,0.06); border: 1px solid rgba(34,197,94,0.2); }
.quiz-result.is-wrong { background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.2); }
.result-icon { font-size: 40rpx; font-weight: 700; flex-shrink: 0; }
.is-correct .result-icon { color: $color-success; }
.is-wrong .result-icon { color: $color-danger; }
.result-body { flex: 1; }
.result-verdict { font-size: 26rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.is-correct .result-verdict { color: $color-success; }
.is-wrong .result-verdict { color: $color-danger; }
.result-answer { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 6rpx; }
.result-analysis { font-size: 24rpx; color: $text-light; line-height: 1.7; }

.quiz-actions { display: flex; gap: 16rpx; padding: 20rpx 28rpx 44rpx; background: $bg-paper; border-top: 1px solid $border-light; }
.btn-action { flex: 1; height: 88rpx; border-radius: $radius-xl; border: none; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 500; }
.btn-action:active { opacity: 0.9; }
.btn-action.secondary { background: $bg-surface; color: $text-main; }
.btn-action.primary { background: $bg-dark; color: #fff; }
.btn-action.finish { background: $accent; color: #fff; font-weight: 600; }

.btn-submit { width: calc(100% - 40rpx); height: 88rpx; margin: 20rpx auto 0; background: $bg-dark; color: #fff; font-size: 30rpx; font-weight: 600; border-radius: $radius-lg; border: none; display: flex; align-items: center; justify-content: center; }
.btn-submit:active { opacity: 0.9; }
.btn-submit.disabled { opacity: 0.4; }

.finish-screen { display: flex; flex-direction: column; align-items: center; padding-top: 180rpx; }
.finish-circle { width: 180rpx; height: 180rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 32rpx; }
.finish-circle.good { background: rgba(34,197,94,0.1); border: 2px solid $color-success; }
.finish-circle.retry { background: rgba(217,117,10,0.1); border: 2px solid $accent; }
.finish-score { font-family: Georgia, serif; font-size: 48rpx; font-weight: 700; color: $text-main; }
.finish-msg { font-size: 28rpx; color: $text-muted; margin-bottom: 56rpx; }
.btn-retry, .btn-back { width: calc(100% - 80rpx); max-width: 500rpx; height: 88rpx; font-size: 30rpx; font-weight: 600; border-radius: $radius-lg; border: none; display: flex; align-items: center; justify-content: center; margin-bottom: 20rpx; }
.btn-retry { background: $bg-dark; color: #fff; }
.btn-back { background: $bg-surface; color: $text-muted; }
.btn-retry:active, .btn-back:active { opacity: 0.9; }

@media (min-width: 1025px) { .practice-do { max-width: 800px; margin: 0 auto; } }
</style>
```

- [ ] **Step 3: Delete old exam pages**

```bash
rm AI-Interview/pages/exam/index.vue
rm AI-Interview/pages/exam/do.vue
```

- [ ] **Step 4: Commit**

```bash
git add AI-Interview/pages/practice-entry/ AI-Interview/pages/exam/
git commit -m "feat: redesign mini-program 自由刷题 as dual-entry page"
```

---

### Task 11: 验证构建

**Files:** None (verification only)

- [ ] **Step 1: Build Web frontend**

```bash
cd AI-Interview/web-app && npx vite build
```

Expected: No errors. Output in `dist/`.

- [ ] **Step 2: Verify no stale route references**

```bash
cd E:/My_Projects/IntervVault && grep -r "'/exam'" AI-Interview/web-app/src/ --include="*.vue" --include="*.ts"
```

Expected: No results (all `/exam` references removed).

- [ ] **Step 3: Verify no stale import references**

```bash
cd E:/My_Projects/IntervVault && grep -r "ExamView\|ExamDoView\|PracticeView" AI-Interview/web-app/src/ --include="*.vue" --include="*.ts"
```

Expected: No references to old component names.

- [ ] **Step 4: Run dev server and spot-check**

```bash
cd AI-Interview/web-app && npx vite --host 0.0.0.0 &
```

Navigate to:
- `http://localhost:5173/questions` — 查看题库 list
- `http://localhost:5173/questions/1` — detail view
- `http://localhost:5173/practice` — 自由刷题 entry
- `http://localhost:5173/` — homepage with updated cards

- [ ] **Step 5: Commit any final fixes**
