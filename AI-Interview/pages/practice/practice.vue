<template>
  <view class="practice">
    <!-- 开始屏 -->
    <view class="start-screen" v-if="!started && !finished">
      <view class="start-card">
        <text class="start-icon">🎯</text>
        <text class="start-title">自由刷题</text>
        <text class="start-desc">随机抽取 10 道题目</text>
        <view class="start-stats">
          <view class="stat"><text class="stat-num">10</text><text class="stat-lbl">题量</text></view>
          <view class="stat-divider" />
          <view class="stat"><text class="stat-num">不限</text><text class="stat-lbl">时间</text></view>
          <view class="stat-divider" />
          <view class="stat"><text class="stat-num">即时</text><text class="stat-lbl">判分</text></view>
        </view>
        <button class="btn-start" @click="startPractice">开始刷题</button>
      </view>
    </view>

    <!-- 答题屏 -->
    <view class="quiz-screen" v-if="started && !finished && currentQuestion">
      <!-- 进度条 -->
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }" />
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
            <text v-if="answered && opt.label === currentQuestion.answer" class="opt-mark">✓</text>
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
        <button v-if="currentIndex < questions.length - 1" class="btn-next" @click="nextQuestion">下一题</button>
        <button v-else class="btn-finish" @click="finish">查看结果</button>
      </view>
      <button v-else class="btn-submit" :disabled="!selectedAnswer" @click="submitAnswer">提交答案</button>
    </view>

    <!-- 完成屏 -->
    <view class="finish-screen" v-if="finished">
      <view class="finish-circle" :class="correctCount >= 6 ? 'good' : 'retry'">
        <text class="finish-score">{{ correctCount }}/{{ questions.length }}</text>
      </view>
      <text class="finish-msg">{{ correctCount >= 8 ? '太棒了！' : correctCount >= 6 ? '继续加油！' : '多多练习！' }}</text>
      <button class="btn-retry" @click="retry">再刷一次</button>
      <button class="btn-back" @click="goHome">返回首页</button>
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
.practice { min-height: 100vh; background: #f0f4ff; }

// Start
.start-screen { display: flex; justify-content: center; padding-top: 180rpx; }
.start-card { background: #fff; border-radius: 28rpx; padding: 60rpx 50rpx; width: 85vw; text-align: center; box-shadow: 0 8rpx 40rpx rgba(43,111,242,0.08); }
.start-icon { font-size: 96rpx; }
.start-title { display: block; font-size: 40rpx; font-weight: 800; color: #0f172a; margin-top: 20rpx; }
.start-desc { display: block; font-size: 26rpx; color: #94a3b8; margin-top: 8rpx; }
.start-stats { display: flex; justify-content: center; align-items: center; gap: 40rpx; margin: 40rpx 0; }
.stat { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 36rpx; font-weight: 800; color: #2b6ff2; }
.stat-lbl { font-size: 22rpx; color: #94a3b8; }
.stat-divider { width: 2rpx; height: 40rpx; background: #e2e8f0; }
.btn-start { width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; margin-top: 10rpx; }

// Quiz
.quiz-screen { padding: 0 24rpx 40rpx; }
.progress-bar { height: 6rpx; background: #e2e8f0; margin: 20rpx 0 24rpx; border-radius: 3rpx; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #2b6ff2, #6366f1); border-radius: 3rpx; transition: width 0.3s; }
.quiz-card { background: #fff; border-radius: 24rpx; padding: 36rpx; box-shadow: 0 4rpx 24rpx rgba(0,0,0,0.04); }
.quiz-meta { display: flex; justify-content: space-between; margin-bottom: 24rpx; }
.quiz-index { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.quiz-type { font-size: 22rpx; color: #94a3b8; background: #f1f5f9; padding: 4rpx 14rpx; border-radius: 8rpx; }
.quiz-title { display: block; font-size: 30rpx; font-weight: 600; color: #0f172a; line-height: 1.8; margin-bottom: 32rpx; }
.options { display: flex; flex-direction: column; gap: 16rpx; }
.option { display: flex; align-items: center; gap: 16rpx; padding: 20rpx; border: 2rpx solid #e2e8f0; border-radius: 16rpx; transition: all 0.15s; position: relative; }
.option.selected { border-color: #2b6ff2; background: #f0f4ff; }
.option.correct { border-color: #10b981; background: #ecfdf5; }
.option.wrong { border-color: #ef4444; background: #fef2f2; }
.opt-letter { width: 48rpx; height: 48rpx; border-radius: 12rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; font-weight: 700; color: #64748b; background: #f1f5f9; }
.opt-letter.active { background: #2b6ff2; color: #fff; }
.opt-text { font-size: 28rpx; color: #1e293b; flex: 1; }
.opt-mark { font-size: 28rpx; color: #10b981; font-weight: 700; }
.fill-input { border: 2rpx solid #e2e8f0; border-radius: 16rpx; padding: 24rpx; font-size: 28rpx; background: #f8fafc; }
.quiz-result { display: flex; align-items: flex-start; gap: 16rpx; margin-top: 24rpx; padding: 20rpx; border-radius: 16rpx; }
.quiz-result.is-correct { background: #ecfdf5; }
.quiz-result.is-wrong { background: #fef2f2; }
.result-icon { font-size: 32rpx; font-weight: 700; margin-top: 4rpx; }
.result-verdict { display: block; font-size: 26rpx; font-weight: 700; }
.result-answer { display: block; font-size: 24rpx; color: #64748b; margin-top: 4rpx; }
.result-analysis { display: block; font-size: 24rpx; color: #94a3b8; margin-top: 4rpx; }
.btn-submit, .btn-next, .btn-finish { width: 100%; height: 96rpx; border: none; border-radius: 48rpx; font-size: 32rpx; font-weight: 700; margin-top: 30rpx; }
.btn-submit { background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; }
.btn-submit[disabled] { background: #cbd5e1; color: #94a3b8; }
.btn-next { background: #0f172a; color: #fff; }
.btn-finish { background: linear-gradient(135deg, #10b981, #34d399); color: #fff; }

// Finish
.finish-screen { display: flex; flex-direction: column; align-items: center; padding-top: 180rpx; }
.finish-circle { width: 200rpx; height: 200rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 24rpx; }
.finish-circle.good { background: linear-gradient(135deg, #dcfce7, #bbf7d0); }
.finish-circle.retry { background: linear-gradient(135deg, #fef3c7, #fde68a); }
.finish-score { font-size: 48rpx; font-weight: 900; color: #0f172a; }
.finish-msg { font-size: 36rpx; font-weight: 700; color: #0f172a; }
.btn-retry, .btn-back { width: 460rpx; height: 96rpx; border: none; border-radius: 48rpx; font-size: 32rpx; font-weight: 700; }
.btn-retry { background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; margin-top: 60rpx; }
.btn-back { background: #f1f5f9; color: #64748b; margin-top: 20rpx; }
</style>
