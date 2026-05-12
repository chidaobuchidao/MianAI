<template>
  <view class="upload-page">
    <DeepStatusBar />
    <!-- 继续优化提示 -->
    <view class="resume-bar" v-if="activeResume" @click="goReport">
      <text class="rb-text">📋 您有简历正在优化中，点击继续查看</text>
    </view>
    <!-- 标题区 -->
    <view class="header">
      <text class="h-title">简历优化</text>
      <text class="h-desc">上传简历，AI 帮你分析优化</text>
    </view>

    <!-- 文件选择区 -->
    <view class="section">
      <view class="file-picker" @click="chooseFile">
        <view class="fp-icon" v-if="!fileInfo">📄</view>
        <view class="fp-icon" v-else>✅</view>
        <text class="fp-text" v-if="!fileInfo">点击选择简历文件</text>
        <text class="fp-text" v-else>{{ fileInfo.name }}</text>
        <text class="fp-hint" v-if="!fileInfo">支持 PDF、Word、图片</text>
        <text class="fp-hint" v-else>{{ formatSize(fileInfo.size) }}</text>
      </view>
    </view>

    <!-- JD 输入区 -->
    <view class="section">
      <text class="s-label">目标岗位描述</text>
      <textarea class="jd-input" v-model="jobDescription"
        placeholder="粘贴目标岗位的 JD 描述，如：&#10;负责公司核心业务系统的后端开发，要求：&#10;1. 精通Java，熟悉Spring Boot&#10;2. 熟悉MySQL、Redis&#10;3. 有分布式系统经验..."
        :maxlength="-1" auto-height />
    </view>

    <!-- 岗位名称 -->
    <view class="section">
      <text class="s-label">岗位名称（选填）</text>
      <input class="pos-input" v-model="position" placeholder="如：Java后端开发工程师" />
    </view>

    <!-- 提交按钮 -->
    <view class="btn-wrap">
      <button class="btn-upload" :disabled="uploading || !canSubmit" @click="doUpload">
        {{ uploading ? '上传中...' : '上传并解析' }}
      </button>
    </view>

    <!-- 解析进度 -->
    <view class="progress-section" v-if="uploading">
      <view class="progress-bar">
        <view class="progress-fill" :class="progressClass" :style="{ width: progressWidth }" />
      </view>
      <text class="progress-text">{{ progressText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { get } from '@/utils/request';

interface FileInfo { name: string; size: number; path: string; }

const fileInfo = ref<FileInfo | null>(null);
const activeResume = ref<any>(null);

function checkActive() {
  try {
    const raw = uni.getStorageSync('deep_optimizing');
    if (raw) activeResume.value = JSON.parse(raw);
  } catch { activeResume.value = null; }
}
checkActive();

function goReport() {
  if (activeResume.value) {
    uni.navigateTo({ url: `/pages/resume/report?resumeId=${activeResume.value.resumeId}` });
  }
}
const jobDescription = ref('');
const position = ref('');
const uploading = ref(false);
const progressWidth = ref('0%');
const progressClass = ref('');
const progressText = ref('');

const canSubmit = computed(() => fileInfo.value && jobDescription.value.trim());

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

function chooseFile() {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png'],
    success: (res) => {
      fileInfo.value = {
        name: res.tempFiles[0].name,
        size: res.tempFiles[0].size,
        path: res.tempFiles[0].path,
      };
    },
  });
}

async function doUpload() {
  if (!canSubmit.value || uploading.value) return;
  uploading.value = true;
  progressWidth.value = '20%';
  progressText.value = '正在上传...';

  try {
    const token = uni.getStorageSync('mianmiantong_token') || '';

    const res = await uni.uploadFile({
      url: 'http://192.168.137.134:8080/api/resume/upload',
      filePath: fileInfo.value!.path,
      name: 'file',
      formData: {
        jobDescription: jobDescription.value.trim(),
        position: position.value.trim(),
      },
      header: {
        Authorization: 'Bearer ' + token,
      },
    });

    const data = JSON.parse(res.data);
    if (data.code !== 200) throw new Error(data.message);

    const { resumeId } = data.data;
    progressWidth.value = '40%';
    progressText.value = '正在解析简历...';

    await pollStatus(resumeId);

  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '上传失败';
    uni.showToast({ title: msg, icon: 'error' });
    uploading.value = false;
  }
}

