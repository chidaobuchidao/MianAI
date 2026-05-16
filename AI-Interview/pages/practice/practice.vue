<template>
  <view class="practice">
    <!-- 开始屏 -->
    <view class="start-screen" v-if="!started && !finished">
      <view class="start-card">
        <view class="start-icon-box">
          <text class="start-icon">🎯</text>
        </view>
        <text class="start-title">自由刷题</text>
        <text class="start-desc">随机抽取 10 道题目，即时判分</text>
        <view class="start-stats">
          <view class="stat"><text class="stat-num">10</text><text class="stat-lbl">题量</text></view>
          <view class="stat-divider" />
          <view class="stat"><text class="stat-num">不限</text><text class="stat-lbl">时间</text></view>
          <view class="stat-divider" />
          <view class="stat"><text class="stat-num">即时</text><text class="stat-lbl">判分</text></view>
        </view>
        <view class="btn-start" @click="startPractice">
          <text>开始刷题</text>
        </view>
      </view>
    </view>

    <!-- 答题屏 -->
    <view class="quiz-screen" v-if="started && !finished && currentQuestion">
      <!-- 进度条 -->
      <view class="progress-wrap">
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }" />
        </view>
        <text class="progress-text">{{ currentIndex + 1 }}/{{ questions.length }}</text>
      </view>

      <view class="quiz-card">
        <view class="quiz-meta">
          <text class="quiz-index">第 {{ currentIndex + 1 }} 题</text>
          <text class="quiz-type">{{ getTypeLabel(currentQuestion.type) }}</text>
        </view>

        <text class="quiz-title">{{ currentQuestion.title }}</text>

        <!-- 选项 -->
        <view v-if="currentQuestion.type <= 2" class="options">
          <view
            v-for="opt in parseOptions(currentQuestion.options)"
            :key="opt.label"
            class="option"
            :class="{
              selected: selectedAnswer === opt.label,
              correct: answered && opt.label === currentQuestion.answer,
              wrong: answered && selectedAnswer === opt.label && selectedAnswer !== currentQuestion.answer
            }"
            @click="selectOption(opt.label)"
          >
            <view class="opt-letter" :class="{ active: selectedAnswer === opt.label }">{{ opt.label }}</view>
            <text class="opt-text">{{ opt.content }}</text>
            <text v-if="answered && opt.label === currentQuestion.answer" class="opt-mark correct-mark">✓</text>
            <text v-if="answered && selectedAnswer === opt.label && selectedAnswer !== currentQuestion.answer" class="opt-mark wrong-mark">✗</text>
          </view>
        </view>

        <!-- 判断 -->
        <view v-if="currentQuestion.type === 3" class="options">
          <view
            v-for="v in ['正确','错误']" :key="v"
            class="option"
            :class="{
              selected: selectedAnswer === v,
              correct: answered && v === currentQuestion.answer,
              wrong: answered && selectedAnswer === v && selectedAnswer !== currentQuestion.answer
            }"
            @click="selectOption(v)"
          >
            <view class="opt-letter" :class="{ active: selectedAnswer === v }">{{ v === '正确' ? '✓' : '✗' }}</view>
            <text class="opt-text">{{ v }}</text>
          </view>
        </view>

        <!-- 填空 -->
        <input v-if="currentQuestion.type === 4" class="fill-input" v-model="selectedAnswer" placeholder="输入答案..." />

        <!-- 判分结果 -->
        <view v-if="answered" class="quiz-result" :class="lastResult?.isCorrect ? 'is-correct' : 'is-wrong'">
          <text class="result-icon">{{ lastResult?.isCorrect ? '✓' : '✗' }}</text>
          <view class="result-body">
            <text class="result-verdict">{{ lastResult?.isCorrect ? '回答正确' : '回答错误' }}</text>
            <text class="result-answer">正确答案：{{ lastResult?.correctAnswer }}</text>
            <text class="result-analysis" v-if="lastResult?.analysis">{{ lastResult?.analysis }}</text>
          </view>
        </view>
      </view>

      <!-- 按钮 -->
      <view class="quiz-actions" v-if="answered">
        <view v-if="currentIndex < questions.length - 1" class="btn-next" @click="nextQuestion">
          <text>下一题</text>
        </view>
        <view v-else class="btn-finish" @click="finish">
          <text>查看结果</text>
        </view>
      </view>
      <view v-else class="btn-submit" :class="{ disabled: !selectedAnswer }" @click="submitAnswer">
        <text>提交答案</text>
      </view>
    </view>

    <!-- 完成屏 -->
    <view class="finish-screen" v-if="finished">
      <view class="finish-circle" :class="correctCount >= 6 ? 'good' : 'retry'">
        <text class="finish-score">{{ correctCount }}/{{ questions.length }}</text>
      </view>
      <text class="finish-msg">{{ correctCount >= 8 ? '太棒了！' : correctCount >= 6 ? '继续加油！' : '多多练习！' }}</text>
      <view class="btn-retry" @click="retry"><text>再刷一次</text></view>
      <view class="btn-back" @click="goHome"><text>返回首页</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { get, post } from '@/utils/request';
