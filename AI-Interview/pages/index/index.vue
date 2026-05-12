<template>
  <view class="home">
    <DeepStatusBar />
    <!-- Hero -->
    <view class="hero">
      <view class="hero-glow" />
      <text class="hero-badge">AI POWERED</text>
      <text class="hero-title">面面通</text>
      <text class="hero-sub">AI模拟面试 · 智能刷题</text>
      <text class="hero-desc">专为计算机学生打造的面试助手</text>
    </view>

    <!-- 快捷入口 -->
    <view class="actions">
      <view class="action interview" @click="goInterview">
        <view class="action-icon-wrap">
          <text class="action-icon">🤖</text>
        </view>
        <text class="action-title">AI面试</text>
        <text class="action-desc">模拟真实面试</text>
      </view>
      <view class="action exam" @click="goExam">
        <view class="action-icon-wrap">
          <text class="action-icon">📝</text>
        </view>
        <text class="action-title">在线试卷</text>
        <text class="action-desc">限时模拟考</text>
      </view>
      <view class="action practice" @click="goPractice">
        <view class="action-icon-wrap">
          <text class="action-icon">🎯</text>
        </view>
        <text class="action-title">自由刷题</text>
        <text class="action-desc">随机10题</text>
      </view>
      <view class="action wrong" @click="goWrongBook">
        <view class="action-icon-wrap">
          <text class="action-icon">📊</text>
        </view>
        <text class="action-title">错题本</text>
        <text class="action-desc">查漏补缺</text>
      </view>
      <view class="action resume-opt" @click="goResume">
        <view class="action-icon-wrap">
          <text class="action-icon">📋</text>
        </view>
        <text class="action-title">简历优化</text>
        <text class="action-desc">AI智能优化</text>
      </view>
    </view>

    <!-- 分类 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">题目分类</text>
        <text class="section-more">共8个分类</text>
      </view>
      <view class="categories">
        <view class="cat-item" v-for="cat in categories" :key="cat.id" @click="goCategory(cat)">
          <text class="cat-emoji">{{ getIcon(cat.name) }}</text>
          <text class="cat-name">{{ cat.name }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';

interface Category { id: number; name: string; icon: string; sortOrder: number; }
const categories = ref<Category[]>([]);

onMounted(async () => {
  try { const res = await get<Category[]>('/api/questions/categories'); categories.value = res.data; } catch {}
});

const icons: Record<string, string> = { '计算机网络':'🌐','操作系统':'💻','数据结构':'🌳','算法':'⚡','数据库':'🗄️','Java':'☕','设计模式':'📐','计算机组成原理':'🔧' };
function getIcon(n: string) { return icons[n] || '📚'; }
function goCategory(c: Category) { uni.navigateTo({ url: `/pages/question/list?categoryId=${c.id}&categoryName=${encodeURIComponent(c.name)}` }); }
function goInterview() { uni.navigateTo({ url: '/pages/interview/chat' }); }
function goExam() { uni.navigateTo({ url: '/pages/exam/index' }); }
function goPractice() { uni.switchTab({ url: '/pages/practice/practice' }); }
function goWrongBook() { uni.switchTab({ url: '/pages/wrong-book/wrong-book' }); }
function goResume() { uni.navigateTo({ url: '/pages/resume/upload' }); }
</script>

<style lang="scss" scoped>
.home { background: #f0f4ff; min-height: 100vh; padding-bottom: 40rpx; }

.hero {
  position: relative;
  background: linear-gradient(135deg, #1a3a6b 0%, #2b6ff2 50%, #4f8dff 100%);
  padding: 80rpx 40rpx 50rpx;
  overflow: hidden;
}
.hero-glow {
  position: absolute; top: -80rpx; right: -60rpx;
  width: 300rpx; height: 300rpx;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
  border-radius: 50%;
}
.hero-badge {
  display: inline-block;
  background: rgba(255,255,255,0.18);
  color: rgba(255,255,255,0.9);
  font-size: 20rpx; padding: 6rpx 20rpx; border-radius: 20rpx;
  letter-spacing: 4rpx; margin-bottom: 24rpx;
}
.hero-title { display: block; font-size: 60rpx; font-weight: 900; color: #fff; letter-spacing: 6rpx; }
.hero-sub { display: block; font-size: 28rpx; color: rgba(255,255,255,0.85); margin-top: 12rpx; }
.hero-desc { display: block; font-size: 24rpx; color: rgba(255,255,255,0.55); margin-top: 8rpx; }

.actions {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16rpx;
  padding: 30rpx; margin-top: -30rpx;
}
.action {
  background: #fff; border-radius: 20rpx; padding: 32rpx 24rpx;
  box-shadow: 0 4rpx 24rpx rgba(43,111,242,0.08);
  position: relative; overflow: hidden;
  transition: transform 0.15s;
  &:active { transform: scale(0.97); }
}
.action-icon-wrap {
  width: 64rpx; height: 64rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 16rpx;
}
.interview .action-icon-wrap { background: linear-gradient(135deg, #eef2ff, #e0e7ff); }
.exam .action-icon-wrap { background: linear-gradient(135deg, #fef3c7, #fde68a); }
.practice .action-icon-wrap { background: linear-gradient(135deg, #dcfce7, #bbf7d0); }
.wrong .action-icon-wrap { background: linear-gradient(135deg, #fee2e2, #fecaca); }
.resume-opt .action-icon-wrap { background: linear-gradient(135deg, #f0fdf4, #dcfce7); }
.action-icon { font-size: 36rpx; }
.action-title { display: block; font-size: 28rpx; font-weight: 700; color: #0f172a; }
.action-desc { display: block; font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; }

.section { padding: 20rpx 30rpx; }
.section-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 20rpx; }
.section-title { font-size: 32rpx; font-weight: 800; color: #0f172a; }
.section-more { font-size: 24rpx; color: #94a3b8; }

.categories { display: flex; flex-wrap: wrap; gap: 16rpx; }
.cat-item {
  display: flex; flex-direction: column; align-items: center;
  background: #fff; border-radius: 16rpx; padding: 28rpx 0;
  width: calc(25% - 12rpx);
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
  transition: all 0.2s;
  &:active { transform: translateY(-4rpx); box-shadow: 0 8rpx 24rpx rgba(43,111,242,0.12); }
}
.cat-emoji { font-size: 44rpx; margin-bottom: 10rpx; }
.cat-name { font-size: 22rpx; color: #334155; font-weight: 500; }
</style>
