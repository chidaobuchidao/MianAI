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
      <span class="hero-label">学术润色</span>
      <h1 class="hero-title">学术润色</h1>
      <p class="hero-sub">精准优化论文表达，从语法修正到逻辑强化，全面提升论文质量</p>
    </section>

    <div class="func-chips">
      <button v-for="pt in polishTypes" :key="pt.value"
        class="chip" :class="{ active: polishType === pt.value }"
        @click="polishType = pt.value">{{ pt.label }}</button>
    </div>

    <div class="quick-actions">
      <button class="btn-quick" @click="runFormatScan" :disabled="formatScanning || !sourceText.trim()">
        {{ formatScanning ? '检测中...' : '格式检查' }}
      </button>
      <span v-if="formatIssues.length" class="format-hint">{{ formatIssues.length }} 个格式建议</span>
    </div>

    <section class="editor-area">
      <div class="panel source">
        <div class="panel-header">
          <span>原文输入</span>
          <button class="btn-upload" @click="triggerUpload">上传文档</button>
          <input ref="fileInput" type="file" accept=".docx,.pdf" style="display:none" @change="handleFileUpload" />
        </div>
        <textarea v-model="sourceText" placeholder="粘贴或拖拽上传论文正文..."
          class="editor-textarea"></textarea>
        <span class="char-count">{{ sourceText.length }} 字</span>
      </div>

      <div class="panel result">
        <div class="panel-header">
          <span>润色结果</span>
          <span class="status">{{ isStreaming ? '实时预览' : '就绪' }}</span>
        </div>
        <div class="result-body" :class="{ placeholder: !output && !isStreaming }">
          <template v-if="output">{{ output }}</template>
          <template v-else-if="isStreaming">等待响应...</template>
          <template v-else>输入文本后点击「开始润色」</template>
        </div>
      </div>
    </section>

    <footer class="control-bar">
      <select v-model="taskType">
        <option>章节正文</option><option>摘要</option><option>引言</option><option>结论</option><option>自定义段落</option>
      </select>
      <select v-model="executionMode">
        <option>标准模式</option><option>学术强化</option><option>结构重组</option><option>精炼压缩</option>
      </select>
      <input v-model="topic" placeholder="主题/章节（可选）" class="topic-input" />
      <span class="spacer"></span>
      <button class="btn-primary" @click="startPolish" :disabled="isStreaming || !sourceText.trim()">
        {{ isStreaming ? '润色中...' : '开始润色' }}
      </button>
    </footer>

    <p v-if="error" class="error-msg">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStreamPolish } from '@/composables/useStreamPolish'

const router = useRouter()
const route = useRoute()

const { output, isStreaming, error, startStream } = useStreamPolish()

const sourceText = ref('')
const polishType = ref('full')
const taskType = ref('章节正文')
const executionMode = ref('标准模式')
const topic = ref('')
const showExport = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const formatScanning = ref(false)
const formatIssues = ref<string[]>([])
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

async function runFormatScan() {
  formatScanning.value = true
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/polish/scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify({ text: sourceText.value }),
    })
    const data = await res.json()
    if (data.result?.issues) {
      formatIssues.value = data.result.issues
    }
  } finally { formatScanning.value = false }
}

const polishTypes = [
  { value: 'full', label: '全面润色' },
  { value: 'vocab', label: '词汇优化' },
  { value: 'logic', label: '逻辑强化' },
]

async function startPolish() {
  await startStream('/api/polish/run', {
    text: sourceText.value,
    taskType: taskType.value,
    polishType: polishType.value,
    executionMode: executionMode.value,
    topic: topic.value,
    notes: '',
  })
}
</script>

<style scoped>
.polish-page {
  max-width: 1280px; margin: 0 auto; padding: 24px;
  background: #FDFCFB; min-height: 100vh; font-family: Inter, 'PingFang SC', sans-serif;
}
.page-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.back-btn { width: 32px; height: 32px; border-radius: 8px; border: 1px solid rgba(0,0,0,.06); background: #FDFCFB; cursor: pointer; font-size: 14px; }
.brand { font-family: Georgia, serif; font-size: 16px; font-weight: 600; }
.header-tag { font-size: 10px; font-weight: 600; color: #D9750A; background: rgba(217,117,10,.08); padding: 3px 10px; border-radius: 100px; letter-spacing: 2px; }
.hero { margin-bottom: 16px; }
.hero-accent { width: 28px; height: 4px; background: #D9750A; border-radius: 2px; margin-bottom: 12px; }
.hero-label { font-size: 11px; font-weight: 600; color: #D9750A; letter-spacing: 3px; display: block; margin-bottom: 8px; }
.hero-title { font-family: Georgia, serif; font-size: 28px; font-weight: 600; margin-bottom: 6px; }
.hero-sub { font-size: 13px; color: #4A4A4A; }
.func-chips { display: flex; gap: 8px; margin-bottom: 16px; }
.chip { padding: 8px 16px; border-radius: 10px; border: 1px solid rgba(0,0,0,.06); background: #FDFCFB; font-size: 13px; cursor: pointer; font-family: inherit; }
.chip.active { border-color: #D9750A; box-shadow: 0 0 0 2px rgba(217,117,10,.12); }
.editor-area { display: flex; gap: 0; border: 1px solid rgba(0,0,0,.06); border-radius: 12px; overflow: hidden; min-height: 400px; margin-bottom: 16px; }
.panel { flex: 1; display: flex; flex-direction: column; }
.panel.source { flex: 4.5; border-right: 1px solid rgba(0,0,0,.06); position: relative; }
.panel.result { flex: 5.5; }
.panel-header { padding: 10px 14px; font-size: 11px; font-weight: 600; color: #4A4A4A; border-bottom: 1px solid rgba(0,0,0,.06); display: flex; justify-content: space-between; }
.status { font-weight: 400; color: #888; }
.editor-textarea { flex: 1; border: none; resize: none; padding: 14px; font-family: Georgia, 'Noto Serif SC', serif; font-size: 14px; line-height: 1.8; outline: none; width: 100%; box-sizing: border-box; }
.char-count { position: absolute; bottom: 8px; right: 14px; font-size: 11px; color: #888; }
.result-body { flex: 1; padding: 14px; font-family: Georgia, 'Noto Serif SC', serif; font-size: 14px; line-height: 1.8; overflow-y: auto; white-space: pre-wrap; }
.result-body.placeholder { display: flex; align-items: center; justify-content: center; color: #888; font-family: Inter, sans-serif; font-size: 13px; }
.control-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.control-bar select { padding: 7px 28px 7px 10px; border-radius: 8px; border: 1px solid rgba(0,0,0,.06); font-size: 12px; background: #FDFCFB; font-family: inherit; }
.topic-input { padding: 7px 10px; border-radius: 8px; border: 1px solid rgba(0,0,0,.06); font-size: 12px; flex: 1; max-width: 180px; font-family: inherit; }
.spacer { flex: 1; }
.btn-primary { padding: 8px 28px; border-radius: 10px; border: none; background: #D9750A; color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; font-family: inherit; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-upload { font-size: 11px; padding: 3px 10px; border-radius: 6px; border: 1px solid rgba(0,0,0,.06); background: #FDFCFB; cursor: pointer; font-family: inherit; }
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
.quick-actions { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.btn-quick { padding: 6px 16px; border-radius: 8px; border: 1px solid rgba(0,0,0,.06); background: #F7F7F5; font-size: 12px; cursor: pointer; font-family: inherit; }
.btn-quick:hover { background: #EEECE5; }
.format-hint { font-size: 12px; color: #D9750A; }
</style>
