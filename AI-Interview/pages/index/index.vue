<template>
  <view class="home">
    <!-- PC 端 Canvas 粒子背景 -->
    <ParticleBg />

    <!-- 小程序 + H5 通用 CSS 纹理层 -->
    <view class="bg-pattern" />

    <!-- Header -->
    <view class="header">
      <text class="brand">Mianmian.</text>
      <view class="header-avatar" @click="goProfile">
        <image v-if="userStore.avatarUrl" class="avatar-img" :src="userStore.avatarUrl" mode="aspectFill" />
        <text v-else class="avatar-letter">{{ (userStore.nickname || '?')[0] }}</text>
      </view>
    </view>

    <!-- Hero -->
    <view class="hero">
      <view class="hero-accent-line" />
      <text class="hero-label">AI 面试平台</text>
      <text class="hero-title">为真实环境
做好准备。</text>
      <text class="hero-sub">与硅谷标准对齐的 AI 技术面试，全流程语音对话与代码考察。</text>

      <!-- Dark Card -->
      <view class="hero-card" @click="goInterview">
        <image
          class="hero-card-bg"
          src="https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=800&q=80"
          mode="aspectFill"
          @error="onHeroImgError"
        />
        <view v-if="heroImgFailed" class="hero-card-fallback" />
        <view class="hero-card-gradient" />
        <view class="hero-card-inner">
          <view class="hero-card-icon-box">
            <uni-icons type="compose" size="22" color="#fff" />
          </view>
          <view class="hero-card-spacer" />
          <view class="hero-card-row">
            <view>
              <text class="hero-card-title">开始 AI 面试</text>
              <text class="hero-card-desc">语音流 · 代码编辑器 · 深度追问</text>
            </view>
            <view class="hero-card-circle">
              <uni-icons type="arrow-right" size="18" color="#141413" />
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能入口 — 两列布局带色彩点缀 -->
    <view class="func-grid">
      <view class="func-card func-card-accent" @click="goResume">
        <view class="func-card-top">
          <view class="func-icon-box icon-amber">
            <uni-icons type="paperclip" size="20" color="#D9750A" />
          </view>
          <text class="func-card-tag">推荐</text>
        </view>
        <text class="func-card-title">简历深度诊断</text>
        <text class="func-card-desc">上传 PDF，AI 定位项目薄弱点</text>
      </view>

      <view class="func-card" @click="goExam">
        <view class="func-card-top">
          <view class="func-icon-box">
            <uni-icons type="calendar" size="20" color="#4A4A4A" />
          </view>
        </view>
        <text class="func-card-title">在线试卷</text>
        <text class="func-card-desc">限时模拟考试，查漏补缺</text>
      </view>

      <view class="func-card" @click="goPractice">
        <view class="func-card-top">
          <view class="func-icon-box">
            <uni-icons type="fire" size="20" color="#4A4A4A" />
          </view>
        </view>
        <text class="func-card-title">自由刷题</text>
        <text class="func-card-desc">按分类随机练习</text>
      </view>

      <view class="func-card" @click="goWrongBook">
        <view class="func-card-top">
          <view class="func-icon-box">
            <uni-icons type="bars" size="20" color="#4A4A4A" />
          </view>
        </view>
        <text class="func-card-title">错题本</text>
        <text class="func-card-desc">记录薄弱环节，反复巩固</text>
      </view>
    </view>

    <!-- Hot Topics -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-line" />
        <text class="section-label">Hot Topics</text>
      </view>
      <view class="chip-list">
        <text class="chip chip-active">Redis 穿透</text>
        <text class="chip">MySQL 索引优化</text>
        <text class="chip">ConcurrentHashMap</text>
        <text class="chip">AQS 源码分析</text>
        <text class="chip">TCP 拥塞控制</text>
      </view>
    </view>

    <!-- 题目分类 -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-line" />
        <text class="section-label">题目分类</text>
      </view>
      <view class="chip-list">
        <text class="chip" v-for="cat in categories" :key="cat.id" @click="goCategory(cat)">{{ cat.name }}</text>
      </view>
    </view>

    <view class="bottom-safe" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';
import { useUserStore } from '@/store/user';
import ParticleBg from '@/components/ParticleBg.vue';

const userStore = useUserStore();
const heroImgFailed = ref(false);
function onHeroImgError() { heroImgFailed.value = true; }

interface Category { id: number; name: string; icon: string; sortOrder: number; }
const categories = ref<Category[]>([]);

onMounted(async () => {
  try { const res = await get<Category[]>('/api/questions/categories'); categories.value = res.data || []; } catch {}
});

