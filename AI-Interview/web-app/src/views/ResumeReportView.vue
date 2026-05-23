<template>
  <div class="page">
    <!-- Loading -->
    <div class="loading-screen" v-if="loading">
      <div class="loading-spinner" />
      <span class="loading-text">{{ loadingText }}</span>
    </div>

    <!-- Report -->
    <div class="page__inner" v-if="!loading && report">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <span class="page-head__title">简历报告</span>
        <div style="width:36px" />
      </div>

      <div class="quota-error" v-if="quotaError" @click="quotaError = ''">{{ quotaError }}</div>

      <!-- Score hero with grid scan background -->
      <div class="score-hero">
        <!-- 卡片扫描动画配置
             Props 说明：
             - lineThickness: 扫描线宽，单位 px（数值），控制线条粗细，示例 1.0~2.5
             - linesColor: 网格/线条颜色，接受 CSS 颜色字符串
             - scanColor: 扫描光带/高亮颜色，接受 CSS 颜色字符串
             - scanOpacity: 光带不透明度（0-1），控制亮度强弱
             - gridScale: 网格缩放比例（0-1），值越小网格越细密
             - scanGlow: 光晕强度（数值），影响发光范围与强度
             - scanSoftness: 光带柔和度（数值），值越大越模糊柔和
             - scanDuration: 单次扫描时长，单位秒
             - scanDelay: 扫描延迟或循环间隔，单位秒
             建议：先在较小数值（scanOpacity 0.4-0.8，scanGlow/scanSoftness 0.5-1.5）调试，
             再根据视觉效果微调到目标视觉一致性。
        -->
        <GridScan :lineThickness="1.4" linesColor="#5a4a35" scanColor="#ffd300" :scanOpacity="0.75" :gridScale="0.10"
          :scanGlow="0.8" :scanSoftness="1.5" :scanDuration="2.5" :scanDelay="1.5" />
        <div class="score-ring" :class="scoreClass">
          <svg viewBox="0 0 160 160">
            <circle cx="80" cy="80" r="72" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="6" />
            <circle cx="80" cy="80" r="72" fill="none" :stroke="ringStroke" stroke-width="6" stroke-linecap="round"
              :stroke-dasharray="452" :stroke-dashoffset="452 - (452 * score / 10)" transform="rotate(-90 80 80)" />
          </svg>
          <span class="score-num">{{ score }}</span>
          <span class="score-unit">/10</span>
        </div>
        <span class="score-label">简历综合评分</span>
        <span class="score-file">{{ report.fileName || '未命名简历' }}</span>
      </div>

      <!-- Dimensions -->
      <div class="card" v-if="dimArray.length > 0">
        <span class="card-label">能力维度</span>
        <div class="dim-item" v-for="d in dimArray" :key="d.name">
          <div class="dim-head">
            <span class="dim-name">{{ d.name }}</span>
            <span class="dim-score">{{ d.score }}/10</span>
          </div>
          <div class="dim-bar-bg">
            <div class="dim-bar-fill" :style="{ width: d.score * 10 + '%' }" />
          </div>
          <span class="dim-comment" v-if="d.comment">{{ d.comment }}</span>
        </div>
      </div>

      <!-- Missing keywords -->
      <div class="card" v-if="missingKw.length > 0">
        <span class="card-label">缺失关键词（对标 JD）</span>
        <div class="kw-list">
          <span class="kw-tag" v-for="kw in missingKw" :key="kw">{{ kw }}</span>
        </div>
      </div>

      <!-- Suggestion -->
      <div class="card" v-if="report.suggestion">
        <span class="card-label">总体建议</span>
        <span class="card-text">{{ report.suggestion }}</span>
      </div>

      <!-- ===== Deep Optimization ===== -->
      <div class="card">
        <span class="card-label">深度优化</span>

        <!-- Idle: start -->
        <div v-if="deepStatus === 0 && score > 0" class="deep-idle">
          <p class="deep-hint">AI 将逐段优化简历并生成面试追问</p>
          <div class="model-bar">
            <span class="model-label">模型</span>
            <div class="capsule-toggle">
              <div class="capsule-slider" :class="{ right: deepModel === 'deepseek-v4-pro' }" />
              <button class="capsule-opt" :class="{ active: deepModel === 'deepseek-v4-flash' }"
                @click="deepModel = 'deepseek-v4-flash'">Flash</button>
              <button class="capsule-opt" :class="{ active: deepModel === 'deepseek-v4-pro' }"
                @click="deepModel = 'deepseek-v4-pro'">Pro</button>
            </div>
          </div>
          <button class="btn btn--primary btn--full" @click="startDeep">
            <span>开始深度优化</span>
          </button>
          <p class="quota-error" v-if="quotaError" @click="quotaError = ''">{{ quotaError }}</p>
        </div>

        <!-- Running -->
        <div v-if="deepStatus === 1" class="deep-running">
          <div class="deep-spinner" />
          <span class="deep-text">AI 正在深度优化简历...</span>
          <span class="deep-time">已运行 {{ deepElapsed }}s</span>
        </div>

        <!-- Failed -->
        <div v-if="deepStatus === -1" class="deep-failed">
          <span class="deep-fail-text">深度优化失败</span>
          <span class="deep-fail-hint" v-if="retryRemaining > 0">还可重试 {{ retryRemaining }} 次</span>
          <span class="deep-fail-hint" v-else>已达最大重试次数</span>
          <div class="btn-row" style="margin-top:16px;justify-content:center;">
            <button v-if="retryRemaining > 0" class="btn btn--dark" @click="retryDeep">重新优化</button>
          </div>
        </div>

        <!-- Completed -->
        <template v-if="deepStatus === 2">
          <!-- Diff highlights -->
          <div v-if="highlights.length > 0">
            <span class="card-sub-label">逐段优化对比</span>
            <div class="highlight-item" v-for="(h, i) in highlights" :key="i">
              <UnifiedDiff :oldText="h.before" :newText="h.after" :sectionName="h.section" :contextLines="2" />
              <span class="hl-reason">{{ h.reason }}</span>
            </div>
          </div>

          <!-- Optimized text -->
          <div v-if="report.optimizedText" class="deep-result">
            <span class="card-sub-label">优化后简历</span>
            <div class="btn-row" style="margin-bottom:12px;">
              <button class="btn btn--outline" @click="copyText">复制</button>
              <button class="btn btn--outline" @click="previewWord">预览</button>
              <a class="btn btn--accent" :href="downloadUrl" target="_blank">下载 Word</a>
            </div>
            <div class="opt-box">
              <pre class="opt-text">{{ report.optimizedText }}</pre>
            </div>
          </div>

          <!-- Interview questions -->
          <div v-if="iqList.length > 0" class="deep-questions">
            <span class="card-sub-label">面试可能追问</span>
            <div class="iq-item" v-for="(q, i) in iqList" :key="i">
              <span class="iq-num">{{ i + 1 }}</span>
              <span class="iq-text">{{ q }}</span>
            </div>
          </div>
        </template>
      </div>

      <!-- Actions -->
      <div class="actions">
        <button class="btn btn--dark btn--full" @click="goInterview">应用到面试</button>
        <button class="btn btn--ghost btn--full" @click="$router.push('/')">返回首页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post } from '@/utils/request'
