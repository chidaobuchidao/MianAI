<template>
  <div class="app-shell">
    <!-- Header -->
    <header class="app-header">
      <div class="app-brand">
        <button class="btn btn-icon" @click="router.replace('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="brand-name">Mianmian.</span>
        <span class="brand-tag">学术润色</span>
        <button class="btn btn-outline btn-switch" @click="router.replace('/paper-tools/ai-reduce')">降AI / 降查重</button>
      </div>
      <div style="display:flex;align-items:center;gap:16px;margin-left:auto;">
        <div class="capsule-toggle">
          <div class="capsule-slider" :class="{ right: polishModel === 'deepseek-v4-pro' }" />
          <button class="capsule-opt" :class="{ active: polishModel === 'deepseek-v4-flash' }" @click="polishModel = 'deepseek-v4-flash'">Flash</button>
          <button class="capsule-opt" :class="{ active: polishModel === 'deepseek-v4-pro' }" @click="polishModel = 'deepseek-v4-pro'">Pro</button>
        </div>
        <span v-if="quotaInfo.quotaRemaining >= 0" class="quota-badge" :class="{ 'quota-low': quotaInfo.quotaRemaining <= 2 && !quotaInfo.unlimited }">
          {{ quotaInfo.unlimited ? '无限次' : `剩余 ${quotaInfo.quotaRemaining}/${quotaInfo.dailyQuota} 次` }}
        </span>
        <div v-if="hasDocument && resultText" class="export-dropdown">
          <button class="btn btn-outline" :disabled="isExporting" @click="showExport = !showExport">
            <svg v-if="!isExporting" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            <span v-if="isExporting" class="btn-spinner"></span>
            {{ isExporting ? '转换中...' : '导出文档' }}
          </button>
          <div v-if="showExport" class="export-menu">
            <div v-if="canPreserveFormat" class="export-item" @click="exportDoc('preserve')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              原格式导出
              <span class="export-badge">推荐</span>
            </div>
            <div v-if="canUsePdfToWordExport && quotaInfo.isAdmin" class="export-item" @click="exportDoc('pdf-beta')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              PDF 转 Word 保留导出
              <span class="export-badge">Beta</span>
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
        <div v-for="t in polishTabs" :key="t.value"
          class="tab-btn" :class="{ active: polishType === t.value }"
          :ref="el => tabRefs[t.value] = el as any"
          @click="setPolishTab(t.value)">{{ t.label }}</div>
      </div>
      <div class="toolbar-spacer" />
      <button v-if="hasDocument" class="btn btn-outline btn-link" @click="showAdvanced = !showAdvanced">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        高级选项
        <svg width="10" height="10" viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="1.5" :style="{ transform: showAdvanced ? 'rotate(180deg)' : 'rotate(0)' }"><polyline points="2 3 5 6 8 3"/></svg>
      </button>
      <button class="btn btn-outline btn-kb" @click="showKbPanel = true">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
        知识库
        <span v-if="kbPapers.length" class="kb-badge">{{ kbPapers.length }}</span>
      </button>
      <button class="btn btn-outline btn-upload" @click="triggerUpload">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        上传文档
      </button>
      <input ref="fileInput" type="file" accept=".docx,.pdf" style="display:none" @change="handleFileUpload" />
      <button class="btn btn-solid accent" @click="startPolish" :disabled="isPolishBusy || !hasDocument">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        {{ isPolishBusy ? '润色中...' : '开始润色' }}
      </button>
      <button v-if="lastRetrievedCount > 0" class="kb-hint kb-hint--button" @click="showKbHitDetails = true">命中 {{ lastRetrievedCount }} 个片段 / 来自 {{ lastRetrievedPaperCount }} 篇论文</button>
      <span v-else-if="kbSettings.autoRetrieve && kbPapers.length > 0 && !isStreaming" class="kb-hint kb-hint--idle">知识库就绪（{{ kbPapers.length }} 篇）</span>
    </div>

    <!-- Advanced Options (collapsible) -->
    <div v-if="showAdvanced" class="advanced-panel">
      <div class="advanced-grid">
        <div class="advanced-item">
          <label class="advanced-label">任务类型</label>
          <select v-model="taskType" class="advanced-select">
            <option value="章节正文">章节正文</option>
            <option value="摘要">摘要</option>
            <option value="引言">引言</option>
            <option value="结论">结论</option>
            <option value="论文大纲">论文大纲</option>
            <option value="自定义段落">自定义段落</option>
          </select>
        </div>
        <div class="advanced-item advanced-item--grow">
          <label class="advanced-label">主题</label>
          <input v-model="topic" type="text" class="advanced-input" placeholder="如：深度学习模型优化" />
        </div>
        <div class="advanced-item advanced-item--grow">
          <label class="advanced-label">补充说明</label>
          <input v-model="notes" type="text" class="advanced-input" placeholder="如：重点优化第3-5段的逻辑衔接，保留所有数据" />
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!hasDocument" class="empty-state">
      <div class="empty-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1" opacity="0.3"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      </div>
      <p class="empty-text">上传 DOCX 开始润色</p>
      <p class="empty-sub">支持 .docx / .pdf 格式，AI 逐段润色并保留原文段落结构</p>
      <textarea v-model="pasteText" class="paste-area" placeholder="或直接粘贴论文正文..." rows="5" />
      <button v-if="pasteText.trim()" class="btn btn-solid accent" @click="usePastedText">使用粘贴文本</button>
    </div>

    <!-- Workspace PC: 50/50 split -->
    <div v-if="isDesktop && hasDocument" class="workspace split-50">
      <!-- LEFT: Original reference (readonly) -->
      <div class="editor-pane left">
        <div class="pane-header">
          <span class="pane-title">原文参考</span>
          <span class="pane-hint">只读对照</span>
        </div>
        <div class="editor-content">
          <template v-for="p in formatParagraphs" :key="'ref-'+p.index">
            <div v-if="p.isHeading" class="fp-heading" :style="headingStyle(p.style)">{{ p.originalText }}</div>
            <div v-else class="fp-body" :style="bodyStyle(p.style)">{{ p.originalText }}</div>
          </template>
        </div>
      </div>

      <!-- RIGHT: Polish result -->
      <div class="editor-pane right">
        <div class="pane-header">
          <span class="pane-title">润色结果</span>
          <div style="display:flex;align-items:center;gap:12px;">
            <span v-if="isStreaming" class="pane-badge live">生成中</span>
            <span v-else-if="resultText" class="pane-hint">完成</span>
            <button v-if="resultText && !isStreaming" class="btn btn-outline" style="font-size:11px;padding:4px 10px;gap:4px;" @click="copyResult">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              复制结果
            </button>
          </div>
        </div>
        <div class="editor-content">
          <!-- Empty placeholder -->
          <div v-if="!resultText && !isStreaming" class="result-placeholder">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            <span>选择润色方式后点击「开始润色」</span>
          </div>

          <!-- Streaming: raw typewriter text -->
          <div v-else-if="isStreaming" class="streaming-text">
            <template v-for="(line, i) in streamingLines" :key="'s-'+i">
              <p v-if="line" class="streaming-line">{{ line }}</p>
            </template>
            <span class="cursor-blink" />
          </div>

          <!-- Done: parsed per-paragraph with diff -->
          <template v-else v-for="p in resultParagraphs" :key="'r-'+p.index">
            <div v-if="p.isHeading" class="fp-heading" :style="headingStyle(p.style)">{{ p.polishedText }}</div>
            <div v-else class="fp-body" contenteditable="true"
              :data-index="p.index"
              :style="bodyStyle(p.style)"
              @input="onParaInput(p.index)"
              @blur="onParaEdit($event, p.index)"
              @keydown="onParaKey"
              v-html="renderDiff(p.originalText, p.polishedText)"
            ></div>
          </template>
        </div>
      </div>
    </div>

    <!-- Workspace Mobile: tab-switch stack -->
    <div v-if="!isDesktop && hasDocument" class="workspace workspace--mobile">
      <div class="mobile-pane-tabs">
        <button :class="{ active: mobilePane === 'original' }" @click="mobilePane = 'original'">原文参考</button>
        <button :class="{ active: mobilePane === 'result' }" @click="mobilePane = 'result'">润色结果</button>
      </div>

      <!-- Original pane -->
      <div class="mobile-pane" v-show="mobilePane === 'original'">
        <div class="editor-content">
          <template v-for="p in formatParagraphs" :key="'mref-'+p.index">
            <div v-if="p.isHeading" class="fp-heading" :style="headingStyle(p.style)">{{ p.originalText }}</div>
            <div v-else class="fp-body" :style="bodyStyle(p.style)">{{ p.originalText }}</div>
          </template>
        </div>
      </div>

      <!-- Result pane -->
      <div class="mobile-pane" v-show="mobilePane === 'result'">
        <div class="mobile-pane-actions" v-if="resultText && !isStreaming">
          <button class="btn btn-outline" style="font-size:11px;padding:4px 10px;gap:4px;" @click="copyResult">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
            复制结果
          </button>
        </div>
        <div class="editor-content" ref="resultPaneMobile">
          <div v-if="!resultText && !isStreaming" class="result-placeholder">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
            <span>选择润色方式后点击「开始润色」</span>
          </div>
          <div v-else-if="isStreaming" class="streaming-text">
            <template v-for="(line, i) in streamingLines" :key="'sm-'+i">
              <p v-if="line" class="streaming-line">{{ line }}</p>
            </template>
            <span class="cursor-blink" />
          </div>
          <template v-else v-for="p in resultParagraphs" :key="'rm-'+p.index">
            <div v-if="p.isHeading" class="fp-heading" :style="headingStyle(p.style)">{{ p.polishedText }}</div>
            <div v-else class="fp-body" contenteditable="true"
              :data-index="p.index"
              :style="bodyStyle(p.style)"
              @input="onParaInput(p.index)"
              @blur="onParaEdit($event, p.index)"
              @keydown="onParaKey"
              v-html="renderDiff(p.originalText, p.polishedText)"
            ></div>
          </template>
        </div>
      </div>
    </div>

    <p v-if="error" class="error-toast">{{ error }}</p>
    <p v-if="warnToast" class="warn-toast" @click="warnToast = ''">{{ warnToast }}</p>

    <!-- Knowledge Base Panel -->
    <PaperKbPanel
      :visible="showKbPanel"
      :papers="kbPapers"
      :is-loading="kbLoading"
      :importing-file="kbImporting"
      :settings="kbSettings"
      :error="kbError"
      @close="showKbPanel = false"
      @import="handleKbImport($event)"
      @delete="handleKbDelete($event)"
      @clear-all="handleKbClearAll()"
      @backup="handleKbBackup()"
      @restore="handleKbRestore($event)"
      @update:settings="kbSettings = $event"
    />
    <KbHitDetails
      :visible="showKbHitDetails"
      :chunks="lastRetrievedChunks"
      :optimized-text="resultText"
      @close="showKbHitDetails = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, type StyleValue } from 'vue'
