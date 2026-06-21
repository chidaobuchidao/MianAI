<template>
  <view class="practice-do">
    <view class="progress-wrap">
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }" />
      </view>
      <text class="progress-text">{{ currentIndex + 1 }}/{{ questions.length }}</text>
    </view>

    <view class="quiz-card" v-if="currentQuestion">
      <view class="quiz-meta">
        <text class="quiz-index">第 {{ currentIndex + 1 }} 题</text>
        <text class="quiz-type">{{ typeLabel(currentQuestion.type) }}</text>
      </view>
      <text class="quiz-title">{{ currentQuestion.title }}</text>

      <view v-if="currentQuestion.type <= 2" class="options">
        <view v-for="(opt, i) in parseOptions(currentQuestion.options)" :key="i"
          class="option" :class="{ selected: selectedIdx === i }"
          @click="selectedIdx = i">
          <view class="opt-letter" :class="{ active: selectedIdx === i }">{{ letters[i] }}</view>
          <text class="opt-text">{{ opt.content || opt }}</text>
        </view>
      </view>

      <view v-if="currentQuestion.type === 3" class="options">
        <view class="option" :class="{ selected: selectedIdx === 0 }" @click="selectedIdx = 0">
          <view class="opt-letter" :class="{ active: selectedIdx === 0 }"><MianIcon name="check" size="28rpx" :color="selectedIdx === 0 ? '#fff' : '#4A4A4A'" stroke-width="2.4" /></view>
          <text class="opt-text">正确</text>
        </view>
        <view class="option" :class="{ selected: selectedIdx === 1 }" @click="selectedIdx = 1">
          <view class="opt-letter" :class="{ active: selectedIdx === 1 }"><MianIcon name="close" size="28rpx" :color="selectedIdx === 1 ? '#fff' : '#4A4A4A'" stroke-width="2.4" /></view>
          <text class="opt-text">错误</text>
        </view>
      </view>

      <input v-if="currentQuestion.type === 4" class="fill-input" v-model="fillAnswer" placeholder="输入答案..." />

      <!-- Result -->
      <view v-if="answered" class="quiz-result" :class="lastCorrect ? 'is-correct' : 'is-wrong'">
        <view class="result-icon"><MianIcon :name="lastCorrect ? 'check' : 'close'" size="44rpx" :color="lastCorrect ? '#16A34A' : '#DC2626'" stroke-width="2.4" /></view>
        <view class="result-body">
          <text class="result-verdict">{{ lastCorrect ? '回答正确' : '回答错误' }}</text>
          <text class="result-answer">正确答案：{{ currentQuestion.answer }}</text>
          <text class="result-analysis" v-if="currentQuestion.analysis">{{ currentQuestion.analysis }}</text>
        </view>
      </view>
    </view>

    <!-- Actions -->
    <view class="quiz-actions" v-if="answered">
      <view v-if="currentIndex > 0" class="btn-action secondary" @click="prevQuestion"><text>上一题</text></view>
      <view v-if="currentIndex < questions.length - 1" class="btn-action primary" @click="nextQuestion"><text>下一题</text></view>
      <view v-else class="btn-action finish" @click="finish"><text>查看结果</text></view>
    </view>
    <view v-else class="btn-submit" :class="{ disabled: !hasAnswer }" @click="submitAnswer"><text>提交答案</text></view>

    <!-- Finish -->
    <view class="finish-screen" v-if="finished">
      <view class="finish-circle" :class="scorePercent >= 70 ? 'good' : 'retry'">
        <text class="finish-score">{{ correctCount }}/{{ questions.length }}</text>
      </view>
      <text class="finish-msg">{{ scorePercent >= 80 ? '太棒了！' : scorePercent >= 60 ? '继续加油！' : '多多练习！' }}</text>
      <view class="btn-retry" @click="retry"><text>再刷一次</text></view>
      <view class="btn-back" @click="goBack"><text>返回</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post } from '@/utils/request';
import MianIcon from '@/components/MianIcon.vue';

interface Question { id: number; type: number; title: string; options: string; answer: string; analysis: string; }
interface AnswerResponse { isCorrect: boolean; correctAnswer: string; analysis: string; }

