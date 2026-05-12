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
.q-list { min-height: 100vh; background: #f0f4ff; }
.filter { display: flex; gap: 16rpx; padding: 20rpx 24rpx; background: #fff; }
.f-tag { display: flex; align-items: center; gap: 8rpx; padding: 12rpx 22rpx; background: #f1f5f9; border-radius: 8rpx; font-size: 24rpx; color: #64748b; }
.f-arrow { font-size: 18rpx; }
.q-card { background: #fff; margin: 16rpx 24rpx; padding: 28rpx; border-radius: 20rpx; box-shadow: 0 2rpx 10rpx rgba(0,0,0,0.03); }
.q-top { display: flex; align-items: center; gap: 14rpx; margin-bottom: 16rpx; }
.q-num { width: 40rpx; height: 40rpx; background: #2b6ff2; color: #fff; font-size: 22rpx; font-weight: 700; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; }
.q-diff { font-size: 20rpx; padding: 4rpx 10rpx; border-radius: 6rpx; }
.d1 { background: #ecfdf5; color: #10b981; }
.d2 { background: #fef3c7; color: #f59e0b; }
.d3 { background: #fef2f2; color: #ef4444; }
.q-type { font-size: 22rpx; color: #94a3b8; }
.q-title { font-size: 28rpx; font-weight: 500; color: #1e293b; line-height: 1.7; display: block; }
.empty { display: flex; flex-direction: column; align-items: center; padding-top: 180rpx; }
.empty-icon { font-size: 80rpx; opacity: 0.6; }
.empty-text { font-size: 28rpx; color: #94a3b8; margin-top: 16rpx; }
.more { text-align: center; padding: 30rpx; color: #2b6ff2; font-size: 26rpx; font-weight: 600; }
</style>
