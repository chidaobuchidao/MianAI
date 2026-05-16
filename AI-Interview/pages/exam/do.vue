<template>
  <view class="exam-do">
    <!-- 顶部栏 -->
    <view class="top-bar">
      <text class="top-title">{{ examInfo?.title || '考试中' }}</text>
      <view class="timer" :class="{ warn: remainingTime < 60 }">
        <text>{{ formatTime(remainingTime) }}</text>
      </view>
    </view>

    <!-- 进度条 -->
    <view class="progress-wrap">
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: progressPct + '%' }" />
      </view>
      <text class="progress-text">{{ currentIndex + 1 }} / {{ questions.length }}</text>
    </view>

    <!-- 题目卡片 -->
    <view class="q-card" v-if="currentQ">
      <text class="q-num">第 {{ currentIndex + 1 }} 题</text>
      <text class="q-title">{{ currentQ.title }}</text>

      <!-- 单选/多选 -->
      <view v-if="currentQ.type <= 2" class="opts">
        <view
          v-for="o in parseOpts(currentQ.options)"
          :key="o.label"
          class="opt"
          :class="{ sel: answers[currentQ.id] === o.label }"
          @click="answers[currentQ.id] = o.label"
        >
          <view class="opt-lab" :class="{ on: answers[currentQ.id] === o.label }">{{ o.label }}</view>
          <text class="opt-txt">{{ o.content }}</text>
        </view>
      </view>

      <!-- 判断 -->
      <view v-if="currentQ.type === 3" class="opts">
        <view
          v-for="v in ['正确','错误']" :key="v"
          class="opt" :class="{ sel: answers[currentQ.id] === v }"
          @click="answers[currentQ.id] = v"
        >
          <view class="opt-lab" :class="{ on: answers[currentQ.id] === v }">{{ v === '正确' ? '✓' : '✗' }}</view>
          <text class="opt-txt">{{ v }}</text>
        </view>
      </view>

      <!-- 填空 -->
      <input v-if="currentQ.type === 4" class="fill-input" v-model="answers[currentQ.id]" placeholder="输入答案..." />
    </view>

    <!-- 底部按钮 -->
    <view class="bottom-bar">
      <view class="bottom-btn" :class="{ disabled: currentIndex === 0 }" @click="prev">
        <text>上一题</text>
      </view>
      <view v-if="currentIndex < questions.length - 1" class="bottom-btn primary" @click="next">
        <text>下一题</text>
      </view>
      <view v-else class="bottom-btn submit" @click="submitExam">
        <text>交卷</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post } from '@/utils/request';

interface Q { id: number; type: number; title: string; options: string; }
interface ExamInfo { id: number; title: string; duration: number; totalScore: number; }

const examInfo = ref<ExamInfo|null>(null); const examId = ref(0);
const questions = ref<Q[]>([]); const currentIndex = ref(0); const currentQ = ref<Q|null>(null);
const answers = ref<Record<number,string>>({}); const remainingTime = ref(0);
let timer: ReturnType<typeof setInterval>|null = null;

const progressPct = computed(() => {
  if (questions.value.length === 0) return 0;
  return Math.round(((currentIndex.value + 1) / questions.value.length) * 100);
});

onLoad(opts => { examId.value = Number(opts?.examId); remainingTime.value = (Number(opts?.duration)||30)*60; });
onMounted(async () => { try {
  const r = await post<{exam:ExamInfo;questions:Q[]}>(`/api/exams/${examId.value}/start`);
  examInfo.value = r.data.exam; questions.value = r.data.questions;
  if (questions.value.length>0) currentQ.value = questions.value[0];
  timer = setInterval(() => { remainingTime.value--; if (remainingTime.value<=0) submitExam(); }, 1000);
} catch { uni.showToast({title:'加载失败',icon:'error'}); } });
onUnmounted(() => { if (timer) clearInterval(timer); });

function parseOpts(o: string) { if (!o) return []; try { return JSON.parse(o); } catch { return []; } }
function formatTime(s: number) { return `${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`; }
function prev() { if (currentIndex.value>0) { currentIndex.value--; currentQ.value = questions.value[currentIndex.value]; } }
function next() { if (currentIndex.value<questions.value.length-1) { currentIndex.value++; currentQ.value = questions.value[currentIndex.value]; } }

