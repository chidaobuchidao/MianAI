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
          <div v-if="hasOptions" class="capsule-toggle">
            <div class="capsule-slider" :style="{ width: (100 / options.length) + '%', transform: 'translateX(' + (options.findIndex(o => o.id === interviewModel) * 100) + '%)' }" />
            <button v-for="opt in options" :key="opt.id" class="capsule-opt" :class="{ active: interviewModel === opt.id }" @click="selectModel(opt.id)">{{ opt.label }}</button>
          </div>
          <div v-else class="capsule-toggle">
            <span class="capsule-opt active" style="cursor:default;padding:5px 12px;">{{ selectedLabel }}</span>
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
        <p class="pos-error" v-if="errorMsg" @click="errorMsg = ''">{{ errorMsg }}</p>
      </div>
    </div>

    <!-- ====== PC: Left Sidebar ====== -->
    <aside class="sidebar" v-if="isDesktop">
      <div class="sidebar__head">
        <span class="sidebar__brand">Mianmian.</span>
        <span class="sidebar__subtitle">{{ selectedPosition }}面试</span>
      </div>
      <div class="sidebar__body no-scrollbar">
        <span class="sidebar__section-label">
          Interview Progress
          <button v-if="userStore.isAdmin" class="dev-test-btn" @click="startCoding()" title="开发者：直接进入编程环节">[测试]</button>
        </span>
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
              <span class="progress-tree__status">
                <template v-if="item.stage === 'coding' && item.active && !item.done">
                  <button class="progress-tree__btn" @click="startCoding">进入</button>
                </template>
                <template v-else>{{ item.status }}</template>
              </span>
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

      <!-- Coding invite bar -->
      <div class="coding-invite" v-if="showCodingInvite && !finished">
        <span class="coding-invite__text">进入编程实战环节？</span>
        <button class="coding-invite__btn coding-invite__btn--skip" @click="skipCoding">跳过</button>
        <button class="coding-invite__btn coding-invite__btn--enter" @click="enterCoding">进入编程</button>
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
            ref="inputEl"
            class="pill-input"
            v-model="inputText"
            placeholder="输入你的回答..."
            :disabled="loading"
            rows="1"
            @keydown.enter.exact.prevent="sendTextAnswer()"
            @input="autoResizeInput"
          />
          <button class="pill-send" @click="sendTextAnswer()" :disabled="loading || !inputText.trim()" title="发送">
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
    <aside class="code-panel" v-if="isDesktop && showCodePanel" :style="{ width: codePanelWidth + 'px' }">
      <div class="code-panel__resize-handle" @mousedown="startResize" />
      <div class="code-panel__editor-wrap">
        <CodeEditor
          v-model="currentCode"
          :filename="codeFilename || 'Solution.java'"
          :language="codeLanguage"
          :languages="availableLanguages"
          @update:language="switchCodeLanguage"
        />
      </div>
      <div class="code-panel__stdin" v-if="showCodePanel">
        <input
          v-model="codeStdin"
          class="code-panel__stdin-input"
          placeholder="程序输入(stdin) — 每行一个值"
          spellcheck="false"
          @keyup.enter.ctrl="runCode"
        />
      </div>
      <div class="code-panel__toolbar">
        <button
          class="code-panel__run-btn"
          :disabled="codeRunning"
          @click="runCode"
        >
          <svg v-if="codeRunning" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
          </svg>
          <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 3 20 12 6 21 6 3"/></svg>
          {{ codeRunning ? '执行中...' : 'Run Code' }}
        </button>
        <button
          class="code-panel__reset-btn"
          title="重置为原始代码"
          :disabled="codeRunning || codeSubmitting"
          @click="resetCode"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="1 4 1 10 7 10"/>
            <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
          </svg>
          重置
        </button>
        <button
          class="code-panel__submit-btn"
          :disabled="codeSubmitting || !currentCode.trim()"
          @click="submitCode"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          {{ codeSubmitting ? '审查中...' : '提交审查' }}
        </button>
      </div>
      <div class="code-panel__results" v-if="codeResult">
        <div class="code-panel__results-head">
          <span class="code-panel__results-title">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" :stroke="codeResult.run?.code === 0 ? 'var(--color-success)' : 'var(--color-danger)'" stroke-width="2">
              <polyline v-if="codeResult.run?.code === 0" points="20 6 9 17 4 12"/>
              <template v-else>
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </template>
            </svg>
            {{ codeResult.run?.code === 0 ? '执行成功' : '执行出错' }}
          </span>
          <button class="code-panel__results-close" @click="codeResult = null">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="code-panel__results-body">
          <template v-if="codeResult.error">
            <pre class="code-panel__output code-panel__output--error">{{ codeResult.error }}</pre>
          </template>
          <template v-else-if="codeResult.run">
            <pre class="code-panel__output" v-if="codeResult.run.stdout">{{ codeResult.run.stdout }}</pre>
            <pre class="code-panel__output code-panel__output--error" v-if="codeResult.run.stderr">{{ codeResult.run.stderr }}</pre>
            <span class="code-panel__meta">exit code: {{ codeResult.run.code }} | signal: {{ codeResult.run.signal || 'none' }}</span>
          </template>
        </div>
      </div>
    </aside>

    <!-- ====== Mobile: Code FAB ====== -->
    <button
      class="code-fab"
      v-if="!isDesktop && codeProblemText && !showCodePanel && !finished"
      @click="showCodePanel = true"
      title="打开代码编辑器"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
    </button>

    <!-- ====== Mobile: Code Overlay ====== -->
    <div class="code-overlay" v-if="!isDesktop && showCodePanel">
      <div class="code-overlay__head">
        <button class="code-overlay__back" @click="showCodePanel = false">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="code-overlay__title">{{ codeFilename || 'Solution.java' }}</span>
        <div style="width:36px" />
      </div>
      <div class="code-overlay__problem" v-if="codeProblemText">
        <p class="code-overlay__problem-text">{{ codeProblemText }}</p>
      </div>
      <div class="code-overlay__editor-wrap">
        <CodeEditor
          v-model="currentCode"
          :filename="codeFilename || 'Solution.java'"
          :language="codeLanguage"
          :languages="availableLanguages"
          @update:language="switchCodeLanguage"
        />
      </div>
      <div class="code-overlay__stdin">
        <input
          v-model="codeStdin"
          class="code-overlay__stdin-input"
          placeholder="程序输入(stdin) — 每行一个值"
          spellcheck="false"
          @keyup.enter.ctrl="runCode"
        />
      </div>
      <div class="code-overlay__toolbar">
        <button class="code-overlay__run-btn" :disabled="codeRunning" @click="runCode">
          <svg v-if="codeRunning" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
          <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="6 3 20 12 6 21 6 3"/></svg>
          {{ codeRunning ? '执行中...' : 'Run' }}
        </button>
        <button class="code-overlay__reset-btn" title="重置" :disabled="codeRunning || codeSubmitting" @click="resetCode">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
        </button>
        <button class="code-overlay__submit-btn" :disabled="codeSubmitting || !currentCode.trim()" @click="submitCode">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
          {{ codeSubmitting ? '审查中...' : '提交' }}
        </button>
      </div>
      <div class="code-overlay__results" v-if="codeResult">
        <div class="code-overlay__results-head">
          <span class="code-overlay__results-title">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" :stroke="codeResult.run?.code === 0 ? 'var(--color-success)' : 'var(--color-danger)'" stroke-width="2">
              <polyline v-if="codeResult.run?.code === 0" points="20 6 9 17 4 12"/>
              <template v-else><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></template>
            </svg>
            {{ codeResult.run?.code === 0 ? '执行成功' : '执行出错' }}
          </span>
          <button class="code-overlay__results-close" @click="codeResult = null">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="code-overlay__results-body" v-if="codeResult.error">
          <pre class="code-overlay__output code-overlay__output--error">{{ codeResult.error }}</pre>
        </div>
        <div class="code-overlay__results-body" v-else-if="codeResult.run">
          <pre class="code-overlay__output" v-if="codeResult.run.stdout">{{ codeResult.run.stdout }}</pre>
          <pre class="code-overlay__output code-overlay__output--error" v-if="codeResult.run.stderr">{{ codeResult.run.stderr }}</pre>
          <span class="code-overlay__meta">exit code: {{ codeResult.run.code }} | signal: {{ codeResult.run.signal || 'none' }}</span>
        </div>
      </div>
    </div>

    <!-- Finished overlay -->
    <!-- Ending loading overlay -->
    <div class="finish-overlay" v-if="finishing && !finished">
      <div class="finish-card animate-scale-in">
        <div class="loader-dots" style="margin-bottom:20px"><span></span><span></span><span></span></div>
        <h2 class="finish-card__title">正在生成评估报告</h2>
        <p class="finish-card__sub">AI 正在综合分析你的面试表现...</p>
      </div>
    </div>

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
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { post } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { marked } from 'marked'
import CodeBlock from '@/components/CodeBlock.vue'
import CodeEditor from '@/components/CodeEditor.vue'
import SkeletonBar from '@/components/SkeletonBar.vue'
import { getPosIcon } from '@/utils/positionIcons'
import { useInterviewStream, type InterviewMessage, type ReportData, type CodeProblem, type CodingReview } from '@/composables/useInterviewStream'
import { useQuota } from '@/composables/useQuota'
import { useResponsive } from '@/composables/useResponsive'
import { useModelToggle } from '@/composables/useModelToggle'

