<template>
  <view class="report-page">
    <view class="score-hero">
      <view class="score-ring" :class="score >= 7 ? 'great' : score >= 4 ? 'ok' : 'low'">
        <text class="score-num">{{ score }}</text>
        <text class="score-unit">/10</text>
      </view>
      <text class="score-label">综合评分</text>
    </view>

    <view class="card" v-if="feedback">
      <text class="card-label">考官总评</text>
      <text class="card-text">{{ feedback }}</text>
    </view>

    <view class="card" v-if="dimensions.length">
      <text class="card-label">能力维度</text>
      <view class="dim-item" v-for="d in dimensions" :key="d.name">
        <view class="dim-head">
          <text class="dim-name">{{ d.name }}</text>
          <text class="dim-score">{{ d.score }}/10</text>
        </view>
        <view class="dim-bar-bg"><view class="dim-bar-fill" :style="{ width: (d.score * 10) + '%' }" /></view>
        <text class="dim-comment" v-if="d.comment">{{ d.comment }}</text>
      </view>
    </view>

    <view class="card" v-if="suggestion">
      <text class="card-label">提升建议</text>
      <text class="card-text">{{ suggestion }}</text>
    </view>

    <view v-if="examResult" class="card" style="text-align:center;">
      <text class="exam-big">{{ examResult.correct }} / {{ examResult.total }}</text>
      <text class="exam-label">正确题数</text>
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

    <view class="actions">
      <button class="btn-primary" @click="goHome">返回首页</button>
    </view>
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
  // 解析报告
  if (opts?.report) {
    try { const r = JSON.parse(decodeURIComponent(opts.report)); score.value=r.score||0; feedback.value=r.feedback||''; dimensions.value=r.dimensions||[]; suggestion.value=r.suggestion||''; } catch { feedback.value='报告解析失败'; }
  } else if (opts?.type==='exam') {
    examResult.value = { correct:Number(opts.correct)||0, total:Number(opts.total)||0 };
    const pct = examResult.value.total>0 ? Math.round(examResult.value.correct/examResult.value.total*100) : 0;
    score.value = Math.round(pct/10); feedback.value = `答对 ${examResult.value.correct}/${examResult.value.total} 题，正确率 ${pct}%`;
    uni.setNavigationBarTitle({ title: decodeURIComponent(opts.title||'考试结果') });
  }

  // 加载聊天记录
  const stored = uni.getStorageSync('lastChatMessages');
  if (stored) {
    chatMessages.value = JSON.parse(stored);
    // 清除已读取的缓存
    uni.removeStorageSync('lastChatMessages');
  } else if (opts?.sessionId) {
    // 从历史记录进入，通过API加载
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
.report-page { min-height: 100vh; background: #f0f4ff; padding-bottom: 40rpx; }
.score-hero { display: flex; flex-direction: column; align-items: center; padding: 80rpx 0 60rpx; background: linear-gradient(135deg, #1a3a6b, #2b6ff2, #4f8dff); }
.score-ring { width: 180rpx; height: 180rpx; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 6rpx solid rgba(255,255,255,0.3); }
.score-num { font-size: 72rpx; font-weight: 900; color: #fff; }
.score-unit { font-size: 24rpx; color: rgba(255,255,255,0.7); }
.score-label { font-size: 28rpx; color: rgba(255,255,255,0.8); margin-top: 16rpx; }
.card { background: #fff; margin: 20rpx 24rpx; padding: 28rpx; border-radius: 20rpx; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03); }
.card-label { font-size: 28rpx; font-weight: 700; color: #0f172a; display: block; margin-bottom: 16rpx; }
.card-text { font-size: 26rpx; color: #64748b; line-height: 1.8; display: block; }
.dim-item { padding: 16rpx 0; }
.dim-item + .dim-item { border-top: 1rpx solid #f1f5f9; }
.dim-head { display: flex; justify-content: space-between; margin-bottom: 10rpx; }
.dim-name { font-size: 26rpx; font-weight: 600; color: #1e293b; }
.dim-score { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.dim-bar-bg { height: 6rpx; background: #e2e8f0; border-radius: 3rpx; overflow: hidden; }
.dim-bar-fill { height: 100%; background: linear-gradient(90deg, #2b6ff2, #6366f1); border-radius: 3rpx; transition: width 0.6s; }
.dim-comment { font-size: 22rpx; color: #94a3b8; margin-top: 6rpx; display: block; }
.exam-big { font-size: 64rpx; font-weight: 900; color: #2b6ff2; display: block; }
.exam-label { font-size: 26rpx; color: #94a3b8; margin-top: 6rpx; display: block; }
.chat-log-section { background: #fff; margin: 20rpx 24rpx; border-radius: 20rpx; overflow: hidden; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); }
.chat-log-toggle { display: flex; justify-content: space-between; align-items: center; padding: 28rpx; }
.chat-log-toggle:active { background: #f8fafc; }
.cl-label { font-size: 28rpx; font-weight: 700; color: #0f172a; }
.cl-arrow { font-size: 24rpx; color: #94a3b8; }
.chat-log-list { padding: 0 28rpx 28rpx; max-height: 600rpx; overflow-y: auto; }
.cl-msg { margin-bottom: 16rpx; }
.cl-role { display: block; font-size: 22rpx; font-weight: 600; margin-bottom: 4rpx; }
.cl-msg.user .cl-role { color: #2b6ff2; }
.cl-msg.ai .cl-role { color: #10b981; }
.cl-content { display: block; font-size: 24rpx; color: #64748b; line-height: 1.6; }

.actions { padding: 30rpx 24rpx; }
.btn-primary { width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; }
</style>
