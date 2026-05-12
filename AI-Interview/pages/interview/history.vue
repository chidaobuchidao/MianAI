<template>
  <view class="history">
    <view class="head-bar"><text class="h-title">面试历史</text></view>
    <view v-if="list.length" class="list">
      <view class="card" v-for="s in list" :key="s.id" @click="goDetail(s)">
        <view class="top"><text class="pos">{{ s.position }}</text><text class="score" :class="s.overallScore>=7?'g':s.overallScore>=4?'m':'b'">{{ s.overallScore }}分</text></view>
        <text class="fb" v-if="s.feedback">{{ s.feedback }}</text>
        <text class="time">{{ s.createTime }}</text>
      </view>
    </view>
    <view v-else class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-title">暂无记录</text>
      <button class="btn-go" @click="goChat">开始面试</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';
interface S { id: number; position: string; overallScore: number; feedback: string; createTime: string; }
const list = ref<S[]>([]);
onMounted(async () => { try { const r = await get<S[]>('/api/interview/list'); list.value = r.data; } catch {} });
function goDetail(s: S) {
  // 从历史页进入：清除可能存在的旧缓存，通过 sessionId 加载数据
  uni.removeStorageSync('lastChatMessages');
  uni.navigateTo({ url: `/pages/interview/report?sessionId=${s.id}` });
}
function goChat() { uni.navigateTo({ url: '/pages/interview/chat' }); }
</script>

<style lang="scss" scoped>
.history { min-height: 100vh; background: #f0f4ff; }
.head-bar { padding: 40rpx 30rpx 20rpx; }
.h-title { font-size: 36rpx; font-weight: 800; color: #0f172a; }
.list { padding: 0 24rpx; }
.card { background: #fff; margin-bottom: 16rpx; padding: 28rpx; border-radius: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); }
.card:active { background: #f8fafc; }
.top { display: flex; justify-content: space-between; align-items: center; }
.pos { font-size: 30rpx; font-weight: 700; color: #0f172a; }
.score { font-size: 32rpx; font-weight: 800; }
.score.g { color: #10b981; } .score.m { color: #f59e0b; } .score.b { color: #ef4444; }
.fb { font-size: 24rpx; color: #94a3b8; margin-top: 10rpx; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.time { font-size: 22rpx; color: #cbd5e1; margin-top: 10rpx; display: block; }
.empty { display: flex; flex-direction: column; align-items: center; padding-top: 220rpx; }
.empty-icon { font-size: 100rpx; }
.empty-title { font-size: 28rpx; color: #94a3b8; margin-top: 20rpx; }
.btn-go { width: 300rpx; height: 80rpx; margin-top: 40rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 28rpx; font-weight: 700; border-radius: 40rpx; border: none; }
</style>
