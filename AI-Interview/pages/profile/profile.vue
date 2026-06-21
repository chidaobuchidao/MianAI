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

    <!-- 配额提示 -->
    <view class="quota-banner" v-if="quota && !quota.isAdmin && !quota.hasApiKey">
      <text class="quota-banner-text">剩余免费次数：<text class="quota-banner-num">{{ quota.quotaRemaining }}</text> 次 · <text class="quota-banner-link" @click="showAiKeyModal = true">配置 API Key</text> 后无限</text>
    </view>
    <view class="quota-banner quota-banner-ok" v-else-if="quota && !quota.isAdmin && quota.hasApiKey">
      <text class="quota-banner-text">已配置个人 API Key，不限使用次数</text>
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

    <!-- AI 模型配置 -->
    <view class="menu">
      <view class="menu-item" @click="showAiKeyModal = true">
        <view class="mi-left">
          <text class="mi-text">AI 模型配置</text>
        </view>
        <view class="mi-right">
          <text class="mi-hint" v-if="!aiKeyConfigured">未配置</text>
          <text class="mi-hint configured" v-else>{{ aiProviderName }}</text>
          <text class="mi-arrow">→</text>
        </view>
      </view>
    </view>

    <!-- 管理后台（仅管理员） -->
    <view class="menu" v-if="canAccessAdmin">
      <view class="menu-item" @click="goAdmin">
        <view class="mi-left">
          <text class="mi-text">管理后台</text>
        </view>
        <view class="mi-right">
          <text class="mi-hint configured">Admin</text>
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
        <text class="modal-title">AI 模型配置</text>
        <text class="modal-desc">填入你自己的 API Key，优先使用你的 Key 调用 AI。留空则使用系统默认。</text>
        <view class="form-item">
          <text class="form-label">Provider</text>
          <picker mode="selector" :range="providerNames" :value="selectedProviderIdx" @change="onProviderChange">
            <view class="form-picker">{{ aiProvider }}</view>
          </picker>
        </view>
        <view class="form-item">
          <text class="form-label">默认模型</text>
          <view class="model-opts">
            <view v-for="m in modelOptions" :key="m.id" class="model-opt" :class="{ active: aiModel === m.id }" @click="aiModel = m.id">{{ m.label }}</view>
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
import { computed, ref, onMounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useUserStore } from '@/store/user';
import { get, put } from '@/utils/request';
import { useQuota, type QuotaInfo } from '@/composables/useQuota';
import { fallbackModels, type ModelOption, type ProviderPreset } from '@/composables/useModelToggle';

const userStore = useUserStore();
const { fetchQuota } = useQuota();

interface Stats {
  practiceCount: number;
  interviewCount: number;
  wrongCount: number;
}

interface AiConfig {
  provider: string;
  apiKey: string;
  model?: string;
  preferredModel?: string;
}

const stats = ref<Stats>({ practiceCount: 0, interviewCount: 0, wrongCount: 0 });
const quota = ref<QuotaInfo | null>(null);

const showAiKeyModal = ref(false);
const inputApiKey = ref('');
const aiProvider = ref('deepseek');
const aiModel = ref('deepseek-v4-flash');
const aiKeyConfigured = ref(false);
const providers = ref<ProviderPreset[]>([
  { id: 'deepseek', name: 'DeepSeek' },
  { id: 'qwen', name: '通义千问' },
]);

const providerNames = computed(() => providers.value.map((p) => p.name || p.id));
const selectedProviderIdx = computed(() => Math.max(0, providers.value.findIndex((p) => p.id === aiProvider.value)));
const aiProviderName = computed(() => providers.value.find((p) => p.id === aiProvider.value)?.name || aiProvider.value);
const canAccessAdmin = computed(() => quota.value?.isAdmin === true || userStore.isAdmin);
const modelOptions = computed<ModelOption[]>(() => {
  const preset = providers.value.find((p) => p.id === aiProvider.value);
  if (preset?.models?.length) {
    return preset.models.map((m) => typeof m === 'string' ? { id: m, label: labelModel(m) } : m);
  }
  return fallbackModels(aiProvider.value);
});

function labelModel(id: string): string {
  const parts = id.split('-');
  return parts[parts.length - 1]?.toUpperCase() || id;
}

function withTimeout<T>(promise: Promise<T>, ms: number, message: string): Promise<T> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  return new Promise((resolve, reject) => {
    timer = setTimeout(() => reject(new Error(message)), ms);
    promise.then(
      (value) => {
        if (timer) clearTimeout(timer);
        resolve(value);
      },
      (error) => {
        if (timer) clearTimeout(timer);
        reject(error);
      },
    );
  });
}

