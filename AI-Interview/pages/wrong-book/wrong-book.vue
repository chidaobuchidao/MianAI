<template>
  <view class="wrong-book">
    <view class="head-bar">
      <text class="h-title">错题本</text>
      <text class="h-count" v-if="questions.length">积累 {{ questions.length }} 题</text>
    </view>

    <view v-if="questions.length" class="list">
      <view class="item" v-for="q in questions" :key="q.id">
        <view class="item-body">
          <text class="item-title">{{ q.title }}</text>
          <text class="item-ans">正确答案：{{ q.answer }}</text>
        </view>
        <view class="item-fn" @click="removeWrong(q.id)">
          <text>已掌握</text>
        </view>
      </view>
    </view>

    <view v-else class="empty">
      <text class="empty-icon">✨</text>
      <text class="empty-title">错题本为空</text>
      <text class="empty-desc">刷题中做错的题目会自动收集到这里</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get, del } from '@/utils/request';
interface Q { id: number; categoryName: string; title: string; answer: string; }
const questions = ref<Q[]>([]);
onMounted(async () => { try { const r = await get<Q[]>('/api/wrong-questions'); questions.value = r.data; } catch {} });
async function removeWrong(id: number) { try { await del(`/api/wrong-questions/${id}`); questions.value = questions.value.filter(q => q.id !== id); uni.showToast({ title:'已掌握', icon:'success' }); } catch {} }
</script>

<style lang="scss" scoped>
.wrong-book { min-height: 100vh; background: #f0f4ff; }
.head-bar { display: flex; justify-content: space-between; align-items: baseline; padding: 40rpx 30rpx 20rpx; }
.h-title { font-size: 36rpx; font-weight: 800; color: #0f172a; }
.h-count { font-size: 24rpx; color: #94a3b8; }
.list { padding: 0 24rpx; }
.item { display: flex; align-items: center; gap: 20rpx; background: #fff; margin-bottom: 16rpx; padding: 28rpx; border-radius: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); }
.item-body { flex: 1; }
.item-title { font-size: 28rpx; font-weight: 500; color: #1e293b; line-height: 1.6; display: block; }
.item-ans { font-size: 24rpx; color: #10b981; margin-top: 8rpx; display: block; }
.item-fn { font-size: 24rpx; color: #2b6ff2; padding: 12rpx 20rpx; border: 1rpx solid #2b6ff2; border-radius: 20rpx; white-space: nowrap; }
.item-fn:active { background: #f0f4ff; }
.empty { display: flex; flex-direction: column; align-items: center; padding-top: 220rpx; }
.empty-icon { font-size: 100rpx; }
.empty-title { font-size: 32rpx; font-weight: 700; color: #0f172a; margin-top: 20rpx; }
.empty-desc { font-size: 26rpx; color: #94a3b8; margin-top: 10rpx; }
</style>
