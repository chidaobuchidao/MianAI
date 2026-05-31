<template>
  <Teleport to="body">
    <Transition name="hit-overlay-fade">
      <div v-if="visible" class="hit-overlay" @click.self="$emit('close')" />
    </Transition>

    <Transition name="hit-panel-slide">
      <section
        v-if="visible"
        class="hit-panel"
        role="dialog"
        aria-modal="true"
        aria-label="文献引用详情"
        tabindex="-1"
        @keydown.esc="$emit('close')"
      >
        <header class="hit-header">
          <div>
            <div class="hit-title">文献引用详情</div>
            <div class="hit-subtitle">{{ chunks.length }} 个片段 / {{ paperCount }} 篇论文</div>
          </div>
          <button class="hit-close" aria-label="关闭引用详情" @click="$emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="hit-summary">
          <div class="hit-stat">
            <span class="hit-stat__value">{{ chunks.length }}</span>
            <span class="hit-stat__label">命中片段</span>
          </div>
          <div class="hit-stat">
            <span class="hit-stat__value">{{ paperCount }}</span>
            <span class="hit-stat__label">来源文献</span>
          </div>
          <div class="hit-stat">
            <span class="hit-stat__value">{{ maxScorePercent }}%</span>
            <span class="hit-stat__label">最高相关度</span>
          </div>
        </div>

        <TransitionGroup name="hit-item-motion" tag="div" class="hit-list">
          <article v-if="normalizedOptimizedText" key="optimized" class="hit-item hit-item--optimized">
            <div class="hit-item-head">
              <span class="hit-index">AI</span>
              <span class="hit-paper">当前优化片段</span>
              <span class="hit-chip hit-chip--safe">对照文本</span>
            </div>
            <p class="hit-content">{{ normalizedOptimizedText }}</p>
          </article>

          <article v-for="chunk in decoratedChunks" :key="chunk.key" class="hit-item">
            <div class="hit-item-head">
              <span class="hit-index">#{{ chunk.displayIndex }}</span>
              <span class="hit-paper" :title="chunk.paperTitle">{{ chunk.paperTitle }}</span>
              <span class="hit-score">{{ chunk.scorePercent }}%</span>
            </div>

            <div class="hit-meta-row">
              <span v-if="chunk.section" class="hit-section">{{ chunk.section }}</span>
              <span v-if="chunk.chunkIndexLabel" class="hit-section">{{ chunk.chunkIndexLabel }}</span>
              <span class="hit-chip" :class="chunk.usageClass">{{ chunk.usageLabel }}</span>
            </div>

            <div class="hit-score-bar" aria-hidden="true">
              <span :style="{ width: `${chunk.scorePercent}%` }" />
            </div>

            <div v-if="chunk.keywords.length" class="hit-keywords">
              <span class="hit-keywords__label">命中词</span>
              <span v-for="keyword in chunk.keywords" :key="`${chunk.key}-${keyword}`" class="hit-keyword">
                {{ keyword }}
              </span>
            </div>

            <div class="hit-content-shell" :class="{ 'is-expanded': isExpanded(chunk.key) }">
              <p class="hit-content">{{ isExpanded(chunk.key) ? chunk.content : chunk.preview }}</p>
            </div>

            <div class="hit-reason">
              <span class="hit-reason__dot" />
              {{ chunk.reason }}
            </div>

            <div class="hit-actions">
              <button class="hit-action" @click="toggleExpanded(chunk.key)">
                {{ isExpanded(chunk.key) ? '收起片段' : '展开片段' }}
              </button>
              <button class="hit-action" @click="copyChunk(chunk)">
                {{ copiedKey === chunk.key ? '已复制' : '复制片段' }}
              </button>
              <button class="hit-action" @click="copyReference(chunk)">
                {{ copiedKey === `${chunk.key}:ref` ? '已复制' : '复制文献信息' }}
              </button>
            </div>
          </article>
        </TransitionGroup>

        <footer class="hit-footer">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>知识库片段只作为术语、背景和表达参考；正式引用仍需按学校或期刊规范核对原文。</span>
        </footer>
      </section>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ContextChunk } from '@/modules/paper-kb'

