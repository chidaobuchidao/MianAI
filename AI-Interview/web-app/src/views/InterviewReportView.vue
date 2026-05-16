<template>
  <div class="page">
    <div class="page__inner" v-if="loading">
      <SkeletonBar :lines="4" :widths="['70%','90%','60%','80%']" height="16px" gap="14px" />
    </div>
    <div class="page__inner animate-fade-in-up" v-else-if="report">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">面试报告</span>
        <div style="width:36px" />
      </div>

      <div class="score-header">
        <div class="score-ring">
          <svg viewBox="0 0 72 72">
            <circle class="score-ring__bg" cx="36" cy="36" r="30"/>
            <circle class="score-ring__fg" :class="ringColor" cx="36" cy="36" r="30"
              :stroke-dasharray="188.5" :stroke-dashoffset="188.5 - (188.5 * (report.overallScore || 0) / 10)"/>
          </svg>
          <div class="score-ring__center">
            <span class="score-ring__num">{{ report.overallScore }}</span>
            <span class="score-ring__max">/10</span>
          </div>
        </div>
        <div class="score-header__text">
          <span class="score-header__verdict">{{ verdict }}</span>
          <p class="score-header__summary">{{ report.feedback }}</p>
        </div>
      </div>

      <div class="diag-section" v-if="dimArray.length > 0">
        <div class="dim-card" v-for="(dim, i) in dimArray" :key="i">
          <span class="dim-card__title">{{ dim.name }} · {{ dim.score }}/10</span>
          <p class="dim-card__comment">{{ dim.comment }}</p>
        </div>
      </div>

      <div class="diag-section" v-if="report.suggestion">
        <div class="dim-card">
          <span class="dim-card__title">提升建议</span>
          <p class="dim-card__comment">{{ report.suggestion }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/utils/request'
import SkeletonBar from '@/components/SkeletonBar.vue'

interface Dim { name: string; score: number; comment: string }
interface Report {
  overallScore: number | null
  feedback: string
  suggestion?: string
  dimensions: Dim[] | string
  position?: string
  createTime?: string
}

const route = useRoute()
const report = ref<Report | null>(null)
const loading = ref(true)

function loadFromStorage() {
  const score = sessionStorage.getItem('interviewScore')
  const feedback = sessionStorage.getItem('interviewFeedback')
  if (!score && !feedback) return null
  let dims: Dim[] = []
  try {
    const raw = sessionStorage.getItem('interviewDims')
    if (raw) dims = JSON.parse(raw)
  } catch {}
  return {
    overallScore: Number(score) || 0,
    feedback: feedback || '',
    suggestion: sessionStorage.getItem('interviewSuggestion') || '',
    dimensions: dims
  } as Report
}

onMounted(async () => {
  const id = route.query.id
  if (!id) { loading.value = false; return }

  // Show cached data immediately from SSE-delivered sessionStorage
  const cached = loadFromStorage()
  if (cached) {
    report.value = cached
    loading.value = false
  }

  try {
    const r = await get<Report>(`/api/interview/${id}`)
    if (r.data) {
      const raw = r.data as any
      let dims: Dim[] = []
      if (typeof raw.dimensions === 'string') {
        try { dims = JSON.parse(raw.dimensions) } catch {}
      } else if (Array.isArray(raw.dimensions)) {
        dims = raw.dimensions
      }
      report.value = {
        overallScore: raw.overallScore ?? 0,
        feedback: raw.feedback || '',
        suggestion: raw.suggestion || raw.feedback || '',
        dimensions: dims,
        position: raw.position,
        createTime: raw.createTime
      }
    }
    // Clear cache after successful API load
    sessionStorage.removeItem('interviewScore')
    sessionStorage.removeItem('interviewFeedback')
    sessionStorage.removeItem('interviewDims')
    sessionStorage.removeItem('interviewSuggestion')
  } catch {}
  if (!report.value) loading.value = false
})

const dimArray = computed(() => {
  if (!report.value) return [] as Dim[]
  const d = report.value.dimensions
  return Array.isArray(d) ? d : []
})

const verdict = computed(() => {
  if (!report.value) return ''
  const s = report.value.overallScore
  if (s >= 8) return '优秀'
  if (s >= 6) return '良好'
  if (s >= 4) return '一般'
  return '需提升'
})

const ringColor = computed(() => {
  if (!report.value) return 'score-ring__fg--muted'
  const s = report.value.overallScore
  if (s >= 8) return 'score-ring__fg--high'
  if (s >= 6) return 'score-ring__fg--mid'
  return 'score-ring__fg--low'
})
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

/* Circular score ring */
.score-ring {
  width: 72px; height: 72px; flex-shrink: 0; position: relative;
  display: flex; align-items: center; justify-content: center;
}
.score-ring svg { width: 72px; height: 72px; transform: rotate(-90deg); }
.score-ring__bg { fill: none; stroke: var(--border-light); stroke-width: 5; }
.score-ring__fg { fill: none; stroke-width: 5; stroke-linecap: round; }
.score-ring__fg--high { stroke: #16A34A; }
.score-ring__fg--mid { stroke: #D97706; }
.score-ring__fg--low { stroke: #DC2626; }
.score-ring__fg--muted { stroke: var(--text-light); }
.score-ring__center {
  position: absolute; inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
}
.score-ring__num {
  font-size: 22px; font-weight: 700; letter-spacing: -1px; line-height: 1;
}
.score-ring__max { font-size: 9px; color: var(--text-light); font-weight: 500; margin-top: 1px; }

.score-header {
  display: flex; gap: 16px; align-items: flex-start;
  margin-bottom: 36px;
}
.score-header__text { flex: 1; }
.score-header__verdict {
  font-size: 18px; font-weight: 600; display: block; margin-bottom: 6px;
}
.score-header__summary {
  font-size: 14px; color: var(--text-muted); line-height: 1.7;
}

.diag-section { margin-bottom: 8px; }
.dim-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 16px;
  margin-bottom: 10px;
  box-shadow: var(--shadow-sm);
}
.dim-card__title {
  font-size: 14px; font-weight: 500; display: block; margin-bottom: 6px;
}
.dim-card__comment {
  font-size: 13px; color: var(--text-muted); line-height: 1.6;
}
</style>