async function submitExam() {
  if (timer) clearInterval(timer);
  uni.showModal({ title:'确认交卷', content:`还有 ${questions.value.length - Object.keys(answers.value).length} 题未答`, success: async res => {
    if (!res.confirm && remainingTime.value>0) { timer = setInterval(() => { remainingTime.value--; if (remainingTime.value<=0) submitExam(); }, 1000); return; }
    try { const list = questions.value.map(q => ({ questionId:q.id, userAnswer:answers.value[q.id]||'' }));
      const r = await post<{totalScore:number;correctCount:number;totalCount:number;examTitle:string}>(`/api/exams/${examId.value}/submit`,{answers:list});
      uni.redirectTo({ url: `/pages/interview/report?type=exam&score=${r.data.totalScore}&correct=${r.data.correctCount}&total=${r.data.totalCount}&title=${encodeURIComponent(r.data.examTitle)}` });
    } catch {}
  }});
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.exam-do { min-height: 100vh; background: $bg-canvas; display: flex; flex-direction: column; }

// 顶部栏
.top-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24rpx 28rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light;
}
.top-title { font-size: 28rpx; font-weight: 600; color: $text-main; }
.timer {
  font-family: monospace; font-size: 28rpx; font-weight: 600; color: $accent;
  background: rgba(217,117,10,0.08); padding: 8rpx 20rpx; border-radius: $radius-full;
}
.timer.warn { color: $color-danger; background: rgba(239,68,68,0.08); }

// 进度条
.progress-wrap {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 28rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light;
}
.progress-bar { flex: 1; height: 6rpx; background: $bg-surface; border-radius: 3rpx; overflow: hidden; }
.progress-fill { height: 100%; background: $accent; border-radius: 3rpx; transition: width 0.3s; }
.progress-text { font-size: 24rpx; color: $text-light; font-weight: 500; }

// 题目卡片
.q-card {
  flex: 1; margin: 20rpx; padding: 36rpx 28rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; box-shadow: $shadow-sm; overflow-y: auto;
}
.q-num { font-size: 24rpx; color: $accent; font-weight: 600; margin-bottom: 20rpx; display: block; }
.q-title { font-size: 30rpx; color: $text-main; line-height: 1.8; margin-bottom: 32rpx; display: block; }

// 选项
.opts { display: flex; flex-direction: column; gap: 14rpx; }
.opt {
  display: flex; align-items: center; gap: 18rpx;
  padding: 24rpx 22rpx; border: 1px solid $border-light;
  border-radius: $radius-md; font-size: 28rpx; color: $text-main;
}
.opt:active { background: $bg-surface; }
.opt.sel { border-color: $accent; background: rgba(217,117,10,0.04); }
.opt-lab {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1px solid $border-medium; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; font-weight: 600;
  color: $text-light; flex-shrink: 0; background: $bg-surface;
}
.opt-lab.on { background: $bg-dark; color: #fff; border-color: $bg-dark; }
.opt-txt { flex: 1; line-height: 1.6; }

// 填空
.fill-input {
  width: 100%; border: 1px solid $border-medium; border-radius: $radius-md;
  padding: 20rpx 24rpx; font-size: 28rpx; background: $bg-surface;
  box-sizing: border-box;
}

// 底部
.bottom-bar {
  display: flex; gap: 16rpx; padding: 20rpx 28rpx 44rpx;
  background: $bg-paper; border-top: 1px solid $border-light;
}
.bottom-btn {
  flex: 1; height: 88rpx; background: $bg-surface;
  color: $text-main; font-size: 28rpx; font-weight: 500;
  border-radius: $radius-xl; border: none;
  display: flex; align-items: center; justify-content: center;
}
.bottom-btn:active { opacity: 0.9; }
.bottom-btn.disabled { opacity: 0.4; }
.bottom-btn.primary { background: $bg-dark; color: #fff; }
.bottom-btn.submit { background: $accent; color: #fff; font-weight: 600; }

@media (min-width: 1025px) { .exam-do { max-width: 800px; margin: 0 auto; } }
</style>