type UsageClass = 'hit-chip--safe' | 'hit-chip--warn' | 'hit-chip--muted'

interface DecoratedChunk {
  key: string
  displayIndex: number
  paperTitle: string
  section?: string
  chunkIndexLabel: string
  content: string
  preview: string
  scorePercent: number
  keywords: string[]
  usageLabel: string
  usageClass: UsageClass
  reason: string
}

const props = defineProps<{
  visible: boolean
  chunks: ContextChunk[]
  optimizedText?: string
}>()

defineEmits<{
  close: []
}>()

const expandedKeys = ref<Set<string>>(new Set())
const copiedKey = ref<string | null>(null)
let copyTimer: ReturnType<typeof window.setTimeout> | null = null

const paperCount = computed(() => new Set(props.chunks.map(c => c.paperTitle)).size)

const maxScorePercent = computed(() => {
  const maxScore = props.chunks.reduce((max, chunk) => Math.max(max, chunk.score ?? 0), 0)
  return Math.round(Math.max(0, Math.min(1, maxScore)) * 100)
})

const normalizedOptimizedText = computed(() => {
  if (!props.optimizedText) return ''
  return preview(props.optimizedText, 520)
})

const decoratedChunks = computed<DecoratedChunk[]>(() =>
  props.chunks.map((chunk, index) => decorateChunk(chunk, index))
)

function decorateChunk(chunk: ContextChunk, index: number): DecoratedChunk {
  const score = Math.max(0, Math.min(1, chunk.score ?? 0))
  const scorePercent = Math.round(score * 100)
  const keywords = pickKeywords(chunk)
  const usage = resolveUsage(score)
  const chunkIndexLabel = typeof chunk.chunkIndex === 'number' ? `第 ${chunk.chunkIndex + 1} 段` : ''

  return {
    key: String(chunk.chunkId ?? `${chunk.paperTitle}-${index}`),
    displayIndex: index + 1,
    paperTitle: chunk.paperTitle,
    section: chunk.section,
    chunkIndexLabel,
    content: normalizeText(chunk.content),
    preview: preview(chunk.content, 360),
    scorePercent,
    keywords,
    usageLabel: usage.label,
    usageClass: usage.className,
    reason: buildReason(scorePercent, keywords, chunk.section),
  }
}

function resolveUsage(score: number): { label: string; className: UsageClass } {
  if (score >= 0.72) return { label: '候选引用', className: 'hit-chip--safe' }
  if (score >= 0.42) return { label: '术语/背景参考', className: 'hit-chip--warn' }
  return { label: '低相关参考', className: 'hit-chip--muted' }
}

function buildReason(scorePercent: number, keywords: string[], section?: string): string {
  const parts = [`相关度 ${scorePercent}%`]
  if (section) parts.push(`来源章节「${section}」`)
  if (keywords.length) parts.push(`匹配 ${keywords.slice(0, 3).join('、')}`)
  return `${parts.join('，')}。建议先核对原文，再决定是否作为正式引用。`
}

function pickKeywords(chunk: ContextChunk): string[] {
  const source = chunk.keywords?.length ? chunk.keywords : extractFallbackKeywords(chunk.content)
  return [...new Set(source.map(k => k.trim()).filter(k => k.length >= 2))].slice(0, 6)
}

function extractFallbackKeywords(content: string): string[] {
  const englishWords = content.match(/[A-Za-z][A-Za-z0-9-]{2,}/g) ?? []
  const chineseTerms = content.match(/[\u4e00-\u9fa5]{2,6}/g) ?? []
  return [...englishWords, ...chineseTerms].filter(word => !STOP_WORDS.has(word))
}

function preview(content: string, maxLength: number): string {
  const normalized = normalizeText(content)
  return normalized.length > maxLength ? `${normalized.slice(0, maxLength)}...` : normalized
}

function normalizeText(content: string): string {
  return content.replace(/\s+/g, ' ').trim()
}

function isExpanded(key: string): boolean {
  return expandedKeys.value.has(key)
}