const letters = ['A', 'B', 'C', 'D'];
const mode = ref('random');
const count = ref(10);
const categoryId = ref('');

const questions = ref<Question[]>([]);
const currentIndex = ref(0);
const currentQuestion = ref<Question | null>(null);
const selectedIdx = ref<number | null>(null);
const fillAnswer = ref('');
const answered = ref(false);
const lastCorrect = ref(false);
const correctCount = ref(0);
const finished = ref(false);

const hasAnswer = computed(() => {
  if (!currentQuestion.value) return false;
  if (currentQuestion.value.type <= 3) return selectedIdx.value !== null;
  return fillAnswer.value.trim() !== '';
});
const scorePercent = computed(() =>
  questions.value.length ? Math.round(correctCount.value / questions.value.length * 100) : 0
);

interface LoadOptions {
  mode?: string;
  count?: string;
  categoryId?: string;
}

onLoad((opts: LoadOptions | undefined) => {
  mode.value = opts?.mode || 'random';
  count.value = Number(opts?.count) || 10;
  categoryId.value = opts?.categoryId || '';
});

function parseOptions(o: string): string[] {
  if (!o) return [];
  try {
    const parsed: unknown = JSON.parse(o);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function typeLabel(t: number): string {
  return ['', '单选', '多选', '判断', '填空'][t] || '';
}

function getAnswerString(): string {
  if (!currentQuestion.value) return '';
  if (currentQuestion.value.type <= 2) {
    return selectedIdx.value !== null ? letters[selectedIdx.value] : '';
  }
  if (currentQuestion.value.type === 3) return selectedIdx.value === 0 ? '正确' : '错误';
  return fillAnswer.value.trim();
}

async function loadQuestions() {
  try {
    if (mode.value === 'topic' && categoryId.value) {
      const r = await get<{ records: Question[] }>('/api/questions', {
        categoryId: categoryId.value,
        size: count.value,
      });
      if (r.data?.records) {
        questions.value = r.data.records;
      }
    } else {
      const r = await get<Question[]>('/api/questions/random', { size: count.value });
      if (r.data) questions.value = r.data;
    }
    if (questions.value.length > 0) currentQuestion.value = questions.value[0];
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '加载失败，请检查网络后重试';
    uni.showToast({ title: msg, icon: 'error' });
  }
}

async function submitAnswer() {
  if (!hasAnswer.value || !currentQuestion.value) return;
  try {
    const r = await post<AnswerResponse>('/api/answers', {
      questionId: currentQuestion.value.id,
      userAnswer: getAnswerString(),
    });
    lastCorrect.value = r.data.isCorrect;
    if (r.data.isCorrect) correctCount.value++;
    answered.value = true;
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '提交失败，请重试';
    uni.showToast({ title: msg, icon: 'error' });
  }
}

function prevQuestion() {
  if (currentIndex.value > 0) {
    currentIndex.value--;
    currentQuestion.value = questions.value[currentIndex.value];
    selectedIdx.value = null; fillAnswer.value = ''; answered.value = false;
  }
}

function nextQuestion() {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++;
    currentQuestion.value = questions.value[currentIndex.value];
    selectedIdx.value = null; fillAnswer.value = ''; answered.value = false;
  }
}

function finish() { finished.value = true; }

function retry() {
  finished.value = false; selectedIdx.value = null; fillAnswer.value = '';
  answered.value = false; correctCount.value = 0; currentIndex.value = 0; questions.value = [];
  loadQuestions();
}

function goBack() { uni.navigateBack(); }

onMounted(() => { loadQuestions(); });
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.practice-do { min-height: 100vh; background: $bg-canvas; display: flex; flex-direction: column; }

.progress-wrap {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 28rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light;
}
.progress-bar { flex: 1; height: 6rpx; background: $bg-surface; border-radius: 3rpx; overflow: hidden; }
.progress-fill { height: 100%; background: $accent; border-radius: 3rpx; transition: width 0.3s; }
.progress-text { font-size: 24rpx; color: $text-light; font-weight: 500; }

.quiz-card {
  flex: 1; margin: 20rpx; padding: 36rpx 28rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; box-shadow: $shadow-sm; overflow-y: auto;
}
.quiz-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.quiz-index { font-size: 24rpx; color: $accent; font-weight: 600; }
.quiz-type { font-size: 22rpx; color: $text-light; background: $bg-surface; padding: 6rpx 16rpx; border-radius: $radius-full; }
.quiz-title { font-size: 30rpx; color: $text-main; line-height: 1.8; margin-bottom: 32rpx; display: block; }

.options { display: flex; flex-direction: column; gap: 14rpx; }
.option {
  display: flex; align-items: center; gap: 18rpx;
  padding: 24rpx 22rpx; border: 1px solid $border-light;
  border-radius: $radius-md; font-size: 28rpx; color: $text-main;
}
.option:active { background: $bg-surface; }
.option.selected { border-color: $accent; background: rgba(217,117,10,0.04); }
.opt-letter {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1px solid $border-medium; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; font-weight: 600;
  color: $text-light; flex-shrink: 0; background: $bg-surface;
}
.opt-letter.active { background: $bg-dark; color: #fff; border-color: $bg-dark; }
.opt-text { flex: 1; line-height: 1.6; }

.fill-input { width: 100%; border: 1px solid $border-medium; border-radius: $radius-md; padding: 20rpx 24rpx; font-size: 28rpx; background: $bg-surface; box-sizing: border-box; }

.quiz-result { margin-top: 28rpx; padding: 24rpx; border-radius: $radius-md; display: flex; gap: 16rpx; }
.quiz-result.is-correct { background: rgba(34,197,94,0.06); border: 1px solid rgba(34,197,94,0.2); }
.quiz-result.is-wrong { background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.2); }
.result-icon { width: 48rpx; height: 48rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.result-body { flex: 1; }
.result-verdict { font-size: 26rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.is-correct .result-verdict { color: $color-success; }
.is-wrong .result-verdict { color: $color-danger; }
.result-answer { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 6rpx; }
.result-analysis { font-size: 24rpx; color: $text-light; line-height: 1.7; }

.quiz-actions { display: flex; gap: 16rpx; padding: 20rpx 28rpx 44rpx; background: $bg-paper; border-top: 1px solid $border-light; }
.btn-action { flex: 1; height: 88rpx; border-radius: $radius-xl; border: none; display: flex; align-items: center; justify-content: center; font-size: 28rpx; font-weight: 500; }
.btn-action:active { opacity: 0.9; }
.btn-action.secondary { background: $bg-surface; color: $text-main; }
.btn-action.primary { background: $bg-dark; color: #fff; }
.btn-action.finish { background: $accent; color: #fff; font-weight: 600; }

.btn-submit { width: calc(100% - 40rpx); height: 88rpx; margin: 20rpx auto 0; background: $bg-dark; color: #fff; font-size: 30rpx; font-weight: 600; border-radius: $radius-lg; border: none; display: flex; align-items: center; justify-content: center; }
.btn-submit:active { opacity: 0.9; }
.btn-submit.disabled { opacity: 0.4; }

.finish-screen { display: flex; flex-direction: column; align-items: center; padding-top: 180rpx; }
.finish-circle { width: 180rpx; height: 180rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 32rpx; }
.finish-circle.good { background: rgba(34,197,94,0.1); border: 2px solid $color-success; }
.finish-circle.retry { background: rgba(217,117,10,0.1); border: 2px solid $accent; }
.finish-score { font-family: Georgia, serif; font-size: 48rpx; font-weight: 700; color: $text-main; }
.finish-msg { font-size: 28rpx; color: $text-muted; margin-bottom: 56rpx; }
.btn-retry, .btn-back { width: calc(100% - 80rpx); max-width: 500rpx; height: 88rpx; font-size: 30rpx; font-weight: 600; border-radius: $radius-lg; border: none; display: flex; align-items: center; justify-content: center; margin-bottom: 20rpx; }
.btn-retry { background: $bg-dark; color: #fff; }
.btn-back { background: $bg-surface; color: $text-muted; }
.btn-retry:active, .btn-back:active { opacity: 0.9; }

@media (min-width: 1025px) { .practice-do { max-width: 800px; margin: 0 auto; } }
</style>