import { useQuota } from '@/composables/useQuota'
import GridScan from '@/components/GridScan.vue'
import UnifiedDiff from '@/components/UnifiedDiff.vue'

interface Dim { name: string; score: number; comment: string }
interface Highlight { section: string; before: string; after: string; reason: string }
interface Report {
  resumeId: number; overallScore: number; fileName: string
  dimensions: Dim[]; missingKeywords: string[]; highlights: Highlight[]
  optimizedText: string; interviewQuestions: string[]; suggestion: string
  parseStatus?: number; deepStatus?: number
}

const route = useRoute()
const router = useRouter()
const { fetchQuota, checkQuota } = useQuota()

const loading = ref(true)
const quotaError = ref('')
const loadingText = ref('AI 正在分析简历...')
const report = ref<Report | null>(null)
const score = ref(0)
const deepStatus = ref(0)
const deepModel = ref('deepseek-v4-flash')
const deepElapsed = ref(0)
const retryRemaining = ref(3)
let deepTimer: ReturnType<typeof setInterval> | null = null
let abortController: AbortController | null = null

const dimArray = computed(() => {
  if (!report.value) return [] as Dim[]
  const d = report.value.dimensions
  return Array.isArray(d) ? d : []
})
const missingKw = computed(() => {
  const k = (report.value as any)?.missingKeywords
  return Array.isArray(k) ? k : []
})
const highlights = computed(() => {
  const h = (report.value as any)?.highlights
  return Array.isArray(h) ? h : []
})
const iqList = computed(() => {
  const q = (report.value as any)?.interviewQuestions
  return Array.isArray(q) ? q : []
})

