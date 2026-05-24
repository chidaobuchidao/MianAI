<template>
  <div class="app-shell">
    <!-- Header -->
    <header class="app-header">
      <div class="app-brand">
        <button class="btn btn-icon" @click="router.replace('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="brand-name">Mianmian.</span>
        <span class="brand-tag">降AI检测</span>
        <button class="btn btn-outline btn-switch" @click="router.replace('/paper-tools/polish')">学术润色</button>
        <button class="btn btn-outline btn-switch" @click="router.replace('/paper-tools/plagiarism-reduce')">降查重</button>
      </div>
      <div style="display:flex;align-items:center;gap:16px;">
        <div class="capsule-toggle">
          <div class="capsule-slider" :class="{ right: aiModel === 'deepseek-v4-pro' }" />
          <button class="capsule-opt" :class="{ active: aiModel === 'deepseek-v4-flash' }" @click="aiModel = 'deepseek-v4-flash'">Flash</button>
          <button class="capsule-opt" :class="{ active: aiModel === 'deepseek-v4-pro' }" @click="aiModel = 'deepseek-v4-pro'">Pro</button>
        </div>
        <span v-if="quotaInfo.quotaRemaining >= 0" class="quota-badge" :class="{ 'quota-low': quotaInfo.quotaRemaining <= 2 && !quotaInfo.unlimited }">
          {{ quotaInfo.unlimited ? '无限次' : `剩余 ${quotaInfo.quotaRemaining}/${quotaInfo.dailyQuota} 次` }}
        </span>
        <div v-if="hasDocument && resultText" class="export-dropdown">
          <button class="btn btn-outline" @click="showExport = !showExport">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            导出文档
          </button>
          <div v-if="showExport" class="export-menu">
            <div class="export-item" @click="exportDoc('preserve')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              保留原格式导出<span class="export-badge">推荐</span>
            </div>
            <div class="export-item" @click="exportDoc('standard')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              标准 Word 导出
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Toolbar -->
    <div class="pro-toolbar">
      <div class="tabs-container">
        <div class="tab-indicator" ref="tabIndicator" />
        <div v-for="t in modeTabs" :key="t.value"
          class="tab-btn" :class="{ active: rewriteMode === t.value }"
          :ref="el => tabRefs[t.value] = el as any"
          @click="setModeTab(t.value)">{{ t.label }}</div>
      </div>
      <div class="toolbar-spacer" />
      <button v-if="hasDocument" class="btn btn-outline btn-link" @click="showAdvanced = !showAdvanced">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        高级选项
        <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="1.5" :style="{ transform: showAdvanced ? 'rotate(180deg)' : 'rotate(0)' }"><polyline points="2 3 5 6 8 3"/></svg>
      </button>
      <button class="btn btn-outline btn-upload" @click="triggerUpload">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        上传文档
      </button>
      <input ref="fileInput" type="file" accept=".docx,.pdf" style="display:none" @change="handleFileUpload" />
      <button v-if="hasDocument && scanDone" class="btn btn-outline" style="border-color:transparent;box-shadow:none;" @click="rescan">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        重新扫描
      </button>
      <button class="btn btn-solid accent" @click="startRewrite" :disabled="isStreaming || !scanDone">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        {{ isStreaming ? '改写中...' : scanDone && activeFlaggedCount > 0 ? `标注驱动净化 (${activeFlaggedCount}处)` : '一键净化全文' }}
      </button>
    </div>

    <!-- Advanced Options -->
    <div v-if="showAdvanced" class="advanced-panel">
      <div class="advanced-grid">
        <div class="advanced-item advanced-item--grow">
          <label class="advanced-label">主题</label>
          <input v-model="topic" type="text" class="advanced-input" placeholder="如：深度学习模型优化" />
        </div>
        <div class="advanced-item advanced-item--grow">
          <label class="advanced-label">补充说明</label>
          <input v-model="notes" type="text" class="advanced-input" placeholder="如：保持专业术语不变，重点去模板化表达" />
        </div>
        <div class="advanced-item">
          <label class="advanced-label">导入检测报告</label>
          <button class="btn btn-outline btn-upload" @click="triggerReportUpload" :disabled="!hasDocument || reportLoading" style="font-size:12px;padding:6px 12px;">
            <svg v-if="reportLoading" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin-icon"><circle cx="12" cy="12" r="10" stroke-dasharray="31.4" stroke-dashoffset="10"/></svg>
            <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            {{ reportLoading ? '解析中…' : reportUploaded ? '报告已导入 (' + reportAnnotationCount + '处标注)' : '上传 AIGC 报告' }}
          </button>
          <input ref="reportFileInput" type="file" accept=".docx,.pdf" style="display:none" @change="handleReportUpload" />
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-if="!hasDocument" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1" opacity="0.3"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
      </div>
      <p class="empty-text">上传文档开始扫描 AI 写作痕迹</p>
      <p class="empty-sub">支持 .docx / .pdf，自动检测模板表达、句式均匀度、连接词密度</p>
      <textarea v-model="pasteText" class="paste-area" placeholder="或直接粘贴论文正文..." rows="5" />
      <button v-if="pasteText.trim()" class="btn btn-solid accent" @click="usePastedText">使用粘贴文本</button>
    </div>

    <!-- Step flow -->
    <div v-if="hasDocument" class="step-flow">
      <div class="step-row">
        <div class="step" :class="{ active: hasDocument, done: true }"><span class="step-num">1</span><span class="step-label">上传文档</span></div>
        <div class="step-line" :class="{ active: scanDone }" />
        <div class="step" :class="{ active: isScanning, done: scanDone }">
          <span class="step-num"><span v-if="isScanning" class="step-spinner" /><template v-else>2</template></span>
          <span class="step-label">{{ isScanning ? '扫描中…' : 'AI 痕迹扫描' }}</span>
        </div>
        <div class="step-line" />
        <div class="step" :class="{ active: isStreaming }"><span class="step-num">3</span><span class="step-label">{{ isStreaming ? '改写中…' : '一键净化' }}</span></div>
      </div>
    </div>

    <!-- Workspace: Bento Layout -->
    <div v-if="hasDocument" class="workspace split-bento">
      <!-- LEFT: Document / Result toggle -->
      <div class="editor-pane left">
        <div class="pane-header">
          <div style="display:flex;gap:0;">
            <button :class="['pane-tab', { active: showResult }]" @click="showResult = true">改写结果</button>
            <button :class="['pane-tab', { active: !showResult }]" @click="showResult = false">文档扫描</button>
          </div>
          <span class="pane-action" v-if="!showResult && scanDone">共发现 {{ activeFlaggedCount }} 处风险痕迹</span>
          <span class="pane-action" v-else-if="showResult && resultText">改写完成 · {{ resultCharCount }} 字</span>
        </div>

        <!-- SCAN VIEW -->
        <div class="editor-content" v-if="!showResult && scanDone">
          <p v-for="(seg, si) in shownSegments" :key="si" style="margin-bottom:16px;">
            <template v-for="(part, pi) in seg" :key="pi">
              <span v-if="part.type === 'text'">{{ part.text }}</span>
              <span v-else-if="part.type === 'resolved'" class="hl-resolved">{{ part.text }}</span>
              <span v-else
                class="hl-item" :class="[part.risk === 'critical' || part.risk === 'high' ? 'hl-danger' : 'hl-warning', { active: activeHighlight === part.id }]"
                @click="selectHighlight(part)"
              >{{ part.text }}</span>
            </template>
          </p>
        </div>
        <div class="editor-content placeholder" v-else-if="!showResult && !scanDone">
          <p style="text-align:center;color:var(--text-muted);margin-top:60px;">
            {{ isScanning ? '正在扫描 AI 写作痕迹...' : '文档已加载，扫描将自动开始' }}
          </p>
        </div>

        <!-- REWRITE RESULT VIEW (format-aware) -->
        <div class="editor-content" v-if="showResult">
          <div v-if="isStreaming" class="streaming-text">
            <template v-for="(line, i) in streamingLines" :key="'s-'+i"><p v-if="line" class="streaming-line">{{ line }}</p></template>
            <span class="cursor-blink" />
          </div>
          <template v-else-if="resultText && formatResultParagraphs.length">
            <div v-for="p in formatResultParagraphs" :key="'rr-'+p.index">
              <div v-if="p.isHeading" class="fp-heading" :style="{ fontSize: p.fontSize + 'px', fontFamily: p.fontFamily, fontWeight: '700' }">{{ p.text }}</div>
              <div v-else class="fp-body" :style="{ fontFamily: p.fontFamily, fontSize: p.fontSize + 'pt', fontWeight: p.bold ? '700' : '400', fontStyle: p.italic ? 'italic' : 'normal', textAlign: p.alignment || 'left' }">{{ p.text }}</div>
            </div>
          </template>
          <template v-else-if="resultText">
            <p v-for="(p, i) in resultParagraphs" :key="i" style="margin-bottom:12px;">{{ p }}</p>
          </template>
          <div v-else class="result-placeholder">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
            <span>点击「一键净化全文」开始改写</span>
          </div>
        </div>
      </div>

      <!-- RIGHT: Bento Sidebar -->
      <div class="bento-sidebar">
        <!-- Risk Score Ring -->
        <div class="bento-card" v-if="scanDone">
          <div class="risk-hero">
            <div class="score-ring">
              <svg viewBox="0 0 80 80">
                <circle class="ring-bg" cx="40" cy="40" r="34"/>
                <circle class="ring-fg" cx="40" cy="40" r="34"
                  :stroke="riskColor" stroke-dasharray="213.6"
                  :stroke-dashoffset="213.6 * (1 - scanScore / 100)"
                  style="transition: stroke-dashoffset 1.5s var(--spring-bounce);"
                />
              </svg>
              <div class="score-num" :style="{ color: riskColor }">{{ scanScore }}<span>%</span></div>
            </div>
            <div>
              <div class="risk-title">{{ scanResult.riskLevel }}</div>
              <div class="risk-desc">{{ riskSummary }}</div>
            </div>
          </div>
          <!-- Severity bars — unique sentence count per severity -->
          <div class="severity-bars">
            <div v-if="severitySentenceCounts.critical" class="sev-bar sev-crit" :style="{flex: sevFlex(severitySentenceCounts.critical)}">致命 {{ severitySentenceCounts.critical }}</div>
            <div v-if="severitySentenceCounts.high" class="sev-bar sev-high" :style="{flex: sevFlex(severitySentenceCounts.high)}">高危 {{ severitySentenceCounts.high }}</div>
            <div v-if="severitySentenceCounts.medium" class="sev-bar sev-med" :style="{flex: sevFlex(severitySentenceCounts.medium)}">中 {{ severitySentenceCounts.medium }}</div>
            <div v-if="severitySentenceCounts.style" class="sev-bar sev-style" :style="{flex: sevFlex(severitySentenceCounts.style)}">风格 {{ severitySentenceCounts.style }}</div>
          </div>
          <div style="margin-top:6px;font-size:10px;color:var(--text-light);">字符熵 {{ scanResult.entropy || '—' }}</div>
        </div>

        <div v-if="!scanDone && !isScanning" class="bento-card">
          <p style="font-size:13px;color:var(--text-muted);text-align:center;padding:40px 0;">上传文档后自动扫描 AI 痕迹</p>
        </div>

        <!-- Interactive Insight Panel -->
        <div v-if="activeInsight" class="bento-card insight-panel" :class="{ pulse: insightPulse }" :style="{ borderLeftColor: activeInsight.risk === 'critical' || activeInsight.risk === 'high' ? 'var(--color-danger)' : 'var(--color-warning)' }">
          <div class="insight-header" :style="{ color: activeInsight.risk === 'critical' || activeInsight.risk === 'high' ? 'var(--color-danger)' : 'var(--color-warning)' }">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>{{ activeInsight.risk === 'critical' || activeInsight.risk === 'high' ? 'AI 标志性问题' : '表达优化建议' }}</span>
          </div>
          <div class="insight-quote">"{{ activeInsight.text.length > 60 ? activeInsight.text.slice(0,60)+'…' : activeInsight.text }}"</div>
          <div class="insight-issue">
            <strong>{{ activeInsight.risk === 'critical' || activeInsight.risk === 'high' ? '高危风险分析' : '格式/表达优化' }}</strong>
            <span>{{ activeInsight.reason }}</span>
          </div>
          <div class="insight-suggestion">
            <div class="insight-sugg-label">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              <span>AI 建议重新表达</span>
            </div>
            <div class="insight-textarea-wrap">
              <textarea v-model="editableSuggestion" class="insight-sugg-textarea" rows="5"
                placeholder="编辑建议文本，或点击右下角 ⚡ 让 AI 精修..." />
              <button class="btn btn-ai-send" @click="sendToAiRewrite" :disabled="sentenceStreaming"
                title="AI 精修此句">
                <svg v-if="!sentenceStreaming" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                <span v-else class="btn-ai-spinner" />
              </button>
            </div>
          </div>

          <!-- AI rewrite sub-flow result -->
          <div v-if="sentenceStreaming" class="insight-stream">
            <span class="streaming-dot" /> AI 正在重写此句...
          </div>
          <div v-if="sentenceRewriteResult && !sentenceStreaming" class="insight-suggestion" style="margin-top:10px;">
            <div class="insight-sugg-label" style="color:var(--color-success);">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              <span>AI 精修结果</span>
            </div>
            <textarea v-model="sentenceRewriteResult" class="insight-sugg-textarea insight-sugg-result" rows="4" placeholder="可继续编辑..." />
          </div>

          <div class="insight-actions">
            <button class="btn btn-outline" style="flex:1;" @click="dismissInsight">忽略</button>
            <button class="btn btn-solid accent" style="flex:1;" @click="applyInsight" :disabled="sentenceStreaming">应用替换</button>
          </div>
        </div>

        <div v-if="!activeInsight && scanDone" class="bento-card">
          <p style="font-size:13px;color:var(--text-light);text-align:center;padding:20px 0;">点击左侧高亮文本查看详情</p>
        </div>
      </div>
    </div>

    <p v-if="error" class="error-toast" @click="error = ''">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useQuota } from '@/composables/useQuota'

