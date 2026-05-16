<template>
  <view class="history">
    <view class="page-header">
      <text class="page-title">面试历史</text>
    </view>

    <view v-if="list.length" class="list">
      <view class="history-card" v-for="s in list" :key="s.id" @click="goDetail(s)">
        <view class="history-card-top">
          <text class="history-card-pos">{{ s.position }}</text>
          <view class="history-card-score" :class="scoreClass(s.overallScore)">
            <text>{{ s.overallScore }}分</text>
          </view>
        </view>
        <text class="history-card-fb" v-if="s.feedback">{{ s.feedback }}</text>
        <view class="history-card-bottom">
          <text class="history-card-time">{{ s.createTime }}</text>
          <view class="history-card-status" :class="s.overallScore >= 7 ? 'status-good' : s.overallScore >= 4 ? 'status-ok' : 'status-low'">
            <text>{{ s.overallScore >= 7 ? '优秀' : s.overallScore >= 4 ? '完成' : '查看' }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📋</text>
      <text class="empty-title">暂无记录</text>
      <text class="empty-desc">完成一场 AI 面试后，记录会显示在这里</text>
      <view class="empty-btn" @click="goChat">
        <text>开始面试</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';
interface S { id: number; position: string; overallScore: number; feedback: string; createTime: string; }
const list = ref<S[]>([]);
onMounted(async () => { try { const r = await get<S[]>('/api/interview/list'); list.value = r.data; } catch {} });

function scoreClass(s: number) {
  if (s >= 7) return 'score-good';
  if (s >= 4) return 'score-ok';
  return 'score-low';
}

function goDetail(s: S) {
  uni.removeStorageSync('lastChatMessages');
  uni.navigateTo({ url: `/pages/interview/report?sessionId=${s.id}` });
}
function goChat() { uni.navigateTo({ url: '/pages/interview/chat' }); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.history { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

.page-header { margin-bottom: 32rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; letter-spacing: -0.5px;
}

.list { display: flex; flex-direction: column; gap: 16rpx; }

.history-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx; box-shadow: $shadow-sm;
}
.history-card:active { background: $bg-surface; }
.history-card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.history-card-pos { font-size: 28rpx; font-weight: 500; color: $text-main; }
.history-card-score {
  font-size: 24rpx; font-weight: 600; padding: 6rpx 16rpx;
  border-radius: $radius-full;
}
.score-good { background: rgba(34,197,94,0.1); color: $color-success; }
.score-ok { background: rgba(217,117,10,0.1); color: $accent; }
.score-low { background: rgba(239,68,68,0.1); color: $color-danger; }

.history-card-fb {
  font-size: 24rpx; color: $text-muted; line-height: 1.6;
  display: block; margin-bottom: 14rpx;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden;
}
.history-card-bottom { display: flex; justify-content: space-between; align-items: center; }
.history-card-time { font-size: 22rpx; color: $text-light; }
.history-card-status {
  font-size: 20rpx; font-weight: 500; padding: 4rpx 14rpx; border-radius: $radius-full;
}
.status-good { background: rgba(34,197,94,0.08); color: $color-success; }
.status-ok { background: $bg-surface; color: $text-light; }
.status-low { background: rgba(239,68,68,0.08); color: $color-danger; }

.empty-state { text-align: center; padding-top: 200rpx; }
.empty-icon { font-size: 96rpx; display: block; margin-bottom: 24rpx; }
.empty-title { font-size: 28rpx; color: $text-main; font-weight: 500; display: block; margin-bottom: 10rpx; }
.empty-desc { font-size: 24rpx; color: $text-light; display: block; margin-bottom: 44rpx; }
.empty-btn {
  width: 320rpx; height: 88rpx; background: $bg-dark; color: #fff;
  font-size: 28rpx; font-weight: 600; border-radius: $radius-lg; border: none;
  display: inline-flex; align-items: center; justify-content: center;
}
.empty-btn:active { opacity: 0.9; }

@media (min-width: 1025px) { .history { max-width: 700px; margin: 0 auto; } }
</style>
