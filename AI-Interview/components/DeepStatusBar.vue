<template>
  <view class="dsb-bar" v-if="info" @click="goReport">
    <view class="dsb-spinner" />
    <view class="dsb-text">
      <text class="dsb-title">简历深度优化中...</text>
      <text class="dsb-sub">点击查看 {{ info.name }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { BASE_URL } from '@/utils/request';

interface DeepInfo { resumeId: number; name: string; }
const info = ref<DeepInfo | null>(null);
let checkTimer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  load();
  checkTimer = setInterval(load, 5000);
});

function load() {
  try {
    const raw = uni.getStorageSync('deep_optimizing');
    if (raw) {
      const d = JSON.parse(raw) as DeepInfo;
      // 检查是否已完成
      uni.request({
        url: `${BASE_URL}/api/resume/${d.resumeId}/deep-status`,
        header: { Authorization: 'Bearer ' + (uni.getStorageSync('mianmiantong_token') || '') },
        success: (r: any) => {
          if (r.data?.data?.deepStatus === 1) {
            info.value = d;
          } else {
            uni.removeStorageSync('deep_optimizing');
            info.value = null;
          }
        },
        fail: () => {},
      });
    }
  } catch { info.value = null; }
}

function goReport() {
  if (info.value) {
    uni.navigateTo({ url: `/pages/resume/report?resumeId=${info.value.resumeId}` });
  }
}
</script>

<style lang="scss" scoped>
.dsb-bar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 99;
  display: flex; align-items: center; gap: 14rpx;
  padding: 10rpx 24rpx;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  box-shadow: 0 2rpx 16rpx rgba(99,102,241,0.3);
}
.dsb-spinner {
  width: 36rpx; height: 36rpx;
  border: 3rpx solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.8s linear infinite; flex-shrink: 0;
}
@keyframes spin { to { transform: rotate(360deg); } }
.dsb-text { flex: 1; }
.dsb-title { font-size: 26rpx; color: #fff; font-weight: 600; display: block; }
.dsb-sub { font-size: 20rpx; color: rgba(255,255,255,0.7); display: block; margin-top: 2rpx; }
</style>