interface ProgressItem {
  name: string
  status: string
  stage: string
  done: boolean
  active: boolean
}

const INTERVIEW_PIPELINE = [
  { stage: 'intro', name: '自我介绍与破冰' },
  { stage: 'technical', name: '项目/技术面考察' },
  { stage: 'deepdive', name: '项目深度问答' },
  { stage: 'coding', name: '笔试编程环节' },
  { stage: 'summary', name: '面试总结' },
]

function initPipeline() {
  return INTERVIEW_PIPELINE.map((p, i) => ({
    ...p,
    status: i === 0 ? '准备中...' : 'Pending',
    done: false,
    active: i === 0
  }))
}

function advancePipeline(currentStage: string) {
  const idx = INTERVIEW_PIPELINE.findIndex(p => p.stage === currentStage)
  if (idx < 0) return
  const codingIdx = INTERVIEW_PIPELINE.findIndex(p => p.stage === 'coding')
  for (let i = 0; i < progressItems.value.length; i++) {
    if (i < idx) {
      // Don't mark coding as completed if user skipped it
      if (i === codingIdx && !codingCompleted.value) {
        progressItems.value[i].done = false
        progressItems.value[i].active = false
        progressItems.value[i].status = 'Skipped'
      } else {
        progressItems.value[i].done = true
        progressItems.value[i].active = false
        progressItems.value[i].status = 'Completed'
      }
    } else if (i === idx) {
      progressItems.value[i].done = false
      progressItems.value[i].active = true
      progressItems.value[i].status = 'In Progress...'
    } else {
      progressItems.value[i].done = false
      progressItems.value[i].active = false
      progressItems.value[i].status = 'Pending'
    }
  }
}

