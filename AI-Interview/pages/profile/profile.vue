<template>
  <view class="profile">
    <!-- 头部 -->
    <view class="head">
      <view class="avatar" v-if="!userStore.avatarUrl">
        <text class="avatar-txt">{{ (userStore.nickname || '?')[0] }}</text>
      </view>
      <image class="avatar" v-else :src="userStore.avatarUrl" mode="aspectFill" />
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
          <text class="mi-icon">📋</text>
          <text class="mi-text">面试历史</text>
        </view>
        <text class="mi-arrow">›</text>
      </view>
      <view class="menu-item" @click="goExam">
        <view class="mi-left">
          <text class="mi-icon">📝</text>
          <text class="mi-text">在线试卷</text>
        </view>
        <text class="mi-arrow">›</text>
      </view>
      <view class="menu-item" @click="goWrongBook">
        <view class="mi-left">
          <text class="mi-icon">📊</text>
          <text class="mi-text">错题本</text>
        </view>
        <text class="mi-arrow">›</text>
      </view>
    </view>

    <!-- AI API Key -->
    <view class="menu" style="margin-top: 24rpx;">
      <view class="menu-item" @click="showAiKeyModal = true">
        <view class="mi-left">
          <text class="mi-icon">🔑</text>
          <text class="mi-text">AI API Key</text>
        </view>
        <view class="mi-right">
          <text class="mi-hint" v-if="!aiKeyConfigured">未配置</text>
          <text class="mi-hint configured" v-else>{{ aiProvider }}</text>
          <text class="mi-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="menu" style="margin-top: 24rpx;">
      <view class="menu-item" @click="handleLogout">
        <view class="mi-left">
          <text class="mi-icon">🚪</text>
          <text class="mi-text" style="color:#ef4444;">退出登录</text>
        </view>
        <text class="mi-arrow">›</text>
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

// AI Key 配置
const showAiKeyModal = ref(false);
const inputApiKey = ref('');
const aiProvider = ref('deepseek');
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
    const res = await get<{provider:string;apiKey:string}>('/api/user/ai-config');
    if (res.data && res.data.apiKey) {
      aiProvider.value = res.data.provider;
      inputApiKey.value = res.data.apiKey;
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
    await put('/api/user/ai-config', { apiKey: inputApiKey.value.trim(), provider: aiProvider.value });
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

// 每次切换到"我的"tab时刷新数据
onShow(() => {
  if (userStore.isLogin) {
    loadStats();
    loadAiConfig();
  }
});

function goInterviewHistory() { uni.navigateTo({ url: '/pages/interview/history' }); }
function goExam() { uni.navigateTo({ url: '/pages/exam/index' }); }
function goWrongBook() { uni.switchTab({ url: '/pages/wrong-book/wrong-book' }); }
function handleLogout() {
  uni.showModal({ title:'提示', content:'确定要退出吗？', success: r => { if (r.confirm) userStore.logout(); } });
}
</script>

<style lang="scss" scoped>
.profile { min-height: 100vh; background: #f0f4ff; }
.head {
  display: flex; flex-direction: column; align-items: center;
  padding: 60rpx 0 40rpx;
  background: linear-gradient(135deg, #1a3a6b 0%, #2b6ff2 50%, #4f8dff 100%);
  position: relative; overflow: hidden;
}
.head::after {
  content: ''; position: absolute; top: -60rpx; left: 50%;
  width: 400rpx; height: 400rpx;
  background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
  border-radius: 50%; transform: translateX(-50%);
}
.avatar { width: 120rpx; height: 120rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,0.4); background: rgba(255,255,255,0.2); position: relative; z-index: 1; display: flex; align-items: center; justify-content: center; }
.avatar-txt { font-size: 56rpx; font-weight: 800; color: #fff; }
.name { font-size: 36rpx; font-weight: 700; color: #fff; margin-top: 16rpx; position: relative; z-index: 1; }
.uid { font-size: 24rpx; color: rgba(255,255,255,0.6); margin-top: 6rpx; position: relative; z-index: 1; }
.data-row { display: flex; gap: 16rpx; padding: 30rpx; margin-top: -20rpx; position: relative; z-index: 1; }
.data-card {
  flex: 1; background: #fff; border-radius: 20rpx; padding: 28rpx 0;
  display: flex; flex-direction: column; align-items: center;
  box-shadow: 0 4rpx 24rpx rgba(43,111,242,0.08);
}
.data-num { font-size: 44rpx; font-weight: 900; color: #2b6ff2; }
.data-lbl { font-size: 24rpx; color: #94a3b8; margin-top: 6rpx; }
.menu { background: #fff; margin: 0 30rpx; border-radius: 20rpx; overflow: hidden; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03); }
.menu-item { display: flex; align-items: center; justify-content: space-between; padding: 36rpx 28rpx; }
.menu-item + .menu-item { border-top: 1rpx solid #f1f5f9; }
.menu-item:active { background: #f8fafc; }
.mi-left { display: flex; align-items: center; gap: 20rpx; }
.mi-icon { font-size: 36rpx; }
.mi-text { font-size: 28rpx; font-weight: 500; color: #1e293b; }
.mi-right { display: flex; align-items: center; gap: 12rpx; }
.mi-arrow { font-size: 36rpx; color: #cbd5e1; }
.mi-hint { font-size: 24rpx; color: #94a3b8; }
.mi-hint.configured { color: #10b981; font-weight: 600; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 999; display: flex; align-items: center; justify-content: center; padding: 40rpx; }
.modal-card { background: #fff; border-radius: 24rpx; padding: 40rpx 36rpx; width: 100%; max-width: 600rpx; }
.modal-title { font-size: 32rpx; font-weight: 800; color: #0f172a; display: block; }
.modal-desc { font-size: 24rpx; color: #94a3b8; display: block; margin-top: 12rpx; line-height: 1.8; }
.form-item { margin-top: 28rpx; }
.form-label { font-size: 26rpx; font-weight: 600; color: #334155; display: block; margin-bottom: 10rpx; }
.form-input { border: 2rpx solid #e2e8f0; border-radius: 12rpx; padding: 20rpx 24rpx; font-size: 26rpx; background: #f8fafc; width: 100%; box-sizing: border-box; }
.form-picker { border: 2rpx solid #e2e8f0; border-radius: 12rpx; padding: 20rpx 24rpx; font-size: 26rpx; background: #f8fafc; color: #334155; }
.modal-btns { display: flex; gap: 16rpx; margin-top: 32rpx; }
.mbtn { flex: 1; height: 80rpx; border: none; border-radius: 40rpx; font-size: 28rpx; font-weight: 700; }
.mbtn.cancel { background: #f1f5f9; color: #64748b; }
.mbtn.save { background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; }
</style>
