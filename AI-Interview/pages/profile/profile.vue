<template>
  <view class="profile">
    <!-- 头部 -->
    <view class="head">
      <view class="avatar" v-if="!userStore.avatarUrl">
        <text class="avatar-txt">{{ (userStore.nickname || '?')[0] }}</text>
      </view>
      <image class="avatar avatar-img" v-else :src="userStore.avatarUrl" mode="aspectFill" />
      <text class="name">{{ userStore.nickname || '未登录' }}</text>
      <text class="uid">ID: {{ userStore.userId }}</text>
    </view>

    <!-- 数据卡片 -->
    <view class="data-row">
      <view class="data-card">
        <text class="data-num">{{ stats.practiceCount }}</text>
        <text class="data-lbl">刷题数</text>
      </view>
      <view class="data-card">
        <text class="data-num">{{ stats.interviewCount }}</text>
        <text class="data-lbl">面试次数</text>
      </view>
      <view class="data-card">
        <text class="data-num">{{ stats.wrongCount }}</text>
        <text class="data-lbl">错题积累</text>
      </view>
    </view>

    <!-- 菜单 -->
    <view class="menu">
      <view class="menu-item" @click="goInterviewHistory">
        <view class="mi-left">
          <text class="mi-text">面试历史</text>
        </view>
        <text class="mi-arrow">→</text>
      </view>
      <view class="menu-item" @click="goExam">
        <view class="mi-left">
          <text class="mi-text">自由刷题</text>
        </view>
        <text class="mi-arrow">→</text>
      </view>
      <view class="menu-item" @click="goWrongBook">
        <view class="mi-left">
          <text class="mi-text">错题本</text>
        </view>
        <text class="mi-arrow">→</text>
      </view>
    </view>

    <!-- AI API Key -->
    <view class="menu">
      <view class="menu-item" @click="showAiKeyModal = true">
        <view class="mi-left">
          <text class="mi-text">AI API Key</text>
        </view>
        <view class="mi-right">
          <text class="mi-hint" v-if="!aiKeyConfigured">未配置</text>
          <text class="mi-hint configured" v-else>{{ aiProvider }}</text>
          <text class="mi-arrow">→</text>
        </view>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="menu">
      <view class="menu-item" @click="handleLogout">
        <view class="mi-left">
          <text class="mi-text logout-text">退出登录</text>
        </view>
      </view>
    </view>

    <!-- AI Key 弹窗 -->
    <view class="modal-mask" v-if="showAiKeyModal" @click="showAiKeyModal = false">
      <view class="modal-card" @click.stop>
        <text class="modal-title">AI API Key 配置</text>
        <text class="modal-desc">填入你自己的 API Key，优先使用你的 Key 调用 AI。留空则使用系统默认。</text>
        <view class="form-item">
          <text class="form-label">Provider</text>
          <picker mode="selector" :range="providers" :value="providers.indexOf(aiProvider)" @change="onProviderChange">
            <view class="form-picker">{{ aiProvider }}</view>
          </picker>
        </view>
        <view class="form-item">
          <text class="form-label">默认模型</text>
          <view class="model-opts">
            <view class="model-opt" :class="{ active: aiModel === 'deepseek-v4-flash' }"
              @click="aiModel = 'deepseek-v4-flash'">Flash</view>
            <view class="model-opt" :class="{ active: aiModel === 'deepseek-v4-pro' }"
              @click="aiModel = 'deepseek-v4-pro'">Pro</view>
          </view>
        </view>
        <view class="form-item">
          <text class="form-label">API Key</text>
          <input class="form-input" v-model="inputApiKey" placeholder="sk-xxxxxxxx" />
        </view>
        <view class="modal-btns">
          <button class="mbtn cancel" @click="showAiKeyModal = false">取消</button>
          <button class="mbtn save" @click="saveAiKey">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/store/user';
import { get, put } from '@/utils/request';

const userStore = useUserStore();

interface Stats { practiceCount: number; interviewCount: number; wrongCount: number; }
const stats = ref<Stats>({ practiceCount: 0, interviewCount: 0, wrongCount: 0 });

const showAiKeyModal = ref(false);
const inputApiKey = ref('');
const aiProvider = ref('deepseek');
const aiModel = ref('deepseek-v4-flash');
const aiKeyConfigured = ref(false);
const providers = ['deepseek', 'qwen'];

async function loadStats() {
  try {
    const res = await get<Stats>('/api/user/stats');
    stats.value = res.data;
  } catch {}
}

async function loadAiConfig() {
  try {
    const res = await get<{provider:string;apiKey:string;model:string}>('/api/user/ai-config');
    if (res.data && res.data.apiKey) {
      aiProvider.value = res.data.provider;
      inputApiKey.value = res.data.apiKey;
      aiModel.value = res.data.model || 'deepseek-v4-flash';
      aiKeyConfigured.value = true;
    }
  } catch {}
}

async function saveAiKey() {
  if (!inputApiKey.value.trim()) {
    uni.showToast({ title:'请输入API Key', icon:'none' });
    return;
  }
  try {
    await put('/api/user/ai-config', { apiKey: inputApiKey.value.trim(), provider: aiProvider.value, model: aiModel.value });
    aiKeyConfigured.value = true;
    showAiKeyModal.value = false;
    uni.showToast({ title:'保存成功', icon:'success' });
  } catch {
    uni.showToast({ title:'保存失败', icon:'error' });
  }
}