const router = useRouter()
const { fetchQuota, checkQuota } = useQuota()

const aiModel = ref('deepseek-v4-flash')
const rewriteMode = ref('light')
const quotaInfo = ref<{ unlimited: boolean; dailyQuota: number; quotaUsed: number; quotaRemaining: number }>({ unlimited: false, dailyQuota: 10, quotaUsed: 0, quotaRemaining: 10 })
const topic = ref('')
const notes = ref('')
const showAdvanced = ref(false)
const showExport = ref(false)
const isStreaming = ref(false)
const isScanning = ref(false)
const scanDone = ref(false)
const hasDocument = ref(false)
const error = ref('')
const storedFile = ref<File | null>(null)
const sourceText = ref('')
const resultText = ref('')
const paragraphData = ref<any[]>([])
const rewrittenParagraphs = ref<Map<number, string>>(new Map())
const scanScore = ref(0)
const scanResult = ref<any>({ riskLevel: '低风险', criticalCount: 0, highCount: 0, mediumCount: 0, styleCount: 0, entropy: 0, flaggedSentences: [], aiSignals: [], humanSignals: [] })
let abortController: AbortController | null = null

// Insight panel state
interface HighlightPart { type: string; text: string; id?: string; risk?: string; reason?: string; suggestion?: string }
const segmentedText = ref<HighlightPart[][]>([])
const activeHighlight = ref<string | null>(null)
const activeInsight = ref<HighlightPart | null>(null)
const insightPulse = ref(false)
const showResult = ref(false)
const resolvedIds = ref<Set<string>>(new Set())

