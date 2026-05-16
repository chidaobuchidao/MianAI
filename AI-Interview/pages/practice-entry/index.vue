<template>
  <view class="practice-entry">
    <view class="page-header">
      <text class="page-title">自由刷题</text>
      <text class="page-desc">随机组卷或按专题专项突破</text>
    </view>

    <!-- Entry 1: Random -->
    <view class="entry-card">
      <view class="entry-icon">
        <text class="entry-icon-text">🎲</text>
      </view>
      <text class="entry-title">随机组卷</text>
      <text class="entry-desc">从题库随机抽取，模拟真实考试</text>
      <view class="count-row">
        <view v-for="n in [5,10,15,20]" :key="n" class="count-chip" :class="{ active: randomCount === n }" @click="randomCount = n">
          <text>{{ n }}</text>
        </view>
        <view class="count-go" @click="startRandom"><text>开始</text></view>
      </view>
    </view>

    <!-- Entry 2: Topic -->
    <view class="entry-card">
      <view class="entry-icon">
        <text class="entry-icon-text">📂</text>
      </view>
      <text class="entry-title">按专题刷题</text>
      <text class="entry-desc">选择分类，集中突破薄弱环节</text>
      <scroll-view scroll-x class="chip-scroll">
        <view class="chip-row">
          <view
            v-for="cat in categories"
            :key="cat.id"
            class="chip"
            :class="{ active: selectedCategory === cat.id }"
            @click="selectedCategory = cat.id; startTopic(cat)"
          >{{ cat.name }}</view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';

interface Category { id: number; name: string }

const randomCount = ref(10);
const selectedCategory = ref<number | null>(null);
const categories = ref<Category[]>([]);

function startRandom() {
  uni.navigateTo({ url: `/pages/practice-entry/do?mode=random&count=${randomCount.value}` });
}

function startTopic(cat: Category) {
  uni.navigateTo({ url: `/pages/practice-entry/do?mode=topic&categoryId=${cat.id}&categoryName=${encodeURIComponent(cat.name)}` });
}

onMounted(async () => {
  try {
    const r = await get<Category[]>('/api/questions/categories');
    categories.value = r.data || [];
  } catch {}
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.practice-entry { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

.page-header { margin-bottom: 36rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; display: block; margin-bottom: 10rpx; letter-spacing: -0.5px;
}
.page-desc { font-size: 26rpx; color: $text-muted; }

.entry-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-xl; padding: 36rpx 28rpx;
  box-shadow: $shadow-sm; margin-bottom: 20rpx;
}
.entry-icon {
  width: 80rpx; height: 80rpx; border-radius: 20rpx;
  background: rgba(217,117,10,0.06);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 20rpx;
}
.entry-icon-text { font-size: 36rpx; }
.entry-title { font-size: 32rpx; font-weight: 500; color: $text-main; display: block; margin-bottom: 8rpx; }
.entry-desc { font-size: 24rpx; color: $text-light; display: block; margin-bottom: 24rpx; }

.count-row { display: flex; gap: 12rpx; align-items: center; }
.count-chip {
  width: 72rpx; height: 72rpx; border-radius: 16rpx;
  border: 1px solid $border-light; background: $bg-paper;
  display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 500; color: $text-main;
}
.count-chip.active { border-color: $accent; background: rgba(217,117,10,0.06); color: $accent; font-weight: 600; }
.count-go {
  margin-left: auto; height: 72rpx; padding: 0 40rpx;
  background: $bg-dark; border-radius: 16rpx;
  display: flex; align-items: center; justify-content: center;
}
.count-go text { color: #fff; font-size: 28rpx; font-weight: 500; }

.chip-scroll { white-space: nowrap; }
.chip-row { display: inline-flex; gap: 14rpx; }
.chip {
  display: inline-block; padding: 14rpx 32rpx; border-radius: $radius-full;
  font-size: 26rpx; color: $text-muted; background: $bg-surface;
  border: 1px solid $border-light;
}
.chip.active { background: rgba(217,117,10,0.06); border-color: $accent; color: $accent; font-weight: 600; }

@media (min-width: 1025px) { .practice-entry { max-width: 700px; margin: 0 auto; } }
</style>