async function pollStatus(resumeId: number) {
  let attempts = 0;
  const maxAttempts = 30;
  progressClass.value = 'animating';

  while (attempts < maxAttempts) {
    await sleep(2000);
    attempts++;

    const pct = 40 + Math.floor(attempts / maxAttempts * 40);
    progressWidth.value = pct + '%';

    try {
      const r = await get<{ parseStatus: number; statusText: string; parsedText?: string }>(
        `/api/resume/${resumeId}/status`
      );

      if (r.data.parseStatus === 1) {
        progressWidth.value = '100%';
        progressText.value = '解析完成，正在跳转...';
        await sleep(500);
        uni.navigateTo({ url: `/pages/resume/report?resumeId=${resumeId}` });
        return;
      }
      if (r.data.parseStatus === -1) {
        throw new Error('解析失败，请检查文件格式');
      }
      progressText.value = r.data.statusText;
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '';
      if (msg.includes('解析失败')) throw e;
    }
  }
  throw new Error('解析超时，请重试');
}

function sleep(ms: number): Promise<void> { return new Promise(resolve => setTimeout(resolve, ms)); }
</script>

<style lang="scss" scoped>
.upload-page { min-height: 100vh; background: #f0f4ff; padding: 30rpx; }
.resume-bar { background: linear-gradient(135deg, #6366f1, #8b5cf6); padding: 16rpx 24rpx; border-radius: 16rpx; margin-bottom: 20rpx; }
.rb-text { color: #fff; font-size: 24rpx; display: block; }
.header { text-align: center; padding: 40rpx 0; }
.h-title { font-size: 40rpx; font-weight: 800; color: #0f172a; display: block; }
.h-desc { font-size: 26rpx; color: #94a3b8; margin-top: 10rpx; display: block; }

.section { margin-bottom: 30rpx; }
.s-label { font-size: 28rpx; font-weight: 600; color: #1e293b; display: block; margin-bottom: 14rpx; }

.file-picker {
  background: #fff; border: 2rpx dashed #cbd5e1; border-radius: 20rpx;
  padding: 60rpx 40rpx; text-align: center;
  transition: all 0.2s;
}
.file-picker:active { border-color: #2b6ff2; background: #f8faff; }
.fp-icon { font-size: 64rpx; margin-bottom: 16rpx; }
.fp-text { font-size: 28rpx; color: #1e293b; font-weight: 500; display: block; }
.fp-hint { font-size: 24rpx; color: #94a3b8; margin-top: 6rpx; display: block; }

.jd-input {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  font-size: 26rpx; line-height: 1.6; min-height: 200rpx;
  width: 100%; box-sizing: border-box;
}
.pos-input {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  font-size: 26rpx; width: 100%; box-sizing: border-box;
}

.btn-wrap { padding: 20rpx 0; }
.btn-upload {
  width: 100%; height: 96rpx; background: linear-gradient(135deg, #2b6ff2, #4f8dff);
  color: #fff; font-size: 32rpx; font-weight: 700; border-radius: 48rpx; border: none;
}
.btn-upload[disabled] { background: #cbd5e1; color: #94a3b8; }

.progress-section { margin-top: 30rpx; }
.progress-bar { height: 8rpx; background: #e2e8f0; border-radius: 4rpx; overflow: hidden; }
.progress-fill { height: 100%; background: #2b6ff2; border-radius: 4rpx; transition: width 0.5s; }
.progress-fill.animating { background: linear-gradient(90deg, #2b6ff2, #6366f1, #2b6ff2); background-size: 200% 100%; animation: shimmer 1.5s infinite; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.progress-text { font-size: 24rpx; color: #64748b; margin-top: 12rpx; display: block; text-align: center; }
</style>