function startCoding() {
  advancePipeline('coding')
  showCodePanel.value = true
  sendAnswer('[进入编程环节]')
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
const { isDesktop } = useResponsive()
const userStore = useUserStore()
const started = ref(false)
const messages = ref<InterviewMessage[]>([])
const loading = ref(false)
const errorMsg = ref('')
const recording = ref(false)
const finished = ref(false)
const finishing = ref(false)
const sessionId = ref(0)
const currentCode = ref('')
const codeFilename = ref('')
const codeLanguage = ref('java')
const codeStdin = ref('')
const showCodePanel = ref(false)
const showCodingInvite = ref(false)
const codeRunning = ref(false)
const codeSubmitting = ref(false)
const codeResult = ref<any>(null)
const SUPPORTED_LANGUAGES = ['java', 'python', 'javascript', 'cpp', 'go']
const allTemplates = ref<Record<string, string>>({})
const availableLanguages = ref<string[]>(SUPPORTED_LANGUAGES)
const codePanelWidth = ref(450)
const inputText = ref('')
const inputMode = ref<'keyboard' | 'voice'>('keyboard')
const inputEl = ref<HTMLTextAreaElement>()
const codeProblemText = ref('')

function autoResizeInput() {
  const el = inputEl.value
  if (!el) return
  // Reset to single-line baseline, then grow to fit content (max 5 lines)
  el.style.height = 'auto'
  const lineHeight = parseFloat(getComputedStyle(el).lineHeight) || 21
  const maxHeight = lineHeight * 5 + 16 // 5 lines + vertical padding
  el.style.height = Math.min(el.scrollHeight, maxHeight) + 'px'
}

const { currentModel: interviewModel, options, hasOptions, selectedLabel, selectModel } = useModelToggle()
const positions = ['Java 后端开发', '前端开发工程师', '算法工程师', '数据分析师', 'DevOps 工程师']
const selectedPosition = ref('')
const progressItems = ref<ProgressItem[]>([])

// ===== Interview flow =====
async function startInterview(position: string) {
  if (loading.value) return  // 防止重复点击
  // Pre-check quota before API call
  await fetchQuota(true)
  const needed = interviewModel.value.includes('pro') ? 2 : 1
  const check = checkQuota(needed)
  if (!check.ok) { errorMsg.value = check.msg!; return }

  loading.value = true
  errorMsg.value = ''
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
    if (!session) {
      errorMsg.value = '启动面试失败，请重试'
      loading.value = false
      return
    }
    sessionId.value = session.sessionId
    currentCode.value = session.codeSnippet || ''
    messages.value = [{
      role: 'ai',
      content: session.question,
      code: session.codeSnippet,
      codeFile: session.codeFile,
      codeLang: session.codeLang
    }]
    progressItems.value = initPipeline()
    roundCount.value = 0
    codingCompleted.value = false
    // Clear any stale report data from previous sessions
    sessionStorage.removeItem('interviewScore')
    sessionStorage.removeItem('interviewFeedback')
    sessionStorage.removeItem('interviewDims')
    sessionStorage.removeItem('interviewSuggestion')
    sessionStorage.removeItem('interviewCodingReview')
    codeProblemText.value = ''
    fetchQuota(true).catch(() => {})
    started.value = true
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '启动失败'
  } finally {
    loading.value = false
  }
}

// ===== SSE stream (composable) =====
async function handleFinish(data: ReportData) {
  advancePipeline('summary')
  for (const item of progressItems.value) { item.done = true; item.active = false; item.status = 'Completed' }

  // 面试报告在进入笔试时已由后台生成存入 DB，报告页从 API 读取
  // 笔试报告也在后端 finish handler 中存入了 DB
  // sessionStorage 仅作为缓存
  if (data.score != null) {
    sessionStorage.setItem('interviewScore', String(data.score))
  }
  if (data.feedback) {
    sessionStorage.setItem('interviewFeedback', data.feedback)
  }
  if (data.dimensions) {
    sessionStorage.setItem('interviewDims', JSON.stringify(data.dimensions))
  }
  if (data.suggestion) {
    sessionStorage.setItem('interviewSuggestion', data.suggestion)
  }
  router.push(`/interview/report?id=${sessionId.value}`)
}

