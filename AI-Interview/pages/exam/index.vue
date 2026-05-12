<template>
  <view class="exam-list">
    <view class="head-bar"><text class="h-title">在线试卷</text></view>

    <view v-if="exams.length" class="list">
      <view class="card" v-for="e in exams" :key="e.id" @click="startExam(e)">
        <view class="card-left">
          <text class="card-title">{{ e.title }}</text>
          <text class="card-desc" v-if="e.description">{{ e.description }}</text>
          <view class="card-meta">
            <text class="meta-tag">⏱ {{ e.duration }}分钟</text>
            <text class="meta-tag">📊 {{ e.totalScore }}分</text>
          </view>
        </view>
        <text class="card-arrow">开始 ›</text>
      </view>
    </view>

    <view v-else class="empty">
      <text class="empty-icon">📝</text>
      <text class="empty-title">暂无试卷</text>
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
.exam-list { min-height: 100vh; background: #f0f4ff; }
.head-bar { padding: 40rpx 30rpx 20rpx; }
.h-title { font-size: 36rpx; font-weight: 800; color: #0f172a; }
.list { padding: 0 24rpx; }
.card { display: flex; align-items: center; background: #fff; margin-bottom: 16rpx; padding: 30rpx; border-radius: 20rpx; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); }
.card:active { background: #f8fafc; }
.card-left { flex: 1; }
.card-title { font-size: 30rpx; font-weight: 700; color: #0f172a; }
.card-desc { font-size: 24rpx; color: #94a3b8; margin-top: 6rpx; display: block; }
.card-meta { display: flex; gap: 24rpx; margin-top: 12rpx; }
.meta-tag { font-size: 22rpx; color: #64748b; }
.card-arrow { font-size: 30rpx; font-weight: 700; color: #2b6ff2; }
.empty { display: flex; flex-direction: column; align-items: center; padding-top: 220rpx; }
.empty-icon { font-size: 100rpx; }
.empty-title { font-size: 28rpx; color: #94a3b8; margin-top: 20rpx; }
</style>