import { useRouter } from 'vue-router'
import { useQuota, type QuotaInfo } from '@/composables/useQuota'
import { useResponsive } from '@/composables/useResponsive'
import { usePaperStore } from '@/composables/usePaperStore'
import { usePaperKbPanel } from '@/composables/usePaperKbPanel'
import { readJsonResponse } from '@/utils/httpResponse'
import { authFetch } from '@/utils/authFetch'
import { isDocxFile, isPdfFile } from '@/utils/documentFile'
import { preserveOriginalSpacing } from '@/utils/spacingGuard'
import PaperKbPanel from '@/components/PaperKbPanel.vue'
import KbHitDetails from '@/components/KbHitDetails.vue'

const router = useRouter()
const { fetchQuota, checkQuota } = useQuota()
const { isDesktop } = useResponsive()
const mobilePane = ref<'original' | 'result'>('original')
const paperStore = usePaperStore()

// === State ===
const quotaInfo = ref<QuotaInfo>({ hasApiKey: false, isAdmin: false, unlimited: false, dailyQuota: 10, quotaUsed: 0, quotaRemaining: 10 })
const sourceText = ref('')
const resultText = ref('')
const polishModel = ref('deepseek-v4-flash')
const polishType = ref('full')
const taskType = ref('章节正文')
const topic = ref('')
const notes = ref('')
const pasteText = ref('')
const showAdvanced = ref(false)
const showExport = ref(false)
const isExporting = ref(false)
const isStreaming = ref(false)
const isPreparingPolish = ref(false)
const isPolishBusy = computed(() => isPreparingPolish.value || isStreaming.value)
const hasDocument = ref(false)
const error = ref('')
const warnToast = ref('')
let warnTimer: ReturnType<typeof setTimeout> | null = null
function showWarn(msg: string) { warnToast.value = msg; if (warnTimer) clearTimeout(warnTimer); warnTimer = setTimeout(() => { warnToast.value = '' }, 5000) }
const storedFile = ref<File | null>(null)
const canPreserveFormat = computed(() => isDocxFile(storedFile.value))
const canUsePdfToWordExport = computed(() => isPdfFile(storedFile.value))
const paragraphData = ref<any[]>([])
const polishedParagraphs = ref<Map<number, string>>(new Map())
const dirtyParagraphs = ref<Set<number>>(new Set())
let abortController: AbortController | null = null

