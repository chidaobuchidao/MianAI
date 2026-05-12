<template>
  <view class="unified-diff">
    <view class="diff-header">
      <text class="diff-file">@@ {{ sectionName }}</text>
      <text class="diff-stats">
        <text class="diff-stat-add">+{{ addCount }}</text>
        <text class="diff-stat-del">-{{ delCount }}</text>
      </text>
    </view>
    <view class="diff-hunks">
      <template v-for="(hunk, hi) in hunks" :key="hi">
        <view v-if="hunk.collapsed" class="diff-collapsed" @click="hunk.collapsed = false">
          <text>⋯ 展开 {{ hunk.skipCount }} 行未改内容 ⋯</text>
        </view>
        <view v-for="(line, li) in hunk.lines" :key="hi + '-' + li" class="diff-row" :class="'diff-row-' + line.type">
          <text class="diff-ln diff-ln-old">{{ line.oldNum || '' }}</text>
          <text class="diff-ln diff-ln-new">{{ line.newNum || '' }}</text>
          <text class="diff-marker diff-marker-{{line.type}}">{{ line.marker }}</text>
          <text class="diff-content">{{ line.text }}</text>
        </view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  oldText: string;
  newText: string;
  sectionName: string;
  contextLines?: number;
}

const props = withDefaults(defineProps<Props>(), {
  contextLines: 3,
});

interface DiffLine {
  type: 'add' | 'del' | 'same';
  text: string;
  oldNum: number | null;
  newNum: number | null;
  marker: string;
}

interface Hunk {
  collapsed: boolean;
  skipCount?: number;
  lines: DiffLine[];
}

const hunks = computed<Hunk[]>(() => {
  const ctx = props.contextLines || 3;
  const oldLines = props.oldText.split('\n');
  const newLines = props.newText.split('\n');
  const allLines = computeLineDiff(oldLines, newLines);

  // Collapse large same blocks
  const result: Hunk[] = [];
  let currentHunk: DiffLine[] = [];
  let sameCount = 0;

  function flushSame() {
    if (sameCount > ctx * 2 + 1) {
      // Collapse: keep first ctx lines, marker, last ctx lines
      const keep = Math.min(ctx, currentHunk.length);
      const first = currentHunk.slice(0, keep);
      const last = currentHunk.slice(currentHunk.length - keep);
      const skipped = currentHunk.length - keep * 2;

      if (first.length > 0) result.push({ collapsed: false, lines: first });
      result.push({ collapsed: true, skipCount: skipped, lines: [] });
      if (last.length > 0) result.push({ collapsed: false, lines: last });
    } else if (currentHunk.length > 0) {
      result.push({ collapsed: false, lines: [...currentHunk] });
    }
    currentHunk = [];
    sameCount = 0;
  }

  for (const line of allLines) {
    if (line.type === 'same') {
      currentHunk.push(line);
      sameCount++;
    } else {
      flushSame();
      currentHunk.push(line);
    }
  }
  flushSame();

  return result;
});

const addCount = computed(() => {
  let c = 0;
  for (const h of hunks.value) if (!h.collapsed) for (const l of h.lines) if (l.type === 'add') c++;
  return c;
});

const delCount = computed(() => {
  let c = 0;
  for (const h of hunks.value) if (!h.collapsed) for (const l of h.lines) if (l.type === 'del') c++;
  return c;
});
</script>

<script lang="ts">
// 行级 LCS diff 算法
interface DiffSegment { type: 'same' | 'add' | 'del'; lines: string[]; }

function computeLineDiff(oldLines: string[], newLines: string[]): DiffLine[] {
  // Build segments using LCS
  const m = oldLines.length, n = newLines.length;
  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));
  for (let i = 1; i <= m; i++)
    for (let j = 1; j <= n; j++)
      dp[i][j] = oldLines[i - 1] === newLines[j - 1]
        ? dp[i - 1][j - 1] + 1
        : Math.max(dp[i - 1][j], dp[i][j - 1]);

  // Backtrack to build segments
  const segments: DiffSegment[] = [];
  let i = m, j = n;
  const sameBuf: string[] = [];
  function flushSame() { if (sameBuf.length) { segments.push({ type: 'same', lines: [...sameBuf.reverse()] }); sameBuf.length = 0; } }

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && oldLines[i - 1] === newLines[j - 1]) {
      sameBuf.push(oldLines[i - 1]); i--; j--;
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      flushSame(); segments.push({ type: 'add', lines: [newLines[j - 1]] }); j--;
    } else {
      flushSame(); segments.push({ type: 'del', lines: [oldLines[i - 1]] }); i--;
    }
  }
  flushSame();
  segments.reverse();

  // Merge adjacent same-type segments
  const merged: DiffSegment[] = [];
  for (const seg of segments) {
    const last = merged[merged.length - 1];
    if (last && last.type === seg.type) { last.lines.push(...seg.lines); }
    else { merged.push({ type: seg.type, lines: [...seg.lines] }); }
  }

  // Convert to DiffLine with line numbers
  const result: DiffLine[] = [];
  let oldNum = 1, newNum = 1;
  for (const seg of merged) {
    for (const line of seg.lines) {
      const dl: DiffLine = {
        type: seg.type,
        text: line,
        oldNum: seg.type === 'add' ? null : oldNum,
        newNum: seg.type === 'del' ? null : newNum,
        marker: seg.type === 'add' ? '+' : seg.type === 'del' ? '-' : ' ',
      };
      result.push(dl);
      if (seg.type !== 'add') oldNum++;
      if (seg.type !== 'del') newNum++;
    }
  }
  return result;
}
</script>

<style lang="scss" scoped>
.unified-diff {
  background: #fff;
  border-radius: 12rpx;
  overflow: hidden;
  font-family: 'Courier New', 'SF Mono', monospace;
  font-size: 22rpx;
  line-height: 1.7;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 16rpx;
  background: #f0f6fc;
  border-bottom: 1rpx solid #d0ddf0;
}

.diff-file {
  color: #0366d6;
  font-size: 20rpx;
  font-weight: 600;
}

.diff-stats { display: flex; gap: 12rpx; }
.diff-stat-add { color: #28a745; font-weight: 600; font-size: 20rpx; }
.diff-stat-del { color: #cb2431; font-weight: 600; font-size: 20rpx; }

.diff-collapsed {
  padding: 16rpx;
  text-align: center;
  background: #f6f8fa;
  color: #586069;
  font-size: 20rpx;
  cursor: pointer;
}

.diff-row {
  display: flex;
  padding: 1rpx 16rpx;
  min-height: 36rpx;
  align-items: flex-start;
}

.diff-row-same { background: #fff; }
.diff-row-add { background: #e6ffec; }
.diff-row-del { background: #ffeef0; }

.diff-ln {
  width: 60rpx;
  text-align: right;
  padding-right: 12rpx;
  color: #959da5;
  flex-shrink: 0;
  font-size: 20rpx;
}
.diff-ln-old { color: #959da5; }
.diff-row-add .diff-ln-old { color: #959da5; }
.diff-row-add .diff-ln-new { color: #444d56; }

.diff-marker {
  width: 24rpx;
  flex-shrink: 0;
  text-align: center;
  font-weight: 700;
  font-size: 20rpx;
}

.diff-marker-add { color: #28a745; }
.diff-marker-del { color: #cb2431; }
.diff-marker-same { color: #d1d5da; }

.diff-content {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
  padding-left: 8rpx;
  color: #24292e;
}

// Remove monospace restriction for CJK content
.diff-content {
  font-family: -apple-system, 'Microsoft YaHei', sans-serif;
}
</style>
