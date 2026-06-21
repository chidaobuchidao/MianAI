<template>
  <view class="detail-page">
    <view class="detail-body" v-if="question">
      <view class="q-card">
        <view class="q-meta">
          <text class="q-type">{{ typeLabel(question.type) }}</text>
        </view>
        <text class="q-title">{{ question.title }}</text>

        <view v-if="question.type <= 2" class="options">
          <view
            v-for="opt in parseOptions(question.options)"
            :key="opt.label"
            class="opt"
            :class="{ correct: opt.label === question.answer }"
          >
            <view class="opt-letter" :class="{ on: opt.label === question.answer }">{{ opt.label }}</view>
            <text class="opt-text">{{ opt.content }}</text>
            <view v-if="opt.label === question.answer" class="opt-check"><MianIcon name="check" size="28rpx" color="#16A34A" stroke-width="2.3" /></view>
          </view>
        </view>

        <view v-if="question.type === 3" class="options">
          <view class="opt" :class="{ correct: question.answer === correctText }">
            <view class="opt-letter" :class="{ on: question.answer === correctText }"><MianIcon name="check" size="28rpx" :color="question.answer === correctText ? '#fff' : '#4A4A4A'" stroke-width="2.3" /></view>
            <text class="opt-text">{{ correctText }}</text>
          </view>
          <view class="opt" :class="{ correct: question.answer === wrongText }">
            <view class="opt-letter" :class="{ on: question.answer === wrongText }"><MianIcon name="close" size="28rpx" :color="question.answer === wrongText ? '#fff' : '#4A4A4A'" stroke-width="2.3" /></view>
            <text class="opt-text">{{ wrongText }}</text>
          </view>
        </view>

        <view v-if="question.type === 4" class="fill-answer">
          <text class="fill-label">参考答案：</text>
          <text class="fill-text">{{ question.answer }}</text>
        </view>
      </view>

      <view class="analysis-card" v-if="question.analysis">
        <text class="analysis-title">答案解析</text>
        <text class="analysis-text">{{ question.analysis }}</text>
      </view>

      <view class="tag-row">
        <text class="tag">{{ question.categoryName }}</text>
        <text class="tag">{{ difficultyLabel(question.difficulty) }}</text>
      </view>
    </view>

    <view v-else class="empty-state"><text>加载中...</text></view>

    <view class="bottom-actions">
      <view class="action-btn" :class="{ disabled: !hasPrev() }" @click="goPrev">&lt; 上一题</view>
      <view class="action-btn primary" :class="{ disabled: !hasNext() }" @click="goNext">下一题 &gt;</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get } from '@/utils/request';
import MianIcon from '@/components/MianIcon.vue';

interface Question {
  id: number;
  categoryName: string;
  type: number;
  title: string;
  options: string;
  answer: string;
  analysis: string;
  difficulty: number;
}

interface Option { label: string; content: string }

const questionId = ref(0);
const categoryId = ref('');
const question = ref<Question | null>(null);
const questionIds = ref<number[]>([]);
const currentIndex = ref(-1);
const correctText = '\u6b63\u786e';
const wrongText = '\u9519\u8bef';

onLoad((opts: Record<string, string> | undefined) => {
  if (opts) {
    questionId.value = Number(opts.id || 0);
    categoryId.value = opts.categoryId || '';
  }
});

function parseOptions(o: string): Option[] {
  if (!o) return [];
  try { return JSON.parse(o) as Option[]; } catch { return []; }
}

function typeLabel(t: number): string {
  return ['', '单选题', '多选题', '判断题', '填空题'][t] || '';
}

function difficultyLabel(d: number): string {
  return d === 3 ? '困难' : d === 2 ? '中等' : '简单';
}

function hasPrev(): boolean {
  return currentIndex.value > 0;
}

function hasNext(): boolean {
  return currentIndex.value < questionIds.value.length - 1;
}

async function fetchDetail() {
  try {
    const r = await get<Question>(`/api/questions/${questionId.value}`);
    question.value = r.data;
  } catch {
    // 静默失败
  }
}