function handleCodingInvite() {
  showCodingInvite.value = true
}

function handleCodingFinish(data: CodingReview) {
  sessionStorage.setItem('interviewCodingReview', JSON.stringify(data))
  codingCompleted.value = true
  // Replace loading card with done message
  const idx = messages.value.findIndex(m => m.content === '__CODING_REVIEW_LOADING__')
  if (idx !== -1) {
    messages.value[idx] = { ...messages.value[idx], content: '__CODING_REVIEW_DONE__' }
  }
  codeSubmitting.value = false
}

/** End the interview and save the report to sessionStorage. Called BEFORE entering or skipping coding. */
async function endInterviewAndSaveReport() {
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch(`/api/interview/${sessionId.value}/end`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
    })
    const result = await res.json()
    if (result.data?.report) {
      const r = result.data.report
      sessionStorage.setItem('interviewScore', String(r.score ?? ''))
      sessionStorage.setItem('interviewFeedback', r.feedback || '')
      if (r.dimensions) sessionStorage.setItem('interviewDims', JSON.stringify(r.dimensions))
      if (r.suggestion) sessionStorage.setItem('interviewSuggestion', r.suggestion)
    }
  } catch { /* end() may have been called already */ }
}

function enterCoding() {
  showCodingInvite.value = false
  advancePipeline('coding')
  showCodePanel.value = true
  // 后端 answerStream 收到 [进入编程环节] 时会自动后台生成面试报告
  sendAnswer('[进入编程环节]')
}

async function skipCoding() {
  showCodingInvite.value = false
  loading.value = true
  await endInterviewAndSaveReport()
  loading.value = false
  advancePipeline('summary')
  for (const item of progressItems.value) { item.done = true; item.active = false; item.status = 'Completed' }
  router.push(`/interview/report?id=${sessionId.value}`)
}

function handleCodeProblem(data: CodeProblem) {
  showCodePanel.value = true
  codeProblemText.value = data.description || ''
  const dbTemplates = data.templates || {}
  // Merge DB templates with default stubs for unsupported languages
  allTemplates.value = {
    java: dbTemplates.java || defaultStub('java'),
    python: dbTemplates.python || defaultStub('python'),
    javascript: dbTemplates.javascript || defaultStub('javascript'),
    cpp: dbTemplates.cpp || defaultStub('cpp'),
    go: dbTemplates.go || defaultStub('go')
  }
  currentCode.value = allTemplates.value[data.language || 'java']
  codeFilename.value = data.title + extForLang(data.language || 'java')
  codeLanguage.value = data.language || 'java'
  codeStdin.value = data.testCases?.[0]?.input || ''
  messages.value = [...messages.value, {
    role: 'ai',
    content: `**${data.title}**\n\n${data.description}`
  }]
}

const LANG_META: Record<string, { stub: string; ext: string }> = {
  java:       { ext: '.java', stub: 'public class Solution {\n    public void solve() {\n        // TODO: 实现代码\n    }\n\n    public static void main(String[] args) {\n        new Solution().solve();\n    }\n}' },
  python:     { ext: '.py',  stub: 'def solve():\n    # TODO: 实现代码\n    pass\n\nif __name__ == "__main__":\n    solve()' },
  javascript: { ext: '.js',  stub: 'function solve() {\n    // TODO: 实现代码\n}\n\nconsole.log(solve());' },
  cpp:        { ext: '.cpp', stub: '#include <iostream>\n#include <vector>\n#include <string>\nusing namespace std;\n\nclass Solution {\npublic:\n    void solve() {\n        // TODO: 实现代码\n    }\n};\n\nint main() {\n    Solution().solve();\n    return 0;\n}' },
  go:         { ext: '.go',  stub: 'package main\n\nimport "fmt"\n\nfunc solve() {\n    // TODO: 实现代码\n}\n\nfunc main() {\n    fmt.Println(solve())\n}' },
}

function defaultStub(lang: string): string {
  return LANG_META[lang]?.stub || LANG_META.java.stub
}

function extForLang(lang: string): string {
  return LANG_META[lang]?.ext || LANG_META.java.ext
}

function switchCodeLanguage(lang: string) {
  if (lang === codeLanguage.value) return
  // Save current code for the old language
  allTemplates.value[codeLanguage.value] = currentCode.value
  // Load template for new language
  codeLanguage.value = lang
  currentCode.value = allTemplates.value[lang] || ''
  codeFilename.value = codeFilename.value.replace(/\.\w+$/, extForLang(lang))
}

const { fetchQuota, checkQuota } = useQuota()