const scoreClass = computed(() => score.value >= 7 ? 'great' : score.value >= 4 ? 'ok' : 'low')
const ringStroke = computed(() => score.value >= 7 ? '#22C55E' : score.value >= 4 ? '#D9770A' : '#EF4444')
const downloadUrl = computed(() => report.value ? `/api/resume/${report.value.resumeId}/export-word` : '#')

function isValidReport(d: unknown): d is Report {
  return d != null && typeof d === 'object' && 'resumeId' in (d as any)
}

async function loadFullReport(resumeId: number) {
  try {
    const r = await get<Report>(`/api/resume/${resumeId}/analysis`)
    if (r.data && isValidReport(r.data)) {
      report.value = r.data
      if (r.data.overallScore != null) {
        score.value = r.data.overallScore
        deepStatus.value = (r.data as any).deepStatus ?? 0
        loading.value = false
        return true
      }
    }
  } catch { }
  return false
}

onMounted(async () => {
  const resumeId = Number(route.query.resumeId)
  if (!resumeId) { loading.value = false; return }

  // Phase 1: load existing report
  let r = await get<any>(`/api/resume/${resumeId}/analysis`)
  if (isValidReport(r.data)) {
    report.value = r.data
    if (r.data.overallScore != null) {
      score.value = r.data.overallScore
      deepStatus.value = (r.data as any).deepStatus ?? 0
      loading.value = false
      if (deepStatus.value === -1) loadRetryStatus(resumeId)
      return
    }
    if (r.data.parseStatus === -1) { loading.value = false; return }
  }

  // Check if backend blocked auto-analysis due to quota
  const quotaMsg = (r.data as any)?.suggestion
  if (quotaMsg && quotaMsg.includes('免费次数已用完')) {
    quotaError.value = quotaMsg
    report.value = r.data as any
    loading.value = false
    return
  }

  // Phase 2: trigger analysis only if quota allows
  if (r.data?.parseStatus === 1) {
    await fetchQuota()
    const qc = checkQuota(1)
    if (!qc.ok) {
      quotaError.value = qc.msg!
      loading.value = false
      return
    }
    await post(`/api/resume/${resumeId}/analyze`).catch(() => { })
  }

  // Phase 3: poll
  for (let i = 0; i < 20; i++) {
    await sleep(2000)
    loadingText.value = `AI 正在分析简历... (${(i + 1) * 2}s)`
    const done = await loadFullReport(resumeId)
    if (done) return
  }
  loading.value = false
})

function loadRetryStatus(resumeId: number) {
  get<{ retryCount: number }>(`/api/resume/${resumeId}/deep-status`).then(s => {
    const cnt = s.data?.retryCount ?? 0
    retryRemaining.value = Math.max(0, 3 - cnt)
  }).catch(() => { })
}

// ===== Deep optimization =====
async function startDeep() {
  if (!report.value) return
  const resumeId = report.value.resumeId
  await fetchQuota()
  const needed = deepModel.value.includes('pro') ? 2 : 1
  const qc = checkQuota(needed)
  if (!qc.ok) { quotaError.value = qc.msg!; return }
  deepStatus.value = 1
  deepElapsed.value = 0
  deepTimer = setInterval(() => deepElapsed.value++, 1000)
  streamDeep(resumeId)
}