const activeFlaggedCount = computed(() =>
  severitySentenceCounts.value.critical + severitySentenceCounts.value.high +
  severitySentenceCounts.value.medium + severitySentenceCounts.value.style
)
const shownSegments = computed(() => {
  return segmentedText.value.map(seg =>
    seg.map(part => {
      if (part.type === 'highlight' && resolvedIds.value.has(part.id!)) {
        return { ...part, type: 'resolved' }
      }
      return part
    })
  )
})
const flaggedTotal = computed(() => scanResult.value.flaggedSentences?.length || 0)

const streamingLines = computed(() => resultText.value ? resultText.value.split('\n').filter(l => l !== undefined) : [''])
const resultCharCount = computed(() => resultText.value.replace(/\s/g, '').length)
const resultParagraphs = computed(() => resultText.value ? resultText.value.split(/\n{2,}/).filter(p => p.trim()) : [])

interface FormatPara { index: number; text: string; isHeading: boolean; fontFamily: string; fontSize: number; bold: boolean; italic: boolean; alignment: string }
const formatResultParagraphs = computed<FormatPara[]>(() => {
  if (!paragraphData.value.length) return []
  return paragraphData.value.map((p: any) => {
    const isHeading = !!(p.styleId && /heading/i.test(p.styleId))
    return {
      index: p.index,
      text: rewrittenParagraphs.value.get(p.index) ?? p.text ?? '',
      isHeading,
      fontFamily: isHeading ? 'Inter, PingFang SC, sans-serif' : (p.fontFamily || 'Georgia, Noto Serif SC, serif'),
      fontSize: isHeading ? (p.styleId && /1/.test(p.styleId) ? 22 : p.styleId && /2/.test(p.styleId) ? 18 : 15) : (p.fontSize || 12),
      bold: p.bold || isHeading,
      italic: p.italic || false,
      alignment: p.alignment || 'left',
    }
  })
})

const riskSummary = computed(() => {
  const s = scanScore.value
  if (s >= 50) return '检测到大量典型大模型套话模板与规律性长短句。'
  if (s >= 25) return '检测到部分可优化表达，建议针对性修改。'
  return '文本自然度良好，仅发现少量风格问题。'
})

// Mode tabs
const modeTabs = [
  { value: 'light', label: '轻度去痕' },
  { value: 'deep', label: '深度重构' },
  { value: 'academic', label: '学术拟合' },
]
const tabRefs: Record<string, HTMLElement | null> = {}
const tabIndicator = ref<HTMLElement | null>(null)
function setModeTab(v: string) { rewriteMode.value = v; nextTick(() => moveIndicator(v)) }
function moveIndicator(v?: string) {
  const val = v || rewriteMode.value
  const indicator = tabIndicator.value; const tab = tabRefs[val]
  if (!indicator || !tab) return
  const p = indicator.parentElement; if (!p) return
  const pr = p.getBoundingClientRect(); const tr = tab.getBoundingClientRect()
  indicator.style.width = `${tr.width}px`; indicator.style.transition = 'all 0.4s var(--spring-bounce)'
  indicator.style.transform = `translateX(${tr.left - pr.left}px)`
}
onMounted(() => nextTick(() => moveIndicator()))
onMounted(async () => { const q = await fetchQuota(); if (q) quotaInfo.value = q })
let _resizeObs: ResizeObserver | null = null
onMounted(() => {
  _resizeObs = new ResizeObserver(() => moveIndicator())
  const parent = tabIndicator.value?.parentElement
  if (parent) _resizeObs.observe(parent)
})
onUnmounted(() => _resizeObs?.disconnect())
watch(rewriteMode, () => nextTick(() => moveIndicator()))

function sevFlex(n: number): number { return Math.min(n, 6) + 1 }
const severitySentenceCounts = computed(() => {
  const counts = { critical: 0, high: 0, medium: 0, style: 0 }
  for (const seg of shownSegments.value) {
    for (const part of seg) {
      if (part.type === 'highlight') {
        if (part.risk === 'critical') counts.critical++
        else if (part.risk === 'high') counts.high++
        else if (part.risk === 'medium') counts.medium++
        else counts.style++
      }
    }
  }
  return counts
})