const { sendAnswer, reportScore } = useInterviewStream({
  sessionId,
  messages,
  loading,
  onFinish: handleFinish,
  onCodeProblem: handleCodeProblem,
  onCodingInvite: handleCodingInvite,
  onCodingFinish: handleCodingFinish
})

// Detect coding round triggers in user messages
const CODING_TRIGGERS = ['[进入编程环节]', '进入笔试环节', '开始编程', '进入编程', '请出题', '出编程题', '开始写代码']

function isCodingTrigger(text: string): boolean {
  return CODING_TRIGGERS.some(t => text.includes(t))
}

// Thin wrapper: extract and trim text, then delegate to composable
const roundCount = ref(0)
const codingCompleted = ref(false)

function updateProgressFromRound() {
  // Auto-advance pipeline based on conversation rounds
  if (roundCount.value <= 1) {
    advancePipeline('intro')
    progressItems.value[0].status = '进行中...'
  } else if (roundCount.value <= 3) {
    advancePipeline('technical')
  } else if (roundCount.value <= 5) {
    advancePipeline('deepdive')
  }
}

function startRecording() { /* TODO: voice input */ }
function cancelRecording() { inputMode.value = 'keyboard' }
function stopRecording() { /* TODO: voice input */ }

function sendTextAnswer() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  nextTick(() => autoResizeInput())

  if (isCodingTrigger(text)) {
    startCoding()
    sendAnswer('[进入编程环节]')
    return
  }

  roundCount.value++
  updateProgressFromRound()
  sendAnswer(text)
}

async function endInterview() {
  if (finishing.value || finished.value) return
  if (!sessionId.value) { finished.value = true; return }
  finishing.value = true
  try {
    await post(`/api/interview/${sessionId.value}/end`)
  } catch { /* ignore */ }
  finishing.value = false
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

function sanitizeHtml(html: string): string {
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<iframe[\s\S]*?<\/iframe>/gi, '')
    .replace(/<object[\s\S]*?<\/object>/gi, '')
    .replace(/<embed[\s\S]*?>/gi, '')
    .replace(/\s+on\w+\s*=\s*"[^"]*"/gi, '')
    .replace(/\s+on\w+\s*=\s*'[^']*'/gi, '')
}

function stripMarkers(text: string): string {
  return text
    .replace(/\[编程题目\]\s*\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}/g, '')
    .replace(/\[笔试结束\]\s*\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}/g, '')
    .replace(/\[面试结束\]\s*\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}/g, '')
    .replace(/\[笔试邀请\]/g, '')
    .replace(/\[进入编程环节\]/g, '')
    .trim()
}

function renderContent(text: string): string {
  // Loading placeholders — Warm Tech light theme design spec
  if (text === '__CODING_REVIEW_LOADING__') {
    return `<div class="loading-card coding-review-loader">
      <div class="loading-card__head">
        <div class="loader-icon-ring"></div>
        <div>
          <div class="loading-card__title">AI 正在审查代码</div>
          <div class="loading-card__subtitle">预计 5–15 秒</div>
        </div>
      </div>
      <div class="loader-stages">
        <div class="loader-stage loader-stage--done"><span class="loader-stage__icon">✓</span>读取代码</div>
        <div class="loader-stage loader-stage--active"><span class="loader-stage__icon">⊕</span>分析逻辑</div>
        <div class="loader-stage"><span class="loader-stage__icon">◯</span>生成评审</div>
      </div>
      <div class="loader-code-preview">
        <span class="c-kw">public</span> <span class="c-ty">class</span> <span class="c-fn">Solution</span> {<br>
        &nbsp;&nbsp;<span class="c-kw">public</span> <span class="c-ty">int</span> <span class="c-fn">solve</span>(...) ...<br>
        &nbsp;&nbsp;&nbsp;&nbsp;<span class="c-co">// analyzing complexity...</span><br>
        }
      </div>
    </div>`
  }
  if (text === '__CODING_REVIEW_DONE__') {
    return `<div class="mini-report">
      <div class="mini-report__score coding">✓</div>
      <div class="mini-report__text">
        <div class="mini-report__verdict">代码审查完成 <span class="badge badge--coding">编程</span></div>
      </div>
    </div>`
  }
  if (text === '__REPORT_GEN_LOADING__') {
    return `<div class="loading-card report-gen-loader">
      <div class="loading-card__head">
        <div class="loader-dots"><span></span><span></span><span></span></div>
        <div>
          <div class="loading-card__title">正在生成综合评估</div>
          <div class="loading-card__subtitle">基于完整对话 + 笔试结果</div>
        </div>
      </div>
      <div class="report-skeleton">
        <div class="skeleton-line" style="width:60%"></div>
        <div class="skeleton-line" style="width:85%"></div>
        <div class="skeleton-line" style="width:40%"></div>
        <div class="skeleton-line" style="width:70%"></div>
      </div>
    </div>`
  }
  const cleaned = stripMarkers(text)
  try {
    return sanitizeHtml(marked.parse(cleaned) as string)
  } catch {
    return cleaned.replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }
}

function resetCode() {
  currentCode.value = allTemplates.value[codeLanguage.value] || ''
}