type TextAlignValue = 'left' | 'right' | 'center' | 'justify' | 'start' | 'end'

function headingStyle(style: any): StyleValue {
  return {
    fontSize: `${style?.fontSize ?? 14}px`,
    fontFamily: style?.fontFamily,
    fontWeight: '700',
  }
}

function bodyStyle(style: any): StyleValue {
  return {
    fontFamily: style?.fontFamily,
    fontSize: `${style?.fontSize ?? 12}pt`,
    fontWeight: style?.bold ? '700' : '400',
    fontStyle: style?.italic ? 'italic' : 'normal',
    textAlign: normalizeTextAlign(style?.alignment),
  }
}

function normalizeTextAlign(value: unknown): TextAlignValue {
  return value === 'right' || value === 'center' || value === 'justify' || value === 'start' || value === 'end'
    ? value
    : 'left'
}

// === Knowledge Base ===
const {
  papers: kbPapers, isLoading: kbLoading, importingFile: kbImporting,
  showKbPanel, settings: kbSettings, error: kbError,
  lastRetrievedCount, lastRetrievedPaperCount, lastRetrievedChunks,
  handleImport: handleKbImport, handleDelete: handleKbDelete,
  handleClearAll: handleKbClearAll, handleBackup: handleKbBackup,
  handleRestore: handleKbRestore, retrieveAndFormat: kbRetrieveAndFormat,
} = usePaperKbPanel('paper_polish')
const showKbHitDetails = ref(false)

// === Tabs ===
const polishTabs = [
  { value: 'vocab', label: '词汇优化' },
  { value: 'logic', label: '逻辑强化' },
  { value: 'full', label: '全面润色' },
]

const tabRefs: Record<string, HTMLElement | null> = {}
const tabIndicator = ref<HTMLElement | null>(null)

function setPolishTab(value: string) { polishType.value = value; nextTick(() => moveIndicator(value)) }
function moveIndicator(value?: string) {
  const val = value || polishType.value
  const indicator = tabIndicator.value
  const tab = tabRefs[val]
  if (!indicator || !tab) return
  const parent = indicator.parentElement
  if (!parent) return
  const pr = parent.getBoundingClientRect()
  const tr = tab.getBoundingClientRect()
  indicator.style.width = `${tr.width}px`
  indicator.style.transition = 'all 0.4s var(--spring-bounce)'
  indicator.style.transform = `translateX(${tr.left - pr.left}px)`
}

onMounted(() => nextTick(() => moveIndicator()))
onMounted(async () => { const q = await fetchQuota(); if (q) quotaInfo.value = q })
onMounted(() => {
  if (hasDocument.value) return
  const doc = paperStore.load()
  if (!doc?.sourceText) return
  sourceText.value = doc.sourceText
  paragraphData.value = doc.paragraphs?.length ? doc.paragraphs : fallbackParagraphs(doc.sourceText)
  polishedParagraphs.value = new Map()
  for (const p of paragraphData.value) polishedParagraphs.value.set(p.index, p.text || p.originalText || '')
  hasDocument.value = true
  storedFile.value = null
})

let resizeObs: ResizeObserver | null = null
onMounted(() => {
  resizeObs = new ResizeObserver(() => moveIndicator())
  const parent = tabIndicator.value?.parentElement
  if (parent) resizeObs.observe(parent)
})
onUnmounted(() => {
  resizeObs?.disconnect()
  abortController?.abort()
})

watch(polishType, () => nextTick(() => moveIndicator()))

// === Paragraph rendering ===
interface ParaStyle { fontSize: number; fontFamily: string; bold: boolean; italic: boolean; alignment: string }
interface RenderPara { index: number; originalText: string; polishedText: string; isHeading: boolean; style: ParaStyle }