function toggleExpanded(key: string) {
  const next = new Set(expandedKeys.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  expandedKeys.value = next
}

async function copyChunk(chunk: DecoratedChunk) {
  await copyText(chunk.content, chunk.key)
}

async function copyReference(chunk: DecoratedChunk) {
  const lines = [
    `标题：${chunk.paperTitle}`,
    chunk.section ? `章节：${chunk.section}` : '',
    chunk.chunkIndexLabel ? `位置：${chunk.chunkIndexLabel}` : '',
    `相关度：${chunk.scorePercent}%`,
    `建议用途：${chunk.usageLabel}`,
  ].filter(Boolean)
  await copyText(lines.join('\n'), `${chunk.key}:ref`)
}

async function copyText(text: string, key: string) {
  try {
    await navigator.clipboard.writeText(text)
    copiedKey.value = key
    if (copyTimer) window.clearTimeout(copyTimer)
    copyTimer = window.setTimeout(() => {
      copiedKey.value = null
    }, 1400)
  } catch {
    copiedKey.value = null
  }
}

const STOP_WORDS = new Set([
  '本文',
  '研究',
  '通过',
  '进行',
  '分析',
  '结果',
  '方法',
  '可以',
  '具有',
  '相关',
])
</script>

<style scoped>
.hit-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(20, 20, 19, 0.42);
  backdrop-filter: blur(2px);
}

.hit-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 1201;
  width: 480px;
  max-width: 100vw;
  background: var(--bg-paper, #FDFCFB);
  box-shadow: -18px 0 48px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  outline: none;
  will-change: transform, opacity;
}

.hit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border-light, rgba(0, 0, 0, 0.06));
}

.hit-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main, #141413);
}

.hit-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-light, #999);
}

