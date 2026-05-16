<template>
  <view class="report-page">
    <!-- 分数头部 -->
    <view class="score-hero">
      <view class="score-ring" :class="score >= 7 ? 'great' : score >= 4 ? 'ok' : 'low'">
        <text class="score-num">{{ score }}</text>
        <text class="score-unit">/10</text>
      </view>
      <text class="score-label">综合评分</text>
    </view>

    <!-- 考官总评 -->
    <view class="report-card" v-if="feedback">
      <text class="report-card-label">考官总评</text>
      <text class="report-card-text">{{ feedback }}</text>
    </view>

    <!-- 考试结果 -->
    <view v-if="examResult" class="report-card exam-result-card">
      <text class="exam-big">{{ examResult.correct }} / {{ examResult.total }}</text>
      <text class="exam-label">正确题数</text>
    </view>

    <!-- 能力维度 -->
    <view class="report-card" v-if="dimensions.length">
      <text class="report-card-label">能力维度</text>
      <view class="dim-item" v-for="d in dimensions" :key="d.name">
        <view class="dim-head">
          <text class="dim-name">{{ d.name }}</text>
          <text class="dim-score">{{ d.score }}/10</text>
        </view>
        <view class="dim-bar-bg">
          <view class="dim-bar-fill" :style="{ width: (d.score * 10) + '%' }" />
        </view>
        <text class="dim-comment" v-if="d.comment">{{ d.comment }}</text>
      </view>
    </view>

    <!-- 提升建议 -->
    <view class="report-card" v-if="suggestion">
      <text class="report-card-label">提升建议</text>
      <text class="report-card-text">{{ suggestion }}</text>
    </view>

    <!-- 对话记录 -->
    <view class="chat-log-section" v-if="chatMessages.length > 0">
      <view class="chat-log-toggle" @click="showChatLog = !showChatLog">
        <text class="cl-label">对话记录 ({{ chatMessages.length }} 条消息)</text>
        <text class="cl-arrow">{{ showChatLog ? '▲' : '▼' }}</text>
      </view>
      <view class="chat-log-list" v-if="showChatLog">
        <view v-for="(m, i) in chatMessages" :key="i" class="cl-msg" :class="m.role">
          <text class="cl-role">{{ m.role === 'user' ? '你' : '面试官' }}</text>
          <text class="cl-content">{{ m.content }}</text>
        </view>
      </view>
    </view>

    <!-- 底部按钮 -->
    <view class="actions">
      <view class="btn-primary" @click="goHome">
        <text>返回首页</text>
      </view>
    </view>

    <view class="bottom-safe" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get } from '@/utils/request';

interface Dim { name: string; score: number; comment: string; }
interface ChatMsg { role: string; content: string; time?: string; }

const score = ref(0); const feedback = ref(''); const dimensions = ref<Dim[]>([]);
const suggestion = ref(''); const showChatLog = ref(true);
const chatMessages = ref<ChatMsg[]>([]);
const examResult = ref<{correct:number;total:number}|null>(null);

onLoad(async (opts) => {
  if (opts?.report) {
    try {
      const r = JSON.parse(decodeURIComponent(opts.report));
      score.value = r.score || 0;
      feedback.value = r.feedback || '';
      dimensions.value = r.dimensions || [];
      suggestion.value = r.suggestion || '';
    } catch { feedback.value = '报告解析失败'; }
  } else if (opts?.type === 'exam') {
    examResult.value = { correct: Number(opts.correct) || 0, total: Number(opts.total) || 0 };
    const pct = examResult.value.total > 0 ? Math.round(examResult.value.correct / examResult.value.total * 100) : 0;
    score.value = Math.round(pct / 10);
    feedback.value = `答对 ${examResult.value.correct}/${examResult.value.total} 题，正确率 ${pct}%`;
    uni.setNavigationBarTitle({ title: decodeURIComponent(opts.title || '考试结果') });
  }

  const stored = uni.getStorageSync('lastChatMessages');
  if (stored) {
    chatMessages.value = JSON.parse(stored);
    uni.removeStorageSync('lastChatMessages');
  } else if (opts?.sessionId) {
    try {
      const res = await get<{ messages: string }>(`/api/interview/${opts.sessionId}`);
      if (res.data?.messages) {
        chatMessages.value = JSON.parse(res.data.messages);
      }
    } catch {}
  }
});

