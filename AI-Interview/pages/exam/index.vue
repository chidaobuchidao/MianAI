<template>
  <view class="exam-list">
    <view class="page-header">
      <text class="page-title">在线试卷</text>
      <text class="page-desc">限时模拟考试，查漏补缺</text>
    </view>

    <view v-if="exams.length" class="list">
      <view class="exam-card" v-for="e in exams" :key="e.id" @click="startExam(e)">
        <view class="exam-card-left">
          <text class="exam-card-title">{{ e.title }}</text>
          <text class="exam-card-desc" v-if="e.description">{{ e.description }}</text>
          <view class="exam-card-meta">
            <text class="exam-meta-tag">⏱ {{ e.duration }}分钟</text>
            <text class="exam-meta-tag">📊 {{ e.totalScore }}分</text>
          </view>
        </view>
        <view class="exam-card-arrow">
          <text>→</text>
        </view>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📝</text>
      <text class="empty-title">暂无试卷</text>
      <text class="empty-desc">管理员正在准备中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';
interface Exam { id: number; title: string; description: string; duration: number; totalScore: number; }
const exams = ref<Exam[]>([]);
onMounted(async () => { try { const r = await get<Exam[]>('/api/exams'); exams.value = r.data; } catch {} });
function startExam(e: Exam) {
  uni.showModal({ title: e.title, content: `时长 ${e.duration} 分钟，共 ${e.totalScore} 分，开始考试吗？`, success: r => {
    if (r.confirm) uni.navigateTo({ url: `/pages/exam/do?examId=${e.id}&duration=${e.duration}` });
  }});
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.exam-list { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

.page-header { margin-bottom: 36rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; display: block; margin-bottom: 10rpx; letter-spacing: -0.5px;
}
.page-desc { font-size: 26rpx; color: $text-muted; display: block; }

.list { display: flex; flex-direction: column; gap: 16rpx; }

.exam-card {
  display: flex; align-items: center; gap: 20rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 32rpx 28rpx;
  box-shadow: $shadow-sm;
}
.exam-card:active { background: $bg-surface; }
.exam-card-left { flex: 1; min-width: 0; }
.exam-card-title { font-size: 28rpx; font-weight: 500; color: $text-main; display: block; margin-bottom: 8rpx; }
.exam-card-desc { font-size: 24rpx; color: $text-light; display: block; margin-bottom: 14rpx; }
.exam-card-meta { display: flex; gap: 20rpx; }
.exam-meta-tag { font-size: 22rpx; color: $text-light; }
.exam-card-arrow {
  width: 56rpx; height: 56rpx; background: $bg-surface;
  border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 24rpx; color: $text-light;
  flex-shrink: 0;
}

.empty-state { text-align: center; padding-top: 200rpx; }
.empty-icon { font-size: 96rpx; display: block; margin-bottom: 24rpx; }
.empty-title { font-size: 28rpx; color: $text-main; font-weight: 500; display: block; margin-bottom: 8rpx; }
.empty-desc { font-size: 24rpx; color: $text-light; display: block; }

@media (min-width: 1025px) { .exam-list { max-width: 700px; margin: 0 auto; } }
</style>