function onProviderChange(e: { detail: { value: number } }) {
  aiProvider.value = providers[e.detail.value];
}

onShow(() => {
  if (userStore.isLogin) {
    loadStats();
    loadAiConfig();
  }
});

function goInterviewHistory() { uni.navigateTo({ url: '/pages/interview/history' }); }
function goExam() { uni.navigateTo({ url: '/pages/practice-entry/index' }); }
function goWrongBook() { uni.switchTab({ url: '/pages/wrong-book/wrong-book' }); }
function handleLogout() {
  uni.showModal({ title:'提示', content:'确定要退出吗？', success: r => { if (r.confirm) userStore.logout(); } });
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.profile { min-height: 100vh; background: $bg-canvas; }

// ===== 头部 =====
.head {
  display: flex; flex-direction: column; align-items: center;
  padding: 80rpx 0 48rpx; background: $bg-paper;
  border-bottom: 1px solid $border-light;
}
.avatar {
  width: 160rpx; height: 160rpx; border-radius: 24px;
  background: $bg-dark; display: flex; align-items: center;
  justify-content: center; margin-bottom: 24rpx;
}
.avatar-img { background: none; border: 2px solid $border-light; }
.avatar-txt {
  font-family: Georgia, serif; font-size: 64rpx;
  font-weight: 600; color: #FDFCFB;
}
.name {
  font-family: Georgia, serif; font-size: 36rpx;
  font-weight: 600; color: $text-main; margin-bottom: 8rpx;
}
.uid { font-size: 24rpx; color: $text-light; }

// ===== 数据卡片 =====
.data-row {
  display: flex; gap: 16rpx; padding: 32rpx 28rpx;
}
.data-card {
  flex: 1; background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 32rpx 0;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: $shadow-sm;
}
.data-num {
  font-family: Georgia, serif; font-size: 48rpx;
  font-weight: 600; color: $text-main;
}
.data-lbl { font-size: 24rpx; color: $text-light; margin-top: 8rpx; }

// ===== 菜单 =====
.menu {
  background: $bg-paper; margin: 0 28rpx 24rpx;
  border: 1px solid $border-light; border-radius: $radius-lg;
  overflow: hidden; box-shadow: $shadow-sm;
}
.menu-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 36rpx 28rpx;
}
.menu-item + .menu-item { border-top: 1px solid $border-light; }
.menu-item:active { background: $bg-surface; }
.mi-left { display: flex; align-items: center; gap: 20rpx; }
.mi-text { font-size: 28rpx; font-weight: 500; color: $text-main; }
.logout-text { color: $color-danger; }
.mi-right { display: flex; align-items: center; gap: 12rpx; }
.mi-arrow { font-size: 28rpx; color: $text-light; }
.mi-hint { font-size: 24rpx; color: $text-light; }
.mi-hint.configured { color: $color-success; font-weight: 600; }

// ===== 弹窗 =====
.modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45);
  z-index: 999; display: flex; align-items: center;
  justify-content: center; padding: 40rpx;
}
.modal-card {
  background: $bg-paper; border-radius: $radius-xl;
  padding: 48rpx 36rpx; width: 100%; max-width: 600rpx;
}
.modal-title {
  font-family: Georgia, serif; font-size: 32rpx;
  font-weight: 600; color: $text-main; display: block; margin-bottom: 12rpx;
}
.modal-desc {
  font-size: 24rpx; color: $text-light; display: block;
  line-height: 1.8; margin-bottom: 32rpx;
}
.form-item { margin-top: 28rpx; }
.form-label { font-size: 26rpx; font-weight: 600; color: $text-main; display: block; margin-bottom: 12rpx; }
.form-input {
  border: 1px solid $border-medium; border-radius: $radius-md;
  padding: 20rpx 24rpx; font-size: 26rpx; background: $bg-surface;
  width: 100%; box-sizing: border-box;
}
.model-opts {
  display: flex; gap: 0; background: $bg-surface;
  border-radius: $radius-sm; overflow: hidden; width: fit-content;
}
.model-opt { font-size: 24rpx; padding: 12rpx 32rpx; color: $text-light; }
.model-opt.active { background: $bg-dark; color: #fff; }
.form-picker {
  border: 1px solid $border-medium; border-radius: $radius-md;
  padding: 20rpx 24rpx; font-size: 26rpx; background: $bg-surface;
  color: $text-main;
}
.modal-btns { display: flex; gap: 16rpx; margin-top: 40rpx; }
.mbtn { flex: 1; height: 80rpx; border: none; border-radius: $radius-xl; font-size: 28rpx; font-weight: 600; }
.mbtn.cancel { background: $bg-surface; color: $text-muted; }
.mbtn.save { background: $bg-dark; color: #fff; }

@media (min-width: 1025px) {
  .profile { display: flex; flex-direction: column; align-items: center; }
  .head { width: 100%; }
  .data-row, .menu { width: 100%; max-width: 600px; margin-left: auto; margin-right: auto; }
  .data-row { padding-left: 0; padding-right: 0; }
}
</style>