function goHome() { uni.switchTab({ url: '/pages/index/index' }); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.report-page { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

// ===== 分数 =====
.score-hero {
  display: flex; flex-direction: column; align-items: center;
  padding: 48rpx 0; margin-bottom: 32rpx;
}
.score-ring {
  width: 180rpx; height: 180rpx; border-radius: 50%;
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; margin-bottom: 20rpx;
  border: 3px solid transparent;
}
.score-ring.great { background: rgba(34,197,94,0.08); border-color: $color-success; }
.score-ring.ok { background: rgba(217,117,10,0.08); border-color: $accent; }
.score-ring.low { background: rgba(239,68,68,0.08); border-color: $color-danger; }

.score-num { font-family: Georgia, serif; font-size: 60rpx; font-weight: 700; color: $text-main; line-height: 1; }
.score-unit { font-size: 22rpx; color: $text-light; margin-top: 4rpx; }
.score-label {
  font-size: 26rpx; font-weight: 600; color: $text-main;
}

// ===== 通用卡片 =====
.report-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx; margin-bottom: 20rpx;
  box-shadow: $shadow-sm;
}
.report-card-label {
  font-size: 26rpx; font-weight: 600; color: $text-main;
  margin-bottom: 14rpx; display: block;
}
.report-card-text { font-size: 26rpx; color: $text-muted; line-height: 1.8; }

// 考试结果
.exam-result-card { text-align: center; padding: 40rpx 28rpx; }
.exam-big {
  font-family: Georgia, serif; font-size: 56rpx; font-weight: 700;
  color: $text-main; display: block; margin-bottom: 8rpx;
}
.exam-label { font-size: 24rpx; color: $text-light; }

// ===== 维度 =====
.dim-item { margin-top: 24rpx; }
.dim-item + .dim-item { border-top: 1px solid $border-light; padding-top: 24rpx; }
.dim-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.dim-name { font-size: 26rpx; font-weight: 500; color: $text-main; }
.dim-score { font-size: 26rpx; font-weight: 600; color: $accent; }
.dim-bar-bg { height: 8rpx; background: $bg-surface; border-radius: 4rpx; margin-bottom: 12rpx; overflow: hidden; }
.dim-bar-fill { height: 100%; background: $accent; border-radius: 4rpx; transition: width 0.5s; }
.dim-comment { font-size: 24rpx; color: $text-light; line-height: 1.6; }

// ===== 对话记录 =====
.chat-log-section { margin-bottom: 20rpx; }
.chat-log-toggle {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 28rpx; background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; box-shadow: $shadow-sm;
}
.chat-log-toggle:active { background: $bg-surface; }
.cl-label { font-size: 26rpx; font-weight: 500; color: $text-main; }
.cl-arrow { font-size: 24rpx; color: $text-light; }
.chat-log-list { padding: 20rpx 0; }
.cl-msg {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-md; padding: 20rpx 24rpx; margin-top: 12rpx;
}
.cl-msg.user { border-left: 4rpx solid $accent; }
.cl-role { font-size: 22rpx; font-weight: 600; color: $text-light; display: block; margin-bottom: 8rpx; }
.cl-content { font-size: 26rpx; color: $text-muted; line-height: 1.7; }

// ===== 底部按钮 =====
.actions { margin-top: 12rpx; }
.btn-primary {
  width: 100%; height: 96rpx; background: $bg-dark; color: #fff;
  font-size: 30rpx; font-weight: 600; border-radius: $radius-lg;
  border: none; display: flex; align-items: center; justify-content: center;
}
.btn-primary:active { opacity: 0.9; }

.bottom-safe { height: 60rpx; }

@media (min-width: 1025px) { .report-page { max-width: 700px; margin: 0 auto; } }
</style>
