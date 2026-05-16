<template>
  <view class="history">
    <view class="page-header">
      <text class="page-title">简历记录</text>
    </view>

    <view v-if="list && list.length > 0" class="list">
      <view class="history-item" v-for="item in list" :key="item.id" @click="goDetail(item)">
        <view class="history-item-top">
          <text class="history-title">{{ item.position || '未命名岗位' }}</text>
          <view class="history-status" :class="statusClass(item.parseStatus)">
            <text>{{ statusLabel(item.parseStatus) }}</text>
          </view>
        </view>
        <text class="history-time">{{ formatTime(item.createTime) }}</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📄</text>
      <text class="empty-title">暂无简历记录</text>
      <text class="empty-desc">上传简历后，AI 会为你生成深度诊断报告</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';

interface ResumeItem { id: number; position: string; parseStatus: number; createTime: string; }
const list = ref<ResumeItem[]>([]);

onMounted(async () => {
  try { const r = await get<ResumeItem[]>('/api/resume/list'); if (r.data) list.value = r.data; } catch (_) {}
});

function formatTime(t: string) {
  if (!t) return '';
  return t.replace('T', ' ').substring(0, 16);
}

function statusClass(s: number) {
  if (s === 1) return 'status-done';
  if (s === -1) return 'status-fail';
  return 'status-pending';
}

function statusLabel(s: number) {
  if (s === 1) return '解析完成';
  if (s === -1) return '解析失败';
  return '解析中';
}

function goDetail(item: ResumeItem) {
  uni.navigateTo({ url: `/pages/resume/report?resumeId=${item.id}` });
}
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

.history-item {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx; box-shadow: $shadow-sm;
}
.history-item:active { background: $bg-surface; }
.history-item-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10rpx; }
.history-title { font-size: 28rpx; font-weight: 500; color: $text-main; }
.history-status {
  font-size: 20rpx; font-weight: 500; padding: 6rpx 14rpx; border-radius: $radius-full;
}
.status-done { background: rgba(34,197,94,0.08); color: $color-success; }
.status-fail { background: rgba(239,68,68,0.08); color: $color-danger; }
.status-pending { background: rgba(217,117,10,0.08); color: $accent; }
.history-time { font-size: 22rpx; color: $text-light; }

.empty-state { text-align: center; padding-top: 200rpx; }
.empty-icon { font-size: 96rpx; display: block; margin-bottom: 24rpx; }
.empty-title { font-size: 28rpx; color: $text-main; font-weight: 500; display: block; margin-bottom: 10rpx; }
.empty-desc { font-size: 24rpx; color: $text-light; display: block; }

@media (min-width: 1025px) { .history { max-width: 700px; margin: 0 auto; } }
</style>