interface Question { id: number; categoryName: string; type: number; title: string; options: string; answer: string; analysis: string; }
interface AR { isCorrect: boolean; correctAnswer: string; analysis: string; }

const started = ref(false); const finished = ref(false);
const questions = ref<Question[]>([]); const currentIndex = ref(0);
const currentQuestion = ref<Question | null>(null);
const selectedAnswer = ref(''); const answered = ref(false);
const lastResult = ref<AR | null>(null); const correctCount = ref(0);

function parseOptions(o: string) { if (!o) return []; try { return JSON.parse(o); } catch { return []; } }
function getTypeLabel(t: number) { return ['','单选','多选','判断','填空'][t] || ''; }

async function startPractice() {
  try { const r = await get<Question[]>('/api/questions/random', { size: 10 } as Record<string,unknown>);
    questions.value = r.data; if (questions.value.length > 0) { currentQuestion.value = questions.value[0]; started.value = true; }
  } catch { uni.showToast({ title:'获取题目失败', icon:'error' }); }
}
function selectOption(v: string) { if (!answered.value) selectedAnswer.value = v; }
async function submitAnswer() {
  if (!selectedAnswer.value || !currentQuestion.value) return;
  try { const r = await post<AR>('/api/answers', { questionId: currentQuestion.value.id, userAnswer: selectedAnswer.value });
    lastResult.value = r.data; if (r.data.isCorrect) correctCount.value++; answered.value = true;
  } catch { uni.showToast({ title:'提交失败', icon:'error' }); }
}
function nextQuestion() { currentIndex.value++; currentQuestion.value = questions.value[currentIndex.value]; selectedAnswer.value = ''; answered.value = false; lastResult.value = null; }
function finish() { finished.value = true; started.value = false; }
function retry() { finished.value = false; selectedAnswer.value = ''; answered.value = false; lastResult.value = null; correctCount.value = 0; currentIndex.value = 0; questions.value = []; }
function goHome() { uni.switchTab({ url: '/pages/index/index' }); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.practice { min-height: 100vh; background: $bg-canvas; }

// ===== 开始屏 =====
.start-screen { display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 40rpx; }
.start-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-xl; padding: 64rpx 48rpx;
  width: 100%; max-width: 600rpx; display: flex; flex-direction: column;
  align-items: center; box-shadow: $shadow-md; text-align: center;
}
.start-icon-box {
  width: 120rpx; height: 120rpx; background: $bg-surface;
  border-radius: 28rpx; display: flex; align-items: center;
  justify-content: center; margin-bottom: 28rpx;
}
.start-icon { font-size: 56rpx; }
.start-title {
  font-family: Georgia, serif; font-size: 36rpx; font-weight: 600;
  color: $text-main; margin-bottom: 10rpx;
}
.start-desc { font-size: 26rpx; color: $text-muted; margin-bottom: 40rpx; }

.start-stats { display: flex; align-items: center; gap: 32rpx; margin-bottom: 44rpx; }
.stat { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-family: Georgia, serif; font-size: 40rpx; font-weight: 600; color: $text-main; }
.stat-lbl { font-size: 22rpx; color: $text-light; margin-top: 4rpx; }
.stat-divider { width: 2rpx; height: 48rpx; background: $border-light; }

.btn-start {
  width: 100%; height: 96rpx; background: $bg-dark;
  color: #fff; font-size: 30rpx; font-weight: 600;
  border-radius: $radius-lg; border: none;
  display: flex; align-items: center; justify-content: center;
}
.btn-start:active { opacity: 0.9; }