const formatParagraphs = computed<RenderPara[]>(() => {
  if (!paragraphData.value.length) return []
  return paragraphData.value.map((p: any) => {
    const isHeading = !!(p.styleId && /heading/i.test(p.styleId))
    const style: ParaStyle = {
      fontSize: isHeading ? (p.styleId && /1/.test(p.styleId) ? 22 : p.styleId && /2/.test(p.styleId) ? 18 : 15) : (p.fontSize || 12),
      fontFamily: isHeading ? 'Inter, PingFang SC, sans-serif' : (p.fontFamily || 'Georgia, Noto Serif SC, serif'),
      bold: p.bold || isHeading,
      italic: p.italic || false,
      alignment: p.alignment || 'left',
    }
    return {
      index: p.index,
      originalText: p.text || '',
      polishedText: polishedParagraphs.value.get(p.index) ?? p.text ?? '',
      isHeading,
      style,
    }
  })
})

const resultParagraphs = computed<RenderPara[]>(() => {
  if (!resultText.value) return []
  if (!paragraphData.value.length) {
    const texts = resultText.value.split(/\n{2,}/).filter(p => p.trim())
    return texts.map((t, i) => ({
      index: i,
      originalText: '',
      polishedText: t.trim(),
      isHeading: false,
      style: { fontSize: 12, fontFamily: 'Georgia, Noto Serif SC, serif', bold: false, italic: false, alignment: 'left' },
    }))
  }
  return formatParagraphs.value.map(p => ({
    ...p,
    polishedText: polishedParagraphs.value.get(p.index) ?? p.originalText,
  }))
})

const streamingLines = computed(() => {
  if (!resultText.value) return ['']
  return resultText.value.split('\n').filter(l => l !== undefined)
})

// === Upload ===
const fileInput = ref<HTMLInputElement | null>(null)
function triggerUpload() { fileInput.value?.click() }

function fallbackParagraphs(text: string): any[] {
  if (!text?.trim()) return []
  return text.split(/\n{2,}/).filter(p => p.trim().length > 3).map((t, i) => ({ index: i, text: t.trim() }))
}

function usePastedText() {
  const text = pasteText.value.trim()
  if (!text) return
  sourceText.value = text
  paragraphData.value = fallbackParagraphs(text)
  polishedParagraphs.value = new Map()
  for (const p of paragraphData.value) polishedParagraphs.value.set(p.index, p.text)
  hasDocument.value = true
  resultText.value = ''
}

async function handleFileUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.name.toLowerCase().endsWith('.pdf')) showWarn('PDF格式可能导致导出效果不佳，建议上传Word文档(.docx)以保证最佳导出效果')
  storedFile.value = file
  input.value = '' // 清除以允许重复选同一文件
  const fd = new FormData(); fd.append('file', file)
  try {
    const res = await authFetch('/api/paper/upload', {
      method: 'POST',
      body: fd,
    })
    const data = await readJsonResponse(res, '文件上传失败')
    if (!res.ok) throw new Error(data.error || '上传失败')
    if (!data.fullText || data.fullText.trim().length < 10) {
      error.value = '文档解析失败：未提取到有效文本内容。请检查文档是否为扫描版图片，或尝试另存为标准 .docx 格式。'
      return
    }
    sourceText.value = data.fullText
    paragraphData.value = (data.paragraphs?.length ? data.paragraphs : fallbackParagraphs(data.fullText))
    polishedParagraphs.value = new Map()
    resultText.value = ''
    hasDocument.value = true
    paperStore.save({ sourceText: data.fullText, paragraphs: paragraphData.value, fileName: file.name, timestamp: Date.now() })
    for (const p of data.paragraphs || []) {
      polishedParagraphs.value.set(p.index, p.text || '')
    }
  } catch (err: any) {
    error.value = '文件解析失败: ' + (err.message || String(err))
  }
}

// === [Pn] Markers ===
function buildMarkedPrompt(): string {
  const paras = paragraphData.value
  if (!paras.length) return `[P0] ${sourceText.value}`
  return paras.map((p: any) => `[P${p.index}] ${p.text || ''}`).join('\n\n')
}

function parseMarkedResponse(response: string): Map<number, string> {
  const result = new Map<number, string>()
  const re = /\[P(\d+)\]\s*([\s\S]*?)(?=\[P\d+\]|$)/g
  let match
  while ((match = re.exec(response)) !== null) {
    const index = parseInt(match[1])
    result.set(index, preserveParagraphSpacing(index, cleanPolishOutput(match[2].trim())))
  }
  if (result.size === 0 && response.trim()) {
    const paras = paragraphData.value
    const parts = response.split(/\n{2,}/).filter(p => p.trim())
    if (paras.length > 0 && parts.length > 0) {
      const count = Math.min(paras.length, parts.length)
      for (let i = 0; i < count; i++) {
        result.set(paras[i].index, preserveParagraphSpacing(paras[i].index, cleanPolishOutput(parts[i].trim())))
      }
    } else {
      result.set(0, cleanPolishOutput(response.trim()))
    }
  }
  return result
}

interface SseEvent {
  event: string
  data: string
}

function parseSseEventBlock(block: string): SseEvent | null {
  const lines = block.replace(/\r/g, '').split('\n')
  let event = 'message'
  const dataLines: string[] = []
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
      continue
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5))
    }
  }
  if (!dataLines.length) return null
  return { event, data: dataLines.join('\n') }
}

function decodeTokenPayload(data: string): string {
  const payload = data.trimStart()
  if (!payload) return ''
  try {
    const parsed = JSON.parse(payload)
    return typeof parsed === 'string' ? parsed : ''
  } catch {
    return data
  }
}

function cleanPolishOutput(text: string): string {
  return collapseRunawayRepetitions(text.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F]/g, ''))
}

