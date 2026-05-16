<template>
  <div class="interview" :class="{ 'interview--desktop': isDesktop }">
    <!-- ====== Position Selection ====== -->
    <div class="position-screen" v-if="!started && !finished">
      <div class="pos-card animate-scale-in">
        <span class="pos-icon">
          <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--text-main)" stroke-width="1.2"><rect x="3" y="3" width="18" height="14" rx="3"/><circle cx="8.5" cy="8.5" r="1.5"/><circle cx="15.5" cy="8.5" r="1.5"/><path d="M8 13c0 0 1.5 2 4 2s4-2 4-2"/><line x1="12" y1="17" x2="12" y2="20"/><line x1="9" y1="20" x2="15" y2="20"/></svg>
        </span>
        <h2 class="pos-title">AI 模拟面试</h2>
        <p class="pos-desc">选择目标岗位，AI 面试官将进行一场真实技术面试</p>
        <div class="model-bar">
          <span class="model-label">模型</span>
          <div class="capsule-toggle">
            <div class="capsule-slider" :class="{ right: interviewModel === 'deepseek-v4-pro' }" />
            <button class="capsule-opt" :class="{ active: interviewModel === 'deepseek-v4-flash' }" @click="interviewModel = 'deepseek-v4-flash'">Flash</button>
            <button class="capsule-opt" :class="{ active: interviewModel === 'deepseek-v4-pro' }" @click="interviewModel = 'deepseek-v4-pro'">Pro</button>
          </div>
        </div>
        <div class="pos-grid">
          <button class="pos-btn card-hover" v-for="p in positions" :key="p" @click="startInterview(p)" :disabled="loading">
            <span class="pos-icon-box" v-html="getPosIcon(p)" />
            <span class="pos-name">{{ p }}</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#CCC" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>
        <p class="pos-loading" v-if="loading">正在启动 AI 面试官...</p>
      </div>
    </div>

    <!-- ====== PC: Left Sidebar ====== -->
    <aside class="sidebar" v-if="isDesktop">
      <div class="sidebar__head">
        <span class="sidebar__brand">Mianmian.</span>
        <span class="sidebar__subtitle">{{ selectedPosition }}面试</span>
      </div>
      <div class="sidebar__body no-scrollbar">
        <span class="sidebar__section-label">Interview Progress</span>
        <div class="progress-tree">
          <div class="progress-tree__line" />
          <div
            class="progress-tree__item"
            v-for="(item, i) in progressItems"
            :key="i"
            :class="{ 'progress-tree__item--done': item.done, 'progress-tree__item--active': item.active }"
          >
            <div class="progress-tree__dot">
              <svg v-if="item.done" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#FFF" stroke-width="3">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </div>
            <div>
              <span class="progress-tree__name">{{ item.name }}</span>
              <span class="progress-tree__status">{{ item.status }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- ====== Chat Area ====== -->
    <main class="chat-main">
      <!-- Top bar -->
      <div class="chat-topbar glass">
        <button class="chat-topbar__back" @click="endInterview" v-if="!isDesktop">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <div class="chat-topbar__center">
          <span class="chat-topbar__title" v-if="!isDesktop">AI 面试官</span>
          <span class="chat-topbar__title" v-else>AI 面试官 · Kevin</span>
          <div class="chat-topbar__status">
            <span class="chat-topbar__dot" />
            <span>在线</span>
          </div>
        </div>
        <button class="end-capsule" @click="endInterview">
          <span class="end-capsule__text">结束</span>
          <span class="end-capsule__x">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </span>
        </button>
      </div>

      <!-- Messages -->
      <div class="chat-messages no-scrollbar" ref="msgContainer">
        <div
          class="msg-group"
          v-for="(m, i) in messages"
          :key="i"
        >
          <!-- AI Message -->
          <div class="msg-row msg-row--ai" v-if="m.role === 'ai'">
            <img
              class="msg-avatar"
              src="https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=64&h=64&q=80"
              alt=""
            />
            <div class="msg-body">
              <span class="msg-role">Interviewer</span>
              <div class="msg-bubble msg-bubble--ai">
                <div class="msg-text" v-html="renderContent(m.content)" />
                <CodeBlock v-if="m.code" :filename="m.codeFile" :language="m.codeLang">
                  {{ m.code }}
                </CodeBlock>
              </div>
            </div>
          </div>

          <!-- User Message -->
          <div class="msg-row msg-row--user" v-else>
            <div class="msg-bubble msg-bubble--user">
              <span class="msg-text msg-text--user">{{ m.content }}</span>
            </div>
          </div>
        </div>

        <!-- Thinking skeleton -->
        <div class="msg-row msg-row--ai" v-if="loading">
          <img
            class="msg-avatar"
            src="https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=64&h=64&q=80"
            alt=""
          />
          <div class="msg-body">
            <span class="msg-role">Interviewer</span>
            <div class="msg-bubble msg-bubble--ai">
              <SkeletonBar :lines="3" :widths="['80%', '55%', '65%']" />
              <div class="msg-thinking">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#AAA" stroke-width="2">
                  <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
                </svg>
                <span>正在分析你的回答...</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Capsule input pill -->
      <div class="input-zone" v-if="!finished">
        <!-- Keyboard mode -->
        <div class="input-pill" v-if="inputMode === 'keyboard'">
          <button class="pill-toggle" @click="inputMode = 'voice'" title="切换到语音">
            <span class="pill-toggle__icon pill-toggle__icon--default">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
            </span>
            <span class="pill-toggle__icon pill-toggle__icon--hover">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="4" width="20" height="16" rx="2"/><line x1="6" y1="8" x2="18" y2="8"/><line x1="6" y1="12" x2="18" y2="12"/><line x1="8" y1="16" x2="16" y2="16"/></svg>
            </span>
          </button>
          <textarea
            class="pill-input"
            v-model="inputText"
            placeholder="输入你的回答..."
            :disabled="loading"
            rows="1"
            @keydown.enter.exact.prevent="sendAnswer()"
          />
          <button class="pill-send" @click="sendAnswer()" :disabled="loading || !inputText.trim()" title="发送">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="12 5 19 12 12 19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>
        </div>

        <!-- Voice mode -->
        <div class="input-pill input-pill--voice" v-else>
          <button class="pill-toggle" @click="inputMode = 'keyboard'" title="切换到键盘">
            <span class="pill-toggle__icon pill-toggle__icon--default">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="4" width="20" height="16" rx="2"/><line x1="6" y1="8" x2="18" y2="8"/><line x1="6" y1="12" x2="18" y2="12"/><line x1="8" y1="16" x2="16" y2="16"/></svg>
            </span>
            <span class="pill-toggle__icon pill-toggle__icon--hover">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>
            </span>
          </button>
          <div class="pill-voice" v-if="!recording" @click="startRecording">
            <span class="pill-voice__hint">轻触开始录音</span>
          </div>
          <div class="pill-voice pill-voice--active" v-else>
            <button class="pill-voice__cancel" @click="cancelRecording">取消</button>
            <div class="pill-voice__waves">
              <span class="wave-bar" v-for="n in 5" :key="n" />
            </div>
            <button class="pill-voice__send" @click="stopRecording">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="12 5 19 12 12 19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- ====== PC: Right Code Editor ====== -->
    <aside class="code-panel" v-if="isDesktop && currentCode">
      <div class="code-panel__head">
        <div class="code-panel__dots">
          <span class="code-panel__dot code-panel__dot--red" />
          <span class="code-panel__dot code-panel__dot--yellow" />
          <span class="code-panel__dot code-panel__dot--green" />
        </div>
        <span class="code-panel__filename">AQS_Source.java</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#888" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="9" y1="3" x2="9" y2="21"/>
        </svg>
      </div>
      <div class="code-panel__body no-scrollbar">
<pre><code>{{ currentCode }}</code></pre>
      </div>
    </aside>

    <!-- Finished overlay -->
    <div class="finish-overlay" v-if="finished">
      <div class="finish-card animate-scale-in">
        <span class="finish-card__icon">
          <svg v-if="(reportScore ?? 0) >= 7" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--color-success)" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><polyline points="8 13 11 16 16 8"/></svg>
          <svg v-else width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
        </span>
        <h2 class="finish-card__title">面试完成</h2>
        <button class="finish-card__btn" @click="goReport">查看面试报告</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { get, post } from '@/utils/request'