const riskLevelKey = computed(() => { const l = scanResult.value.riskLevel; if (l.includes('高')) return 'high'; if (l.includes('中')) return 'mid'; return 'low' })
const riskColor = computed(() => ({ high: '#EF4444', mid: '#F59E0B', low: '#22C55E' }[riskLevelKey.value] || '#888'))

// Upload + Scan
const pasteText = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
function triggerUpload() { fileInput.value?.click() }

// Report upload
const reportFileInput = ref<HTMLInputElement | null>(null)
const reportUploaded = ref(false)
const reportLoading = ref(false)
const reportAnnotations = ref<any[]>([])
const reportAnnotationCount = computed(() => reportAnnotations.value.length)
function triggerReportUpload() { if (hasDocument.value) reportFileInput.value?.click() }

async function handleReportUpload(e: Event) {
  const reportFile = (e.target as HTMLInputElement).files?.[0]; if (!reportFile) return
  const token = localStorage.getItem('token') || ''; const fd = new FormData(); fd.append('file', reportFile)
  if (sourceText.value) fd.append('sourceText', sourceText.value)
  reportLoading.value = true
  try {
    const res = await fetch('/api/paper/report-analyze', { method:'POST', headers:{Authorization:`Bearer ${token}`}, body:fd })
    const data = await res.json(); if (!res.ok) throw new Error(data.error||'解析失败')
    reportAnnotations.value = data.annotations || []
    if (reportAnnotations.value.length === 0) {
      const isPdf = reportFile.name.toLowerCase().endsWith('.pdf')
      error.value = isPdf
        ? '报告解析无标注：该PDF可能为图片版，请从检测平台导出Word格式(.docx)后重新上传'
        : '报告解析无标注：未检测到高亮/批注标记，请确认报告格式'
      return
    }
    reportUploaded.value = true
    if (data.fallbackUsed) {
      error.value = '报告为图片版PDF，已通过云端AI解析完成识别'
      setTimeout(() => { if (error.value === '报告为图片版PDF，已通过云端AI解析完成识别') error.value = '' }, 4000)
    }
    buildSegments()
  } catch(e:any) { error.value = '报告解析失败: '+(e.message||String(e)) }
  finally { reportLoading.value = false; (e.target as HTMLInputElement).value = '' }
}

function fallbackParagraphs(text: string): any[] {
  if (!text?.trim()) return []
  return text.split(/\n{2,}/).filter(p => p.trim().length > 3).map((t, i) => ({ index: i, text: t.trim() }))
}

function usePastedText() {
  const text = pasteText.value.trim(); if (!text) return
  sourceText.value = text
  paragraphData.value = fallbackParagraphs(text)
  rewrittenParagraphs.value = new Map(); for (const p of paragraphData.value) rewrittenParagraphs.value.set(p.index, p.text)
  hasDocument.value = true; resultText.value = ''; scanDone.value = false; runScan(text)
}

async function handleFileUpload(e: Event) {
  const target = e.target as HTMLInputElement
  if (!target || !target.files || target.files.length === 0) return
  const uploadedFile = target.files[0]
  storedFile.value = uploadedFile
  target.value = ''
  resetInsight(); showResult.value = false; scanDone.value = false
  segmentedText.value = []; reportUploaded.value = false; reportAnnotations.value = []
  const token = localStorage.getItem('token') || ''; const fd = new FormData(); fd.append('file', uploadedFile)
  try {
    const res = await fetch('/api/paper/upload', { method:'POST', headers:{Authorization:`Bearer ${token}`}, body:fd })
    const data = await res.json(); if (!res.ok) throw new Error(data.error||'上传失败')
    if (!data.fullText || data.fullText.trim().length < 10) {
      error.value = '文档解析失败：未提取到有效文本内容。请检查文档是否为扫描版图片，或尝试另存为标准 .docx 格式。'
      return
    }
    sourceText.value = data.fullText
    paragraphData.value = (data.paragraphs?.length ? data.paragraphs : fallbackParagraphs(data.fullText))
    rewrittenParagraphs.value = new Map(); resultText.value = ''; hasDocument.value = true
    for (const p of paragraphData.value) rewrittenParagraphs.value.set(p.index, p.text||'')
    await runScan(data.fullText)
  } catch(e:any) { error.value = '解析失败: '+(e.message||String(e)) }
}

async function runScan(text: string) {
  isScanning.value = true
  try {
    const token = localStorage.getItem('token')||''
    const res = await fetch('/api/ai-reduce/scan', { method:'POST', headers:{'Content-Type':'application/json', Authorization:`Bearer ${token}`}, body:JSON.stringify({text}) })
    const data = await res.json()
    if (data.result) { scanResult.value = data.result; scanScore.value = data.result.score||0 }
    scanDone.value = true
    buildSegments()
  } catch(e:any) { error.value = '扫描失败: '+(e.message||String(e)) } finally { isScanning.value = false }
}

async function rescan() { if (sourceText.value) { scanDone.value = false; activeInsight.value = null; await runScan(sourceText.value) } }

