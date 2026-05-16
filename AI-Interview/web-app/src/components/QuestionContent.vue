<template>
  <div>
    <div class="q-card">
      <span class="q-type">{{ typeLabel(q.type) }}</span>
      <h3 class="q-text">{{ q.title }}</h3>

      <div v-if="q.type <= 2" class="options">
        <div
          v-for="opt in parseOptions(q.options)"
          :key="opt.label"
          class="option-btn"
          :class="{ correct: isCorrect(opt.label, q.answer, q.type) }"
        >
          <span class="option-letter">{{ opt.label }}</span>
          <span class="option-text">{{ opt.content }}</span>
          <svg v-if="isCorrect(opt.label, q.answer, q.type)" class="option-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
        </div>
      </div>

      <div v-if="q.type === 3" class="options">
        <div class="option-btn" :class="{ correct: q.answer === '正确' }">
          <span class="option-letter">&#10003;</span>
          <span class="option-text">正确</span>
        </div>
        <div class="option-btn" :class="{ correct: q.answer === '错误' }">
          <span class="option-letter">&#10007;</span>
          <span class="option-text">错误</span>
        </div>
      </div>

      <div v-if="q.type === 4" class="fill-answer">
        <span class="fill-label">参考答案：</span>
        <span class="fill-text">{{ q.answer }}</span>
      </div>
    </div>

    <div class="analysis-card" v-if="q.analysis">
      <span class="analysis-title">答案解析</span>
      <p class="analysis-text">{{ q.analysis }}</p>
    </div>

    <div class="detail-tags">
      <span class="detail-tag">{{ q.categoryName || '未分类' }}</span>
      <span class="detail-tag">{{ difficultyLabel(q.difficulty) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Option { label: string; content: string }
interface Question {
  id: number; categoryName: string; type: number; title: string;
  options: string; answer: string; analysis: string; difficulty: number;
}

defineProps<{ q: Question }>()

function parseOptions(o: string): Option[] {
  if (!o) return []
  try { return JSON.parse(o) } catch { return [] }
}

function isCorrect(optLabel: string, answer: string, type: number): boolean {
  if (!answer) return false
  if (type === 2) {
    // Multiple choice: answer may be "AB", "A,B", "A|B"
    const labels = answer.includes(',') ? answer.split(',')
      : answer.includes('|') ? answer.split('|')
      : answer.split('')
    return labels.map(l => l.trim()).includes(optLabel)
  }
  return optLabel === answer
}

function typeLabel(t: number) {
  return ['', '单选题', '多选题', '判断题', '填空题'][t] || ''
}

function difficultyLabel(d: number) {
  return d === 3 ? '困难' : d === 2 ? '中等' : '简单'
}
</script>

<style scoped>
.q-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: 28px 24px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 12px;
}
.q-type {
  font-size: 11px; font-weight: 600; color: var(--accent);
  text-transform: uppercase; letter-spacing: 1px;
  margin-bottom: 12px; display: block;
}
.q-text {
  font-size: 17px; font-weight: 500; line-height: 1.6;
  margin-bottom: 24px;
}
.options { display: flex; flex-direction: column; gap: 10px; }
.option-btn {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-paper);
  font-size: 15px;
}
.option-btn.correct {
  border-color: var(--color-success);
  background: rgba(34,197,94,0.04);
}
.option-letter {
  width: 32px; height: 32px; border-radius: 8px;
  background: var(--bg-surface);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: var(--text-muted);
  flex-shrink: 0;
}
.option-btn.correct .option-letter {
  background: var(--color-success); color: #fff;
}
.option-text { flex: 1; }
.option-check { flex-shrink: 0; color: var(--color-success); }
.fill-answer {
  background: var(--bg-surface); border-radius: var(--radius-md);
  padding: 14px 16px; display: flex; gap: 8px;
}
.fill-label { font-size: 14px; color: var(--text-light); }
.fill-text { font-size: 14px; font-weight: 500; }
.analysis-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 18px 16px;
  margin-bottom: 12px;
}
.analysis-title {
  font-size: 14px; font-weight: 600; color: var(--text-main);
  display: block; margin-bottom: 8px;
}
.analysis-text { font-size: 14px; color: var(--text-muted); line-height: 1.7; }
.detail-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.detail-tag {
  font-size: 12px; color: var(--text-muted);
  background: var(--bg-surface);
  padding: 6px 14px; border-radius: var(--radius-full);
}
</style>