.hit-close {
  width: 40px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-muted, #555);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.18s cubic-bezier(0.22, 1, 0.36, 1), color 0.18s cubic-bezier(0.22, 1, 0.36, 1), transform 0.18s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-close:hover {
  background: var(--bg-surface, #F5F4F1);
  color: var(--text-main, #141413);
}

.hit-close:active {
  transform: scale(0.94);
}

.hit-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light, rgba(0, 0, 0, 0.06));
  background: var(--bg-surface, #F5F4F1);
}

.hit-stat {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light, rgba(0, 0, 0, 0.06));
  border-radius: 8px;
  background: var(--bg-paper, #FDFCFB);
}

.hit-stat__value {
  display: block;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--accent, #D9750A);
  line-height: 1;
}

.hit-stat__label {
  display: block;
  margin-top: 5px;
  font-size: 11px;
  color: var(--text-light, #999);
}

.hit-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 20px 18px;
}

.hit-list::-webkit-scrollbar {
  width: 5px;
}

.hit-list::-webkit-scrollbar-thumb {
  background: var(--border-light, rgba(0, 0, 0, 0.06));
  border-radius: 4px;
}

.hit-item {
  padding: 15px 0;
  border-bottom: 1px solid var(--border-light, rgba(0, 0, 0, 0.06));
}

.hit-item--optimized {
  margin-bottom: 6px;
  padding: 14px;
  border: 1px solid rgba(217, 117, 10, 0.18);
  border-radius: 8px;
  background: rgba(217, 117, 10, 0.05);
}

.hit-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.hit-index {
  flex-shrink: 0;
  color: var(--accent, #D9750A);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.hit-paper {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-main, #141413);
  font-size: 13px;
  font-weight: 650;
}

.hit-score {
  flex-shrink: 0;
  margin-left: auto;
  font-size: 11px;
  font-weight: 700;
  color: var(--accent, #D9750A);
  font-variant-numeric: tabular-nums;
}

.hit-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.hit-section,
.hit-chip {
  flex-shrink: 0;
  font-size: 11px;
  line-height: 1.4;
  padding: 3px 7px;
  border-radius: 999px;
}

.hit-section {
  color: var(--text-light, #999);
  background: var(--bg-surface, #F5F4F1);
}

.hit-chip {
  color: var(--text-muted, #555);
  border: 1px solid var(--border-light, rgba(0, 0, 0, 0.06));
  background: var(--bg-paper, #FDFCFB);
}

.hit-chip--safe {
  color: #15803D;
  border-color: rgba(22, 163, 74, 0.18);
  background: rgba(22, 163, 74, 0.06);
}

.hit-chip--warn {
  color: var(--accent, #D9750A);
  border-color: rgba(217, 117, 10, 0.2);
  background: rgba(217, 117, 10, 0.06);
}

.hit-chip--muted {
  color: var(--text-light, #999);
  background: var(--bg-surface, #F5F4F1);
}

.hit-score-bar {
  height: 4px;
  margin-top: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--bg-surface, #F5F4F1);
}

.hit-score-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(217, 117, 10, 0.6), var(--accent, #D9750A));
  transition: width 0.42s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-keywords {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.hit-keywords__label {
  font-size: 11px;
  color: var(--text-light, #999);
}

.hit-keyword {
  font-size: 11px;
  color: var(--text-muted, #555);
  background: var(--bg-surface, #F5F4F1);
  border-radius: 6px;
  padding: 2px 7px;
}

.hit-content-shell {
  max-height: 7.5em;
  overflow: hidden;
  transition: max-height 0.34s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-content-shell.is-expanded {
  max-height: 38em;
}

.hit-content {
  margin: 10px 0 0;
  color: var(--text-muted, #555);
  font-size: 12px;
  line-height: 1.72;
  word-break: break-word;
}

.hit-reason {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  margin-top: 10px;
  color: var(--text-light, #999);
  font-size: 11px;
  line-height: 1.5;
}

.hit-reason__dot {
  width: 6px;
  height: 6px;
  margin-top: 5px;
  border-radius: 50%;
  background: var(--accent, #D9750A);
  box-shadow: 0 0 0 4px rgba(217, 117, 10, 0.08);
  flex-shrink: 0;
}

.hit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.hit-action {
  min-height: 32px;
  border: 1px solid var(--border-medium, rgba(0, 0, 0, 0.12));
  border-radius: 8px;
  background: var(--bg-paper, #FDFCFB);
  color: var(--text-muted, #555);
  cursor: pointer;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 10px;
  transition: background 0.18s cubic-bezier(0.22, 1, 0.36, 1), color 0.18s cubic-bezier(0.22, 1, 0.36, 1), border-color 0.18s cubic-bezier(0.22, 1, 0.36, 1), transform 0.18s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-action:hover {
  color: var(--accent, #D9750A);
  border-color: rgba(217, 117, 10, 0.32);
  background: rgba(217, 117, 10, 0.05);
}

.hit-action:active {
  transform: scale(0.96);
}

.hit-footer {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 12px 20px;
  border-top: 1px solid var(--border-light, rgba(0, 0, 0, 0.06));
  color: var(--text-light, #999);
  font-size: 11px;
  line-height: 1.5;
  background: var(--bg-surface, #F5F4F1);
}

.hit-footer svg {
  flex-shrink: 0;
  margin-top: 1px;
}

.hit-overlay-fade-enter-active,
.hit-overlay-fade-leave-active {
  transition: opacity 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-overlay-fade-enter-from,
.hit-overlay-fade-leave-to {
  opacity: 0;
}

.hit-panel-slide-enter-active {
  transition: transform 0.34s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-panel-slide-leave-active {
  transition: transform 0.22s cubic-bezier(0.4, 0, 1, 1), opacity 0.18s cubic-bezier(0.4, 0, 1, 1);
}

.hit-panel-slide-enter-from,
.hit-panel-slide-leave-to {
  opacity: 0;
  transform: translateX(28px);
}

.hit-item-motion-enter-active,
.hit-item-motion-leave-active,
.hit-item-motion-move {
  transition: opacity 0.24s cubic-bezier(0.22, 1, 0.36, 1), transform 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.hit-item-motion-enter-from,
.hit-item-motion-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 560px) {
  .hit-panel {
    width: 100vw;
  }

  .hit-summary {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hit-overlay-fade-enter-active,
  .hit-overlay-fade-leave-active,
  .hit-panel-slide-enter-active,
  .hit-panel-slide-leave-active,
  .hit-item-motion-enter-active,
  .hit-item-motion-leave-active,
  .hit-item-motion-move,
  .hit-close,
  .hit-action,
  .hit-score-bar span,
  .hit-content-shell {
    transition: none;
  }
}
</style>