// Build segmented text with highlighted spans
function escapeHtml(s: string) { return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') }

function buildSegments() {
  const text = sourceText.value
  const flagged = scanResult.value.flaggedSentences || []

  // Merge scan results with report annotations
  const allFlagged = [...flagged]
  for (const ann of reportAnnotations.value) {
    const matchedText = ann.matchedSourceText || ann.text
    if (matchedText && matchedText.length > 3) {
      allFlagged.push({
        text: matchedText,
        reason: `检测报告标注 (${ann.riskLevel})`,
        suggestion: `根据检测报告${ann.riskLevel === 'high' ? '高' : '中'}风险标注，建议重写此段落以降低检测概率。`
      })
    }
  }

  if (!allFlagged.length) { segmentedText.value = []; return }

  const paragraphs = text.split('\n').filter(p => p.trim())
  const result: HighlightPart[][] = []
  let id = 0

  for (const para of paragraphs) {
    const pSegs: HighlightPart[] = []
    let remaining = para
    for (const fs of allFlagged) {
      const flagText = fs.text || ''
      if (flagText.length < 4) continue
      // 先精确匹配，失败则用包含匹配（处理截断/差异）
      let idx = remaining.indexOf(flagText)
      if (idx < 0 && flagText.length > 8) {
        // 用 flagText 的前半段做包含搜索
        const half = flagText.substring(0, Math.floor(flagText.length / 2))
        idx = remaining.indexOf(half)
      }
      if (idx < 0 && flagText.length > 6) {
        // 用前 8 个字符做包含搜索
        const prefix = flagText.substring(0, Math.min(8, flagText.length))
        idx = remaining.indexOf(prefix)
      }
      if (idx >= 0) {
        if (idx > 0) pSegs.push({ type: 'text', text: remaining.substring(0, idx) })
        // 用实际匹配的长度（不一定是 flagText 的长度）
        const matchLen = Math.min(flagText.length, remaining.length - idx)
        const actualMatch = remaining.substring(idx, idx + matchLen)
        const risk = fs.reason?.includes('机械连接词') || fs.reason?.includes('空洞宏大词') || fs.reason?.includes('模糊匹配') ? 'critical' :
                     fs.reason?.includes('模板句式') || fs.reason?.includes('AI高频词') || fs.reason?.includes('句式') ? 'high' : 'medium'
        pSegs.push({ type: 'highlight', text: actualMatch, id: `hl-${id}`, risk, reason: fs.reason, suggestion: fs.suggestion })
        remaining = remaining.substring(idx + matchLen)
        id++
      }
    }
    if (remaining.trim()) pSegs.push({ type: 'text', text: remaining })
    if (pSegs.length) result.push(pSegs)
  }
  segmentedText.value = result
}

// Interactive highlight → insight panel sync
// Sentence-level AI rewrite state
const editableSuggestion = ref('')
const sentenceStreaming = ref(false)
const sentenceRewriteResult = ref('')
let sentenceAbort: AbortController | null = null

function selectHighlight(part: HighlightPart) {
  if (part.type === 'resolved') return
  activeHighlight.value = part.id || null
  activeInsight.value = { ...part }
  editableSuggestion.value = part.suggestion || ''
  sentenceRewriteResult.value = ''
  insightPulse.value = false
  nextTick(() => { insightPulse.value = true })
}
function dismissInsight() {
  if (activeInsight.value?.id) resolvedIds.value.add(activeInsight.value.id)
  sentenceAbort?.abort()
  resetInsight()
}
function resetInsight() {
  activeInsight.value = null; activeHighlight.value = null
  editableSuggestion.value = ''; sentenceRewriteResult.value = ''; sentenceStreaming.value = false
}

async function sendToAiRewrite() {
  const insight = activeInsight.value
  if (!insight || !insight.text || sentenceStreaming.value) return
  sentenceStreaming.value = true; sentenceRewriteResult.value = ''
  sentenceAbort = new AbortController()
  try {
    const token = localStorage.getItem('token') || ''
    const prompt = `改写以下学术文本片段，去除AI写作痕迹和模板化表达，使表达更自然更像人类学者写作。保持原意、数据和术语不变。只输出改写后的文本，不要任何解释。\n\n原文："${insight.text}"\n\n改写：`
    const res = await fetch('/api/polish/run', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({ text: prompt, taskType: '自定义段落', polishType: 'full', topic: '', notes: '这是单句去AI化改写，保持原意和术语不变，让表达更像人类学者自然写作' }),
      signal: sentenceAbort.signal,
    })
    if (!res.ok) throw new Error(`请求失败: ${res.status}`)
    const reader = res.body?.getReader()
    if (!reader) { sentenceStreaming.value = false; return }
    const decoder = new TextDecoder(); let buffer = '', raw = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }); const lines = buffer.split('\n'); buffer = lines.pop() || ''
      for (const line of lines) {
        if (line.startsWith('event:') || line.startsWith('id:') || line.startsWith('retry:')) continue
        if (line.startsWith('data:')) { const d = line.slice(5).trim(); if (d === 'finish' || d === '[DONE]') break; if (d.startsWith('{') || d.startsWith('"')) continue; raw += d; sentenceRewriteResult.value = raw; editableSuggestion.value = raw }
      }
    }
  } catch (e: unknown) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = 'AI 重写失败: ' + (e instanceof Error ? e.message : String(e))
  } finally { sentenceStreaming.value = false }
}

function applyInsight() {
  const insight = activeInsight.value
  if (!insight || !insight.text) return
  const replacement = sentenceRewriteResult.value || editableSuggestion.value || insight.suggestion || insight.text
  const escaped = insight.text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const pattern = new RegExp('(^|[。！？\\n])' + escaped + '(?=[。！？\\n]|$)', 'g')
  sourceText.value = sourceText.value.replace(pattern, (_full: string, prefix: string) => {
    return prefix + replacement
  })
  if (insight.id) resolvedIds.value.add(insight.id)
  resetInsight()
  buildSegments()
}

// SSE Rewrite
async function startRewrite() {
  if (!hasDocument.value||isStreaming.value||!scanDone.value) return
  const needed = aiModel.value.includes('pro') ? 2 : 1
  const qr = checkQuota(needed, `今日免费次数不足（需 ${needed} 次），请配置 API Key 后继续使用`)
  if (!qr.ok) { error.value = qr.msg || '配额不足'; return }

  const taggedText = paragraphData.value.length>0 ? paragraphData.value.map((p:any)=>`[P${p.index}] ${p.text||''}`).join('\n\n') : `[P0] ${sourceText.value}`
  resultText.value = ''; error.value = ''; isStreaming.value = true; abortController = new AbortController()
  try {
    const token = localStorage.getItem('token')||''
    const flagged = (scanResult.value.flaggedSentences || []).map((fs: any) => ({ text: fs.text, reason: fs.reason, suggestion: fs.suggestion }))
    const res = await fetch('/api/ai-reduce/rewrite', { method:'POST', headers:{'Content-Type':'application/json', Authorization:`Bearer ${token}`}, body:JSON.stringify({text:taggedText, mode:rewriteMode.value, model:aiModel.value, flaggedSentences: flagged}), signal:abortController.signal })
    if (!res.ok) {
      if (res.status === 429) { const d = await res.json().catch(() => ({ error: '配额已用完' })); throw new Error(d.error || '今日免费次数已用完') }
      throw new Error(`请求失败: ${res.status}`)
    }
    if (!res.ok) throw new Error(`请求失败: ${res.status}`)
    const reader = res.body?.getReader(); if (!reader) { isStreaming.value=false; return }
    const decoder = new TextDecoder(); let buffer='', raw=''
    while (true) {
      const {done,value} = await reader.read(); if (done) break
      buffer += decoder.decode(value, {stream:true}); const lines = buffer.split('\n'); buffer = lines.pop()||''
      for (const line of lines) {
        if (line.startsWith('event:')||line.startsWith('id:')||line.startsWith('retry:')) continue
        if (line.startsWith('data:')) { const d=line.slice(5).trim(); if (d==='finish'||d==='[DONE]') break; if (d.startsWith('{')||d.startsWith('"')) continue; raw+=d; resultText.value=raw }
      }
    }
    if (raw && paragraphData.value.length>0) { rewrittenParagraphs.value = parseMarked(raw) }
    showResult.value = true
    fetchQuota().then(q => { if (q) quotaInfo.value = q })
  } catch(e:unknown) { if (e instanceof DOMException && e.name==='AbortError') return; error.value = '请求失败: '+(e instanceof Error?e.message:String(e)) }
  finally { isStreaming.value = false }
}

