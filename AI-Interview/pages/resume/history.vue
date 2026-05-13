<template>
  <view class="history-page">
    <DeepStatusBar />
    <view class="list" v-if="items.length">
      <view class="item-wrap" v-for="r in items" :key="r.id">
        <view class="item-delete-bg" @click.stop="doDelete(r.id)">
          <text class="del-text">删除</text>
        </view>
        <view class="item-content"
          :class="{ 'item-swiped': swipedId === r.id }"
          @touchstart="onTouchStart"
          @touchend="onTouchEnd($event, r.id)"
          @click="goReport(r.id)">
          <view class="item-left">
            <text class="item-name">{{ r.fileName }}</text>
            <text class="item-time">{{ formatTime(r.createTime) }}</text>
          </view>
          <view class="item-right">
            <text class="tag" :class="tagClass(r)">{{ tagText(r) }}</text>
          </view>
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
  id: number;
  fileName: string;
  parseStatus: number;
  overallScore: number | null;
  deepStatus: number | null;
  createTime: string;
}

const items = ref<ResumeItem[]>([]);
const swipedId = ref<number | null>(null);
let touchStartX = 0;

onMounted(async () => {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list');
    if (r.data) items.value = r.data;
  } catch {}
});

function tagClass(r: ResumeItem): string {
  if (r.deepStatus === 2) return 'tag-done';
  if (r.overallScore != null) return 'tag-partial';
  if (r.parseStatus === 0) return 'tag-parsing';
  if (r.parseStatus === -1) return 'tag-fail';
  return 'tag-default';
}
function tagText(r: ResumeItem): string {
  if (r.deepStatus === 2) return '已优化';
  if (r.overallScore != null) return '待优化';
  if (r.parseStatus === 0) return '解析中';
  if (r.parseStatus === -1) return '解析失败';
  return '待解析';
}

function onTouchStart(e: any) {
  touchStartX = e.touches[0].clientX;
}
function onTouchEnd(e: any, id: number) {
  const delta = touchStartX - e.changedTouches[0].clientX;
  if (delta > 50) {
    swipedId.value = (swipedId.value === id) ? null : id;
  } else {
    swipedId.value = null;
  }
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
    swipedId.value = null;
    uni.showToast({ title: '已删除', icon: 'success' });
  } catch {
    uni.showToast({ title: '删除失败', icon: 'error' });
  }
}
</script>

<style lang="scss" scoped>
.history-page { min-height: 100vh; background: #f0f4ff; }
.list { padding: 20rpx 24rpx; }

.item-wrap { position: relative; overflow: hidden; border-radius: 16rpx; margin-bottom: 14rpx; }
.item-delete-bg {
  position: absolute; right: 0; top: 0; bottom: 0;
  width: 140rpx; background: #ef4444;
  display: flex; align-items: center; justify-content: center;
}
.del-text { color: #fff; font-size: 26rpx; font-weight: 600; }

.item-content {
  position: relative; z-index: 1; background: #fff;
  border-radius: 16rpx; padding: 24rpx;
  display: flex; justify-content: space-between; align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.03);
  transition: transform 0.25s ease;
  &:active { background: #f8fafc; }
}
.item-swiped { transform: translateX(-140rpx); }

.item-left { flex: 1; overflow: hidden; }
.item-name {
  font-size: 28rpx; font-weight: 600; color: #0f172a; display: block;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.item-time { font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; display: block; }
.item-right { display: flex; flex-direction: column; align-items: flex-end; gap: 6rpx; flex-shrink: 0; margin-left: 12rpx; }

.tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; white-space: nowrap; }
.tag-done { background: #dcfce7; color: #16a34a; }
.tag-partial { background: #eff6ff; color: #2b6ff2; }
.tag-parsing { background: #fef3c7; color: #d97706; }
.tag-fail { background: #fee2e2; color: #ef4444; }
.tag-default { background: #f1f5f9; color: #94a3b8; }

.empty { display: flex; flex-direction: column; align-items: center; padding-top: 200rpx; }
.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
.btn-upload { margin-top: 30rpx; font-size: 28rpx; color: #fff; background: linear-gradient(135deg, #2b6ff2, #4f8dff); border-radius: 40rpx; border: none; padding: 16rpx 48rpx; }
</style>
