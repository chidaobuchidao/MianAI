<template>
  <div class="page">
    <div class="page__inner">
      <div class="page-head">
        <button class="back-btn" @click="$router.back()">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </button>
        <span class="page-head__title">答题</span>
        <span class="page-head__counter">{{ current + 1 }}/{{ total }}</span>
      </div>

      <!-- Progress -->
      <div class="progress-bar">
        <div class="progress-bar__fill" :style="{ width: `${((current + 1) / total) * 100}%` }" />
      </div>

      <!-- Question -->
      <div class="question-card animate-fade-in-up" :key="current">
        <span class="question-card__type">单选题</span>
        <h3 class="question-card__title">{{ question?.title || '加载中...' }}</h3>

        <div class="options">
          <button
            class="option"
            v-for="(opt, i) in (question?.options || [])"
            :key="i"
            :class="{ 'option--selected': selected === i }"
            @click="selected = i"
          >
            <span class="option__letter">{{ letters[i] }}</span>
            <span class="option__text">{{ opt }}</span>
          </button>
        </div>
      </div>

      <!-- Actions -->
      <div class="actions">
        <button class="action-btn action-btn--secondary" v-if="current > 0" @click="current--">上一题</button>
        <button class="action-btn action-btn--primary" @click="nextQuestion">
          {{ current < total - 1 ? '下一题' : '提交试卷' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Question { title: string; options: string[] }
const letters = ['A', 'B', 'C', 'D']

const current = ref(0)
const total = ref(10)
const selected = ref<number | null>(null)

const question = ref<Question>({
  title: '在 Java 中，以下哪个集合类是线程安全的？',
  options: ['ArrayList', 'HashMap', 'ConcurrentHashMap', 'LinkedList']
})

function nextQuestion() {
  if (current.value < total.value - 1) {
    current.value++
    selected.value = null
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: var(--bg-canvas); }
.page__inner { max-width: 700px; margin: 0 auto; padding: 0 20px; }

.page-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 0;
}
.back-btn {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-main);
}
.page-head__title { font-size: 16px; font-weight: 600; }
.page-head__counter { font-size: 14px; color: var(--text-light); }

.progress-bar {
  height: 3px; background: var(--border-light); border-radius: 2px;
  margin-bottom: 32px; overflow: hidden;
}
.progress-bar__fill {
  height: 100%; background: var(--accent);
  border-radius: 2px; transition: width 0.3s var(--ease-out-expo);
}

.question-card {
  background: var(--bg-paper);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  padding: 28px 24px;
  box-shadow: var(--shadow-sm);
}
.question-card__type {
  font-size: 11px; font-weight: 600; color: var(--accent);
  text-transform: uppercase; letter-spacing: 1px;
  margin-bottom: 12px; display: block;
}
.question-card__title {
  font-size: 17px; font-weight: 500; line-height: 1.6;
  margin-bottom: 28px;
}

.options { display: flex; flex-direction: column; gap: 10px; }
.option {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-paper);
  font-size: 15px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s;
}
.option:hover { border-color: var(--text-main); background: var(--bg-surface); }
.option--selected {
  border-color: var(--accent);
  background: rgba(217,117,10,0.05);
}
.option__letter {
  width: 32px; height: 32px;
  background: var(--bg-surface);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; color: var(--text-muted);
  flex-shrink: 0;
}
.option--selected .option__letter {
  background: var(--accent); color: #fff;
}

.actions {
  display: flex; gap: 12px; margin-top: 28px;
}
.action-btn {
  flex: 1; padding: 14px; border-radius: var(--radius-lg);
  font-size: 15px; font-weight: 500; cursor: pointer;
}
.action-btn--secondary {
  background: var(--bg-paper); border: 1px solid var(--border-medium);
  color: var(--text-main);
}
.action-btn--primary {
  background: var(--bg-dark); color: #fff; border: none;
  box-shadow: var(--shadow-md);
}
</style>
