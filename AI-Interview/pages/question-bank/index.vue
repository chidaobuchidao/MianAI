<template>
  <view class="question-bank">
    <view class="page-header">
      <text class="page-title">查看题库</text>
    </view>

    <!-- Category chips -->
    <scroll-view scroll-x class="chip-scroll">
      <view class="chip-row">
        <view class="chip" :class="{ active: !activeCategory }" @click="selectCategory('')">全部</view>
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="chip"
          :class="{ active: activeCategory === cat.id }"
          @click="selectCategory(cat.id)"
        >{{ cat.name }}</view>
      </view>
    </scroll-view>

    <!-- Question list -->
    <view class="q-list" v-if="questions.length > 0">
      <view class="q-card" v-for="(q, i) in questions" :key="q.id" @click="goDetail(q)">
        <text class="q-number">Q{{ i + 1 }}</text>
        <view class="q-content">
          <text class="q-title">{{ q.title }}</text>
          <text class="q-meta">{{ q.categoryName || '未分类' }} · {{ difficultyLabel(q.difficulty) }}</text>
        </view>
        <text class="q-arrow">›</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-title">暂无题目</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get } from '@/utils/request';

interface Category { id: number; name: string }
interface Question { id: number; title: string; categoryName: string; difficulty: number }

const activeCategory = ref<string | number>('');
const categories = ref<Category[]>([]);
const questions = ref<Question[]>([]);

function difficultyLabel(d: number): string {
  return d === 3 ? '困难' : d === 2 ? '中等' : '简单';
}

function selectCategory(id: string | number) {
  activeCategory.value = id;
  fetchQuestions();
}

async function fetchQuestions() {
  try {
    const params: Record<string, unknown> = { size: 50 };
    if (activeCategory.value) params.categoryId = activeCategory.value;
    const res = await get<{ records: Question[] }>('/api/questions', params);
    const data = res.data as Record<string, unknown>;
    questions.value = (data.records || data || []) as Question[];
  } catch {
    // 静默失败，显示空状态
  }
}

function goDetail(q: Question) {
  uni.navigateTo({
    url: `/pages/question-bank/detail?id=${q.id}&categoryId=${activeCategory.value || ''}&categoryName=${encodeURIComponent(q.categoryName || '')}`
  });
}

onMounted(async () => {
  try {
    const r = await get<Category[]>('/api/questions/categories');
    categories.value = (r.data as Category[]) || [];
  } catch {
    // 静默失败
  }
  fetchQuestions();
});
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.question-bank { min-height: 100vh; background: $bg-canvas; padding: 0 28rpx 40rpx; }

.page-header { padding-top: 40rpx; margin-bottom: 24rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; letter-spacing: -0.5px;
}

.chip-scroll { white-space: nowrap; margin-bottom: 28rpx; }
.chip-row { display: inline-flex; gap: 14rpx; }
.chip {
  display: inline-block; padding: 12rpx 28rpx; border-radius: $radius-full;
  font-size: 26rpx; color: $text-muted; background: $bg-surface;
  border: 1px solid $border-light;
}
.chip.active { background: rgba(217,117,10,0.06); border-color: $accent; color: $accent; font-weight: 600; }

.q-list { display: flex; flex-direction: column; gap: 14rpx; }
.q-card {
  display: flex; align-items: flex-start; gap: 16rpx;
  padding: 28rpx 24rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg;
}
.q-card:active { background: $bg-surface; }
.q-number { font-size: 26rpx; font-weight: 600; color: $accent; flex-shrink: 0; }
.q-content { flex: 1; min-width: 0; }
.q-title { font-size: 28rpx; color: $text-main; line-height: 1.5; display: block; margin-bottom: 6rpx; }
.q-meta { font-size: 22rpx; color: $text-light; }
.q-arrow { color: #CCC; font-size: 32rpx; flex-shrink: 0; }

.empty-state { text-align: center; padding-top: 200rpx; }
.empty-title { font-size: 28rpx; color: $text-light; }

@media (min-width: 1025px) { .question-bank { max-width: 700px; margin: 0 auto; } }
</style>
