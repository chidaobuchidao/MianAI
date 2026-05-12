<template>
  <view class="ts-overlay" v-if="visible" @click="$emit('close')">
    <view class="ts-panel" @click.stop>
      <text class="ts-title">选择简历模板</text>
      <scroll-view scroll-y class="ts-list">
        <view class="ts-item"
              v-for="tpl in list" :key="tpl.id"
              :class="{ selected: selectedId === tpl.id }"
              @click="selectedId = tpl.id">
          <view class="ts-preview" :style="{ background: '#'+(tpl.bgColor||'fff'), borderTop: '6rpx solid #'+(tpl.accentColor||'2b6ff2') }">
            <text class="ts-preview-name">{{ tpl.name }}</text>
          </view>
          <view class="ts-info">
            <text class="ts-name">{{ tpl.name }}</text>
            <text class="ts-desc">{{ tpl.description }}</text>
          </view>
          <view class="ts-check" v-if="selectedId === tpl.id">&#10003;</view>
        </view>
      </scroll-view>
      <button class="ts-btn" :disabled="!selectedId" @click="confirm">
        使用模板生成
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface Template {
  id: number; name: string; description: string;
  styleClass: string; bgColor: string; accentColor: string;
}

defineProps<{ visible: boolean; list: Template[] }>();
const emit = defineEmits<{ close: []; select: [id: number] }>();
const selectedId = ref<number | null>(null);

function confirm() {
  if (selectedId.value != null) {
    emit('select', selectedId.value);
    selectedId.value = null;
  }
}
</script>

<style lang="scss" scoped>
.ts-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 999; display: flex; align-items: flex-end; }
.ts-panel { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 30rpx 24rpx 40rpx; }
.ts-title { font-size: 32rpx; font-weight: 700; color: #0f172a; text-align: center; display: block; margin-bottom: 24rpx; }
.ts-list { max-height: 50vh; }
.ts-item { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 12rpx; border-radius: 16rpx; margin-bottom: 12rpx; position: relative; }
.ts-item.selected { background: #f0f4ff; }
.ts-preview { width: 100rpx; height: 70rpx; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; padding: 6rpx; }
.ts-preview-name { font-size: 18rpx; font-weight: 700; color: #333; text-align: center; }
.ts-info { flex: 1; }
.ts-name { font-size: 28rpx; font-weight: 600; color: #1e293b; display: block; }
.ts-desc { font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; display: block; }
.ts-check { width: 40rpx; height: 40rpx; background: #2b6ff2; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 22rpx; flex-shrink: 0; }
.ts-btn { width: 100%; height: 88rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff); color: #fff; font-size: 30rpx; font-weight: 700; border-radius: 44rpx; border: none; margin-top: 24rpx; }
.ts-btn[disabled] { background: #cbd5e1; color: #94a3b8; }
</style>