function collapseRunawayRepetitions(text: string): string {
  let result = text
  const patterns = [
    /([\u4e00-\u9fff]{2,36})(?:\1){2,}/g,
    /([A-Za-z][A-Za-z0-9' -]{8,80})(?:\1){2,}/g,
  ]
  for (let pass = 0; pass < 3; pass++) {
    const before = result
    for (const pattern of patterns) {
      result = result.replace(pattern, '$1')
    }
    if (result === before) break
  }
  return result
}

function preserveParagraphSpacing(index: number, text: string): string {
  const original = paragraphData.value.find((p: any) => p.index === index)?.text ?? ''
  return preserveOriginalSpacing(original, text)
}

function serializeMarkedParagraphs(paragraphs: Map<number, string>): string {
  return paragraphData.value
    .map((p: any) => `[P${p.index}] ${paragraphs.get(p.index) ?? p.text ?? ''}`)
    .join('\n\n')
}

// === SSE Polish ===
async function startPolish() {
  if (!hasDocument.value || isPolishBusy.value) return

  const needed = polishModel.value.includes('pro') ? 2 : 1
  const qr = checkQuota(needed, `今日免费次数不足（需 ${needed} 次），请配置 API Key 后继续使用`)
  if (!qr.ok) { error.value = qr.msg || '配额不足'; return }
  if (!isDesktop.value) mobilePane.value = 'result'

  isPreparingPolish.value = true
  resultText.value = ''
  error.value = ''
  dirtyParagraphs.value = new Set()

  try {
    const text = paragraphData.value.length > 0 ? buildMarkedPrompt() : sourceText.value
    const kbContext = await kbRetrieveAndFormat(text, {
      focusText: `${taskType.value} ${topic.value}`.trim(),
      notes: notes.value,
    })
    const augmentedNotes = [
      notes.value,
      paragraphData.value.length > 1 ? '按[P{n}]标记逐段润色。保持[P{n}]标记不变。段落数量必须与输入一致。' : '',
      kbContext,
    ].filter(Boolean).join('\n')

    isStreaming.value = true
    abortController = new AbortController()

    const res = await authFetch('/api/polish/run', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        text,
        taskType: taskType.value,
        polishType: polishType.value,
        topic: topic.value,
        notes: augmentedNotes,
        model: polishModel.value,
      }),
      signal: abortController.signal,
    })

    if (!res.ok) {
      if (res.status === 429) { const d = await res.json().catch(() => ({ error: '配额已用完' })); throw new Error(d.error || '今日免费次数已用完') }
      throw new Error(`请求失败: ${res.status}`)
    }

    const reader = res.body?.getReader()
    if (!reader) { isStreaming.value = false; return }

    const decoder = new TextDecoder()
    let buffer = ''
    let rawResult = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ''
      for (const block of blocks) {
        const event = parseSseEventBlock(block)
        if (!event) continue
        if (event.event === 'token') {
          rawResult += decodeTokenPayload(event.data)
          resultText.value = rawResult
        } else if (event.event === 'error') {
          throw new Error(event.data.trim() || '润色失败')
        }
      }
    }
    const tail = decoder.decode()
    if (tail) buffer += tail
    const finalEvent = parseSseEventBlock(buffer)
    if (finalEvent?.event === 'token') {
      rawResult += decodeTokenPayload(finalEvent.data)
      resultText.value = rawResult
    }

    // Parse [Pn] markers
    if (rawResult && paragraphData.value.length > 0) {
      const parsed = parseMarkedResponse(rawResult)
      polishedParagraphs.value = parsed
      resultText.value = serializeMarkedParagraphs(parsed)
    }
    fetchQuota().then(q => { if (q) quotaInfo.value = q })
  } catch (e: unknown) {
    if (e instanceof DOMException && e.name === 'AbortError') return
    error.value = '请求失败: ' + (e instanceof Error ? e.message : String(e))
  } finally {
    isStreaming.value = false
    isPreparingPolish.value = false
    abortController = null
  }
}

async function copyResult() {
  if (resultText.value) {
    await navigator.clipboard.writeText(resultText.value)
  }
}

// === Contenteditable ===
function onParaInput(idx: number) {
  const next = new Set(dirtyParagraphs.value)
  next.add(idx)
  dirtyParagraphs.value = next
}

function onParaEdit(e: FocusEvent, idx: number) {
  if (!dirtyParagraphs.value.has(idx)) return
  const el = e.target as HTMLElement
  const text = extractEditablePolishedText(el)
  if (text) polishedParagraphs.value.set(idx, text)
  const next = new Set(dirtyParagraphs.value)
  next.delete(idx)
  dirtyParagraphs.value = next
}

function onParaKey(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    e.preventDefault()
    const el = e.target as HTMLElement
    const next = el.nextElementSibling as HTMLElement | null
    if (next?.contentEditable === 'true') next.focus()
  }
}

function extractEditablePolishedText(el: HTMLElement): string {
  const clone = el.cloneNode(true) as HTMLElement
  clone.querySelectorAll('.diff-del').forEach(node => node.remove())
  return clone.innerText?.trim() || ''
}

