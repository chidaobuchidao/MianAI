<template>
  <div class="unified-diff">
    <div class="diff-header">
      <span class="diff-file">@@ {{ sectionName }}</span>
      <span class="diff-stats">
        <span class="diff-stat-add">+{{ addCount }}</span>
        <span class="diff-stat-del">-{{ delCount }}</span>
      </span>
    </div>
    <div class="diff-hunks">
      <template v-for="(hunk, hi) in hunks" :key="hi">
        <div v-if="hunk.collapsed" class="diff-collapsed" @click="hunk.collapsed = false">
          ··· 展开 {{ hunk.skipCount }} 行未改内容 ···
        </div>
        <div v-for="(line, li) in hunk.lines" :key="hi + '-' + li" class="diff-row" :class="'diff-row-' + line.type">
          <span class="diff-ln diff-ln-old">{{ line.oldNum || '' }}</span>
          <span class="diff-ln diff-ln-new">{{ line.newNum || '' }}</span>
          <span class="diff-marker" :class="'diff-marker-' + line.type">{{ line.marker }}</span>
          <span class="diff-content">{{ line.text }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  oldText: string
  newText: string
  sectionName: string
  contextLines?: number
}
const props = withDefaults(defineProps<Props>(), { contextLines: 3 })

interface DiffLine { type: 'add' | 'del' | 'same'; text: string; oldNum: number | null; newNum: number | null; marker: string }
interface Hunk { collapsed: boolean; skipCount?: number; lines: DiffLine[] }

const hunks = computed<Hunk[]>(() => {
  const ctx = props.contextLines || 3
  const oldLines = props.oldText.split('\n')
  const newLines = props.newText.split('\n')
  const allLines = computeLineDiff(oldLines, newLines)

  const result: Hunk[] = []
  let currentHunk: DiffLine[] = []
  let sameCount = 0

  function flushSame() {
    if (sameCount > ctx * 2 + 1) {
      const keep = Math.min(ctx, currentHunk.length)
      if (keep > 0) result.push({ collapsed: false, lines: currentHunk.slice(0, keep) })
      result.push({ collapsed: true, skipCount: currentHunk.length - keep * 2, lines: [] })
      if (keep > 0) result.push({ collapsed: false, lines: currentHunk.slice(-keep) })
    } else if (currentHunk.length > 0) {
      result.push({ collapsed: false, lines: [...currentHunk] })
    }
    currentHunk = []
    sameCount = 0
  }

  for (const line of allLines) {
    if (line.type === 'same') { currentHunk.push(line); sameCount++ }
    else { flushSame(); currentHunk.push(line) }
  }
  flushSame()
  return result
})

const addCount = computed(() => {
  let c = 0
  for (const h of hunks.value) if (!h.collapsed) for (const l of h.lines) if (l.type === 'add') c++
  return c
})
const delCount = computed(() => {
  let c = 0
  for (const h of hunks.value) if (!h.collapsed) for (const l of h.lines) if (l.type === 'del') c++
  return c
})

// LCS-based line diff
interface DiffSegment { type: 'same' | 'add' | 'del'; lines: string[] }

function computeLineDiff(oldLines: string[], newLines: string[]): DiffLine[] {
  const m = oldLines.length, n = newLines.length
  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0))
  for (let i = 1; i <= m; i++)
    for (let j = 1; j <= n; j++)
      dp[i][j] = oldLines[i - 1] === newLines[j - 1] ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1])

  const segments: DiffSegment[] = []
  let i = m, j = n
  const sameBuf: string[] = []
  function flushSame() { if (sameBuf.length) { segments.push({ type: 'same', lines: [...sameBuf.reverse()] }); sameBuf.length = 0 } }

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && oldLines[i - 1] === newLines[j - 1]) { sameBuf.push(oldLines[i - 1]); i--; j-- }
    else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) { flushSame(); segments.push({ type: 'add', lines: [newLines[j - 1]] }); j-- }
    else { flushSame(); segments.push({ type: 'del', lines: [oldLines[i - 1]] }); i-- }
  }
  flushSame()
  segments.reverse()

  const merged: DiffSegment[] = []
  for (const seg of segments) {
    const last = merged[merged.length - 1]
    if (last && last.type === seg.type) last.lines.push(...seg.lines)
    else merged.push({ type: seg.type, lines: [...seg.lines] })
  }

  const result: DiffLine[] = []
  let oldNum = 1, newNum = 1
  for (const seg of merged) {
    for (const line of seg.lines) {
      result.push({ type: seg.type, text: line, oldNum: seg.type === 'add' ? null : oldNum, newNum: seg.type === 'del' ? null : newNum, marker: seg.type === 'add' ? '+' : seg.type === 'del' ? '-' : ' ' })
      if (seg.type !== 'add') oldNum++
      if (seg.type !== 'del') newNum++
    }
  }
  return result
}
</script>

<style scoped>
.unified-diff {
  background: var(--bg-paper); border: 1px solid var(--border-light);
  border-radius: 8px; overflow: hidden; font-size: 13px; line-height: 1.6;
}
.diff-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 14px; background: #f0f6fc; border-bottom: 1px solid #d0ddf0;
}
.diff-file { color: #0366d6; font-size: 12px; font-weight: 600; }
.diff-stats { display: flex; gap: 10px; }
.diff-stat-add { color: #28a745; font-weight: 600; font-size: 12px; }
.diff-stat-del { color: #cb2431; font-weight: 600; font-size: 12px; }
.diff-collapsed { padding: 12px; text-align: center; background: #f6f8fa; color: #586069; font-size: 12px; cursor: pointer; }
.diff-row { display: flex; padding: 0 14px; min-height: 24px; align-items: flex-start; }
.diff-row-same { background: #fff; }
.diff-row-add { background: #e6ffec; }
.diff-row-del { background: #ffeef0; }
.diff-ln { width: 40px; text-align: right; padding-right: 8px; color: #959da5; flex-shrink: 0; font-size: 11px; }
.diff-marker { width: 16px; flex-shrink: 0; text-align: center; font-weight: 700; font-size: 11px; }
.diff-marker-add { color: #28a745; }
.diff-marker-del { color: #cb2431; }
.diff-marker-same { color: #d1d5da; }
.diff-content { flex: 1; white-space: pre-wrap; word-break: break-all; padding-left: 6px; color: #24292e; }
</style>