async function retryDeep() {
  if (!report.value) return
  await fetchQuota()
  const needed = deepModel.value.includes('pro') ? 2 : 1
  const qc = checkQuota(needed)
  if (!qc.ok) { quotaError.value = qc.msg!; return }
  deepStatus.value = 1
  deepElapsed.value = 0
  deepTimer = setInterval(() => deepElapsed.value++, 1000)
  streamDeep(report.value.resumeId)
}

async function streamDeep(resumeId: number) {
  abortController = new AbortController()
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch(`/api/resume/${resumeId}/analyze-deep?model=${encodeURIComponent(deepModel.value)}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      signal: abortController.signal
    })
    const reader = res.body?.getReader()
    if (!reader) throw new Error('No stream')
    while (true) {
      const { done } = await reader.read()
      if (done) break
    }
    cleanupStream()
    deepStatus.value = 2
    await loadFullReport(resumeId)
  } catch {
    if (deepStatus.value === 2) return
    cleanupStream()
    try {
      const s = await get<{ deepStatus: number; retryCount: number }>(`/api/resume/${resumeId}/deep-status`)
      deepStatus.value = s.data?.deepStatus ?? -1
      const cnt = s.data?.retryCount ?? 0
      retryRemaining.value = Math.max(0, 3 - cnt)
    } catch {
      deepStatus.value = -1
      retryRemaining.value = 0
    }
  }
}

function cleanupStream() {
  abortController?.abort()
  abortController = null
  if (deepTimer) { clearInterval(deepTimer); deepTimer = null }
}

// ===== Actions =====
function copyText() {
  if (report.value?.optimizedText) {
    navigator.clipboard.writeText(report.value.optimizedText).then(() => alert('已复制'))
  }
}
function previewWord() {
  if (report.value) window.open(`/api/resume/${report.value.resumeId}/preview-html?token=${localStorage.getItem('token')}`, '_blank')
}
function goInterview() {
  if (report.value) {
    localStorage.setItem('resumeForInterview', JSON.stringify({ resumeId: report.value.resumeId }))
  }
  router.push('/interview')
}

function sleep(ms: number) { return new Promise(r => setTimeout(r, ms)) }

onUnmounted(() => { cleanupStream() })
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--bg-canvas);
}

.page__inner {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 20px 60px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 0;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-main);
}

.page-head__title {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
}

/* Loading */
.loading-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-light);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 20px;
}

/* Score hero */
.score-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0 36px;
  margin: 0 -20px;
  background: #141413;
  position: relative;
  overflow: hidden;
  border-radius: 10px;
}

/* Score content above GridScan */
.score-hero> :not(:first-child) {
  position: relative;
  z-index: 1;
}

.score-ring,
.score-label,
.score-file {
  position: relative;
  z-index: 1;
}

.score-ring {
  width: 140px;
  height: 140px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-ring svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.score-num {
  font-size: 52px;
  font-weight: 900;
  color: #fff;
  font-family: var(--font-serif);
  line-height: 1;
}

.score-unit {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 2px;
}

.score-label {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 12px;
}

.score-file {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 4px;
}

/* Cards */
.card {
  background: var(--bg-paper);
  margin-top: 14px;
  padding: 22px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.card-label {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  display: block;
  margin-bottom: 14px;
}

.card-sub-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
  display: block;
  margin: 20px 0 14px;
}

.card-text {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.7;
  display: block;
}

/* Dimensions */
.dim-item {
  padding: 12px 0;
}

.dim-item+.dim-item {
  border-top: 1px solid var(--border-light);
}

.dim-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.dim-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.dim-score {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent);
}

.dim-bar-bg {
  height: 4px;
  background: var(--bg-surface);
  border-radius: 2px;
  overflow: hidden;
}

.dim-bar-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 2px;
  transition: width 0.6s ease;
}

.dim-comment {
  font-size: 13px;
  color: var(--text-light);
  margin-top: 4px;
  display: block;
}

/* Keywords */
.kw-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.kw-tag {
  font-size: 12px;
  background: rgba(239, 68, 68, 0.08);
  color: var(--color-danger);
  padding: 5px 12px;
  border-radius: var(--radius-sm);
}

/* Buttons */
.btn {
  padding: 12px 24px;
  border-radius: 100px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s;
  text-decoration: none;
}

.btn--primary {
  background: var(--bg-dark);
  color: #fff;
}

.btn--dark {
  background: var(--bg-dark);
  color: #fff;
}

.btn--accent {
  background: var(--accent);
  color: #fff;
}

.btn--outline {
  background: var(--bg-surface);
  border: 1px solid var(--border-medium);
  color: var(--text-muted);
}

.btn--ghost {
  background: var(--bg-surface);
  color: var(--text-muted);
}

.btn--full {
  width: 100%;
  justify-content: center;
  padding: 14px;
  font-size: 15px;
  font-weight: 600;
}

.btn:hover {
  opacity: 0.9;
}

.btn:active {
  transform: scale(0.98);
}

.btn-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* Deep optimization */
.deep-idle {
  text-align: center;
  padding: 16px 0;
}

.deep-hint {
  font-size: 13px;
  color: var(--text-light);
  margin-bottom: 16px;
}

.model-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}

.model-label { font-size: 13px; color: var(--text-muted); }

.capsule-toggle {
  position: relative;
  display: inline-flex;
  border: 1px solid var(--border-medium);
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface);
}
.capsule-slider {
  position: absolute;
  top: 0; left: 0;
  width: 50%; height: 100%;
  background: var(--bg-dark);
  border-radius: var(--radius-full);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 0;
}
.capsule-slider.right {
  transform: translateX(100%);
}
.capsule-opt {
  position: relative; z-index: 1;
  padding: 5px 16px;
  font-size: 13px; font-weight: 500;
  border: none; background: transparent;
  color: var(--text-muted); cursor: pointer;
  transition: color 0.25s;
}
.capsule-opt.active {
  color: #fff;
}

.deep-running {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
}

.deep-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-light);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.deep-text {
  font-size: 14px;
  color: var(--accent);
  margin-top: 12px;
}

.deep-time {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 6px;
}

.deep-failed {
  text-align: center;
  padding: 16px 0;
}

.deep-fail-text {
  font-size: 16px;
  color: var(--color-danger);
  font-weight: 600;
  display: block;
}

.deep-fail-hint {
  font-size: 13px;
  color: var(--text-light);
  margin-top: 6px;
  display: block;
}

/* Diff highlights */
.highlight-item {
  padding: 14px 0;
}

.highlight-item+.highlight-item {
  border-top: 1px solid var(--border-light);
}

.hl-reason {
  font-size: 13px;
  color: var(--text-light);
  margin-top: 8px;
  display: block;
}

/* Optimized text */
.deep-result {
  margin-top: 8px;
}

.opt-box {
  background: var(--bg-surface);
  border-radius: var(--radius-md);
  padding: 18px;
  max-height: 500px;
  overflow-y: auto;
}

.opt-text {
  font-size: 14px;
  color: var(--text-main);
  line-height: 1.8;
  white-space: pre-wrap;
  margin: 0;
  font-family: inherit;
}

/* Interview questions */
.deep-questions {
  margin-top: 8px;
}

.iq-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
}

.iq-item+.iq-item {
  border-top: 1px solid var(--border-light);
}

.iq-num {
  width: 28px;
  height: 28px;
  background: rgba(217, 117, 10, 0.08);
  color: var(--accent);
  font-size: 13px;
  font-weight: 700;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.iq-text {
  font-size: 14px;
  color: var(--text-main);
  line-height: 1.6;
}

/* Actions */
.actions {
  padding: 24px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.quota-error {
  margin-top: 12px; padding: 10px 14px;
  background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.15);
  border-radius: 8px; color: #EF4444;
  font-size: 13px; text-align: center; cursor: pointer;
}
</style>
