<template>
  <view class="login">
    <view class="login-card">
      <view class="brand-row">
        <text class="brand">Mianmian.</text>
      </view>
      <text class="desc">AI 模拟面试平台</text>
      <text class="sub-desc">专为计算机学生打造 · 智能刷题 · 简历诊断</text>

      <view class="login-btn" @click="handleLogin">
        <text>微信一键登录</text>
      </view>

      <text class="privacy">登录即同意《用户协议》和《隐私政策》</text>
    </view>

    <text class="bottom-text">让你的每一场面试都更有把握</text>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '@/store/user';
const userStore = useUserStore();

async function handleLogin() {
  uni.showLoading({ title: '登录中...' });

  try {
    let code = '';
    try {
      const r = await new Promise<{code:string}>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: (res: any) => resolve(res), fail: reject });
      });
      code = r.code || '';
    } catch {
      // 非微信环境
    }
    if (!code || code.length < 3) {
      code = 'mock_' + Date.now();
    }

    await userStore.login(code);
    uni.hideLoading();
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500);
  } catch {
    uni.hideLoading();
    userStore.devLogin();
    uni.showToast({ title: '开发模式(后端离线)', icon: 'none' });
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500);
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.login {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; min-height: 100vh;
  background: $bg-canvas; padding: 60rpx 40rpx;
}

.login-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-xl; padding: 80rpx 56rpx;
  width: 100%; max-width: 600rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: $shadow-md;
}

.brand-row { margin-bottom: 20rpx; }
.brand {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 60rpx; font-weight: 600; color: $text-main;
  letter-spacing: -1.5px;
}

.desc {
  font-size: 30rpx; color: $text-main; font-weight: 500;
  margin-bottom: 12rpx; text-align: center;
}
.sub-desc {
  font-size: 24rpx; color: $text-light; margin-bottom: 64rpx;
  text-align: center; line-height: 1.6;
}

.login-btn {
  width: 100%; height: 100rpx;
  background: $bg-dark; color: #fff;
  font-size: 30rpx; font-weight: 600;
  border-radius: $radius-lg; border: none;
  display: flex; align-items: center; justify-content: center;
}
.login-btn:active { opacity: 0.9; }

.privacy {
  font-size: 22rpx; color: $text-light;
  margin-top: 32rpx; text-align: center;
}

.bottom-text {
  font-size: 24rpx; color: $text-light;
  margin-top: 60rpx; text-align: center;
}
</style>
