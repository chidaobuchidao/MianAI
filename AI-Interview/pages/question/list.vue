<template>
  <view class="q-list">
    <view class="filter">
      <picker mode="selector" :range="diffs" @change="onDiff"><view class="f-tag"><text>{{ diffText }}</text><text class="f-arrow">▼</text></view></picker>
      <picker mode="selector" :range="types" @change="onType"><view class="f-tag"><text>{{ typeText }}</text><text class="f-arrow">▼</text></view></picker>
    </view>

    <view v-if="questions.length">
      <view class="q-card" v-for="(q,i) in questions" :key="q.id">
        <view class="q-top">
          <text class="q-num">{{ (page-1)*10 + i + 1 }}</text>
          <text class="q-diff" :class="'d'+q.difficulty">{{ ['','简单','中等','困难'][q.difficulty] }}</text>
          <text class="q-type">{{ ['','单选','多选','判断','填空'][q.type] }}</text>
        </view>
        <text class="q-title">{{ q.title }}</text>
      </view>
    </view>
    <view v-else class="empty"><text class="empty-icon">📭</text><text class="empty-text">暂无题目</text></view>
    <view v-if="hasMore" class="more" @click="loadMore">加载更多</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { get } from '@/utils/request';
interface Q { id: number; categoryId: number; categoryName: string; type: number; title: string; difficulty: number; }
const questions = ref<Q[]>([]); const page = ref(1); const hasMore = ref(true);
const catId = ref<number|null>(null); const diff = ref<number|null>(null); const typ = ref<number|null>(null);
const diffs = ['全部难度','简单','中等','困难']; const diffText = ref('全部难度');
const types = ['全部题型','单选','多选','判断','填空']; const typeText = ref('全部题型');

onLoad(opts => {
  if (opts?.categoryId) { catId.value = Number(opts.categoryId); uni.setNavigationBarTitle({ title: decodeURIComponent(opts.categoryName||'题库') }); }
  load();
});

function load() {
  const params: Record<string, unknown> = { page: page.value, size: 10 };
  if (catId.value != null) params.categoryId = catId.value;
  if (diff.value != null) params.difficulty = diff.value;
  if (typ.value != null) params.type = typ.value;
  get<{records:Q[];current:number;pages:number}>('/api/questions', params).then(r => {
    if (page.value===1) questions.value = r.data.records; else questions.value.push(...r.data.records);
    hasMore.value = (r.data.current||1) < (r.data.pages||1);
  });
}
function loadMore() { page.value++; load(); }
function onDiff(e:{detail:{value:number}}) { diff.value = e.detail.value===0?null:e.detail.value; diffText.value = diffs[e.detail.value]; page.value=1; load(); }
function onType(e:{detail:{value:number}}) { typ.value = e.detail.value===0?null:e.detail.value; typeText.value = types[e.detail.value]; page.value=1; load(); }
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.q-list { min-height: 100vh; background: $bg-canvas; padding: 24rpx 28rpx; }

.filter { display: flex; gap: 12rpx; margin-bottom: 24rpx; flex-wrap: wrap; }
.f-tag {
  display: flex; align-items: center; gap: 8rpx;
  padding: 14rpx 24rpx; border: 1px solid $border-light; border-radius: $radius-full;
  font-size: 24rpx; color: $text-muted; background: $bg-paper;
}
.f-tag:active { background: $bg-surface; }
.f-arrow { font-size: 18rpx; }

.q-card {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx; margin-bottom: 16rpx;
  box-shadow: $shadow-sm;
}
.q-card:active { background: $bg-surface; }
.q-top { display: flex; align-items: center; gap: 12rpx; margin-bottom: 14rpx; }
.q-num { font-size: 22rpx; color: $text-light; font-weight: 500; }
.q-diff {
  font-size: 20rpx; font-weight: 600; padding: 4rpx 12rpx;
  border-radius: $radius-sm;
}
.q-diff.d1 { background: rgba(34,197,94,0.1); color: $color-success; }
.q-diff.d2 { background: rgba(217,117,10,0.1); color: $accent; }
.q-diff.d3 { background: rgba(239,68,68,0.1); color: $color-danger; }
.q-type {
  font-size: 20rpx; font-weight: 500; padding: 4rpx 12rpx;
  border-radius: $radius-sm; background: $bg-surface; color: $text-light;
}
.q-title { font-size: 28rpx; font-weight: 500; color: $text-main; line-height: 1.6; display: block; }

.empty { text-align: center; padding-top: 200rpx; }
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: $text-light; }

.more {
  text-align: center; padding: 28rpx; font-size: 26rpx;
  color: $text-light; font-weight: 500;
}
.more:active { color: $text-main; }

@media (min-width: 1025px) { .q-list { max-width: 800px; margin: 0 auto; } }
</style>
