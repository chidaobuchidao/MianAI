<template>
  <view class="exam-do">
    <view class="top-bar">
      <text class="top-title">{{ examInfo?.title }}</text>
      <text class="timer" :class="{ warn: remainingTime < 60 }">{{ formatTime(remainingTime) }}</text>
    </view>

    <view class="q-card" v-if="currentQ">
      <text class="q-num">{{ currentIndex + 1 }} / {{ questions.length }}</text>
      <text class="q-title">{{ currentQ.title }}</text>

      <view v-if="currentQ.type <= 2" class="opts">
        <view v-for="o in parseOpts(currentQ.options)" :key="o.label" class="opt" :class="{ sel: answers[currentQ.id] === o.label }" @click="answers[currentQ.id] = o.label">
          <view class="opt-lab" :class="{ on: answers[currentQ.id] === o.label }">{{ o.label }}</view>
          <text class="opt-txt">{{ o.content }}</text>
        </view>
      </view>

      <view v-if="currentQ.type === 3" class="opts">
        <view v-for="v in ['正确','错误']" :key="v" class="opt" :class="{ sel: answers[currentQ.id] === v }" @click="answers[currentQ.id] = v">
          <view class="opt-lab" :class="{ on: answers[currentQ.id] === v }">{{ v === '正确' ? '✓' : '✗' }}</view>
          <text class="opt-txt">{{ v }}</text>
        </view>
      </view>

      <input v-if="currentQ.type === 4" class="fill" v-model="answers[currentQ.id]" placeholder="输入答案" />
    </view>

    <view class="bottom-bar">
      <button class="b-btn" @click="prev" :disabled="currentIndex === 0">上一题</button>
      <text class="b-indicator">{{ currentIndex + 1 }}/{{ questions.length }}</text>
      <button v-if="currentIndex < questions.length - 1" class="b-btn" @click="next">下一题</button>
      <button v-else class="b-btn submit" @click="submitExam">交卷</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get, post } from '@/utils/request';
interface Q { id: number; type: number; title: string; options: string; }
interface ExamInfo { id: number; title: string; duration: number; totalScore: number; }
const examInfo = ref<ExamInfo|null>(null); const examId = ref(0);
const questions = ref<Q[]>([]); const currentIndex = ref(0); const currentQ = ref<Q|null>(null);
const answers = ref<Record<number,string>>({}); const remainingTime = ref(0);
let timer: ReturnType<typeof setInterval>|null = null;

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
.exam-do { min-height: 100vh; background: #f0f4ff; padding-bottom: 140rpx; }
.top-bar { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 30rpx; background: #fff; }
.top-title { font-size: 28rpx; font-weight: 600; color: #0f172a; }
.timer { font-size: 36rpx; font-weight: 800; color: #2b6ff2; }
.timer.warn { color: #ef4444; animation: pulse 0.5s infinite alternate; }
@keyframes pulse { to { opacity: 0.5; } }
.q-card { background: #fff; margin: 20rpx 24rpx; padding: 30rpx; border-radius: 24rpx; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03); }
.q-num { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.q-title { display: block; font-size: 30rpx; font-weight: 600; color: #0f172a; line-height: 1.8; margin-top: 16rpx; }
.opts { display: flex; flex-direction: column; gap: 14rpx; margin-top: 28rpx; }
.opt { display: flex; align-items: center; gap: 16rpx; padding: 18rpx; border: 2rpx solid #e2e8f0; border-radius: 14rpx; }
.opt.sel { border-color: #2b6ff2; background: #f0f4ff; }
.opt-lab { width: 44rpx; height: 44rpx; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; font-size: 22rpx; font-weight: 700; color: #64748b; background: #f1f5f9; }
.opt-lab.on { background: #2b6ff2; color: #fff; }
.opt-txt { font-size: 28rpx; color: #1e293b; flex: 1; }
.fill { border: 2rpx solid #e2e8f0; border-radius: 14rpx; padding: 22rpx; font-size: 28rpx; background: #f8fafc; margin-top: 28rpx; }
.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; display: flex; align-items: center; justify-content: space-between; padding: 16rpx 24rpx 44rpx; background: #fff; box-shadow: 0 -2rpx 20rpx rgba(0,0,0,0.05); }
.b-btn { padding: 14rpx 32rpx; border-radius: 24rpx; font-size: 26rpx; font-weight: 600; border: none; background: #f1f5f9; color: #334155; min-width: 130rpx; }
.b-btn.submit { background: #ef4444; color: #fff; }
.b-btn[disabled] { opacity: 0.4; }
.b-indicator { font-size: 26rpx; color: #64748b; font-weight: 600; }
</style>