function parseMarked(resp: string): Map<number,string> {
  const r = new Map<number,string>(); const re = /\[P(\d+)\]\s*([\s\S]*?)(?=\[P\d+\]|$)/g; let m
  while ((m=re.exec(resp))!==null) r.set(parseInt(m[1]), m[2].trim())
  if (r.size===0&&resp.trim()) { const parts=resp.split(/\n{2,}/).filter(p=>p.trim()); const paras=paragraphData.value; if (paras.length>0&&parts.length>0) { for (let i=0;i<Math.min(paras.length,parts.length);i++) r.set(paras[i].index,parts[i].trim()) } else r.set(0,resp.trim()) }
  return r
}

async function exportDoc(mode: string) {
  showExport.value=false; const token=localStorage.getItem('token')||''
  const paras=paragraphData.value.map((p:any)=>({index:p.index,text:rewrittenParagraphs.value.get(p.index)??p.text??''}))
  if (mode === 'preserve' && storedFile.value) {
    // 格式保留导出：将原始文件 + 段落映射一起提交，服务器处理后立即丢弃
    const fd = new FormData()
    fd.append('file', storedFile.value)
    fd.append('mappings', JSON.stringify({ fileName: 'ai-reduced', paragraphs: paras }))
    try {
      const res = await fetch('/api/paper-export/preserve-format', { method:'POST', headers:{Authorization:`Bearer ${token}`}, body:fd })
      if (res.ok) { const blob=await res.blob(); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='ai-reduced.docx'; a.click(); URL.revokeObjectURL(url) }
    } catch(e) { console.error('Export failed:',e) }
  } else {
    // 标准导出或没有原始文件：纯文本生成
    try {
      const res = await fetch('/api/paper-export/standard', { method:'POST', headers:{'Content-Type':'application/json',Authorization:`Bearer ${token}`}, body:JSON.stringify({ fileName:'ai-reduced', paragraphs:paras }) })
      if (res.ok) { const blob=await res.blob(); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='ai-reduced.docx'; a.click(); URL.revokeObjectURL(url) }
    } catch(e) { console.error('Export failed:',e) }
  }
}
</script>