// === Export ===
async function exportDoc(mode: string) {
  showExport.value = false
  if (mode === 'preserve' && !canPreserveFormat.value) {
    error.value = 'PDF 暂不支持原格式导出，已改用标准 Word 导出。原格式和图片保留目前仅支持 DOCX。'
    mode = 'standard'
  }
  const allParas = paragraphData.value.map((p: any) => ({
    index: p.index, text: polishedParagraphs.value.get(p.index) ?? p.text ?? '', originalText: p.text ?? '',
  }))
  const changedParas = allParas.filter((p: any) => (polishedParagraphs.value.get(p.index) ?? '').trim())

  if (mode === 'pdf-beta' && storedFile.value && canUsePdfToWordExport.value) {
    isExporting.value = true
    const fd = new FormData()
    fd.append('file', storedFile.value)
    fd.append('mappings', JSON.stringify({ fileName: 'polished', paragraphs: allParas }))
    try {
      const res = await authFetch('/api/paper-export/pdf-to-docx-preserve-format', { method:'POST', body:fd })
      if (res.ok) { const blob=await res.blob(); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='polished.docx'; a.click(); URL.revokeObjectURL(url); return }
      const errData = await res.json().catch(() => null)
      error.value = (errData?.detail || errData?.error || 'PDF 转 Word 保留导出失败') + '。是否改用标准导出？'
      if (!confirm(error.value)) { error.value = ''; return }
    } catch(err: any) {
      error.value = 'PDF 转 Word 导出失败: ' + (err.message || String(err)) + '。是否改用标准导出？'
      if (!confirm(error.value)) { error.value = ''; return }
    } finally {
      isExporting.value = false
    }
  }

  if (mode === 'preserve' && storedFile.value && canPreserveFormat.value) {
    const fd = new FormData()
    fd.append('file', storedFile.value)
    fd.append('mappings', JSON.stringify({ fileName: 'polished', paragraphs: changedParas }))
    try {
      const res = await authFetch('/api/paper-export/preserve-format', { method:'POST', body:fd })
      if (res.ok) { const blob=await res.blob(); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='polished.docx'; a.click(); URL.revokeObjectURL(url); return }
      const errData = await res.json().catch(() => null)
      error.value = (errData?.detail || errData?.error || '格式保留导出失败') + '。是否改用标准导出？'
      if (!confirm(error.value)) { error.value = ''; return }
    } catch(err: any) {
      error.value = '导出失败: ' + (err.message || String(err)) + '。是否改用标准导出？'
      if (!confirm(error.value)) { error.value = ''; return }
    }
  }
  try {
    const res = await authFetch('/api/paper-export/standard', { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({ fileName:'polished', paragraphs:allParas }) })
    if (res.ok) { const blob=await res.blob(); const url=URL.createObjectURL(blob); const a=document.createElement('a'); a.href=url; a.download='polished.docx'; a.click(); URL.revokeObjectURL(url); return }
    error.value = '标准导出也失败了，请重试'
  } catch(err: any) { error.value = '导出失败: ' + (err.message || String(err)) }
}

/** Simple word-level diff: marks added/deleted segments */
function renderDiff(original: string, polished: string): string {
  if (!original || !polished || original === polished) return escapeHtml(polished)
  const oWords = original.split(/([一-鿿]+|[a-zA-Z]+|\s+|[，。、；：！？""''《》【】（）])/g).filter(w => w)
  const pWords = polished.split(/([一-鿿]+|[a-zA-Z]+|\s+|[，。、；：！？""''《》【】（）])/g).filter(w => w)

  // LCS-based diff
  const m = oWords.length, n = pWords.length
  const dp: number[][] = Array.from({length: m+1}, () => new Array(n+1).fill(0))
  for (let i = 1; i <= m; i++)
    for (let j = 1; j <= n; j++)
      dp[i][j] = oWords[i-1] === pWords[j-1] ? dp[i-1][j-1] + 1 : Math.max(dp[i-1][j], dp[i][j-1])

  // Backtrack
  const result: string[] = []
  let i = m, j = n
  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && oWords[i-1] === pWords[j-1]) {
      result.unshift(escapeHtml(oWords[i-1]))
      i--; j--
    } else if (j > 0 && (i === 0 || dp[i][j-1] >= dp[i-1][j])) {
      result.unshift(`<span class="diff-add">${escapeHtml(pWords[j-1])}</span>`)
      j--
    } else if (i > 0) {
      result.unshift(`<span class="diff-del">${escapeHtml(oWords[i-1])}</span>`)
      i--
    }
  }
  return result.join('')
}

function escapeHtml(s: string) { return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') }
</script>

<style scoped>
.app-shell {
  --accent: #D9750A; --accent-hover: #C26300; --color-success: #16A34A; --color-danger: #DC2626;
  --border-light: rgba(0,0,0,0.06); --border-medium: rgba(0,0,0,0.12);
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.04); --shadow-md: 0 12px 32px rgba(0,0,0,0.08);
  --spring: cubic-bezier(0.25, 1, 0.4, 1); --spring-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
  --bg-canvas: #F3EFE8; --bg-paper: #FDFCFB; --bg-surface: #F5F4F1;
  --text-main: #141413; --text-muted: #555; --text-light: #999;
  max-width: 1280px; height: calc(100vh - 48px); margin: 24px auto;
  background: var(--bg-paper); border-radius: 20px;
  box-shadow: 0 24px 48px rgba(0,0,0,0.08), 0 0 0 1px var(--border-light);
  display: flex; flex-direction: column; overflow: hidden;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* Header */
.app-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 24px; border-bottom: 1px solid var(--border-light); background: var(--bg-paper); z-index: 20; flex-shrink: 0; }
.app-brand { display: flex; align-items: center; gap: 12px; }
.brand-name { font-family: Georgia, serif; font-size: 18px; font-weight: 600; }
.brand-tag { font-size: 10px; font-weight: 600; color: var(--accent); background: rgba(217,117,10,0.08); padding: 4px 10px; border-radius: 100px; letter-spacing: 1px; }

