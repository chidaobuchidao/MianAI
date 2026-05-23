<template>
  <div class="polish-page">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">&larr;</button>
      <span class="brand">Mianmian.</span>
      <span class="header-tag">论文工具</span>
      <div class="header-right">
        <div class="export-dropdown">
          <button class="btn-export" @click="showExport = !showExport">导出 ▾</button>
          <div v-if="showExport" class="export-menu">
            <div class="export-menu-item" @click="exportDoc('preserve')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              保留原格式导出
              <span class="badge">推荐</span>
            </div>
            <div class="export-menu-item" @click="exportDoc('standard')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              标准 Word 导出
            </div>
            <div class="export-menu-item" @click="copyOutput">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
              复制到剪贴板
            </div>
          </div>
        </div>
      </div>
    </header>

    <nav class="sub-nav">
      <a href="#" @click.prevent="router.replace('/paper-tools/polish')" class="sub-nav-item" :class="{ active: route.path === '/paper-tools/polish' }">学术润色</a>
      <a href="#" @click.prevent="router.replace('/paper-tools/ai-reduce')" class="sub-nav-item" :class="{ active: route.path === '/paper-tools/ai-reduce' }">降AI</a>
      <a href="#" @click.prevent="router.replace('/paper-tools/plagiarism-reduce')" class="sub-nav-item" :class="{ active: route.path === '/paper-tools/plagiarism-reduce' }">降查重</a>
    </nav>

    <section class="hero">
      <div class="hero-accent"></div>
      <span class="hero-label">降AI检测</span>
      <h1 class="hero-title">降AI检测</h1>
      <p class="hero-sub">识别并弱化AI写作痕迹，使文本风格更接近真实学者写作</p>
    </section>

    <!-- Risk Score Card -->
    <div v-if="scanResult" class="risk-card">
      <div class="risk-header">
        <span>AI 痕迹风险</span>
        <span class="risk-badge" :class="riskClass">{{ scanResult.riskLevel }} · {{ scanResult.score }}%</span>
      </div>
      <div class="risk-bar">
        <div class="risk-fill" :class="riskClass" :style="{ width: scanResult.score + '%' }"></div>
      </div>
      <ul class="risk-issues">
        <li v-for="(f, i) in scanResult.features" :key="i">{{ f }}</li>
      </ul>
    </div>

    <section class="editor-area">
      <div class="panel source">
        <div class="panel-header">
          <span>原文输入</span>
          <button class="btn-upload" @click="triggerUpload">上传文档</button>
          <input ref="fileInput" type="file" accept=".docx,.pdf" style="display:none" @change="handleFileUpload" />
        </div>
        <textarea v-model="sourceText" placeholder="粘贴待检测的论文正文..." class="editor-textarea"></textarea>
        <span class="char-count">{{ sourceText.length }} 字</span>
      </div>
      <div class="panel result">
        <div class="panel-header">
          <span>改写结果</span>
          <span class="status">{{ isStreaming ? '实时预览' : scanning ? '扫描中...' : '就绪' }}</span>
        </div>
        <div class="result-body" :class="{ placeholder: !output && !isStreaming }">
          <template v-if="output">{{ output }}</template>
          <template v-else-if="isStreaming">等待响应...</template>
          <template v-else>点击「扫描痕迹」先检测 → 选择改写强度 → 点击「开始改写」</template>
        </div>
      </div>
    </section>

    <footer class="control-bar">
      <button class="btn-secondary" @click="scanTraces" :disabled="scanning || !sourceText.trim()">
        {{ scanning ? '扫描中...' : '扫描痕迹' }}
      </button>
      <span class="sep">|</span>
      <span class="mode-label">改写强度:</span>
      <select v-model="mode">
        <option value="light">轻度去痕</option>
        <option value="deep">深度重构</option>
        <option value="academic">学术拟合</option>
      </select>
      <span class="spacer"></span>
      <button class="btn-primary" @click="startRewrite" :disabled="isStreaming || !sourceText.trim()">
        {{ isStreaming ? '改写中...' : '开始改写' }}
      </button>
    </footer>
    <p v-if="error" class="error-msg">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStreamPolish } from '@/composables/useStreamPolish'

const router = useRouter()
const route = useRoute()

const { output, isStreaming, error, startStream } = useStreamPolish()

const sourceText = ref('')
const mode = ref('light')
const scanning = ref(false)
const scanResult = ref<{ score: number; riskLevel: string; features: string[] } | null>(null)
const showExport = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadId = ref('')
const paragraphCount = ref(0)

function triggerUpload() { fileInput.value?.click() }
async function handleFileUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const token = localStorage.getItem('token') || ''
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await fetch('/api/paper/upload', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: formData,
    })
    const data = await res.json()
    if (data.fullText) {
      sourceText.value = data.fullText
      uploadId.value = data.uploadId
      paragraphCount.value = data.paragraphCount
    }
  } catch (e) {
    console.error('Upload failed:', e)
  }
}

function exportDoc(_mode: string) {
  showExport.value = false
  // TODO: integrate with backend export API
}

async function copyOutput() {
  showExport.value = false
  if (output.value) {
    await navigator.clipboard.writeText(output.value)
  }
}

const riskClass = computed(() =>
  scanResult.value?.riskLevel === '高风险' ? 'high' :
  scanResult.value?.riskLevel === '中风险' ? 'medium' : 'low'
)

async function scanTraces() {
  scanning.value = true
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/ai-reduce/scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({ text: sourceText.value }),
    })
    const data = await res.json()
    scanResult.value = data.result
  } finally { scanning.value = false }
}

async function startRewrite() {
  await startStream('/api/ai-reduce/rewrite', { text: sourceText.value, mode: mode.value })
}
</script>