<style scoped>
.app-shell {
  --accent: #D9750A; --accent-hover: #C26300; --color-success: #22C55E; --color-danger: #EF4444; --color-warning: #F59E0B;
  --border-light: rgba(0,0,0,0.06); --border-medium: rgba(0,0,0,0.12);
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.04); --shadow-md: 0 12px 32px rgba(0,0,0,0.08);
  --spring: cubic-bezier(0.25,1,0.4,1); --spring-bounce: cubic-bezier(0.34,1.56,0.64,1);
  --bg-canvas: #F3EFE8; --bg-paper: #FDFCFB; --bg-surface: #F5F4F1;
  --text-main: #141413; --text-muted: #555; --text-light: #999;
  max-width:1280px; height:calc(100vh - 48px); margin:24px auto; background:var(--bg-paper);
  border-radius:20px; box-shadow:0 24px 48px rgba(0,0,0,0.08),0 0 0 1px var(--border-light);
  display:flex; flex-direction:column; overflow:hidden; font-family:'Inter','PingFang SC',sans-serif;
}
.app-header { display:flex; align-items:center; justify-content:space-between; padding:16px 24px; border-bottom:1px solid var(--border-light); background:var(--bg-paper); z-index:20; flex-shrink:0; }
.app-brand { display:flex; align-items:center; gap:12px; }
.brand-name { font-family:Georgia,serif; font-size:18px; font-weight:600; }
.brand-tag { font-size:10px; font-weight:600; color:var(--accent); background:rgba(217,117,10,0.08); padding:4px 10px; border-radius:100px; letter-spacing:1px; }
.btn { display:inline-flex; align-items:center; justify-content:center; gap:8px; padding:8px 16px; border-radius:10px; font-size:13px; font-weight:600; cursor:pointer; transition:all 0.2s var(--spring); border:1px solid transparent; font-family:inherit; }
.btn:active { transform:scale(0.96); }
.btn-icon { width:36px; height:36px; padding:0; border-radius:10px; border-color:var(--border-medium); background:var(--bg-paper); color:var(--text-muted); }
.btn-icon:hover { background:var(--bg-surface); color:var(--text-main); }
.btn-outline { background:var(--bg-paper); border-color:var(--border-medium); color:var(--text-main); }
.btn-outline:hover { background:var(--bg-surface); }
.btn-solid { background:#141413; color:#fff; box-shadow:var(--shadow-sm); border:none; }
.btn-solid:hover { opacity:0.9; }
.btn-solid.accent { background:var(--accent); }
.btn-solid:disabled { opacity:0.4; cursor:not-allowed; }
.btn-link { border-color:transparent; color:var(--text-muted); font-weight:500; gap:5px; }
.btn-link:hover { color:var(--accent); background:transparent; }
.btn-upload { font-weight:600; color:var(--accent); border-color:rgba(217,117,10,0.25); }
.btn-upload:hover { background:rgba(217,117,10,0.06); border-color:var(--accent); color:var(--accent-hover); }
.btn-switch { font-size:11px; padding:5px 12px; color:var(--text-muted); margin-left:8px; }
.btn-switch:hover { color:var(--accent); border-color:var(--accent); }
.export-dropdown { position:relative; }
.export-menu { position:absolute; top:calc(100% + 6px); right:0; background:var(--bg-paper); border:1px solid var(--border-medium); border-radius:12px; box-shadow:var(--shadow-md); min-width:200px; z-index:100; overflow:hidden; }
.export-item { padding:10px 14px; font-size:12px; cursor:pointer; display:flex; align-items:center; gap:8px; border-bottom:1px solid var(--border-light); transition:background 0.15s; }
.export-item:last-child { border-bottom:none; }
.export-item:hover { background:var(--bg-surface); }
.export-badge { font-size:10px; padding:2px 8px; border-radius:100px; background:rgba(217,117,10,0.08); color:var(--accent); font-weight:600; margin-left:auto; }

.pro-toolbar { display:flex; align-items:center; gap:14px; padding:12px 24px; background:var(--bg-surface); border-bottom:1px solid var(--border-light); z-index:10; flex-shrink:0; }
.toolbar-spacer { flex:1; }
.tabs-container { position:relative; display:flex; align-items:center; background:rgba(0,0,0,0.04); padding:4px; border-radius:10px; }
.tab-btn { position:relative; z-index:2; padding:6px 16px; font-size:13px; font-weight:500; color:var(--text-muted); cursor:pointer; user-select:none; transition:color 0.3s; }
.tab-btn.active { color:var(--text-main); font-weight:600; }
.tab-indicator { position:absolute; top:4px; bottom:4px; left:0; background:#fff; border-radius:6px; z-index:1; box-shadow:0 2px 8px rgba(0,0,0,0.06); transition:all 0.4s var(--spring-bounce); }

.advanced-panel { padding:14px 24px; background:var(--bg-surface); border-bottom:1px solid var(--border-light); flex-shrink:0; animation:slideDown 0.25s var(--spring); }
@keyframes slideDown { from{opacity:0;transform:translateY(-8px)} to{opacity:1;transform:translateY(0)} }
.advanced-grid { display:flex; gap:14px; align-items:flex-end; }
.advanced-item { display:flex; flex-direction:column; gap:5px; }
.advanced-item--grow { flex:1; }
.advanced-label { font-size:11px; font-weight:600; color:var(--text-light); text-transform:uppercase; letter-spacing:0.5px; }
.advanced-input { padding:7px 10px; border-radius:8px; border:1px solid var(--border-medium); background:#fff; font-size:13px; font-family:inherit; color:var(--text-main); outline:none; }
.advanced-input::placeholder { color:var(--text-light); }
.advanced-input:focus { border-color:var(--accent); }

.empty-state { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:16px; background:var(--bg-canvas); }
.empty-icon { width:80px; height:80px; border-radius:20px; background:rgba(217,117,10,0.04); display:flex; align-items:center; justify-content:center; }
.empty-text { font-family:Georgia,serif; font-size:18px; color:var(--text-muted); }
.empty-sub { font-size:13px; color:var(--text-light); }
.paste-area { width:100%; max-width:480px; background:var(--bg-surface); border:1px solid var(--border-light); border-radius:10px; padding:12px 14px; font-size:13px; color:var(--text-main); resize:vertical; outline:none; line-height:1.7; font-family:inherit; }
.paste-area:focus { border-color:var(--accent); }
.paste-area::placeholder { color:var(--text-light); }

.step-flow { padding:14px 24px; background:var(--bg-surface); border-bottom:1px solid var(--border-light); flex-shrink:0; }
.step-row { display:flex; align-items:center; justify-content:center; gap:0; }
.step { display:flex; align-items:center; gap:8px; }
.step-num { width:28px; height:28px; border-radius:50%; border:2px solid var(--border-medium); display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:600; color:var(--text-light); transition:all 0.3s; }
.step.active .step-num { border-color:var(--accent); color:var(--accent); }
.step.done .step-num { border-color:var(--color-success); color:var(--color-success); background:rgba(34,197,94,0.06); }
.step-label { font-size:12px; color:var(--text-light); font-weight:500; }
.step.active .step-label { color:var(--text-main); font-weight:600; }
.step.done .step-label { color:var(--color-success); }
.step-line { width:60px; height:2px; background:var(--border-medium); margin:0 16px; transition:background 0.5s; }
.step-line.active { background:var(--color-success); }
.step-spinner { display:inline-block; width:14px; height:14px; border:2px solid var(--border-light); border-top-color:var(--accent); border-radius:50%; animation:spin 0.6s linear infinite; }
@keyframes spin { to{transform:rotate(360deg)} }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.3} }

/* Workspace: Bento */
.workspace { flex:1; min-height:0; display:grid; background:var(--bg-canvas); overflow:hidden; }
.workspace.split-bento { grid-template-columns:1fr 340px; }
.editor-pane { display:flex; flex-direction:column; background:var(--bg-paper); min-height:0; }
.editor-pane.left { border-right:1px solid var(--border-light); }
.pane-header { padding:14px 24px; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--border-light); flex-shrink:0; background:transparent; }
.pane-title { font-size:12px; font-weight:600; color:var(--text-muted); text-transform:uppercase; letter-spacing:1px; }
.pane-action { font-size:12px; color:var(--text-light); }
.editor-content { flex:1; padding:24px 32px; overflow-y:auto; font-family:Georgia,'Noto Serif SC',serif; font-size:16px; line-height:1.9; color:var(--text-main); }
.editor-content.placeholder { display:flex; align-items:center; justify-content:center; font-family:var(--font-sans); }

/* Highlights */
.hl-item { position:relative; cursor:pointer; padding:2px 0; border-radius:4px 4px 0 0; border-bottom-width:2px; border-bottom-style:solid; background-size:100% 200%; background-position:top; transition:background-position 0.3s var(--spring),border-color 0.3s; }
.hl-danger { background-image:linear-gradient(to top, rgba(239,68,68,0.1) 50%, transparent 50%); border-bottom-color:rgba(239,68,68,0.3); }
.hl-warning { background-image:linear-gradient(to top, rgba(245,158,11,0.1) 50%, transparent 50%); border-bottom-color:rgba(245,158,11,0.3); }
.hl-danger:hover, .hl-item.active.hl-danger { background-position:bottom; border-bottom-color:var(--color-danger); }
.hl-warning:hover, .hl-item.active.hl-warning { background-position:bottom; border-bottom-color:var(--color-warning); }
.hl-resolved { opacity:0.4; text-decoration:line-through; }

/* Pane tabs */
.pane-tab { padding:6px 16px; border:none; background:transparent; font-size:12px; font-weight:500; color:var(--text-light); cursor:pointer; border-bottom:2px solid transparent; transition:all 0.2s; font-family:inherit; }
.pane-tab.active { color:var(--text-main); font-weight:600; border-bottom-color:var(--accent); }