async function runCode() {
  if (!currentCode.value.trim() || codeRunning.value) return
  codeRunning.value = true
  codeResult.value = null
  try {
    const userStore = useUserStore()
    const res = await fetch(`${import.meta.env.VITE_API_BASE || ''}/api/coding/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {})
      },
      body: JSON.stringify({ code: currentCode.value, language: codeLanguage.value, stdin: codeStdin.value })
    })
    codeResult.value = await res.json()
  } catch (e: unknown) {
    codeResult.value = { error: e instanceof Error ? e.message : '执行请求失败' }
  } finally {
    codeRunning.value = false
  }
}

// ===== Resize handle =====
let resizeStartX = 0
let resizeStartWidth = 0

function startResize(e: MouseEvent) {
  e.preventDefault()
  resizeStartX = e.clientX
  resizeStartWidth = codePanelWidth.value
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function onResize(e: MouseEvent) {
  const delta = resizeStartX - e.clientX
  codePanelWidth.value = Math.max(320, Math.min(800, resizeStartWidth + delta))
}

function stopResize() {
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function submitCode() {
  if (!currentCode.value.trim() || codeSubmitting.value) return
  codeSubmitting.value = true
  if (!isDesktop.value) showCodePanel.value = false
  // Insert loading card in chat immediately
  messages.value = [...messages.value, { role: 'ai', content: '__CODING_REVIEW_LOADING__' }]
  sendAnswer({
    text: '我的代码已写好了，请帮我审查一下。',
    code: currentCode.value,
    codeLang: codeLanguage.value,
    codeFile: codeFilename.value || 'Solution.' + (codeLanguage.value === 'java' ? 'java' : codeLanguage.value === 'python' ? 'py' : 'js')
  })
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
  display: flex; align-items: center; justify-content: space-between;
}
.dev-test-btn {
  font-size: 10px; font-weight: 500;
  padding: 2px 8px; border-radius: 4px;
  border: 1px dashed var(--accent);
  background: transparent;
  color: var(--accent); cursor: pointer;
  text-transform: none; letter-spacing: 0;
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
.progress-tree__btn {
  font-size: 11px; font-weight: 600; color: #fff;
  background: var(--accent);
  border: none; border-radius: 4px;
  padding: 2px 10px; cursor: pointer;
  margin-top: 2px;
}
.progress-tree__btn:hover { opacity: 0.85; }

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
  position: relative;
}
.code-panel__resize-handle {
  position: absolute; left: -3px; top: 0; bottom: 0;
  width: 6px; cursor: col-resize; z-index: 10;
  transition: background 0.15s;
}
.code-panel__resize-handle:hover,
.code-panel__resize-handle:active {
  background: var(--accent);
}
.code-panel__editor-wrap {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.code-panel__stdin {
  padding: 8px 14px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.code-panel__stdin-input {
  width: 100%;
  padding: 7px 10px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 5px;
  color: #aaa;
  font-family: var(--font-mono);
  font-size: 12px;
  outline: none;
  transition: border-color 0.15s;
}
.code-panel__stdin-input:focus {
  border-color: rgba(255,255,255,0.2);
  color: #ddd;
}
.code-panel__stdin-input::placeholder {
  color: #555;
}
.code-panel__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.code-panel__run-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  border-radius: 6px;
  border: 1px solid var(--color-success);
  background: rgba(97,197,84,0.1);
  color: var(--color-success);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.code-panel__run-btn:hover:not(:disabled) {
  background: var(--color-success);
  color: #fff;
}
.code-panel__run-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.code-panel__reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.12);
  background: transparent;
  color: #999;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.code-panel__reset-btn:hover:not(:disabled) {
  border-color: rgba(255,255,255,0.25);
  color: #fff;
}
.code-panel__reset-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.code-panel__submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 18px;
  border-radius: 6px;
  border: 1px solid var(--accent);
  background: rgba(217,117,10,0.1);
  color: var(--accent);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  margin-left: auto;
}
.code-panel__submit-btn:hover:not(:disabled) {
  background: var(--accent);
  color: #fff;
}
.code-panel__submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.code-panel__results {
  border-top: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0;
  max-height: 200px;
  display: flex;
  flex-direction: column;
}
.code-panel__results-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: rgba(255,255,255,0.03);
}
.code-panel__results-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #aaa;
}
.code-panel__results-close {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 4px;
  border: none; background: transparent;
  color: #888; cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.code-panel__results-close:hover {
  color: #fff;
  background: rgba(255,255,255,0.08);
}
.code-panel__results-body {
  padding: 10px 14px;
  overflow-y: auto;
}
.code-panel__output {
  margin: 0;
  padding: 10px 12px;
  background: rgba(0,0,0,0.3);
  border-radius: 6px;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  color: #d4d4d4;
  white-space: pre-wrap;
  word-break: break-all;
}
.code-panel__output--error {
  color: #ED6A5E;
}
.code-panel__meta {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: #666;
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
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
.code-panel__placeholder {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; height: 100%; gap: 12px;
  color: #666; font-size: 13px;
}
.code-panel__placeholder p { margin: 0; }

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
  margin-bottom: 10px;
}
.finish-card__sub {
  font-size: 13px; color: var(--text-light);
  margin-bottom: 16px;
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
  height: 100%;
  background: var(--bg-dark);
  border-radius: var(--radius-full);
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 0;
}
.capsule-opt {
  position: relative; z-index: 1;
  padding: 6px 18px;
  font-size: 13px; font-weight: 500;
  border: none; background: transparent;
  color: var(--text-muted); cursor: pointer;
  transition: color 0.25s;
  white-space: nowrap;
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
.pos-error {
  margin-top: 16px; padding: 12px 16px;
  background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.15);
  border-radius: var(--radius-md); color: var(--color-danger);
  font-size: 13px; text-align: center; cursor: pointer;
}

/* ===== Coding Invite Bar ===== */
.coding-invite {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 20px; margin: 0 16px 8px;
  background: var(--bg-paper);
  border: 1px solid var(--accent);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.coding-invite__text { font-size: 14px; font-weight: 500; flex: 1; }
.coding-invite__btn {
  padding: 8px 20px; border-radius: var(--radius-full);
  font-size: 13px; font-weight: 500; cursor: pointer; border: none;
}
.coding-invite__btn--skip {
  background: var(--bg-surface); color: var(--text-muted);
}
.coding-invite__btn--enter {
  background: var(--accent); color: #fff;
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

/* Text input (center of pill) — auto-grows to max 5 lines */
.pill-input {
  flex: 1; min-width: 0;
  border: none; background: transparent;
  padding: 8px 6px;
  font-size: 14px; line-height: 1.5;
  outline: none; resize: none; font-family: inherit;
  color: var(--text-main);
  max-height: calc(1.5em * 5 + 16px);
  overflow-y: auto;
  transition: height 0.1s ease;
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

<!-- Non-scoped: v-html loading animations (Warm Tech light theme) -->
<style>
/* ====== Base Loading Card ====== */
.loading-card {
  background: #FDFCFB;
  border: 1px solid rgba(0,0,0,0.06);
  border-radius: 20px;
  padding: 20px 24px;
  margin: 8px 0;
  max-width: 420px;
  display: flex; flex-direction: column; gap: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.04);
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.loading-card__head { display: flex; align-items: center; gap: 14px; }
.loading-card__title { font-size: 15px; font-weight: 600; color: #141413; letter-spacing: -0.2px; }
.loading-card__subtitle { font-size: 12px; color: #888; margin-top: 2px; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ====== Style A: Coding Review (Warm Slate) ====== */
.coding-review-loader {
  border-color: rgba(107,130,153,0.18);
  box-shadow: 0 8px 24px rgba(107,130,153,0.06);
}
.loader-icon-ring {
  width: 36px; height: 36px; border-radius: 50%;
  border: 2.5px solid rgba(107,130,153,0.15); border-top-color: #6B8299;
  animation: spin 0.9s linear infinite; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }

.loader-stages { display: flex; gap: 8px; }
.loader-stage {
  flex: 1; padding: 10px; background: #F7F7F5;
  border-radius: 10px; text-align: center;
  font-size: 11px; font-weight: 500; color: #888;
  border: 1px solid rgba(0,0,0,0.06); transition: all 0.4s;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.loader-stage--active {
  background: #fff; border-color: rgba(107,130,153,0.25);
  color: #6B8299; box-shadow: 0 4px 12px rgba(107,130,153,0.08);
}
.loader-stage--done { color: #141413; background: rgba(107,130,153,0.05); border-color: transparent; }
.loader-stage__icon { font-size: 16px; }

/* Dark code preview inside light card */
.loader-code-preview {
  background: #1a1a18; border-radius: 12px; padding: 14px 16px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 11px;
  line-height: 1.7; color: #E6EDF3;
  max-height: 110px; overflow: hidden; position: relative;
  box-shadow: inset 0 2px 10px rgba(0,0,0,0.15);
}
.loader-code-preview::after {
  content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 50px;
  background: linear-gradient(transparent, #1a1a18);
}
.c-kw { color: #FF7B72; }
.c-fn { color: #D2A8FF; }
.c-ty { color: #79C0FF; }
.c-co { color: #8B949E; font-style: italic; }

/* ====== Done: Mini Report Card ====== */
.mini-report {
  background: #FDFCFB; border: 1px solid rgba(0,0,0,0.06);
  border-radius: 20px; padding: 18px 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  margin: 8px 0; max-width: 420px;
  animation: fadeInUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}
.mini-report__score {
  width: 50px; height: 50px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 700; background: #F7F7F5;
}
.mini-report__score.coding { color: #6B8299; border: 2px solid rgba(107,130,153,0.25); }
.mini-report__text { flex: 1; }
.mini-report__verdict { font-size: 15px; font-weight: 600; color: #141413; display: flex; align-items: center; gap: 8px; }

.badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 100px;
  font-size: 11px; font-weight: 600; letter-spacing: 0.5px;
}
.badge--coding { background: rgba(107,130,153,0.1); color: #6B8299; border: 1px solid rgba(107,130,153,0.18); }

/* ====== Style B: Report Generation (Warm Orange) ====== */
.report-gen-loader {
  border-color: rgba(217,117,10,0.15);
  box-shadow: 0 8px 24px rgba(217,117,10,0.04);
}
.loader-dots { display: flex; gap: 5px; width: 36px; justify-content: center; flex-shrink: 0; }
.loader-dots span {
  width: 6px; height: 6px; border-radius: 50%; background: #D9750A;
  animation: dot-bounce 1.4s ease-in-out infinite;
}
.loader-dots span:nth-child(2) { animation-delay: 0.16s; }
.loader-dots span:nth-child(3) { animation-delay: 0.32s; }
@keyframes dot-bounce {
  0%,80%,100%{transform:scale(0.6);opacity:0.3}
  40%{transform:scale(1);opacity:1}
}

/* Light skeleton */
.report-skeleton { display: flex; flex-direction: column; gap: 12px; margin-top: 4px; }
.skeleton-line {
  height: 10px; border-radius: 4px;
  background: linear-gradient(90deg, #F0F0F0 25%, #E4E4E4 50%, #F0F0F0 75%);
  background-size: 200% 100%; animation: shimmer 1.5s infinite linear;
}
@keyframes shimmer {
  0%{background-position:200% 0}
  100%{background-position:-200% 0}
}

/* ===== Mobile Code FAB ===== */
.code-fab {
  position: fixed; bottom: 100px; right: 20px; z-index: 60;
  width: 48px; height: 48px; border-radius: 50%;
  background: var(--bg-dark); color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: var(--shadow-lg);
  transition: background 0.2s, transform 0.2s;
}
.code-fab:hover { background: var(--accent); transform: scale(1.06); }

/* ===== Mobile Code Overlay ===== */
.code-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: var(--bg-dark);
  display: flex; flex-direction: column;
}
.code-overlay__problem {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
  max-height: 30vh;
  overflow-y: auto;
}
.code-overlay__problem-text {
  font-size: 14px; color: #ccc; line-height: 1.7;
  white-space: pre-wrap;
}
.code-overlay__head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.code-overlay__back {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: #999;
}
.code-overlay__title {
  font-size: 14px; font-weight: 500; color: #ccc;
  font-family: var(--font-mono);
}
.code-overlay__editor-wrap {
  flex: 1; min-height: 0; overflow: hidden;
}
.code-overlay__stdin {
  padding: 8px 14px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.code-overlay__stdin-input {
  width: 100%; padding: 7px 10px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 5px; color: #aaa;
  font-family: var(--font-mono); font-size: 12px; outline: none;
}
.code-overlay__stdin-input:focus {
  border-color: rgba(255,255,255,0.2); color: #ddd;
}
.code-overlay__stdin-input::placeholder { color: #555; }
.code-overlay__toolbar {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.code-overlay__run-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 18px; border-radius: 6px;
  border: 1px solid var(--color-success);
  background: rgba(97,197,84,0.1); color: var(--color-success);
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.code-overlay__run-btn:hover:not(:disabled) { background: var(--color-success); color: #fff; }
.code-overlay__run-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.code-overlay__reset-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border-radius: 6px;
  border: 1px solid rgba(255,255,255,0.12);
  background: transparent; color: #999;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.code-overlay__reset-btn:hover:not(:disabled) { border-color: rgba(255,255,255,0.25); color: #fff; }
.code-overlay__reset-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.code-overlay__submit-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 18px; border-radius: 6px;
  border: 1px solid var(--accent);
  background: rgba(217,117,10,0.1); color: var(--accent);
  font-size: 13px; font-weight: 500; cursor: pointer; margin-left: auto;
}
.code-overlay__submit-btn:hover:not(:disabled) { background: var(--accent); color: #fff; }
.code-overlay__submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.code-overlay__results {
  border-top: 1px solid rgba(255,255,255,0.08);
  flex-shrink: 0; max-height: 200px;
  display: flex; flex-direction: column;
}
.code-overlay__results-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 14px; background: rgba(255,255,255,0.03);
}
.code-overlay__results-title {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 500; color: #aaa;
}
.code-overlay__results-close {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 4px; background: transparent; color: #888; cursor: pointer;
}
.code-overlay__results-close:hover { color: #fff; background: rgba(255,255,255,0.08); }
.code-overlay__results-body { padding: 10px 14px; overflow-y: auto; }
.code-overlay__output {
  margin: 0; padding: 10px 12px;
  background: rgba(0,0,0,0.3); border-radius: 6px;
  font-family: var(--font-mono); font-size: 12px; line-height: 1.6;
  color: #d4d4d4; white-space: pre-wrap; word-break: break-all;
}
.code-overlay__output--error { color: #ED6A5E; }
.code-overlay__meta { display: block; margin-top: 6px; font-size: 11px; color: #666; }
</style>