<style scoped>
.polish-page { max-width: 1280px; margin: 0 auto; padding: 24px; background: #FDFCFB; min-height: 100vh; font-family: Inter, 'PingFang SC', sans-serif; }
.page-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.back-btn { width: 32px; height: 32px; border-radius: 8px; border: 1px solid rgba(0,0,0,.06); background: #FDFCFB; cursor: pointer; }
.brand { font-family: Georgia, serif; font-size: 16px; font-weight: 600; }
.header-tag { font-size: 10px; font-weight: 600; color: #D9750A; background: rgba(217,117,10,.08); padding: 3px 10px; border-radius: 100px; letter-spacing: 2px; }
.hero { margin-bottom: 16px; }
.hero-accent { width: 28px; height: 4px; background: #D9750A; border-radius: 2px; margin-bottom: 12px; }
.hero-label { font-size: 11px; font-weight: 600; color: #D9750A; letter-spacing: 3px; display: block; margin-bottom: 8px; }
.hero-title { font-family: Georgia, serif; font-size: 28px; font-weight: 600; margin-bottom: 6px; }
.hero-sub { font-size: 13px; color: #4A4A4A; }
.editor-area { display: flex; gap: 0; border: 1px solid rgba(0,0,0,.06); border-radius: 12px; overflow: hidden; min-height: 350px; margin-bottom: 16px; }
.panel { flex: 1; display: flex; flex-direction: column; }
.panel.source { flex: 4.5; border-right: 1px solid rgba(0,0,0,.06); position: relative; }
.panel.result { flex: 5.5; }
.panel-header { padding: 10px 14px; font-size: 11px; font-weight: 600; color: #4A4A4A; border-bottom: 1px solid rgba(0,0,0,.06); display: flex; justify-content: space-between; }
.status { font-weight: 400; color: #888; }
.editor-textarea { flex: 1; border: none; resize: none; padding: 14px; font-family: Georgia, 'Noto Serif SC', serif; font-size: 14px; line-height: 1.8; outline: none; width: 100%; box-sizing: border-box; }
.char-count { position: absolute; bottom: 8px; right: 14px; font-size: 11px; color: #888; }
.result-body { flex: 1; padding: 14px; font-family: Georgia, 'Noto Serif SC', serif; font-size: 14px; line-height: 1.8; overflow-y: auto; white-space: pre-wrap; }
.result-body.placeholder { display: flex; align-items: center; justify-content: center; color: #888; font-family: Inter, sans-serif; font-size: 13px; }
.risk-card { margin-bottom: 16px; padding: 16px 20px; background: #FDFCFB; border: 1px solid rgba(0,0,0,.06); border-radius: 14px; }
.risk-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-size: 13px; font-weight: 600; }
.risk-badge { padding: 6px 16px; border-radius: 100px; font-size: 12px; font-weight: 600; color: #fff; }
.risk-badge.high { background: #EF4444; }
.risk-badge.medium { background: #F59E0B; }
.risk-badge.low { background: #22C55E; }
.risk-bar { height: 6px; background: #F7F7F5; border-radius: 3px; overflow: hidden; margin-bottom: 10px; }
.risk-fill { height: 100%; border-radius: 3px; }
.risk-fill.high { background: #EF4444; }
.risk-fill.medium { background: #F59E0B; }
.risk-fill.low { background: #22C55E; }
.risk-issues { list-style: none; font-size: 12px; color: #4A4A4A; }
.risk-issues li { padding: 4px 0; }
.control-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.control-bar select { padding: 7px 10px; border-radius: 8px; border: 1px solid rgba(0,0,0,.06); font-size: 12px; background: #FDFCFB; font-family: inherit; }
.sep { color: #888; font-size: 12px; }
.mode-label { font-size: 12px; color: #4A4A4A; }
.spacer { flex: 1; }
.btn-primary { padding: 8px 28px; border-radius: 10px; border: none; background: #D9750A; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; font-family: inherit; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-secondary { padding: 8px 20px; border-radius: 10px; border: 1px solid rgba(0,0,0,.06); background: #F7F7F5; color: #141413; font-size: 13px; font-weight: 500; cursor: pointer; font-family: inherit; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
.error-msg { color: #EF4444; font-size: 12px; margin-top: 8px; }
.sub-nav { display: flex; gap: 4px; margin-bottom: 20px; background: #F7F7F5; border-radius: 10px; padding: 4px; width: fit-content; }
.sub-nav-item { padding: 6px 16px; border-radius: 8px; font-size: 13px; color: #4A4A4A; text-decoration: none; transition: all 0.2s; }
.sub-nav-item:hover { color: #141413; }
.sub-nav-item.active { background: #FDFCFB; color: #141413; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.header-right { margin-left: auto; display: flex; align-items: center; }
.export-dropdown { position: relative; }
.btn-export { font-size: 11px; padding: 4px 14px; border-radius: 6px; border: 1px solid rgba(0,0,0,.06); background: #FDFCFB; cursor: pointer; font-family: inherit; color: #4A4A4A; }
.export-menu { position: absolute; top: 100%; right: 0; margin-top: 4px; background: #FDFCFB; border: 1px solid rgba(0,0,0,.1); border-radius: 10px; box-shadow: 0 8px 24px rgba(0,0,0,.1); min-width: 180px; z-index: 100; overflow: hidden; }
.export-menu-item { padding: 10px 14px; font-size: 12px; cursor: pointer; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid rgba(0,0,0,.06); }
.export-menu-item:last-child { border-bottom: none; }
.export-menu-item:hover { background: #F7F7F5; }
.export-menu-item .badge { font-size: 10px; padding: 2px 8px; border-radius: 100px; background: rgba(217,117,10,.08); color: #D9750A; font-weight: 600; margin-left: auto; }
</style>