import { marked } from 'marked'
import CodeBlock from '@/components/CodeBlock.vue'
import SkeletonBar from '@/components/SkeletonBar.vue'

interface Message {
  role: 'ai' | 'user'
  content: string
  code?: string
  codeFile?: string
  codeLang?: string
}

interface ProgressItem {
  name: string
  status: string
  done: boolean
  active: boolean
}

interface InterviewSession {
  sessionId: number
  question: string
  stage: string
  codeSnippet?: string
  codeLang?: string
  codeFile?: string
  progress?: { stage: string; completed: boolean }[]
}

const router = useRouter()
const isDesktop = ref(window.innerWidth > 768)
const started = ref(false)
const messages = ref<Message[]>([])
const loading = ref(false)
const recording = ref(false)
const finished = ref(false)
const sessionId = ref(0)
const currentCode = ref('')
const inputText = ref('')
const inputMode = ref<'keyboard' | 'voice'>('keyboard')
const msgContainer = ref<HTMLElement>()

const positions = ['Java 后端开发', '前端开发工程师', '算法工程师', '数据分析师', 'DevOps 工程师']
const interviewModel = ref('deepseek-v4-flash')
const selectedPosition = ref('')
const progressItems = ref<ProgressItem[]>([])

function getPosIcon(p: string): string {
  const icons: Record<string, string> = {
    'Java 后端开发': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#D9750A" stroke-width="1.5"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>',
    '前端开发工程师': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><circle cx="12" cy="12" r="2"/><path d="M16.24 7.76a6 6 0 0 1 0 8.49m-8.48 0a6 6 0 0 1 0-8.49m11.31-2.82a10 10 0 0 1 0 14.14m-14.14 0a10 10 0 0 1 0-14.14"/></svg>',
    '算法工程师': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="8" y1="6" x2="8" y2="3"/></svg>',
    '数据分析师': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>',
    'DevOps 工程师': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4A4A4A" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>'
  }
  return icons[p] || icons['DevOps 工程师']
}

