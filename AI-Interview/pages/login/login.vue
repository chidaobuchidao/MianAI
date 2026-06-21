<template>
  <view class="login">
    <view class="login-card" :class="`login-card--${tab}`">
      <view class="brand-row">
        <text class="brand">Mianmian.</text>
      </view>
      <text class="desc">AI 模拟面试平台</text>

      <!-- Tab 切换 -->
      <view class="tabs" v-if="tab !== 'reset'">
        <view class="tab" :class="{ active: tab === 'wechat' }" @click="switchTab('wechat')">
          <text>微信登录</text>
        </view>
        <view class="tab" :class="{ active: tab === 'email' || tab === 'register' }" @click="switchTab('email')">
          <text>邮箱登录</text>
        </view>
      </view>

      <view class="form-head">
        <text class="form-title">
          {{ tab === 'wechat' ? '一键授权' : tab === 'email' ? '欢迎回来' : tab === 'register' ? '创建账号' : '重置密码' }}
        </text>
        <text class="form-sub">
          {{ tab === 'wechat' ? '使用微信快速登录' : tab === 'email' ? '使用邮箱或用户名继续访问' : tab === 'register' ? '邮箱验证后即可开始使用' : '验证邮箱后设置新密码' }}
        </text>
      </view>

      <!-- 微信登录 -->
      <view v-if="tab === 'wechat'" class="form-stage">
        <view class="login-btn" @click="handleWechatLogin">
          <text>微信一键登录</text>
        </view>
        <text class="privacy">登录即同意《用户协议》和《隐私政策》</text>
      </view>

      <!-- 邮箱登录 -->
      <view v-if="tab === 'email'" class="form-stage">
        <view class="field">
          <text class="field-label">账号</text>
          <input
            class="field-input"
            v-model="account"
            placeholder="邮箱或用户名"
            :disabled="loading"
          />
        </view>
        <view class="field">
          <text class="field-label">密码</text>
          <input
            class="field-input"
            v-model="password"
            type="password"
            placeholder="输入登录密码"
            :disabled="loading"
          />
        </view>
        <view class="login-btn" :class="{ disabled: loading || !account.trim() || !password }" @click="handleEmailLogin">
          <text>{{ loading ? '处理中...' : '登录' }}</text>
        </view>
        <view class="extra-links">
          <text @click="switchTab('register')">注册账号</text>
          <text @click="switchTab('reset')">忘记密码？</text>
        </view>
      </view>

      <!-- 邮箱注册 -->
      <view v-if="tab === 'register'" class="form-stage">
        <view class="field">
          <text class="field-label">邮箱</text>
          <input
            class="field-input"
            v-model="email"
            type="email"
            placeholder="请输入邮箱地址"
            :disabled="loading"
          />
        </view>
        <view class="field">
          <text class="field-label">验证码</text>
          <view class="code-row">
            <input
              class="field-input code-input"
              v-model="code"
              placeholder="邮箱验证码"
              :disabled="loading"
              maxlength="6"
            />
            <view
              class="send-code-btn"
              :class="{ disabled: codeCountdown > 0 || loading || !isValidEmail }"
              @click="sendCode"
            >
              <text>{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</text>
            </view>
          </view>
        </view>
        <view class="field">
          <text class="field-label">设置密码</text>
          <input
            class="field-input"
            v-model="password"
            type="password"
            placeholder="6-20位密码"
            :disabled="loading"
          />
        </view>
        <view class="field">
          <text class="field-label">用户名（选填）</text>
          <input
            class="field-input"
            v-model="username"
            placeholder="给自己取个名字"
            :disabled="loading"
          />
        </view>
        <view class="login-btn" :class="{ disabled: loading || !canRegister }" @click="handleRegister">
          <text>{{ loading ? '处理中...' : '注册' }}</text>
        </view>
        <view class="extra-links">
          <text @click="switchTab('email')">已有账号？去登录</text>
        </view>
      </view>

      <!-- 重置密码 -->
      <view v-if="tab === 'reset'" class="form-stage">
        <view class="field">
          <text class="field-label">邮箱</text>
          <input
            class="field-input"
            v-model="email"
            type="email"
            placeholder="请输入注册邮箱"
            :disabled="loading"
          />
        </view>
        <view class="field">
          <text class="field-label">验证码</text>
          <view class="code-row">
            <input
              class="field-input code-input"
              v-model="code"
              placeholder="邮箱验证码"
              :disabled="loading"
              maxlength="6"
            />
            <view
              class="send-code-btn"
              :class="{ disabled: codeCountdown > 0 || loading || !isValidEmail }"
              @click="sendCode"
            >
              <text>{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</text>
            </view>
          </view>
        </view>
        <view class="field">
          <text class="field-label">新密码</text>
          <input
            class="field-input"
            v-model="password"
            type="password"
            placeholder="6-20位新密码"
            :disabled="loading"
          />
        </view>
        <view class="login-btn" :class="{ disabled: loading || !canReset }" @click="handleResetPassword">
          <text>{{ loading ? '处理中...' : '重置密码' }}</text>
        </view>
        <view class="extra-links">
          <text @click="switchTab('email')">返回登录</text>
        </view>
      </view>
    </view>

    <text class="bottom-text">让你的每一场面试都更有把握</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useUserStore } from '@/store/user';
import { post } from '@/utils/request';

const userStore = useUserStore();

type TabType = 'wechat' | 'email' | 'register' | 'reset';
const tab = ref<TabType>('wechat');
const loading = ref(false);

// 表单字段
const account = ref('');
const password = ref('');
const email = ref('');
const code = ref('');
const username = ref('');
const codeCountdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

// 邮箱格式校验
const isValidEmail = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim()));