/* Streaming text */
.streaming-text { font-family:Georgia,'Noto Serif SC',serif; font-size:14px; line-height:1.9; color:var(--text-main); white-space:pre-wrap; }
.streaming-line { margin:0 0 4px; }
.cursor-blink { display:inline-block; width:2px; height:16px; background:var(--accent); vertical-align:text-bottom; margin-left:2px; animation:blink 0.8s infinite; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.result-placeholder { display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; gap:12px; color:var(--text-light); font-size:13px; }

.fp-heading { margin:16px 0 6px; line-height:1.3; color:var(--text-main); user-select:none; }
.fp-body { margin:0 0 6px; line-height:1.9; color:var(--text-main); }

/* Bento sidebar */
.bento-sidebar { background:var(--bg-surface); padding:24px; overflow-y:auto; display:flex; flex-direction:column; gap:16px; border-left:1px solid var(--border-light); }
.bento-card { background:var(--bg-paper); border:1px solid var(--border-light); border-radius:14px; padding:20px; box-shadow:var(--shadow-sm); animation:slideUpFade 0.6s var(--spring) both; }
.bento-card:nth-child(2) { animation-delay:0.1s; }
@keyframes slideUpFade { 0%{opacity:0;transform:translateY(20px) scale(0.98)} 100%{opacity:1;transform:translateY(0) scale(1)} }

.risk-hero { display:flex; align-items:center; gap:16px; margin-bottom:16px; }
.score-ring { position:relative; width:72px; height:72px; flex-shrink:0; }
.score-ring svg { transform:rotate(-90deg); width:100%; height:100%; }
.ring-bg { fill:none; stroke:var(--border-light); stroke-width:6; }
.ring-fg { fill:none; stroke-width:6; stroke-linecap:round; }
.score-num { position:absolute; inset:0; display:flex; flex-direction:column; align-items:center; justify-content:center; font-size:22px; font-weight:700; letter-spacing:-1px; line-height:1; font-family:var(--font-sans); }
.score-num span { font-size:10px; font-weight:500; color:var(--text-light); letter-spacing:0; margin-top:2px; }
.risk-title { font-size:16px; font-weight:600; margin-bottom:4px; }
.risk-desc { font-size:12px; color:var(--text-muted); line-height:1.5; }

.severity-bars { display:flex; gap:3px; height:22px; border-radius:4px; overflow:hidden; margin-top:6px; }
.sev-bar { display:flex; align-items:center; justify-content:center; font-size:10px; font-weight:600; color:#fff; min-width:44px; padding:0 8px; white-space:nowrap; overflow:hidden; text-overflow:clip; }
.sev-crit { background:#EF4444; } .sev-high { background:#F59E0B; } .sev-med { background:#3B82F6; } .sev-style { background:#9CA3AF; }

.insight-panel { transition:transform 0.2s var(--spring); border-left:4px solid var(--color-danger); }
.insight-panel.pulse { animation:insightPulse 0.4s var(--spring); }
@keyframes insightPulse { 0%{transform:scale(1)} 50%{transform:scale(0.98)} 100%{transform:scale(1)} }
.insight-header { font-size:11px; font-weight:600; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:12px; display:flex; align-items:center; gap:6px; }
.insight-quote { font-family:var(--font-serif); font-size:13px; color:var(--text-muted); line-height:1.6; padding:6px 0 6px 12px; border-left:3px solid var(--border-medium); margin-bottom:16px; font-style:italic; word-break:break-all; }
.insight-issue { display:flex; flex-direction:column; gap:4px; font-size:13px; color:var(--text-muted); line-height:1.5; padding:12px; background:rgba(0,0,0,0.02); border-radius:8px; border-left:3px solid var(--color-danger); margin-bottom:16px; }
.insight-issue strong { color:var(--text-main); font-weight:600; }
.insight-suggestion { background:rgba(217,117,10,0.05); border:1px solid rgba(217,117,10,0.15); border-radius:12px; padding:16px; }
.insight-sugg-label { font-size:11px; font-weight:600; color:var(--accent); margin-bottom:8px; display:flex; align-items:center; gap:4px; }
.insight-sugg-text { font-family:var(--font-serif); font-size:14px; color:var(--text-main); line-height:1.6; }
.insight-sugg-textarea { width:100%; border:1px solid var(--border-light); border-radius:10px; padding:10px 44px 10px 12px; font-family:var(--font-serif); font-size:13px; line-height:1.7; color:var(--text-main); background:var(--bg-surface); resize:vertical; outline:none; transition:border-color 0.2s, box-shadow 0.2s; }
.insight-sugg-textarea:focus { border-color:var(--accent); box-shadow:0 0 0 3px rgba(217,117,10,0.08); background:var(--bg-paper); }
.insight-sugg-textarea::placeholder { color:var(--text-light); font-family:var(--font-sans); font-size:12px; }
.insight-sugg-result { border-color:rgba(22,163,74,0.2); background:rgba(22,163,74,0.02); }
.insight-sugg-result:focus { border-color:var(--color-success); box-shadow:0 0 0 3px rgba(22,163,74,0.06); }

.insight-textarea-wrap { position:relative; }
.btn-ai-send {
  position:absolute; bottom:10px; right:10px;
  width:36px; height:36px; padding:0;
  border-radius:10px; border:none;
  background:var(--accent); color:#fff; cursor:pointer;
  display:flex; align-items:center; justify-content:center;
  transition:all 0.2s var(--spring);
  box-shadow:0 2px 6px rgba(217,117,10,0.2);
}
.btn-ai-send:hover { background:var(--accent-hover); transform:scale(1.08); box-shadow:0 4px 12px rgba(217,117,10,0.3); }
.btn-ai-send:active { transform:scale(0.94); }
.btn-ai-send:disabled { opacity:0.4; cursor:not-allowed; transform:none; box-shadow:none; }
.btn-ai-spinner { width:14px; height:14px; border:2px solid rgba(255,255,255,0.3); border-top-color:#fff; border-radius:50%; animation:spin 0.6s linear infinite; }
@keyframes spin { to{transform:rotate(360deg)} }
.insight-stream { display:flex; align-items:center; gap:6px; margin-top:8px; font-size:12px; color:var(--accent); }
.streaming-dot { width:6px; height:6px; background:var(--accent); border-radius:50%; animation:pulse 1s infinite; }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.3} }
.insight-actions { display:flex; gap:10px; margin-top:16px; }

.capsule-toggle { position:relative; display:inline-flex; border:1px solid var(--border-medium); border-radius:100px; overflow:hidden; background:var(--bg-surface); }
.capsule-slider { position:absolute; top:0; left:0; width:50%; height:100%; background:#141413; border-radius:100px; transition:transform 0.35s cubic-bezier(0.34,1.56,0.64,1); z-index:0; }
.capsule-slider.right { transform:translateX(100%); }
.capsule-opt { position:relative; z-index:1; padding:5px 16px; font-size:12px; font-weight:500; border:none; background:transparent; color:var(--text-muted); cursor:pointer; transition:color 0.25s; font-family:inherit; }
.capsule-opt.active { color:#fff; }
.quota-badge{font-size:11px;color:var(--text-muted);background:var(--bg-surface);padding:4px 12px;border-radius:100px;white-space:nowrap;border:1px solid var(--border-light);}
.quota-badge.quota-low{color:var(--color-danger);border-color:rgba(239,68,68,0.25);background:rgba(239,68,68,0.04);}

.spin-icon{animation:spin 1s linear infinite}@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}
.error-toast { position:fixed; bottom:24px; left:50%; transform:translateX(-50%); background:var(--color-danger); color:#fff; padding:10px 24px; border-radius:10px; font-size:13px; cursor:pointer; z-index:200; }
</style>