async function fetchQuestionList() {
  try {
    const params: Record<string, unknown> = { size: 200 };
    if (categoryId.value) params.categoryId = categoryId.value;
    const r = await get<{ records: Question[] }>('/api/questions', params);
    const data = r.data as Record<string, unknown>;
    const records: Question[] = (data.records || data || []) as Question[];
    questionIds.value = records.map(q => q.id);
    currentIndex.value = questionIds.value.indexOf(questionId.value);
  } catch {
    // 静默失败
  }
}

function goPrev() {
  if (currentIndex.value <= 0) return;
  const prevId = questionIds.value[currentIndex.value - 1];
  uni.redirectTo({ url: `/pages/question-bank/detail?id=${prevId}&categoryId=${categoryId.value}` });
}

function goNext() {
  if (currentIndex.value >= questionIds.value.length - 1) return;
  const nextId = questionIds.value[currentIndex.value + 1];
  uni.redirectTo({ url: `/pages/question-bank/detail?id=${nextId}&categoryId=${categoryId.value}` });
}

onMounted(async () => {
  await fetchQuestionList();
  await fetchDetail();
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.detail-page { min-height: 100vh; background: $bg-canvas; display: flex; flex-direction: column; }
.detail-body { flex: 1; padding: 20rpx 28rpx; }

.q-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-xl; padding: 36rpx 28rpx;
  box-shadow: $shadow-sm; margin-bottom: 16rpx;
}
.q-meta { margin-bottom: 20rpx; }
.q-type { font-size: 22rpx; font-weight: 600; color: $accent; text-transform: uppercase; letter-spacing: 1px; }
.q-title { font-size: 32rpx; color: $text-main; line-height: 1.8; margin-bottom: 32rpx; display: block; }

.options { display: flex; flex-direction: column; gap: 14rpx; }
.opt {
  display: flex; align-items: center; gap: 18rpx;
  padding: 24rpx 22rpx; border: 1px solid $border-light;
  border-radius: $radius-md; font-size: 28rpx; color: $text-main;
  background: $bg-paper;
}
.opt.correct { border-color: $color-success; background: rgba(34,197,94,0.04); }
.opt-letter {
  width: 52rpx; height: 52rpx; border-radius: 50%;
  border: 1px solid $border-medium; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; font-weight: 600;
  color: $text-light; flex-shrink: 0; background: $bg-surface;
}
.opt-letter.on { background: $color-success; color: #fff; border-color: $color-success; }
.opt-text { flex: 1; line-height: 1.6; }
.opt-check { width: 32rpx; height: 32rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.fill-answer { background: $bg-surface; border-radius: $radius-md; padding: 24rpx; display: flex; gap: 12rpx; }
.fill-label { font-size: 26rpx; color: $text-light; }
.fill-text { font-size: 26rpx; font-weight: 500; color: $text-main; }

.analysis-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx 24rpx; margin-bottom: 16rpx;
}
.analysis-title { font-size: 28rpx; font-weight: 600; color: $text-main; display: block; margin-bottom: 12rpx; }
.analysis-text { font-size: 26rpx; color: $text-muted; line-height: 1.8; }

.tag-row { display: flex; gap: 14rpx; flex-wrap: wrap; }
.tag { font-size: 24rpx; color: $text-muted; background: $bg-surface; padding: 10rpx 24rpx; border-radius: $radius-full; }

.bottom-actions { display: flex; gap: 16rpx; padding: 20rpx 28rpx 44rpx; background: $bg-paper; border-top: 1px solid $border-light; }
.action-btn {
  flex: 1; height: 88rpx; background: $bg-surface;
  color: $text-main; font-size: 28rpx; font-weight: 500;
  border-radius: $radius-xl; border: none;
  display: flex; align-items: center; justify-content: center;
}
.action-btn:active { opacity: 0.9; }
.action-btn.disabled { opacity: 0.3; }
.action-btn.primary { background: $bg-dark; color: #fff; }

.empty-state { text-align: center; padding-top: 200rpx; }

@media (min-width: 1025px) { .detail-page { max-width: 800px; margin: 0 auto; } }
</style>