.btn { display: inline-flex; align-items: center; justify-content: center; gap: 8px; padding: 8px 16px; border-radius: 10px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s var(--spring); border: 1px solid transparent; font-family: inherit; }
.btn:active { transform: scale(0.96); }
.btn-icon { width: 36px; height: 36px; padding: 0; border-radius: 10px; border-color: var(--border-medium); background: var(--bg-paper); color: var(--text-muted); }
.btn-icon:hover { background: var(--bg-surface); color: var(--text-main); }
.btn-outline { background: var(--bg-paper); border-color: var(--border-medium); color: var(--text-main); }
.btn-outline:hover { background: var(--bg-surface); }
.btn-spinner { width: 14px; height: 14px; border: 2px solid var(--border-medium); border-top-color: var(--accent); border-radius: 50%; animation: btn-spin 0.6s linear infinite; }
@keyframes btn-spin { to { transform: rotate(360deg); } }
.btn-solid { background: var(--bg-paper); color: #141413; box-shadow: var(--shadow-sm); border: 1px solid var(--border-medium); }
.btn-solid:hover { opacity: 0.9; }
.btn-solid.accent { background: var(--accent); color: #fff; border-color: var(--accent); }
.btn-solid:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-upload { font-weight: 600; color: var(--accent); border-color: rgba(217,117,10,0.25); }
.btn-upload:hover { background: rgba(217,117,10,0.06); border-color: var(--accent); color: var(--accent-hover); }
.btn-switch { font-size: 11px; padding: 5px 12px; color: var(--text-muted); margin-left: 12px; }
.btn-switch:hover { color: var(--accent); border-color: var(--accent); }
.btn-kb { position: relative; font-weight: 600; color: var(--accent); border-color: rgba(217,117,10,0.25); }
.btn-kb:hover { background: rgba(217,117,10,0.06); border-color: var(--accent); color: var(--accent-hover); }
.kb-badge { font-size: 10px; min-width: 16px; height: 16px; display: inline-flex; align-items: center; justify-content: center; background: var(--accent); color: #fff; border-radius: 100px; padding: 0 4px; margin-left: 4px; }
.kb-hint { font-size: 11px; color: var(--accent); white-space: nowrap; }
.kb-hint--button { border: 0; background: transparent; padding: 0; cursor: pointer; font-family: inherit; }
.kb-hint--button:hover { text-decoration: underline; }
.kb-hint--idle { color: var(--text-light); }

.export-dropdown { position: relative; }
.export-menu { position: absolute; top: calc(100% + 6px); right: 0; background: var(--bg-paper); border: 1px solid var(--border-medium); border-radius: 12px; box-shadow: var(--shadow-md); min-width: 200px; z-index: 100; overflow: hidden; }
.export-item { padding: 10px 14px; font-size: 12px; cursor: pointer; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid var(--border-light); transition: background 0.15s; }
.export-item:last-child { border-bottom: none; }
.export-item:hover { background: var(--bg-surface); }
.export-badge { font-size: 10px; padding: 2px 8px; border-radius: 100px; background: rgba(217,117,10,0.08); color: var(--accent); font-weight: 600; margin-left: auto; }

/* Toolbar */
.pro-toolbar { display: flex; align-items: center; gap: 16px; padding: 12px 24px; background: var(--bg-surface); border-bottom: 1px solid var(--border-light); z-index: 10; flex-shrink: 0; }
.toolbar-spacer { flex: 1; }
.tabs-container { position: relative; display: flex; align-items: center; background: rgba(0,0,0,0.04); padding: 4px; border-radius: 10px; }
.tab-btn { position: relative; z-index: 2; padding: 6px 16px; font-size: 13px; font-weight: 500; color: var(--text-muted); cursor: pointer; user-select: none; transition: color 0.3s; }
.tab-btn.active { color: var(--text-main); font-weight: 600; }
.tab-indicator { position: absolute; top: 4px; bottom: 4px; left: 0; background: #fff; border-radius: 6px; z-index: 1; box-shadow: 0 2px 8px rgba(0,0,0,0.06); transition: all 0.4s var(--spring-bounce); }

.btn-link { border-color: transparent; color: var(--text-muted); font-weight: 500; gap: 5px; }
.btn-link:hover { color: var(--accent); background: transparent; }

/* Advanced panel */
.advanced-panel { padding: 14px 24px; background: var(--bg-surface); border-bottom: 1px solid var(--border-light); flex-shrink: 0; animation: slideDown 0.25s var(--spring); }
@keyframes slideDown { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
.advanced-grid { display: flex; gap: 14px; align-items: flex-end; }
.advanced-item { display: flex; flex-direction: column; gap: 5px; }
.advanced-item--grow { flex: 1; }
.advanced-label { font-size: 11px; font-weight: 600; color: var(--text-light); text-transform: uppercase; letter-spacing: 0.5px; }
.advanced-select { padding: 7px 28px 7px 10px; border-radius: 8px; border: 1px solid var(--border-medium); background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 10 10'%3E%3Cpath d='M2 4l3 3 3-3' stroke='%23888' stroke-width='1.5' fill='none'/%3E%3C/svg%3E") no-repeat right 8px center; font-size: 13px; font-family: inherit; color: var(--text-main); appearance: none; cursor: pointer; outline: none; }
.advanced-input { padding: 7px 10px; border-radius: 8px; border: 1px solid var(--border-medium); background: #fff; font-size: 13px; font-family: inherit; color: var(--text-main); outline: none; }
.advanced-input::placeholder { color: var(--text-light); }
.advanced-input:focus, .advanced-select:focus { border-color: var(--accent); }

/* Empty state */
.empty-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; background: var(--bg-canvas); }
.empty-icon { width: 80px; height: 80px; border-radius: 20px; background: rgba(217,117,10,0.04); display: flex; align-items: center; justify-content: center; }
.empty-text { font-family: Georgia, serif; font-size: 18px; color: var(--text-muted); }
.empty-sub { font-size: 13px; color: var(--text-light); }
.paste-area { width: 100%; max-width: 480px; background: var(--bg-surface); border: 1px solid var(--border-light); border-radius: 10px; padding: 12px 14px; font-family: var(--font-sans); font-size: 13px; color: var(--text-main); resize: vertical; outline: none; line-height: 1.7; }
.paste-area:focus { border-color: var(--accent); }
.paste-area::placeholder { color: var(--text-light); }

/* Workspace */
.workspace { flex: 1; min-height: 0; display: grid; background: var(--bg-canvas); overflow: hidden; }
.workspace.split-50 { grid-template-columns: 1fr 1fr; }

/* Editor panes */
.editor-pane { display: flex; flex-direction: column; background: var(--bg-paper); min-height: 0; }
.editor-pane.left { border-right: 1px solid var(--border-light); }
.editor-pane.right { border-left: 1px solid var(--border-light); }
.pane-header { padding: 14px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-light); flex-shrink: 0; }
.pane-title { font-size: 12px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px; }
.pane-hint { font-size: 11px; color: var(--text-light); }
.pane-badge { font-size: 11px; font-weight: 600; display: flex; align-items: center; gap: 5px; }
.pane-badge.live { color: var(--accent); }
.pane-badge.live::before { content: ''; width: 6px; height: 6px; background: var(--accent); border-radius: 50%; animation: pulse 1s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }

.editor-content { flex: 1; min-height: 0; padding: 24px 32px; overflow-y: auto; }
.editor-content::-webkit-scrollbar { width: 5px; }
.editor-content::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 3px; }

/* Format paragraphs */
.fp-heading { margin: 16px 0 6px; line-height: 1.3; color: var(--text-main); user-select: none; }
.fp-body { margin: 0 0 6px; line-height: 1.9; color: var(--text-main); outline: none; border-radius: 4px; padding: 4px 8px; margin-left: -8px; margin-right: -8px; transition: background 0.15s; min-height: 1.5em; }
.fp-body[contenteditable="true"]:hover { background: rgba(217,117,10,0.03); }
.fp-body[contenteditable="true"]:focus { background: rgba(217,117,10,0.06); box-shadow: 0 0 0 2px rgba(217,117,10,0.15); outline: none; border-radius: 6px; }

.streaming-hint { font-family: 'Inter', 'PingFang SC', sans-serif; font-size: 12px; color: var(--accent); margin-top: 16px; }

.result-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; gap: 12px; color: var(--text-light); font-size: 13px; }

/* Diff highlights */
:deep(.diff-add) { background: rgba(22, 163, 74, 0.1); color: #166534; padding: 2px 4px; border-radius: 4px; border-bottom: 1px solid rgba(22, 163, 74, 0.2); }
:deep(.diff-del) { background: rgba(220, 38, 38, 0.06); color: #991B1B; text-decoration: line-through; padding: 2px 4px; border-radius: 4px; }

/* Streaming typewriter */
.streaming-text {
  font-family: Georgia, 'Noto Serif SC', serif;
  font-size: 14px; line-height: 1.9; color: var(--text-main);
  white-space: pre-wrap;
}

.streaming-line { margin: 0 0 4px; }

.cursor-blink {
  display: inline-block; width: 2px; height: 16px; background: var(--accent);
  vertical-align: text-bottom; margin-left: 2px;
  animation: blink 0.8s infinite;
}

@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0; } }

/* Capsule toggle */
.capsule-toggle { position: relative; display: inline-flex; border: 1px solid var(--border-medium); border-radius: 100px; overflow: hidden; background: var(--bg-surface); }
.capsule-slider { position: absolute; top: 0; left: 0; width: 50%; height: 100%; background: #141413; border-radius: 100px; transition: transform 0.35s cubic-bezier(0.34,1.56,0.64,1); z-index: 0; }
.capsule-slider.right { transform: translateX(100%); }
.capsule-opt { position: relative; z-index: 1; padding: 5px 16px; font-size: 12px; font-weight: 500; border: none; background: transparent; color: var(--text-muted); cursor: pointer; transition: color 0.25s; font-family: inherit; }
.capsule-opt.active { color: #fff; }
.quota-badge{font-size:11px;color:var(--text-muted);background:var(--bg-surface);padding:4px 12px;border-radius:100px;white-space:nowrap;border:1px solid var(--border-light);}
.quota-badge.quota-low{color:var(--color-danger);border-color:rgba(239,68,68,0.25);background:rgba(239,68,68,0.04);}

.error-toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--color-danger); color: #fff; padding: 10px 24px; border-radius: 10px; font-size: 13px; z-index: 200; }
.warn-toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--accent); color: #fff; padding: 10px 24px; border-radius: 10px; font-size: 13px; cursor: pointer; z-index: 200; }

/* ===== Mobile ===== */
.workspace--mobile {
  display: flex; flex-direction: column;
  flex: 1; min-height: 0; background: var(--bg-canvas); overflow: hidden;
}
.mobile-pane-tabs {
  display: flex; gap: 0;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-paper);
  flex-shrink: 0;
}
.mobile-pane-tabs button {
  flex: 1; padding: 12px 0;
  font-size: 14px; font-weight: 500; color: var(--text-light);
  border-bottom: 2px solid transparent;
  background: none; cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}
.mobile-pane-tabs button.active {
  color: var(--text); border-bottom-color: var(--accent); font-weight: 600;
}
.mobile-pane {
  flex: 1; overflow-y: auto; padding: 16px;
}
.mobile-pane-actions {
  display: flex; justify-content: flex-end;
  padding-bottom: 8px;
}

@media (max-width: 767px) {
  .pro-toolbar {
    flex-wrap: wrap; gap: 8px; padding: 10px 16px;
  }
  .tabs-container {
    width: 100%; overflow-x: auto;
  }
  .app-header {
    padding: 10px 14px;
    flex-wrap: wrap; gap: 8px;
  }
  .app-brand {
    gap: 6px;
  }
  .brand-name {
    font-size: 18px;
  }
  .brand-tag {
    font-size: 9px; padding: 3px 8px;
  }
  .btn-switch {
    font-size: 10px; padding: 4px 8px; margin-left: 0;
  }
  .export-dropdown .btn {
    padding: 5px 8px; font-size: 11px; gap: 4px;
  }
  .export-menu {
    right: 0; top: calc(100% + 6px); bottom: auto;
    min-width: 160px; max-width: calc(100vw - 32px);
  }
  .btn {
    padding: 6px 10px; font-size: 12px;
  }
  .btn-icon {
    width: 32px; height: 32px;
  }
  .quota-badge {
    font-size: 10px; padding: 2px 8px;
  }
  .advanced-panel {
    padding: 12px 16px;
  }
  .advanced-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .advanced-item--grow:last-child {
    grid-column: 1 / -1;
  }
  .app-shell {
    max-width: 100%;
    height: 100vh;
    margin: 0;
    border-radius: 0;
  }
}
</style>
