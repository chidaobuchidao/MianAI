<template>
  <view class="history-page">
    <DeepStatusBar />
    <view class="list" v-if="items.length">
      <view class="item" v-for="r in items" :key="r.id" @click="goReport(r.id)">
        <view class="item-left">
          <text class="item-name">{{ r.fileName }}</text>
          <text class="item-pos" v-if="r.position">{{ r.position }}</text>
          <text class="item-time">{{ formatTime(r.createTime) }}</text>
        </view>
        <view class="item-right">
          <text class="tag" :class="parseTagClass(r.parseStatus)">
            {{ parseTagText(r.parseStatus) }}
          </text>
          <text class="score" v-if="r.score != null">{{ r.score }}/10</text>
        </view>
        <view class="item-swipe">
          <button class="btn-del" @click.stop="doDelete(r.id)">删除</button>
        </view>
      </view>
    </view>
    <view class="empty" v-else>
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无简历记录</text>
      <button class="btn-upload" @click="goUpload">上传第一份简历</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { get, del } from '@/utils/request';
import DeepStatusBar from '@/components/DeepStatusBar.vue';

interface ResumeItem {
  id: number; fileName: string; position: string; fileType: string;
  parseStatus: number; score: number | null; createTime: string;
}

const items = ref<ResumeItem[]>([]);

onMounted(async () => {
  await loadList();
});

async function loadList() {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list');
    if (r.data) {
      const enriched: ResumeItem[] = [];
      for (const item of r.data) {
        try {
          const a = await get<{ overallScore: number }>(`/api/resume/${item.id}/analysis`);
          enriched.push({ ...item, score: a.data?.overallScore ?? null });
        } catch { enriched.push({ ...item, score: null }); }
      }
      items.value = enriched;
    }
  } catch {}
}

function parseTagClass(s: number) {
  if (s === 0) return 'tag-parsing';
  if (s === 1) return 'tag-done';
  return 'tag-fail';
}
function parseTagText(s: number) {
  if (s === 0) return '解析中';
  if (s === 1) return '已解析';
  return '失败';
}
function formatTime(t: string) {
  if (!t) return '';
  return t.replace('T', ' ').substring(0, 16);
}
function goReport(id: number) { uni.navigateTo({ url: `/pages/resume/report?resumeId=${id}` }); }
function goUpload() { uni.redirectTo({ url: '/pages/resume/upload' }); }

async function doDelete(id: number) {
  const res = await uni.showModal({ title: '确认删除', content: '删除后无法恢复' });
  if (!res.confirm) return;
  try {
    await del(`/api/resume/${id}`);
    items.value = items.value.filter(i => i.id !== id);
    uni.showToast({ title: '已删除', icon: 'success' });
  } catch {
    uni.showToast({ title: '删除失败', icon: 'error' });
  }
}
</script>

<style lang="scss" scoped>
.history-page { min-height: 100vh; background: #f0f4ff; }
.list { padding: 20rpx 24rpx; }
.item {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  margin-bottom: 14rpx; display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
  &:active { background: #f8fafc; }
}
.item-left { flex: 1; }
.item-name { font-size: 28rpx; font-weight: 600; color: #0f172a; display: block; }
.item-pos { font-size: 24rpx; color: #64748b; margin-top: 4rpx; display: block; }
.item-time { font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; display: block; }
.item-right { display: flex; flex-direction: column; align-items: flex-end; gap: 6rpx; }
.tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.tag-parsing { background: #fef3c7; color: #d97706; }
.tag-done { background: #dcfce7; color: #16a34a; }
.tag-fail { background: #fee2e2; color: #ef4444; }
.score { font-size: 26rpx; font-weight: 700; color: #2b6ff2; }
.btn-del { font-size: 22rpx; color: #ef4444; background: #fef2f2; border: none; padding: 8rpx 16rpx; border-radius: 8rpx; }

.empty { display: flex; flex-direction: column; align-items: center; padding-top: 200rpx; }
.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
.btn-upload { margin-top: 30rpx; font-size: 28rpx; color: #fff; background: linear-gradient(135deg, #2b6ff2, #4f8dff); border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }
</style>