function goCategory(c: Category) {
  if (!c) return;
  uni.navigateTo({ url: `/pages/question/list?categoryId=${c.id}&categoryName=${encodeURIComponent(c.name)}` });
}
function goInterview() { uni.navigateTo({ url: '/pages/interview/chat' }); }
function goExam() { uni.navigateTo({ url: '/pages/exam/index' }); }
function goPractice() { uni.switchTab({ url: '/pages/practice/practice' }); }
function goWrongBook() { uni.switchTab({ url: '/pages/wrong-book/wrong-book' }); }
function goResume() { uni.navigateTo({ url: '/pages/resume/upload' }); }
function goProfile() { uni.switchTab({ url: '/pages/profile/profile' }); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

// ===== 基础 =====
.home {
  background: $bg-canvas;
  min-height: 100vh;
  position: relative;
}

// ===== CSS 纹理层（全平台） =====
.bg-pattern {
  position: fixed; inset: 0; pointer-events: none; z-index: 0;

  // 圆点图案 — 用径向渐变模拟
  background-image:
    radial-gradient(circle at 15% 20%, rgba(217,117,10,0.05) 0%, transparent 45%),
    radial-gradient(circle at 85% 75%, rgba(20,20,19,0.04) 0%, transparent 40%),
    radial-gradient(circle at 50% 50%, rgba(217,117,10,0.03) 0%, transparent 60%);
}

// ===== Header =====
.header {
  position: relative; z-index: 2;
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 32rpx 20rpx;
}
.brand {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 42rpx; font-weight: 600;
  letter-spacing: -1px; color: $text-main;
}
.header-avatar {
  width: 68rpx; height: 68rpx; border-radius: 50%;
  border: 1.5px solid $border-medium;
  display: flex; align-items: center; justify-content: center;
  background: $bg-paper; overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-letter {
  font-family: Georgia, serif;
  font-size: 30rpx; font-weight: 600; color: $text-main;
}

// ===== Hero =====
.hero {
  position: relative; z-index: 2;
  padding: 16rpx 32rpx 36rpx;
}
.hero-accent-line {
  width: 48rpx; height: 6rpx;
  background: $accent; border-radius: 3rpx; margin-bottom: 20rpx;
}
.hero-label {
  font-size: 22rpx; font-weight: 600; color: $accent;
  letter-spacing: 4rpx; text-transform: uppercase;
  display: block; margin-bottom: 14rpx;
}
.hero-title {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 58rpx; line-height: 1.18; color: $text-main;
  letter-spacing: -1.5px; margin-bottom: 18rpx;
  white-space: pre-line; display: block;
}
.hero-sub {
  font-size: 26rpx; color: $text-muted; line-height: 1.7;
  margin-bottom: 36rpx; display: block; max-width: 520rpx;
}

// ===== Dark Hero Card =====
.hero-card {
  position: relative; border-radius: 20px; overflow: hidden;
  box-shadow:
    0 8px 32px rgba(20,20,19,0.12),
    0 2px 8px rgba(20,20,19,0.06);
  height: 340rpx;
}
.hero-card-bg {
  position: absolute; inset: 0; width: 100%; height: 100%;
  opacity: 0.35; object-fit: cover;
}
.hero-card-fallback {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 50%, #1a1a1a 100%);
}
.hero-card-gradient {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(20,20,19,0.15) 0%, rgba(20,20,19,0.93) 100%);
}
.hero-card-inner {
  position: relative; z-index: 1; height: 100%;
  padding: 36rpx; display: flex; flex-direction: column;
}
.hero-card-icon-box {
  width: 72rpx; height: 72rpx;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.15);
  border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
}
.hero-card-spacer { flex: 1; }
.hero-card-row {
  display: flex; justify-content: space-between; align-items: flex-end;
}
.hero-card-title {
  font-size: 38rpx; font-weight: 500; color: #fff;
  display: block; margin-bottom: 8rpx; letter-spacing: -0.5px;
}
.hero-card-desc {
  font-size: 24rpx; color: rgba(255,255,255,0.45); display: block;
}
.hero-card-circle {
  width: 58rpx; height: 58rpx; background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

// ===== 功能卡片网格 =====
.func-grid {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx;
  padding: 0 32rpx; margin-bottom: 44rpx;
}
.func-card {
  background: $bg-paper;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 28rpx 24rpx 26rpx;
  box-shadow: $shadow-sm;
  display: flex; flex-direction: column;
  transition: all 0.15s;
  position: relative; overflow: hidden;
}
// 第一个卡片加琥珀色顶部细线
.func-card-accent::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4rpx;
  background: $accent; border-radius: 0 0 2rpx 2rpx;
}
.func-card:active { background: $bg-surface; border-color: $border-medium; }
.func-card-top {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 18rpx;
}
.func-icon-box {
  width: 60rpx; height: 60rpx;
  background: $bg-surface; border-radius: 14rpx;
  display: flex; align-items: center; justify-content: center;
}
// 推荐卡片的图标用琥珀底色
.icon-amber { background: rgba(217,117,10,0.08); }
.func-card-tag {
  font-size: 20rpx; font-weight: 600; color: $accent;
  background: rgba(217,117,10,0.08);
  padding: 4rpx 14rpx; border-radius: $radius-full;
}
.func-card-title {
  font-size: 28rpx; font-weight: 500; color: $text-main;
  margin-bottom: 6rpx;
}
.func-card-desc {
  font-size: 22rpx; color: $text-light; line-height: 1.5;
}

// ===== 标签区 =====
.section {
  position: relative; z-index: 2;
  padding: 0 32rpx; margin-bottom: 36rpx;
}
.section-head {
  display: flex; align-items: center; gap: 14rpx; margin-bottom: 18rpx;
}
.section-head-line {
  width: 6rpx; height: 24rpx; background: $text-main; border-radius: 3rpx;
}
.section-label {
  font-size: 24rpx; font-weight: 600; color: $text-main;
  letter-spacing: 0.5rpx;
}
.chip-list { display: flex; flex-wrap: wrap; gap: 12rpx; }
.chip {
  border: 1px solid $border-light;
  padding: 12rpx 26rpx; border-radius: $radius-full;
  font-size: 26rpx; color: $text-main; background: $bg-paper;
  transition: all 0.15s;
}
.chip-active {
  background: $bg-dark; color: #fff; border-color: $bg-dark;
}
.chip:active { background: $bg-surface; border-color: $text-main; }

.bottom-safe { height: 80rpx; }

// ===== PC 端 =====
@media (min-width: 1025px) {
  .home { display: flex; flex-direction: column; align-items: center; }
  .header, .hero, .func-grid, .section {
    width: 100%; max-width: 820px; padding-left: 0; padding-right: 0;
  }
  .hero-card { height: 380rpx; }
  .hero-title { font-size: 64rpx; }
  .func-grid { grid-template-columns: 1fr 1fr; gap: 20rpx; }
}
</style>
