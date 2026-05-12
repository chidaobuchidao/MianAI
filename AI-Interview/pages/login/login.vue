<template>
  <view class="login">
    <view class="login-card">
      <view class="logo-wrap">
        <text class="logo-txt">面</text>
      </view>
      <text class="app-name">面面通</text>
      <text class="app-desc">AI模拟面试 · 智能刷题</text>
      <button class="wx-btn" open-type="getUserInfo" @click="handleLogin">
        <text>微信一键登录</text>
      </button>
      <text class="privacy">登录即同意《用户协议》和《隐私政策》</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user';
const userStore = useUserStore();
async function handleLogin() {
  uni.showLoading({ title: '登录中...' });

  try {
    // 1. 获取微信临时 code
    let code = '';
    try {
      const r = await new Promise<{code:string}>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: (res: any) => resolve(res), fail: reject });
      });
      code = r.code || '';
    } catch {
      // 非微信环境，用 mock code
    }
    if (!code || code.length < 3) {
      code = 'mock_' + Date.now();
    }

    // 2. 用 code 换后端 JWT（等 10 秒）
    await userStore.login(code);
    uni.hideLoading();
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500);
  } catch {
    // 3. 后端不通 → 开发模式直接进去
    uni.hideLoading();
    userStore.devLogin();
    uni.showToast({ title: '开发模式(后端离线)', icon: 'none' });
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500);
  }
}
</script>

<style lang="scss" scoped>
.login { display: flex; align-items: center; justify-content: center; min-height: 100vh; background: linear-gradient(160deg, #1a3a6b 0%, #2b6ff2 40%, #4f8dff 100%); }
.login-card { background: #fff; border-radius: 28rpx; padding: 80rpx 60rpx; width: 80vw; display: flex; flex-direction: column; align-items: center; box-shadow: 0 20rpx 60rpx rgba(0,0,0,0.15); }
.logo-wrap { width: 140rpx; height: 140rpx; border-radius: 36rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); display: flex; align-items: center; justify-content: center; margin-bottom: 30rpx; }
.logo-txt { font-size: 72rpx; font-weight: 900; color: #fff; }
.app-name { font-size: 48rpx; font-weight: 900; color: #0f172a; letter-spacing: 4rpx; }
.app-desc { font-size: 28rpx; color: #94a3b8; margin-top: 12rpx; margin-bottom: 80rpx; }
.wx-btn { width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none; display: flex; align-items: center; justify-content: center; }
.privacy { font-size: 24rpx; color: #cbd5e1; margin-top: 30rpx; }
</style>
