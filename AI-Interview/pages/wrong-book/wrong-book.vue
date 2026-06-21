<template>
  <view class="wrong-book">
    <view class="page-header">
      <text class="page-title">错题本</text>
      <text class="page-count" v-if="questions.length">积累 {{ questions.length }} 题</text>
    </view>

    <view v-if="questions.length" class="list">
      <view class="wrong-card" v-for="q in questions" :key="q.id">
        <view class="wrong-card-indicator" />
        <view class="wrong-card-body">
          <text class="wrong-card-title">{{ q.title }}</text>
          <text class="wrong-card-answer">正确答案：{{ q.answer }}</text>
          <text class="wrong-card-category" v-if="q.categoryName">{{ q.categoryName }}</text>
        </view>
        <view class="wrong-card-action" @click="removeWrong(q.id)">
          <MianIcon name="check" size="30rpx" color="#4A4A4A" stroke-width="2.2" />
          <text class="wrong-card-action-label">掌握</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <view class="empty-icon"><MianIcon name="sparkle" size="96rpx" color="#D9750A" stroke-width="1.7" /></view>
      <text class="empty-title">还没有错题</text>
      <text class="empty-desc">刷题中做错的题目会自动收集到这里</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get, del } from '@/utils/request';
import MianIcon from '@/components/MianIcon.vue';
interface Q { id: number; categoryName: string; title: string; answer: string; }
const questions = ref<Q[]>([]);
onMounted(async () => { try { const r = await get<Q[]>('/api/wrong-questions'); questions.value = r.data; } catch {} });
async function removeWrong(id: number) {
  try {
    await del(`/api/wrong-questions/${id}`);
    questions.value = questions.value.filter(q => q.id !== id);
    uni.showToast({ title: '已掌握', icon: 'success' });
  } catch {
    uni.showToast({ title: '操作失败，请重试', icon: 'error' });
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.wrong-book { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

.page-header { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 32rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; letter-spacing: -0.5px;
}
.page-count { font-size: 24rpx; color: $color-danger; font-weight: 500; }

.list { display: flex; flex-direction: column; gap: 16rpx; }

.wrong-card {
  display: flex; gap: 16rpx; align-items: flex-start;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx; box-shadow: $shadow-sm;
}
.wrong-card:active { background: $bg-surface; }
.wrong-card-indicator {
  width: 8rpx; height: 8rpx; background: $color-danger;
  border-radius: 50%; margin-top: 10rpx; flex-shrink: 0;
}
.wrong-card-body { flex: 1; min-width: 0; }
.wrong-card-title { font-size: 28rpx; font-weight: 500; color: $text-main; line-height: 1.6; display: block; margin-bottom: 10rpx; }
.wrong-card-answer { font-size: 24rpx; color: $color-success; display: block; margin-bottom: 6rpx; }
.wrong-card-category { font-size: 22rpx; color: $text-light; }

.wrong-card-action {
  display: flex; flex-direction: column; align-items: center; gap: 4rpx;
  padding: 12rpx 16rpx; flex-shrink: 0; color: $text-light;
  font-size: 22rpx; border-radius: $radius-sm;
}
.wrong-card-action:active { background: $bg-surface; }
.wrong-card-action-label { font-size: 20rpx; }

.empty-state { text-align: center; padding-top: 200rpx; }
.empty-icon { display: flex; justify-content: center; margin-bottom: 24rpx; }
.empty-title { font-size: 28rpx; color: $text-main; font-weight: 500; display: block; margin-bottom: 10rpx; }
.empty-desc { font-size: 24rpx; color: $text-light; display: block; line-height: 1.6; }

@media (min-width: 1025px) { .wrong-book { max-width: 700px; margin: 0 auto; } }
</style>