const canRegister = computed(
  () => isValidEmail.value && code.value.trim().length > 0 && password.value.length >= 6 && password.value.length <= 20,
);

const canReset = computed(
  () => isValidEmail.value && code.value.trim().length > 0 && password.value.length >= 6 && password.value.length <= 20,
);

function switchTab(t: TabType) {
  // 切换时清空表单
  tab.value = t;
  account.value = '';
  password.value = '';
  email.value = '';
  code.value = '';
  username.value = '';
}

function startCountdown() {
  codeCountdown.value = 60;
  if (countdownTimer) clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    codeCountdown.value--;
    if (codeCountdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }, 1000);
}

// ============ 发送验证码 ============
async function sendCode() {
  if (!isValidEmail.value || loading.value) return;
  loading.value = true;
  try {
    await post('/api/auth/send-code', {
      email: email.value.trim(),
      type: tab.value === 'register' ? 'register' : 'reset',
    });
    startCountdown();
    uni.showToast({ title: '验证码已发送', icon: 'success' });
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '发送失败';
    uni.showToast({ title: msg, icon: 'error' });
  } finally {
    loading.value = false;
  }
}

// ============ 微信登录 ============
async function handleWechatLogin() {
  uni.showLoading({ title: '登录中...' });

  try {
    let code = '';
    try {
      const r = await new Promise<{ code: string }>((resolve, reject) => {
        uni.login({ provider: 'weixin', success: (res) => resolve(res as { code: string }), fail: reject });
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

// ============ 邮箱登录 ============
async function handleEmailLogin() {
  const trimmedAccount = account.value.trim();
  if (!trimmedAccount || !password.value) {
    uni.showToast({ title: '请输入账号和密码', icon: 'none' });
    return;
  }
  loading.value = true;
  try {
    const isEmailLogin = trimmedAccount.includes('@');
    const res = await post<{ token: string; userId: number; nickname: string; avatarUrl: string }>(
      isEmailLogin ? '/api/auth/login/email' : '/api/auth/login/password',
      isEmailLogin
        ? { email: trimmedAccount, password: password.value }
        : { username: trimmedAccount, password: password.value },
    );
    userStore.setUser(res.data);
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败';
    uni.showToast({ title: msg, icon: 'error' });
  } finally {
    loading.value = false;
  }
}

// ============ 邮箱注册 ============
async function handleRegister() {
  if (!canRegister.value) return;
  loading.value = true;
  try {
    const res = await post<{ token: string; userId: number; nickname: string; avatarUrl: string }>(
      '/api/auth/register/email',
      {
        email: email.value.trim(),
        code: code.value.trim(),
        password: password.value,
        nickname: username.value.trim() || undefined,
      },
    );
    userStore.setUser(res.data);
    uni.showToast({ title: '注册成功', icon: 'success' });
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '注册失败';
    uni.showToast({ title: msg, icon: 'error' });
  } finally {
    loading.value = false;
  }
}

// ============ 重置密码 ============
async function handleResetPassword() {
  if (!canReset.value) return;
  loading.value = true;
  try {
    await post('/api/auth/reset-password', {
      email: email.value.trim(),
      code: code.value.trim(),
      newPassword: password.value,
    });
    uni.showToast({ title: '密码重置成功', icon: 'success' });
    switchTab('email');
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '重置失败';
    uni.showToast({ title: msg, icon: 'error' });
  } finally {
    loading.value = false;
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
  border-radius: $radius-xl; padding: 60rpx 48rpx;
  width: 100%; max-width: 640rpx;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: $shadow-md;
}

.brand-row { margin-bottom: 16rpx; }
.brand {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 56rpx; font-weight: 600; color: $text-main;
  letter-spacing: -1.5px;
}

.desc {
  font-size: 28rpx; color: $text-muted; font-weight: 500;
  margin-bottom: 36rpx; text-align: center;
}

// ===== Tab =====
.tabs {
  display: flex; width: 100%; background: $bg-surface;
  border-radius: $radius-sm; overflow: hidden; margin-bottom: 36rpx;
}
.tab {
  flex: 1; padding: 20rpx 0; text-align: center;
  font-size: 26rpx; color: $text-light; font-weight: 500;
}
.tab.active { background: $bg-dark; color: #fff; }

// ===== 表单头部 =====
.form-head { text-align: center; margin-bottom: 36rpx; }
.form-title {
  font-size: 32rpx; font-weight: 600; color: $text-main;
  display: block; margin-bottom: 8rpx;
}
.form-sub {
  font-size: 24rpx; color: $text-light; display: block; line-height: 1.6;
}

// ===== 表单 =====
.form-stage { width: 100%; }

.field { margin-bottom: 24rpx; }
.field-label {
  font-size: 24rpx; font-weight: 600; color: $text-main;
  display: block; margin-bottom: 10rpx;
}
.field-input {
  width: 100%; height: 80rpx; background: $bg-surface;
  border: 1px solid $border-medium; border-radius: $radius-md;
  padding: 0 24rpx; font-size: 28rpx; color: $text-main;
  box-sizing: border-box;
}

.code-row { display: flex; gap: 16rpx; }
.code-input { flex: 1; }
.send-code-btn {
  flex-shrink: 0; height: 80rpx; padding: 0 24rpx;
  background: $bg-dark; color: #fff; font-size: 24rpx; font-weight: 600;
  border-radius: $radius-md; display: flex; align-items: center; justify-content: center;
}
.send-code-btn.disabled { opacity: 0.4; }

.login-btn {
  width: 100%; height: 96rpx;
  background: $bg-dark; color: #fff;
  font-size: 30rpx; font-weight: 600;
  border-radius: $radius-lg; border: none;
  display: flex; align-items: center; justify-content: center;
  margin-top: 16rpx;
}
.login-btn:active { opacity: 0.9; }
.login-btn.disabled { opacity: 0.4; }

.extra-links {
  display: flex; justify-content: center; gap: 48rpx;
  margin-top: 28rpx;
}
.extra-links text {
  font-size: 24rpx; color: $accent; font-weight: 500;
}

.privacy {
  font-size: 22rpx; color: $text-light;
  margin-top: 28rpx; text-align: center;
}

.bottom-text {
  font-size: 24rpx; color: $text-light;
  margin-top: 48rpx; text-align: center;
}
</style>