// ===== Interview flow =====
async function startInterview(position: string) {
  loading.value = true
  selectedPosition.value = position
  try {
    // Read resumeId from localStorage if coming from resume report
    const resumeData = localStorage.getItem('resumeForInterview')
    let resumeId: number | undefined
    if (resumeData) {
      try { resumeId = JSON.parse(resumeData).resumeId } catch {}
      localStorage.removeItem('resumeForInterview')
    }
    const res = await post<InterviewSession>('/api/interview/start', {
      position,
      model: interviewModel.value,
      resumeId: resumeId || undefined
    })
    const session = res.data
    sessionId.value = session.sessionId
    currentCode.value = session.codeSnippet || ''
    messages.value = [{
      role: 'ai',
      content: session.question,
      code: session.codeSnippet,
      codeFile: session.codeFile,
      codeLang: session.codeLang
    }]
    if (session.progress) {
      progressItems.value = session.progress.map((p, i) => ({
        name: p.stage,
        status: p.completed ? 'Completed' : (i === 0 ? 'In Progress...' : 'Pending'),
        done: p.completed,
        active: i === 0 && !p.completed
      }))
    }
    started.value = true
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '启动失败'
    alert('面试启动失败：' + msg)
  } finally {
    loading.value = false
  }
}

async function sendAnswer() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  loading.value = true

  try {
    const token = localStorage.getItem('token') || ''
    const response = await fetch(`/api/interview/${sessionId.value}/answer/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ answer: text })
    })

    const reader = response.body?.getReader()
    if (!reader) throw new Error('No stream')

    const decoder = new TextDecoder()
    let aiContent = ''
    let currentEvent = ''
    let buffer = ''
    let endDetected = false

    // Add placeholder AI message
    const aiMsgIdx = messages.value.length
    messages.value.push({ role: 'ai', content: '' })

    const processSSE = async () => {
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''
      for (const part of parts) {
        const lines = part.split('\n')
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            if (currentEvent === 'token') {
              aiContent += data
              // Detect [面试结束] inline — backup trigger
              if (aiContent.includes('[面试结束]')) {
                const match = aiContent.match(/\[面试结束\]\s*(\{.*\})/s)
                if (match) {
                  let endJson: any = {}
                  try {
                    endJson = JSON.parse(match[1])
                    if (endJson.score) {
                      sessionStorage.setItem('interviewScore', String(endJson.score || ''))
                      sessionStorage.setItem('interviewFeedback', endJson.feedback || '')
                      reportScore.value = endJson.score
                    }
                  } catch {}
                  if (endJson.dimensions) sessionStorage.setItem('interviewDims', JSON.stringify(endJson.dimensions))
                  if (endJson.suggestion) sessionStorage.setItem('interviewSuggestion', endJson.suggestion)
                  // Strip marker from display and trigger navigation
                  const clean = aiContent.replace(/\[面试结束\].*/s, '').trim()
                  messages.value[aiMsgIdx] = { role: 'ai', content: clean || '面试已结束' }
                  loading.value = false
                  await post(`/api/interview/${sessionId.value}/end`).catch(() => {})
                  setTimeout(() => { router.push(`/interview/report?id=${sessionId.value}`) }, 300)
                  endDetected = true
                  return
                }
              }
              const displayContent = aiContent.replace(/\[面试结束\].*/s, '').trim()
              messages.value[aiMsgIdx] = { role: 'ai', content: displayContent }
            } else if (currentEvent === 'finish') {
              try {
                const json = JSON.parse(data)
                if (json.report) {
                  sessionStorage.setItem('interviewScore', String(json.report.score || ''))
                  sessionStorage.setItem('interviewFeedback', json.report.feedback || '')
                  if (json.report.dimensions) sessionStorage.setItem('interviewDims', JSON.stringify(json.report.dimensions))
                  if (json.report.suggestion) sessionStorage.setItem('interviewSuggestion', json.report.suggestion)
                  reportScore.value = json.report.score
                }
                if (json.finished) {
                  loading.value = false
                  await post(`/api/interview/${sessionId.value}/end`).catch(() => {})
                  const cleanContent = aiContent.replace(/\[面试结束\].*/s, '').trim()
                  messages.value[aiMsgIdx] = { role: 'ai', content: cleanContent || '面试已结束' }
                  setTimeout(() => {
                    router.push(`/interview/report?id=${sessionId.value}`)
                  }, 300)
                  endDetected = true
                }
              } catch { /* ignore */ }
            } else if (currentEvent === 'error') {
              messages.value.push({ role: 'ai', content: '⚠️ ' + data })
            }
          }
        }
      }
    }

    while (true) {
      if (endDetected) break
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      await processSSE()
      await nextTick()
      if (endDetected) break
    }
    // Flush remaining buffer
    if (!endDetected) await processSSE()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '请求失败'
    messages.value.push({ role: 'ai', content: '抱歉，请求出错了：' + msg })
  } finally {
    loading.value = false
  }
}

async function endInterview() {
  if (!sessionId.value) { finished.value = true; return }
  try {
    await post(`/api/interview/${sessionId.value}/end`)
  } catch { /* ignore */ }
  finished.value = true
}

function goReport() {
  if (sessionId.value) {
    router.push(`/interview/report?id=${sessionId.value}`)
  } else {
    router.push('/interview/report')
  }
}

// Configure marked for safe, clean rendering
marked.setOptions({
  breaks: true,
  gfm: true
})

function renderContent(text: string): string {
  return marked.parse(text) as string
}
</script>

<style scoped>
/* ===== Layout ===== */
.interview {
  display: flex;
  height: 100vh;
  background: var(--bg-canvas);
  overflow: hidden;
}

/* ===== Sidebar (PC) ===== */
.sidebar {
  width: 280px;
  background: var(--bg-surface);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.sidebar__head {
  padding: 20px;
  border-bottom: 1px solid var(--border-light);
}
.sidebar__brand {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
}
.sidebar__subtitle {
  font-size: 12px;
  color: var(--text-light);
}
.sidebar__body {
  flex: 1;
  padding: 24px 20px;
  overflow-y: auto;
}
.sidebar__section-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-light);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 20px;
  display: block;
}

/* Progress tree */
.progress-tree { position: relative; display: flex; flex-direction: column; gap: 20px; }
.progress-tree__line {
  position: absolute; left: 7px; top: 10px; bottom: 10px;
  width: 2px; background: var(--border-light);
}
.progress-tree__item { display: flex; gap: 12px; align-items: flex-start; position: relative; z-index: 1; }
.progress-tree__dot {
  width: 16px; height: 16px; border-radius: 50%; margin-top: 2px;
  background: #E4E4E4; border: 2px solid var(--bg-surface); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.progress-tree__item--done .progress-tree__dot { background: var(--color-success); }
.progress-tree__item--active .progress-tree__dot {
  background: var(--bg-dark); border-color: var(--bg-surface);
}
.progress-tree__name { font-size: 13px; font-weight: 500; display: block; }
.progress-tree__item--active .progress-tree__name { font-weight: 600; }
.progress-tree__status { font-size: 11px; color: var(--text-light); display: block; margin-top: 2px; }
.progress-tree__item--done .progress-tree__status { color: var(--text-light); }
.progress-tree__item--active .progress-tree__status { color: var(--color-success); }

/* ===== Chat Main ===== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* Top bar */
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  z-index: 10;
  flex-shrink: 0;
}
@media (min-width: 769px) {
  .chat-topbar {
    padding: 16px 24px;
  }
}
.chat-topbar__back {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted);
}
.chat-topbar__center { text-align: center; }
.chat-topbar__title { font-size: 15px; font-weight: 600; }
.chat-topbar__status {
  font-size: 11px; color: var(--color-success);
  display: flex; align-items: center; gap: 5px; justify-content: center;
}
.chat-topbar__dot {
  width: 6px; height: 6px; background: var(--color-success); border-radius: 50%;
}
.end-capsule {
  display: inline-flex; align-items: center; justify-content: center;
  width: 72px; height: 34px; padding: 0;
  border-radius: 100px;
  border: 1px solid var(--border-medium);
  background: var(--bg-paper);
  cursor: pointer;
  position: relative; overflow: hidden;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.end-capsule::before {
  content: ''; position: absolute; inset: 0;
  background: var(--color-danger);
  transform: translateX(-100%);
  border-radius: 100px;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 0;
}
.end-capsule:hover { border-color: var(--color-danger); }
.end-capsule:hover::before { transform: translateX(0); }
.end-capsule__text {
  position: relative; z-index: 1;
  font-size: 13px; font-weight: 500; color: var(--text-muted);
  transition: all 0.25s ease;
  white-space: nowrap;
}
.end-capsule:hover .end-capsule__text {
  opacity: 0;
  transform: scale(0.6);
  position: absolute;
}
.end-capsule__x {
  position: absolute; z-index: 1;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  opacity: 0;
  transform: rotate(-90deg) scale(0.5);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.end-capsule:hover .end-capsule__x {
  opacity: 1;
  transform: rotate(0deg) scale(1);
  position: relative;
}

/* Messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}
@media (min-width: 769px) {
  .chat-messages { padding: 28px 36px; }
}

/* Message rows */
.msg-row { display: flex; gap: 12px; }
.msg-row--ai { align-items: flex-start; }
.msg-row--user { justify-content: flex-end; }
.msg-avatar {
  width: 32px; height: 32px; border-radius: 8px;
  object-fit: cover; border: 1px solid var(--border-light); flex-shrink: 0;
}
.msg-body { flex: 1; min-width: 0; }
.msg-role { font-size: 13px; font-weight: 500; display: block; margin-bottom: 6px; }
.msg-bubble { display: flex; flex-direction: column; gap: 12px; }
.msg-bubble--ai {  }
.msg-bubble--user {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 16px 16px 4px 16px;
  padding: 14px 16px;
  max-width: 85%;
}
.msg-text { font-size: 15px; color: var(--text-muted); line-height: 1.7; }
.msg-text--user { color: var(--text-main); }

/* Markdown rendered content */
.msg-text :deep(h1), .msg-text :deep(h2), .msg-text :deep(h3) {
  font-family: var(--font-serif);
  margin: 16px 0 8px;
  color: var(--text-main);
  font-size: 1.1em; font-weight: 600;
}
.msg-text :deep(p) { margin: 0 0 8px; }
.msg-text :deep(p:last-child) { margin-bottom: 0; }
.msg-text :deep(ul), .msg-text :deep(ol) { margin: 8px 0; padding-left: 20px; }
.msg-text :deep(li) { margin-bottom: 4px; }
.msg-text :deep(strong) { color: var(--text-main); font-weight: 600; }
.msg-text :deep(em) { font-style: italic; }
.msg-text :deep(code) {
  background: rgba(0,0,0,0.06);
  padding: 1px 6px; border-radius: 4px;
  font-family: var(--font-mono); font-size: 0.88em;
  color: var(--text-main);
}
.msg-text :deep(pre) {
  background: var(--bg-dark);
  border-radius: 10px; padding: 14px 16px;
  overflow-x: auto; margin: 10px 0;
}
.msg-text :deep(pre code) {
  background: none; padding: 0; color: #E4E4E4;
  font-size: 12px; line-height: 1.6;
}
.msg-text :deep(blockquote) {
  border-left: 3px solid var(--accent);
  margin: 10px 0; padding: 6px 14px;
  background: rgba(217,117,10,0.04);
  border-radius: 0 6px 6px 0;
  color: var(--text-muted);
}
.msg-text :deep(table) {
  border-collapse: collapse; margin: 10px 0; width: 100%;
  font-size: 13px;
}
.msg-text :deep(th), .msg-text :deep(td) {
  border: 1px solid var(--border-light);
  padding: 6px 10px; text-align: left;
}
.msg-text :deep(th) { background: var(--bg-surface); font-weight: 600; }
.msg-text :deep(hr) {
  border: none; border-top: 1px solid var(--border-light);
  margin: 16px 0;
}

/* Thinking */
.msg-thinking {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: #AAA; margin-top: 6px;
}

/* ===== Code Panel (PC) ===== */
.code-panel {
  width: 450px;
  background: var(--bg-dark);
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--border-medium);
  flex-shrink: 0;
}
.code-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255,255,255,0.05);
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.code-panel__dots { display: flex; gap: 6px; }
.code-panel__dot { width: 12px; height: 12px; border-radius: 50%; }
.code-panel__dot--red    { background: #ED6A5E; }
.code-panel__dot--yellow { background: #F4BF4F; }
.code-panel__dot--green  { background: #61C554; }
.code-panel__filename { font-size: 12px; color: #888; font-family: var(--font-mono); }
.code-panel__body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
.code-panel__body pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.7;
  color: #E4E4E4;
  white-space: pre-wrap;
}
.code-panel__body code { font-family: inherit; }

/* ===== Finished ===== */
.finish-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(20,20,19,0.6);
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.finish-card {
  background: var(--bg-paper);
  border-radius: var(--radius-xl);
  padding: 48px 40px;
  text-align: center;
  box-shadow: var(--shadow-xl);
}
.finish-card__icon { font-size: 48px; display: block; margin-bottom: 16px; }
.finish-card__title {
  font-family: var(--font-serif);
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
}
.finish-card__btn {
  background: var(--bg-dark);
  color: #fff;
  padding: 14px 36px;
  border-radius: var(--radius-lg);
  font-size: 15px;
  font-weight: 500;
  box-shadow: var(--shadow-md);
}
/* ===== Position Selection ===== */
.position-screen {
  position: fixed; inset: 0; z-index: 50;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-canvas);
  padding: 24px;
}
.pos-card {
  max-width: 420px; width: 100%;
  text-align: center;
}
.pos-icon { display: flex; justify-content: center; margin-bottom: 16px; }
.pos-icon svg { display: block; }
.pos-title {
  font-family: var(--font-serif); font-size: 24px;
  font-weight: 600; margin-bottom: 8px;
}
.pos-desc {
  font-size: 14px; color: var(--text-light); line-height: 1.6;
  margin-bottom: 24px;
}
.model-bar {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; margin-bottom: 24px;
}
.model-label { font-size: 13px; color: var(--text-light); }
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
  padding: 6px 18px;
  font-size: 13px; font-weight: 500;
  border: none; background: transparent;
  color: var(--text-muted); cursor: pointer;
  transition: color 0.25s;
}
.capsule-opt.active {
  color: #fff;
}
.pos-grid {
  display: flex; flex-direction: column; gap: 10px;
}
.pos-btn {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 20px;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  font-size: 15px; font-weight: 500;
  color: var(--text-main); cursor: pointer;
  transition: all 0.15s;
}
.pos-btn:hover { border-color: var(--text-main); background: var(--bg-surface); }
.pos-btn:active { transform: scale(0.98); }
.pos-btn:disabled { opacity: 0.5; pointer-events: none; }
.pos-icon-box {
  width: 36px; height: 36px; border-radius: 10px;
  background: var(--bg-surface);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.pos-name { flex: 1; text-align: left; }
.pos-loading {
  margin-top: 20px; font-size: 13px; color: var(--text-light);
  display: flex; align-items: center; justify-content: center; gap: 8px;
}

/* ===== Capsule Input Pill ===== */
.input-zone {
  padding: 12px 20px 28px;
  flex-shrink: 0;
}

.input-pill {
  display: flex; align-items: center;
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: 100px;
  padding: 5px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-pill:focus-within {
  border-color: var(--text-main);
  box-shadow: 0 0 0 3px rgba(20,20,19,0.06);
}
.input-pill--voice {
  padding: 6px 8px;
}

/* Toggle button (left side of pill) */
.pill-toggle {
  width: 38px; height: 38px; border-radius: 50%;
  border: none; background: transparent;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-light); flex-shrink: 0; cursor: pointer;
  position: relative; overflow: hidden;
  transition: background 0.2s, color 0.2s;
}
.pill-toggle:hover {
  background: var(--bg-surface);
  color: var(--text-main);
}
.pill-toggle__icon {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  transition: opacity 0.2s, transform 0.2s;
}
.pill-toggle__icon--hover {
  opacity: 0;
  transform: translateY(4px);
}
.pill-toggle:hover .pill-toggle__icon--default {
  opacity: 0;
  transform: translateY(-4px);
}
.pill-toggle:hover .pill-toggle__icon--hover {
  opacity: 1;
  transform: translateY(0);
}

/* Text input (center of pill) */
.pill-input {
  flex: 1; min-width: 0;
  border: none; background: transparent;
  padding: 8px 6px;
  font-size: 14px; line-height: 1.5;
  outline: none; resize: none; font-family: inherit;
  color: var(--text-main);
}
.pill-input::placeholder { color: var(--text-light); }
.pill-input:disabled { opacity: 0.5; }

/* Send button (right side of pill) */
.pill-send {
  width: 38px; height: 38px; border-radius: 50%;
  border: none; background: var(--bg-dark);
  display: flex; align-items: center; justify-content: center;
  color: #fff; flex-shrink: 0; cursor: pointer;
  transition: background 0.2s, transform 0.15s, box-shadow 0.2s;
}
.pill-send:hover {
  background: var(--accent);
  box-shadow: 0 4px 16px rgba(217,117,10,0.35);
}
.pill-send:active { transform: scale(0.92); }
.pill-send:disabled {
  background: var(--border-medium);
  color: var(--text-light);
  cursor: default; box-shadow: none;
}

/* Voice mode inside pill */
.pill-voice {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  padding: 6px 12px; cursor: pointer;
  border-radius: 100px;
  transition: background 0.15s;
}
.pill-voice:hover { background: var(--bg-surface); }
.pill-voice__hint { font-size: 14px; color: var(--text-light); }

.pill-voice--active {
  justify-content: space-between; cursor: default;
  background: var(--bg-dark); border-radius: 100px;
}
.pill-voice--active:hover { background: var(--bg-dark); }
.pill-voice__cancel {
  color: #888; font-size: 13px; font-weight: 500;
  background: none; border: none; cursor: pointer; padding: 0 8px;
}
.pill-voice__waves {
  display: flex; align-items: center; gap: 4px; height: 28px;
}
.pill-voice__send {
  width: 36px; height: 36px; border-radius: 50%;
  background: #fff; border: none;
  display: flex; align-items: center; justify-content: center;
  color: #141413; cursor: pointer;
  transition: background 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.pill-voice__send:hover {
  background: var(--accent); color: #fff;
  box-shadow: 0 4px 16px rgba(217,117,10,0.4);
}

/* Wave bars inside voice pill */
.pill-voice__waves .wave-bar {
  background: var(--color-success);
}

@media (min-width: 769px) {
  .input-zone { padding: 16px 36px 24px; }
}

</style>