async function loadStats() {
  try {
    const res = await get<Stats>('/api/user/stats');
    if (res.data) stats.value = res.data;
  } catch {
    uni.showToast({ title: '加载统计数据失败', icon: 'none' });
  }
}

async function loadProviders() {
  try {
    const res = await get<ProviderPreset[]>('/api/user/ai-providers');
    if (Array.isArray(res.data) && res.data.length) {
      providers.value = res.data;
    }
  } catch {
    // 使用内置 provider 列表
  }
}

async function loadAiConfig() {
  try {
    const res = await get<AiConfig>('/api/user/ai-config');
    if (res.data) {
      aiProvider.value = res.data.provider || 'deepseek';
      inputApiKey.value = res.data.apiKey || '';
      aiKeyConfigured.value = !!res.data.apiKey;
      const preferred = res.data.preferredModel || res.data.model;
      const options = modelOptions.value;
      aiModel.value = preferred && options.some((m) => m.id === preferred)
        ? preferred
        : (options[0]?.id || 'deepseek-v4-flash');
    }
  } catch {
    // 配置加载失败，使用默认值
  }
}

async function saveAiKey() {
  const trimmed = inputApiKey.value.trim();
  if (!trimmed) {
    uni.showToast({ title: '请输入 API Key', icon: 'none' });
    return;
  }
  try {
    await put('/api/user/ai-config', {
      apiKey: trimmed,
      provider: aiProvider.value,
      model: aiModel.value,
      preferredModel: aiModel.value,
    });
    aiKeyConfigured.value = true;
    showAiKeyModal.value = false;
    uni.showToast({ title: '保存成功', icon: 'success' });
    quota.value = await fetchQuota(true);
  } catch {
    uni.showToast({ title: '保存失败，请重试', icon: 'error' });
  }
}

function onProviderChange(e: { detail: { value: number } }) {
  const next = providers.value[e.detail.value];
  if (!next) return;
  aiProvider.value = next.id;
  aiModel.value = (next.models?.[0] || fallbackModels(next.id)[0])?.id || 'deepseek-v4-flash';
}

async function refreshProfile() {
  if (!userStore.isLogin) return;
  await loadProviders();
  const [q] = await Promise.allSettled([
    fetchQuota(),
    loadStats(),
    loadAiConfig(),
  ]);
  if (q.status === 'fulfilled') quota.value = q.value;
}

onMounted(refreshProfile);
onShow(refreshProfile);

function goInterviewHistory() { uni.navigateTo({ url: '/pages/interview/history' }); }
function goExam() { uni.navigateTo({ url: '/pages/practice-entry/index' }); }
function goWrongBook() { uni.switchTab({ url: '/pages/wrong-book/wrong-book' }); }
async function goAdmin() {
  try {
    if (quota.value?.isAdmin === true || userStore.isAdmin) {
      uni.navigateTo({ url: '/pages/admin/index' });
      void withTimeout(fetchQuota(true), 8_000, '管理员权限校验超时')
        .then((nextQuota) => {
          quota.value = nextQuota;
          if (nextQuota.isAdmin !== true) userStore.setAdmin(false);
        })
        .catch((error) => {
          console.warn('[profile] 管理员权限复核失败', error);
        });
      return;
    }

    const q = quota.value || await withTimeout(fetchQuota(true), 8_000, '管理员权限校验超时');
    quota.value = q;
    if (q.isAdmin !== true) {
      userStore.setAdmin(false);
      uni.showToast({ title: "无管理员权限", icon: 'none' });
      return;
    }
    userStore.setAdmin(true);
    uni.navigateTo({ url: '/pages/admin/index' });
  } catch {
    uni.showToast({ title: "权限校验失败，请稍后重试", icon: 'none' });
  }
}
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出吗？',
    success: (r) => { if (r.confirm) userStore.logout(); },
  });
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

// ===== 配额 =====
.quota-banner {
  margin: 0 28rpx 24rpx; padding: 20rpx 24rpx;
  background: rgba(217,117,10,0.06); border: 1px solid rgba(217,117,10,0.15);
  border-radius: $radius-md;
}
.quota-banner-ok {
  background: rgba(34,197,94,0.06); border-color: rgba(34,197,94,0.15);
}
.quota-banner-text { font-size: 24rpx; color: $text-muted; line-height: 1.6; }
.quota-banner-num { font-weight: 600; color: $accent; }
.quota-banner-link { color: $accent; font-weight: 600; }

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
  .data-row, .menu, .quota-banner { width: 100%; max-width: 600px; margin-left: auto; margin-right: auto; }
  .data-row { padding-left: 0; padding-right: 0; }
}
</style>