// ===== 答题屏 =====
.quiz-screen { padding-bottom: 40rpx; }

.progress-wrap {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 28rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light;
}
.progress-bar { flex: 1; height: 6rpx; background: $bg-surface; border-radius: 3rpx; overflow: hidden; }
.progress-fill { height: 100%; background: $accent; border-radius: 3rpx; transition: width 0.3s; }
.progress-text { font-size: 24rpx; color: $text-light; font-weight: 500; }

.quiz-card {
  margin: 20rpx; padding: 36rpx 28rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; box-shadow: $shadow-sm;
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
.option.correct { border-color: $color-success; background: rgba(34,197,94,0.04); }
.option.wrong { border-color: $color-danger; background: rgba(239,68,68,0.04); }

.opt-letter {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1px solid $border-medium; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; font-weight: 600;
  color: $text-light; flex-shrink: 0; background: $bg-surface;
}
.opt-letter.active { background: $bg-dark; color: #fff; border-color: $bg-dark; }
.opt-text { flex: 1; line-height: 1.6; }
.opt-mark { font-size: 28rpx; font-weight: 700; flex-shrink: 0; }
.correct-mark { color: $color-success; }
.wrong-mark { color: $color-danger; }

.fill-input {
  width: 100%; border: 1px solid $border-medium; border-radius: $radius-md;
  padding: 20rpx 24rpx; font-size: 28rpx; background: $bg-surface;
  box-sizing: border-box;
}

// 判分结果
.quiz-result {
  margin-top: 28rpx; padding: 24rpx; border-radius: $radius-md;
  display: flex; gap: 16rpx;
}
.quiz-result.is-correct { background: rgba(34,197,94,0.06); border: 1px solid rgba(34,197,94,0.2); }
.quiz-result.is-wrong { background: rgba(239,68,68,0.06); border: 1px solid rgba(239,68,68,0.2); }
.result-icon { font-size: 40rpx; font-weight: 700; flex-shrink: 0; }
.is-correct .result-icon { color: $color-success; }
.is-wrong .result-icon { color: $color-danger; }
.result-body { flex: 1; display: flex; flex-direction: column; }
.result-verdict { font-size: 26rpx; font-weight: 600; color: $text-main; margin-bottom: 8rpx; }
.result-answer { font-size: 24rpx; color: $text-muted; margin-bottom: 6rpx; }
.result-analysis { font-size: 24rpx; color: $text-light; line-height: 1.7; }

// 按钮
.btn-submit, .btn-next, .btn-finish {
  width: calc(100% - 40rpx); height: 88rpx; margin: 20rpx auto 0;
  background: $bg-dark; color: #fff; font-size: 30rpx; font-weight: 600;
  border-radius: $radius-lg; border: none;
  display: flex; align-items: center; justify-content: center;
}
.btn-submit:active, .btn-next:active, .btn-finish:active { opacity: 0.9; }
.btn-submit.disabled { opacity: 0.4; }
.btn-finish { background: $accent; }

// ===== 完成屏 =====
.finish-screen { display: flex; flex-direction: column; align-items: center; padding-top: 180rpx; }
.finish-circle {
  width: 180rpx; height: 180rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; margin-bottom: 32rpx;
}
.finish-circle.good { background: rgba(34,197,94,0.1); border: 2px solid $color-success; }
.finish-circle.retry { background: rgba(217,117,10,0.1); border: 2px solid $accent; }
.finish-score { font-family: Georgia, serif; font-size: 48rpx; font-weight: 700; color: $text-main; }
.finish-msg { font-size: 28rpx; color: $text-muted; margin-bottom: 56rpx; }

.btn-retry, .btn-back {
  width: calc(100% - 80rpx); max-width: 500rpx; height: 88rpx;
  font-size: 30rpx; font-weight: 600; border-radius: $radius-lg; border: none;
  display: flex; align-items: center; justify-content: center; margin-bottom: 20rpx;
}
.btn-retry { background: $bg-dark; color: #fff; }
.btn-back { background: $bg-surface; color: $text-muted; }
.btn-retry:active, .btn-back:active { opacity: 0.9; }

@media (min-width: 1025px) { .practice { max-width: 800px; margin: 0 auto; } }
</style>
